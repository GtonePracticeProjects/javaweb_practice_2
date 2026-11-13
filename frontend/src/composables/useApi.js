import { onMounted, ref } from 'vue'

/**
 * Composable that wraps any promise-returning function with loading / error / data state.
 *
 *   const accounts = useApi(api.accounts)           // runs on mount
 *   const slow     = useApi(api.demoSlow, { immediate: false })
 *   slow.execute(2000)
 *
 * Race-condition guard: if execute() is called again before the previous call finished,
 * only the LAST call is allowed to write its result (a slow old response can never
 * overwrite a newer one).
 */
export function useApi(fn, { immediate = true } = {}) {
  const data = ref(null)
  const error = ref(null)
  const loading = ref(false)
  let latest = 0

  async function execute(...args) {
    const mine = ++latest
    loading.value = true
    error.value = null
    try {
      const result = await fn(...args)
      if (mine === latest) data.value = result
      return result
    } catch (e) {
      if (mine === latest) error.value = e
    } finally {
      if (mine === latest) loading.value = false
    }
  }

  if (immediate) onMounted(() => execute())

  return { data, error, loading, execute }
}
