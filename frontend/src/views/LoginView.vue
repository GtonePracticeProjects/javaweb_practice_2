<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, auth } from '../api/http'

const router = useRouter()
const form = reactive({ loginId: '', password: '' })
const error = ref('')

async function submit() {
  error.value = ''
  try {
    auth.user = await api.login(form)
    router.push('/accounts')
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <form class="card narrow" @submit.prevent="submit">
    <h2>로그인</h2>
    <label>아이디</label>
    <input v-model="form.loginId" autocomplete="username" required />
    <label>비밀번호</label>
    <input v-model="form.password" type="password" autocomplete="current-password" required />
    <div v-if="error" class="msg err">{{ error }}</div>
    <p><button type="submit">로그인</button> <router-link to="/signup">회원가입</router-link></p>
    <p class="tag">데모: hong / Hong1234! · kim / Kim12345! · admin / Admin1234!</p>
  </form>
</template>
