import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * Permission Store 单元测试
 * 测试: state, getters, actions, buildMenuTree
 */

// ==================== Mocks ====================

const mockRequestGet = vi.fn()

vi.mock('@/utils/request', () => ({
  default: {
    get: (...args: unknown[]) => mockRequestGet(...args),
  },
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  },
}))

// ==================== Tests ====================

describe('Permission Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('permissions 默认为空数组', () => {
      const store = usePermissionStore()
      expect(store.permissions).toEqual([])
    })

    it('roles 默认为空数组', () => {
      const store = usePermissionStore()
      expect(store.roles).toEqual([])
    })

    it('currentRole 默认为 null', () => {
      const store = usePermissionStore()
      expect(store.currentRole).toBeNull()
    })

    it('menuTree 默认为空数组', () => {
      const store = usePermissionStore()
      expect(store.menuTree).toEqual([])
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('hasPermission - 检查单个权限码', () => {
      const store = usePermissionStore()
      store.permissions = [
        { id: 1, name: 'Agent管理', code: 'agent:view', type: 'menu' },
        { id: 2, name: 'Agent编辑', code: 'agent:edit', type: 'button' },
      ]

      expect(store.hasPermission('agent:view')).toBe(true)
      expect(store.hasPermission('agent:delete')).toBe(false)
    })

    it('hasPermission - 检查权限码数组（任一匹配）', () => {
      const store = usePermissionStore()
      store.permissions = [
        { id: 1, name: 'Agent管理', code: 'agent:view', type: 'menu' },
      ]

      expect(store.hasPermission(['agent:view', 'workflow:view'])).toBe(true)
      expect(store.hasPermission(['workflow:view', 'workflow:edit'])).toBe(false)
    })

    it('hasPermission - 支持嵌套权限', () => {
      const store = usePermissionStore()
      store.permissions = [
        {
          id: 1, name: '系统管理', code: 'system', type: 'menu',
          children: [
            { id: 2, name: '用户管理', code: 'system:user', type: 'menu' },
            { id: 3, name: '用户编辑', code: 'system:user:edit', type: 'button' },
          ],
        },
      ]

      expect(store.hasPermission('system:user')).toBe(true)
      expect(store.hasPermission('system:user:edit')).toBe(true)
    })

    it('menuItems 返回 menuTree', () => {
      const store = usePermissionStore()
      store.menuTree = [
        { key: 'dashboard', title: 'Dashboard' },
      ]

      expect(store.menuItems).toEqual(store.menuTree)
    })
  })

  // ---------- Actions ----------

  describe('fetchPermissions', () => {
    it('成功获取权限并构建菜单树', async () => {
      const mockPerms = [
        { id: 1, name: 'Dashboard', code: 'dashboard', type: 'menu', sort: 1 },
        { id: 2, name: 'Agent', code: 'agent', type: 'menu', sort: 2 },
        { id: 3, name: 'Agent View', code: 'agent:view', type: 'menu', parentId: 2, sort: 1 },
      ]
      mockRequestGet.mockResolvedValue({ data: mockPerms })

      const store = usePermissionStore()
      await store.fetchPermissions()

      expect(store.permissions).toHaveLength(3)
      expect(store.menuTree.length).toBeGreaterThan(0)
    })

    it('获取失败时 permissions 保持不变', async () => {
      mockRequestGet.mockRejectedValue(new Error('Network Error'))

      const store = usePermissionStore()
      store.permissions = [{ id: 1, name: 'Test', code: 'test', type: 'menu' }]

      await store.fetchPermissions()

      expect(store.permissions).toHaveLength(1)
    })

    it('data 为 null 时 permissions 设为空数组', async () => {
      mockRequestGet.mockResolvedValue({ data: null })

      const store = usePermissionStore()
      await store.fetchPermissions()

      expect(store.permissions).toEqual([])
    })
  })

  describe('fetchRoles', () => {
    it('成功获取角色列表', async () => {
      const mockRoles = [
        { id: 1, name: 'Admin', code: 'admin' },
        { id: 2, name: 'User', code: 'user' },
      ]
      mockRequestGet.mockResolvedValue({ data: mockRoles })

      const store = usePermissionStore()
      await store.fetchRoles()

      expect(store.roles).toHaveLength(2)
    })

    it('获取失败时 roles 保持不变', async () => {
      mockRequestGet.mockRejectedValue(new Error('Error'))

      const store = usePermissionStore()
      await store.fetchRoles()

      expect(store.roles).toEqual([])
    })
  })

  describe('checkPermission', () => {
    it('有权限时返回 true', async () => {
      mockRequestGet.mockResolvedValue({ data: true })

      const store = usePermissionStore()
      const result = await store.checkPermission('agent:view')

      expect(result).toBe(true)
    })

    it('无权限时返回 false', async () => {
      mockRequestGet.mockResolvedValue({ data: false })

      const store = usePermissionStore()
      const result = await store.checkPermission('admin:panel')

      expect(result).toBe(false)
    })

    it('请求失败时返回 false', async () => {
      mockRequestGet.mockRejectedValue(new Error('Error'))

      const store = usePermissionStore()
      const result = await store.checkPermission('any')

      expect(result).toBe(false)
    })
  })
})
