/**
 * 格式化工具函数
 *
 * 保留原有 API 向后兼容，内部实现委托给 formatUtils
 * 新代码建议直接使用 formatUtils.ts 中的函数
 */

// 重新导出 formatUtils 中的所有格式化函数
export {
  formatNumber,
  formatCurrency,
  formatDate as formatDateFull,
  formatDateTime,
  formatRelativeTime,
  formatFileSize,
  formatPercentage,
  formatDuration,
  formatCompactNumber,
} from './formatUtils'

import { formatDate as formatDateUtil, formatDateTime as formatDateTimeUtil } from './formatUtils'

/**
 * 格式化日期时间（向后兼容）
 * @deprecated 请使用 formatDateTime from formatUtils
 */
export function formatDate(date: string | Date | null | undefined): string {
  return formatDateTimeUtil(date)
}

/**
 * 格式化日期（仅日期部分，向后兼容）
 * @deprecated 请使用 formatDate from formatUtils (format: 'date')
 */
export function formatDateShort(date: string | Date | null | undefined): string {
  return formatDateUtil(date, 'date')
}

/**
 * 获取状态颜色
 */
export function getStatusColor(status: string): string {
  const colorMap: Record<string, string> = {
    active: 'green',
    published: 'green',
    approved: 'green',
    passed: 'green',
    success: 'green',
    completed: 'green',
    running: 'blue',
    pending: 'orange',
    pending_approval: 'orange',
    reviewing: 'blue',
    testing: 'blue',
    failed: 'red',
    rejected: 'red',
    error: 'red',
    disabled: 'default',
    draft: 'default',
    archived: 'default',
  }
  return colorMap[status] || 'default'
}

/**
 * 获取状态文本
 */
export function getStatusText(status: string): string {
  const textMap: Record<string, string> = {
    active: '活跃',
    published: '已发布',
    approved: '已通过',
    passed: '通过',
    success: '成功',
    completed: '已完成',
    running: '运行中',
    pending: '待处理',
    pending_approval: '待审批',
    reviewing: '审核中',
    testing: '测试中',
    failed: '失败',
    rejected: '已驳回',
    error: '错误',
    disabled: '已禁用',
    draft: '草稿',
    archived: '已归档',
  }
  return textMap[status] || status
}
