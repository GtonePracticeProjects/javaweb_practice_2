import { reactive } from 'vue'
import { request } from './fetchClient'

/**
 * JWT ("Bearer token") client.
 * The token lives only in memory here: a page reload logs the demo out, but an XSS attack
 * cannot read it from localStorage. (Trade-offs are explained on the demo screen.)
 */
export const tokenStore = reactive({ token: '', member: null, expiresInSeconds: 0 })

export async function tokenLogin(loginId, password) {
  // credentials:'omit' -> no session cookie is created or sent, the token is the only credential
  const res = await request('/auth/token', { method: 'POST', body: { loginId, password }, credentials: 'omit' })
  tokenStore.token = res.accessToken
  tokenStore.member = res.member
  tokenStore.expiresInSeconds = res.expiresInSeconds
  return res
}

export function tokenLogout() {
  tokenStore.token = ''
  tokenStore.member = null
}

/** Calls the API with the Authorization header and WITHOUT cookies. */
export function tokenRequest(path, options = {}, tokenOverride) {
  const token = tokenOverride ?? tokenStore.token
  return request(path, {
    ...options,
    credentials: 'omit',
    headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(options.headers || {}) }
  })
}

/** Reads the (unverified!) payload of a JWT for display. The server is the only one who can verify it. */
export function decodeJwt(token) {
  try {
    const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(decodeURIComponent(escape(atob(b64))))
  } catch {
    return null
  }
}
