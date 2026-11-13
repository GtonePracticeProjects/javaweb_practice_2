/**
 * Thin wrapper over the browser's built-in fetch().
 * Things axios does for you but fetch does NOT:
 *   - fetch only rejects on network errors; 4xx/5xx still "resolve" -> we must check res.ok
 *   - JSON must be stringified / parsed by hand
 *   - query strings, timeouts and interceptors are DIY
 */
export async function request(path, { method = 'GET', body, params, signal, headers = {}, credentials = 'same-origin' } = {}) {
  const query = params
    ? '?' + new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null))
    : ''

  const res = await fetch('/api' + path + query, {
    method,
    signal,          // AbortController.signal -> lets the caller cancel the request
    credentials,     // 'same-origin' sends the session cookie, 'omit' never sends cookies
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...headers },
    body: body ? JSON.stringify(body) : undefined
  })

  if (!res.ok) {
    let message = res.statusText
    try {
      message = (await res.json()).message ?? message   // our server answers {"message": "..."}
    } catch { /* body was not JSON */ }
    throw Object.assign(new Error(message), { status: res.status })
  }
  if (res.status === 204) return null
  return (res.headers.get('content-type') || '').includes('json') ? res.json() : res.text()
}
