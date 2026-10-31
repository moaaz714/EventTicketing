import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend has no CORS config, so the dev server forwards /api to the
// Spring Cloud Gateway. Override the target with GATEWAY_URL if needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.GATEWAY_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
