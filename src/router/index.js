import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/agent-chat',
    name: 'AgentChat',
    component: () => import('../views/AgentChat.vue'),
    meta: { title: 'AI学习助手' }
  },
  {
    path: '/chat',
    name: 'AiChat',
    component: () => import('../views/AiChat.vue'),
    meta: { title: '智能对话' }
  },
  {
    path: '/learning-paths',
    name: 'LearningPaths',
    component: () => import('../views/LearningPaths.vue'),
    meta: { title: '学习路径' }
  },
  {
    path: '/paths',
    name: 'LearningPath',
    component: () => import('../views/LearningPath.vue'),
    meta: { title: '学习路径' }
  },
  {
    path: '/resources',
    name: 'Resources',
    component: () => import('../views/Resources.vue'),
    meta: { title: '生成资源' }
  },
  {
    path: '/knowledge',
    name: 'KnowledgeGraph',
    component: () => import('../views/KnowledgeGraph.vue'),
    meta: { title: '知识图谱' }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('../views/ProfilePage.vue'),
    meta: { title: '用户画像' }
  },
  {
    path: '/videos',
    name: 'VideoList',
    component: () => import('../views/VideoList.vue'),
    meta: { title: '视频学习' }
  },
  {
    path: '/videos/:id',
    name: 'VideoDetail',
    component: () => import('../views/VideoDetail.vue'),
    meta: { title: '视频详情' }
  },
  {
    path: '/video/:id',
    redirect: to => `/videos/${to.params.id}`
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    redirect: '/admin/category-manage',
    children: [
      {
        path: 'category-manage',
        name: 'CategoryManage',
        component: () => import('../views/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'video-manage',
        name: 'VideoManage',
        component: () => import('../views/VideoManage.vue'),
        meta: { title: '视频管理' }
      },
      {
        path: 'video-category-manage',
        name: 'VideoCategoryManage',
        component: () => import('../views/VideoCategoryManage.vue'),
        meta: { title: '视频分类管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + ' - AI云学智训平台'
  }
  const token = localStorage.getItem('token')
  if (to.meta.guest) {
    next()
  } else if (!token && to.path !== '/login' && to.path !== '/register') {
    next('/login')
  } else {
    next()
  }
})

router.afterEach((to) => {
  document.title = to.meta.title + ' - AI云学智训平台'
})

export default router
