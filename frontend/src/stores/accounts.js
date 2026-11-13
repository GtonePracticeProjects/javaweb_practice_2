import { defineStore } from 'pinia'
import { api } from '../api/http'

/** Shared account state: every component that calls useAccountStore() sees the same data. */
export const useAccountStore = defineStore('accounts', {
  state: () => ({
    accounts: [],
    totalBalance: 0,
    loading: false,
    error: '',
    loadedAt: null,
    apiCalls: 0
  }),
  getters: {
    activeCount: (s) => s.accounts.filter((a) => a.status === 'ACTIVE').length
  },
  actions: {
    async load() {
      this.loading = true
      this.error = ''
      try {
        const data = await api.accounts()
        this.accounts = data.accounts
        this.totalBalance = data.totalBalance
        this.loadedAt = new Date()
        this.apiCalls++
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    },
    /** Updates the one changed account in place instead of reloading the whole list. */
    async deposit(id, amount) {
      const updated = await api.deposit(id, { amount, memo: 'Pinia 데모 입금' })
      const i = this.accounts.findIndex((a) => a.id === id)
      const diff = Number(updated.balance) - Number(this.accounts[i].balance)
      this.accounts[i] = updated
      this.totalBalance = Number(this.totalBalance) + diff
      this.apiCalls++
    }
  }
})
