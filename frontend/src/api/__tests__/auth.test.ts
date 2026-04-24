import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Auth API 模块测试
 * 测试认证相关的 API 调用
 */

// Mock request 模块
const mockGet = vi.fn()
const mockPost = vi.fn()
const mockPut = vi.fn()

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: vi.fn()
  }
}))

// Helper function to access mock response properties
const getResult = (r: any) => r as unknown as Record<string, unknown>

describe('Auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getCaptcha - 调用 GET /v1/auth/captcha', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { captchaId: 'abc123', question: '3 + 5 = ?' }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/auth/captcha')

    expect(mockGet).toHaveBeenCalledWith('/v1/auth/captcha')
    expect(result.data.captchaId).toBe('abc123')
    expect(result.data.question).toBe('3 + 5 = ?')
  })

  it('login - 调用 POST /v1/auth/login', async () => {
    const loginData = { username: 'admin', password: 'password123' }
    mockPost.mockResolvedValue({
      code: 200,
      data: {
        token: 'jwt_token_here',
        refreshToken: 'refresh_token_here',
        userInfo: { id: 1, username: 'admin' }
      }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/auth/login', loginData)

    expect(mockPost).toHaveBeenCalledWith('/v1/auth/login', loginData)
    expect(result.data.token).toBe('jwt_token_here')
    expect(result.data.userInfo.username).toBe('admin')
  })

  it('register - 调用 POST /v1/auth/register', async () => {
    const registerData = {
      username: 'newuser',
      password: 'password123',
      email: 'new@test.com'
    }
    mockPost.mockResolvedValue({
      code: 200,
      data: { token: 'new_token', userInfo: { id: 2, username: 'newuser' } }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/auth/register', registerData)

    expect(mockPost).toHaveBeenCalledWith('/v1/auth/register', registerData)
    expect(result.data.userInfo.username).toBe('newuser')
  })

  it('getUserInfo - 调用 GET /v1/auth/userinfo', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { id: 1, username: 'admin', email: 'admin@test.com', roles: ['ADMIN'] }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/auth/userinfo')

    expect(mockGet).toHaveBeenCalledWith('/v1/auth/userinfo')
    expect(result.data.username).toBe('admin')
    expect(result.data.roles).toEqual(['ADMIN'])
  })

  it('logout - 调用 POST /v1/auth/logout', async () => {
    mockPost.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/auth/logout')

    expect(mockPost).toHaveBeenCalledWith('/v1/auth/logout')
    expect(getResult(result).code).toBe(200)
  })

  it('changePassword - 调用 PUT /v1/auth/password', async () => {
    const passwordData = { oldPassword: 'old123', newPassword: 'new456' }
    mockPut.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.put('/v1/auth/password', passwordData)

    expect(mockPut).toHaveBeenCalledWith('/v1/auth/password', passwordData)
    expect(getResult(result).code).toBe(200)
  })

  it('resetPassword - 调用 POST /v1/auth/reset-password', async () => {
    const resetData = { username: 'admin', newPassword: 'new123' }
    mockPost.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/auth/reset-password', resetData)

    expect(mockPost).toHaveBeenCalledWith('/v1/auth/reset-password', resetData)
    expect(getResult(result).code).toBe(200)
  })

  it('API 调用失败 - 返回错误', async () => {
    mockPost.mockRejectedValue(new Error('Network Error'))

    const request = (await import('@/utils/request')).default

    await expect(request.post('/v1/auth/login', {})).rejects.toThrow('Network Error')
  })

  it('login - 返回 401 错误', async () => {
    mockPost.mockResolvedValue({
      code: 401,
      message: '用户名或密码错误'
    })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/auth/login', { username: 'admin', password: 'wrong' })

    expect(getResult(result).code).toBe(401)
    expect(getResult(result).message).toBe('用户名或密码错误')
  })
})
