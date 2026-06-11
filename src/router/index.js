import { createRouter, createWebHistory } from 'vue-router'
import { storage } from '@/utils/storage'

const routes = [
  // 公共页面
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/AboutView.vue'),
    meta: { title: '关于我们' }
  },

  // 认证页面（无需登录）
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '忘记密码', guest: true }
  },

  // 目的地页面
  {
    path: '/destinations',
    name: 'DestinationList',
    component: () => import('@/views/destination/DestinationList.vue'),
    meta: { title: '目的地' }
  },
  {
    path: '/destinations/:id',
    name: 'DestinationDetail',
    component: () => import('@/views/destination/DestinationDetail.vue'),
    meta: { title: '目的地详情' }
  },

  // 攻略页面
  {
    path: '/guides',
    name: 'GuideList',
    component: () => import('@/views/guide/GuideList.vue'),
    meta: { title: '攻略列表' }
  },
  {
    path: '/guides/create',
    name: 'GuideCreate',
    component: () => import('@/views/guide/GuideEditor.vue'),
    meta: { title: '发布攻略', requiresAuth: true }
  },
  {
    path: '/guides/:id',
    name: 'GuideDetail',
    component: () => import('@/views/guide/GuideDetail.vue'),
    meta: { title: '攻略详情' }
  },
  {
    path: '/guides/:id/edit',
    name: 'GuideEdit',
    component: () => import('@/views/guide/GuideEditor.vue'),
    meta: { title: '编辑攻略', requiresAuth: true }
  },

  // 行程页面
  {
    path: '/itineraries',
    name: 'ItineraryList',
    component: () => import('@/views/itinerary/ItineraryList.vue'),
    meta: { title: '我的行程', requiresAuth: true }
  },
  {
    path: '/itineraries/create',
    name: 'ItineraryCreate',
    component: () => import('@/views/itinerary/ItineraryEditor.vue'),
    meta: { title: '创建行程', requiresAuth: true }
  },
  {
    path: '/itineraries/:id',
    name: 'ItineraryDetail',
    component: () => import('@/views/itinerary/ItineraryDetail.vue'),
    meta: { title: '行程详情' }
  },
  {
    path: '/itineraries/:id/edit',
    name: 'ItineraryEdit',
    component: () => import('@/views/itinerary/ItineraryEditor.vue'),
    meta: { title: '编辑行程', requiresAuth: true }
  },
  {
    path: '/share/:code',
    name: 'ShareView',
    component: () => import('@/views/itinerary/ItineraryDetail.vue'),
    meta: { title: '分享行程', isShare: true }
  },

  // 盲盒页面
  {
    path: '/blind-box',
    name: 'BlindBoxList',
    component: () => import('@/views/blindbox/BlindBoxList.vue'),
    meta: { title: '盲盒专区' }
  },
  {
    path: '/blind-box/my',
    name: 'MyBlindBox',
    component: () => import('@/views/blindbox/MyBlindBox.vue'),
    meta: { title: '我的盲盒', requiresAuth: true }
  },
  {
    path: '/blind-box/:id',
    name: 'BlindBoxDetail',
    component: () => import('@/views/blindbox/BlindBoxDetail.vue'),
    meta: { title: '盲盒详情' }
  },
  {
    path: '/blind-box/:id/order',
    name: 'OrderCreate',
    component: () => import('@/views/blindbox/OrderCreate.vue'),
    meta: { title: '创建订单', requiresAuth: true }
  },
  {
    path: '/blind-box/orders/:orderNo/pay',
    name: 'OrderPay',
    component: () => import('@/views/blindbox/OrderPay.vue'),
    meta: { title: '支付订单', requiresAuth: true }
  },
  {
    path: '/blind-box/orders/:orderNo/open',
    name: 'OpenBox',
    component: () => import('@/views/blindbox/OpenBox.vue'),
    meta: { title: '开盒', requiresAuth: true }
  },
  {
    path: '/blind-box/orders/:orderNo/result',
    name: 'BlindBoxResult',
    component: () => import('@/views/blindbox/ResultDetail.vue'),
    meta: { title: '结果详情', requiresAuth: true }
  },
  {
    path: '/blind-box/share/:code',
    name: 'BlindBoxShare',
    component: () => import('@/views/blindbox/ResultDetail.vue'),
    meta: { title: '分享结果', isShare: true }
  },

  // 用户中心页面
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/ProfileView.vue'),
    meta: { title: '个人资料', requiresAuth: true }
  },
  {
    path: '/user/collections',
    name: 'UserCollections',
    component: () => import('@/views/user/CollectionsView.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/OrdersView.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/user/guides',
    name: 'UserGuides',
    component: () => import('@/views/user/MyGuidesView.vue'),
    meta: { title: '我的攻略', requiresAuth: true }
  },
  {
    path: '/user/password',
    name: 'UserPassword',
    component: () => import('@/views/user/PasswordView.vue'),
    meta: { title: '修改密码', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 捷程旅行网` : '捷程旅行网'

  const token = storage.getToken()

  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && token) {
    // 已登录用户访问登录/注册页，跳转到首页
    next('/')
  } else {
    next()
  }
})

export default router
