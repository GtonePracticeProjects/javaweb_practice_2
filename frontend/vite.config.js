import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // The browser talks to Vite; Vite forwards /api to Spring Boot (same origin => session cookie just works)
    proxy: { '/api': 'http://localhost:8080' }
  },
  build: {
    // `npm run build` puts the SPA into Spring Boot's static folder -> one runnable jar
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})
