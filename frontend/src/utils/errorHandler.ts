import { App } from 'vue'

/**
 * 全局错误处理
 * 
 * 最佳实践:
 * - app.config.errorHandler 捕获 Vue 组件错误
 * - window.onerror 捕获全局 JS 错误
 * - window.onunhandledrejection 捕获未处理的 Promise 异常
 * - 错误上报到监控系统（生产环境）
 */
export function setupErrorHandler(app: App) {
  // Vue 组件错误捕获
  app.config.errorHandler = (err, instance, info) => {
    console.error('[Vue Error]', err)
    console.error('[Error Info]', info)
    console.error('[Error Component]', instance?.$options?.name || 'Unknown')

    // 生产环境上报错误
    if (import.meta.env.PROD) {
      reportError({
        type: 'vue',
        message: String(err),
        stack: err instanceof Error ? err.stack : undefined,
        component: instance?.$options?.name,
        info,
      })
    }
  }

  // 全局 JS 错误
  window.onerror = (message, source, lineno, colno, error) => {
    console.error('[Global Error]', { message, source, lineno, colno, error })
    if (import.meta.env.PROD) {
      reportError({
        type: 'global',
        message: String(message),
        source,
        lineno,
        colno,
        stack: error?.stack,
      })
    }
    return false // 不阻止默认处理
  }

  // 未处理的 Promise 异常
  window.addEventListener('unhandledrejection', (event) => {
    console.error('[Unhandled Promise]', event.reason)
    if (import.meta.env.PROD) {
      reportError({
        type: 'promise',
        message: String(event.reason),
        stack: event.reason instanceof Error ? event.reason.stack : undefined,
      })
    }
    // 阻止默认的控制台警告
    event.preventDefault()
  })

  // 资源加载错误
  window.addEventListener('error', (event) => {
    const target = event.target as HTMLElement
    if (target?.tagName) {
      console.warn('[Resource Error]', target.tagName, (target as any).src || (target as any).href)
    }
  }, true) // 使用捕获阶段
}

interface ErrorReport {
  type: 'vue' | 'global' | 'promise'
  message: string
  stack?: string
  source?: string
  lineno?: number
  colno?: number
  component?: string
  info?: string
}

function reportError(report: ErrorReport) {
  // TODO: 接入 Sentry / 自建错误上报服务
  // 当前仅 console 输出，生产环境应替换为实际的上报逻辑
  console.warn('[Error Report]', JSON.stringify(report))
}
