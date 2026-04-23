import request from '@/utils/request'

// 生成建议
export const generateSuggestions = (agentId: number) => {
  return request({
    url: `/suggestions/generate/${agentId}`,
    method: 'post'
  })
}

// 创建建议
export const createSuggestion = (data: Record<string, unknown>) => {
  return request({
    url: '/suggestions',
    method: 'post',
    data
  })
}

// 更新建议
export const updateSuggestion = (id: number, data: Record<string, unknown>) => {
  return request({
    url: `/suggestions/${id}`,
    method: 'put',
    data
  })
}

// 删除建议
export const deleteSuggestion = (id: number) => {
  return request({
    url: `/suggestions/${id}`,
    method: 'delete'
  })
}

// 获取单个建议
export const getSuggestion = (id: number) => {
  return request({
    url: `/suggestions/${id}`,
    method: 'get'
  })
}

// 获取所有建议
export const getAllSuggestions = () => {
  return request({
    url: '/suggestions',
    method: 'get'
  })
}

// 搜索建议
export const searchSuggestions = (params: Record<string, unknown>) => {
  return request({
    url: '/suggestions/search',
    method: 'get',
    params
  })
}

// 按Agent ID获取建议
export const getSuggestionsByAgentId = (agentId: number) => {
  return request({
    url: `/suggestions/agent/${agentId}`,
    method: 'get'
  })
}

// 按类型获取建议
export const getSuggestionsByType = (type: string) => {
  return request({
    url: `/suggestions/type/${type}`,
    method: 'get'
  })
}

// 按优先级获取建议
export const getSuggestionsByPriority = (agentId: number) => {
  return request({
    url: `/suggestions/priority/${agentId}`,
    method: 'get'
  })
}

// 更新建议状态
export const updateSuggestionStatus = (id: number, status: string) => {
  return request({
    url: `/suggestions/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 更新实现状态
export const updateImplementationStatus = (id: number, implementationStatus: string) => {
  return request({
    url: `/suggestions/${id}/implementation-status`,
    method: 'put',
    params: { implementationStatus }
  })
}

// 分析建议效果
export const analyzeSuggestionEffectiveness = () => {
  return request({
    url: '/suggestions/analysis/effectiveness',
    method: 'get'
  })
}
