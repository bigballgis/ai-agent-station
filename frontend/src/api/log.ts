import request from '@/utils/request'

export function getLogs(params?: { page?: number; size?: number }) {
  return request.get('/v1/logs', { params })
}

export function getLogsByDateRange(params: {
  startTime: string
  endTime: string
  page?: number
  size?: number
}) {
  return request.get('/v1/logs/date-range', { params })
}

export function getLogsByModule(
  module: string,
  params?: { page?: number; size?: number }
) {
  return request.get(`/v1/logs/module/${module}`, { params })
}

export function getLogById(id: number) {
  return request.get(`/v1/logs/${id}`)
}
