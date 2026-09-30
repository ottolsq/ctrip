# 内容与目的地模块逻辑分析

## 📋 一、整体架构

内容模块采用与用户模块一致的**三层架构**：

- **Controller层**：处理HTTP请求和响应
- **Service层**：业务逻辑处理
- **Mapper层**：数据访问（MyBatis Plus）

### 核心组件

| 组件 | 路径 | 职责 |
|------|------|------|
| DestinationController | `/api/v1/destinations/**` | 目的地列表、详情查询（公开） |
| AttractionController | `/api/v1/attractions/**` | 景点列表、详情查询（公开） |
| GuideController | `/api/v1/guides/**` | 攻略发布、浏览、编辑、删除、点赞 |
| CommentController | `/api/v1/guides/{id}/comments/**` | 评论列表、发表、删除、回复 |
| UploadController | `/api/v1/uploads/**` | 图片上传、删除 |
| DestinationService | `com.ctrip.content.service` | 目的地业务逻辑 |
| AttractionService | `com.ctrip.content.service` | 景点业务逻辑 |
| GuideService | `com.ctrip.content.service` | 攻略业务逻辑 |
| CommentService | `com.ctrip.content.service` | 评论业务逻辑 |
| ImageStorageService | `com.ctrip.content.service` | 图片存储接口抽象（MVP 本地存储） |

### 包结构

```
com.ctrip.content/
├── controller/
│   ├── DestinationController.java
│   ├── AttractionController.java
│   ├── GuideController.java
│   ├── CommentController.java
│   └── UploadController.java
├── dto/
│   ├── request/
│   │   ├── CreateDestinationRequest.java
│   │   ├── UpdateDestinationRequest.java
│   │   ├── CreateAttractionRequest.java
│   │   ├── UpdateAttractionRequest.java
│   │   ├── CreateGuideRequest.java
│   │   ├── UpdateGuideRequest.java
│   │   └── CreateCommentRequest.java
│   └── response/
│       ├── DestinationResponse.java
│       ├── AttractionResponse.java
│       ├── GuideResponse.java
│       ├── GuideListResponse.java
│       ├── CommentResponse.java
│       ├── CommentTreeResponse.java
│       └── ImageUploadResponse.java
├── entity/
│   ├── enums/
│   │   ├── GuideStatus.java
│   │   └── ItemType.java
│   ├── Destination.java
│   ├── Attraction.java
│   ├── Guide.java
│   └── Comment.java
├── mapper/
│   ├── DestinationMapper.java
│   ├── AttractionMapper.java
│   ├── GuideMapper.java
│   └── CommentMapper.java
├── service/
│   ├── DestinationService.java
│   ├── DestinationServiceImpl.java
│   ├── AttractionService.java
│   ├── AttractionServiceImpl.java
│   ├── GuideService.java
│   ├── GuideServiceImpl.java
│   ├── CommentService.java
│   ├── CommentServiceImpl.java
│   ├── ImageStorageService.java
│   └── LocalStorageServiceImpl.java
└── converter/
    ├── DestinationConverter.java
    ├── AttractionConverter.java
    ├── GuideConverter.java
    └── CommentConverter.java
```

---

## 🗄️ 二、数据模型设计

### 1. Destination 实体（destination 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| name | String | 目的地名称 |
| country | String | 国家 |
| province | String | 省份/州 |
| description | String | 描述信息 |
| bestSeason | String | 最佳旅行季节 |
| coverUrl | String | 封面图 URL |
| imageUrls | String | 多图集 JSON 数组 `["url1", "url2", ...]` |
| createdAt | LocalDateTime | 创建时间（自动填充） |

**设计要点：**
- `imageUrls` 使用 JSON 类型，存储图片 URL 数组，MVP 阶段不建独立图片表（方案 A）
- 管理端可维护 `coverUrl` + `imageUrls`，前台展示时返回完整图集

---

### 2. Attraction 实体（attraction 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| destinationId | Long | 所属目的地 ID（FK→destination） |
| name | String | 景点名称 |
| description | String | 景点描述 |
| location | String | 地理位置/地址 |
| ticketPrice | BigDecimal | 门票价格（可为 null） |
| coverUrl | String | 封面图 URL |
| imageUrls | String | 多图集 JSON 数组 |
| createdAt | LocalDateTime | 创建时间（自动填充） |

---

