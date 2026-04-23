import request from '@/utils/request'

// ==================== Auth ====================

export function login(data: {
  username: string
  password: string
  tenantId?: string
}) {
  return request.post('/auth/login', data)
}

// ==================== Users ====================

export function getUsers() {
  return request.get('/users')
}

export function getUserById(id: number) {
  return request.get(`/users/${id}`)
}

export function createUser(data: Record<string, unknown>) {
  return request.post('/users', data)
}

export function updateUser(id: number, data: Record<string, unknown>) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function resetUserPassword(id: number, newPassword: string) {
  return request.post(`/users/${id}/reset-password`, { newPassword })
}
