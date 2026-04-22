import request from '@/utils/request'

export interface Agent {
  id?: number
  name: string
  description?: string
  config?: any
  graphDefinition?: any
  isActive?: boolean
  status?: string
  type?: string
  isTemplate?: boolean
  category?: string
  creator?: string
  usageCount?: number
  rating?: number
  favorited?: boolean
  latestVersionId?: number
  createdAt?: string
  updatedAt?: string
  [key: string]: any
}

export interface AgentVersion {
  id: number
  agentId: number
  versionNumber: number
  graphDefinition?: any
  config?: any
  status?: string
  remark?: string
  createdAt: string
  updatedAt?: string
  [key: string]: any
}

/**
 * 获取所有 Agent 列表
 */
export function getAllAgents() {
  return request.get('/agents')
}

/**
 * 获取 Agent 详情
 */
export function getAgentById(id: string | number) {
  return request.get(`/agents/${id}`)
}

/**
 * 创建 Agent
 */
export function createAgent(data: any) {
  return request.post('/agents', data)
}

/**
 * 更新 Agent
 */
export function updateAgent(id: string | number, data: any) {
  return request.put(`/agents/${id}`, data)
}

/**
 * 删除 Agent
 */
export function deleteAgent(id: string | number) {
  return request.delete(`/agents/${id}`)
}

/**
 * 复制 Agent
 */
export function copyAgent(id: string | number, data?: any) {
  return request.post(`/agents/${id}/copy`, data)
}

/**
 * 获取 Agent 版本列表
 */
export function getAgentVersions(id: string | number) {
  return request.get(`/agents/${id}/versions`)
}

/**
 * 获取 Agent 特定版本
 */
export function getAgentVersion(id: string | number, versionNumber: number) {
  return request.get(`/agents/${id}/versions/${versionNumber}`)
}

/**
 * 回滚到指定版本
 */
export function rollbackToVersion(id: string | number, versionNumber: number) {
  return request.post(`/agents/${id}/versions/${versionNumber}/rollback`)
}

/**
 * 默认导出 agentApi 对象，兼容对象式调用
 */
const agentApi = {
  getAllAgents,
  getAgentById,
  createAgent,
  updateAgent,
  deleteAgent,
  copyAgent,
  getAgentVersions,
  getAgentVersion,
  rollbackToVersion,
}

export { agentApi }
export default agentApi
