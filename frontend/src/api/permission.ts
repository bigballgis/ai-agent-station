import request from '@/utils/request'

// ==================== Permissions ====================

export function getPermissions() {
  return request.get('/v1/permissions')
}

export function getPermissionById(id: number) {
  return request.get(`/v1/permissions/${id}`)
}

export function createPermission(data: Record<string, unknown>) {
  return request.post('/v1/permissions', data)
}

export function updatePermission(id: number, data: Record<string, unknown>) {
  return request.put(`/v1/permissions/${id}`, data)
}

export function deletePermission(id: number) {
  return request.delete(`/v1/permissions/${id}`)
}

export function assignPermission(roleId: number, permissionId: number) {
  return request.post(`/v1/permissions/roles/${roleId}/permissions/${permissionId}`)
}

export function removePermission(roleId: number, permissionId: number) {
  return request.delete(`/v1/permissions/roles/${roleId}/permissions/${permissionId}`)
}

export function getRolePermissions(roleId: number) {
  return request.get(`/v1/permissions/role/${roleId}`)
}

// ==================== Roles ====================

export function getRoles() {
  return request.get('/v1/roles')
}

export function getRoleById(id: number) {
  return request.get(`/v1/roles/${id}`)
}

export function createRole(data: Record<string, unknown>) {
  return request.post('/v1/roles', data)
}

export function updateRole(id: number, data: Record<string, unknown>) {
  return request.put(`/v1/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/v1/roles/${id}`)
}

export function assignRole(userId: number, roleId: number) {
  return request.post(`/v1/roles/users/${userId}/roles/${roleId}`)
}

export function removeRole(userId: number, roleId: number) {
  return request.delete(`/v1/roles/users/${userId}/roles/${roleId}`)
}

export function getUserRoles(userId: number) {
  return request.get(`/v1/roles/user/${userId}`)
}
