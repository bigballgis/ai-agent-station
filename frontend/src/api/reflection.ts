import request from '@/utils/request'

export function getReflections(params?: Record<string, unknown>) {
  return request.get('/v1/reflections', { params })
}

export function getReflectionById(id: number) {
  return request.get(`/v1/reflections/${id}`)
}

export function createReflection(data: Record<string, unknown>) {
  return request.post('/v1/reflections', data)
}

export function triggerEvaluation(agentId: number) {
  return request.post(`/v1/reflections/agent/${agentId}/evaluate`)
}
