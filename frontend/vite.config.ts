import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [tailwindcss(), react()],
  server: {
    host: true,
    allowedHosts: ['paqrap-g9i.duckdns.org'],
    port: 5173,
    hmr: {
      host: 'paqrap-g9i.duckdns.org',
      clientPort: 443,
      protocol: 'wss'
    },
    proxy: {
      // Backend de SIMULACIÓN (5D y colapso)
      '/sim/api': {
        target: process.env.API_SIM_URL || 'http://app-simulacion:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/sim/, ''),
      },
      // Backend de OPERACIÓN (operación día a día y registro)
      '/api': {
        target: process.env.API_URL || 'http://app:8080',
        changeOrigin: true,
      },
    },
  },
})
