export interface User {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: string
  tenantId?: number
  tenantName?: string
  roles?: string[]
  createdAt?: string
}

export interface LoginRequest {
  username: string
  password: string
  tenantId?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}
