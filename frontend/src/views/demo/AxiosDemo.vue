<script setup>
import { onUnmounted, ref } from 'vue'
import axios from 'axios'
import CodeCard from '../../components/CodeCard.vue'
import { api } from '../../api/http'

const logs = ref([])
const log = (text, kind = '') => logs.value.unshift({ text, kind, at: new Date().toLocaleTimeString() })

// A dedicated instance with interceptors that measure the elapsed time of every call
const inst = axios.create({ baseURL: '/api' })
inst.interceptors.request.use((config) => {
  config.meta = { start: performance.now() }
  return config
})
inst.interceptors.response.use(
  (res) => {
    log('← ' + res.status + ' ' + res.config.url + ' (' + Math.round(performance.now() - res.config.meta.start) + 'ms)', 'ok')
    return res
  },
  (err) => {
    const ms = err.config?.meta ? Math.round(performance.now() - err.config.meta.start) : '?'
    log('← 실패 ' + err.config?.url + ' (' + ms + 'ms): ' + (axios.isCancel(err) ? '취소됨' : err.code === 'ECONNABORTED' ? '타임아웃' : err.message), 'err')
    return Promise.reject(err)
  }
)

async function basic() {
  const data = await api.accounts()               // wrapper from src/api/http.js
  log('공용 api 래퍼: 계좌 ' + data.accounts.length + '개, 총 자산 ' + data.totalBalance)
}

async function timeout() {
  try {
    await inst.get('/demo/slow', { params: { ms: 3000 }, timeout: 1000 })   // server needs 3s, we wait 1s
  } catch { /* logged by the interceptor */ }
}

let controller = null
async function startCancelable() {
  controller = new AbortController()
  try {
    await inst.get('/demo/slow', { params: { ms: 4000 }, signal: controller.signal })
  } catch { /* logged */ }
}
function cancel() {
  controller?.abort()
}

async function parallel() {
  const t0 = performance.now()
  await Promise.all([inst.get('/demo/slow', { params: { ms: 1000 } }), inst.get('/demo/slow', { params: { ms: 1000 } }), inst.get('/demo/slow', { params: { ms: 1000 } })])
  log('병렬 3건 합계 ' + Math.round(performance.now() - t0) + 'ms (≈1초)', 'ok')
}
async function sequential() {
  const t0 = performance.now()
  for (let i = 0; i < 3; i++) await inst.get('/demo/slow', { params: { ms: 1000 } })
  log('순차 3건 합계 ' + Math.round(performance.now() - t0) + 'ms (≈3초)', 'ok')
}

async function serverError() {
  try {
    await api.demoFail(503)
  } catch (e) {
    log('래퍼 인터셉터가 변환한 에러: status=' + e.status + ', message="' + e.message + '"', 'err')
  }
}

onUnmounted(() => controller?.abort())

const code = `// 1) 인스턴스 만들기 (공통 baseURL)
const http = axios.create({ baseURL: '/api' })

// 2) 인터셉터: 모든 응답/에러를 한 곳에서 처리
http.interceptors.response.use(
  (res) => res,
  (err) => Promise.reject(Object.assign(new Error(err.response?.data?.message), { status: err.response?.status }))
)

// 3) 타임아웃
inst.get('/demo/slow', { params: { ms: 3000 }, timeout: 1000 })

// 4) 취소 (AbortController)
const controller = new AbortController()
inst.get('/demo/slow', { signal: controller.signal })
controller.abort()

// 5) 병렬 호출
await Promise.all([a(), b(), c()])`
</script>

<template>
  <CodeCard title="① axios" desc="인스턴스 + 인터셉터 + 타임아웃 + 취소 + 병렬 호출. 서버의 /api/demo/slow 가 지정한 시간만큼 늦게 응답합니다." :code="code">
    <div class="row">
      <button @click="basic">기본 호출 (공용 래퍼)</button>
      <button class="secondary" @click="timeout">타임아웃 (서버 3초 / 제한 1초)</button>
      <button class="secondary" @click="startCancelable">느린 요청 시작 (4초)</button>
      <button class="danger" @click="cancel">취소</button>
    </div>
    <div class="row" style="margin-top: 8px">
      <button class="secondary" @click="parallel">병렬 3건 (각 1초)</button>
      <button class="secondary" @click="sequential">순차 3건 (각 1초)</button>
      <button class="secondary" @click="serverError">서버 503 에러 처리</button>
      <button class="link" style="color:#6b7280" @click="logs = []">로그 지우기</button>
    </div>
    <ul class="log">
      <li v-for="(l, i) in logs" :key="i" :class="l.kind"><span class="tag">{{ l.at }}</span>{{ l.text }}</li>
    </ul>
  </CodeCard>
</template>
