import request from '@/utils/request'
import { getUserInfo } from '@/utils/authStorage'

function getTenantHeaders(): Record<string, string> {
  const userInfo = getUserInfo()
  return userInfo?.tenantId ? { 'X-Tenant-ID': String(userInfo.tenantId) } : {}
}

/**
 * 获取租户配额
 */
export function getTenantQuota(tenantId: number) {
  return request.get(`/v1/quotas/tenant/${tenantId}`, { headers: getTenantHeaders() })
}

/**
 * 获取租户配额详情
 */
export function getTenantQuotaDetails(tenantId: number) {
  return request.get(`/v1/quotas/tenant/${tenantId}/details`, { headers: getTenantHeaders() })
}

/**
 * 更新租户配额
 */
export function updateTenantQuota(tenantId: number, data: Record<string, unknown>) {
  return request.put(`/v1/quotas/tenant/${tenantId}`, data, { headers: getTenantHeaders() })
}
