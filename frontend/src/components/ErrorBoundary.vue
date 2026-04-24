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
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-2">{{ categoryMessage }}</p>

      <!-- 恢复建议 -->
      <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-3">
        {{ recoverySuggestion }}
      </p>

      <!-- 最后发生时间 -->
      <div v-if="lastOccurrenceTime" class="mb-3">
        <span class="text-xs text-neutral-400 dark:text-neutral-500">
          {{ t('password.lastOccurrence') }}: {{ lastOccurrenceTime }}
        </span>
      </div>

      <!-- 自动重试倒计时（网络错误和 chunk-load 错误） -->
      <div v-if="shouldAutoRetry && retryCountdown > 0" class="mb-4">
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
          v-if="errorCategory === 'chunk-load'"
          @click="reloadPage"
          class="btn btn-secondary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
        >
          {{ t('password.reloadPage') }}
        </button>
        <button
          @click="copyErrorDetails"
          class="btn btn-secondary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
        >
          {{ copySuccess ? t('common.copiedToClipboard') : t('password.copyErrorDetails') }}
        </button>
        <button
          @click="reportError"
          class="btn btn-secondary focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:outline-none"
        >
          {{ t('password.reportError') }}
        </button>
      </div>

      <!-- 可折叠的错误详情 -->
      <div class="mt-4 text-left">
        <button
          @click="showDetails = !showDetails"
          class="text-xs text-neutral-400 dark:text-neutral-500 hover:text-neutral-600 dark:hover:text-neutral-300 transition-colors"
        >
          {{ showDetails ? t('password.hideDetails') : t('password.showDetails') }}
        </button>
        <div v-if="showDetails" class="mt-2 p-3 rounded-lg bg-neutral-100 dark:bg-neutral-800 text-xs text-neutral-500 dark:text-neutral-400 font-mono break-all max-h-48 overflow-auto">
          <div>Message: {{ error?.message }}</div>
          <div v-if="error?.stack">Stack: {{ error.stack }}</div>
          <div>Category: {{ errorCategory }}</div>
          <div>URL: {{ currentUrl }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import service from '@/utils/request'

export type ErrorCategory = 'network' | 'auth' | 'validation' | 'runtime' | 'chunk-load' | 'unknown'

const { t } = useI18n()
const error = ref<Error | null>(null)
const errorCategory = ref<ErrorCategory>('unknown')
const retryCountdown = ref(0)
const hasLastContent = ref(false)
const lastContentText = ref('')
const contentRef = ref<HTMLElement | null>(null)
const lastOccurrenceTime = ref('')
const showDetails = ref(false)
const copySuccess = ref(false)
const currentUrl = ref('')

let retryTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
let copyResetTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 根据错误信息分类
 */
function categorizeError(err: Error): ErrorCategory {
  const msg = err.message.toLowerCase()
  const stack = (err.stack || '').toLowerCase()
  void stack

  // Chunk-load 错误 (lazy loading failures)
  if (
    msg.includes('chunk') ||
    msg.includes('loading chunk') ||
    msg.includes('failed to fetch dynamically imported module') ||
    msg.includes('loading css chunk') ||
    msg.includes('importing a module script failed') ||
    stack.includes('chunk')
  ) {
    return 'chunk-load'
  }

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

  // 验证错误
  if (
    msg.includes('400') ||
    msg.includes('422') ||
    msg.includes('validation') ||
    msg.includes('参数') ||
    msg.includes('校验') ||
    msg.includes('invalid') ||
    msg.includes('required')
  ) {
    return 'validation'
  }

  // 运行时错误 (TypeError, ReferenceError, etc.)
  if (
    msg.includes('typeerror') ||
    msg.includes('referenceerror') ||
    msg.includes('rangeerror') ||
    msg.includes('syntaxerror') ||
    msg.includes('cannot read') ||
    msg.includes('is not a function') ||
    msg.includes('is not defined') ||
    msg.includes('is null') ||
    msg.includes('is undefined') ||
    msg.includes('maximum call stack')
  ) {
    return 'runtime'
  }

  // 业务错误
  if (
    msg.includes('409') ||
    msg.includes('business')
  ) {
    return 'validation'
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
    validation: 'bg-yellow-50 dark:bg-yellow-950/30 text-yellow-600 dark:text-yellow-400',
    runtime: 'bg-purple-50 dark:bg-purple-950/30 text-purple-600 dark:text-purple-400',
    'chunk-load': 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    unknown: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400',
  }
  return styles[errorCategory.value]
})

