import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // Backend de SIMULACIÓN (5D y colapso): las llamadas que empiezan con /sim/api
      // se envían al segundo backend y se les quita el "/sim" antes de llegar
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

