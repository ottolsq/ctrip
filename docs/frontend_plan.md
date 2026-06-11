# 捷程旅行网前端开发规划

> 基于后端 API 文档，制定前端开发计划和页面结构

---

## 一、项目概述

### 1.1 技术栈

- **框架**: Vue 3.5.13
- **构建工具**: Vite 6.0.5
- **路由**: vue-router 4.5.0
- **状态管理**: Pinia 2.3.0
- **HTTP客户端**: Axios 1.7.9
- **UI组件库**: Element Plus 2.9.1 + @element-plus/icons-vue 2.3.1

### 1.2 项目定位

轻量级旅游 Web 平台，核心功能：
- 攻略内容社区（轻社区）
- 行程规划
- 旅游盲盒（核心特色）

### 1.3 目标用户

年轻人、学生、自由行爱好者、性价比用户

---

## 二、后端 API 模块总结

### 2.1 已完成模块

#### Phase 1: 用户模块 ✅

**认证接口**:
- `POST /api/v1/auth/register/phone` - 手机号注册
- `POST /api/v1/auth/register/email` - 邮箱注册
- `POST /api/v1/auth/login` - 登录
- `POST /api/v1/auth/refresh` - 刷新令牌
- `POST /api/v1/auth/logout` - 登出
- `POST /api/v1/auth/password/forgot` - 忘记密码
- `POST /api/v1/auth/password/reset` - 重置密码
- `POST /api/v1/auth/sms/send` - 发送短信验证码

**用户接口**:
- `GET /api/v1/users/me` - 获取个人资料
- `PUT /api/v1/users/me` - 更新个人资料
- `PUT /api/v1/users/me/password` - 修改密码
- `PUT /api/v1/users/me/avatar` - 更新头像

**认证机制**: JWT (HS256)
- Access Token: 15分钟
- Refresh Token: 15天

### 2.2 内容模块（Phase 2）

**目的地接口**:
- `GET /api/v1/destinations` - 目的地列表（分页、筛选）
- `GET /api/v1/destinations/{id}` - 目的地详情

**景点接口**:
- `GET /api/v1/attractions` - 景点列表
- `GET /api/v1/attractions/{id}` - 景点详情

**攻略接口**:
- `GET /api/v1/guides` - 攻略列表
- `GET /api/v1/guides/{id}` - 攻略详情
- `POST /api/v1/guides` - 发布攻略
- `PUT /api/v1/guides/{id}` - 编辑攻略
- `DELETE /api/v1/guides/{id}` - 删除攻略
- `POST /api/v1/guides/{id}/like` - 点赞攻略
- `DELETE /api/v1/guides/{id}/like` - 取消点赞

**评论接口**:
- `GET /api/v1/guides/{guideId}/comments` - 评论列表（树形结构）
- `POST /api/v1/guides/{guideId}/comments` - 发表评论
- `DELETE /api/v1/guides/{guideId}/comments/{commentId}` - 删除评论

**上传接口**:
- `POST /api/v1/uploads/image` - 上传图片
- `POST /api/v1/uploads/images` - 批量上传
- `DELETE /api/v1/uploads/{filename}` - 删除图片

**管理端接口**（需要 ADMIN/CONTENT_OPERATOR 角色）:
- `POST /api/v1/admin/destinations` - 创建目的地
- `PUT /api/v1/admin/destinations/{id}` - 更新目的地
- `DELETE /api/v1/admin/destinations/{id}` - 删除目的地
- `POST /api/v1/admin/attractions` - 创建景点
- `PUT /api/v1/admin/attractions/{id}` - 更新景点
- `DELETE /api/v1/admin/attractions/{id}` - 删除景点

### 2.3 行程模块（Phase 3）

**行程接口**:
- `POST /api/v1/itineraries` - 创建行程
- `GET /api/v1/itineraries` - 我的行程列表
- `GET /api/v1/itineraries/{id}` - 行程详情
- `PUT /api/v1/itineraries/{id}` - 编辑行程
- `DELETE /api/v1/itineraries/{id}` - 删除行程

