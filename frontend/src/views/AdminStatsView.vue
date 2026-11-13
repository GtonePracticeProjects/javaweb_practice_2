<script setup>
import { onMounted, ref } from 'vue'
import { api, won } from '../api/http'

const accounts = ref([])
const members = ref([])
const txs = ref([])
const typeLabel = { DEPOSIT: '입금', WITHDRAW: '출금', TRANSFER_IN: '이체입금', TRANSFER_OUT: '이체출금' }

onMounted(async () => {
  ;[accounts.value, members.value, txs.value] = await Promise.all([
    api.statAccounts(), api.statMembers(), api.statTransactions()
  ])
})
</script>

<template>
  <div class="card">
    <h3>계좌 종류별 현황 <span class="tag">Native SQL</span></h3>
    <table>
      <thead><tr><th>종류</th><th class="num">계좌 수</th><th class="num">총 잔액</th></tr></thead>
      <tbody><tr v-for="a in accounts" :key="a.accountType"><td>{{ a.accountType }}</td><td class="num">{{ a.accountCount }}</td><td class="num">{{ won(a.totalBalance) }}</td></tr></tbody>
    </table>
  </div>
  <div class="card">
    <h3>회원 상태별 현황 <span class="tag">JPQL 생성자 표현식</span></h3>
    <table>
      <thead><tr><th>상태</th><th class="num">회원 수</th></tr></thead>
      <tbody><tr v-for="m in members" :key="m.status"><td><span class="badge" :class="m.status">{{ m.status }}</span></td><td class="num">{{ m.count }}</td></tr></tbody>
    </table>
  </div>
  <div class="card">
    <h3>월별 거래 통계 <span class="tag">QueryDSL group by</span></h3>
    <table>
      <thead><tr><th>월</th><th>구분</th><th class="num">건수</th><th class="num">합계</th></tr></thead>
      <tbody><tr v-for="(t, i) in txs" :key="i"><td>{{ t.year }}-{{ String(t.month).padStart(2, '0') }}</td><td>{{ typeLabel[t.type] }}</td><td class="num">{{ t.count }}</td><td class="num">{{ won(t.total) }}</td></tr></tbody>
    </table>
  </div>
</template>
