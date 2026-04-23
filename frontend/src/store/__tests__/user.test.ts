import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/modules/user'

/**
 * User Store 测试
 * 测试用户状态管理
 */

// Mock @/api/user 模块（store 实际导入的是这个模块）
const mockLogin = vi.fn()
const mockGetUserInfo = vi.fn()
const mockLogout = vi.fn()

vi.mock('@/api/user', () => ({
  login: (...args: unknown[]) => mockLogin(...args),
  getUserInfo: () => mockGetUserInfo(),
  logout: () => mockLogout()
}))

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

Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock })

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorageMock.clear()
    vi.clearAllMocks()
  })

  it('初始化 - 从 localStorage 恢复 token', () => {
    localStorage.setItem('token', 'existing_token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, username: 'admin' }))

    const store = useUserStore()

    expect(store.token).toBe('existing_token')
    expect(store.userInfo.username).toBe('admin')
  })

  it('初始化 - 从 sessionStorage 恢复 token（当 localStorage 无值时）', () => {
    sessionStorageMock.setItem('token', 'session_token')
    sessionStorageMock.setItem('userInfo', JSON.stringify({ id: 2, username: 'user2' }))

    const store = useUserStore()

    expect(store.token).toBe('session_token')
    expect(store.userInfo.username).toBe('user2')
  })

  it('isLoggedIn - 有 token 时返回 true', () => {
    localStorage.setItem('token', 'test_token')

    const store = useUserStore()

    expect(store.isLoggedIn).toBe(true)
  })

  it('isLoggedIn - 无 token 时返回 false', () => {
    const store = useUserStore()

    expect(store.isLoggedIn).toBe(false)
  })

  it('setToken - 默认存储到 sessionStorage', () => {
    const store = useUserStore()

    store.setToken('new_token')

    expect(store.token).toBe('new_token')
    expect(sessionStorageMock.getItem('token')).toBe('new_token')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('setToken - remember=true 时存储到 localStorage', () => {
    const store = useUserStore()

    store.setToken('remember_token', true)

    expect(store.token).toBe('remember_token')
    expect(localStorage.getItem('token')).toBe('remember_token')
    expect(sessionStorageMock.getItem('token')).toBeNull()
  })

  it('setUserInfo - 默认存储到 sessionStorage', () => {
    const store = useUserStore()

    store.setUserInfo({ id: 1, username: 'admin', roles: ['admin'] })

    expect(store.userInfo.id).toBe(1)
    expect(store.userInfo.username).toBe('admin')
    expect(store.userInfo.roles).toEqual(['admin'])
    expect(sessionStorageMock.getItem('userInfo')).toContain('admin')
    expect(localStorage.getItem('userInfo')).toBeNull()
  })

  it('setUserInfo - remember=true 时存储到 localStorage', () => {
    const store = useUserStore()

    store.setUserInfo({ id: 1, username: 'admin', roles: ['admin'] }, true)

    expect(store.userInfo.id).toBe(1)
    expect(localStorage.getItem('userInfo')).toContain('admin')
    expect(sessionStorageMock.getItem('userInfo')).toBeNull()
  })

  it('login - 成功登录', async () => {
    mockLogin.mockResolvedValue({
      code: 200,
      data: {
        token: 'login_token',
        userInfo: { id: 1, username: 'admin', email: 'admin@test.com' }
      }
    })

    const store = useUserStore()
    const result = await store.login({ username: 'admin', password: 'password123' })

    expect(result).toBe(true)
    expect(store.token).toBe('login_token')
    expect(store.userInfo.username).toBe('admin')
    // 默认 remember=false，存到 sessionStorage
    expect(sessionStorageMock.getItem('token')).toBe('login_token')
  })

  it('login - 成功登录并记住登录', async () => {
    mockLogin.mockResolvedValue({
      code: 200,
      data: {
        token: 'login_token',
        userInfo: { id: 1, username: 'admin', email: 'admin@test.com' }
      }
    })

    const store = useUserStore()
    const result = await store.login({ username: 'admin', password: 'password123', remember: true })

    expect(result).toBe(true)
    expect(store.token).toBe('login_token')
    // remember=true，存到 localStorage
    expect(localStorage.getItem('token')).toBe('login_token')
    expect(sessionStorageMock.getItem('token')).toBeNull()
  })

  it('login - 登录失败（网络错误）', async () => {
    mockLogin.mockRejectedValue(new Error('密码错误'))

    const store = useUserStore()
    const result = await store.login({ username: 'admin', password: 'wrong' })

    expect(result).toBe(false)
    expect(store.token).toBe('')
  })

  it('login - 返回 code 非 200', async () => {
    mockLogin.mockResolvedValue({
      code: 401,
      message: '密码错误'
    })

    const store = useUserStore()
    const result = await store.login({ username: 'admin', password: 'wrong' })

    expect(result).toBe(false)
  })

  it('logout - 清除 token 和用户信息', async () => {
    localStorage.setItem('token', 'test_token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1 }))

    const store = useUserStore()
    store.setToken('test_token')
    store.setUserInfo({ id: 1, username: 'admin' })

    mockLogout.mockResolvedValue({ code: 200 })

    await store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userInfo')).toBeNull()
    expect(sessionStorageMock.getItem('token')).toBeNull()
    expect(sessionStorageMock.getItem('userInfo')).toBeNull()
  })

  it('logout - 即使 API 调用失败也清除本地状态', async () => {
    localStorage.setItem('token', 'test_token')

    const store = useUserStore()
    store.setToken('test_token')

    mockLogout.mockRejectedValue(new Error('Network Error'))

    await store.logout()

    expect(store.token).toBe('')
    expect(localStorage.getItem('token')).toBeNull()
    expect(sessionStorageMock.getItem('token')).toBeNull()
  })

  it('getUserInfo - 成功获取用户信息', async () => {
    mockGetUserInfo.mockResolvedValue({
      code: 200,
      data: { id: 1, username: 'admin', nickname: '管理员' }
    })

    const store = useUserStore()
    await store.getUserInfo()

    expect(store.userInfo.nickname).toBe('管理员')
  })

  it('getUserInfo - 失败时不影响现有状态', async () => {
    const store = useUserStore()
    store.setUserInfo({ id: 1, username: 'admin' })

    mockGetUserInfo.mockRejectedValue(new Error('Network Error'))

    await store.getUserInfo()

    expect(store.userInfo.username).toBe('admin')
  })
})