**日程接口**:
- `POST /api/v1/itineraries/{id}/days` - 添加日程
- `PUT /api/v1/itineraries/days/{dayId}` - 编辑日程
- `DELETE /api/v1/itineraries/days/{dayId}` - 删除日程

**行程项接口**:
- `POST /api/v1/itineraries/{id}/days/{dayId}/items` - 添加行程项
- `PUT /api/v1/itineraries/items/{itemId}` - 编辑行程项
- `DELETE /api/v1/itineraries/items/{itemId}` - 删除行程项
- `POST /api/v1/itineraries/days/{dayId}/items/reorder` - 批量排序

**收藏接口**:
- `POST /api/v1/itineraries/{id}/collection?add=true` - 收藏行程
- `POST /api/v1/itineraries/{id}/collection?add=false` - 取消收藏

**分享接口**:
- `POST /api/v1/itineraries/{id}/share` - 生成分享链接
- `GET /api/v1/share/{shareCode}` - 查看分享行程（公开）
- `DELETE /api/v1/itineraries/{id}/share` - 取消分享

### 2.4 盲盒模块（Phase 4）

**盲盒模板接口**:
- `GET /api/v1/blind-box` - 盲盒模板列表
- `GET /api/v1/blind-box/{id}` - 模板详情
- `GET /api/v1/blind-box/my` - 我的盲盒列表

**订单接口**:
- `POST /api/v1/blind-box/orders` - 创建订单
- `GET /api/v1/blind-box/orders` - 我的订单列表
- `GET /api/v1/blind-box/orders/{orderNo}` - 订单详情
- `POST /api/v1/blind-box/orders/{orderNo}/cancel` - 取消订单

**支付接口**:
- `POST /api/v1/payments/blind-box/callback` - 模拟支付回调

**开盒接口**:
- `POST /api/v1/blind-box/orders/{orderNo}/open` - 开盒

**结果接口**:
- `GET /api/v1/blind-box/orders/{orderNo}/result` - 结果详情

**分享接口**:
- `POST /api/v1/blind-box/orders/{orderNo}/share-result` - 生成分享链接
- `GET /api/v1/blind-box/share/{shareCode}` - 查看分享结果（公开）

**管理端接口**（需要 ADMIN 角色）:
- `GET /api/v1/admin/blind-box/templates` - 模板列表
- `POST /api/v1/admin/blind-box/templates` - 创建模板
- `PUT /api/v1/admin/blind-box/templates/{id}` - 更新模板
- `DELETE /api/v1/admin/blind-box/templates/{id}` - 删除模板
- `GET /api/v1/admin/blind-box/orders` - 全部订单列表

---

## 三、前端页面规划

### 3.1 页面清单

#### 公共页面
| 页面 | 路由 | 说明 | 优先级 |
|------|------|------|--------|
| 首页 | `/` | 轮播Banner、热门目的地、攻略精选、盲盒入口 | P0 |
| 登录 | `/login` | 手机号/邮箱+密码登录 | P0 |
| 注册 | `/register` | 手机号/邮箱注册 | P0 |
| 忘记密码 | `/forgot-password` | 找回密码 | P1 |

#### 目的地与攻略页面
| 页面 | 路由 | 说明 | 优先级 |
|------|------|------|--------|
| 目的地列表 | `/destinations` | 分页、筛选、搜索 | P0 |
| 目的地详情 | `/destinations/:id` | 基础信息、景点列表、相关攻略 | P0 |
| 攻略列表 | `/guides` | 分页、筛选、排序 | P0 |
| 攻略详情 | `/guides/:id` | 内容、评论、点赞、收藏 | P0 |
| 发布攻略 | `/guides/create` | 富文本编辑器、图片上传 | P1 |
| 编辑攻略 | `/guides/:id/edit` | 编辑已有攻略 | P1 |

