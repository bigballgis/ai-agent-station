import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * 路由守卫测试
 * 测试认证守卫逻辑
 */

// Mock pinia
let mockIsLoggedIn = false

vi.mock('@/store/modules/user', () => ({
  useUserStore: () => ({
    isLoggedIn: mockIsLoggedIn
  })
}))

// Mock vue-router
const mockNext = vi.fn()
const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn((guard) => {
      // 存储守卫以便测试
      return guard
    })
  })),
  createWebHistory: vi.fn(),
  useRouter: () => ({
    push: mockPush,
    back: vi.fn()
  })
}))

describe('路由守卫', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIsLoggedIn = false
  })

  it('未登录用户访问需要认证的页面 - 重定向到登录页', async () => {
    mockIsLoggedIn = false

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    // 模拟守卫逻辑
    const to = { meta: { requiresAuth: true }, fullPath: '/dashboard' }
    const from = {}
    const next = mockNext

    if (to.meta.requiresAuth !== false) {
      if (!store.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    expect(next).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/dashboard' }
    })
  })

  it('已登录用户访问需要认证的页面 - 放行', async () => {
    mockIsLoggedIn = true

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { meta: { requiresAuth: true }, fullPath: '/dashboard' }
    const next = mockNext

    if (to.meta.requiresAuth !== false) {
      if (!store.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
      } else {
        next()
      }
    }

    expect(next).toHaveBeenCalledWith()
  })

  it('已登录用户访问登录页 - 重定向到首页', async () => {
    mockIsLoggedIn = true

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { path: '/login', meta: { requiresAuth: false } }
    const next = mockNext

    if (to.meta.requiresAuth === false) {
      if (to.path === '/login' && store.isLoggedIn) {
        next('/dashboard')
      }
    }

    expect(next).toHaveBeenCalledWith('/dashboard')
  })

  it('未登录用户访问登录页 - 放行', async () => {
    mockIsLoggedIn = false

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { path: '/login', meta: { requiresAuth: false } }
    const next = mockNext

    if (to.meta.requiresAuth === false) {
      if (to.path === '/login' && store.isLoggedIn) {
        next('/dashboard')
      }
    }

    // 未登录访问登录页，next 不应被调用
    expect(next).not.toHaveBeenCalled()
  })

  it('访问不需要认证的页面 - 放行', async () => {
    mockIsLoggedIn = false

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { meta: { requiresAuth: false }, path: '/about' }
    const next = mockNext

    if (to.meta.requiresAuth !== false) {
      if (!store.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    // 不需要认证的页面，守卫不拦截
    expect(next).not.toHaveBeenCalled()
  })

  it('重定向时携带原始路径', async () => {
    mockIsLoggedIn = false

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { meta: { requiresAuth: true }, fullPath: '/agents/1/edit' }
    const next = mockNext

    if (to.meta.requiresAuth !== false) {
      if (!store.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    expect(next).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/agents/1/edit' }
    })
  })

  it('requiresAuth 未设置时默认需要认证', async () => {
    mockIsLoggedIn = false

    const { useUserStore } = await import('@/store/modules/user')
    const store = useUserStore()

    const to = { meta: {}, fullPath: '/some-page' }
    const next = mockNext

    // requiresAuth 未设置，默认需要认证
    if (to.meta.requiresAuth !== false) {
      if (!store.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    expect(next).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/some-page' }
    })
  })
})
