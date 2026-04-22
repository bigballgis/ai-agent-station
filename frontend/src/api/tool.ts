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
