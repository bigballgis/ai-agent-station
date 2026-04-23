import request from '@/utils/request'
import type { ApiResponse } from '@/types/common'

export interface ExecutionHistoryItem {
  id: number
  agentId: number
  tenantId?: number
  userId?: number
  message: string
  role: string
  timestamp: string
}

/** 获取Agent执行历史 */
export function getExecutionHistory(agentId: number, page: number = 0, size: number = 50): Promise<ApiResponse<ExecutionHistoryItem[]>> {
  return request.get(`/v1/execution-history/agent/${agentId}`, { params: { page, size } })
}

/** 清除Agent执行历史 */
export function deleteExecutionHistory(agentId: number): Promise<ApiResponse<void>> {
  return request.delete(`/v1/execution-history/agent/${agentId}`)
}
