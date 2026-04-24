import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAgentStore } from '@/store/modules/agent'

/**
 * Agent Store 单元测试
 * 测试: state, getters, actions, 错误处理
 */

// ==================== Mocks ====================

const mockGetAllAgents = vi.fn()
const mockGetAgentById = vi.fn()
const mockCreateAgent = vi.fn()
const mockUpdateAgent = vi.fn()
const mockDeleteAgent = vi.fn()

vi.mock('@/api/agent', () => ({
  agentApi: {
    getAllAgents: () => mockGetAllAgents(),
    getAgentById: (id: string | number) => mockGetAgentById(id),
    createAgent: (data: Record<string, unknown>) => mockCreateAgent(data),
    updateAgent: (id: string | number, data: Record<string, unknown>) => mockUpdateAgent(id, data),
    deleteAgent: (id: string | number) => mockDeleteAgent(id),
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

describe('Agent Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('agents 默认为空数组', () => {
      const store = useAgentStore()
      expect(store.agents).toEqual([])
    })

    it('currentAgent 默认为 null', () => {
      const store = useAgentStore()
      expect(store.currentAgent).toBeNull()
    })

    it('loading 默认为 false', () => {
      const store = useAgentStore()
      expect(store.loading).toBe(false)
    })

    it('filters 默认为空对象', () => {
      const store = useAgentStore()
      expect(store.filters).toEqual({})
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('activeAgents 过滤出活跃的 agent', () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Active', isActive: true, status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'Inactive', isActive: false, status: 'draft', type: 'chat', description: '' },
        { id: 3, name: 'No flag', status: 'published', type: 'chat', description: '' },
      ] as unknown as typeof store.agents

      expect(store.activeAgents).toHaveLength(2)
      expect(store.activeAgents.map((a) => a.name)).toEqual(['Active', 'No flag'])
    })

    it('agentById 根据 id 查找 agent', () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Agent 1', status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'Agent 2', status: 'draft', type: 'chat', description: '' },
      ] as unknown as typeof store.agents

      expect(store.agentById(1)?.name).toBe('Agent 1')
      expect(store.agentById(2)?.name).toBe('Agent 2')
      expect(store.agentById(999)).toBeUndefined()
    })

    it('filteredAgents 按关键字过滤', () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Chat Bot', description: 'A chatbot', status: 'published', type: 'chat' },
        { id: 2, name: 'Search Agent', description: 'Search tool', status: 'draft', type: 'search' },
      ] as unknown as typeof store.agents

      store.setFilters({ keyword: 'chat' })
      expect(store.filteredAgents).toHaveLength(1)
      expect(store.filteredAgents[0].name).toBe('Chat Bot')
    })

    it('filteredAgents 按状态过滤', () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'A1', status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'A2', status: 'draft', type: 'chat', description: '' },
      ] as unknown as typeof store.agents

      store.setFilters({ status: 'draft' })
      expect(store.filteredAgents).toHaveLength(1)
    })

    it('filteredAgents 按类型过滤', () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'A1', status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'A2', status: 'draft', type: 'search', description: '' },
      ] as unknown as typeof store.agents

      store.setFilters({ type: 'search' })
      expect(store.filteredAgents).toHaveLength(1)
    })
  })

  // ---------- Actions ----------

  describe('fetchAgents', () => {
    it('成功获取 agent 列表（数组格式）', async () => {
      const mockAgents = [
        { id: 1, name: 'Agent 1', status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'Agent 2', status: 'draft', type: 'chat', description: '' },
      ]
      mockGetAllAgents.mockResolvedValue({ data: mockAgents })

      const store = useAgentStore()
      await store.fetchAgents()

      expect(store.agents).toHaveLength(2)
      expect(store.loading).toBe(false)
    })

    it('成功获取 agent 列表（分页格式）', async () => {
      mockGetAllAgents.mockResolvedValue({
        data: {
          records: [{ id: 1, name: 'Agent', status: 'published', type: 'chat', description: '' }],
          total: 1,
        },
      })

      const store = useAgentStore()
      await store.fetchAgents()

      expect(store.agents).toHaveLength(1)
    })

    it('获取失败时 loading 重置为 false', async () => {
      mockGetAllAgents.mockRejectedValue(new Error('Network Error'))

      const store = useAgentStore()
      await expect(store.fetchAgents()).rejects.toThrow('Network Error')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchAgentById', () => {
    it('成功获取单个 agent', async () => {
      const mockAgent = { id: 1, name: 'Agent', status: 'published', type: 'chat', description: '' }
      mockGetAgentById.mockResolvedValue({ data: mockAgent })

      const store = useAgentStore()
      await store.fetchAgentById(1)

      expect(store.currentAgent).toEqual(mockAgent)
      expect(store.loading).toBe(false)
    })

    it('获取失败时 loading 重置为 false', async () => {
      mockGetAgentById.mockRejectedValue(new Error('Not found'))

      const store = useAgentStore()
      await expect(store.fetchAgentById(999)).rejects.toThrow('Not found')
      expect(store.loading).toBe(false)
    })
  })

  describe('createAgent', () => {
    it('成功创建 agent 并添加到列表', async () => {
      const newAgent = { id: 3, name: 'New Agent', status: 'draft', type: 'chat', description: '' }
      mockCreateAgent.mockResolvedValue({ data: newAgent })

      const store = useAgentStore()
      await store.createAgent({ name: 'New Agent' })

      expect(store.agents).toHaveLength(1)
      expect(store.agents[0].name).toBe('New Agent')
    })

    it('创建失败时抛出错误', async () => {
      mockCreateAgent.mockRejectedValue(new Error('Create failed'))

      const store = useAgentStore()
      await expect(store.createAgent({ name: 'Fail' })).rejects.toThrow('Create failed')
      expect(store.agents).toHaveLength(0)
    })
  })

  describe('updateAgent', () => {
    it('成功更新 agent', async () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Old Name', status: 'published', type: 'chat', description: '' },
      ] as unknown as typeof store.agents

      const updatedAgent = { id: 1, name: 'New Name', status: 'published', type: 'chat', description: '' }
      mockUpdateAgent.mockResolvedValue({ data: updatedAgent })

      await store.updateAgent(1, { name: 'New Name' })

      expect(store.agents[0].name).toBe('New Name')
    })

    it('更新 currentAgent 如果匹配', async () => {
      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Old', status: 'published', type: 'chat', description: '' },
      ] as unknown as typeof store.agents
      store.currentAgent = { id: 1, name: 'Old', status: 'published', type: 'chat', description: '' } as unknown as typeof store.currentAgent

      const updated = { id: 1, name: 'Updated', status: 'published', type: 'chat', description: '' }
      mockUpdateAgent.mockResolvedValue({ data: updated })

      await store.updateAgent(1, { name: 'Updated' })

      expect(store.currentAgent?.name).toBe('Updated')
    })

    it('更新失败时抛出错误', async () => {
      mockUpdateAgent.mockRejectedValue(new Error('Update failed'))

      const store = useAgentStore()
      await expect(store.updateAgent(1, { name: 'Fail' })).rejects.toThrow('Update failed')
    })
  })

  describe('deleteAgent', () => {
    it('成功删除 agent', async () => {
      mockDeleteAgent.mockResolvedValue({})

      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Agent 1', status: 'published', type: 'chat', description: '' },
        { id: 2, name: 'Agent 2', status: 'draft', type: 'chat', description: '' },
      ] as unknown as typeof store.agents

      await store.deleteAgent(1)

      expect(store.agents).toHaveLength(1)
      expect(store.agents[0].id).toBe(2)
    })

    it('删除时清除 currentAgent 如果匹配', async () => {
      mockDeleteAgent.mockResolvedValue({})

      const store = useAgentStore()
      store.agents = [
        { id: 1, name: 'Agent', status: 'published', type: 'chat', description: '' },
      ] as unknown as typeof store.agents
      store.currentAgent = { id: 1, name: 'Agent', status: 'published', type: 'chat', description: '' } as unknown as typeof store.currentAgent

      await store.deleteAgent(1)

      expect(store.currentAgent).toBeNull()
    })
  })

  describe('setFilters / resetFilters', () => {
    it('setFilters 合并过滤器', () => {
      const store = useAgentStore()
      store.setFilters({ keyword: 'test' })
      store.setFilters({ status: 'published' })

      expect(store.filters.keyword).toBe('test')
      expect(store.filters.status).toBe('published')
    })

    it('resetFilters 清空所有过滤器', () => {
      const store = useAgentStore()
      store.setFilters({ keyword: 'test', status: 'published' })
      store.resetFilters()

      expect(store.filters).toEqual({})
    })
  })
})
