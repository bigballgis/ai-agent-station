import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/modules/user'

/**
 * User Store 测试
 * 测试用户状态管理
 */

// Mock request 模块
const mockPost = vi.fn()
const mockGet = vi.fn()

vi.mock('@/utils/request', () => ({
  default: {
    post: mockPost,
    get: mockGet
  }
}))

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('初始化 - 从 localStorage 恢复 token', () => {
    localStorage.setItem('token', 'existing_token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, username: 'admin' }))

    const store = useUserStore()

    expect(store.token).toBe('existing_token')
    expect(store.userInfo.username).toBe('admin')
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

  it('setToken - 更新 token 并存储到 localStorage', () => {
    const store = useUserStore()

    store.setToken('new_token')

    expect(store.token).toBe('new_token')
    expect(localStorage.getItem('token')).toBe('new_token')
  })

  it('setUserInfo - 更新用户信息并存储到 localStorage', () => {
    const store = useUserStore()

    store.setUserInfo({ id: 1, username: 'admin', roles: ['admin'] })

    expect(store.userInfo.id).toBe(1)
    expect(store.userInfo.username).toBe('admin')
    expect(store.userInfo.roles).toEqual(['admin'])
    expect(localStorage.getItem('userInfo')).toContain('admin')
  })

  it('login - 成功登录', async () => {
    mockPost.mockResolvedValue({
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
    expect(localStorage.getItem('token')).toBe('login_token')
  })

  it('login - 登录失败', async () => {
    mockPost.mockRejectedValue(new Error('密码错误'))

    const store = useUserStore()
    const result = await store.login({ username: 'admin', password: 'wrong' })

    expect(result).toBe(false)
    expect(store.token).toBe('')
  })

  it('login - 返回 code 非 200', async () => {
    mockPost.mockResolvedValue({
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

    mockPost.mockResolvedValue({ code: 200 })

    await store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userInfo')).toBeNull()
  })

  it('logout - 即使 API 调用失败也清除本地状态', async () => {
    localStorage.setItem('token', 'test_token')

    const store = useUserStore()
    store.setToken('test_token')

    mockPost.mockRejectedValue(new Error('Network Error'))

    await store.logout()

    expect(store.token).toBe('')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('getUserInfo - 成功获取用户信息', async () => {
    mockGet.mockResolvedValue({
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

    mockGet.mockRejectedValue(new Error('Network Error'))

    await store.getUserInfo()

    expect(store.userInfo.username).toBe('admin')
  })
})
