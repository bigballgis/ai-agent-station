import request from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import type { LoginRequest, LoginResultData, RegisterRequest, User, UserInfo } from '@/types/user'

// ==================== Auth ====================

export function getCaptcha(): Promise<ApiResponse<{ captchaId: string; question: string }>> {
  return request.get('/v1/auth/captcha')
}

export function login(data: LoginRequest): Promise<ApiResponse<LoginResultData>> {
  return request.post('/v1/auth/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest): Promise<ApiResponse<LoginResultData>> {
  return request.post('/v1/auth/register', data)
}

/** 获取当前登录用户信息 */
export function getUserInfo(): Promise<ApiResponse<UserInfo>> {
  return request.get('/v1/auth/userinfo')
}

/** 登出 */
export function logout(): Promise<ApiResponse<void>> {
  return request.post('/v1/auth/logout')
}

/** 修改密码 - 用户自行修改，需验证旧密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<ApiResponse<void>> {
  return request.put('/v1/auth/password', data)
}

/** 管理员重置密码 */
export function resetPassword(data: { username: string; newPassword: string }): Promise<ApiResponse<void>> {
  return request.post('/v1/auth/reset-password', data)
}

// ==================== Users ====================

export function getUsers(): Promise<ApiResponse<User[]>> {
  return request.get('/v1/users')
}

export function getUserById(id: number): Promise<ApiResponse<User>> {
  return request.get(`/v1/users/${id}`)
}

export function createUser(data: Record<string, unknown>): Promise<ApiResponse<User>> {
  return request.post('/v1/users', data)
}

export function updateUser(id: number, data: Record<string, unknown>): Promise<ApiResponse<User>> {
  return request.put(`/v1/users/${id}`, data)
}

export function deleteUser(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/v1/users/${id}`)
}

export function resetUserPassword(id: number, newPassword: string): Promise<ApiResponse<void>> {
  return request.post(`/v1/users/${id}/reset-password`, { newPassword })
}
