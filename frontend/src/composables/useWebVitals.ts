/**
 * useWebVitals - Core Web Vitals 监控 composable
 *
 * 追踪以下 Core Web Vitals 指标：
 * - LCP (Largest Contentful Paint): 最大内容绘制
 * - FID (First Input Delay): 首次输入延迟
 * - CLS (Cumulative Layout Shift): 累积布局偏移
 * - INP (Interaction to Next Paint): 交互到下一次绘制
 * - TTFB (Time to First Byte): 首字节时间
 *
 * 使用 PerformanceObserver API 进行非阻塞采集，
 * 通过 sendBeacon / requestIdleCallback 非阻塞上报到后端。
 */

/** Web Vitals 指标类型 */
export interface WebVitalMetric {
  /** 指标名称 */
  name: string
  /** 指标值（毫秒） */
  value: number
  /** 指标评分: good | needs-improvement | poor */
  rating: 'good' | 'needs-improvement' | 'poor'
  /** 采集时间戳 */
  timestamp: number
  /** 导航类型 */
  navigationType?: string
  /** 关联的 DOM 元素（如有） */
  element?: string
  /** 唯一标识 */
  id: string
}

/** 上报批次数据 */
interface PerformanceReportPayload {
  /** 页面 URL */
  url: string
  /** 用户代理 */
  userAgent: string
  /** 指标列表 */
  metrics: WebVitalMetric[]
  /** 采集时间 */
  collectedAt: number
}

/** 各指标的评分阈值（毫秒，CLS 为无单位值） */
const RATING_THRESHOLDS: Record<string, [number, number]> = {
  LCP: [2500, 4000],
  FID: [100, 300],
  CLS: [0.1, 0.25],
  INP: [200, 500],
  TTFB: [800, 1800],
}

/** 上报端点 */
const REPORT_ENDPOINT = '/v1/performance/metrics'

/** 上报批次缓冲区 */
let reportBuffer: WebVitalMetric[] = []
/** 上报定时器 */
let flushTimer: ReturnType<typeof setTimeout> | null = null
/** 上报批次间隔（毫秒） */
const FLUSH_INTERVAL = 5000
/** 最大缓冲区大小 */
const MAX_BUFFER_SIZE = 20

/**
 * 根据指标名称和值计算评分
 */
function getRating(name: string, value: number): 'good' | 'needs-improvement' | 'poor' {
  const thresholds = RATING_THRESHOLDS[name]
  if (!thresholds) return 'needs-improvement'
  if (value <= thresholds[0]) return 'good'
  if (value <= thresholds[1]) return 'needs-improvement'
  return 'poor'
}

/**
 * 生成唯一 ID
 */
