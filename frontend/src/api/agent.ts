import request from '@/utils/request'

export interface Agent {
  id?: number
  name: string
  description?: string
  config?: Record<string, unknown>
  graphDefinition?: Record<string, unknown>
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
  [key: string]: unknown
}

export interface AgentVersion {
  id: number
  agentId: number
  versionNumber: number
  graphDefinition?: Record<string, unknown>
  config?: Record<string, unknown>
  status?: string
  remark?: string
  createdAt: string
  updatedAt?: string
  [key: string]: unknown
}

/**
 * 获取所有 Agent 列表
 */
export function getAllAgents() {
  return request.get('/v1/agents')
}

/**
 * 获取 Agent 详情
 */
export function getAgentById(id: string | number) {
  return request.get(`/v1/agents/${id}`)
}

/**
 * 创建 Agent
 */
export function createAgent(data: Record<string, unknown>) {
  return request.post('/v1/agents', data)
}

/**
 * 更新 Agent
 */
export function updateAgent(id: string | number, data: Record<string, unknown>) {
  return request.put(`/v1/agents/${id}`, data)
}

/**
 * 删除 Agent
 */
export function deleteAgent(id: string | number) {
  return request.delete(`/v1/agents/${id}`)
}

/**
 * 复制 Agent
 */
export function copyAgent(id: string | number, data?: Record<string, unknown> | string) {
  return request.post(`/v1/agents/${id}/copy`, typeof data === 'string' ? { newName: data } : data)
}

/**
 * 获取 Agent 版本列表
 */
export function getAgentVersions(id: string | number) {
  return request.get(`/v1/agents/${id}/versions`)
}

/**
 * 获取 Agent 特定版本
 */
export function getAgentVersion(id: string | number, versionNumber: number) {
  return request.get(`/v1/agents/${id}/versions/${versionNumber}`)
}

/**
 * 回滚到指定版本
 */
export function rollbackToVersion(id: string | number, versionNumber: number) {
  return request.post(`/v1/agents/${id}/versions/${versionNumber}/rollback`)
}

/**
 * 导出单个Agent为JSON
 */
export function exportAgent(id: string | number) {
  return request.get(`/v1/agents/export`, { params: { id }, responseType: 'blob' })
}

/**
 * 导出所有Agent为JSON
 */
export function exportAllAgents() {
  return request.get('/v1/agents/export-all', { responseType: 'blob' })
}

/**
 * 导入Agent从JSON
 */
export function importAgent(data: Record<string, unknown>) {
  return request.post('/v1/agents/import', data)
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
  exportAgent,
  exportAllAgents,
  importAgent,
}

export { agentApi }
export default agentApi
