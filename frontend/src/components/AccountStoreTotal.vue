<script setup>
import { useAccountStore } from '../stores/accounts'
import { won } from '../api/http'

// No props, no events: it just reads the same store as component A.
const store = useAccountStore()
</script>

<template>
  <div>
    <h4>컴포넌트 B: 총 자산 (A와 형제 컴포넌트)</h4>
    <div class="balance">{{ won(store.totalBalance) }}</div>
    <div class="tag">활성 계좌 {{ store.activeCount }}개 · API 호출 {{ store.apiCalls }}회 ·
      마지막 로드 {{ store.loadedAt ? store.loadedAt.toLocaleTimeString() : '-' }}</div>
  </div>
</template>
