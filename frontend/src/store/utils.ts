/**
 * Store 工具函数
 * 提供标准化的状态管理模式：loading/error 状态管理、错误处理、缓存策略
 */
import { ref, type Ref } from 'vue'
import { logger } from '@/utils/logger'

// ==================== Loading State ====================

/**
 * 创建标准化的 loading/error 状态 refs
 * 用于 store actions 中统一管理异步操作状态
 */
export function createLoadingState<T>() {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const data = ref<T | null>(null) as Ref<T | null>

  return {
    loading,
    error,
    data,
  }
}

// ==================== Error Handling ====================

/**
 * Store action 的标准化错误处理
 * - 记录错误日志
 * - 可选设置 error ref
 * - 重新抛出错误供调用方处理
 */
export function handleStoreError(
  error: unknown,
  context: string,
  errorRef?: Ref<string | null>
): never {
  const message = error instanceof Error ? error.message : String(error)
  logger.debug(`${context} failed:`, error)
  if (errorRef) {
    errorRef.value = message
  }
  throw error
}

/**
 * 执行带 loading 状态管理的异步操作
 * 自动管理 loading ref 的开关，统一错误处理
 */
export async function withLoading<T>(
  loadingRef: Ref<boolean>,
  fn: () => Promise<T>,
  context?: string,
  errorRef?: Ref<string | null>
): Promise<T> {
  loadingRef.value = true
  try {
    return await fn()
  } catch (error) {
    if (context) {
      const message = error instanceof Error ? error.message : String(error)
      logger.debug(`${context} failed:`, error)
      if (errorRef) {
        errorRef.value = message
      }
    }
    throw error
  } finally {
    loadingRef.value = false
  }
}

// ==================== Cache Strategy ====================

interface CacheEntry<T> {
  data: T
  timestamp: number
  key: string
}

const DEFAULT_STALE_TIME = 5 * 60 * 1000 // 5 minutes

/**
 * 创建内存缓存管理器
 * 支持 stale-while-revalidate 模式
 */
export function createStoreCache<T>(options?: { staleTime?: number }) {
  const staleTime = options?.staleTime ?? DEFAULT_STALE_TIME
  const cache = new Map<string, CacheEntry<T>>()

  function get(key: string): T | null {
    const entry = cache.get(key)
    if (!entry) return null
    return entry.data
  }

  function set(key: string, data: T): void {
    cache.set(key, { data, timestamp: Date.now(), key })
  }

  function has(key: string): boolean {
    return cache.has(key)
  }

  function isStale(key: string): boolean {
    const entry = cache.get(key)
    if (!entry) return true
    return Date.now() - entry.timestamp > staleTime
  }

  function invalidate(key?: string): void {
    if (key) {
      cache.delete(key)
    } else {
      cache.clear()
    }
  }

  function clear(): void {
    cache.clear()
  }

  return {
    get,
    set,
    has,
    isStale,
    invalidate,
    clear,
  }
}

/**
 * 带缓存的 fetch composable
 * stale-while-revalidate 模式：先返回缓存数据，后台刷新
 */
export async function useCachedFetch<T>(
  cacheKey: string,
  cache: ReturnType<typeof createStoreCache<T>>,
  fetchFn: () => Promise<T>,
  options?: { forceRefresh?: boolean }
): Promise<{ data: T; isStale: boolean }> {
  const forceRefresh = options?.forceRefresh ?? false
  const cached = cache.get(cacheKey)
  const stale = cache.isStale(cacheKey)

  if (cached && !stale && !forceRefresh) {
    return { data: cached, isStale: false }
  }

  // 如果有缓存（即使是 stale 的），先返回缓存数据
  if (cached && stale && !forceRefresh) {
    // 后台刷新
    fetchFn().then((freshData) => {
      cache.set(cacheKey, freshData)
    }).catch(() => {
      // 后台刷新失败，静默处理，继续使用缓存
    })
    return { data: cached, isStale: true }
  }

  // 无缓存或强制刷新
  const freshData = await fetchFn()
  cache.set(cacheKey, freshData)
  return { data: freshData, isStale: false }
}

// ==================== Store Guard ====================

let storeReady = false

/**
 * 标记 store 已就绪（在 app.mount 之后调用）
 */
export function markStoresReady(): void {
  storeReady = true
}

/**
 * 检查 store 是否已就绪
 */
export function isStoreReady(): boolean {
  return storeReady
}

/**
 * Store 初始化守卫
 * 在开发环境下，如果 store 未就绪就访问，打印警告
 */
export function requireStoreReady(storeName: string): void {
  if (import.meta.env.DEV && !storeReady) {
    logger.warn(`[Store Guard] "${storeName}" store accessed before app is ready.`)
  }
}
