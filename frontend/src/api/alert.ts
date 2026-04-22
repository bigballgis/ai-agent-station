import request from '@/utils/request'

/**
 * 获取告警规则列表
 */
export function getAlertRules() {
  return request.get('/v1/alerts/rules')
}

/**
 * 创建告警规则
 */
export function createAlertRule(data: any) {
  return request.post('/v1/alerts/rules', data)
}

/**
 * 更新告警规则
 */
export function updateAlertRule(id: number, data: any) {
  return request.put(`/v1/alerts/rules/${id}`, data)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: number) {
  return request.delete(`/v1/alerts/rules/${id}`)
}

/**
 * 获取告警记录
 */
export function getAlertRecords(params?: any) {
  return request.get('/v1/alerts/records', { params })
}

/**
 * 获取告警统计
 */
export function getAlertStats() {
  return request.get('/v1/alerts/stats')
}
