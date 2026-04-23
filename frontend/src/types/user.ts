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
  captchaId?: string
  captchaAnswer?: string
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  email?: string
  tenantId?: number
}

/** 登录接口实际返回的 data 结构（与后端 /v1/auth/login 对齐） */
export interface LoginResultData {
  token: string
  userInfo: UserInfo
}

/** 用户信息（与后端 /v1/auth/userinfo 对齐） */
export interface UserInfo {
  id?: number
  username?: string
  nickname?: string
  avatar?: string
  roles?: string[]
  permissions?: string[]
}

/** 标准 OAuth2 风格登录响应（备用，当前后端未使用） */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}
