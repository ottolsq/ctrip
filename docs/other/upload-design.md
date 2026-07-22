# 文件上传功能设计方案

> 实施前先在此文档中写明方案，确认后再执行。

---

## 一、背景与需求

### 1.1 现状

文件上传模块的基础代码已存在（`ImageStorageService` 接口 + `LocalStorageServiceImpl` + `UploadController`），但有若干问题需要修复和完善。

### 1.2 上传接口定位

`POST /api/v1/uploads/image` 是一个**通用基础设施**——谁需要存文件谁就用它，上传完拿到 URL 字符串后填入对应业务表的对应字段即可。

从**普通用户**的角度，只有两种场景会主动触发上传：

| 场景 | 触发者 | 存储字段 | 所在表 |
|------|--------|---------|--------|
| 发攻略时插入图片 | 普通用户 | `cover_url` / `image_urls` | `guide` |
| 修改个人头像 | 普通用户 | `avatar_url` | `users` |

目的地/景点图片不需要普通用户上传——那是管理员在后台通过 `POST /api/v1/admin/destinations` 等接口配置的，图片来源同样是先调上传接口拿到 URL。

### 1.3 所有存储图片的字段

项目中所有图片都以**路径字符串**形式存储在数据库中，没有独立的图片表：

| 场景 | 存储字段 | 所在表 | 数量 | 谁写入 |
|------|---------|--------|------|--------|
| 攻略封面 | `cover_url` | `guide` | 单张 | 普通用户 |
| 攻略配图 | `image_urls` | `guide` | JSON 数组 | 普通用户 |
| 用户头像 | `avatar_url` | `users` | 单张 | 普通用户 |
| 目的地封面 | `cover_url` | `destination` | 单张 | 管理员 |
| 目的地图集 | `image_urls` | `destination` | JSON 数组 | 管理员 |
| 景点封面 | `cover_url` | `attraction` | 单张 | 管理员 |
| 景点图集 | `image_urls` | `attraction` | JSON 数组 | 管理员 |

### 1.4 数据流

```
① 上传:  用户上传图片 → 服务端保存到磁盘 → 返回路径 URL
          ↓
② 写入:  前端拿到 URL → 填入业务请求（发布攻略/改头像/管理目的地等）
          ↓
③ 存储:  数据库存储路径字符串 (cover_url / image_urls / avatar_url)
          ↓
④ 读取:  前端调用业务 API → Response JSON 中返回路径字符串
          ↓
⑤ 渲染:  浏览器 <img src="路径"> → GET /uploads/images/xxx.jpg
          ↓
⑥ 映射:  WebConfig 静态资源映射 → 磁盘文件返回
```

**关键：第⑤步是浏览器直接发 GET 请求加载图片**，不经过 Controller，走的是 Spring MVC 静态资源映射。因此 `SecurityConfig` 必须放行 `/uploads/**`，否则所有图片 401。

---

## 二、现有代码问题

### 2.1 🔴 SecurityConfig 未放行上传目录

**问题：** `SecurityConfig.java:92` 有 `.anyRequest().authenticated()`，`/uploads/**` 路径也需要 JWT 认证。

**影响：** 攻略详情页看不到配图、用户头像加载失败、目的地/景点图片无法公开浏览。**前端无法直接通过 URL 加载任何图片。**

**修复：** 在 `SecurityConfig` 中添加 `/uploads/**` 到 `permitAll()`。

### 2.2 🟡 存储路径可以简化

**当前：** `uploads/images/{yyyy}/{MM}/{dd}/{uuid}.ext`
**建议：** `uploads/images/{uuid}.ext`

**理由：**
- MVP 阶段图片量不大，日期分目录意义有限
- 后续需要时可随时改回日期分目录（`ImageStorageService` 接口抽象，改实现即可）
- 路径更短，API 返回的 URL 更简洁

### 2.3 🟡 Magic Bytes 校验缺失

**当前：** 仅检查文件扩展名（`.jpg`/`.png`），没有读取文件头魔数。

**风险：** 攻击者可以把恶意文件改扩展名为 `.jpg` 上传绕过校验。

**修复：** 上传时读取文件前几个字节，校验魔数是否匹配声称的扩展名。

### 2.4 🟡 文件大小校验依赖 Spring

**当前：** `LocalStorageServiceImpl` 中定义了 `MAX_FILE_SIZE = 5MB` 但从未使用。实际拦截靠的是 `spring.servlet.multipart.max-file-size=5MB`。

