import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import Inspector from 'vite-plugin-vue-inspector'

export default defineConfig({
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 520,
    minify: 'esbuild',
    target: 'es2020',
    // CDN 部署：确保所有构建产物文件名包含内容哈希，实现长期缓存
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        manualChunks(id) {
          // Vue 核心框架
          if (id.includes('node_modules/vue/') || id.includes('node_modules/@vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) {
            return 'vue-vendor'
          }
          // Ant Design Vue - 按功能分组拆分，避免单一大包
          if (id.includes('node_modules/ant-design-vue/')) {
            // 图标库单独分包（通常较大）
            if (id.includes('@ant-design/icons-vue')) {
              return 'antd-icons'
            }
            // 日期相关组件依赖 dayjs，单独分包
            if (id.includes('date-picker') || id.includes('calendar') || id.includes('time-picker')) {
              return 'antd-date'
            }
            // 其他 antd 组件归入核心包
            return 'antd-core'
          }
          // 图表库单独分包（仅在 Dashboard 等页面使用）
          if (id.includes('node_modules/chart.js/')) {
            return 'chart'
          }
          // 国际化库
          if (id.includes('node_modules/vue-i18n/')) {
            return 'i18n'
          }
          // dayjs 单独分包
          if (id.includes('node_modules/dayjs/')) {
            return 'dayjs'
          }
          // @ant-design/icons-vue 在 node_modules 根目录的情况
          if (id.includes('node_modules/@ant-design/icons-vue/')) {
            return 'antd-icons'
          }
        },
      },
    },
  },
  esbuild: {
    drop: ['console', 'debugger'],
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
