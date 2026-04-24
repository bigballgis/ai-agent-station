import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types/common'
import i18n from '@/locales'
import { getErrorDisplayMessage } from '@/utils/errorMessageMapper'
import { addPendingRequest, removePendingRequest, cancelAllPendingRequests } from '@/utils/requestManager'
import { circuitBreaker } from '@/utils/circuitBreaker'
import { globalPerformanceTracker } from '@/composables/usePerformanceMark'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const DEFAULT_TIMEOUT = 30000

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: DEFAULT_TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== 增强重试机制（指数退避 + 断路器） ====================

/** 5xx 错误最大重试次数 */
const MAX_RETRY_5XX = 3
/** 网络错误最大重试次数 */
const MAX_RETRY_NETWORK = 2
/** 5xx 重试初始延迟(ms) */
const RETRY_DELAY_5XX_MS = 1000
/** 网络错误重试初始延迟(ms) */
const RETRY_DELAY_NETWORK_MS = 1500
/** 退避乘数 */
const RETRY_MULTIPLIER = 2
/** 最大退避延迟(ms) */
const MAX_RETRY_DELAY_MS = 10000

/**
 * 延迟指定毫秒
 */
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 计算指数退避延迟
 * @param attempt 当前重试次数（从1开始）
 * @param baseDelay 基础延迟(ms)
 * @returns 延迟毫秒数
 */
function calculateBackoff(attempt: number, baseDelay: number): number {
  const delayMs = baseDelay * Math.pow(RETRY_MULTIPLIER, attempt - 1)
  return Math.min(delayMs, MAX_RETRY_DELAY_MS)
}

/**
 * 判断错误是否为网络错误（无响应）
 */
function isNetworkError(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false
  const err = error as { response?: unknown; code?: string }
  return !err.response && (
    err.code === 'ERR_NETWORK' ||
    err.code === 'ECONNABORTED' ||
    err.code === 'ECONNRESET' ||
    err.code === 'ETIMEDOUT' ||
    err.code === 'ERR_CANCELED'
  )
}

/**
 * 判断错误是否可重试
 * - 网络异常（无响应）可重试（最多2次）
 * - 5xx 服务端错误可重试（最多3次）
 * - 429 限流错误可重试（按 Retry-After 头等待，最多3次）
 * - 4xx 错误（除429外）不重试
 */
function isRetryableError(error: unknown): boolean {
  // 网络错误可重试
  if (isNetworkError(error)) return true

  if (!error || typeof error !== 'object' || !('response' in error)) {
    return false
  }
  const response = (error as { response?: { status?: number } }).response
  if (!response) return false
  const status = response.status ?? 0
  // 5xx 和 429 可重试，4xx（除429）不重试
  return status >= 500 || status === 429
}

/**
 * 获取当前错误类型的最大重试次数
 */
function getMaxRetryCount(error: unknown): number {
  if (isNetworkError(error)) return MAX_RETRY_NETWORK
  return MAX_RETRY_5XX
}

/**
 * 获取当前错误类型的基础延迟
 */
function getBaseDelay(error: unknown): number {
  if (isNetworkError(error)) return RETRY_DELAY_NETWORK_MS
  return RETRY_DELAY_5XX_MS
}

/**
 * 从 429 响应中获取 Retry-After 值（秒）
 */
function getRetryAfterSeconds(error: unknown): number {
  if (!error || typeof error !== 'object' || !('response' in error)) return RETRY_DELAY_5XX_MS / 1000
  const response = (error as { response?: { headers?: Record<string, string | undefined> } }).response
  const retryAfter = response?.headers?.['retry-after']
  if (!retryAfter) return RETRY_DELAY_5XX_MS / 1000
  const seconds = parseInt(retryAfter, 10)
  if (!isNaN(seconds) && seconds > 0 && seconds <= 60) return seconds
  // Retry-After 也可能是 HTTP 日期格式，简单处理
  return RETRY_DELAY_5XX_MS / 1000
}

