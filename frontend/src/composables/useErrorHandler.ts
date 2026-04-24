import { ref, type Ref } from 'vue'
import { message, notification } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { AxiosError } from 'axios'
import { categorizeError, type ErrorCategory } from '@/utils/errorHandler'

export type ErrorHandlerType = ErrorCategory

interface ErrorHandlerOptions {
  /** 是否显示通知（默认 false，使用 message） */
  useNotification?: boolean
  /** 通知持续时间（秒） */
  duration?: number
  /** 自定义重试回调 */
  retryCallback?: () => void | Promise<void>
}

/**
 * useErrorHandler composable
 *
 * 提供统一的错误处理能力：
 * - handleError(error) - 处理任何错误并显示适当的 UI 反馈
 * - errorType ref - 当前错误类别
 * - errorMessage ref - 用户友好的错误消息
 * - retryCallback ref - 可重试时的回调函数
 */
export function useErrorHandler(options: ErrorHandlerOptions = {}) {
  const { t } = useI18n()
  const { useNotification = false, duration = 4.5, retryCallback: externalRetryCallback } = options

  const errorType = ref<ErrorHandlerType>('unknown') as Ref<ErrorHandlerType>
  const errorMessage = ref('')
  const retryCallback = ref<(() => void | Promise<void>) | null>(null)

  /**
   * 获取用户友好的错误消息
   */
  function getUserFriendlyMessage(error: unknown, category: ErrorHandlerType): string {
    // Axios 错误
    if (error && typeof error === 'object' && 'isAxiosError' in error) {
      const axiosError = error as AxiosError<{ message?: string }>
      const status = axiosError.response?.status
      const data = axiosError.response?.data

      if (status === 401) return t('common.error.sessionExpired')
      if (status === 403) return t('common.error.permissionDenied')
      if (status === 429) return t('common.error.rateLimit')
      if (status === 502 || status === 503) return t('common.error.serviceUnavailable')
      if (status && status >= 500) return t('common.error.serviceUnavailable')

      if (data?.message) return data.message
    }

    // Error 实例
    if (error instanceof Error) {
      switch (category) {
        case 'network':
          return navigator.onLine
            ? t('common.error.serverUnreachable')
            : t('common.error.offlineError')
        case 'auth':
          return t('common.error.sessionExpired')
        case 'validation':
          return error.message || t('password.errorCategoryMessages.validation')
        case 'runtime':
          return t('password.errorCategoryMessages.runtime')
        case 'chunk-load':
          return t('password.errorCategoryMessages.chunk-load')
        default:
          return error.message || t('password.errorCategoryMessages.unknown')
      }
    }

    // 字符串错误
    if (typeof error === 'string') {
      return error
    }

    return t('password.errorCategoryMessages.unknown')
  }

  /**
   * 判断错误是否可重试
   */
  function isRetryable(error: unknown): boolean {
    if (error && typeof error === 'object' && 'isAxiosError' in error) {
      const axiosError = error as AxiosError
      const status = axiosError.response?.status
      return status === 429 || status === 502 || status === 503 || (status ?? 0) >= 500 || !axiosError.response
    }
    return false
  }

  /**
   * 处理错误并显示 UI 反馈
   */
  function handleError(error: unknown, customRetryCallback?: () => void | Promise<void>) {
    const category = categorizeError(error)
    errorType.value = category
    errorMessage.value = getUserFriendlyMessage(error, category)

    // 设置重试回调
    if (isRetryable(error)) {
      retryCallback.value = customRetryCallback || externalRetryCallback || null
    } else {
      retryCallback.value = null
    }

    // 显示错误消息
    if (useNotification) {
      notification.error({
        message: t(`password.errorCategories.${category}`),
        description: errorMessage.value,
        duration,
      })
    } else {
      message.error(errorMessage.value)
    }

    // 记录到控制台
    console.error(`[ErrorHandler] [${category}]`, error)

    return { category, message: errorMessage.value }
  }

  /**
   * 清除错误状态
   */
  function clearError() {
    errorType.value = 'unknown'
    errorMessage.value = ''
    retryCallback.value = null
  }

  /**
   * 执行重试
   */
  async function retry() {
    if (retryCallback.value) {
      clearError()
      await retryCallback.value()
    }
  }

  return {
    errorType,
    errorMessage,
    retryCallback,
    handleError,
    clearError,
    retry,
  }
}
