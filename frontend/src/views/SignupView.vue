<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/http'

const router = useRouter()
const form = reactive({ loginId: '', password: '', name: '', email: '', phone: '' })
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await api.signup(form)
    alert('가입이 완료되었습니다. 로그인해 주세요.')
    router.push('/login')
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <form class="card narrow" @submit.prevent="submit">
    <h2>회원가입</h2>
    <label>아이디 (4~20자, 영문/숫자/_)</label>
    <input v-model="form.loginId" required />
    <label>비밀번호 (8자 이상)</label>
    <input v-model="form.password" type="password" required />
    <label>이름</label>
    <input v-model="form.name" required />
    <label>이메일</label>
    <input v-model="form.email" type="email" required />
    <label>전화번호</label>
    <input v-model="form.phone" placeholder="010-1234-5678" />
    <div v-if="error" class="msg err">{{ error }}</div>
    <p><button type="submit">가입하기</button> <router-link to="/login">로그인으로</router-link></p>
  </form>
</template>
