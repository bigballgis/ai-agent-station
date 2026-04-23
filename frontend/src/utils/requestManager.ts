import type { AxiosRequestConfig } from 'axios'

/**
 * 请求去重 + 取消管理器
 *
 * 最佳实践:
 * - 相同请求在短时间内只发送一次（去重）
 * - 页面切换时自动取消未完成的请求（防数据覆盖）
 * - 使用 AbortController（现代浏览器标准）
 *
 * 使用方式:
 *   // 在请求拦截器中自动处理
 *   const pendingKey = generateKey(config)
 *   if (pendingMap.has(pendingKey)) return // 去重
 *   const controller = new AbortController()
 *   config.signal = controller.signal
 *   pendingMap.set(pendingKey, controller)
 */

// 存储进行中的请求: key -> AbortController
const pendingMap = new Map<string, AbortController>()

// 需要去重的请求方法
const DEDUP_METHODS = ['get', 'post', 'put', 'patch']

// 需要去重的 URL 前缀（API 请求）
const DEDUP_PREFIXES = ['/api/', '/v1/auth/']

/**
 * 生成请求唯一 Key
 */
export function generateRequestKey(config: AxiosRequestConfig): string {
  const method = (config.method || 'get').toLowerCase()
  const url = config.url || ''
  const params = config.params ? JSON.stringify(sortKeys(config.params)) : ''
  const data = config.data && DEDUP_METHODS.includes(method)
    ? JSON.stringify(sortKeys(config.data))
    : ''
  return `${method}:${url}:${params}:${data}`
}

/**
 * 添加请求到 pending Map（去重 + 取消）
 * @returns true 表示请求被去重（应取消），false 表示正常放行
 */
export function addPendingRequest(config: AxiosRequestConfig): boolean {
  const method = (config.method || 'get').toLowerCase()
  const url = config.url || ''

  // 仅对配置的请求进行去重
  if (!DEDUP_METHODS.includes(method)) return false
  if (!DEDUP_PREFIXES.some(prefix => url.startsWith(prefix))) return false

  const key = generateRequestKey(config)

  // 去重检查：相同请求已在进行中
  if (pendingMap.has(key)) {
    // 对于 GET 请求，直接复用（返回同一个 Promise）
    // 对于 POST/PUT 请求，取消旧请求，发送新请求
    if (method === 'get') {
      return true // 去重
    }
    // 非幂等请求：取消旧的
    removePendingRequest(key)
  }

  // 创建 AbortController
  const controller = new AbortController()
  config.signal = controller.signal
  pendingMap.set(key, controller)

  return false
}

/**
 * 移除已完成的请求
 */
export function removePendingRequest(config: AxiosRequestConfig | string): void {
  const key = typeof config === 'string'
    ? config
    : generateRequestKey(config)
  const controller = pendingMap.get(key)
  if (controller) {
    controller.abort()
    pendingMap.delete(key)
  }
}

/**
 * 取消所有进行中的请求（页面切换/登出时调用）
 */
export function cancelAllPendingRequests(): void {
  pendingMap.forEach((controller) => {
    controller.abort()
  })
  pendingMap.clear()
  console.debug('[RequestManager] 已取消所有进行中的请求')
}

/**
 * 获取当前进行中的请求数量
 */
export function getPendingRequestCount(): number {
  return pendingMap.size
}

// ==================== 辅助函数 ====================

/**
 * 对对象键排序（确保相同参数生成相同的 key）
 */
function sortKeys(obj: unknown): unknown {
  if (obj === null || typeof obj !== 'object') return obj
  if (Array.isArray(obj)) return obj.map(sortKeys)
  const record = obj as Record<string, unknown>
  const sorted: Record<string, unknown> = {}
  Object.keys(record).sort().forEach(key => {
    sorted[key] = sortKeys(record[key])
  })
  return sorted
}
