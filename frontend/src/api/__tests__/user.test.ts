import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * User API 测试
 * 测试 API 路径正确性（使用 /v1 前缀）
 * 测试 HTTP 方法正确性
 * 测试 DTO 转换正确性
 */

// Mock request 模块，记录调用参数
const mockRequestCalls: Array<{ method: string; url: string; data?: unknown; params?: unknown }> = []

const mockRequest = {
  get: vi.fn((url: string, config?: { params?: unknown }) => {
    mockRequestCalls.push({ method: 'get', url, params: config?.params })
    return Promise.resolve({ code: 200, data: null, message: 'ok' })
  }),
  post: vi.fn((url: string, data?: unknown) => {
    mockRequestCalls.push({ method: 'post', url, data })
    return Promise.resolve({ code: 200, data: null, message: 'ok' })
  }),
  put: vi.fn((url: string, data?: unknown) => {
    mockRequestCalls.push({ method: 'put', url, data })
    return Promise.resolve({ code: 200, data: null, message: 'ok' })
  }),
  delete: vi.fn((url: string) => {
    mockRequestCalls.push({ method: 'delete', url })
    return Promise.resolve({ code: 200, data: null, message: 'ok' })
  }),
  patch: vi.fn((url: string, data?: unknown) => {
    mockRequestCalls.push({ method: 'patch', url, data })
    return Promise.resolve({ code: 200, data: null, message: 'ok' })
  })
}

vi.mock('@/utils/request', () => ({
  default: mockRequest
}))

describe('User API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRequestCalls.length = 0
  })

  // ==================== 认证路径正确性（/v1 前缀） ====================

  describe('认证路径正确性', () => {
    it('login 使用 POST /v1/auth/login', async () => {
      const { login } = await import('@/api/user')
      await login({ username: 'admin', password: '123456' })

      expect(mockRequest.post).toHaveBeenCalledTimes(1)
      expect(mockRequestCalls[0]).toEqual({
        method: 'post',
        url: '/v1/auth/login',
        data: { username: 'admin', password: '123456' }
      })
    })

    it('getUserInfo 使用 GET /v1/auth/userinfo', async () => {
      const { getUserInfo } = await import('@/api/user')
      await getUserInfo()

      expect(mockRequest.get).toHaveBeenCalledTimes(1)
      expect(mockRequestCalls[0]).toEqual({
        method: 'get',
        url: '/v1/auth/userinfo',
        params: undefined
      })
    })

    it('logout 使用 POST /v1/auth/logout', async () => {
      const { logout } = await import('@/api/user')
      await logout()

      expect(mockRequest.post).toHaveBeenCalledTimes(1)
      expect(mockRequestCalls[0]).toEqual({
        method: 'post',
        url: '/v1/auth/logout',
        data: undefined
      })
    })
  })

  // ==================== 用户管理路径正确性 ====================

  describe('用户管理路径正确性', () => {
    it('getUsers 使用 GET /v1/users', async () => {
      const { getUsers } = await import('@/api/user')
      await getUsers()

      expect(mockRequest.get).toHaveBeenCalledWith('/v1/users')
    })

    it('getUserById 使用 GET /v1/users/:id', async () => {
      const { getUserById } = await import('@/api/user')
      await getUserById(42)

      expect(mockRequest.get).toHaveBeenCalledWith('/v1/users/42')
    })

    it('createUser 使用 POST /v1/users', async () => {
      const { createUser } = await import('@/api/user')
      const data = { username: 'newuser', password: 'pass' }
      await createUser(data)

      expect(mockRequest.post).toHaveBeenCalledWith('/v1/users', data)
    })

    it('updateUser 使用 PUT /v1/users/:id', async () => {
      const { updateUser } = await import('@/api/user')
      const data = { nickname: 'updated' }
      await updateUser(42, data)

      expect(mockRequest.put).toHaveBeenCalledWith('/v1/users/42', data)
    })

    it('deleteUser 使用 DELETE /v1/users/:id', async () => {
      const { deleteUser } = await import('@/api/user')
      await deleteUser(42)

      expect(mockRequest.delete).toHaveBeenCalledWith('/v1/users/42')
    })

    it('resetUserPassword 使用 POST /v1/users/:id/reset-password', async () => {
      const { resetUserPassword } = await import('@/api/user')
      await resetUserPassword(42, 'newPass123')

      expect(mockRequest.post).toHaveBeenCalledWith('/v1/users/42/reset-password', {
        newPassword: 'newPass123'
      })
    })
  })

  // ==================== 所有路径都以 /v1 开头 ====================

  describe('所有 API 路径都以 /v1 开头', () => {
    it('所有 user API 路径都使用 /v1 前缀', async () => {
      const userApi = await import('@/api/user')

      // 依次调用所有 API 方法
      await userApi.login({ username: 'a', password: 'b' })
      await userApi.getUserInfo()
      await userApi.logout()
      await userApi.getUsers()
      await userApi.getUserById(1)
      await userApi.createUser({})
      await userApi.updateUser(1, {})
      await userApi.deleteUser(1)
      await userApi.resetUserPassword(1, 'pass')

      // 验证所有调用的 URL 都以 /v1 开头
      for (const call of mockRequestCalls) {
        expect(call.url).toMatch(/^\/v1\//)
      }
    })
  })

  // ==================== DTO 转换正确性 ====================

  describe('DTO 转换正确性', () => {
    it('login 请求 DTO 包含 username 和 password', async () => {
      const { login } = await import('@/api/user')
      await login({ username: 'admin', password: 'secret123' })

      const callData = mockRequestCalls[0].data as Record<string, unknown>
      expect(callData).toHaveProperty('username', 'admin')
      expect(callData).toHaveProperty('password', 'secret123')
    })

    it('login 请求 DTO 支持可选的 tenantId', async () => {
      const { login } = await import('@/api/user')
      await login({ username: 'admin', password: 'secret123', tenantId: 100 })

      const callData = mockRequestCalls[0].data as Record<string, unknown>
      expect(callData).toHaveProperty('tenantId', '100')
    })

    it('resetUserPassword 请求 DTO 包含 newPassword 字段', async () => {
      const { resetUserPassword } = await import('@/api/user')
      await resetUserPassword(5, 'NewPass!2024')

      const callData = mockRequestCalls[0].data as Record<string, unknown>
      expect(callData).toHaveProperty('newPassword', 'NewPass!2024')
    })

    it('createUser 请求 DTO 直接传递', async () => {
      const { createUser } = await import('@/api/user')
      const userData = { username: 'test', password: 'pass', email: 'test@test.com', roles: ['user'] }
      await createUser(userData)

      const callData = mockRequestCalls[0].data as Record<string, unknown>
      expect(callData).toEqual(userData)
    })

    it('updateUser 请求 DTO 直接传递', async () => {
      const { updateUser } = await import('@/api/user')
      const updateData = { nickname: '新昵称', email: 'new@test.com' }
      await updateUser(10, updateData)

      const callData = mockRequestCalls[0].data as Record<string, unknown>
      expect(callData).toEqual(updateData)
    })
  })
})