**决议：** Service 层加入冗余校验（多层防御，即使 Spring 配置被误改也能兜底）。

### 2.5 🟡 gif 格式支持

**当前：** `ALLOWED_EXTENSIONS` 包含 `gif`，但文档（`content_module.md`、`api_standards.md`）只写了 `jpg/png/webp`。

**决议：** 移除 `gif`，仅支持 jpg/jpeg/png/webp，与文档保持一致。

### 2.6 🔴 删除接口问题

**问题两处：**

1. **路径结构**：Controller 用 `{year}/{month}/{day}/{filename}` 拼接 URL，与 `LocalStorageServiceImpl.delete()` 的路径解析耦合。存储路径改为扁平结构后，Delete 接口简化为 `DELETE /api/v1/uploads/{filename}`。

2. **无所有者校验**：`@AuthenticationPrincipal Long userId` 参数接收了但从未使用，任何认证用户都能删除任意图片。

**决议：** MVP 阶段暂不校验所有者，后续建 `upload_records` 表记录上传者再补齐。

---

## 三、设计方案

### 3.1 存储路径

```
{项目根目录}/uploads/images/{uuid}.{ext}
```

- 访问 URL：`/uploads/images/{uuid}.{ext}`
- 文件名：`UUID.randomUUID()` 保证唯一性
- 配置项：
  ```properties
  app.upload.dir=uploads/images
  app.upload.base-url=/uploads/images
  ```

### 3.2 支持的文件类型

| 格式 | 扩展名 | 魔数 (Magic Bytes) | 说明 |
|------|--------|-------------------|------|
| JPEG | `.jpg`, `.jpeg` | `FF D8 FF` | 最常用 |
| PNG | `.png` | `89 50 4E 47` | 无损/透明 |
| WebP | `.webp` | `52 49 46 46` | 现代格式，体积小 |

> GIF 已移除，与 `api_standards.md` / `content_module.md` 文档保持一致。

### 3.3 大小限制

| 限制 | 值 | 配置位置 |
|------|-----|---------|
| 单文件最大 | 5MB | `spring.servlet.multipart.max-file-size` |
| 请求最大 | 50MB | `spring.servlet.multipart.max-request-size`（批量上传 10 张） |
| Service 层冗余校验 | 5MB | `LocalStorageServiceImpl` |

### 3.4 安全校验链路

```
请求进入
  → Spring MultipartResolver 校验文件大小 (max-file-size=5MB)
    → UploadController 接收 MultipartFile
      → LocalStorageServiceImpl.upload()
        ① 校验文件大小 → MAX_FILE_SIZE 冗余检查
        ② 提取扩展名 → 校验白名单 (jpg/jpeg/png/webp)
        ③ 读取文件头魔数 → 校验与扩展名匹配
        ④ 生成 UUID 文件名
        ⑤ 写入磁盘
        ⑥ 返回 URL
```

### 3.5 删除接口

```
DELETE /api/v1/uploads/{filename}
```

- `filename` 为上传时返回的文件名（含扩展名），如 `a1b2c3d4.jpg`
- Controller 校验 `filename` 不包含 `..`、`/`、`\`
- Service 层根据 `baseUrl` + `filename` 定位文件并删除
- **所有者校验：** MVP 阶段暂不做，后续建 `upload_records` 表补齐

### 3.6 SecurityConfig 修改

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/uploads/**").permitAll()          // ← 新增：图片公开访问
    .requestMatchers("/api/v1/blind-box").permitAll()
    // ... 其余不变
)
```

> **注意：** `/uploads/**` 本质是本地静态文件，不涉及业务逻辑，公开访问无安全风险。
> 但需确保 `LocalStorageServiceImpl` 的路径穿越防御（已有）防止恶意路径读取非上传目录的文件。

---

## 四、涉及文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `config/SecurityConfig.java` | 修改 | 添加 `/uploads/**` 到 permitAll |
| `content/service/storage/LocalStorageServiceImpl.java` | 修改 | 简化路径 + magic bytes + 大小校验 |
| `content/service/storage/ImageStorageService.java` | 不变 | 接口无需改动 |
| `content/controller/UploadController.java` | 修改 | 修复删除接口路径 |
| `content/dto/response/ImageUploadResponse.java` | 不变 | — |
| `content/dto/response/ImageBatchUploadResponse.java` | 不变 | — |
| `config/WebConfig.java` | 不变 | 静态资源映射已正确 |
| `resources/application.properties` | 不变 | 配置项不变 |