#### 行程页面
| 页面 | 路由 | 说明 | 优先级 |
|------|------|------|--------|
| 我的行程 | `/itineraries` | 行程列表、状态筛选 | P0 |
| 行程详情 | `/itineraries/:id` | 日程视图、行程项列表 | P0 |
| 创建行程 | `/itineraries/create` | 表单创建 | P0 |
| 编辑行程 | `/itineraries/:id/edit` | 拖拽排序、编辑 | P1 |
| 分享行程 | `/share/:code` | 只读查看分享 | P1 |

#### 盲盒页面
| 页面 | 路由 | 说明 | 优先级 |
|------|------|------|--------|
| 盲盒专区 | `/blind-box` | 盲盒列表、类型筛选 | P0 |
| 模板详情 | `/blind-box/:id` | 规则说明、购买入口 | P0 |
| 创建订单 | `/blind-box/:id/order` | 填写预选参数 | P0 |
| 支付页面 | `/blind-box/orders/:orderNo/pay` | 模拟支付 | P0 |
| 我的盲盒 | `/blind-box/my` | 购买记录、开盒记录 | P0 |
| 开盒页面 | `/blind-box/orders/:orderNo/open` | 开盒动画、结果展示 | P0 |
| 结果详情 | `/blind-box/orders/:orderNo/result` | 完整旅行方案 | P0 |
| 分享结果 | `/blind-box/share/:code` | 只读查看分享 | P1 |

#### 用户中心页面
| 页面 | 路由 | 说明 | 优先级 |
|------|------|------|--------|
| 个人资料 | `/user/profile` | 查看/编辑资料 | P0 |
| 我的收藏 | `/user/collections` | 攻略、行程收藏 | P1 |
| 我的订单 | `/user/orders` | 盲盒订单列表 | P1 |
| 我的攻略 | `/user/guides` | 我发布的攻略 | P1 |
| 修改密码 | `/user/password` | 修改密码表单 | P2 |

### 3.2 页面优先级说明

- **P0**: MVP 必须实现，核心功能页面
- **P1**: MVP 后补充，提升用户体验
- **P2**: 后续迭代，非核心功能

---

## 四、前端目录结构规划

```
src/
├── api/                    # API 接口封装
│   ├── auth.js            # 认证相关接口
│   ├── user.js            # 用户相关接口
│   ├── destination.js     # 目的地接口
│   ├── attraction.js      # 景点接口
│   ├── guide.js           # 攻略接口
│   ├── comment.js         # 评论接口
│   ├── upload.js          # 上传接口
│   ├── itinerary.js       # 行程接口
│   ├── blindbox.js        # 盲盒接口
│   └── collection.js      # 收藏接口
│
├── assets/                # 静态资源
│   ├── css/              # 样式文件（已创建）
│   ├── images/           # 图片资源
│   └── icons/            # 图标资源
│
├── components/            # 公共组件
│   ├── common/           # 通用组件
│   │   ├── AppHeader.vue     # 顶部导航
│   │   ├── AppFooter.vue     # 底部导航
│   │   ├── Pagination.vue    # 分页组件
│   │   └── Loading.vue       # 加载组件
│   ├── destination/      # 目的地相关组件
│   ├── guide/            # 攻略相关组件
│   ├── itinerary/        # 行程相关组件
│   └── blindbox/         # 盲盒相关组件
│
├── composables/           # 组合式函数
│   ├── useAuth.js        # 认证逻辑
│   ├── usePagination.js  # 分页逻辑
│   └── useUpload.js      # 上传逻辑
│
├── router/               # 路由配置
│   └── index.js
│
├── stores/               # 状态管理
│   ├── user.js          # 用户状态（已创建）
│   ├── destination.js   # 目的地状态
│   ├── guide.js         # 攻略状态
│   ├── itinerary.js     # 行程状态
│   └── blindbox.js      # 盲盒状态
│
├── utils/                # 工具函数
│   ├── request.js       # Axios 封装（已创建）
│   ├── storage.js       # 本地存储
│   ├── format.js        # 格式化工具
│   └── validate.js      # 表单验证
│
├── views/                # 页面组件
│   ├── HomeView.vue     # 首页（已创建）
│   ├── AboutView.vue    # 关于页（已创建）
│   ├── auth/            # 认证页面
│   │   ├── LoginView.vue
│   │   ├── RegisterView.vue
│   │   └── ForgotPasswordView.vue
│   ├── destination/     # 目的地页面
│   │   ├── DestinationList.vue
│   │   └── DestinationDetail.vue
│   ├── guide/           # 攻略页面
│   │   ├── GuideList.vue
│   │   ├── GuideDetail.vue
│   │   └── GuideEditor.vue
│   ├── itinerary/       # 行程页面
│   │   ├── ItineraryList.vue
│   │   ├── ItineraryDetail.vue
│   │   └── ItineraryEditor.vue
│   ├── blindbox/        # 盲盒页面
│   │   ├── BlindBoxList.vue
│   │   ├── BlindBoxDetail.vue
│   │   ├── OrderCreate.vue
│   │   ├── OrderPay.vue
│   │   ├── MyBlindBox.vue
│   │   ├── OpenBox.vue
│   │   └── ResultDetail.vue
│   └── user/            # 用户中心页面
│       ├── ProfileView.vue
│       ├── CollectionsView.vue
│       ├── OrdersView.vue
│       └── MyGuidesView.vue
│
├── App.vue              # 根组件（已创建）
└── main.js              # 入口文件（已创建）
```

