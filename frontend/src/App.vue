<template>
  <ErrorBoundary>
    <a-config-provider :locale="currentLocale" :theme="antdTheme">
      <!-- Skip to main content link for keyboard navigation -->
      <a class="skip-to-content" href="#main-content">Skip to main content</a>
      <!-- 页面导航进度条 -->
      <ProgressBar :loading="isNavigating" />
      <!-- 页面过渡动画 -->
      <router-view v-slot="{ Component, route }">
        <transition :name="transitionName" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </a-config-provider>
  </ErrorBoundary>
  <!-- 开发模式性能覆盖层（不影响生产环境） -->
  <PerformanceOverlay />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { theme as antdThemeConfig } from 'ant-design-vue'
import { useAppStore } from '@/store/modules/app'
import { useTheme } from '@/composables/useTheme'
import { usePageTransition } from '@/composables/usePageTransition'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'

// 性能覆盖层仅在开发模式加载，生产环境不会包含此组件
const PerformanceOverlay = import.meta.env.DEV
  ? defineAsyncComponent(() => import('@/components/PerformanceOverlay.vue'))
  : () => null

const appStore = useAppStore()
const { isDark } = useTheme()
const { transitionName, isNavigating } = usePageTransition()

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

/* 页面过渡动画 - 淡入淡出 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

/* 页面过渡动画 - 向左滑动（前进） */
.page-slide-left-enter-active,
.page-slide-left-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-slide-left-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.page-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 页面过渡动画 - 向右滑动（后退） */
.page-slide-right-enter-active,
.page-slide-right-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.page-slide-right-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
