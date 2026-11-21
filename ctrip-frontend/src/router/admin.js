const adminRoutes = [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
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
          roles: ['ADMIN', 'CONTENT_OPERATOR'],
          title: '数据概览'
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
