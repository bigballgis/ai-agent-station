import request from '@/utils/request'

export function getApiInterfaces(params?: { page?: number; size?: number }) {
  return request.get('/v1/api-interfaces', { params })
}

export function getApiInterfaceById(id: number) {
  return request.get(`/v1/api-interfaces/${id}`)
}

export function getApiInterfacesByAgent(agentId: number) {
  return request.get(`/v1/api-interfaces/agent/${agentId}`)
}

export function createApiInterface(data: any) {
  return request.post('/v1/api-interfaces', data)
}

export function updateApiInterface(id: number, data: any) {
  return request.put(`/v1/api-interfaces/${id}`, data)
}

export function deleteApiInterface(id: number) {
  return request.delete(`/v1/api-interfaces/${id}`)
}

export function toggleApiInterface(id: number, isActive: boolean) {
  return request.patch(`/v1/api-interfaces/${id}/toggle`, { isActive })
}
