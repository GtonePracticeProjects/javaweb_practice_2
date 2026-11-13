<script setup>
import { ref } from 'vue'
import { useAccountStore } from '../stores/accounts'
import { won } from '../api/http'

const store = useAccountStore()
const amount = ref(1000)
const error = ref('')

async function deposit(id) {
  error.value = ''
  try {
    await store.deposit(id, Number(amount.value))
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div>
    <h4>컴포넌트 A: 계좌 목록 (입금 가능)</h4>
    <div class="row" style="margin-bottom: 8px">
      <div><label>입금액</label><input v-model="amount" type="number" min="1" /></div>
    </div>
    <table>
      <thead><tr><th>계좌</th><th class="num">잔액</th><th></th></tr></thead>
      <tbody>
        <tr v-for="a in store.accounts" :key="a.id">
          <td>{{ a.accountNumber }}</td><td class="num">{{ won(a.balance) }}</td>
          <td><button :disabled="a.status !== 'ACTIVE'" @click="deposit(a.id)">+ 입금</button></td>
        </tr>
      </tbody>
    </table>
    <div v-if="error" class="msg err">{{ error }}</div>
  </div>
</template>