---

## 五、开发阶段规划

### Phase 1: 基础框架搭建（1-2天）✅ 已完成

**任务清单**:
- [x] 确定 UI 组件库 → Element Plus
- [x] 完善 Axios 封装（请求/响应拦截器、Token自动刷新、错误处理）
- [x] 实现 Token 自动刷新机制（请求队列、并发刷新）
- [x] 创建公共组件（AppHeader、AppFooter、PlaceholderView）
- [x] 配置路由守卫（登录状态检查、页面标题、guest重定向）
- [x] 实现基础布局（顶部导航 + 内容区 + 底部）
- [x] 创建工具函数（storage.js、format.js、validate.js）
- [x] 创建 API 模块（auth.js、user.js）
- [x] 完善用户状态管理（Pinia store + refreshToken）
- [x] 修复 vite proxy 配置

**交付物**:
- 完整的请求封装
- 公共组件
- 路由配置
- 基础布局

### Phase 2: 用户模块（2-3天）✅ 已完成

**任务清单**:
- [x] 登录页面（手机号/邮箱切换、记住我、表单验证）
- [x] 注册页面（手机号/邮箱注册、验证码倒计时、密码强度校验）
- [x] 忘记密码页面（步骤条引导、手机验证 + 重置密码）
- [x] 个人资料页面（编辑资料、头像上传、用户中心侧边栏）
- [x] 修改密码页面（旧密码验证、新密码强度校验）
- [x] 用户状态管理（Pinia store + Token管理）
- [x] 提取 UserSidebar 共享组件

**交付物**:
- 完整的用户认证流程
- 用户中心基础功能

### Phase 3: 内容模块（3-4天）

**任务清单**:
- [ ] 目的地列表页面
- [ ] 目的地详情页面
- [ ] 攻略列表页面
- [ ] 攻略详情页面
- [ ] 发布/编辑攻略页面（富文本编辑器）
- [ ] 评论组件（树形展示）
- [ ] 图片上传组件
- [ ] 点赞/收藏功能

**交付物**:
- 完整的攻略社区功能
- 目的地浏览功能

### Phase 4: 行程模块（3-4天）

**任务清单**:
- [ ] 行程列表页面
- [ ] 行程详情页面（日程视图）
- [ ] 创建/编辑行程页面
- [ ] 拖拽排序功能（行程项排序）
- [ ] 分享功能
- [ ] 收藏功能

**交付物**:
- 完整的行程规划功能
- 分享查看功能

### Phase 5: 盲盒模块（4-5天）

**任务清单**:
- [ ] 盲盒专区页面
- [ ] 模板详情页面
- [ ] 创建订单页面（预选参数）
- [ ] 支付页面（模拟支付）
- [ ] 开盒页面（动画效果）
- [ ] 结果详情页面
- [ ] 我的盲盒页面
- [ ] 分享功能

