<template>
  <div>
    <!-- 正常内容 -->
    <div v-if="!error" ref="contentRef">
      <slot />
    </div>

    <!-- 错误显示（保留上次成功内容作为背景） -->
    <div v-else class="error-boundary p-6 text-center" role="alert">
      <!-- 上次成功内容（半透明显示） -->
      <div v-if="hasLastContent" class="mb-4 opacity-30 pointer-events-none select-none overflow-hidden max-h-48">
        <div class="text-xs text-neutral-400 dark:text-neutral-500 mb-2">
          {{ t('password.lastSuccessfulContent') }}
        </div>
        <div class="text-sm text-neutral-400 dark:text-neutral-500 line-clamp-4">
          {{ lastContentText }}
        </div>
      </div>

      <!-- 错误分类标签 -->
      <div class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium mb-3"
        :class="categoryStyle">
        <span class="w-1.5 h-1.5 rounded-full" :class="categoryDot" />
        {{ t(`password.errorCategories.${errorCategory}`) }}
      </div>

      <!-- 错误标题 -->
      <h3 class="text-lg font-semibold text-red-500 dark:text-red-400 mb-2">{{ t('password.renderError') }}</h3>

      <!-- 错误消息 -->
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-4">{{ categoryMessage }}</p>

      <!-- 自动重试倒计时（仅网络错误） -->
      <div v-if="errorCategory === 'network' && retryCountdown > 0" class="mb-4">
        <span class="text-xs text-neutral-400 dark:text-neutral-500">
          {{ t('password.autoRetryIn', { seconds: retryCountdown }) }}
        </span>
      </div>

      <!-- 操作按钮 -->
      <div class="flex items-center justify-center gap-3">
        <button
          @click="reset"
          class="btn btn-primary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
        >
          {{ t('password.retry') }}
        </button>
        <button
          @click="reportError"
          class="btn btn-secondary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
        >
          {{ t('password.reportError') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'

export type ErrorCategory = 'network' | 'auth' | 'business' | 'unknown'

const { t } = useI18n()
const error = ref<Error | null>(null)
const errorCategory = ref<ErrorCategory>('unknown')
const retryCountdown = ref(0)
const hasLastContent = ref(false)
const lastContentText = ref('')
const contentRef = ref<HTMLElement | null>(null)

let retryTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

/**
 * 根据错误信息分类
 */
function categorizeError(err: Error): ErrorCategory {
  const msg = err.message.toLowerCase()
  void (err.stack || '')

  // 网络错误
  if (
    msg.includes('network') ||
    msg.includes('fetch') ||
    msg.includes('timeout') ||
    msg.includes('net::') ||
    msg.includes('failed to fetch') ||
    msg.includes('err_network') ||
    msg.includes('err_connection')
  ) {
    return 'network'
  }

  // 认证错误
  if (
    msg.includes('401') ||
    msg.includes('403') ||
    msg.includes('unauthorized') ||
    msg.includes('forbidden') ||
    msg.includes('token') ||
    msg.includes('permission') ||
    msg.includes('认证') ||
    msg.includes('权限')
  ) {
    return 'auth'
  }

  // 业务错误
  if (
    msg.includes('400') ||
    msg.includes('409') ||
    msg.includes('422') ||
    msg.includes('business') ||
    msg.includes('validation') ||
    msg.includes('参数') ||
    msg.includes('校验')
  ) {
    return 'business'
  }

  return 'unknown'
}

/**
 * 分类对应的样式
 */
const categoryStyle = computed(() => {
  const styles: Record<ErrorCategory, string> = {
    network: 'bg-orange-50 dark:bg-orange-950/30 text-orange-600 dark:text-orange-400',
    auth: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
    business: 'bg-yellow-50 dark:bg-yellow-950/30 text-yellow-600 dark:text-yellow-400',
    unknown: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400',
  }
  return styles[errorCategory.value]
})

const categoryDot = computed(() => {
  const styles: Record<ErrorCategory, string> = {
    network: 'bg-orange-500',
    auth: 'bg-red-500',
    business: 'bg-yellow-500',
    unknown: 'bg-neutral-400',
  }
  return styles[errorCategory.value]
})

/**
 * 根据分类生成用户友好的错误消息
 */
const categoryMessage = computed(() => {
  const key = `password.errorCategoryMessages.${errorCategory.value}`
  const template = t(key)
  if (errorCategory.value === 'business') {
    return template.replace('{message}', error.value?.message || '')
  }
  return template
})

/**
 * 保存当前内容快照
 */
function saveContentSnapshot() {
  if (contentRef.value) {
    const text = contentRef.value.textContent?.trim()
    if (text && text.length > 0) {
      hasLastContent.value = true
      lastContentText.value = text.substring(0, 200)
    }
  }
}

/**
 * 网络错误自动重试（3秒倒计时）
 */
function startAutoRetry() {
  retryCountdown.value = 3
  countdownTimer = setInterval(() => {
    retryCountdown.value--
    if (retryCountdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      reset()
    }
  }, 1000)
}

/**
 * 清除定时器
 */
function clearTimers() {
  if (retryTimer) {
    clearTimeout(retryTimer)
    retryTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  retryCountdown.value = 0
}

/**
 * 报告错误（输出到控制台供调试）
 */
function reportError() {
  const report = {
    category: errorCategory.value,
    message: error.value?.message,
    stack: error.value?.stack,
    url: window.location.href,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
  }
  console.error('[ErrorBoundary] Error Report:', JSON.stringify(report, null, 2))
  console.table(report)
}

onErrorCaptured((err, _instance, info) => {
  // 保存当前内容快照
  saveContentSnapshot()

  error.value = err
  errorCategory.value = categorizeError(err)

  console.warn(`[ErrorBoundary] Captured ${errorCategory.value} error:`, err.message, '\nInfo:', info)

  // 网络错误自动重试
  if (errorCategory.value === 'network') {
    clearTimers()
    startAutoRetry()
  }

  return false
})

function reset() {
  clearTimers()
  error.value = null
  errorCategory.value = 'unknown'
}

onUnmounted(() => {
  clearTimers()
})
</script>
