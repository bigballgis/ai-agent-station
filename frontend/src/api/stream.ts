export function streamChat(params: {
  provider?: string
  systemPrompt?: string
  message: string
  sessionId?: string
}) {
  // SSE needs special handling - return EventSource URL
  const token = localStorage.getItem('token')
  const tenantId = localStorage.getItem('tenantId')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const url = new URL(`${baseUrl}/v1/stream/chat`)
  Object.entries(params).forEach(([k, v]) => {
    if (v) url.searchParams.set(k, v)
  })
  if (token) url.searchParams.set('token', token)
  if (tenantId) url.searchParams.set('X-Tenant-ID', tenantId)
  return new EventSource(url.toString())
}

export function streamAgentExecution(
  agentId: number,
  params: { message: string; sessionId?: string }
) {
  const token = localStorage.getItem('token')
  const tenantId = localStorage.getItem('tenantId')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const url = new URL(`${baseUrl}/v1/stream/agent/${agentId}`)
  Object.entries(params).forEach(([k, v]) => {
    if (v) url.searchParams.set(k, v)
  })
  if (token) url.searchParams.set('token', token)
  if (tenantId) url.searchParams.set('X-Tenant-ID', tenantId)
  return new EventSource(url.toString())
}
