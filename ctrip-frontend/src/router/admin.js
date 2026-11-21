import { useUserStore } from '@/stores/user'
import { adminHomePath } from '@/utils/businessRules'

const adminRoutes = [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    // 内容运营无权访问数据概览，落到其可用的攻略审核页
    redirect: () => adminHomePath(useUserStore().isAdmin),
    meta: {
      adminLayout: true,
      requiresAuth: true,
      roles: ['ADMIN', 'CONTENT_OPERATOR'],
      title: '后台管理'
    },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '数据概览'
        }
      },
      {
        path: 'guides',
        name: 'AdminGuideAudit',
        component: () => import('@/views/admin/GuideAuditView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'CONTENT_OPERATOR'],
          title: '攻略审核'
        }
      },
      {
        path: 'comments',
        name: 'AdminCommentManage',
        component: () => import('@/views/admin/CommentManageView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'CONTENT_OPERATOR'],
          title: '评论管理'
        }
      },
      {
        path: 'content-stats',
        name: 'AdminContentStats',
        component: () => import('@/views/admin/ContentStatsView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN', 'CONTENT_OPERATOR'],
          title: '内容数据'
        }
      },
      {
        path: 'blind-box/templates',
        name: 'AdminBlindBoxTemplate',
        component: () => import('@/views/admin/BlindBoxTemplateView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '盲盒模板'
        }
      },
      {
        path: 'blind-box/orders',
        name: 'AdminBlindBoxOrder',
        component: () => import('@/views/admin/OrderManageView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '订单管理'
        }
      },
      {
        path: 'banners',
        name: 'AdminBannerManage',
        component: () => import('@/views/admin/BannerManageView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '轮播图'
        }
      },
      {
        path: 'recommendations',
        name: 'AdminRecommendationManage',
        component: () => import('@/views/admin/RecommendationManageView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '推荐位'
        }
      },
      {
        path: 'users',
        name: 'AdminUserManage',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          title: '用户管理'
        }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/admin/ForbiddenView.vue'),
    meta: { title: '无权访问' }
  }
]

export default adminRoutes
