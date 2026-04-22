import request from '@/utils/request'

/**
 * 获取租户配额
 */
export function getTenantQuota(tenantId: number) {
  return request.get(`/v1/quotas/tenant/${tenantId}`)
}

/**
 * 获取租户配额详情
 */
export function getTenantQuotaDetails(tenantId: number) {
  return request.get(`/v1/quotas/tenant/${tenantId}/details`)
}

/**
 * 更新租户配额
 */
export function updateTenantQuota(tenantId: number, data: any) {
  return request.put(`/v1/quotas/tenant/${tenantId}`, data)
}
