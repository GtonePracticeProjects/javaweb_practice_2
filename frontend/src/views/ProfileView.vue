<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, auth, dt } from '../api/http'

const profile = reactive({ email: '', phone: '' })
const pw = reactive({ currentPassword: '', newPassword: '' })
const msg = reactive({ profileOk: '', profileErr: '', pwOk: '', pwErr: '' })
const me = ref(null)

onMounted(async () => {
  me.value = await api.me()
  profile.email = me.value.email
  profile.phone = me.value.phone || ''
})

async function saveProfile() {
  msg.profileOk = msg.profileErr = ''
  try {
    me.value = auth.user = await api.updateProfile(profile)
    msg.profileOk = '저장되었습니다.'
  } catch (e) {
    msg.profileErr = e.message
  }
}

async function savePassword() {
  msg.pwOk = msg.pwErr = ''
  try {
    await api.changePassword(pw)
    pw.currentPassword = pw.newPassword = ''
    msg.pwOk = '비밀번호가 변경되었습니다.'
  } catch (e) {
    msg.pwErr = e.message
  }
}
</script>

<template>
  <div class="card" v-if="me">
    <h2>내 정보</h2>
    <p>{{ me.name }} ({{ me.loginId }}) · <span class="badge" :class="me.status">{{ me.status }}</span> · 최근 로그인 {{ dt(me.lastLoginAt) }}</p>
  </div>
  <form class="card" @submit.prevent="saveProfile">
    <h3>연락처 변경</h3>
    <label>이메일</label><input v-model="profile.email" type="email" required />
    <label>전화번호</label><input v-model="profile.phone" />
    <div v-if="msg.profileOk" class="msg ok">{{ msg.profileOk }}</div>
    <div v-if="msg.profileErr" class="msg err">{{ msg.profileErr }}</div>
    <p><button type="submit">저장</button></p>
  </form>
  <form class="card" @submit.prevent="savePassword">
    <h3>비밀번호 변경</h3>
    <label>현재 비밀번호</label><input v-model="pw.currentPassword" type="password" required />
    <label>새 비밀번호 (8자 이상)</label><input v-model="pw.newPassword" type="password" minlength="8" required />
    <div v-if="msg.pwOk" class="msg ok">{{ msg.pwOk }}</div>
    <div v-if="msg.pwErr" class="msg err">{{ msg.pwErr }}</div>
    <p><button type="submit">변경</button></p>
  </form>
</template>
