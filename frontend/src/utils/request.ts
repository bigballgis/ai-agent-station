import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types/common'
import i18n from '@/locales'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
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
 * 判断错误是否可重试（仅网络异常和 5xx 服务端错误可重试）
 */
function isRetryableError(error: unknown): boolean {
  if (!error || typeof error !== 'object' || !('response' in error)) {
    // 无响应对象，可能是网络错误（如断网、DNS 解析失败），可重试
    return true
  }
  const response = (error as { response?: { status?: number } }).response
  if (!response) return true
  // 仅 5xx 服务端错误可重试，4xx 客户端错误不重试
  return (response.status ?? 0) >= 500
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

// ==================== 请求拦截器 ====================

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器（含 Token 自动刷新 + GET 重试） ====================

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as ApiResponse
    if (res.code !== 200 && res.code !== 0) {
      message.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    // 返回 response.data，保留 ApiResponse<T> 结构供调用方使用
    // 注意：axios 拦截器中无法自动推断泛型 T，调用方需通过 ApiResponse<T> 断言具体 data 类型
    return response.data as unknown as AxiosResponse
  },
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    // GET 请求自动重试（最多 2 次，间隔 1 秒）
    if (
      originalRequest.method?.toLowerCase() === 'get' &&
      isRetryableError(error) &&
      (originalRequest._retryCount ?? 0) < MAX_RETRY_COUNT
    ) {
      originalRequest._retryCount = (originalRequest._retryCount ?? 0) + 1
      console.warn(`[Retry] GET ${originalRequest.url} retry #${originalRequest._retryCount}`)
      await delay(RETRY_DELAY_MS)
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

    // 429 限流: 提示用户稍后重试
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
    const errorMsg = error.response?.data?.message
      || (!error.response && i18n.global.t('common.error.networkError'))
      || error.message
      || 'Request failed'
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

export default service
