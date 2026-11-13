<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, won } from '../api/http'

const accounts = ref([])
const form = reactive({ fromAccountNumber: '', toAccountNumber: '', amount: '', memo: '' })
const owner = ref('')
const msg = reactive({ ok: '', err: '' })

onMounted(async () => {
  const data = await api.accounts()
  accounts.value = data.accounts.filter((a) => a.status === 'ACTIVE')
  if (accounts.value.length) form.fromAccountNumber = accounts.value[0].accountNumber
})

async function lookup() {
  owner.value = ''
  msg.err = ''
  try {
    owner.value = (await api.lookup(form.toAccountNumber)).ownerName
  } catch (e) {
    msg.err = e.message
  }
}

async function submit() {
  msg.ok = msg.err = ''
  try {
    await api.transfer({ ...form, amount: Number(form.amount) })
    msg.ok = `${owner.value || form.toAccountNumber}님께 ${won(form.amount)} 이체했습니다.`
    form.toAccountNumber = form.amount = form.memo = ''
    owner.value = ''
    const data = await api.accounts()
    accounts.value = data.accounts.filter((a) => a.status === 'ACTIVE')
  } catch (e) {
    msg.err = e.message
  }
}
</script>

<template>
  <form class="card" @submit.prevent="submit">
    <h2>계좌 이체</h2>
    <label>출금 계좌</label>
    <select v-model="form.fromAccountNumber" required>
      <option v-for="a in accounts" :key="a.id" :value="a.accountNumber">{{ a.accountNumber }} ({{ won(a.balance) }})</option>
    </select>
    <label>받는 계좌번호</label>
    <div class="row">
      <input v-model="form.toAccountNumber" placeholder="110-000-000000" required @input="owner = ''" />
      <button type="button" class="secondary" @click="lookup">예금주 조회</button>
    </div>
    <div v-if="owner" class="msg ok">예금주: {{ owner }}</div>
    <label>금액</label>
    <input v-model="form.amount" type="number" min="1" required />
    <label>메모</label>
    <input v-model="form.memo" maxlength="100" />
    <div v-if="msg.err" class="msg err">{{ msg.err }}</div>
    <div v-if="msg.ok" class="msg ok">{{ msg.ok }}</div>
    <p><button type="submit">이체하기</button></p>
  </form>
</template>