### 3. Guide 实体（guide 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| authorId | Long | 作者 ID（FK→users） |
| title | String | 攻略标题 |
| content | String | 攻略正文（支持 Markdown/HTML） |
| destinationId | Long | 关联目的地 ID（FK→destination，可为 null） |
| coverUrl | String | 封面图 URL |
| imageUrls | String | 内容图集 JSON 数组 |
| status | GuideStatus | 状态（DRAFT/PUBLISHED/REJECTED） |
| viewCount | Integer | 浏览量（默认 0） |
| likeCount | Integer | 点赞数（默认 0） |
| createdAt | LocalDateTime | 创建时间（自动填充） |
| updatedAt | LocalDateTime | 更新时间（自动填充） |

**状态枚举（GuideStatus）：**

| 状态 | 值 | 说明 |
|------|-----|------|
| DRAFT | 0 | 草稿，仅作者可见 |
| PUBLISHED | 1 | 已发布，所有人可见 |
| REJECTED | 2 | 审核拒绝 |

---

### 4. Comment 实体（comment 表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| guideId | Long | 所属攻略 ID（FK→guide） |
| userId | Long | 评论者 ID（FK→users） |
| parentId | Long | 父评论 ID（自关联，支持回复，null 表示根评论） |
| content | String | 评论内容 |
| likeCount | Integer | 点赞数（默认 0） |
| createdAt | LocalDateTime | 创建时间（自动填充） |

---

## 📍 三、目的地模块

### 1. 目的地列表 (`GET /api/v1/destinations`)

**流程：**

1. 接收分页参数（page、limit）和筛选参数（country、province、keyword）
2. 构建 LambdaQueryWrapper 动态查询条件
3. 执行分页查询
4. 通过 DestinationConverter 转换为分页响应
5. 返回分页结果

**关键代码位置：** `DestinationController.list()` → `DestinationServiceImpl.list()`

---

### 2. 目的地详情 (`GET /api/v1/destinations/{id}`)

**流程：**

1. 根据 ID 查询目的地
2. 不存在则返回 404
3. 关联查询该目的地下所有景点列表
4. 转换为 DestinationResponse（含景点列表）
5. 返回

**关键代码位置：** `DestinationController.getDetail()` → `DestinationServiceImpl.getDetail()`

---

### 3. 目的地 CRUD（管理端）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/admin/destinations` | 列表（含筛选） | 仅 ADMIN |
| POST | `/api/v1/admin/destinations` | 创建 | 仅 ADMIN |
| PUT | `/api/v1/admin/destinations/{id}` | 更新 | 仅 ADMIN |
| DELETE | `/api/v1/admin/destinations/{id}` | 删除 | 仅 ADMIN |

---

## 🏞️ 四、景点模块

### 1. 景点列表 (`GET /api/v1/attractions`)

**流程：**

1. 接收分页参数和筛选参数（destinationId、keyword）
2. 构建 LambdaQueryWrapper 动态查询
3. 执行分页查询
4. 转换为 AttractionResponse 分页结果
5. 返回

**关键代码位置：** `AttractionController.list()` → `AttractionServiceImpl.list()`

---

### 2. 景点详情 (`GET /api/v1/attractions/{id}`)

**流程：**

1. 根据 ID 查询景点
2. 不存在则返回 404
3. 转换为 AttractionResponse
4. 返回

**关键代码位置：** `AttractionController.getDetail()`

---

### 3. 景点 CRUD（管理端）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/admin/attractions` | 列表（含筛选） | 仅 ADMIN |
| POST | `/api/v1/admin/attractions` | 创建 | 仅 ADMIN |
| PUT | `/api/v1/admin/attractions/{id}` | 更新 | 仅 ADMIN |
| DELETE | `/api/v1/admin/attractions/{id}` | 删除 | 仅 ADMIN |

---

## 📝 五、攻略社区模块

### 1. 发布攻略 (`POST /api/v1/guides`)

**流程：**

1. 从 @AuthenticationPrincipal 获取当前用户 ID（authorId）
2. 校验请求参数（title 非空、content 非空等）
3. 创建 Guide 实体，状态为 DRAFT 或 PUBLISHED
4. 设置 authorId、coverUrl、imageUrls
5. 保存到数据库
6. 返回 GuideResponse

**关键代码位置：** `GuideController.createGuide()` → `GuideServiceImpl.createGuide()`

---

