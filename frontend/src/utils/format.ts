/**
 * 格式化日期时间
 */
export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化日期（仅日期部分）
 */
export function formatDateShort(date: string | Date | null | undefined): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
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
