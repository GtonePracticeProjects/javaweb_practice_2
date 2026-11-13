<script setup>
import { computed, reactive, ref } from 'vue'
import CodeCard from '../../components/CodeCard.vue'
import { decodeJwt, tokenLogin, tokenLogout, tokenRequest, tokenStore } from '../../api/tokenClient'

const form = reactive({ loginId: 'hong', password: 'Hong1234!' })
const results = ref([])
const add = (title, ok, text) => results.value.unshift({ title, ok, text })

const payload = computed(() => (tokenStore.token ? decodeJwt(tokenStore.token) : null))

async function login() {
  try {
    await tokenLogin(form.loginId, form.password)
    add('토큰 로그인', true, 'access token 발급됨 (' + tokenStore.expiresInSeconds + '초 유효). 세션 쿠키는 만들어지지 않습니다.')
  } catch (e) {
    add('토큰 로그인', false, e.message)
  }
}

// Every call below uses fetch(credentials:'omit'), so the existing session cookie of this browser is NOT sent.
async function call(title, path, token) {
  try {
    const r = await tokenRequest(path, {}, token)
    add(title, true, JSON.stringify(r).substring(0, 160))
  } catch (e) {
    add(title, false, 'HTTP ' + e.status + ' · ' + e.message)
  }
}
const withToken = () => call('토큰 O · GET /auth/me', '/auth/me')
const withoutToken = () => call('토큰 X · GET /auth/me', '/auth/me', '')
const tampered = () => call('변조된 토큰 · GET /auth/me', '/auth/me', tokenStore.token.slice(0, -2) + 'xx')
const adminApi = () => call('USER 토큰 · GET /admin/stats/members', '/admin/stats/members')

const code = `// 1) 로그인 → 토큰 받기 (쿠키 사용 안 함)
const res = await fetch('/api/auth/token', { method: 'POST', credentials: 'omit', body: JSON.stringify({ loginId, password }),
  headers: { 'Content-Type': 'application/json' } })
const { accessToken } = await res.json()

// 2) 이후 모든 요청에 Authorization 헤더
fetch('/api/accounts', { credentials: 'omit', headers: { Authorization: 'Bearer ' + accessToken } })

// axios 라면 요청 인터셉터에서 한 번만 설정
http.interceptors.request.use((config) => { config.headers.Authorization = 'Bearer ' + token; return config })

// 서버(AuthInterceptor): 세션이 없으면 Bearer 토큰을 검증해서 memberId/role 을 꺼냄`
</script>

<template>
  <CodeCard title="⑥ JWT Bearer 토큰" desc="세션 쿠키 대신 Authorization 헤더로 인증합니다. 모바일 앱, 다른 도메인의 프론트, 외부 시스템 연동에 쓰는 방식입니다. 이 화면의 모든 호출은 쿠키를 보내지 않습니다(credentials: omit)." :code="code">
    <form class="row" @submit.prevent="login">
      <div><label>아이디</label><input v-model="form.loginId" /></div>
      <div><label>비밀번호</label><input v-model="form.password" type="password" /></div>
      <button type="submit">토큰 발급</button>
      <button type="button" class="secondary" :disabled="!tokenStore.token" @click="tokenLogout">토큰 폐기</button>
    </form>

    <template v-if="tokenStore.token">
      <h4>발급된 토큰 (header.payload.signature)</h4>
      <pre class="code token">{{ tokenStore.token }}</pre>
      <div class="tag">payload (누구나 base64 로 읽을 수 있음 — 비밀 정보를 넣지 마세요): {{ JSON.stringify(payload) }}</div>
    </template>

    <h4>호출해 보기</h4>
    <div class="row">
      <button :disabled="!tokenStore.token" @click="withToken">토큰으로 호출</button>
      <button class="secondary" @click="withoutToken">토큰 없이 호출 → 401</button>
      <button class="secondary" :disabled="!tokenStore.token" @click="tampered">변조 토큰 → 401</button>
      <button class="secondary" :disabled="!tokenStore.token" @click="adminApi">관리자 API → 403</button>
    </div>
    <ul class="log">
      <li v-for="(r, i) in results" :key="i" :class="r.ok ? 'ok' : 'err'"><b>{{ r.title }}</b> — {{ r.text }}</li>
    </ul>

    <div class="msg">
      <b>세션 vs JWT</b><br />
      세션: 서버가 로그인 상태를 기억 → 즉시 로그아웃/잠금 가능, 브라우저 쿠키(HttpOnly)라 JS 가 못 읽음.<br />
      JWT: 서버가 상태를 안 가짐 → 서버 여러 대에 쉬움, 하지만 만료 전에는 취소하기 어렵고(계정 잠금도 토큰 만료까지 유효), 저장 위치(메모리/스토리지)를 신경 써야 함.
      이 데모는 토큰을 메모리에만 두므로 새로고침하면 사라집니다.
    </div>
  </CodeCard>
</template>