### 2. 攻略列表 (`GET /api/v1/guides`)

**流程：**

1. 接收分页参数和筛选参数（destinationId、authorId、keyword）
2. 排序参数（sortBy: create_time/view_count/like_count）
3. 仅返回 status=PUBLISHED 的攻略
4. 构建 LambdaQueryWrapper
5. 执行分页查询
6. 转换为 GuideListResponse
7. 返回分页结果

**关键代码位置：** `GuideController.listGuides()` → `GuideServiceImpl.listGuides()`

---

### 3. 攻略详情 (`GET /api/v1/guides/{id}`)

**流程：**

1. 根据 ID 查询攻略
2. 状态必须为 PUBLISHED，否则返回 404
3. 浏览量 +1（原子更新 viewCount）
4. 转换为 GuideResponse
5. 返回

**关键代码位置：** `GuideController.getDetail()` → `GuideServiceImpl.getDetail()`

---

### 4. 编辑/删除攻略 (`PUT/DELETE /api/v1/guides/{id}`)

**流程：**

1. 根据 ID 查询攻略
2. 校验权限：当前 userId 必须等于 authorId（或 ADMIN）
3. PUT：更新 title/content/coverUrl/imageUrls/status 等字段
4. DELETE：执行删除操作

**关键代码位置：** `GuideController.updateGuide()` / `GuideController.deleteGuide()`

---

### 5. 攻略点赞 (`POST/DELETE /api/v1/guides/{id}/like`)

**流程（POST）：**

1. 查询攻略是否存在
2. 原子更新 likeCount +1
3. 返回成功

**流程（DELETE）：**

1. 原子更新 likeCount -1（不低于 0）
2. 返回成功

**设计要点：**
- MVP 阶段不做用户粒度的去重（可重复点赞），后续加 `guide_like` 关联表实现幂等

---

## 💬 六、评论模块

### 1. 评论列表 (`GET /api/v1/guides/{id}/comments`)

**流程：**

1. 查询攻略是否存在
2. 根据 guideId 查询所有评论
3. 按 parentId 分组构建树形结构（parentId=null 为根评论）
4. 转换为 CommentTreeResponse
5. 返回

**关键代码位置：** `CommentController.listComments()` → `CommentServiceImpl.listComments()`

---

### 2. 发表评论 (`POST /api/v1/guides/{id}/comments`)

**流程：**

1. 从 @AuthenticationPrincipal 获取 userId
2. 校验攻略是否存在
3. 如果是回复（parentId != null），校验父评论是否存在且属于该攻略
4. 创建 Comment 实体
5. 保存到数据库
6. 返回 CommentResponse

**关键代码位置：** `CommentController.createComment()` → `CommentServiceImpl.createComment()`

---

### 3. 删除评论 (`DELETE /api/v1/comments/{id}`)

**流程：**

1. 根据 ID 查询评论
2. 校验权限：当前 userId 必须等于评论的 userId（或 ADMIN）
3. 删除评论（级联删除子回复）
4. 返回成功

**关键代码位置：** `CommentController.deleteComment()` → `CommentServiceImpl.deleteComment()`

---

## 📤 七、图片上传模块

### 1. 上传图片 (`POST /api/v1/uploads/image`)

**流程：**

1. 接收 MultipartFile 文件
2. 校验：文件大小 ≤ 5MB、文件头校验（jpg/png/webp）
3. 调用 `ImageStorageService.upload()`
4. MVP 阶段：保存到 `uploads/images/` 目录，按日期分目录（如 `2026/05/18/xxx.jpg`）
5. 返回可访问 URL：`{ "url": "/uploads/images/2026/05/18/xxx.jpg" }`

**关键代码位置：** `UploadController.uploadImage()` → `LocalStorageServiceImpl.upload()`

---

### 2. 批量上传 (`POST /api/v1/uploads/images`)

**流程：**

1. 接收多个 MultipartFile
2. 逐个调用 `ImageStorageService.upload()`
3. 返回 URL 数组：`{ "urls": ["url1", "url2", ...] }`

---

### 3. 删除图片 (`DELETE /api/v1/uploads/{filename}`)

**流程：**

1. 校验 filename 格式（防止路径遍历攻击）
2. 调用 `ImageStorageService.delete()`
3. 从本地目录删除文件
4. 返回成功

---

## 🗃️ 八、ImageStorageService 抽象设计

