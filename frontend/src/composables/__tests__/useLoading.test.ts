import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * useLoading composable 单元测试
 * 测试: setLoading, withLoading, isLoading, loading, 多 key 支持, 错误处理
 */

describe('useLoading', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  async function getUseLoading() {
    const { useLoading } = await import('@/composables/useLoading')
    return useLoading()
  }

  // ---------- 基础 loading 状态 ----------

  describe('loading 默认状态', () => {
    it('默认 loading 为 false', async () => {
      const { loading } = await getUseLoading()
      expect(loading.value).toBe(false)
    })
  })

  // ---------- setLoading ----------

  describe('setLoading', () => {
    it('setLoading(true) 将 loading 设为 true', async () => {
      const { loading, setLoading } = await getUseLoading()
      setLoading(undefined, true)
      expect(loading.value).toBe(true)
    })

    it('setLoading(false) 将 loading 设为 false', async () => {
      const { loading, setLoading } = await getUseLoading()
      setLoading(undefined, true)
      setLoading(undefined, false)
      expect(loading.value).toBe(false)
    })

    it('setLoading(key, value) 设置指定 key 的状态', async () => {
      const { isLoading, setLoading } = await getUseLoading()
      setLoading('fetchData', true)
      expect(isLoading('fetchData')).toBe(true)
      expect(isLoading('otherKey')).toBe(false)
    })
  })

  // ---------- isLoading ----------

  describe('isLoading', () => {
    it('未设置的 key 返回 false', async () => {
      const { isLoading } = await getUseLoading()
      expect(isLoading('nonexistent')).toBe(false)
    })

    it('已设置 true 的 key 返回 true', async () => {
      const { isLoading, setLoading } = await getUseLoading()
      setLoading('submit', true)
      expect(isLoading('submit')).toBe(true)
    })

    it('已设置 false 的 key 返回 false', async () => {
      const { isLoading, setLoading } = await getUseLoading()
      setLoading('submit', true)
      setLoading('submit', false)
      expect(isLoading('submit')).toBe(false)
    })
  })

  // ---------- withLoading ----------

  describe('withLoading', () => {
    it('无 key 调用时自动管理 loading 状态', async () => {
      const { loading, withLoading } = await getUseLoading()
      const mockFn = vi.fn().mockResolvedValue('result')

      const result = await withLoading(mockFn)

      expect(result).toBe('result')
      expect(mockFn).toHaveBeenCalledOnce()
      expect(loading.value).toBe(false)
    })

    it('执行期间 loading 为 true', async () => {
      const { loading, withLoading } = await getUseLoading()
      let loadingDuringExecution = false

      const mockFn = vi.fn().mockImplementation(async () => {
        loadingDuringExecution = loading.value
        return 'done'
      })

      await withLoading(mockFn)
      expect(loadingDuringExecution).toBe(true)
    })

    it('带 key 调用时管理指定 key 的状态', async () => {
      const { isLoading, withLoading } = await getUseLoading()
      const mockFn = vi.fn().mockResolvedValue('ok')

      await withLoading('fetchData', mockFn)

      expect(mockFn).toHaveBeenCalledOnce()
      expect(isLoading('fetchData')).toBe(false)
    })

    it('执行期间指定 key 的 loading 为 true', async () => {
      const { isLoading, withLoading } = await getUseLoading()
      let loadingDuringExecution = false

      const mockFn = vi.fn().mockImplementation(async () => {
        loadingDuringExecution = isLoading('saveData')
        return 'saved'
      })

      await withLoading('saveData', mockFn)
      expect(loadingDuringExecution).toBe(true)
    })

    it('函数抛出错误时自动重置 loading 并重新抛出', async () => {
      const { loading, withLoading } = await getUseLoading()
      const error = new Error('test error')
      const mockFn = vi.fn().mockRejectedValue(error)

      await expect(withLoading(mockFn)).rejects.toThrow('useLoading: withLoading error')
      expect(loading.value).toBe(false)
    })

    it('带 key 的函数抛出错误时重置对应 key', async () => {
      const { isLoading, withLoading } = await getUseLoading()
      const mockFn = vi.fn().mockRejectedValue(new Error('fail'))

      await expect(withLoading('riskyOp', mockFn)).rejects.toThrow('useLoading: withLoading error')
      expect(isLoading('riskyOp')).toBe(false)
    })
  })

  // ---------- 多 key 并发 ----------

  describe('多 key 并发支持', () => {
    it('多个 key 可以同时处于 loading 状态', async () => {
      const { isLoading, setLoading } = await getUseLoading()
      setLoading('key1', true)
      setLoading('key2', true)

      expect(isLoading('key1')).toBe(true)
      expect(isLoading('key2')).toBe(true)
    })

    it('一个 key 的状态不影响另一个 key', async () => {
      const { isLoading, setLoading } = await getUseLoading()
      setLoading('key1', true)
      setLoading('key2', false)

      expect(isLoading('key1')).toBe(true)
      expect(isLoading('key2')).toBe(false)
    })

    it('多个 withLoading 并发执行各自管理 key', async () => {
      const { isLoading, withLoading } = await getUseLoading()

      const fn1 = vi.fn().mockImplementation(async () => {
        await new Promise((r) => setTimeout(r, 10))
        return 'r1'
      })
      const fn2 = vi.fn().mockImplementation(async () => {
        await new Promise((r) => setTimeout(r, 5))
        return 'r2'
      })

      const [r1, r2] = await Promise.all([
        withLoading('a', fn1),
        withLoading('b', fn2),
      ])

      expect(r1).toBe('r1')
      expect(r2).toBe('r2')
      expect(isLoading('a')).toBe(false)
      expect(isLoading('b')).toBe(false)
    })
  })
})