function generateId(): string {
  return `vital-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

/**
 * 非阻塞上报：优先使用 sendBeacon，降级使用 fetch + keepalive
 */
function sendReport(payload: PerformanceReportPayload): void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const url = `${baseUrl}${REPORT_ENDPOINT}`
  const data = JSON.stringify(payload)

  // 优先使用 navigator.sendBeacon（非阻塞，页面卸载时也能发送）
  if (navigator.sendBeacon) {
    const blob = new Blob([data], { type: 'application/json' })
    const sent = navigator.sendBeacon(url, blob)
    if (sent) return
  }

  // 降级：使用 fetch + keepalive
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      fetch(url, {
        method: 'POST',
        body: data,
        headers: { 'Content-Type': 'application/json' },
        keepalive: true,
      }).catch(() => {
        // 静默失败，不影响用户体验
      })
    })
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
}

/**
 * 将指标加入缓冲区，达到阈值或定时触发上报
 */
function addToBuffer(metric: WebVitalMetric): void {
  reportBuffer.push(metric)

  // 缓冲区满立即上报
  if (reportBuffer.length >= MAX_BUFFER_SIZE) {
    flushReport()
    return
  }

  // 设置定时上报
  if (!flushTimer) {
    flushTimer = setTimeout(() => {
      flushReport()
    }, FLUSH_INTERVAL)
  }
}

/**
 * 立即上报缓冲区中的所有指标
 */
function flushReport(): void {
  if (flushTimer) {
    clearTimeout(flushTimer)
    flushTimer = null
  }

  if (reportBuffer.length === 0) return

  const payload: PerformanceReportPayload = {
    url: window.location.href,
    userAgent: navigator.userAgent,
    metrics: [...reportBuffer],
    collectedAt: Date.now(),
  }

  reportBuffer = []
  sendReport(payload)
}

/**
 * 追踪 LCP (Largest Contentful Paint)
 */
function trackLCP(onMetric: (metric: WebVitalMetric) => void): void {
  try {
    const observer = new PerformanceObserver((entryList) => {
      const entries = entryList.getEntries()
      const lastEntry = entries[entries.length - 1] as PerformanceEntry & {
        startTime: number
        element?: Element
      }

      const metric: WebVitalMetric = {
        id: generateId(),
        name: 'LCP',
        value: Math.round(lastEntry.startTime),
        rating: getRating('LCP', lastEntry.startTime),
        timestamp: Date.now(),
        element: lastEntry.element?.tagName?.toLowerCase() || undefined,
      }

      onMetric(metric)
    })

    observer.observe({ type: 'largest-contentful-paint', buffered: true })
  } catch {
    // 浏览器不支持 LCP 追踪
  }
}

/**
 * 追踪 FID (First Input Delay)
 */
function trackFID(onMetric: (metric: WebVitalMetric) => void): void {
  try {
    const observer = new PerformanceObserver((entryList) => {
      const entry = entryList.getEntries()[0] as PerformanceEntry & {
        processingStart: number
        startTime: number
      }

      const delay = entry.processingStart - entry.startTime

      const metric: WebVitalMetric = {
        id: generateId(),
        name: 'FID',
        value: Math.round(delay),
        rating: getRating('FID', delay),
        timestamp: Date.now(),
      }

      onMetric(metric)
    })

    observer.observe({ type: 'first-input', buffered: true })
  } catch {
    // 浏览器不支持 FID 追踪
  }
}

/**
 * 追踪 CLS (Cumulative Layout Shift)
 */
function trackCLS(onMetric: (metric: WebVitalMetric) => void): void {
  try {
    let clsValue = 0
    let sessionEntries: PerformanceEntry[] = []
    let sessionValue = 0

    const observer = new PerformanceObserver((entryList) => {
      for (const entry of entryList.getEntries()) {
        const layoutShift = entry as PerformanceEntry & {
          hadRecentInput: boolean
          value: number
          startTime: number
        }

        // 忽略有最近用户输入的布局偏移
        if (!layoutShift.hadRecentInput) {
          const firstSessionEntry = sessionEntries[0]
          const lastSessionEntry = sessionEntries[sessionEntries.length - 1]

          // 如果条目与上一个会话间隔超过 1 秒，或与第一个条目间隔超过 5 秒，开始新会话
          if (
            sessionValue &&
            layoutShift.startTime - lastSessionEntry.startTime > 1000 &&
            layoutShift.startTime - firstSessionEntry.startTime > 5000
          ) {
            sessionValue = 0
            sessionEntries = []
          }

          sessionValue += layoutShift.value
          sessionEntries.push(layoutShift)

          clsValue = Math.max(clsValue, sessionValue)
        }
      }

      const metric: WebVitalMetric = {
        id: generateId(),
        name: 'CLS',
        value: Math.round(clsValue * 1000) / 1000,
        rating: getRating('CLS', clsValue),
        timestamp: Date.now(),
      }

      onMetric(metric)
    })

    observer.observe({ type: 'layout-shift', buffered: true })
  } catch {
    // 浏览器不支持 CLS 追踪
  }
}

/**
 * 追踪 INP (Interaction to Next Paint)
 */
function trackINP(onMetric: (metric: WebVitalMetric) => void): void {
  try {
    let maxDuration = 0

    const observer = new PerformanceObserver((entryList) => {
      for (const entry of entryList.getEntries()) {
        const interactionEntry = entry as PerformanceEntry & {
          duration: number
          interactionId: number
          name: string
        }

        // 仅处理有 interactionId 的条目（即用户交互事件）
        if (interactionEntry.interactionId) {
          maxDuration = Math.max(maxDuration, interactionEntry.duration)
        }
      }

      const metric: WebVitalMetric = {
        id: generateId(),
        name: 'INP',
        value: Math.round(maxDuration),
        rating: getRating('INP', maxDuration),
        timestamp: Date.now(),
      }

      onMetric(metric)
    })

    observer.observe({ type: 'event', buffered: true, durationThreshold: 16 } as PerformanceObserverInit)
  } catch {
    // 浏览器不支持 INP 追踪
  }
}

/**
 * 追踪 TTFB (Time to First Byte)
 */
function trackTTFB(onMetric: (metric: WebVitalMetric) => void): void {
  try {
    const observer = new PerformanceObserver((entryList) => {
      const entry = entryList.getEntries()[0] as PerformanceEntry & {
        responseStart: number
        requestStart: number
        transferSize: number
        nextHopProtocol: string
      }

      // TTFB = responseStart - requestStart
      const ttfb = entry.responseStart - entry.requestStart

      const metric: WebVitalMetric = {
        id: generateId(),
        name: 'TTFB',
        value: Math.round(ttfb),
        rating: getRating('TTFB', ttfb),
        timestamp: Date.now(),
        navigationType: entry.nextHopProtocol,
      }

      onMetric(metric)
    })

    observer.observe({ type: 'navigation', buffered: true })
  } catch {
    // 浏览器不支持 navigation 追踪
  }
}

/**
 * useWebVitals composable
 *
 * 初始化所有 Core Web Vitals 追踪器，将指标非阻塞上报到后端。
 *
 * @param options 配置选项
 * @param options.reportToBackend 是否自动上报到后端，默认 true
 * @param options.onMetric 自定义指标回调（可用于本地调试面板）
 * @returns 包含最新指标和控制方法的对象
 */
export function useWebVitals(options?: {
  reportToBackend?: boolean
  onMetric?: (metric: WebVitalMetric) => void
}) {
  const reportToBackend = options?.reportToBackend ?? true
  const onMetric = options?.onMetric

  /** 最新采集的各指标快照 */
  const latestMetrics = reactive<Record<string, WebVitalMetric | null>>({
    LCP: null,
    FID: null,
    CLS: null,
    INP: null,
    TTFB: null,
  })

  /** 所有采集到的指标历史记录 */
  const metricsHistory = ref<WebVitalMetric[]>([])

  /**
   * 处理采集到的指标
   */
  function handleMetric(metric: WebVitalMetric): void {
    // 更新最新快照
    latestMetrics[metric.name] = metric

    // 记录历史
    metricsHistory.value.push(metric)

    // 自定义回调
    if (onMetric) {
      onMetric(metric)
    }

    // 上报到后端
    if (reportToBackend) {
      addToBuffer(metric)
    }
  }

  /**
   * 初始化所有追踪器
   */
  function init(): void {
    trackLCP(handleMetric)
    trackFID(handleMetric)
    trackCLS(handleMetric)
    trackINP(handleMetric)
    trackTTFB(handleMetric)
  }

  /**
   * 手动触发上报缓冲区中的指标
   */
  function flush(): void {
    flushReport()
  }

  /**
   * 获取所有指标的汇总
   */
  function getSummary(): Record<string, WebVitalMetric | null> {
    return { ...latestMetrics }
  }

  /**
   * 销毁所有追踪器并上报剩余指标
   */
  function destroy(): void {
    flush()
  }

  // 自动初始化
  init()

  // 页面卸载前上报剩余指标
  if (typeof window !== 'undefined') {
    window.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        flush()
      }
    })

    window.addEventListener('pagehide', () => {
      flush()
    })
  }

  return {
    /** 最新各指标快照 */
    latestMetrics,
    /** 指标历史记录 */
    metricsHistory,
    /** 手动触发上报 */
    flush,
    /** 获取指标汇总 */
    getSummary,
    /** 销毁追踪器 */
    destroy,
  }
}

// Vue reactivity imports
import { reactive, ref } from 'vue'