```java
/**
 * 图片存储服务接口抽象。
 * MVP 阶段使用 LocalStorageServiceImpl（本地文件系统）。
 * 后续切换 OssStorageServiceImpl（阿里云 OSS），业务代码无需修改。
 */
public interface ImageStorageService {
    
    /**
     * 上传图片，返回可访问 URL。
     */
    String upload(InputStream inputStream, String originalFilename);
    
    /**
     * 删除图片。
     */
    void delete(String url);
}
```

### LocalStorageServiceImpl（MVP 实现）

- 存储路径：`{项目根目录}/uploads/images/{year}/{month}/{day}/{UUID}.{ext}`
- 访问 URL：通过 `WebConfig.addResourceHandlers()` 注册静态资源映射
- 文件名：使用 UUID 避免冲突
- 目录结构：按日期分层，避免单目录文件过多

---

## 🔒 九、权限设计

| 接口 | 认证 | 权限 |
|------|------|------|
| 目的地列表/详情 | 无需 | 公开 |
| 景点列表/详情 | 无需 | 公开 |
| 攻略列表/详情 | 无需 | 公开（仅 PUBLISHED） |
| 攻略发布/编辑/删除 | 需要 JWT | 仅作者或 ADMIN |
| 评论发表/删除 | 需要 JWT | 仅评论作者或 ADMIN |
| 点赞 | 需要 JWT | 已登录用户 |
| 图片上传 | 需要 JWT | 已登录用户 |
| 管理端 CRUD | 需要 JWT + ADMIN | 仅 ADMIN |

**权限检查实现：**
- 攻略/评论的编辑/删除：Service 层校验 `userId == authorId`，不等则抛 `AuthenticationException`
- 管理端接口：通过 Spring Security 配置 `/api/v1/admin/**` 需要 ADMIN 角色

---

## 🛡️ 十、异常处理

复用 `com.ctrip.common.exception.*` 中的异常类型：

| 异常类 | 使用场景 |
|--------|---------|
| ResourceNotFoundException | 目的地/景点/攻略/评论不存在 |
| DuplicateResourceException | 同一目的地+景点名称重复（管理端） |
| AuthenticationException | 无权操作他人资源 |
| BusinessException | 图片格式不支持、文件过大 |

全局异常处理器 `GlobalExceptionHandler` 自动转换为统一 ApiResponse 格式。

---

## 🔄 十一、事务管理

**读操作：**
```java
@Transactional(readOnly = true)
public DestinationResponse getDetail(Long id) { ... }
```

**写操作：**
```java
@Transactional
public GuideResponse createGuide(Long authorId, CreateGuideRequest request) { ... }
```

**事务边界：**
- Service 层方法级别
- 评论删除 + 级联子评论删除 在同一事务

---

## 📦 十二、DTO 转换层

每个实体对应一个 Converter 类，遵循与 `UserConverter` 相同的设计原则：

- 静态方法，无状态，不注册为 Spring Bean
- Entity → Response DTO 的映射逻辑集中管理
- Service 层不直接操作字段映射

```java
public class DestinationConverter {
    private DestinationConverter() {}  // 禁止实例化
    
    public static DestinationResponse toResponse(Destination entity) { ... }
}
```

---

## 📊 十三、分页查询模式

使用 MyBatis Plus 的 `Page` 对象 + `LambdaQueryWrapper`：

```java
// Service 层分页查询
public Page<Guide> listGuides(int page, int limit, Long destinationId, String keyword, String sortBy) {
    Page<Guide> pageParam = new Page<>(page, limit);
    
    LambdaQueryWrapper<Guide> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Guide::getStatus, GuideStatus.PUBLISHED);
    
    if (destinationId != null) {
        wrapper.eq(Guide::getDestinationId, destinationId);
    }
    if (keyword != null && !keyword.isBlank()) {
        wrapper.like(Guide::getTitle, keyword);
    }
    
    // 排序
    if ("view_count".equals(sortBy)) {
        wrapper.orderByDesc(Guide::getViewCount);
    } else {
        wrapper.orderByDesc(Guide::getCreatedAt);
    }
    
    return guideMapper.selectPage(pageParam, wrapper);
}
```

---

## 📊 十四、API 端点汇总

### 目的地（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/destinations` | 目的地列表（分页、筛选） |
| GET | `/api/v1/destinations/{id}` | 目的地详情（含景点列表） |

