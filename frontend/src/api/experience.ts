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

export function analyzeEffectiveness() {
  return request.get('/v1/experiences/analysis/effectiveness')
}

export function deduplicateExperiences() {
  return request.post('/v1/experiences/deduplicate')
}

export function cleanupExperiences() {
  return request.post('/v1/experiences/cleanup')
}
