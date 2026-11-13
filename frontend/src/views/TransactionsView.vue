<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, dt, won } from '../api/http'

const accounts = ref([])
const cond = reactive({ accountNumber: '', type: '', minAmount: '', maxAmount: '', from: '', to: '', keyword: '' })
const page = ref({ content: [], number: 0, totalPages: 0, totalElements: 0 })
const error = ref('')

async function search(p = 0) {
  error.value = ''
  try {
    page.value = await api.searchTx({ ...cond, page: p })   // QueryDSL dynamic query
  } catch (e) {
    error.value = e.message
  }
}

function reset() {
  Object.keys(cond).forEach((k) => (cond[k] = ''))
  search(0)
}

const typeLabel = { DEPOSIT: '입금', WITHDRAW: '출금', TRANSFER_IN: '이체입금', TRANSFER_OUT: '이체출금' }

onMounted(async () => {
  accounts.value = (await api.accounts()).accounts
  search(0)
})
</script>

<template>
  <form class="card" @submit.prevent="search(0)">
    <h3>거래 조회 <span class="tag">QueryDSL 동적 쿼리</span></h3>
    <div class="row">
      <div><label>계좌</label>
        <select v-model="cond.accountNumber"><option value="">전체</option>
          <option v-for="a in accounts" :key="a.id" :value="a.accountNumber">{{ a.accountNumber }}</option></select></div>
      <div><label>구분</label>
        <select v-model="cond.type"><option value="">전체</option>
          <option v-for="(l, k) in typeLabel" :key="k" :value="k">{{ l }}</option></select></div>
      <div><label>최소 금액</label><input v-model="cond.minAmount" type="number" /></div>
      <div><label>최대 금액</label><input v-model="cond.maxAmount" type="number" /></div>
    </div>
    <div class="row">
      <div><label>시작일</label><input v-model="cond.from" type="date" /></div>
      <div><label>종료일</label><input v-model="cond.to" type="date" /></div>
      <div><label>메모 / 상대계좌</label><input v-model="cond.keyword" /></div>
      <button type="submit">검색</button>
      <button type="button" class="secondary" @click="reset">초기화</button>
    </div>
  </form>

  <div class="card">
    <div v-if="error" class="msg err">{{ error }}</div>
    <div>총 {{ page.totalElements }}건</div>
    <table>
      <thead><tr><th>일시</th><th>계좌</th><th>구분</th><th>상대 계좌</th><th>메모</th><th class="num">금액</th></tr></thead>
      <tbody>
        <tr v-for="t in page.content" :key="t.id">
          <td>{{ dt(t.createdAt) }}</td><td>{{ t.accountNumber }}</td><td>{{ typeLabel[t.type] }}</td>
          <td>{{ t.counterAccountNumber || '-' }}</td><td>{{ t.memo || '-' }}</td><td class="num">{{ won(t.amount) }}</td>
        </tr>
        <tr v-if="!page.content.length"><td colspan="6">조회 결과가 없습니다.</td></tr>
      </tbody>
    </table>
    <div class="pager" v-if="page.totalPages > 1">
      <button class="secondary" :disabled="page.number === 0" @click="search(page.number - 1)">이전</button>
      {{ page.number + 1 }} / {{ page.totalPages }}
      <button class="secondary" :disabled="page.number + 1 >= page.totalPages" @click="search(page.number + 1)">다음</button>
    </div>
  </div>
</template>
