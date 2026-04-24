import request from '@/utils/request'

export function getTools() {
  return request.get('/v1/tools')
}

export function getToolStats() {
  return request.get('/v1/tools/stats')
}

export function getToolSource(toolName: string) {
  return request.get(`/v1/tools/${toolName}/source`)
}

export function refreshTools() {
  return request.post('/v1/tools/refresh')
}

export function getToolsHealth() {
  return request.get('/v1/tools/health')
}

export function testToolConnection(toolId: number | string) {
  return request.post(`/v1/tools/${toolId}/test-connection`)
}
