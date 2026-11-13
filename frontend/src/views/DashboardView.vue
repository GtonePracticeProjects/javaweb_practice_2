<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, won } from '../api/http'

const data = ref({ accounts: [], totalBalance: 0 })
const form = reactive({ accountType: 'CHECKING', alias: '', initialDeposit: 0 })
const error = ref('')

async function load() {
  data.value = await api.accounts()
}

async function open() {
  error.value = ''
  try {
    await api.openAccount({ ...form, initialDeposit: Number(form.initialDeposit) || 0 })
    form.alias = ''
    form.initialDeposit = 0
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="card">
    <div>총 자산 (활성 계좌)</div>
    <div class="balance">{{ won(data.totalBalance) }}</div>
  </div>

  <div class="card">
    <h3>내 계좌</h3>
    <table>
      <thead><tr><th>계좌번호</th><th>별칭</th><th>종류</th><th>상태</th><th class="num">잔액</th></tr></thead>
      <tbody>
        <tr v-for="a in data.accounts" :key="a.id">
          <td><router-link :to="`/accounts/${a.id}`">{{ a.accountNumber }}</router-link></td>
          <td>{{ a.alias || '-' }}</td>
          <td>{{ a.accountType === 'CHECKING' ? '입출금' : '예금' }}</td>
          <td><span class="badge" :class="a.status">{{ a.status }}</span></td>
          <td class="num">{{ won(a.balance) }}</td>
        </tr>
        <tr v-if="!data.accounts.length"><td colspan="5">개설된 계좌가 없습니다.</td></tr>
      </tbody>
    </table>
  </div>

  <form class="card" @submit.prevent="open">
    <h3>계좌 개설</h3>
    <div class="row">
      <div><label>종류</label>
        <select v-model="form.accountType"><option value="CHECKING">입출금</option><option value="SAVINGS">예금</option></select></div>
      <div><label>별칭</label><input v-model="form.alias" maxlength="50" /></div>
      <div><label>초기 입금액</label><input v-model="form.initialDeposit" type="number" min="0" /></div>
      <button type="submit">개설</button>
    </div>
    <div v-if="error" class="msg err">{{ error }}</div>
  </form>
</template>
