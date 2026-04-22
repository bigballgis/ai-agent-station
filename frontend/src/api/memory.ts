import request from '@/utils/request'

/**
 * 获取 Agent 记忆列表
 */
export function getAgentMemories(agentId: string, params?: any) {
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
