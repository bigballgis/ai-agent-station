import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * formatUtils 单元测试
 * 测试所有 9 个格式化函数及边界情况
 */

// Mock @/locales
const mockT = vi.fn()
vi.mock('@/locales', () => ({
  default: {
    global: {
      locale: { value: 'zh-CN' },
      t: mockT,
    },
  },
}))

describe('formatUtils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockT.mockImplementation((key: string, params?: Record<string, unknown>) => {
      // Simulate some i18n translations
      const translations: Record<string, string> = {
        'format.justNow': '刚刚',
        'format.minutesAgo': '{count} 分钟前',
        'format.hoursAgo': '{count} 小时前',
        'format.daysAgo': '{count} 天前',
        'format.weeksAgo': '{count} 周前',
        'format.monthsAgo': '{count} 个月前',
        'format.yearsAgo': '{count} 年前',
        'format.fileSize.byte': '{count} B',
        'format.fileSize.kilobyte': '{count} KB',
        'format.fileSize.megabyte': '{count} MB',
        'format.fileSize.gigabyte': '{count} GB',
        'format.fileSize.terabyte': '{count} TB',
      }
      let result = translations[key] || key
      if (params) {
        Object.entries(params).forEach(([k, v]) => {
          result = result.replace(`{${k}}`, String(v))
        })
      }
      return result
    })
  })

  // ---------- formatNumber ----------

  describe('formatNumber', () => {
    it('格式化普通数字（带千分位）', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      const result = formatNumber(1234567.89)
      expect(result).toContain('1')
      expect(result).toContain('567')
    })

    it('null 返回 "-"', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      expect(formatNumber(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      expect(formatNumber(undefined)).toBe('-')
    })

    it('NaN 返回 "-"', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      expect(formatNumber(NaN)).toBe('-')
    })

    it('负数格式化', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      const result = formatNumber(-1234)
      expect(result).toContain('1,234')
      expect(result).toContain('-')
    })

    it('零返回 "0"', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      expect(formatNumber(0)).toBe('0')
    })

    it('非常大的数字', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      const result = formatNumber(999999999999)
      expect(result).toBeTruthy()
      expect(typeof result).toBe('string')
    })

    it('支持自定义 Intl 选项', async () => {
      const { formatNumber } = await import('@/utils/formatUtils')
      const result = formatNumber(1234.567, 'en-US', { maximumFractionDigits: 0 })
      expect(result).toBe('1,235')
    })
  })

  // ---------- formatCurrency ----------

  describe('formatCurrency', () => {
    it('格式化 CNY 金额', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      const result = formatCurrency(1234.56, 'CNY')
      expect(result).toContain('1,234.56')
    })

    it('null 返回 "-"', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      expect(formatCurrency(null, 'CNY')).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      expect(formatCurrency(undefined, 'CNY')).toBe('-')
    })

    it('NaN 返回 "-"', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      expect(formatCurrency(NaN, 'USD')).toBe('-')
    })

    it('负数金额', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      const result = formatCurrency(-100, 'CNY')
      expect(result).toBeTruthy()
    })

    it('零金额', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      const result = formatCurrency(0, 'CNY')
      expect(result).toContain('0.00')
    })

    it('默认货币为 CNY', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      const result = formatCurrency(100)
      expect(result).toBeTruthy()
    })

    it('USD 货币格式', async () => {
      const { formatCurrency } = await import('@/utils/formatUtils')
      const result = formatCurrency(1234.56, 'USD', 'en-US')
      expect(result).toContain('1,234.56')
    })
  })

  // ---------- formatDate ----------

  describe('formatDate', () => {
    it('默认格式 (date) 返回 YYYY-MM-DD', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      const result = formatDate('2024-01-15T10:30:00Z')
      expect(result).toBe('2024-01-15')
    })

    it('null 返回 "-"', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      expect(formatDate(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      expect(formatDate(undefined)).toBe('-')
    })

    it('无效日期字符串返回 "-"', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      expect(formatDate('invalid-date')).toBe('-')
    })

    it('Date 对象作为输入', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      const result = formatDate(new Date(2024, 0, 15))
      expect(result).toBe('2024-01-15')
    })

    it('long 格式', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      const result = formatDate('2024-01-15', 'long')
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('full 格式', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      const result = formatDate('2024-01-15', 'full')
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('time 格式', async () => {
      const { formatDate } = await import('@/utils/formatUtils')
      const result = formatDate('2024-01-15T10:30:45', 'time')
      expect(result).toBeTruthy()
    })
  })

  // ---------- formatDateTime ----------

  describe('formatDateTime', () => {
    it('格式化日期时间', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      const result = formatDateTime('2024-01-15T10:30:00')
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('null 返回 "-"', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      expect(formatDateTime(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      expect(formatDateTime(undefined)).toBe('-')
    })

    it('无效日期返回 "-"', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      expect(formatDateTime('not-a-date')).toBe('-')
    })

    it('Date 对象作为输入', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      const result = formatDateTime(new Date(2024, 0, 15, 10, 30, 0))
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('包含年月日时分秒', async () => {
      const { formatDateTime } = await import('@/utils/formatUtils')
      const result = formatDateTime('2024-06-15T14:30:45')
      // Should contain date and time parts
      expect(result).toContain('2024')
    })
  })

  // ---------- formatRelativeTime ----------

  describe('formatRelativeTime', () => {
    it('null 返回 "-"', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      expect(formatRelativeTime(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      expect(formatRelativeTime(undefined)).toBe('-')
    })

    it('无效日期返回 "-"', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      expect(formatRelativeTime('invalid')).toBe('-')
    })

    it('不到 10 秒前显示 "刚刚"', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      const result = formatRelativeTime(new Date(Date.now() - 5000))
      expect(result).toBe('刚刚')
    })

    it('几分钟前', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      const result = formatRelativeTime(new Date(Date.now() - 3 * 60 * 1000))
      expect(result).toContain('3')
    })

    it('几小时前', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      const result = formatRelativeTime(new Date(Date.now() - 2 * 3600 * 1000))
      expect(result).toContain('2')
    })

    it('几天前', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      const result = formatRelativeTime(new Date(Date.now() - 3 * 24 * 3600 * 1000))
      expect(result).toContain('3')
    })

    it('Date 对象作为输入', async () => {
      const { formatRelativeTime } = await import('@/utils/formatUtils')
      const result = formatRelativeTime(new Date(Date.now() - 5000))
      expect(result).toBe('刚刚')
    })
  })

  // ---------- formatFileSize ----------

  describe('formatFileSize', () => {
    it('null 返回 "0 B"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      expect(formatFileSize(null)).toBe('0 B')
    })

    it('undefined 返回 "0 B"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      expect(formatFileSize(undefined)).toBe('0 B')
    })

    it('0 字节返回 "0 B"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      expect(formatFileSize(0)).toBe('0 B')
    })

    it('1024 字节返回 "1 KB"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      const result = formatFileSize(1024)
      expect(result).toContain('1')
      expect(result).toContain('KB')
    })

    it('1048576 字节返回 "1 MB"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      const result = formatFileSize(1048576)
      expect(result).toContain('1')
      expect(result).toContain('MB')
    })

    it('1073741824 字节返回 "1 GB"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      const result = formatFileSize(1073741824)
      expect(result).toContain('1')
      expect(result).toContain('GB')
    })

    it('512 字节返回 "512 B"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      const result = formatFileSize(512)
      expect(result).toContain('512')
      expect(result).toContain('B')
    })

    it('1536 字节返回 "1.5 KB"', async () => {
      const { formatFileSize } = await import('@/utils/formatUtils')
      const result = formatFileSize(1536)
      expect(result).toContain('1.5')
      expect(result).toContain('KB')
    })
  })

  // ---------- formatPercentage ----------

  describe('formatPercentage', () => {
    it('格式化普通百分比', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      const result = formatPercentage(95.5)
      expect(result).toContain('95.5')
      expect(result).toContain('%')
    })

    it('null 返回 "-"', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      expect(formatPercentage(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      expect(formatPercentage(undefined)).toBe('-')
    })

    it('NaN 返回 "-"', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      expect(formatPercentage(NaN)).toBe('-')
    })

    it('分数形式 (0-1) 转换为百分比', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      const result = formatPercentage(0.955, undefined, { isFraction: true })
      expect(result).toContain('95.5')
    })

    it('自定义小数位数', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      const result = formatPercentage(33.333, undefined, { decimals: 2 })
      expect(result).toContain('33.33')
    })

    it('零百分比', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      const result = formatPercentage(0)
      expect(result).toContain('0')
      expect(result).toContain('%')
    })

    it('100 百分比', async () => {
      const { formatPercentage } = await import('@/utils/formatUtils')
      const result = formatPercentage(100)
      expect(result).toContain('100')
    })
  })

  // ---------- formatDuration ----------

  describe('formatDuration', () => {
    it('null 返回 "-"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(undefined)).toBe('-')
    })

    it('NaN 返回 "-"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(NaN)).toBe('-')
    })

    it('5000ms 返回 "5s"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(5000)).toBe('5s')
    })

    it('65000ms 返回 "1m 5s"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(65000)).toBe('1m 5s')
    })

    it('3665000ms 返回 "1h 1m 5s"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(3665000)).toBe('1h 1m 5s')
    })

    it('0ms 返回 "0s"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(0)).toBe('0s')
    })

    it('3600000ms 返回 "1h"', async () => {
      const { formatDuration } = await import('@/utils/formatUtils')
      expect(formatDuration(3600000)).toBe('1h')
    })
  })

  // ---------- formatCompactNumber ----------

  describe('formatCompactNumber', () => {
    it('null 返回 "-"', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      expect(formatCompactNumber(null)).toBe('-')
    })

    it('undefined 返回 "-"', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      expect(formatCompactNumber(undefined)).toBe('-')
    })

    it('NaN 返回 "-"', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      expect(formatCompactNumber(NaN)).toBe('-')
    })

    it('1234 格式化为紧凑形式', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      const result = formatCompactNumber(1234)
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('3456789 格式化为紧凑形式', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      const result = formatCompactNumber(3456789)
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })

    it('零返回 "0"', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      expect(formatCompactNumber(0)).toBe('0')
    })

    it('负数格式化', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      const result = formatCompactNumber(-1234)
      expect(result).toBeTruthy()
    })

    it('非常大的数字', async () => {
      const { formatCompactNumber } = await import('@/utils/formatUtils')
      const result = formatCompactNumber(999999999999)
      expect(result).toBeTruthy()
      expect(result).not.toBe('-')
    })
  })
})
