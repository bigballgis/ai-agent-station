/**
 * formatUtils.ts - 国际化格式化工具函数
 *
 * 使用 Intl API 实现数字、日期、货币、相对时间、文件大小、百分比等格式化
 * 所有函数支持 locale 参数，默认从 i18n 实例获取当前语言
 */

import i18n from '@/locales'

/**
 * 获取当前 locale
 */
function getCurrentLocale(override?: string): string {
  if (override) return override
  return i18n.global.locale.value as string || 'zh-CN'
}

/**
 * 将 locale 标准化为 Intl API 支持的格式
 * zh-CN -> zh-CN, en-US -> en-US
 */
function normalizeLocale(locale: string): string {
  return locale
}

/**
 * 格式化数字（带千分位分隔符）
 *
 * @param value - 数字值
 * @param locale - 语言环境，默认使用当前 i18n locale
 * @param options - Intl.NumberFormat 选项
 * @returns 格式化后的字符串
 *
 * @example
 * formatNumber(1234567.89) // zh-CN: "1,234,567.89"  en-US: "1,234,567.89"
 * formatNumber(1234567.89, 'en-US', { maximumFractionDigits: 0 }) // "1,234,568"
 */
export function formatNumber(
  value: number | null | undefined,
  locale?: string,
  options?: Intl.NumberFormatOptions
): string {
  if (value == null || isNaN(value)) return '-'
  try {
    return new Intl.NumberFormat(normalizeLocale(getCurrentLocale(locale)), options).format(value)
  } catch {
    return String(value)
  }
}

/**
 * 格式化货币金额
 *
 * @param value - 金额数值
 * @param currency - 货币代码，如 'CNY', 'USD', 'EUR'
 * @param locale - 语言环境
 * @returns 格式化后的货币字符串
 *
 * @example
 * formatCurrency(1234.56, 'CNY') // zh-CN: "¥1,234.56"
 * formatCurrency(1234.56, 'USD', 'en-US') // "$1,234.56"
 * formatCurrency(1234.56, 'EUR', 'de-DE') // "1.234,56 €"
 */
export function formatCurrency(
  value: number | null | undefined,
  currency: string = 'CNY',
  locale?: string
): string {
  if (value == null || isNaN(value)) return '-'
  try {
    return new Intl.NumberFormat(normalizeLocale(getCurrentLocale(locale)), {
      style: 'currency',
      currency,
    }).format(value)
  } catch {
    return `${value} ${currency}`
  }
}

/**
 * 格式化日期
 *
 * @param date - 日期值（字符串、Date 对象或时间戳）
 * @param format - 格式类型: 'full' | 'long' | 'medium' | 'short' | 'date' | 'time' | 'custom'
 * @param locale - 语言环境
 * @param customOptions - 当 format 为 'custom' 时的 Intl.DateTimeFormat 选项
 * @returns 格式化后的日期字符串
 *
 * @example
 * formatDate(new Date()) // zh-CN: "2024/1/15"  en-US: "1/15/2024"
 * formatDate(new Date(), 'long') // zh-CN: "2024年1月15日"
 * formatDate(new Date(), 'full') // zh-CN: "2024年1月15日星期一"
 * formatDate(new Date(), 'date') // zh-CN: "2024-01-15"
 */
export function formatDate(
  date: string | Date | null | undefined,
  format: 'full' | 'long' | 'medium' | 'short' | 'date' | 'time' | 'custom' = 'date',
  locale?: string,
  customOptions?: Intl.DateTimeFormatOptions
): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'

  const loc = normalizeLocale(getCurrentLocale(locale))

  // 自定义日期格式 (YYYY-MM-DD)
  if (format === 'date') {
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const formatMap: Record<string, Intl.DateTimeFormatOptions> = {
    full: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' },
    long: { year: 'numeric', month: 'long', day: 'numeric' },
    medium: { year: 'numeric', month: 'short', day: 'numeric' },
    short: { year: '2-digit', month: '2-digit', day: '2-digit' },
    time: { hour: '2-digit', minute: '2-digit', second: '2-digit' },
    custom: customOptions || {},
  }

  try {
    return new Intl.DateTimeFormat(loc, formatMap[format]).format(d)
  } catch {
    return d.toISOString().slice(0, 10)
  }
}

