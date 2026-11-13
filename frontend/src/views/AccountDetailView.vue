<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, dt, won } from '../api/http'

const props = defineProps({ id: String })
const router = useRouter()

const account = ref(null)
const page = ref({ content: [], number: 0, totalPages: 0 })
const summary = ref([])
const monthly = ref([])
const form = reactive({ amount: '', memo: '' })
const msg = reactive({ ok: '', err: '' })

async function loadAccount() {
  account.value = await api.account(props.id)
}
async function loadHistory(p = 0) {
  page.value = await api.history(props.id, p)   // Spring Data derived query + Pageable
}
async function loadReports() {
  summary.value = await api.summary(props.id)   // JPQL constructor expression
  monthly.value = await api.monthly(props.id)   // native SQL
}
async function reloadAll() {
  await Promise.all([loadAccount(), loadHistory(0), loadReports()])
}

async function act(kind) {
  msg.ok = msg.err = ''
  try {
    const body = { amount: Number(form.amount), memo: form.memo || null }
    account.value = kind === 'deposit' ? await api.deposit(props.id, body) : await api.withdraw(props.id, body)
    msg.ok = kind === 'deposit' ? '입금되었습니다.' : '출금되었습니다.'
    form.amount = form.memo = ''
    await Promise.all([loadHistory(0), loadReports()])
  } catch (e) {
    msg.err = e.message
  }
}

async function close() {
  if (!confirm('계좌를 해지할까요?')) return
  try {
    await api.closeAccount(props.id)
    router.push('/accounts')
  } catch (e) {
    msg.err = e.message
  }
}

const typeLabel = { DEPOSIT: '입금', WITHDRAW: '출금', TRANSFER_IN: '이체입금', TRANSFER_OUT: '이체출금' }
onMounted(reloadAll)
</script>

<template>
  <template v-if="account">
    <div class="card">
      <div>{{ account.accountNumber }} <span class="badge" :class="account.status">{{ account.status }}</span>
        <span class="tag">{{ account.alias }}</span></div>
      <div class="balance">{{ won(account.balance) }}</div>
    </div>

    <div class="card" v-if="account.status === 'ACTIVE'">
      <h3>입금 / 출금</h3>
      <div class="row">
        <div><label>금액</label><input v-model="form.amount" type="number" min="1" /></div>
        <div><label>메모</label><input v-model="form.memo" maxlength="100" /></div>
        <button @click="act('deposit')">입금</button>
        <button class="secondary" @click="act('withdraw')">출금</button>
        <button class="danger" @click="close">계좌 해지</button>
      </div>
      <div v-if="msg.ok" class="msg ok">{{ msg.ok }}</div>
      <div v-if="msg.err" class="msg err">{{ msg.err }}</div>
    </div>

    <div class="card">
      <h3>거래 내역 <span class="tag">Spring Data 파생 쿼리 + 페이징</span></h3>
      <table>
        <thead><tr><th>일시</th><th>구분</th><th>상대 계좌</th><th>메모</th><th class="num">금액</th><th class="num">거래 후 잔액</th></tr></thead>
        <tbody>
          <tr v-for="t in page.content" :key="t.id">
            <td>{{ dt(t.createdAt) }}</td>
            <td>{{ typeLabel[t.type] }}</td>
            <td>{{ t.counterAccountNumber || '-' }}</td>
            <td>{{ t.memo || '-' }}</td>
            <td class="num" :class="t.type.endsWith('IN') || t.type === 'DEPOSIT' ? 'in' : 'out'">{{ won(t.amount) }}</td>
            <td class="num">{{ won(t.balanceAfter) }}</td>
          </tr>
          <tr v-if="!page.content.length"><td colspan="6">거래 내역이 없습니다.</td></tr>
        </tbody>
      </table>
      <div class="pager" v-if="page.totalPages > 1">
        <button class="secondary" :disabled="page.number === 0" @click="loadHistory(page.number - 1)">이전</button>
        {{ page.number + 1 }} / {{ page.totalPages }}
        <button class="secondary" :disabled="page.number + 1 >= page.totalPages" @click="loadHistory(page.number + 1)">다음</button>
      </div>
    </div>

    <div class="row" style="align-items: flex-start">
      <div class="card">
        <h3>유형별 합계 <span class="tag">JPQL 생성자 표현식</span></h3>
        <table>
          <thead><tr><th>구분</th><th class="num">건수</th><th class="num">합계</th></tr></thead>
          <tbody><tr v-for="s in summary" :key="s.type"><td>{{ typeLabel[s.type] }}</td><td class="num">{{ s.count }}</td><td class="num">{{ won(s.total) }}</td></tr></tbody>
        </table>
      </div>
      <div class="card">
        <h3>월별 리포트 <span class="tag">Native SQL</span></h3>
        <table>
          <thead><tr><th>월</th><th>구분</th><th class="num">건수</th><th class="num">합계</th></tr></thead>
          <tbody><tr v-for="(m, i) in monthly" :key="i"><td>{{ m.year }}-{{ String(m.month).padStart(2, '0') }}</td><td>{{ typeLabel[m.type] }}</td><td class="num">{{ m.count }}</td><td class="num">{{ won(m.total) }}</td></tr></tbody>
        </table>
      </div>
    </div>
  </template>
</template>
