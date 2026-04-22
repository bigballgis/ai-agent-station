import request from '@/utils/request'

export function invokeAgent(
  agentId: number,
  data: {
    message: string
    sessionId?: string
    variables?: Record<string, any>
  }
) {
  return request.post(`/v1/agent/${agentId}/invoke`, data)
}

export function getAgentStatus(agentId: number) {
  return request.get(`/v1/agent/${agentId}/status`)
}

export function getTaskStatus(taskId: string) {
  return request.get(`/v1/task/${taskId}`)
}
