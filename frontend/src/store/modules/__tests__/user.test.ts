import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/modules/user'

/**
 * User Store 测试
 * 测试用户状态管理 - 完整覆盖
 */

// ==================== Mocks ====================

const mockLogin = vi.fn()
const mockGetUserInfo = vi.fn()
const mockLogout = vi.fn()

vi.mock('@/api/user', () => ({
  login: (...args: unknown[]) => mockLogin(...args),
  getUserInfo: () => mockGetUserInfo(),
  logout: () => mockLogout()
}))

vi.mock('@/utils/authStorage', () => ({
  setRefreshToken: vi.fn(),
  clearAuth: vi.fn()
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  }
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

Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock, writable: true })

// ==================== Tests ====================

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorageMock.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('1. 默认 token 为空字符串', () => {
      const store = useUserStore()
      expect(store.token).toBe('')
    })

    it('2. 默认 userInfo 为空对象', () => {
      const store = useUserStore()
      expect(store.userInfo).toEqual({})
    })

    it('3. 从 localStorage 恢复 token', () => {
      localStorage.setItem('token', 'existing_token')
      localStorage.setItem('userInfo', JSON.stringify({ id: 1, username: 'admin' }))

      const store = useUserStore()

      expect(store.token).toBe('existing_token')
      expect(store.userInfo.username).toBe('admin')
    })

    it('4. 从 sessionStorage 恢复 token（当 localStorage 无值时）', () => {
      sessionStorageMock.setItem('token', 'session_token')
      sessionStorageMock.setItem('userInfo', JSON.stringify({ id: 2, username: 'user2' }))

      const store = useUserStore()

      expect(store.token).toBe('session_token')
      expect(store.userInfo.username).toBe('user2')
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('5. isLoggedIn - 有 token 时返回 true', () => {
      localStorage.setItem('token', 'test_token')

      const store = useUserStore()
      expect(store.isLoggedIn).toBe(true)
    })

    it('6. isLoggedIn - 无 token 时返回 false', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  // ---------- setToken ----------

  describe('setToken', () => {
    it('7. 默认存储到 sessionStorage', () => {
      const store = useUserStore()
      store.setToken('new_token')

      expect(store.token).toBe('new_token')
      expect(sessionStorageMock.getItem('token')).toBe('new_token')
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('8. remember=true 时存储到 localStorage', () => {
      const store = useUserStore()
      store.setToken('remember_token', true)

      expect(store.token).toBe('remember_token')
      expect(localStorage.getItem('token')).toBe('remember_token')
      expect(sessionStorageMock.getItem('token')).toBeNull()
    })

    it('9. 切换存储位置时清除另一个存储', () => {
      const store = useUserStore()

      // 先存到 sessionStorage
      store.setToken('session_tok')
      expect(sessionStorageMock.getItem('token')).toBe('session_tok')

      // 再存到 localStorage（应清除 sessionStorage）
      store.setToken('local_tok', true)
      expect(localStorage.getItem('token')).toBe('local_tok')
      expect(sessionStorageMock.getItem('token')).toBeNull()
    })
  })

  // ---------- setUserInfo ----------

  describe('setUserInfo', () => {
    it('10. 默认存储到 sessionStorage', () => {
      const store = useUserStore()
      store.setUserInfo({ id: 1, username: 'admin', roles: ['admin'] })

      expect(store.userInfo.id).toBe(1)
      expect(store.userInfo.username).toBe('admin')
      expect(store.userInfo.roles).toEqual(['admin'])
      expect(sessionStorageMock.getItem('userInfo')).toContain('admin')
      expect(localStorage.getItem('userInfo')).toBeNull()
    })

    it('11. remember=true 时存储到 localStorage', () => {
      const store = useUserStore()
      store.setUserInfo({ id: 1, username: 'admin', roles: ['admin'] }, true)

      expect(store.userInfo.id).toBe(1)
      expect(localStorage.getItem('userInfo')).toContain('admin')
      expect(sessionStorageMock.getItem('userInfo')).toBeNull()
    })
  })

  // ---------- login action ----------

  describe('login', () => {
    it('12. 成功登录 - 设置 token 和 userInfo', async () => {
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
      expect(store.userInfo.email).toBe('admin@test.com')
    })

    it('13. 成功登录 - 默认 remember=false 存到 sessionStorage', async () => {
      mockLogin.mockResolvedValue({
        code: 200,
        data: {
          token: 'login_token',
          userInfo: { id: 1, username: 'admin' }
        }
      })

      const store = useUserStore()
      await store.login({ username: 'admin', password: 'password123' })

      expect(sessionStorageMock.getItem('token')).toBe('login_token')
    })

    it('14. 成功登录 - remember=true 存到 localStorage', async () => {
      mockLogin.mockResolvedValue({
        code: 200,
        data: {
          token: 'login_token',
          userInfo: { id: 1, username: 'admin' }
        }
      })

      const store = useUserStore()
      await store.login({ username: 'admin', password: 'password123', remember: true })

      expect(localStorage.getItem('token')).toBe('login_token')
      expect(sessionStorageMock.getItem('token')).toBeNull()
    })

    it('15. 成功登录 - code 为 0 也视为成功', async () => {
      mockLogin.mockResolvedValue({
        code: 0,
        data: {
          token: 'login_token_0',
          userInfo: { id: 2, username: 'user' }
        }
      })

      const store = useUserStore()
      const result = await store.login({ username: 'user', password: 'pass' })

      expect(result).toBe(true)
      expect(store.token).toBe('login_token_0')
    })

    it('16. 登录失败 - 返回 code 非 200/0', async () => {
      mockLogin.mockResolvedValue({
        code: 401,
        message: '密码错误'
      })

      const store = useUserStore()
      const result = await store.login({ username: 'admin', password: 'wrong' })

      expect(result).toBe(false)
      expect(store.token).toBe('')
    })

    it('17. 登录失败 - 网络错误', async () => {
      mockLogin.mockRejectedValue(new Error('网络错误'))

      const store = useUserStore()
      const result = await store.login({ username: 'admin', password: 'pass' })

      expect(result).toBe(false)
      expect(store.token).toBe('')
      expect(store.userInfo).toEqual({})
    })

    it('18. 登录时传递验证码参数', async () => {
      mockLogin.mockResolvedValue({
        code: 200,
        data: {
          token: 'token',
          userInfo: { id: 1, username: 'admin' }
        }
      })

      const store = useUserStore()
      await store.login({
        username: 'admin',
        password: 'pass',
        captchaId: 'captcha-123',
        captchaAnswer: '5'
      })

      expect(mockLogin).toHaveBeenCalledWith({
        username: 'admin',
        password: 'pass',
        captchaId: 'captcha-123',
        captchaAnswer: '5'
      })
    })
  })

  // ---------- logout action ----------

  describe('logout', () => {
    it('19. 登出 - 清除 token 和 userInfo', async () => {
      const store = useUserStore()
      store.setToken('test_token')
      store.setUserInfo({ id: 1, username: 'admin' })

      mockLogout.mockResolvedValue({ code: 200 })

      await store.logout()

      expect(store.token).toBe('')
      expect(store.userInfo).toEqual({})
    })

    it('20. 登出 - 即使 API 失败也清除本地状态', async () => {
      const store = useUserStore()
      store.setToken('test_token')

      mockLogout.mockRejectedValue(new Error('Network Error'))

      await store.logout()

      expect(store.token).toBe('')
      expect(store.userInfo).toEqual({})
    })
  })

  // ---------- getUserInfo action ----------

  describe('getUserInfo', () => {
    it('21. 成功获取用户信息', async () => {
      mockGetUserInfo.mockResolvedValue({
        code: 200,
        data: { id: 1, username: 'admin', nickname: '管理员' }
      })

      const store = useUserStore()
      await store.getUserInfo()

      expect(store.userInfo.nickname).toBe('管理员')
    })

    it('22. 获取用户信息失败不影响现有状态', async () => {
      const store = useUserStore()
      store.setUserInfo({ id: 1, username: 'admin' })

      mockGetUserInfo.mockRejectedValue(new Error('Network Error'))

      await store.getUserInfo()

      expect(store.userInfo.username).toBe('admin')
    })

    it('23. code 为 0 也视为成功', async () => {
      mockGetUserInfo.mockResolvedValue({
        code: 0,
        data: { id: 1, username: 'admin', roles: ['user'] }
      })

      const store = useUserStore()
      await store.getUserInfo()

      expect(store.userInfo.roles).toEqual(['user'])
    })
  })
})
