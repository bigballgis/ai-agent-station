<template>
  <ErrorBoundary>
    <a-config-provider :locale="currentLocale" :theme="antdTheme">
      <!-- Skip to main content link for keyboard navigation -->
      <a class="skip-to-content" href="#main-content">Skip to main content</a>
      <router-view />
    </a-config-provider>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { theme as antdThemeConfig } from 'ant-design-vue'
import { useAppStore } from '@/store/modules/app'
import { useTheme } from '@/composables/useTheme'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'

const appStore = useAppStore()
const { isDark } = useTheme()

const currentLocale = computed(() => {
  return appStore.locale === 'zh-CN' ? zhCN : enUS
})

const antdTheme = computed(() => {
  return {
    algorithm: isDark.value ? antdThemeConfig.darkAlgorithm : antdThemeConfig.defaultAlgorithm,
  }
})
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
}
</style>
