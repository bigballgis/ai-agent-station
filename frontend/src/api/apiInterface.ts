import request from '@/utils/request'
import { getUserInfo } from '@/utils/authStorage'

function getTenantHeaders(): Record<string, string> {
  const userInfo = getUserInfo()
  return userInfo?.tenantId ? { 'X-Tenant-ID': String(userInfo.tenantId) } : {}
}

export function getApiInterfaces(params?: { page?: number; size?: number }) {
  return request.get('/v1/api-interfaces', { params, headers: getTenantHeaders() })
}

export function getApiInterfaceById(id: number) {
  return request.get(`/v1/api-interfaces/${id}`, { headers: getTenantHeaders() })
}

export function getApiInterfacesByAgent(agentId: number) {
  return request.get(`/v1/api-interfaces/agent/${agentId}`, { headers: getTenantHeaders() })
}

export function createApiInterface(data: Record<string, unknown>) {
  return request.post('/v1/api-interfaces', data, { headers: getTenantHeaders() })
}

export function updateApiInterface(id: number, data: Record<string, unknown>) {
  return request.put(`/v1/api-interfaces/${id}`, data, { headers: getTenantHeaders() })
}

export function deleteApiInterface(id: number) {
  return request.delete(`/v1/api-interfaces/${id}`, { headers: getTenantHeaders() })
}

export function toggleApiInterface(id: number, isActive: boolean) {
  return request.patch(`/v1/api-interfaces/${id}/toggle`, { isActive }, { headers: getTenantHeaders() })
}
