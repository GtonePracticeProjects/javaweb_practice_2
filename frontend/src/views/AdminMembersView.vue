<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, dt, won } from '../api/http'

const cond = reactive({ loginId: '', name: '', status: '', minTotalBalance: '', createdFrom: '', createdTo: '' })
const page = ref({ content: [], number: 0, totalPages: 0, totalElements: 0 })
const msg = reactive({ ok: '', err: '' })

async function search(p = 0) {
  msg.err = ''
  try {
    page.value = await api.adminMembers({ ...cond, page: p })   // QueryDSL: dynamic where + group by + having
  } catch (e) {
    msg.err = e.message
  }
}

async function setStatus(m, status) {
  msg.ok = msg.err = ''
  try {
    await api.adminChangeStatus(m.id, status)
    await search(page.value.number)
  } catch (e) {
    msg.err = e.message
  }
}

async function dormant() {
  if (!confirm('장기 미접속 회원을 휴면 처리할까요?')) return
  const r = await api.adminDormant()   // JPQL bulk update
  msg.ok = `${r.updated}명을 휴면 처리했습니다.`
  await search(0)
}

onMounted(() => search(0))
</script>

<template>
  <form class="card" @submit.prevent="search(0)">
    <h3>회원 검색 <span class="tag">QueryDSL 동적 쿼리 + 집계</span></h3>
    <div class="row">
      <div><label>아이디</label><input v-model="cond.loginId" /></div>
      <div><label>이름</label><input v-model="cond.name" /></div>
      <div><label>상태</label>
        <select v-model="cond.status"><option value="">전체</option>
          <option>ACTIVE</option><option>LOCKED</option><option>DORMANT</option><option>WITHDRAWN</option></select></div>
      <div><label>총 잔액 이상</label><input v-model="cond.minTotalBalance" type="number" /></div>
    </div>
    <div class="row">
      <div><label>가입일 from</label><input v-model="cond.createdFrom" type="date" /></div>
      <div><label>가입일 to</label><input v-model="cond.createdTo" type="date" /></div>
      <button type="submit">검색</button>
      <button type="button" class="secondary" @click="dormant">휴면 일괄 처리</button>
    </div>
  </form>

  <div class="card">
    <div v-if="msg.err" class="msg err">{{ msg.err }}</div>
    <div v-if="msg.ok" class="msg ok">{{ msg.ok }}</div>
    <div>총 {{ page.totalElements }}명</div>
    <table>
      <thead><tr><th>ID</th><th>아이디</th><th>이름</th><th>이메일</th><th>상태</th><th class="num">계좌수</th><th class="num">총 잔액</th><th>가입일</th><th></th></tr></thead>
      <tbody>
        <tr v-for="m in page.content" :key="m.id">
          <td>{{ m.id }}</td><td>{{ m.loginId }}</td><td>{{ m.name }}</td><td>{{ m.email }}</td>
          <td><span class="badge" :class="m.status">{{ m.status }}</span></td>
          <td class="num">{{ m.accountCount }}</td><td class="num">{{ won(m.totalBalance) }}</td><td>{{ dt(m.createdAt) }}</td>
          <td>
            <button v-if="m.status !== 'ACTIVE'" class="secondary" @click="setStatus(m, 'ACTIVE')">활성화</button>
            <button v-else class="danger" @click="setStatus(m, 'LOCKED')">잠금</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pager" v-if="page.totalPages > 1">
      <button class="secondary" :disabled="page.number === 0" @click="search(page.number - 1)">이전</button>
      {{ page.number + 1 }} / {{ page.totalPages }}
      <button class="secondary" :disabled="page.number + 1 >= page.totalPages" @click="search(page.number + 1)">다음</button>
    </div>
  </div>
</template>