---

## 五、Magic Bytes 校验实现参考

```java
// 各格式魔数（文件头字节）
private static final byte[][] JPEG_MAGIC = {
    {(byte)0xFF, (byte)0xD8, (byte)0xFF}            // FF D8 FF
};
private static final byte[] PNG_MAGIC = {
    (byte)0x89, 0x50, 0x4E, 0x47                      // 89 50 4E 47
};
private static final byte[][] WEBP_MAGIC = {
    {0x52, 0x49, 0x46, 0x46}                          // RIFF
};

/**
 * 校验文件头魔数与扩展名是否匹配。
 * 防止攻击者修改扩展名绕过格式校验。
 */
private boolean isValidMagicBytes(byte[] header, String ext) {
    return switch (ext) {
        case "jpg", "jpeg" -> matchesAny(header, JPEG_MAGIC);
        case "png" -> matchesExact(header, PNG_MAGIC);
        case "webp" -> matchesAny(header, WEBP_MAGIC);
        default -> false;
    };
}
```

---

## 六、实施步骤

| 步骤 | 内容 | 状态 |
|------|------|------|
| 1 | `SecurityConfig` 添加 `/uploads/**` permitAll | ✅ 已完成 |
| 2 | `LocalStorageServiceImpl` 简化存储路径 | ✅ 已完成 |
| 3 | `LocalStorageServiceImpl` 添加 Magic Bytes 校验 | ✅ 已完成 |
| 4 | `LocalStorageServiceImpl` 添加文件大小冗余校验 | ✅ 已完成 |
| 5 | `UploadController` 修复删除接口路径 | ✅ 已完成 |
| 6 | 手动测试验证 | ✅ 已完成 |

---

## 七、测试结果（2026-07-02）

| # | 测试项 | 预期 | 实际结果 | 状态 |
|---|--------|------|---------|------|
| 1 | 上传有效 JPEG | 200 + URL | `{"url":"/uploads/images/uuid.jpg"}` | ✅ |
| 2 | 上传伪装的 .jpg（文本文件改扩展名） | Magic Bytes 拦截 | `"文件类型与扩展名不匹配，拒绝上传"` | ✅ |
| 3 | 上传有效 PNG | 200 + URL | `{"url":"/uploads/images/uuid.png"}` | ✅ |
| 4 | 无 JWT 访问已上传图片 | 200 + 图片内容 | HTTP 200，文件大小正确 | ✅ |
| 5 | 上传 GIF 格式 | 扩展名白名单拦截 | `"不支持的图片格式：gif，仅支持 [jpeg, webp, png, jpg]"` | ✅ |
| 6 | 删除已上传图片 | 200 + 磁盘文件删除 | `{"success":true}` + 磁盘文件已删除 | ✅ |
| 7 | 路径穿越攻击（`../`） | 被拦截 | 请求被拒绝 | ✅ |

### 测试中发现的 Bug 及修复

| Bug | 原因 | 修复 |
|-----|------|------|
| `DELETE` 接口返回"非法路径" | `Path.startsWith()` 比较时，相对路径 `Path` 与绝对路径 `Path` 永远不匹配（Java NIO 语义）。原代码 `Path.of(uploadDir, relativePath).normalize()` 得到相对路径，无法通过 `startsWith(absolutePath)` 检查。 | 改为 `Path.of(uploadDir).toAbsolutePath().normalize().resolve(relativePath)` 确保 filePath 也是绝对路径后再比较。 |







后端不需要区分。上传接口 POST /api/v1/uploads/image 不知道也不关心图片的用途——它只管：

  接收文件 →校验 →存盘 →返回 URL

  区分发生在下一步：

  同一个上传接口                         不同的业务接口
  ─────────────────────────────────────────────────────────────
  POST /api/v1/uploads/image  → URL ─→PUT /api/v1/users/me/avatar     →头像
                                      ─→POST /api/v1/guides            →攻略图片
                                      ─→POST /api/v1/admin/destinations →目的地图片

  就像文件系统里的 write() 函数——它只管把字节写到磁盘，不会问"你这 Word 文档还是 Excel
  表格？"。上传接口就是通用的文件存取层，业务语义由调用方决定。