**交付物**:
- 完整的盲盒购买流程
- 开盒体验
- 结果分享

### Phase 6: 用户中心完善（2天）

**任务清单**:
- [ ] 我的收藏页面
- [ ] 我的订单页面
- [ ] 我的攻略页面
- [ ] 消息通知（后续）

**交付物**:
- 完整的用户中心

### Phase 7: 首页与优化（2-3天）

**任务清单**:
- [ ] 首页设计（轮播Banner、热门目的地、攻略精选）
- [ ] 全局搜索功能
- [ ] 性能优化（懒加载、缓存）
- [ ] 响应式适配
- [ ] 错误处理优化

**交付物**:
- 完整的首页
- 优化后的整体体验

---

## 六、技术选型建议

### 6.1 UI 组件库

**推荐**: Element Plus

**理由**:
- 完善的组件生态
- 详细的中文文档
- 适合中后台系统
- 支持按需引入

**安装**:
```bash
npm install element-plus
```

### 6.2 富文本编辑器

**推荐**: WangEditor 或 TinyMCE

**理由**:
- 支持 Markdown 格式
- 图片上传集成
- 易于定制

### 6.3 拖拽排序

**推荐**: vuedraggable (SortableJS)

**用途**: 行程项拖拽排序

### 6.4 图表库（可选）

**推荐**: ECharts

**用途**: 数据统计、预算展示

---

## 七、关键功能实现方案

### 7.1 Token 自动刷新

```javascript
// utils/request.js
request.interceptors.response.use(
  response => response.data,
  async error => {
    if (error.response?.status === 401) {
      // Token 过期，尝试刷新
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post('/api/v1/auth/refresh', { refreshToken })
          localStorage.setItem('token', res.data.accessToken)
          localStorage.setItem('refreshToken', res.data.refreshToken)
          // 重试原请求
          error.config.headers.Authorization = `Bearer ${res.data.accessToken}`
          return request(error.config)
        } catch (e) {
          // 刷新失败，跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)
```

### 7.2 图片上传

```vue
<template>
  <el-upload
    action="/api/v1/uploads/image"
    :headers="uploadHeaders"
    :on-success="handleSuccess"
    list-type="picture-card"
  >
    <el-icon><Plus /></el-icon>
  </el-upload>
</template>

<script setup>
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const handleSuccess = (response) => {
  console.log('上传成功:', response.data.url)
}
</script>
```

### 7.3 拖拽排序

```vue
<template>
  <draggable v-model="items" item-key="id" @end="handleReorder">
    <template #item="{ element }">
      <div class="item">{{ element.name }}</div>
    </template>
  </draggable>
</template>

<script setup>
import draggable from 'vuedraggable'
import { ref } from 'vue'

const items = ref([])

const handleReorder = () => {
  const itemIds = items.value.map(item => item.id)
  // 调用批量排序接口
  api.itinerary.reorderItems(dayId, { itemIds })
}
</script>
```

### 7.4 开盒动画

```vue
<template>
  <div class="blind-box" @click="openBox">
    <div class="box" :class="{ opened: isOpened }">
      <div class="lid"></div>
      <div class="content">
        <div v-if="isOpened" class="result">
          <h2>{{ result.destination }}</h2>
          <p>{{ result.theme }} · {{ result.days }}天</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const isOpened = ref(false)
const result = ref(null)

const openBox = async () => {
  const res = await api.blindbox.openBox(orderNo)
  result.value = res.data
  isOpened.value = true
}
</script>

<style scoped>
.box {
  transition: transform 0.5s;
}
.box.opened {
  transform: rotateY(180deg);
}
</style>
```

---

## 八、注意事项

### 8.1 认证与权限

- 所有需要登录的接口必须在请求头携带 `Authorization: Bearer <token>`
- 管理端接口需要 ADMIN 或 CONTENT_OPERATOR 角色
- Token 过期后自动刷新，刷新失败跳转登录页

### 8.2 数据格式

