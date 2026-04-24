import { App } from 'vue'

/**
 * 全局错误处理
 *
 * 最佳实践:
 * - app.config.errorHandler 捕获 Vue 组件错误
 * - window.onerror 捕获全局 JS 错误
 * - window.onunhandledrejection 捕获未处理的 Promise 异常
 * - 错误上报到监控系统（生产环境）
 * - 错误分类：network / auth / business / unknown
 */

export type ErrorCategory = 'network' | 'auth' | 'business' | 'unknown'

const ERROR_STORAGE_KEY = '__frontend_error_reports__'
const MAX_STORED_ERRORS = 50

interface ErrorReport {
  type: 'vue' | 'global' | 'promise'
  category: ErrorCategory
  message: string
  stack?: string
  source?: string
  lineno?: number
  colno?: number
  component?: string
  info?: string
}

interface StoredErrorReport extends ErrorReport {
  timestamp: string
  url: string
  userAgent: string
  userId?: string
}

/**
 * 获取当前用户ID（从 localStorage 中读取）
 */
function getCurrentUserId(): string | undefined {
  try {
    const userStr = localStorage.getItem('user') || localStorage.getItem('currentUser')
    if (userStr) {
      const user = JSON.parse(userStr)
      return user.id || user.userId || user.username || undefined
    }
  } catch {
    // 解析失败时忽略
  }
  return undefined
}

/**
 * 将错误信息存储到 localStorage 中（最多保留50条）
 */
function storeError(report: ErrorReport): void {
  try {
    const storedReport: StoredErrorReport = {
      ...report,
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      userId: getCurrentUserId(),
    }

    const raw = localStorage.getItem(ERROR_STORAGE_KEY)
    const errors: StoredErrorReport[] = raw ? JSON.parse(raw) : []

    errors.push(storedReport)

    // 只保留最新的 MAX_STORED_ERRORS 条记录
    if (errors.length > MAX_STORED_ERRORS) {
      errors.splice(0, errors.length - MAX_STORED_ERRORS)
    }

    localStorage.setItem(ERROR_STORAGE_KEY, JSON.stringify(errors))
  } catch {
    // localStorage 写入失败时静默处理，避免影响主流程
  }
}

/**
 * 获取已存储的错误记录（供开发者调试使用）
 */
export function getStoredErrors(): StoredErrorReport[] {
  try {
    const raw = localStorage.getItem(ERROR_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

/**
 * 清除已存储的错误记录
 */
export function clearStoredErrors(): void {
  try {
    localStorage.removeItem(ERROR_STORAGE_KEY)
  } catch {
    // 忽略
  }
}
/**
 * 将错误信息分类
 */
export function categorizeError(error: unknown): ErrorCategory {
  const msg = String(error).toLowerCase()
  void (error instanceof Error ? (error.stack || '').toLowerCase() : '')

  // 网络错误
  if (
    msg.includes('network') ||
    msg.includes('fetch') ||
    msg.includes('timeout') ||
    msg.includes('net::') ||
    msg.includes('failed to fetch') ||
    msg.includes('err_network') ||
    msg.includes('err_connection') ||
    msg.includes('econnrefused') ||
    msg.includes('econnreset')
  ) {
    return 'network'
  }

  // 认证错误
  if (
    msg.includes('401') ||
    msg.includes('403') ||
    msg.includes('unauthorized') ||
    msg.includes('forbidden') ||
    msg.includes('token') ||
    msg.includes('permission') ||
    msg.includes('认证') ||
    msg.includes('权限')
  ) {
    return 'auth'
  }

  // 业务错误
  if (
    msg.includes('400') ||
    msg.includes('409') ||
    msg.includes('422') ||
    msg.includes('business') ||
    msg.includes('validation') ||
    msg.includes('参数') ||
    msg.includes('校验')
  ) {
    return 'business'
  }

  return 'unknown'
}

export function setupErrorHandler(app: App) {
  // Vue 组件错误捕获
  app.config.errorHandler = (err, instance, info) => {
    const category = categorizeError(err)
    console.error(`[Vue Error] [${category}]`, err)
    console.error('[Error Info]', info)
    console.error('[Error Component]', instance?.$options?.name || 'Unknown')

    // 生产环境上报错误
    if (import.meta.env.PROD) {
      reportError({
        type: 'vue',
        category,
        message: String(err),
        stack: err instanceof Error ? err.stack : undefined,
        component: instance?.$options?.name,
        info,
      })
    }
  }

  // 全局 JS 错误
  window.onerror = (message, source, lineno, colno, error) => {
    const category = categorizeError(message)
    console.error(`[Global Error] [${category}]`, { message, source, lineno, colno, error })
    if (import.meta.env.PROD) {
      reportError({
        type: 'global',
        category,
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
    const category = categorizeError(event.reason)
    console.error(`[Unhandled Promise] [${category}]`, event.reason)
    if (import.meta.env.PROD) {
      reportError({
        type: 'promise',
        category,
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
      console.warn('[Resource Error]', target.tagName, (target as HTMLImageElement).src || (target as HTMLAnchorElement).href)
    }
  }, true) // 使用捕获阶段
}

function reportError(report: ErrorReport) {
  // 将错误信息存储到 localStorage 中，供开发者调试查看
  // TODO: 生产环境建议接入 Sentry 或自建错误上报服务以获取更详细的错误追踪
  console.warn('[Error Report]', JSON.stringify(report))
  storeError(report)
}
