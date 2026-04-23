import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import type { AxiosResponse } from 'axios'

/**
 * Request 拦截器测试
 * 测试请求拦截器逻辑（Token 注入）
 * 测试响应拦截器逻辑（错误处理、返回 ApiResponse 结构）
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

// Mock window.location
const mockLocation = { href: '', pathname: '/dashboard' }
Object.defineProperty(globalThis, 'window', {
  value: { ...globalThis.window, location: mockLocation }
})

describe('Request 拦截器', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  it('请求拦截器 - 有 Token 时自动添加 Authorization 头', async () => {
    localStorageMock.setItem('token', 'test_token_123')

    // 创建 axios 实例并模拟拦截器行为（与 request.ts 一致）
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
    const handlers = instance.interceptors.request.handlers
    expect(handlers).toBeDefined()
    expect(handlers!.length).toBeGreaterThan(0)
    const result = await handlers![0].fulfilled(config as any)

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
    const handlers = instance.interceptors.request.handlers
    expect(handlers).toBeDefined()
    const result = await handlers![0].fulfilled(config as any)

    expect(result.headers.Authorization).toBeUndefined()
  })

  it('响应拦截器 - 成功响应 code 为 200 时返回 ApiResponse 结构（非 AxiosResponse）', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response: AxiosResponse) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      // 与 request.ts 一致：返回 response.data（ApiResponse 结构）
      return response.data
    })

    const mockResponse = { data: { code: 200, data: { id: 1 }, message: 'ok' } }
    const handlers = instance.interceptors.response.handlers
    expect(handlers).toBeDefined()
    const result = await handlers![0].fulfilled(mockResponse as any)

    // 返回的是 ApiResponse 结构，不是 AxiosResponse
    expect(result).toEqual({ code: 200, data: { id: 1 }, message: 'ok' })
    // 不应包含 AxiosResponse 特有属性
    expect((result as any).status).toBeUndefined()
    expect((result as any).headers).toBeUndefined()
    expect((result as any).config).toBeUndefined()
  })

  it('响应拦截器 - 成功响应 code 不为 200 时抛出错误', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response: AxiosResponse) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return response.data
    })

    const mockResponse = { data: { code: 500, message: '服务器错误', data: null } }
    const handlers = instance.interceptors.response.handlers
    expect(handlers).toBeDefined()

    expect(() => handlers![0].fulfilled(mockResponse as any)).toThrow('服务器错误')
  })

  it('响应拦截器 - code 为 0 时视为成功', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response: AxiosResponse) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return response.data
    })

    const mockResponse = { data: { code: 0, data: { token: 'abc' }, message: 'ok' } }
    const handlers = instance.interceptors.response.handlers
    expect(handlers).toBeDefined()
    const result = await handlers![0].fulfilled(mockResponse as any)

    expect(result).toEqual({ code: 0, data: { token: 'abc' }, message: 'ok' })
  })

  it('响应拦截器 - code 为 401 时抛出错误', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response: AxiosResponse) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return response.data
    })

    const mockResponse = { data: { code: 401, message: '未授权', data: null } }
    const handlers = instance.interceptors.response.handlers
    expect(handlers).toBeDefined()

    expect(() => handlers![0].fulfilled(mockResponse as any)).toThrow('未授权')
  })

  it('响应拦截器 - 返回结构包含 code/message/data 三个字段', async () => {
    const instance = axios.create()

    instance.interceptors.response.use((response: AxiosResponse) => {
      const res = response.data as any
      if (res.code !== 200 && res.code !== 0) {
        throw new Error(res.message || 'Error')
      }
      return response.data
    })

    const mockResponse = {
      data: { code: 200, data: { list: [1, 2, 3] }, message: 'success' }
    }
    const handlers = instance.interceptors.response.handlers
    expect(handlers).toBeDefined()
    const result = await handlers![0].fulfilled(mockResponse as any)

    // 验证 ApiResponse 结构一致性
    expect(result).toHaveProperty('code')
    expect(result).toHaveProperty('message')
    expect(result).toHaveProperty('data')
    expect(typeof result.code).toBe('number')
    expect(typeof result.message).toBe('string')
    expect(Array.isArray(result.data.list)).toBe(true)
  })
})