/**
 * 格式化日期时间
 *
 * @param date - 日期值
 * @param locale - 语言环境
 * @returns 格式化后的日期时间字符串
 *
 * @example
 * formatDateTime(new Date()) // zh-CN: "2024-01-15 10:30:00"
 */
export function formatDateTime(
  date: string | Date | null | undefined,
  locale?: string
): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'

  const loc = normalizeLocale(getCurrentLocale(locale))

  try {
    return new Intl.DateTimeFormat(loc, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(d)
  } catch {
    return date.toString()
  }
}

/**
 * 格式化相对时间（如 "3 分钟前"、"2 小时前"）
 *
 * @param date - 日期值
 * @param locale - 语言环境
 * @returns 相对时间字符串
 *
 * @example
 * formatRelativeTime(new Date(Date.now() - 3 * 60 * 1000)) // "3 分钟前" / "3 minutes ago"
 * formatRelativeTime(new Date(Date.now() - 2 * 3600 * 1000)) // "2 小时前" / "2 hours ago"
 */
export function formatRelativeTime(
  date: string | Date | null | undefined,
  locale?: string
): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'

  const now = Date.now()
  const diffMs = now - d.getTime()
  const absDiffMs = Math.abs(diffMs)
  const isFuture = diffMs < 0
  const loc = normalizeLocale(getCurrentLocale(locale))
  const { t } = i18n.global

  // 尝试使用 Intl.RelativeTimeFormat
  try {
    const rtf = new Intl.RelativeTimeFormat(loc, { numeric: 'auto' })

    // 定义阈值（秒）
    const thresholds = [
      { unit: 'year' as Intl.RelativeTimeFormatUnit, seconds: 365 * 24 * 3600 },
      { unit: 'month' as Intl.RelativeTimeFormatUnit, seconds: 30 * 24 * 3600 },
      { unit: 'week' as Intl.RelativeTimeFormatUnit, seconds: 7 * 24 * 3600 },
      { unit: 'day' as Intl.RelativeTimeFormatUnit, seconds: 24 * 3600 },
      { unit: 'hour' as Intl.RelativeTimeFormatUnit, seconds: 3600 },
      { unit: 'minute' as Intl.RelativeTimeFormatUnit, seconds: 60 },
      { unit: 'second' as Intl.RelativeTimeFormatUnit, seconds: 1 },
    ]

    // 不到 10 秒显示"刚刚"
    if (absDiffMs < 10 * 1000) {
      return t('format.justNow')
    }

    for (const { unit, seconds } of thresholds) {
      const diff = Math.round(absDiffMs / 1000 / seconds)
      if (diff >= 1) {
        if (isFuture) {
          return rtf.format(diff, unit)
        }
        return rtf.format(-diff, unit)
      }
    }

    return rtf.format(0, 'second')
  } catch {
    // Fallback: 使用 i18n key
    const seconds = Math.floor(absDiffMs / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    const weeks = Math.floor(days / 7)
    const months = Math.floor(days / 30)
    const years = Math.floor(days / 365)

    if (seconds < 60) return t('format.justNow')
    if (minutes < 60) return t('format.minutesAgo', { count: minutes })
    if (hours < 24) return t('format.hoursAgo', { count: hours })
    if (days < 7) return t('format.daysAgo', { count: days })
    if (weeks < 4) return t('format.weeksAgo', { count: weeks })
    if (months < 12) return t('format.monthsAgo', { count: months })
    return t('format.yearsAgo', { count: years })
  }
}

