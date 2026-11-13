import { createRouter, createWebHashHistory } from 'vue-router'
import { api, auth } from '../api/http'

const routes = [
  { path: '/', redirect: '/accounts' },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/signup', component: () => import('../views/SignupView.vue'), meta: { public: true } },
  { path: '/accounts', component: () => import('../views/DashboardView.vue') },
  { path: '/accounts/:id', component: () => import('../views/AccountDetailView.vue'), props: true },
  { path: '/transfer', component: () => import('../views/TransferView.vue') },
  { path: '/transactions', component: () => import('../views/TransactionsView.vue') },
  { path: '/profile', component: () => import('../views/ProfileView.vue') },
  { path: '/demo', component: () => import('../views/demo/DemoHome.vue') },
  { path: '/demo/axios', component: () => import('../views/demo/AxiosDemo.vue') },
  { path: '/demo/fetch', component: () => import('../views/demo/FetchDemo.vue') },
  { path: '/demo/composable', component: () => import('../views/demo/ComposableDemo.vue') },
  { path: '/demo/pinia', component: () => import('../views/demo/PiniaDemo.vue') },
  { path: '/demo/query', component: () => import('../views/demo/QueryDemo.vue') },
  { path: '/demo/jwt', component: () => import('../views/demo/JwtDemo.vue') },
  { path: '/demo/realtime', component: () => import('../views/demo/RealtimeDemo.vue') },
  { path: '/demo/download', component: () => import('../views/demo/DownloadDemo.vue') },
  { path: '/admin/members', component: () => import('../views/AdminMembersView.vue'), meta: { admin: true } },
  { path: '/admin/stats', component: () => import('../views/AdminStatsView.vue'), meta: { admin: true } }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach(async (to) => {
  // restore the session after a page reload
  if (!auth.loaded) {
    try {
      auth.user = await api.me()
    } catch {
      auth.user = null
    }
    auth.loaded = true
  }
  if (!to.meta.public && !auth.user) return '/login'
  if (to.meta.public && auth.user) return '/accounts'
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/accounts'
})

export default router
