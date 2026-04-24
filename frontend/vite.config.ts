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
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'antd-core': [
            'ant-design-vue/es/layout',
            'ant-design-vue/es/button',
            'ant-design-vue/es/input',
            'ant-design-vue/es/form',
            'ant-design-vue/es/modal',
            'ant-design-vue/es/message',
            'ant-design-vue/es/notification',
            'ant-design-vue/es/grid',
            'ant-design-vue/es/typography',
            'ant-design-vue/es/space',
            'ant-design-vue/es/config-provider',
            'ant-design-vue/es/icon',
          ],
          'antd-data-entry': [
            'ant-design-vue/es/table',
            'ant-design-vue/es/pagination',
            'ant-design-vue/es/list',
            'ant-design-vue/es/cascader',
            'ant-design-vue/es/transfer',
          ],
          'antd-data-selection': [
            'ant-design-vue/es/select',
            'ant-design-vue/es/tree',
            'ant-design-vue/es/checkbox',
            'ant-design-vue/es/radio',
            'ant-design-vue/es/switch',
          ],
          'antd-feedback': [
            'ant-design-vue/es/drawer',
            'ant-design-vue/es/popover',
            'ant-design-vue/es/tooltip',
            'ant-design-vue/es/dropdown',
            'ant-design-vue/es/menu',
            'ant-design-vue/es/tabs',
            'ant-design-vue/es/collapse',
            'ant-design-vue/es/steps',
            'ant-design-vue/es/breadcrumb',
            'ant-design-vue/es/alert',
          ],
          'antd-visual': [
            'ant-design-vue/es/date-picker',
            'ant-design-vue/es/calendar',
            'ant-design-vue/es/time-picker',
            'ant-design-vue/es/slider',
            'ant-design-vue/es/progress',
            'ant-design-vue/es/badge',
            'ant-design-vue/es/tag',
            'ant-design-vue/es/statistic',
            'ant-design-vue/es/avatar',
            'ant-design-vue/es/card',
            'ant-design-vue/es/descriptions',
          ],
          'chart': ['chart.js', 'chart.js/auto'],
          'i18n': ['vue-i18n'],
          'dayjs': ['dayjs'],
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
