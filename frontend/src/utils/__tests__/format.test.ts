import { describe, it, expect } from 'vitest'
import { cn } from '@/lib/utils'

/**
 * 工具函数 - format 相关测试
 * 测试 cn (className 合并) 工具函数
 */
describe('cn 工具函数', () => {
  it('合并多个类名', () => {
    const result = cn('px-4', 'py-2', 'bg-red-500')
    expect(result).toContain('px-4')
    expect(result).toContain('py-2')
    expect(result).toContain('bg-red-500')
  })

  it('合并条件类名', () => {
    const isActive = true
    const result = cn('base', isActive && 'active-class')
    expect(result).toContain('base')
    expect(result).toContain('active-class')
  })

  it('条件为 false 时不包含类名', () => {
    const isActive = false
    const result = cn('base', isActive && 'active-class')
    expect(result).toContain('base')
    expect(result).not.toContain('active-class')
  })

  it('处理空字符串和 undefined', () => {
    const result = cn('base', '', undefined, null, 'extra')
    expect(result).toContain('base')
    expect(result).toContain('extra')
  })

  it('处理 tailwind 冲突类名（后者覆盖前者）', () => {
    const result = cn('px-4', 'px-8')
    // tailwind-merge 应该合并冲突的类名
    expect(result).toContain('px-8')
    expect(result).not.toContain('px-4')
  })

  it('处理数组形式的类名', () => {
    const result = cn(['px-4', 'py-2'], 'bg-blue-500')
    expect(result).toContain('px-4')
    expect(result).toContain('py-2')
    expect(result).toContain('bg-blue-500')
  })

  it('处理对象形式的类名', () => {
    const result = cn({
      'text-red-500': true,
      'text-blue-500': false,
      'font-bold': true
    })
    expect(result).toContain('text-red-500')
    expect(result).not.toContain('text-blue-500')
    expect(result).toContain('font-bold')
  })
})

/**
 * 格式化工具函数测试
 */
describe('格式化工具函数', () => {
  // 测试常用的格式化场景
  it('格式化文件大小', () => {
    function formatFileSize(bytes: number): string {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    expect(formatFileSize(0)).toBe('0 B')
    expect(formatFileSize(1024)).toBe('1 KB')
    expect(formatFileSize(1048576)).toBe('1 MB')
    expect(formatFileSize(1073741824)).toBe('1 GB')
  })

  it('格式化日期时间', () => {
    function formatDateTime(dateStr: string): string {
      const date = new Date(dateStr)
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const h = String(date.getHours()).padStart(2, '0')
      const min = String(date.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d} ${h}:${min}`
    }

    expect(formatDateTime('2024-01-15T10:30:00')).toBe('2024-01-15 10:30')
  })

  it('截断文本', () => {
    function truncateText(text: string, maxLength: number): string {
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    }

    expect(truncateText('Hello', 10)).toBe('Hello')
    expect(truncateText('Hello World', 5)).toBe('Hello...')
    expect(truncateText('', 10)).toBe('')
  })
})
