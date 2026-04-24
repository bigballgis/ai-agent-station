import request from '@/utils/request'

export interface CreateMemoryRequest {
  agentId: number
  sessionId?: string
  memoryType: string
  content: string
  summary?: string
  tags?: string
  importance?: number
  tenantId?: number
  createdBy?: number
}

/**
 * 创建记忆
 */
export function createMemory(data: CreateMemoryRequest) {
  return request.post('/v1/memories', data)
}

/**
 * 获取 Agent 记忆列表
 */
export function getAgentMemories(agentId: string, params?: Record<string, unknown>) {
  return request.get(`/v1/memories/agent/${agentId}`, { params })
}

/**
 * 删除记忆
 */
export function deleteMemory(memoryId: number) {
  return request.delete(`/v1/memories/${memoryId}`)
}

/**
 * 清理过期记忆
 */
export function cleanupAgentMemories(agentId: string) {
  return request.delete(`/v1/memories/agent/${agentId}/cleanup`)
}
