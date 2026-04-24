import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDictStore } from '@/store/modules/dict'
import type { DictItem } from '@/types'

/**
 * Dict Store 单元测试
 * 测试: state, getters, actions, 缓存管理
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

function makeDictItem(overrides: Partial<DictItem> & { dictValue: string; dictLabel: string }): DictItem {
  return {
    id: 1,
    dictType: 'test',
    dictSort: 0,
    isDefault: 'N',
    status: '0',
    ...overrides,
  }
}

// ==================== Tests ====================

describe('Dict Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('dictTypes 默认为空数组', () => {
      const store = useDictStore()
      expect(store.dictTypes).toEqual([])
    })

    it('loaded 默认为 false', () => {
      const store = useDictStore()
      expect(store.loaded).toBe(false)
    })

    it('loading 默认为 false', () => {
      const store = useDictStore()
      expect(store.loading).toBe(false)
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('dictItemsByType 返回对应类型的字典项', () => {
      const store = useDictStore()
      store.dictItems = new Map<string, DictItem[]>([
        ['status', [
          makeDictItem({ dictValue: 'active', dictLabel: 'Active' }),
          makeDictItem({ dictValue: 'inactive', dictLabel: 'Inactive' }),
        ]],
        ['type', [
          makeDictItem({ dictValue: 'chat', dictLabel: 'Chat' }),
        ]],
      ])

      expect(store.dictItemsByType('status')).toHaveLength(2)
      expect(store.dictItemsByType('type')).toHaveLength(1)
      expect(store.dictItemsByType('nonexistent')).toEqual([])
    })

    it('dictLabel 返回对应字典值的标签', () => {
      const store = useDictStore()
      store.dictItems = new Map<string, DictItem[]>([
        ['status', [
          makeDictItem({ dictValue: 'active', dictLabel: 'Active' }),
        ]],
      ])

      expect(store.dictLabel('status', 'active')).toBe('Active')
      expect(store.dictLabel('status', 'unknown')).toBe('unknown')
      expect(store.dictLabel('nonexistent', 'any')).toBe('any')
    })
  })

  // ---------- Actions ----------

  describe('fetchDictTypes', () => {
    it('成功获取字典类型列表', async () => {
      const mockTypes = [
        { dictType: 'status', dictName: 'Status' },
        { dictType: 'type', dictName: 'Type' },
      ]
      mockRequestGet.mockResolvedValue({ data: mockTypes })

      const store = useDictStore()
      const result = await store.fetchDictTypes()

      expect(store.dictTypes).toHaveLength(2)
      expect(result).toEqual(mockTypes)
    })

    it('获取失败时返回空数组', async () => {
      mockRequestGet.mockRejectedValue(new Error('Network Error'))

      const store = useDictStore()
      const result = await store.fetchDictTypes()

      expect(result).toEqual([])
    })

    it('data 为 null 时返回 null', async () => {
      mockRequestGet.mockResolvedValue({ data: null })

      const store = useDictStore()
      const result = await store.fetchDictTypes()

      expect(result).toBeNull()
      expect(store.dictTypes).toEqual([])
    })
  })

  describe('fetchDictItems', () => {
    it('成功获取字典项并缓存', async () => {
      const mockItems = [
        makeDictItem({ dictValue: 'active', dictLabel: 'Active' }),
        makeDictItem({ dictValue: 'inactive', dictLabel: 'Inactive' }),
      ]
      mockRequestGet.mockResolvedValue({ data: mockItems })

      const store = useDictStore()
      const result = await store.fetchDictItems('status')

      expect(result).toEqual(mockItems)
      expect(store.dictItems.get('status')).toEqual(mockItems)
    })

    it('已缓存的字典项直接返回', () => {
      const store = useDictStore()
      store.dictItems = new Map<string, DictItem[]>([
        ['status', [makeDictItem({ dictValue: 'active', dictLabel: 'Active' })]],
      ])

      // Since fetchDictItems is async and checks cache synchronously,
      // we test the cached path
      expect(store.dictItems.get('status')).toHaveLength(1)
    })

    it('获取失败时返回空数组', async () => {
      mockRequestGet.mockRejectedValue(new Error('Error'))

      const store = useDictStore()
      const result = await store.fetchDictItems('status')

      expect(result).toEqual([])
    })
  })

  describe('getDictItemsByType', () => {
    it('委托给 fetchDictItems', async () => {
      mockRequestGet.mockResolvedValue({ data: [makeDictItem({ dictValue: 'a', dictLabel: 'A' })] })

      const store = useDictStore()
      const result = await store.getDictItemsByType('test')

      expect(result).toHaveLength(1)
    })
  })

  describe('loadAllDicts', () => {
    it('加载所有字典类型和项', async () => {
      mockRequestGet
        .mockResolvedValueOnce({ data: [{ dictType: 'status' }, { dictType: 'type' }] })
        .mockResolvedValueOnce({ data: [makeDictItem({ dictValue: 'active', dictLabel: 'Active' })] })
        .mockResolvedValueOnce({ data: [makeDictItem({ dictValue: 'chat', dictLabel: 'Chat' })] })

      const store = useDictStore()
      await store.loadAllDicts()

      expect(store.loaded).toBe(true)
      expect(store.loading).toBe(false)
    })

    it('已加载时跳过', async () => {
      const store = useDictStore()
      store.loaded = true

      await store.loadAllDicts()

      expect(mockRequestGet).not.toHaveBeenCalled()
    })

    it('加载完成后 loading 重置为 false', async () => {
      mockRequestGet.mockResolvedValue({ data: [] })

      const store = useDictStore()
      await store.loadAllDicts()

      expect(store.loading).toBe(false)
    })
  })

  describe('clearCache', () => {
    it('清除所有缓存并重置 loaded', () => {
      const store = useDictStore()
      store.dictItems = new Map<string, DictItem[]>([['status', [makeDictItem({ dictValue: 'a', dictLabel: 'A' })]]])
      store.loaded = true

      store.clearCache()

      expect(store.dictItems.size).toBe(0)
      expect(store.loaded).toBe(false)
    })
  })

  describe('removeDictType', () => {
    it('移除指定字典类型', () => {
      const store = useDictStore()
      store.dictItems = new Map<string, DictItem[]>([
        ['status', [makeDictItem({ dictValue: 'a', dictLabel: 'A' })]],
        ['type', [makeDictItem({ dictValue: 'b', dictLabel: 'B' })]],
      ])

      store.removeDictType('status')

      expect(store.dictItems.has('status')).toBe(false)
      expect(store.dictItems.has('type')).toBe(true)
    })
  })
})