- 后端统一返回格式: `{ success: boolean, data: any, error: string }`
- 分页格式: `{ records: [], total: number, size: number, current: number, pages: number }`
- 日期格式: ISO 8601 (`YYYY-MM-DDTHH:mm:ss`)

### 8.3 错误处理

- 400: 参数错误，显示具体错误信息
- 401: 未登录或 Token 过期，跳转登录
- 403: 权限不足，提示用户
- 404: 资源不存在，显示空状态
- 500: 服务器错误，提示稍后重试

### 8.4 图片处理

- 封面图: `coverUrl` (单张)
- 多图: `imageUrls` (JSON 数组字符串，需解析)
- 上传后返回相对路径: `/uploads/images/xxx.jpg`
- 完整 URL: `http://localhost:8080/uploads/images/xxx.jpg`

### 8.5 暂不实现功能

以下功能后端 MVP 阶段暂不实现，前端也暂不开发:
- AI 图片分析
- AI 结果图生成
- 结果图下载
- 真实支付（使用模拟支付）

---

## 九、开发顺序建议

### 推荐顺序

1. **基础框架** → 2. **用户模块** → 3. **内容模块** → 4. **行程模块** → 5. **盲盒模块** → 6. **用户中心** → 7. **首页**

**理由**:
- 先完成用户认证，后续页面都需要登录状态
- 内容模块是基础，盲盒模块依赖行程模块
- 首页放在最后，因为需要聚合各模块内容

### 时间估算

- Phase 1: 1-2天
- Phase 2: 2-3天
- Phase 3: 3-4天
- Phase 4: 3-4天
- Phase 5: 4-5天
- Phase 6: 2天
- Phase 7: 2-3天

**总计**: 17-23天（约 3-4 周）

---

## 十、下一步行动

### 立即开始

1. **确定 UI 组件库**
   - 建议: Element Plus
   - 安装并配置

2. **完善请求封装**
   - 添加 Token 自动刷新
   - 完善错误处理

3. **创建公共组件**
   - AppHeader.vue
   - AppFooter.vue
   - Pagination.vue

4. **开始用户模块**
   - 登录页面
   - 注册页面
   - 用户状态管理

### 已确认

1. UI 组件库：**Element Plus** ✅
2. 富文本编辑器选择：WangEditor 还是 TinyMCE？（Phase 3 确认）
3. 是否需要移动端适配？（后续确认）
4. 图片存储方案：本地存储还是 OSS？（后续确认）

---

## 附录：API 端点快速参考

### 认证相关
```
POST /api/v1/auth/register/phone    # 手机号注册
POST /api/v1/auth/register/email    # 邮箱注册
POST /api/v1/auth/login             # 登录
POST /api/v1/auth/refresh           # 刷新令牌
POST /api/v1/auth/logout            # 登出
```

### 用户相关
```
GET  /api/v1/users/me               # 获取资料
PUT  /api/v1/users/me               # 更新资料
PUT  /api/v1/users/me/password      # 修改密码
PUT  /api/v1/users/me/avatar        # 更新头像
```

### 内容相关
```
GET  /api/v1/destinations           # 目的地列表
GET  /api/v1/destinations/:id       # 目的地详情
GET  /api/v1/attractions            # 景点列表
GET  /api/v1/attractions/:id        # 景点详情
GET  /api/v1/guides                 # 攻略列表
GET  /api/v1/guides/:id             # 攻略详情
POST /api/v1/guides                 # 发布攻略
```

### 行程相关
```
POST /api/v1/itineraries            # 创建行程
GET  /api/v1/itineraries            # 我的行程
GET  /api/v1/itineraries/:id        # 行程详情
POST /api/v1/itineraries/:id/share  # 分享行程
GET  /api/v1/share/:code            # 查看分享
```

### 盲盒相关
```
GET  /api/v1/blind-box              # 盲盒列表
POST /api/v1/blind-box/orders       # 创建订单
POST /api/v1/payments/callback      # 支付回调
POST /api/v1/blind-box/orders/:orderNo/open    # 开盒
GET  /api/v1/blind-box/orders/:orderNo/result  # 结果详情
```
