import { describe, it, expect, beforeEach } from 'vitest'
import {
  getToken,
  setToken,
  getRefreshToken,
  setRefreshToken,
  getUserInfo,
  setUserInfo,
  clearAuth,
  isRemembered
} from '@/utils/authStorage'

/**
 * authStorage 双存储测试
 * 测试 localStorage / sessionStorage 双存储策略
 * 所有读取逻辑统一：先 localStorage，后 sessionStorage
 */

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} }
  }
})()

// Mock sessionStorage
const sessionStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} }
  }
})()

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })
Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock })

describe('authStorage', () => {
  beforeEach(() => {
    localStorageMock.clear()
    sessionStorageMock.clear()
  })

  // ==================== Token ====================

  describe('getToken', () => {
    it('两个存储都为空时返回 null', () => {
      expect(getToken()).toBeNull()
    })

    it('仅 localStorage 有值时返回 localStorage 的值', () => {
      localStorageMock.setItem('token', 'local_token')
      expect(getToken()).toBe('local_token')
    })

    it('仅 sessionStorage 有值时返回 sessionStorage 的值', () => {
      sessionStorageMock.setItem('token', 'session_token')
      expect(getToken()).toBe('session_token')
    })

    it('两个存储都有值时优先返回 localStorage 的值', () => {
      localStorageMock.setItem('token', 'local_token')
      sessionStorageMock.setItem('token', 'session_token')
      expect(getToken()).toBe('local_token')
    })
  })

  describe('setToken', () => {
    it('remember=false（默认）时存入 sessionStorage 并清除 localStorage', () => {
      localStorageMock.setItem('token', 'old_local_token')
      setToken('new_session_token', false)

      expect(sessionStorageMock.getItem('token')).toBe('new_session_token')
      expect(localStorageMock.getItem('token')).toBeNull()
    })

    it('remember=true 时存入 localStorage 并清除 sessionStorage', () => {
      sessionStorageMock.setItem('token', 'old_session_token')
      setToken('new_local_token', true)

      expect(localStorageMock.getItem('token')).toBe('new_local_token')
      expect(sessionStorageMock.getItem('token')).toBeNull()
    })

    it('默认 remember=false', () => {
      setToken('default_token')
      expect(sessionStorageMock.getItem('token')).toBe('default_token')
      expect(localStorageMock.getItem('token')).toBeNull()
    })
  })

  // ==================== RefreshToken ====================

  describe('getRefreshToken', () => {
    it('两个存储都为空时返回 null', () => {
      expect(getRefreshToken()).toBeNull()
    })

    it('仅 localStorage 有值时返回 localStorage 的值', () => {
      localStorageMock.setItem('refreshToken', 'local_refresh')
      expect(getRefreshToken()).toBe('local_refresh')
    })

    it('仅 sessionStorage 有值时返回 sessionStorage 的值', () => {
      sessionStorageMock.setItem('refreshToken', 'session_refresh')
      expect(getRefreshToken()).toBe('session_refresh')
    })

    it('两个存储都有值时优先返回 localStorage 的值', () => {
      localStorageMock.setItem('refreshToken', 'local_refresh')
      sessionStorageMock.setItem('refreshToken', 'session_refresh')
      expect(getRefreshToken()).toBe('local_refresh')
    })
  })

  describe('setRefreshToken', () => {
    it('remember=false 时存入 sessionStorage 并清除 localStorage', () => {
      localStorageMock.setItem('refreshToken', 'old_local')
      setRefreshToken('new_session', false)

      expect(sessionStorageMock.getItem('refreshToken')).toBe('new_session')
      expect(localStorageMock.getItem('refreshToken')).toBeNull()
    })

    it('remember=true 时存入 localStorage 并清除 sessionStorage', () => {
      sessionStorageMock.setItem('refreshToken', 'old_session')
      setRefreshToken('new_local', true)

      expect(localStorageMock.getItem('refreshToken')).toBe('new_local')
      expect(sessionStorageMock.getItem('refreshToken')).toBeNull()
    })
  })

  // ==================== UserInfo ====================

  describe('getUserInfo', () => {
    it('两个存储都为空时返回 null', () => {
      expect(getUserInfo()).toBeNull()
    })

    it('从 localStorage 读取并解析 JSON', () => {
      localStorageMock.setItem('userInfo', JSON.stringify({ id: 1, username: 'admin' }))
      expect(getUserInfo()).toEqual({ id: 1, username: 'admin' })
    })

    it('从 sessionStorage 读取并解析 JSON', () => {
      sessionStorageMock.setItem('userInfo', JSON.stringify({ id: 2, username: 'user2' }))
      expect(getUserInfo()).toEqual({ id: 2, username: 'user2' })
    })

    it('localStorage 优先于 sessionStorage', () => {
      localStorageMock.setItem('userInfo', JSON.stringify({ id: 1, username: 'local' }))
      sessionStorageMock.setItem('userInfo', JSON.stringify({ id: 2, username: 'session' }))
      expect(getUserInfo()).toEqual({ id: 1, username: 'local' })
    })

    it('JSON 解析失败时返回 null', () => {
      localStorageMock.setItem('userInfo', 'invalid-json{')
      expect(getUserInfo()).toBeNull()
    })
  })

  describe('setUserInfo', () => {
    it('remember=false 时存入 sessionStorage 并清除 localStorage', () => {
      localStorageMock.setItem('userInfo', '{}')
      setUserInfo({ id: 1, username: 'admin' }, false)

      const stored = sessionStorageMock.getItem('userInfo')
      expect(stored).toBeTruthy()
      expect(JSON.parse(stored!)).toEqual({ id: 1, username: 'admin' })
      expect(localStorageMock.getItem('userInfo')).toBeNull()
    })

    it('remember=true 时存入 localStorage 并清除 sessionStorage', () => {
      sessionStorageMock.setItem('userInfo', '{}')
      setUserInfo({ id: 1, username: 'admin' }, true)

      const stored = localStorageMock.getItem('userInfo')
      expect(stored).toBeTruthy()
      expect(JSON.parse(stored!)).toEqual({ id: 1, username: 'admin' })
      expect(sessionStorageMock.getItem('userInfo')).toBeNull()
    })
  })

  // ==================== clearAuth ====================

  describe('clearAuth', () => {
    it('清除两个存储中的所有认证信息', () => {
      localStorageMock.setItem('token', 'local_token')
      localStorageMock.setItem('refreshToken', 'local_refresh')
      localStorageMock.setItem('userInfo', '{}')
      sessionStorageMock.setItem('token', 'session_token')
      sessionStorageMock.setItem('refreshToken', 'session_refresh')
      sessionStorageMock.setItem('userInfo', '{}')

      clearAuth()

      expect(localStorageMock.getItem('token')).toBeNull()
      expect(localStorageMock.getItem('refreshToken')).toBeNull()
      expect(localStorageMock.getItem('userInfo')).toBeNull()
      expect(sessionStorageMock.getItem('token')).toBeNull()
      expect(sessionStorageMock.getItem('refreshToken')).toBeNull()
      expect(sessionStorageMock.getItem('userInfo')).toBeNull()
    })
  })

  // ==================== isRemembered ====================

  describe('isRemembered', () => {
    it('localStorage 有 token 时返回 true', () => {
      localStorageMock.setItem('token', 'local_token')
      expect(isRemembered()).toBe(true)
    })

    it('仅 sessionStorage 有 token 时返回 false', () => {
      sessionStorageMock.setItem('token', 'session_token')
      expect(isRemembered()).toBe(false)
    })

    it('两个存储都为空时返回 false', () => {
      expect(isRemembered()).toBe(false)
    })
  })
})
