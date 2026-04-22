/**
 * 统一 API 响应类型
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/**
 * 分页结果
 */
export interface PageResult<T = any> {
  total: number
  records: T[]
  page?: number
  size?: number
  totalPages?: number
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  page?: number
  size?: number
  sort?: string
  keyword?: string
}

/**
 * Agent DTO
 */
export interface AgentDTO {
  id?: number
  name: string
  description?: string
  type?: string
  status?: string
  config?: Record<string, any>
  tags?: string[]
  tenantId?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * User DTO
 */
export interface UserDTO {
  id?: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  status?: number
  roles?: string[]
  tenantId?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * Login Request
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * UserInfo
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}
