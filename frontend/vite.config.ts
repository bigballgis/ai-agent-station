import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import Inspector from 'vite-plugin-vue-inspector'

export default defineConfig({
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'antd': ['ant-design-vue', '@ant-design/icons-vue'],
          'chart': ['chart.js'],
          'i18n': ['vue-i18n'],
        },
      },
    },
  },
  plugins: [vue(), Inspector()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