// ==================== Token 自动刷新机制 ====================

let isRefreshing = false
let failedQueue: Array<{ resolve: (token: string) => void; reject: (error: unknown) => void }> = []

/**
 * 将等待中的请求重新发送
 */
function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else if (token) {
      resolve(token)
    } else {
      reject(new Error('No token available'))
    }
  })
  failedQueue = []
}

/**
 * 使用 Refresh Token 获取新的 Access Token
 */
async function refreshAccessToken(): Promise<string> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    throw new Error('No refresh token available')
  }

  const response = await axios.post(`${BASE_URL}/v1/auth/refresh`, {
    refreshToken
  })

  const data = response.data?.data || response.data
  if (data.token) {
    localStorage.setItem('token', data.token)
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken)
    }
    return data.token
  }
  throw new Error('Refresh failed')
}

/**
 * 清除认证信息并跳转登录页
 */
function clearAuthAndRedirect() {
  message.warning(i18n.global.t('common.error.sessionExpired'))
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
  // 避免重复跳转
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// ==================== 请求拦截器（去重 + AbortController + 断路器） ====================

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 性能追踪：记录 API 请求开始时间
    const apiMarkName = `api:${config.method?.toUpperCase() || 'GET'}:${config.url || 'unknown'}`
    globalPerformanceTracker.startMark(apiMarkName)
    // 将标记名存储在 config 上，以便响应拦截器使用
    ;(config as InternalAxiosRequestConfig & { _perfMarkName?: string })._perfMarkName = apiMarkName

    // 断路器检查：如果断路器已打开，直接拒绝请求
    if (!circuitBreaker.canProceed(config.url)) {
      const controller = new AbortController()
      config.signal = controller.signal
      controller.abort()
      console.warn(
        `[CircuitBreaker] 请求被断路器拒绝: ${config.method?.toUpperCase()} ${config.url}`
      )
    }

    // 请求去重检查
    const isDeduped = addPendingRequest(config)
    if (isDeduped) {
      // 请求被去重，中止当前请求（返回已存在的请求）
      const controller = new AbortController()
      config.signal = controller.signal
      controller.abort()
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器（含 Token 自动刷新 + 增强重试 + 断路器） ====================

service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 请求完成，从 pending map 中移除
    removePendingRequest(response.config)

    // 性能追踪：记录 API 请求结束时间
    const perfMarkName = (response.config as InternalAxiosRequestConfig & { _perfMarkName?: string })._perfMarkName
    if (perfMarkName) {
      globalPerformanceTracker.endMark(perfMarkName)
    }

    // 断路器：记录成功
    circuitBreaker.recordSuccess(response.config.url)

    const res = response.data as ApiResponse
    if (res.code !== 200 && res.code !== 0) {
      const errorMsg = getErrorDisplayMessage(
        res as unknown as Record<string, unknown>,
        res.message || 'Error'
      )
      message.error(errorMsg)
      return Promise.reject(new Error(errorMsg))
    }
    // 返回 response.data，保留 ApiResponse<T> 结构供调用方使用
    return response.data as unknown as AxiosResponse
  },
  async (error) => {
    // 请求完成（无论成功失败），从 pending map 中移除
    if (error.config) {
      removePendingRequest(error.config)

      // 性能追踪：记录 API 请求结束时间（即使失败）
      const perfMarkName = (error.config as InternalAxiosRequestConfig & { _perfMarkName?: string })._perfMarkName
      if (perfMarkName) {
        globalPerformanceTracker.endMark(perfMarkName)
      }
    }

    // 被去重中止的请求，静默处理
    if (error.code === 'ERR_CANCELED' || axios.isCancel(error)) {
      return Promise.reject(error)
    }

    // 被断路器中止的请求，静默处理
    if (error.config?.signal?.aborted && !error.response) {
      return Promise.reject(error)
    }

    const originalRequest = error.config
    const status = error.response?.status
    const retryCount = originalRequest._retryCount ?? 0
    const maxRetries = getMaxRetryCount(error)

    // 自动重试逻辑（5xx + 429 + 网络错误，指数退避）
    if (isRetryableError(error) && retryCount < maxRetries) {
      let waitMs: number

      if (status === 429) {
        // 429 限流：按 Retry-After 头等待
        const retryAfterSec = getRetryAfterSeconds(error)
        waitMs = retryAfterSec * 1000
        message.warning(i18n.global.t('common.error.rateLimit'))
      } else {
        // 5xx 或网络错误：指数退避
        const baseDelay = getBaseDelay(error)
        waitMs = calculateBackoff(retryCount + 1, baseDelay)
      }

      originalRequest._retryCount = retryCount + 1
      console.warn(
        `[Retry] ${originalRequest.method?.toUpperCase()} ${originalRequest.url} retry #${originalRequest._retryCount}/${maxRetries}, waiting ${waitMs}ms (backoff)`
      )
      await delay(waitMs)
      return service(originalRequest)
    }

    // 重试耗尽后，记录断路器失败（仅 5xx 和网络错误）
    if (isRetryableError(error) && retryCount >= maxRetries) {
      circuitBreaker.recordFailure(originalRequest.url)
    }

    // Token 过期: 尝试自动刷新
    if (status === 401 && !originalRequest._retry) {
      // 登录接口本身 401 不刷新
      if (originalRequest.url?.includes('/v1/auth/login')) {
        clearAuthAndRedirect()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 正在刷新中，将请求加入等待队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return service(originalRequest)
        }).catch(err => {
          return Promise.reject(err)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const newToken = await refreshAccessToken()
        processQueue(null, newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return service(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearAuthAndRedirect()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 禁止访问: 显示权限不足消息（不自动跳转登录页）
    if (status === 403) {
      message.error(i18n.global.t('common.error.permissionDenied'))
      return Promise.reject(error)
    }

    // 429 限流（已达最大重试次数）: 显示 Retry-After 倒计时
    if (status === 429) {
      const retryAfterSec = getRetryAfterSeconds(error)
      if (retryAfterSec > 0) {
        message.warning(i18n.global.t('common.error.retryAfter', { seconds: retryAfterSec }))
      } else {
        message.warning(i18n.global.t('common.error.rateLimit'))
      }
      return Promise.reject(error)
    }

    // 502/503 服务不可用: 显示特定消息
    if (status === 502 || status === 503) {
      message.error(i18n.global.t('common.error.serviceUnavailable'))
      return Promise.reject(error)
    }

    // 500/504 其他服务端错误
    if (status >= 500) {
      message.error(i18n.global.t('common.error.serviceUnavailable'))
      return Promise.reject(error)
    }

    // 请求超时处理
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      message.error(i18n.global.t('common.error.requestTimeout'))
      return Promise.reject(error)
    }

    // 网络错误区分（离线 vs 服务器不可达）
    if (!navigator.onLine) {
      message.error(i18n.global.t('common.error.offlineError'))
      return Promise.reject(error)
    }

    if (error.code === 'ERR_NETWORK' || !error.response) {
      message.error(i18n.global.t('common.error.serverUnreachable'))
      return Promise.reject(error)
    }

    // 其他错误（如 400、404 等）
    const errorData = error.response?.data as Record<string, unknown> | undefined
    const errorMsg = getErrorDisplayMessage(
      errorData,
      error.message || 'Request failed'
    )
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

// ==================== 路由切换取消请求 ====================

/**
 * 在路由切换时取消所有进行中的请求
 * 在 router afterEach 中调用
 */
export function setupRouteChangeCancellation(router: { afterEach: (guard: () => void) => void }) {
  router.afterEach(() => {
    cancelAllPendingRequests()
  })
}

/**
 * 创建支持自定义超时的请求方法
 */
export function createRequest(config: InternalAxiosRequestConfig & { timeout?: number }) {
  return service({
    ...config,
    timeout: config.timeout ?? DEFAULT_TIMEOUT,
  })
}

export default service
