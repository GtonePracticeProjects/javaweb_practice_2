<script setup>
import { onUnmounted, ref, watch } from 'vue'
import CodeCard from '../../components/CodeCard.vue'
import { request } from '../../api/fetchClient'
import { won } from '../../api/http'

// --- 1) plain GET / error handling ------------------------------------------
const result = ref('')
async function load() {
  const data = await request('/accounts')
  result.value = '계좌 ' + data.accounts.length + '개 · 총 자산 ' + won(data.totalBalance)
}
async function notFound() {
  try {
    await request('/accounts/999999')     // fetch would "succeed" with a 404; our wrapper throws
  } catch (e) {
    result.value = '에러 처리됨 → status=' + e.status + ', message="' + e.message + '"'
  }
}

// --- 2) typeahead: debounce + cancel the previous request -------------------
const accountNumber = ref('')
const owner = ref('')
const stats = ref({ sent: 0, cancelled: 0 })
let timer = null
let controller = null

watch(accountNumber, (value) => {
  clearTimeout(timer)                         // debounce: wait until typing pauses
  owner.value = ''
  if (value.length < 8) return
  timer = setTimeout(() => lookup(value), 300)
})

async function lookup(value) {
  controller?.abort()                         // cancel the request that is still running
  controller = new AbortController()
  stats.value.sent++
  try {
    const r = await request('/accounts/lookup', { params: { accountNumber: value }, signal: controller.signal })
    owner.value = '예금주: ' + r.ownerName
  } catch (e) {
    if (e.name === 'AbortError') stats.value.cancelled++
    else owner.value = e.message
  }
}

onUnmounted(() => {
  clearTimeout(timer)
  controller?.abort()
})

const code = `// fetch 는 4xx/5xx 에서도 reject 하지 않는다 → res.ok 확인 필수
const res = await fetch('/api/accounts/1', { credentials: 'same-origin', signal })
if (!res.ok) throw new Error((await res.json()).message)
const data = await res.json()

// 디바운스 + 이전 요청 취소
watch(accountNumber, (v) => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    controller?.abort()
    controller = new AbortController()
    request('/accounts/lookup', { params: { accountNumber: v }, signal: controller.signal })
  }, 300)
})`
</script>

<template>
  <CodeCard title="② fetch (브라우저 내장)" desc="의존성 없이 호출합니다. src/api/fetchClient.js 의 작은 래퍼가 JSON 변환, 쿼리스트링, 에러 처리를 맡습니다." :code="code">
    <div class="row">
      <button @click="load">계좌 조회</button>
      <button class="secondary" @click="notFound">없는 계좌 조회 (404)</button>
    </div>
    <div v-if="result" class="msg ok">{{ result }}</div>

    <h4>예금주 자동완성 (디바운스 300ms + 이전 요청 취소)</h4>
    <label>계좌번호를 입력해 보세요 (예: 110-414-593794 처럼 8자 이상, 시드 데이터의 번호는 내 계좌 화면에서 확인)</label>
    <input v-model="accountNumber" placeholder="110-000-000000" />
    <div v-if="owner" class="msg ok">{{ owner }}</div>
    <div class="tag">전송 {{ stats.sent }}회 · 취소 {{ stats.cancelled }}회</div>
  </CodeCard>
</template>
