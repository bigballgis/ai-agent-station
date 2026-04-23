import request from '@/utils/request'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

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
export function createAgent(data: Record<string, unknown>) {
  return request.post('/agents', data)
}

/**
 * 更新 Agent
 */
export function updateAgent(id: string | number, data: Record<string, unknown>) {
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
export function copyAgent(id: string | number, data?: Record<string, unknown> | string) {
  return request.post(`/agents/${id}/copy`, typeof data === 'string' ? { name: data } : data)
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
 * Execute agent via SSE streaming
 *
 * Connects to the backend SSE endpoint (GET /api/v1/stream/agent/{agentId}?message=...)
 * and streams node execution events to the caller.
 *
 * SSE event types from backend:
 * - node_start: { nodeId, nodeType }
 * - token:      { content }
 * - node_end:   { nodeId, status, error? }
 * - done:       { type: "done", content }
 * - error:      { type: "error", content }
 *
 * @param agentId Agent ID
 * @param message User input message
 * @param onMessage Callback for each SSE event
 * @param onError Error callback
 * @param onComplete Completion callback
 * @returns Object with cancel() method to abort the stream
 */
export function executeAgentStream(
  agentId: string | null,
  message: string,
  onMessage: (event: { type: string; data: unknown }) => void,
  onError: (error: Error) => void,
  onComplete: () => void
): { cancel: () => void } {
  const controller = new AbortController()

  const url = agentId
    ? `${BASE_URL}/v1/stream/agent/${agentId}?message=${encodeURIComponent(message)}`
    : `${BASE_URL}/v1/stream/chat?message=${encodeURIComponent(message)}`

  // Attach auth token and tenant header
  const token = localStorage.getItem('token')
  const tenantId = localStorage.getItem('tenantId')
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (tenantId) headers['X-Tenant-ID'] = tenantId

  fetch(url, {
    method: 'GET',
    headers,
    signal: controller.signal,
  })
    .then(response => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      let buffer = ''

      function read() {
        reader?.read().then(({ done, value }) => {
          if (done) {
            // Process any remaining buffer
            if (buffer.trim()) processSSEBuffer(buffer)
            onComplete()
            return
          }
          buffer += decoder.decode(value, { stream: true })

          // Parse SSE format - events are separated by double newlines
          const parts = buffer.split('\n\n')
          // Keep the last (possibly incomplete) part in the buffer
          buffer = parts.pop() || ''

          for (const part of parts) {
            processSSEBlock(part, onMessage)
          }

          read()
        }).catch(err => {
          if (err.name !== 'AbortError') onError(err)
        })
      }
      read()
    })
    .catch(err => {
      if (err.name !== 'AbortError') onError(err)
    })

  return { cancel: () => controller.abort() }
}

/**
 * Process a single SSE block (may contain event: and data: lines)
 */
function processSSEBlock(block: string, onMessage: (event: { type: string; data: unknown }) => void) {
  let eventType = ''
  let dataStr = ''

  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      eventType = line.substring(6).trim()
    } else if (line.startsWith('data:')) {
      dataStr = line.substring(5).trim()
    }
  }

  if (!dataStr) return

  try {
    const data = JSON.parse(dataStr)
    // Use the SSE event name as the type, falling back to data.type
    const type = eventType || data.type || 'message'
    onMessage({ type, data })
  } catch {
    // Non-JSON data, pass as string
    onMessage({ type: eventType || 'message', data: dataStr })
  }
}

/**
 * Process remaining SSE buffer (incomplete blocks at stream end)
 */
function processSSEBuffer(_buffer: string) {
  // Silently ignore incomplete buffer on stream end
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
  executeAgentStream,
}

export { agentApi }
export default agentApi
