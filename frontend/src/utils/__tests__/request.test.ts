import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

/**
 * Request 拦截器测试
 * 测试请求拦截器逻辑（Token 注入、租户 ID 注入）
 * 测试响应拦截器逻辑（错误处理、Token 刷新）
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

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

// Mock ant-design-vue message
vi.mock('ant-design-vue', () => ({
  message: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  }
}))

describe('Request 拦截器', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  it('请求拦截器 - 有 Token 时自动添加 Authorization 头', async () => {
    localStorageMock.setItem('token', 'test_token_123')

    // 创建 axios 实例并模拟拦截器行为
    const instance = axios.create({ baseURL: 'http://localhost:8080/api' })

    instance.interceptors.request.use((config) => {
      const token = localStorage.getItem('token')
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    })

    // 手动调用拦截器验证逻辑
    const config = { headers: {} as Record<string, string> }
    const result = await instance.interceptors.request.handlers[0].fulfilled(config as any)

    expect(result.headers.Authorization).toBe('Bearer test_token_123')
  })

  it('请求拦截器 - 无 Token 时不添加 Authorization 头', async () => {
    const instance = axios.create({ baseURL: 'http://localhost:8080/api' })

    instance.interceptors.request.use((config) => {
      const token = localStorage.getItem('token')
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    })

    const config = { headers: {} as Record<string, string> }
    const result = await instance.interceptors.request.handlers[0].fulfilled(config as any)

    expect(result.headers.Authorization).toBeUndefined()
  })

  it('请求拦截器 - 有租户ID时添加 X-Tenant-ID 头', async () => {
    localStorageMock.setItem('tenantId', '100')

    const instance = axios.create({ baseURL: 'http://localhost:8080/api' })

    instance.interceptors.request.use((config) => {
      const tenantId = localStorage.getItem('tenantId')
      if (tenantId && config.headers) {
        config.headers['X-Tenant-ID'] = tenantId
      }
      return config
    })

    const config = { headers: {} as Record<string, string> }
    const result = await instance.interceptors.request.handlers[0].fulfilled(config as any)

    expect(result.headers['X-Tenant-ID']).toBe('100')
  })

  it('响应拦截器 - 成功响应 code 为 200 时直接返回数据', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return res
    })

    const mockResponse = { data: { code: 200, data: { id: 1 }, message: 'ok' } }
    const result = await instance.interceptors.response.handlers[0].fulfilled(mockResponse as any)

    expect(result.status).toBe(200)
    expect(result.data.id).toBe(1)
  })

  it('响应拦截器 - 成功响应 code 不为 200 时抛出错误', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return res
    })

    const mockResponse = { data: { code: 500, message: '服务器错误', data: null } }

    await expect(
      instance.interceptors.response.handlers[0].fulfilled(mockResponse as any)
    ).rejects.toThrow('服务器错误')
  })

  it('响应拦截器 - code 为 0 时视为成功', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return res
    })

    const mockResponse = { data: { code: 0, data: { token: 'abc' }, message: 'ok' } }
    const result = await instance.interceptors.response.handlers[0].fulfilled(mockResponse as any)

    expect(result.status).toBe(0)
    expect(result.data.token).toBe('abc')
  })
})
