/**
 * 简易断路器 (Circuit Breaker) 工具
 *
 * 功能:
 * - 按端点跟踪连续失败次数
 * - 连续 N 次失败后打开断路器（停止发送请求）
 * - 超时后进入半开状态（尝试一个请求）
 * - 半开状态请求成功则关闭断路器
 *
 * 使用方式:
 *   import { circuitBreaker } from '@/utils/circuitBreaker'
 *   const canProceed = circuitBreaker.canProceed('/api/v1/agents')
 *   if (!canProceed) { ... }
 *   circuitBreaker.recordSuccess('/api/v1/agents')
 *   circuitBreaker.recordFailure('/api/v1/agents')
 */

export interface CircuitBreakerConfig {
  /** 连续失败多少次后打开断路器，默认 5 */
  failureThreshold?: number
  /** 断路器打开后多久进入半开状态(ms)，默认 30000 */
  resetTimeoutMs?: number
  /** 半开状态允许通过的请求数，默认 1 */
  halfOpenMaxRequests?: number
}

interface CircuitState {
  status: 'closed' | 'open' | 'half-open'
  consecutiveFailures: number
  lastFailureTime: number
  halfOpenRequests: number
}

const DEFAULT_CONFIG: Required<CircuitBreakerConfig> = {
  failureThreshold: 5,
  resetTimeoutMs: 30000,
  halfOpenMaxRequests: 1,
}

// 按端点存储断路器状态
const circuitStates = new Map<string, CircuitState>()

/**
 * 从 URL 中提取端点 key（去掉 query string，按路径分组）
 */
function extractEndpoint(url: string | undefined): string {
  if (!url) return 'unknown'
  // 去掉 query string
  const path = url.split('?')[0]
  // 将路径参数（如 /api/v1/agents/123）归一化为 /api/v1/agents/:id
  return path.replace(/\/\d+(?=\/|$)/g, '/:id')
}

/**
 * 获取或创建断路器状态
 */
function getState(endpoint: string, config: Required<CircuitBreakerConfig>): CircuitState {
  let state = circuitStates.get(endpoint)
  if (!state) {
    state = {
      status: 'closed',
      consecutiveFailures: 0,
      lastFailureTime: 0,
      halfOpenRequests: 0,
    }
    circuitStates.set(endpoint, state)
  }

  // 检查是否应该从 open 转为 half-open
  if (state.status === 'open') {
    const elapsed = Date.now() - state.lastFailureTime
    if (elapsed >= config.resetTimeoutMs) {
      state.status = 'half-open'
      state.halfOpenRequests = 0
      console.info(
        `[CircuitBreaker] "${endpoint}" 从 OPEN 转为 HALF-OPEN (elapsed=${elapsed}ms)`
      )
    }
  }

  return state
}

/**
 * 断路器实例
 */
export const circuitBreaker = {
  /**
   * 判断请求是否可以通过断路器
   * @param url 请求 URL
   * @param config 可选配置
   * @returns true 表示允许请求，false 表示断路器已打开
   */
  canProceed(url: string | undefined, config?: CircuitBreakerConfig): boolean {
    const mergedConfig = { ...DEFAULT_CONFIG, ...config }
    const endpoint = extractEndpoint(url)
    const state = getState(endpoint, mergedConfig)

    if (state.status === 'closed') {
      return true
    }

    if (state.status === 'open') {
      console.warn(`[CircuitBreaker] "${endpoint}" 断路器已打开，请求被拒绝`)
      return false
    }

    // half-open: 允许有限请求通过
    if (state.halfOpenRequests < mergedConfig.halfOpenMaxRequests) {
      state.halfOpenRequests++
      return true
    }

    console.warn(`[CircuitBreaker] "${endpoint}" 半开状态已达上限，请求被拒绝`)
    return false
  },

  /**
   * 记录请求成功
   * @param url 请求 URL
   * @param config 可选配置
   */
  recordSuccess(url: string | undefined, _config?: CircuitBreakerConfig): void {
    const endpoint = extractEndpoint(url)
    const state = circuitStates.get(endpoint)

    if (!state) return

    const wasHalfOpen = state.status === 'half-open'

    state.status = 'closed'
    state.consecutiveFailures = 0
    state.halfOpenRequests = 0

    if (wasHalfOpen) {
      console.info(`[CircuitBreaker] "${endpoint}" 从 HALF-OPEN 转为 CLOSED`)
    }
  },

  /**
   * 记录请求失败
   * @param url 请求 URL
   * @param config 可选配置
   */
  recordFailure(url: string | undefined, config?: CircuitBreakerConfig): void {
    const mergedConfig = { ...DEFAULT_CONFIG, ...config }
    const endpoint = extractEndpoint(url)
    const state = getState(endpoint, mergedConfig)

    state.consecutiveFailures++
    state.lastFailureTime = Date.now()

    if (state.status === 'half-open') {
      // 半开状态下失败，重新打开
      state.status = 'open'
      console.warn(
        `[CircuitBreaker] "${endpoint}" 半开状态请求失败，重新打开断路器 (failures=${state.consecutiveFailures})`
      )
    } else if (state.consecutiveFailures >= mergedConfig.failureThreshold) {
      state.status = 'open'
      console.warn(
        `[CircuitBreaker] "${endpoint}" 连续失败 ${state.consecutiveFailures} 次，断路器已打开`
      )
    }
  },

  /**
   * 获取断路器状态（用于调试/监控）
   */
  getState(url: string | undefined): CircuitState | undefined {
    return circuitStates.get(extractEndpoint(url))
  },

  /**
   * 获取所有断路器状态
   */
  getAllStates(): Record<string, CircuitState> {
    const result: Record<string, CircuitState> = {}
    circuitStates.forEach((state, endpoint) => {
      result[endpoint] = { ...state }
    })
    return result
  },

  /**
   * 重置指定端点的断路器
   */
  reset(url: string | undefined): void {
    circuitStates.delete(extractEndpoint(url))
  },

  /**
   * 重置所有断路器
   */
  resetAll(): void {
    circuitStates.clear()
    console.info('[CircuitBreaker] 所有断路器已重置')
  },
}
