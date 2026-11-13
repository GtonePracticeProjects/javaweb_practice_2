<script setup>
import CodeCard from '../../components/CodeCard.vue'
import { useApi } from '../../composables/useApi'
import { api, won } from '../../api/http'

const accounts = useApi(api.accounts)                          // runs when the component mounts
const slow = useApi(api.demoSlow, { immediate: false })        // runs on demand
const failing = useApi(api.demoFail, { immediate: false })

// Race condition demo: the 2s call is started FIRST but finishes LAST.
// Thanks to the guard in useApi only the newest call (200ms) writes the result.
function race() {
  slow.execute(2000)
  slow.execute(200)
}

const code = `// src/composables/useApi.js
export function useApi(fn, { immediate = true } = {}) {
  const data = ref(null), error = ref(null), loading = ref(false)
  let latest = 0
  async function execute(...args) {
    const mine = ++latest
    loading.value = true; error.value = null
    try { const r = await fn(...args); if (mine === latest) data.value = r }
    catch (e) { if (mine === latest) error.value = e }
    finally { if (mine === latest) loading.value = false }
  }
  if (immediate) onMounted(() => execute())
  return { data, error, loading, execute }
}

// 사용
const accounts = useApi(api.accounts)
// <div v-if="accounts.loading.value">...</div>`
</script>

<template>
  <CodeCard title="③ 컴포저블 (useApi)" desc="loading / error / data 상태 코드를 화면마다 반복하지 않도록 재사용 함수로 묶습니다." :code="code">
    <h4>마운트 시 자동 호출</h4>
    <div v-if="accounts.loading.value" class="msg">불러오는 중…</div>
    <div v-else-if="accounts.error.value" class="msg err">{{ accounts.error.value.message }}</div>
    <div v-else-if="accounts.data.value">
      계좌 {{ accounts.data.value.accounts.length }}개 · 총 자산 {{ won(accounts.data.value.totalBalance) }}
    </div>
    <button class="secondary" style="margin-top: 8px" @click="accounts.execute()">다시 불러오기</button>

    <h4>로딩 / 에러 상태</h4>
    <div class="row">
      <button @click="slow.execute(2000)">느린 호출 (2초)</button>
      <button class="secondary" @click="failing.execute(500)">500 에러 호출</button>
      <button class="secondary" @click="race">경쟁 상태 테스트 (2초 → 0.2초)</button>
    </div>
    <div v-if="slow.loading.value" class="msg">느린 요청 처리 중…</div>
    <div v-else-if="slow.data.value" class="msg ok">마지막 응답: {{ slow.data.value.waitedMs }}ms 대기 (경쟁 테스트 후에도 200이면 정상)</div>
    <div v-if="failing.error.value" class="msg err">{{ failing.error.value.message }}</div>
  </CodeCard>
</template>
