import { ref, readonly, onMounted, onUnmounted } from 'vue'
import type { AxiosRequestConfig } from 'axios'
import service from '@/utils/request'

/**
 * 网络状态检测 composable
 *
 * 功能:
 * - 检测 online/offline 状态（navigator.onLine + 事件监听）
 * - 离线时队列化失败的请求
 * - 恢复在线时自动重试队列中的请求
 */

interface QueuedRequest {
  config: AxiosRequestConfig
  resolve: (value: unknown) => void
  reject: (reason: unknown) => void
}

const isOnline = ref(navigator.onLine)
const isManuallyOffline = ref(false)
const queuedRequests: QueuedRequest[] = []
let isRetrying = false

function handleOnline() {
  isOnline.value = true
  isManuallyOffline.value = false
  console.info('[NetworkStatus] Back online, retrying queued requests...')
  retryQueuedRequests()
}

function handleOffline() {
  isOnline.value = false
  console.warn('[NetworkStatus] Network offline, requests will be queued.')
}

/**
 * 重试队列中所有请求
 */
async function retryQueuedRequests() {
  if (isRetrying || queuedRequests.length === 0) return
  isRetrying = true

  // 复制队列并清空，避免重试过程中新请求被加入
  const requests = [...queuedRequests]
  queuedRequests.length = 0

  for (const { config, resolve, reject } of requests) {
    try {
      const response = await service(config)
      resolve(response)
    } catch (err) {
      reject(err)
    }
  }

  isRetrying = false
}

/**
 * 将请求加入离线队列
 */
export function queueRequest(config: AxiosRequestConfig): Promise<unknown> {
  return new Promise((resolve, reject) => {
    queuedRequests.push({ config, resolve, reject })
  })
}

/**
 * 获取当前队列中的请求数量
 */
export function getQueuedRequestCount(): number {
  return queuedRequests.length
}

/**
 * useNetworkStatus composable
 */
export function useNetworkStatus() {
  onMounted(() => {
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
  })

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  })

  const effectiveOnline = () => isOnline.value && !isManuallyOffline.value

  return {
    isOnline: readonly(isOnline),
    isOffline: readonly(ref(false)), // will be computed below
    effectiveOnline,
    queuedRequestCount: () => queuedRequests.length,
  }
}

/**
 * 获取响应式的离线状态（供模板使用）
 */
export function useOfflineState() {
  const offline = ref(!navigator.onLine)

  function update() {
    offline.value = !navigator.onLine || isManuallyOffline.value
  }

  onMounted(() => {
    window.addEventListener('online', update)
    window.addEventListener('offline', update)
  })

  onUnmounted(() => {
    window.removeEventListener('online', update)
    window.removeEventListener('offline', update)
  })

  return { isOffline: readonly(offline) }
}
