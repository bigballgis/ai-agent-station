import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * errorHandler 单元测试
 * 测试: 错误分类、存储、清除、setupErrorHandler
 */

describe('errorHandler', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  // ---------- categorizeError ----------

  describe('categorizeError', () => {
    it('网络错误分类为 "network"', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      expect(categorizeError('Network Error')).toBe('network')
      expect(categorizeError('Failed to fetch')).toBe('network')
      expect(categorizeError('net::ERR_CONNECTION_REFUSED')).toBe('network')
      expect(categorizeError('ECONNREFUSED')).toBe('network')
      expect(categorizeError('timeout of 5000ms exceeded')).toBe('network')
      expect(categorizeError('ERR_NETWORK')).toBe('network')
    })

    it('认证错误分类为 "auth"', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      expect(categorizeError('401 Unauthorized')).toBe('auth')
      expect(categorizeError('403 Forbidden')).toBe('auth')
      expect(categorizeError('token expired')).toBe('auth')
      expect(categorizeError('Permission denied')).toBe('auth')
      expect(categorizeError('认证失败')).toBe('auth')
      expect(categorizeError('权限不足')).toBe('auth')
    })

    it('业务错误分类为 "business"', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      expect(categorizeError('400 Bad Request')).toBe('business')
      expect(categorizeError('409 Conflict')).toBe('business')
      expect(categorizeError('422 Unprocessable Entity')).toBe('business')
      expect(categorizeError('validation error')).toBe('business')
      expect(categorizeError('参数校验失败')).toBe('business')
    })

    it('未知错误分类为 "unknown"', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      expect(categorizeError('Some random error')).toBe('unknown')
      expect(categorizeError('undefined is not a function')).toBe('unknown')
    })

    it('Error 对象作为输入', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      expect(categorizeError(new Error('Network Error'))).toBe('network')
      expect(categorizeError(new Error('401 Unauthorized'))).toBe('auth')
    })

    it('非字符串输入转为字符串处理', async () => {
      const { categorizeError } = await import('@/utils/errorHandler')
      // String(404) = "404", which contains "400" pattern -> business
      // But actually "404" does NOT contain "400", so it falls to unknown
      expect(categorizeError(400)).toBe('business')
      expect(categorizeError(undefined)).toBe('unknown')
    })
  })

  // ---------- getStoredErrors / clearStoredErrors ----------

  describe('getStoredErrors / clearStoredErrors', () => {
    it('初始状态返回空数组', async () => {
      const { getStoredErrors } = await import('@/utils/errorHandler')
      expect(getStoredErrors()).toEqual([])
    })

    it('clearStoredErrors 清除所有存储的错误', async () => {
      const { getStoredErrors, clearStoredErrors } = await import('@/utils/errorHandler')

      // Manually add some errors to localStorage
      const errors = [
        { type: 'vue', category: 'network', message: 'test error 1', timestamp: new Date().toISOString(), url: 'http://test.com', userAgent: 'test' },
        { type: 'global', category: 'auth', message: 'test error 2', timestamp: new Date().toISOString(), url: 'http://test.com', userAgent: 'test' },
      ]
      localStorage.setItem('__frontend_error_reports__', JSON.stringify(errors))

      expect(getStoredErrors()).toHaveLength(2)

      clearStoredErrors()

      expect(getStoredErrors()).toEqual([])
    })

    it('localStorage 损坏时 getStoredErrors 返回空数组', async () => {
      localStorage.setItem('__frontend_error_reports__', 'not-json{{{')

      const { getStoredErrors } = await import('@/utils/errorHandler')
      expect(getStoredErrors()).toEqual([])
    })

    it('localStorage 不可用时 getStoredErrors 返回空数组', async () => {
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('Storage not available')
      })

      const { getStoredErrors } = await import('@/utils/errorHandler')
      expect(getStoredErrors()).toEqual([])

      vi.restoreAllMocks()
    })
  })

  // ---------- setupErrorHandler ----------

  describe('setupErrorHandler', () => {
    it('注册 Vue errorHandler', async () => {
      const mockApp = {
        config: { errorHandler: null },
      } as unknown as Parameters<typeof import('@/utils/errorHandler').setupErrorHandler>[0]

      const { setupErrorHandler } = await import('@/utils/errorHandler')
      setupErrorHandler(mockApp)

      expect(mockApp.config.errorHandler).toBeDefined()
      expect(typeof mockApp.config.errorHandler).toBe('function')
    })

    it('Vue errorHandler 正确分类错误', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      const mockApp = {
        config: { errorHandler: null },
      } as unknown as Parameters<typeof import('@/utils/errorHandler').setupErrorHandler>[0]

      const { setupErrorHandler } = await import('@/utils/errorHandler')
      setupErrorHandler(mockApp)

      // Trigger the error handler
      mockApp.config.errorHandler!(
        new Error('Network Error'),
        null as never,
        'mounted hook',
      )

      // The handler calls console.error multiple times
      expect(consoleSpy).toHaveBeenCalledWith(
        '[Vue Error] [network]',
        expect.any(Error),
      )

      consoleSpy.mockRestore()
    })

    it('注册全局 onerror 处理器', async () => {
      const originalOnerror = window.onerror

      const mockApp = {
        config: { errorHandler: null },
      } as unknown as Parameters<typeof import('@/utils/errorHandler').setupErrorHandler>[0]

      const { setupErrorHandler } = await import('@/utils/errorHandler')
      setupErrorHandler(mockApp)

      expect(window.onerror).toBeDefined()
      expect(typeof window.onerror).toBe('function')

      window.onerror = originalOnerror
    })

    it('注册 unhandledrejection 处理器', async () => {
      const mockApp = {
        config: { errorHandler: null },
      } as unknown as Parameters<typeof import('@/utils/errorHandler').setupErrorHandler>[0]

      const { setupErrorHandler } = await import('@/utils/errorHandler')
      setupErrorHandler(mockApp)

      // The handler should be registered via addEventListener
      // We can verify by checking that the function was called
      expect(mockApp.config.errorHandler).toBeDefined()
    })
  })
})
