export interface SSEOptions {
  onMessage?: (data: unknown) => void
  onError?: (err: Error) => void
}

import { getToken } from '@/utils/authStorage'

export function streamChat(
  params: {
    provider?: string
    systemPrompt?: string
    message: string
    sessionId?: string
  },
  options?: SSEOptions
): { close: () => void } {
  return createSSEConnection('/v1/stream/chat', params, options?.onMessage, options?.onError)
}

export function streamAgentExecution(
  agentId: number,
  params: { message: string; sessionId?: string },
  options?: SSEOptions
): { close: () => void } {
  return createSSEConnection(`/v1/stream/agent/${agentId}`, params, options?.onMessage, options?.onError)
}

export function createSSEConnection(
  url: string,
  params: Record<string, string>,
  onMessage?: (data: unknown) => void,
  onError?: (err: Error) => void
): { close: () => void } {
  const controller = new AbortController()
  const token = getToken()
  const tenantId = localStorage.getItem('tenantId')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

  const fullUrl = `${baseUrl}${url}`

  fetch(fullUrl, {
    method: 'POST',
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'X-Tenant-ID': tenantId || '',
      'Accept': 'text/event-stream',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(params),
    signal: controller.signal,
  })
    .then(response => {
      if (!response.ok) throw new Error(`SSE connection failed: ${response.status}`)
      const reader = response.body?.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader?.read().then(({ done, value }) => {
          if (done) return
          buffer += decoder.decode(value, { stream: true })

          // Process complete SSE events (separated by double newlines)
          const events = buffer.split('\n\n')
          buffer = events.pop() || '' // Keep incomplete event in buffer

          for (const event of events) {
            const dataLine = event.split('\n').find(l => l.startsWith('data: '))
            if (dataLine) {
              try {
                onMessage?.(JSON.parse(dataLine.substring(6)))
              } catch { /* ignore parse errors */ }
            }
          }
          read()
        }).catch(err => {
          if (err.name !== 'AbortError') onError?.(err)
        })
      }
      read()
    })
    .catch(err => {
      if (err.name !== 'AbortError') onError?.(err)
    })

  return { close: () => controller.abort() }
}
