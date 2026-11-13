<script setup>
import { computed, ref } from 'vue'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import CodeCard from '../../components/CodeCard.vue'
import { api, dt, won } from '../../api/http'

const qc = useQueryClient()

// 1) cached query: data is "fresh" for 10s -> re-visiting this screen does NOT call the server again
const accounts = useQuery({
  queryKey: ['accounts'],
  queryFn: api.accounts,
  staleTime: 10_000
})

// 2) mutation + invalidation: after a deposit, mark the cached list as outdated -> automatic refetch
const amount = ref(1000)
const deposit = useMutation({
  mutationFn: ({ id, amount }) => api.deposit(id, { amount, memo: 'vue-query 데모' }),
  onSuccess: () => {
    qc.invalidateQueries({ queryKey: ['accounts'] })
    qc.invalidateQueries({ queryKey: ['tx'] })
  }
})

// 3) paged query: the key contains the page -> every page is cached separately
const page = ref(0)
const tx = useQuery({
  queryKey: ['tx', page],
  queryFn: () => api.searchTx({ page: page.value, size: 5 }),
  placeholderData: keepPreviousData      // keep showing the old page while the next one loads
})

// 4) failing query with automatic retries
const failing = useQuery({
  queryKey: ['always-fails'],
  queryFn: () => api.demoFail(500),
  retry: 2,
  retryDelay: 700,
  enabled: false                         // only runs when we call refetch()
})

const updatedAt = computed(() => (accounts.dataUpdatedAt.value ? new Date(accounts.dataUpdatedAt.value).toLocaleTimeString() : '-'))

const code = `const accounts = useQuery({ queryKey: ['accounts'], queryFn: api.accounts, staleTime: 10_000 })
// accounts.data / isPending / isFetching / error / refetch()

const qc = useQueryClient()
const deposit = useMutation({
  mutationFn: ({ id, amount }) => api.deposit(id, { amount }),
  onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts'] })   // 목록 자동 재조회
})
deposit.mutate({ id, amount })

// 페이지별 캐시 + 이전 페이지 유지
useQuery({ queryKey: ['tx', page], queryFn: () => api.searchTx({ page: page.value }), placeholderData: keepPreviousData })

// 실패 시 자동 재시도
useQuery({ queryKey: ['x'], queryFn, retry: 2, retryDelay: 700 })`
</script>

<template>
  <CodeCard title="⑤ TanStack Query (vue-query)" desc="서버 데이터를 캐시로 관리합니다. 다른 메뉴에 갔다가 10초 안에 돌아오면 서버 호출 없이 즉시 표시되고, 창을 다시 클릭하면 오래된 데이터는 조용히 갱신됩니다." :code="code">
    <h4>1) 캐시된 조회 + mutation 후 자동 갱신</h4>
    <div class="tag">
      상태: {{ accounts.status.value }} · {{ accounts.isFetching.value ? '서버에서 가져오는 중' : '대기' }} ·
      마지막 갱신 {{ updatedAt }} · {{ accounts.isStale.value ? 'stale(오래됨)' : 'fresh(신선)' }}
    </div>
    <div v-if="accounts.isPending.value" class="msg">불러오는 중…</div>
    <div v-else-if="accounts.error.value" class="msg err">{{ accounts.error.value.message }}</div>
    <template v-else>
      <div>총 자산 <b>{{ won(accounts.data.value.totalBalance) }}</b></div>
      <table>
        <tbody>
          <tr v-for="a in accounts.data.value.accounts" :key="a.id">
            <td>{{ a.accountNumber }}</td><td class="num">{{ won(a.balance) }}</td>
            <td><button :disabled="a.status !== 'ACTIVE' || deposit.isPending.value" @click="deposit.mutate({ id: a.id, amount: Number(amount) })">+ 입금</button></td>
          </tr>
        </tbody>
      </table>
    </template>
    <div class="row" style="margin-top: 8px">
      <div><label>입금액</label><input v-model="amount" type="number" min="1" /></div>
      <button class="secondary" @click="accounts.refetch()">수동 refetch</button>
      <button class="secondary" @click="qc.invalidateQueries({ queryKey: ['accounts'] })">캐시 무효화</button>
    </div>
    <div v-if="deposit.error.value" class="msg err">{{ deposit.error.value.message }}</div>

    <h4>2) 페이지별 캐시</h4>
    <div class="tag">{{ tx.isFetching.value ? '가져오는 중' : '완료' }} · 이미 본 페이지로 돌아가면 즉시 표시됩니다.</div>
    <table v-if="tx.data.value">
      <thead><tr><th>일시</th><th>계좌</th><th>구분</th><th class="num">금액</th></tr></thead>
      <tbody><tr v-for="t in tx.data.value.content" :key="t.id"><td>{{ dt(t.createdAt) }}</td><td>{{ t.accountNumber }}</td><td>{{ t.type }}</td><td class="num">{{ won(t.amount) }}</td></tr></tbody>
    </table>
    <div class="pager" v-if="tx.data.value">
      <button class="secondary" :disabled="page === 0" @click="page--">이전</button>
      {{ page + 1 }} / {{ tx.data.value.totalPages }}
      <button class="secondary" :disabled="page + 1 >= tx.data.value.totalPages" @click="page++">다음</button>
    </div>

    <h4>3) 실패 시 자동 재시도 (2회, 0.7초 간격)</h4>
    <button class="secondary" @click="failing.refetch()">항상 실패하는 API 호출</button>
    <div class="tag">실패 횟수 {{ failing.failureCount.value }} · {{ failing.isFetching.value ? '재시도 중…' : '' }}</div>
    <div v-if="failing.isError.value" class="msg err">최종 실패: {{ failing.error.value.message }}</div>
  </CodeCard>
</template>
