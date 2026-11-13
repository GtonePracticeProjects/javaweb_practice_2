import axios from 'axios'
import { reactive } from 'vue'

/** Tiny global store: the logged-in member (null = anonymous). */
export const auth = reactive({ user: null, loaded: false })

export const http = axios.create({ baseURL: '/api' })

// Turn the server's {message} into a plain Error message the views can show.
http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const message = err.response?.data?.message || err.message
    if (status === 401 && auth.user) {
      auth.user = null // session expired
      window.location.hash = '#/login'
    }
    return Promise.reject(Object.assign(new Error(message), { status }))
  }
)

export const api = {
  // auth
  signup: (b) => http.post('/auth/signup', b).then((r) => r.data),
  login: (b) => http.post('/auth/login', b).then((r) => r.data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me').then((r) => r.data),
  updateProfile: (b) => http.put('/members/me', b).then((r) => r.data),
  changePassword: (b) => http.put('/members/me/password', b),

  // accounts
  accounts: () => http.get('/accounts').then((r) => r.data),
  openAccount: (b) => http.post('/accounts', b).then((r) => r.data),
  account: (id) => http.get(`/accounts/${id}`).then((r) => r.data),
  closeAccount: (id) => http.delete(`/accounts/${id}`),
  deposit: (id, b) => http.post(`/accounts/${id}/deposit`, b).then((r) => r.data),
  withdraw: (id, b) => http.post(`/accounts/${id}/withdraw`, b).then((r) => r.data),
  transfer: (b) => http.post('/accounts/transfer', b),
  lookup: (accountNumber) => http.get('/accounts/lookup', { params: { accountNumber } }).then((r) => r.data),
  history: (id, page) => http.get(`/accounts/${id}/transactions`, { params: { page, size: 10 } }).then((r) => r.data),
  summary: (id) => http.get(`/accounts/${id}/summary`).then((r) => r.data),
  monthly: (id) => http.get(`/accounts/${id}/monthly`).then((r) => r.data),
  searchTx: (params) => http.get('/transactions', { params: clean(params) }).then((r) => r.data),

  // demo endpoints (slow / always failing) used by the "호출 방식 데모" screens
  demoSlow: (ms) => http.get('/demo/slow', { params: { ms } }).then((r) => r.data),
  demoFail: (status = 500) => http.get('/demo/fail', { params: { status } }).then((r) => r.data),

  // admin
  adminMembers: (params) => http.get('/admin/members', { params: clean(params) }).then((r) => r.data),
  adminChangeStatus: (id, status) => http.patch(`/admin/members/${id}/status`, { status }).then((r) => r.data),
  adminDormant: () => http.post('/admin/members/dormant-batch').then((r) => r.data),
  statAccounts: () => http.get('/admin/stats/accounts').then((r) => r.data),
  statMembers: () => http.get('/admin/stats/members').then((r) => r.data),
  statTransactions: () => http.get('/admin/stats/transactions').then((r) => r.data)
}

/** Drop empty values so the server sees "no condition". */
function clean(params = {}) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null))
}

export const won = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR') + '원')
export const dt = (v) => (v ? v.replace('T', ' ').substring(0, 19) : '-')
