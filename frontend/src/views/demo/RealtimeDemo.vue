<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import CodeCard from '../../components/CodeCard.vue'
import { api, won } from '../../api/http'

const accounts = ref([])
const form = reactive({ from: '', to: '', amount: 1000 })
const msg = reactive({ ok: '', err: '' })

async function loadAccounts() {
  accounts.value = (await api.accounts()).accounts.filter((a) => a.status === 'ACTIVE')
  if (accounts.value.length >= 2 && !form.from) {
    form.from = accounts.value[0].accountNumber
    form.to = accounts.value[1].accountNumber
  }
}

// ---- SSE ---------------------------------------------------------------------
const sseState = ref('끊김')
const events = ref([])
let es = null

function connect() {
  if (es) return
  es = new EventSource('/api/notifications/stream')          // GET, text/event-stream, session cookie is sent
  es.onopen = () => (sseState.value = '연결됨')
  es.onerror = () => (sseState.value = '재연결 중… (브라우저가 자동으로 다시 연결)')
  es.addEventListener('connected', () => (sseState.value = '연결됨'))
  es.addEventListener('transfer-in', (e) => {                // the event name chosen by the server
    const d = JSON.parse(e.data)
    events.value.unshift({ ...d, receivedAt: new Date().toLocaleTimeString() })
    loadAccounts()
  })
}
function disconnect() {
  es?.close()
  es = null
  sseState.value = '끊김'
}

// ---- polling -----------------------------------------------------------------
const polling = reactive({ on: false, calls: 0, total: null })
let pollTimer = null
async function pollOnce() {
  const d = await api.accounts()
  polling.calls++
  polling.total = d.totalBalance
}
function startPolling() {
  if (pollTimer) return
  polling.on = true
  pollOnce()
  pollTimer = setInterval(pollOnce, 3000)
}
function stopPolling() {
  clearInterval(pollTimer)
  pollTimer = null
  polling.on = false
}

async function transfer() {
  msg.ok = msg.err = ''
  try {
    await api.transfer({ fromAccountNumber: form.from, toAccountNumber: form.to, amount: Number(form.amount), memo: 'SSE 데모' })
    msg.ok = '이체 완료 — 받는 계좌의 소유자에게 서버가 알림을 푸시합니다.'
  } catch (e) {
    msg.err = e.message
  }
}

onMounted(loadAccounts)
onUnmounted(() => {
  disconnect()      // always release the connection when leaving the screen
  stopPolling()
})

const code = `// SSE: 서버 → 브라우저 단방향 푸시 (연결 하나를 계속 열어 둠)
const es = new EventSource('/api/notifications/stream')
es.addEventListener('transfer-in', (e) => console.log(JSON.parse(e.data)))
es.close()            // 화면을 떠날 때 반드시 닫기

// 서버(Spring): SseEmitter 를 회원별로 보관 → 이체 커밋 후 publish
emitter.send(SseEmitter.event().name('transfer-in').data(payload))

// 폴링: 일정 간격으로 계속 물어보기
setInterval(() => api.accounts(), 3000)`
</script>

<template>
  <CodeCard title="⑦ 서버 푸시 (SSE) vs 폴링" desc="이체가 일어나면 서버가 받는 사람에게 먼저 알림을 보냅니다. 같은 사용자의 두 계좌 사이에서 이체해도 받는 계좌의 소유자(=나)에게 알림이 옵니다." :code="code">
    <div v-if="accounts.length < 2" class="msg err">이 데모는 계좌가 2개 이상 필요합니다. (hong 계정은 2개)</div>

    <div class="row" style="align-items: flex-start">
      <div>
        <h4>SSE</h4>
        <div class="tag">상태: {{ sseState }}</div>
        <p>
          <button :disabled="!!es" @click="connect">연결</button>
          <button class="secondary" @click="disconnect">끊기</button>
        </p>
      </div>
      <div>
        <h4>폴링 (3초 간격)</h4>
        <div class="tag">요청 {{ polling.calls }}회 · 총 자산 {{ polling.total == null ? '-' : won(polling.total) }}</div>
        <p>
          <button :disabled="polling.on" @click="startPolling">시작</button>
          <button class="secondary" @click="stopPolling">중지</button>
        </p>
      </div>
    </div>

    <h4>이체로 알림 발생시키기</h4>
    <div class="row">
      <div><label>출금</label>
        <select v-model="form.from"><option v-for="a in accounts" :key="a.id" :value="a.accountNumber">{{ a.accountNumber }}</option></select></div>
      <div><label>입금</label>
        <select v-model="form.to"><option v-for="a in accounts" :key="a.id" :value="a.accountNumber">{{ a.accountNumber }}</option></select></div>
      <div><label>금액</label><input v-model="form.amount" type="number" min="1" /></div>
      <button @click="transfer">이체</button>
    </div>
    <div v-if="msg.ok" class="msg ok">{{ msg.ok }}</div>
    <div v-if="msg.err" class="msg err">{{ msg.err }}</div>

    <h4>수신한 이벤트</h4>
    <ul class="log">
      <li v-for="(e, i) in events" :key="i" class="ok">
        <span class="tag">{{ e.receivedAt }}</span>{{ e.fromAccountNumber }} → {{ e.accountNumber }} {{ won(e.amount) }} (잔액 {{ won(e.balance) }})
      </li>
      <li v-if="!events.length" class="tag">아직 없음 — 먼저 SSE 를 연결한 뒤 이체해 보세요.</li>
    </ul>
    <p class="desc">폴링은 변화가 없어도 계속 요청하고 최대 3초 늦게 알게 되지만, SSE 는 변화가 생긴 순간에만 전달됩니다. 대신 서버가 연결을 계속 유지해야 합니다.</p>
  </CodeCard>
</template>
