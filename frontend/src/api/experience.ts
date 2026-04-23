import request from '@/utils/request'

export function getExperiences(params?: Record<string, unknown>) {
  return request.get('/v1/experiences', { params })
}

export function getExperienceById(id: number) {
  return request.get(`/v1/experiences/${id}`)
}

export function createExperience(data: Record<string, unknown>) {
  return request.post('/v1/experiences', data)
}

export function updateExperience(id: number, data: Record<string, unknown>) {
  return request.put(`/v1/experiences/${id}`, data)
}

export function deleteExperience(id: number) {
  return request.delete(`/v1/experiences/${id}`)
}

export function analyzeEffectiveness(agentId: number) {
  return request.get(`/v1/experiences/agent/${agentId}/effectiveness`)
}

export function deduplicateExperiences(agentId: number) {
  return request.post(`/v1/experiences/agent/${agentId}/deduplicate`)
}

export function cleanupExperiences(agentId: number) {
  return request.delete(`/v1/experiences/agent/${agentId}/cleanup`)
}
