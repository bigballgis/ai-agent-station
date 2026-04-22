import request from '@/utils/request'

// ==================== Permissions ====================

export function getPermissions() {
  return request.get('/permissions')
}

export function getPermissionById(id: number) {
  return request.get(`/permissions/${id}`)
}

export function createPermission(data: any) {
  return request.post('/permissions', data)
}

export function updatePermission(id: number, data: any) {
  return request.put(`/permissions/${id}`, data)
}

export function deletePermission(id: number) {
  return request.delete(`/permissions/${id}`)
}

export function assignPermission(roleId: number, permissionId: number) {
  return request.post('/permissions/assign', { roleId, permissionId })
}

export function removePermission(roleId: number, permissionId: number) {
  return request.delete('/permissions/remove', { params: { roleId, permissionId } })
}

export function getRolePermissions(roleId: number) {
  return request.get(`/permissions/role/${roleId}`)
}

// ==================== Roles ====================

export function getRoles() {
  return request.get('/roles')
}

export function getRoleById(id: number) {
  return request.get(`/roles/${id}`)
}

export function createRole(data: any) {
  return request.post('/roles', data)
}

export function updateRole(id: number, data: any) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}

export function assignRole(userId: number, roleId: number) {
  return request.post('/roles/assign', { userId, roleId })
}

export function removeRole(userId: number, roleId: number) {
  return request.delete('/roles/remove', { params: { userId, roleId } })
}

export function getUserRoles(userId: number) {
  return request.get(`/roles/user/${userId}`)
}
