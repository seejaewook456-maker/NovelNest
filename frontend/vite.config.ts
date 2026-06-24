import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // /api 로 시작하는 요청을 백엔드(8080)로 전달 — CORS 우회
      '/api': 'http://localhost:8080',
    },
  },
})
