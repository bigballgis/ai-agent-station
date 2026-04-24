import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/**
 * useNetworkStatus composable 单元测试
 * 测试: online/offline 检测, 请求队列, 自动重试, useOfflineState
 */

// Mock @/utils/request
const mockService = vi.fn()
vi.mock('@/utils/request', () => ({
  default: (...args: unknown[]) => mockService(...args),
}))

describe('useNetworkStatus', () => {
  let addEventListenerSpy: ReturnType<typeof vi.spyOn>
  let removeEventListenerSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.resetModules()

    // Spy on window event listeners
    addEventListenerSpy = vi.spyOn(window, 'addEventListener')
    removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')

    // Default navigator.onLine
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      writable: true,
      configurable: true,
    })
  })

  afterEach(() => {
    addEventListenerSpy.mockRestore()
    removeEventListenerSpy.mockRestore()
  })

  // ---------- online/offline 检测 ----------

  describe('online/offline 检测', () => {
    it('注册 online 和 offline 事件监听器', async () => {
      // Need to mock onMounted/onUnmounted to actually call the callbacks
      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: (fn: () => void) => fn(),
        }
      })

      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      const { isOnline } = useNetworkStatus()

      expect(addEventListenerSpy).toHaveBeenCalledWith('online', expect.any(Function))
      expect(addEventListenerSpy).toHaveBeenCalledWith('offline', expect.any(Function))
      expect(isOnline.value).toBe(true)

      vi.doUnmock('vue')
    })

    it('offline 事件将 isOnline 设为 false', async () => {
      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      const { isOnline } = useNetworkStatus()

      // Simulate offline event
      const offlineHandler = addEventListenerSpy.mock.calls.find(
        (call) => call[0] === 'offline',
      )?.[1] as EventListener

      if (offlineHandler) {
        offlineHandler(new Event('offline'))
        expect(isOnline.value).toBe(false)
      }

      vi.doUnmock('vue')
    })

    it('online 事件将 isOnline 设为 true', async () => {
      Object.defineProperty(navigator, 'onLine', { value: false, writable: true, configurable: true })

      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      const { isOnline } = useNetworkStatus()

      // Simulate online event
      const onlineHandler = addEventListenerSpy.mock.calls.find(
        (call) => call[0] === 'online',
      )?.[1] as EventListener

      if (onlineHandler) {
        onlineHandler(new Event('online'))
        expect(isOnline.value).toBe(true)
      }

      vi.doUnmock('vue')
    })

    it('组件卸载时移除事件监听器', async () => {
      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: (fn: () => void) => fn(),
        }
      })

      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      useNetworkStatus()

      expect(removeEventListenerSpy).toHaveBeenCalledWith('online', expect.any(Function))
      expect(removeEventListenerSpy).toHaveBeenCalledWith('offline', expect.any(Function))

      vi.doUnmock('vue')
    })
  })

  // ---------- 请求队列 ----------

  describe('请求队列', () => {
    it('queueRequest 将请求加入队列', async () => {
      const { queueRequest, getQueuedRequestCount } = await import('@/composables/useNetworkStatus')
      expect(getQueuedRequestCount()).toBe(0)

      queueRequest({ url: '/api/test', method: 'GET' })
      expect(getQueuedRequestCount()).toBe(1)

      queueRequest({ url: '/api/test2', method: 'POST' })
      expect(getQueuedRequestCount()).toBe(2)
    })

    it('queuedRequestCount 返回队列长度', async () => {
      const { queueRequest } = await import('@/composables/useNetworkStatus')
      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      const { queuedRequestCount } = useNetworkStatus()

      expect(queuedRequestCount()).toBe(0)

      queueRequest({ url: '/api/a' })
      queueRequest({ url: '/api/b' })
      queueRequest({ url: '/api/c' })

      expect(queuedRequestCount()).toBe(3)
    })
  })

  // ---------- 自动重试 ----------

  describe('自动重试', () => {
    it('恢复在线时重试队列中的请求', async () => {
      mockService.mockResolvedValue({ data: 'ok' })

      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { queueRequest, getQueuedRequestCount } = await import('@/composables/useNetworkStatus')
      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      useNetworkStatus()

      // Queue a request
      const promise = queueRequest({ url: '/api/retry-test', method: 'GET' })
      expect(getQueuedRequestCount()).toBe(1)

      // Simulate coming back online
      const onlineHandler = addEventListenerSpy.mock.calls.find(
        (call) => call[0] === 'online',
      )?.[1] as EventListener

      if (onlineHandler) {
        onlineHandler(new Event('online'))
        // Wait for async retry
        await vi.waitFor(() => {
          expect(getQueuedRequestCount()).toBe(0)
        })
        expect(mockService).toHaveBeenCalledWith({ url: '/api/retry-test', method: 'GET' })
      }

      // The queued promise should resolve
      await expect(promise).resolves.toBeDefined()

      vi.doUnmock('vue')
    })

    it('重试失败的请求时 reject 对应的 promise', async () => {
      mockService.mockRejectedValue(new Error('Server Error'))

      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { queueRequest, getQueuedRequestCount } = await import('@/composables/useNetworkStatus')
      const { useNetworkStatus } = await import('@/composables/useNetworkStatus')
      useNetworkStatus()

      const promise = queueRequest({ url: '/api/fail', method: 'GET' })

      const onlineHandler = addEventListenerSpy.mock.calls.find(
        (call) => call[0] === 'online',
      )?.[1] as EventListener

      if (onlineHandler) {
        onlineHandler(new Event('online'))
        await vi.waitFor(() => {
          expect(getQueuedRequestCount()).toBe(0)
        })
      }

      await expect(promise).rejects.toThrow('Server Error')

      vi.doUnmock('vue')
    })
  })

  // ---------- useOfflineState ----------

  describe('useOfflineState', () => {
    it('初始状态根据 navigator.onLine 设置', async () => {
      Object.defineProperty(navigator, 'onLine', { value: false, writable: true, configurable: true })

      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { useOfflineState } = await import('@/composables/useNetworkStatus')
      const { isOffline } = useOfflineState()

      expect(isOffline.value).toBe(true)

      vi.doUnmock('vue')
    })

    it('online 事件更新 isOffline 为 false', async () => {
      Object.defineProperty(navigator, 'onLine', { value: false, writable: true, configurable: true })

      vi.doMock('vue', async (importOriginal) => {
        const actual = await importOriginal<typeof import('vue')>()
        return {
          ...actual,
          onMounted: (fn: () => void) => fn(),
          onUnmounted: () => {},
        }
      })

      const { useOfflineState } = await import('@/composables/useNetworkStatus')
      const { isOffline } = useOfflineState()

      const onlineHandler = addEventListenerSpy.mock.calls.find(
        (call) => call[0] === 'online',
      )?.[1] as EventListener

      if (onlineHandler) {
        Object.defineProperty(navigator, 'onLine', { value: true, writable: true, configurable: true })
        onlineHandler(new Event('online'))
        expect(isOffline.value).toBe(false)
      }

      vi.doUnmock('vue')
    })
  })
})