const categoryDot = computed(() => {
  const styles: Record<ErrorCategory, string> = {
    network: 'bg-orange-500',
    auth: 'bg-red-500',
    validation: 'bg-yellow-500',
    runtime: 'bg-purple-500',
    'chunk-load': 'bg-amber-500',
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
  if (errorCategory.value === 'validation') {
    return template.replace('{message}', error.value?.message || '')
  }
  return template
})

/**
 * 根据错误分类生成恢复建议
 */
const recoverySuggestion = computed(() => {
  const suggestions: Record<ErrorCategory, string> = {
    network: t('password.recoverySuggestions.network'),
    auth: t('password.recoverySuggestions.auth'),
    validation: t('password.recoverySuggestions.validation'),
    runtime: t('password.recoverySuggestions.runtime'),
    'chunk-load': t('password.recoverySuggestions.chunkLoad'),
    unknown: t('password.recoverySuggestions.unknown'),
  }
  return suggestions[errorCategory.value]
})

/**
 * 是否应该自动重试
 */
const shouldAutoRetry = computed(() => {
  return errorCategory.value === 'network' || errorCategory.value === 'chunk-load'
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
 * 自动重试（3秒倒计时）
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
  if (copyResetTimer) {
    clearTimeout(copyResetTimer)
    copyResetTimer = null
  }
  retryCountdown.value = 0
}

/**
 * 复制错误详情到剪贴板
 */
async function copyErrorDetails() {
  if (!error.value) return

  const details = {
    category: errorCategory.value,
    message: error.value.message,
    stack: error.value.stack,
    url: window.location.href,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
  }

  const text = JSON.stringify(details, null, 2)

  try {
    await navigator.clipboard.writeText(text)
    copySuccess.value = true
    copyResetTimer = setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch {
    // Fallback for older browsers
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      copySuccess.value = true
      copyResetTimer = setTimeout(() => {
        copySuccess.value = false
      }, 2000)
    } catch {
      // Silently fail
    }
    document.body.removeChild(textarea)
  }
}

/**
 * 报告错误到后端（如果 API 可用）
 */
async function reportError() {
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

  // Try to report to backend if API is available
  try {
    await service.post('/v1/error-reports', report, { timeout: 5000 })
  } catch {
    // Silently fail - backend may not have this endpoint
    console.warn('[ErrorBoundary] Failed to report error to backend')
  }
}

/**
 * 重新加载页面（用于 chunk-load 错误）
 */
function reloadPage() {
  window.location.reload()
}

onErrorCaptured((err, _instance, info) => {
  // 保存当前内容快照
  saveContentSnapshot()

  error.value = err
  errorCategory.value = categorizeError(err)
  lastOccurrenceTime.value = new Date().toLocaleString()
  currentUrl.value = window.location.href

  console.warn(`[ErrorBoundary] Captured ${errorCategory.value} error:`, err.message, '\nInfo:', info)

  // 网络错误和 chunk-load 错误自动重试
  if (shouldAutoRetry.value) {
    clearTimers()
    startAutoRetry()
  }

  return false
})

function reset() {
  clearTimers()
  error.value = null
  errorCategory.value = 'unknown'
  showDetails.value = false
  lastOccurrenceTime.value = ''
}

onUnmounted(() => {
  clearTimers()
})
</script>
