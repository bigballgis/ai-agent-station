import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types/common'
import i18n from '@/locales'
import { getErrorDisplayMessage } from '@/utils/errorMessageMapper'
import { addPendingRequest, removePendingRequest, cancelAllPendingRequests } from '@/utils/requestManager'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const DEFAULT_TIMEOUT = 30000

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: DEFAULT_TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== GET 请求自动重试机制 ====================

const MAX_RETRY_COUNT = 2
const RETRY_DELAY_MS = 1000

/**
 * 延迟指定毫秒
 */
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 判断错误是否可重试
 * - 网络异常（无响应）可重试
 * - 5xx 服务端错误可重试
 * - 429 限流错误可重试（按 Retry-After 头等待）
 * - 503 服务不可用可重试
 */
function isRetryableError(error: unknown): boolean {
  if (!error || typeof error !== 'object' || !('response' in error)) {
    return true
  }
  const response = (error as { response?: { status?: number } }).response
  if (!response) return true
  const status = response.status ?? 0
  return status >= 500 || status === 429
}

/**
 * 从 429 响应中获取 Retry-After 值（秒）
 */
function getRetryAfterSeconds(error: unknown): number {
  if (!error || typeof error !== 'object' || !('response' in error)) return RETRY_DELAY_MS / 1000
  const response = (error as { response?: { headers?: Record<string, string | undefined> } }).response
  const retryAfter = response?.headers?.['retry-after']
  if (!retryAfter) return RETRY_DELAY_MS / 1000
  const seconds = parseInt(retryAfter, 10)
  if (!isNaN(seconds) && seconds > 0 && seconds <= 60) return seconds
  // Retry-After 也可能是 HTTP 日期格式，简单处理
  return RETRY_DELAY_MS / 1000
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

// ==================== 请求拦截器（去重 + AbortController） ====================

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
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

// ==================== 响应拦截器（含 Token 自动刷新 + 增强重试） ====================

service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 请求完成，从 pending map 中移除
    removePendingRequest(response.config)

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
    }

    // 被去重中止的请求，静默处理
    if (error.code === 'ERR_CANCELED' || axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const originalRequest = error.config
    const status = error.response?.status

    // 自动重试逻辑（GET 请求 + 429/503/网络错误）
    if (
      isRetryableError(error) &&
      (originalRequest._retryCount ?? 0) < MAX_RETRY_COUNT
    ) {
      // 429 限流：按 Retry-After 头等待
      let waitMs = RETRY_DELAY_MS
      if (status === 429) {
        const retryAfterSec = getRetryAfterSeconds(error)
        waitMs = retryAfterSec * 1000
        message.warning(i18n.global.t('common.error.rateLimit'))
      }

      originalRequest._retryCount = (originalRequest._retryCount ?? 0) + 1
      console.warn(
        `[Retry] ${originalRequest.method?.toUpperCase()} ${originalRequest.url} retry #${originalRequest._retryCount}, waiting ${waitMs}ms`
      )
      await delay(waitMs)
      return service(originalRequest)
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

    // 403 禁止访问: 直接清除认证
    if (status === 403) {
      clearAuthAndRedirect()
      return Promise.reject(error)
    }

    // 429 限流（已达最大重试次数）: 提示用户稍后重试
    if (status === 429) {
      message.warning(i18n.global.t('common.error.rateLimit'))
      return Promise.reject(error)
    }

    // 500/502/503 服务端错误
    if (status >= 500) {
      message.error(i18n.global.t('common.error.serviceUnavailable'))
      return Promise.reject(error)
    }

    // 其他错误（包括网络异常）
    const errorData = error.response?.data as Record<string, unknown> | undefined
    const errorMsg = getErrorDisplayMessage(
      errorData,
      (!error.response && i18n.global.t('common.error.networkError'))
      || error.message
      || 'Request failed'
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
