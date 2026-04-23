/**
 * 统一的认证信息存储工具
 * 支持 localStorage 和 sessionStorage 双存储策略
 * 所有读取逻辑统一：先 localStorage，后 sessionStorage
 */

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string, remember: boolean = false): void {
  const storage = remember ? localStorage : sessionStorage
  const otherStorage = remember ? sessionStorage : localStorage
  storage.setItem(TOKEN_KEY, token)
  otherStorage.removeItem(TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string, remember: boolean = false): void {
  const storage = remember ? localStorage : sessionStorage
  const otherStorage = remember ? sessionStorage : localStorage
  storage.setItem(REFRESH_TOKEN_KEY, token)
  otherStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getUserInfo(): Record<string, unknown> | null {
  const raw = localStorage.getItem(USER_INFO_KEY) || sessionStorage.getItem(USER_INFO_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setUserInfo(info: Record<string, unknown>, remember: boolean = false): void {
  const storage = remember ? localStorage : sessionStorage
  const otherStorage = remember ? sessionStorage : localStorage
  storage.setItem(USER_INFO_KEY, JSON.stringify(info))
  otherStorage.removeItem(USER_INFO_KEY)
}

export function clearAuth(_remember: boolean = false): void {
  // 清除两个存储
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(USER_INFO_KEY)
}

export function isRemembered(): boolean {
  return !!localStorage.getItem(TOKEN_KEY)
}
