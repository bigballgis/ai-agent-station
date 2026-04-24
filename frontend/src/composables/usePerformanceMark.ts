/**
 * usePerformanceMark - 组件性能追踪 composable
 *
 * 基于 Performance API (performance.mark / performance.measure) 提供精细化的性能标记功能：
 * - startMark(name) - 开始一个性能标记
 * - endMark(name) - 结束标记并测量持续时间
 * - reportMark(name) - 上报测量结果
 *
 * 用于追踪：
 * - 页面加载时间（router beforeEach/afterEach）
 * - API 调用耗时（request interceptor）
 * - 组件渲染时间（在关键组件中使用）
 */

/** 性能测量记录 */
export interface PerformanceMeasurement {
  /** 测量名称 */
  name: string
  /** 持续时间（毫秒） */
  duration: number
  /** 开始时间戳 */
  startTime: number
  /** 结束时间戳 */
  endTime: number
  /** 测量分类 */
  category: PerformanceCategory
}

/** 性能分类 */
export type PerformanceCategory = 'navigation' | 'api' | 'render' | 'custom'

/** 性能追踪配置 */
export interface PerformanceMarkOptions {
  /** 分类标签 */
  category?: PerformanceCategory
  /** 是否自动上报，默认 false */
  autoReport?: boolean
  /** 自定义标记前缀 */
  prefix?: string
}

/** 上报端点 */
const REPORT_ENDPOINT = '/v1/performance/marks'

/** 活跃的标记集合 */
const activeMarks = new Map<string, number>()

/** 测量历史记录（内存中保留最近 200 条） */
const MAX_HISTORY = 200
const measurementHistory: PerformanceMeasurement[] = []

/**
 * 格式化标记名称（添加前缀避免冲突）
 */
function formatMarkName(name: string, prefix?: string): string {
  const markPrefix = prefix || 'perf'
  return `${markPrefix}:${name}`
}

/**
 * 获取当前时间戳（毫秒级精度）
 */
function now(): number {
  return performance.now()
}

/**
 * 非阻塞上报性能测量数据
 */
function reportMeasurement(measurement: PerformanceMeasurement): void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const url = `${baseUrl}${REPORT_ENDPOINT}`
  const data = JSON.stringify({
    url: window.location.href,
    userAgent: navigator.userAgent,
    measurement,
    collectedAt: Date.now(),
  })

  // 使用 requestIdleCallback 非阻塞上报
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      if (navigator.sendBeacon) {
        const blob = new Blob([data], { type: 'application/json' })
        navigator.sendBeacon(url, blob)
      } else {
        fetch(url, {
          method: 'POST',
          body: data,
          headers: { 'Content-Type': 'application/json' },
          keepalive: true,
        }).catch(() => {
          // 静默失败
        })
      }
    })
  }
}

/**
 * usePerformanceMark composable
 *
 * @param defaultOptions 默认配置选项
 * @returns 性能标记控制方法
 */
export function usePerformanceMark(defaultOptions?: PerformanceMarkOptions) {
  const category = defaultOptions?.category ?? 'custom'
  const autoReport = defaultOptions?.autoReport ?? false
  const prefix = defaultOptions?.prefix

  /**
   * 开始一个性能标记
   * @param name 标记名称
   */
  function startMark(name: string): void {
    const markName = formatMarkName(name, prefix)
    const timestamp = now()

    // 使用 Performance API 创建标记
    try {
      performance.mark(`${markName}:start`)
    } catch {
      // Performance API 不可用时静默处理
    }

    activeMarks.set(markName, timestamp)
  }

  /**
   * 结束一个性能标记并测量持续时间
   * @param name 标记名称
   * @returns 测量结果，如果标记未开始则返回 null
   */
  function endMark(name: string): PerformanceMeasurement | null {
    const markName = formatMarkName(name, prefix)
    const startTimestamp = activeMarks.get(markName)

    if (startTimestamp === undefined) {
      return null
    }

    const endTimestamp = now()
    const duration = endTimestamp - startTimestamp

    // 使用 Performance API 创建结束标记和测量
    try {
      performance.mark(`${markName}:end`)
      performance.measure(markName, `${markName}:start`, `${markName}:end`)

      // 清理 Performance API 中的标记（避免内存泄漏）
      try {
        performance.clearMarks(`${markName}:start`)
        performance.clearMarks(`${markName}:end`)
        performance.clearMeasures(markName)
      } catch {
        // 清理失败不影响功能
      }
    } catch {
      // Performance API 不可用时使用手动计算的时间
    }

    // 从活跃标记中移除
    activeMarks.delete(markName)

    const measurement: PerformanceMeasurement = {
      name,
      duration: Math.round(duration * 100) / 100,
      startTime: Math.round(startTimestamp * 100) / 100,
      endTime: Math.round(endTimestamp * 100) / 100,
      category,
    }

    // 添加到历史记录
    measurementHistory.push(measurement)
    if (measurementHistory.length > MAX_HISTORY) {
      measurementHistory.shift()
    }

    // 自动上报
    if (autoReport) {
      reportMeasurement(measurement)
    }

    return measurement
  }

  /**
   * 上报指定标记的测量结果
   * @param name 标记名称
   * @returns 测量结果，如果标记未结束则返回 null
   */
  function reportMark(name: string): PerformanceMeasurement | null {
    const markName = formatMarkName(name, prefix)
    const measurement = measurementHistory.find(
      (m) => formatMarkName(m.name, prefix) === markName
    )

    if (measurement) {
      reportMeasurement(measurement)
      return measurement
    }

    return null
  }

  /**
   * 获取指定标记的最新测量结果
   * @param name 标记名称
   */
  function getMeasurement(name: string): PerformanceMeasurement | null {
    const markName = formatMarkName(name, prefix)
    return (
      measurementHistory.find(
        (m) => formatMarkName(m.name, prefix) === markName
      ) ?? null
    )
  }

  /**
   * 获取指定分类的所有测量结果
   * @param filterCategory 分类筛选
   */
  function getMeasurementsByCategory(
    filterCategory: PerformanceCategory
  ): PerformanceMeasurement[] {
    return measurementHistory.filter((m) => m.category === filterCategory)
  }

  /**
   * 获取所有测量历史记录
   */
  function getAllMeasurements(): PerformanceMeasurement[] {
    return [...measurementHistory]
  }

  /**
   * 清除所有活跃标记和历史记录
   */
  function clearAll(): void {
    activeMarks.clear()
    measurementHistory.length = 0
  }

  return {
    startMark,
    endMark,
    reportMark,
    getMeasurement,
    getMeasurementsByCategory,
    getAllMeasurements,
    clearAll,
  }
}

/**
 * 全局性能追踪器实例（用于路由和 API 拦截器）
 */
export const globalPerformanceTracker = usePerformanceMark({
  prefix: 'global',
  autoReport: true,
})

/**
 * 获取全局测量历史（只读副本）
 */
export function getGlobalMeasurements(): PerformanceMeasurement[] {
  return globalPerformanceTracker.getAllMeasurements()
}
