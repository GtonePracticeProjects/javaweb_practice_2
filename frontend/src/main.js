import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'
import './styles.css'

createApp(App)
  .use(createPinia())        // demo: shared state store
  .use(VueQueryPlugin)       // demo: server-state cache (TanStack Query)
  .use(router)
  .mount('#app')