### 目的地（管理端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/destinations` | 管理端列表 |
| POST | `/api/v1/admin/destinations` | 创建目的地 |
| PUT | `/api/v1/admin/destinations/{id}` | 更新目的地 |
| DELETE | `/api/v1/admin/destinations/{id}` | 删除目的地 |

### 景点（公开）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/attractions` | 景点列表（分页、筛选） |
| GET | `/api/v1/attractions/{id}` | 景点详情 |

### 景点（管理端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/attractions` | 管理端列表 |
| POST | `/api/v1/admin/attractions` | 创建景点 |
| PUT | `/api/v1/admin/attractions/{id}` | 更新景点 |
| DELETE | `/api/v1/admin/attractions/{id}` | 删除景点 |

### 攻略

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/guides` | 攻略列表（分页、筛选、排序） |
| GET | `/api/v1/guides/{id}` | 攻略详情 |
| POST | `/api/v1/guides` | 发布攻略（需 JWT） |
| PUT | `/api/v1/guides/{id}` | 编辑攻略（需 JWT，仅作者） |
| DELETE | `/api/v1/guides/{id}` | 删除攻略（需 JWT，仅作者） |
| POST | `/api/v1/guides/{id}/like` | 点赞（需 JWT） |
| DELETE | `/api/v1/guides/{id}/like` | 取消点赞（需 JWT） |

### 评论

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/guides/{id}/comments` | 评论列表（树形） |
| POST | `/api/v1/guides/{id}/comments` | 发表评论（需 JWT，支持回复） |
| DELETE | `/api/v1/comments/{id}` | 删除评论（需 JWT，仅作者） |

### 图片上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/uploads/image` | 单张图片上传 |
| POST | `/api/v1/uploads/images` | 批量图片上传 |
| DELETE | `/api/v1/uploads/{filename}` | 删除图片 |

---

## 💡 十五、设计模式与最佳实践

### 1. 依赖注入

**方式：** 构造器注入（与用户模块一致）

### 2. 单一职责原则

- **Controller**：请求解析、参数校验、调用 Service、包装 ApiResponse
- **Service**：业务逻辑、事务控制、调用 Mapper
- **Mapper**：数据访问（MyBatis Plus CRUD）
- **Converter**：Entity ↔ DTO 转换

### 3. 图片关联方案（方案 A）

- 业务表使用 `coverUrl`（单张封面）+ `imageUrls`（JSON 数组存多图）
- 不建独立图片表，图片是业务实体的附属资源
- 上传接口 `ImageStorageService` 抽象化，后续可切换 OSS

### 4. 评论树形结构

- 使用 `parentId` 自关联实现回复
- `parentId = null` 为根评论，`parentId != null` 为回复
- Service 层按 guideId 查询后，内存中按 parentId 分组构建树

---

## 📝 十六、开发顺序建议

按依赖关系从底层开始：

```
1. 基础设施：Destination/Attraction 实体 + Mapper + Service + Controller
2. 图片上传：ImageStorageService 接口 + LocalStorageServiceImpl + UploadController
3. 攻略社区：Guide 实体 + Service + Controller + Converter
4. 评论模块：Comment 实体 + Service + Controller + 树形构建
5. 互动功能：点赞
```

---

## ✅ 十七、总结

### 模块特点

| 维度 | 说明 |
|------|------|
| 架构 | 与用户模块一致的三层架构，遵循相同编码规范 |
| 数据 | MyBatis Plus + LambdaQueryWrapper，无 XML |
| 图片 | `ImageStorageService` 接口抽象，MVP 本地存储，后续切换 OSS |
| 图片关联 | 方案 A（coverUrl + imageUrls JSON 数组），不建独立表 |
| 安全 | 复用现有异常体系、全局异常处理、JWT 认证 |
| 事务 | Service 层方法级别，读写分离 |
| 测试 | 单元测试（Mockito）+ 集成测试（H2） |

### 与用户模块的一致性

- 相同的包结构、命名规范
- 相同的 DTO（record）、Converter、Service 接口 + Impl 模式
- 相同的 Controller 返回格式（`ResponseEntity<ApiResponse<T>>`）
- 相同的异常处理（抛出 common 异常，由 GlobalExceptionHandler 统一处理）
- 相同的事务策略（读 `@Transactional(readOnly = true)`，写 `@Transactional`）
