<script setup>
import { useRouter } from 'vue-router'
import { api, auth } from './api/http'

const router = useRouter()

async function logout() {
  await api.logout()
  auth.user = null
  router.push('/login')
}
</script>

<template>
  <header class="topbar">
    <router-link to="/" class="brand">🏦 Sample Bank</router-link>
    <nav v-if="auth.user">
      <router-link to="/accounts">내 계좌</router-link>
      <router-link to="/transfer">이체</router-link>
      <router-link to="/transactions">거래 조회</router-link>
      <router-link to="/profile">내 정보</router-link>
      <router-link to="/demo">호출 방식 데모</router-link>
      <template v-if="auth.user.role === 'ADMIN'">
        <router-link to="/admin/members">회원 관리</router-link>
        <router-link to="/admin/stats">통계</router-link>
      </template>
    </nav>
    <div v-if="auth.user" class="who">
      {{ auth.user.name }}님
      <button class="link" @click="logout">로그아웃</button>
    </div>
  </header>
  <main class="container">
    <router-view />
  </main>
</template>
