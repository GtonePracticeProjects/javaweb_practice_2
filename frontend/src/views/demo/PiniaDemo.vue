<script setup>
import { onMounted } from 'vue'
import CodeCard from '../../components/CodeCard.vue'
import AccountStoreList from '../../components/AccountStoreList.vue'
import AccountStoreTotal from '../../components/AccountStoreTotal.vue'
import { useAccountStore } from '../../stores/accounts'

const store = useAccountStore()
onMounted(() => {
  if (!store.loadedAt) store.load()   // already loaded? other screens reuse it
})

const code = `// src/stores/accounts.js
export const useAccountStore = defineStore('accounts', {
  state: () => ({ accounts: [], totalBalance: 0 }),
  getters: { activeCount: (s) => s.accounts.filter(a => a.status === 'ACTIVE').length },
  actions: {
    async load() { const d = await api.accounts(); this.accounts = d.accounts; this.totalBalance = d.totalBalance },
    async deposit(id, amount) {
      const updated = await api.deposit(id, { amount })
      // 변경된 계좌만 교체 → 다른 컴포넌트(총 자산)도 자동으로 갱신됨
    }
  }
})

// 어느 컴포넌트에서든
const store = useAccountStore()`
</script>

<template>
  <CodeCard title="④ Pinia 스토어" desc="두 컴포넌트는 부모-자식이 아니라 형제입니다. props/emit 없이 같은 스토어를 읽기 때문에, A에서 입금하면 B의 총 자산이 바로 바뀝니다." :code="code">
    <div v-if="store.loading" class="msg">불러오는 중…</div>
    <div v-if="store.error" class="msg err">{{ store.error }}</div>
    <div class="row" style="align-items: flex-start">
      <AccountStoreList />
      <AccountStoreTotal />
    </div>
    <p><button class="secondary" @click="store.load()">서버에서 다시 불러오기</button></p>
    <p class="desc">다른 메뉴로 이동했다가 돌아와도 스토어에 데이터가 남아 있습니다. (새로고침하면 메모리 상태는 초기화됩니다.)</p>
  </CodeCard>
</template>
