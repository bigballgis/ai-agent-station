import request from '@/utils/request'

/**
 * 获取租户列表
 */
export function getTenants() {
  return request.get('/tenants')
}

/**
 * 获取租户详情
 */
export function getTenantById(id: number) {
  return request.get(`/tenants/${id}`)
}

/**
 * 创建租户
 */
export function createTenant(data: Record<string, unknown>) {
  return request.post('/tenants', data)
}

/**
 * 更新租户
 */
export function updateTenant(id: number, data: Record<string, unknown>) {
  return request.put(`/tenants/${id}`, data)
}

/**
 * 删除租户
 */
export function deleteTenant(id: number) {
  return request.delete(`/tenants/${id}`)
}

/**
 * 重新生成 API Key
 */
export function regenerateApiKey(id: number) {
  return request.post(`/tenants/${id}/regenerate-api-key`)
}
