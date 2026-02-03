import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: 'Home' }
      },
      {
        path: '/upload',
        name: 'Upload',
        component: () => import('@/views/Upload.vue'),
        meta: { title: 'File Upload' }
      },
      {
        path: '/visualization',
        name: 'Visualization',
        component: () => import('@/views/Visualization.vue'),
        meta: { title: 'Result Visualization' }
      },
      {
        path: '/history',
        name: 'History',
        component: () => import('@/views/History.vue'),
        meta: { title: 'History' }
      },
      {
        path: '/introduction',
        name: 'Introduction',
        component: () => import('@/views/Introduction.vue'),
        meta: { title: 'Introduction' }
      },
      {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/Admin.vue'),
        meta: { title: 'Admin', requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  document.title = to.meta.title
    ? `${to.meta.title} - ARG Identification System`
    : 'ARG Identification System'

  if (to.meta.requiresAdmin) {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('Please log in first')
      next('/login')
      return
    }
    if (!userStore.isAdmin) {
      ElMessage.error('Admin permission required')
      next('/')
      return
    }
  }

  next()
})

export default router
