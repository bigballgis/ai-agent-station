import request from '@/utils/request'

// 生成建议
export const generateSuggestions = (agentId: number) => {
  return request.post(`/v1/suggestions/generate/${agentId}`)
}

// 创建建议
export const createSuggestion = (data: Record<string, unknown>) => {
  return request.post('/v1/suggestions', data)
}

// 更新建议
export const updateSuggestion = (id: number, data: Record<string, unknown>) => {
  return request.put(`/v1/suggestions/${id}`, data)
}

// 删除建议
export const deleteSuggestion = (id: number) => {
  return request.delete(`/v1/suggestions/${id}`)
}

// 获取单个建议
export const getSuggestion = (id: number) => {
  return request.get(`/v1/suggestions/${id}`)
}

// 获取所有建议
export const getAllSuggestions = () => {
  return request.get('/v1/suggestions')
}

// 搜索建议
export const searchSuggestions = (params: Record<string, unknown>) => {
  return request.get('/v1/suggestions/search', { params })
}

// 按Agent ID获取建议
export const getSuggestionsByAgentId = (agentId: number) => {
  return request.get(`/v1/suggestions/agent/${agentId}`)
}

// 按类型获取建议
export const getSuggestionsByType = (type: string) => {
  return request.get(`/v1/suggestions/type/${type}`)
}

// 按优先级获取建议
export const getSuggestionsByPriority = (agentId: number) => {
  return request.get(`/v1/suggestions/priority/${agentId}`)
}

// 更新建议状态
export const updateSuggestionStatus = (id: number, status: string) => {
  return request.put(`/v1/suggestions/${id}/status`, null, { params: { status } })
}

// 更新实现状态
export const updateImplementationStatus = (id: number, implementationStatus: string) => {
  return request.put(`/v1/suggestions/${id}/implementation-status`, null, { params: { implementationStatus } })
}

// 分析建议效果
export const analyzeSuggestionEffectiveness = () => {
  return request.get('/v1/suggestions/analysis/effectiveness')
}