/**
 * 格式化文件大小
 *
 * @param bytes - 字节数
 * @param locale - 语言环境
 * @returns 格式化后的文件大小字符串
 *
 * @example
 * formatFileSize(0) // "0 B"
 * formatFileSize(1024) // "1 KB"
 * formatFileSize(1048576) // "1 MB"
 * formatFileSize(1073741824) // "1 GB"
 * formatFileSize(1536) // "1.5 KB"
 */
export function formatFileSize(
  bytes: number | null | undefined,
  _locale?: string
): string {
  if (bytes == null || bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB'] as const
  const k = 1024
  const i = Math.min(
    Math.floor(Math.log(Math.abs(bytes)) / Math.log(k)),
    units.length - 1
  )

  const value = parseFloat((bytes / Math.pow(k, i)).toFixed(2))

  // 使用 i18n key 获取带单位的格式化字符串
  const { t } = i18n.global
  const unitKeys: Record<string, string> = {
    '0': 'format.fileSize.byte',
    '1': 'format.fileSize.kilobyte',
    '2': 'format.fileSize.megabyte',
    '3': 'format.fileSize.gigabyte',
    '4': 'format.fileSize.terabyte',
  }

  const key = unitKeys[String(i)]
  if (key) {
    return t(key, { count: value })
  }

  return `${value} ${units[i]}`
}

/**
 * 格式化百分比
 *
 * @param value - 数值（0-100 或 0-1，根据 isFraction 参数）
 * @param locale - 语言环境
 * @param options - 格式化选项
 * @param options.isFraction - 是否为分数形式（0-1），默认 false
 * @param options.decimals - 小数位数，默认 1
 * @returns 格式化后的百分比字符串
 *
 * @example
 * formatPercentage(95.5) // "95.5%"
 * formatPercentage(0.955, undefined, { isFraction: true }) // "95.5%"
 * formatPercentage(33.333, undefined, { decimals: 2 }) // "33.33%"
 */
export function formatPercentage(
  value: number | null | undefined,
  locale?: string,
  options?: { isFraction?: boolean; decimals?: number }
): string {
  if (value == null || isNaN(value)) return '-'

  const { isFraction = false, decimals = 1 } = options || {}
  const actualValue = isFraction ? value * 100 : value
  const loc = normalizeLocale(getCurrentLocale(locale))

  try {
    return new Intl.NumberFormat(loc, {
      style: 'percent',
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(isFraction ? value : value / 100)
  } catch {
    return `${actualValue.toFixed(decimals)}%`
  }
}

/**
 * 格式化持续时间（毫秒转为可读格式）
 *
 * @param ms - 毫秒数
 * @param locale - 语言环境
 * @returns 格式化后的持续时间字符串
 *
 * @example
 * formatDuration(5000) // "5s"
 * formatDuration(65000) // "1m 5s"
 * formatDuration(3665000) // "1h 1m 5s"
 */
export function formatDuration(
  ms: number | null | undefined,
  _locale?: string
): string {
  if (ms == null || isNaN(ms)) return '-'

  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const parts: string[] = []
  if (hours > 0) parts.push(`${hours}h`)
  if (minutes > 0) parts.push(`${minutes}m`)
  if (seconds > 0 || parts.length === 0) parts.push(`${seconds}s`)

  return parts.join(' ')
}

/**
 * 格式化数字为紧凑形式（如 1.2K, 3.5M）
 *
 * @param value - 数字值
 * @param locale - 语言环境
 * @returns 紧凑格式的字符串
 *
 * @example
 * formatCompactNumber(1234) // "1.2K"
 * formatCompactNumber(3456789) // "3.5M"
 */
export function formatCompactNumber(
  value: number | null | undefined,
  locale?: string
): string {
  if (value == null || isNaN(value)) return '-'
  try {
    return new Intl.NumberFormat(normalizeLocale(getCurrentLocale(locale)), {
      notation: 'compact',
      compactDisplay: 'short',
      maximumFractionDigits: 1,
    }).format(value)
  } catch {
    return String(value)
  }
}
