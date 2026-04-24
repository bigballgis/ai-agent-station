import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWorkflowStore } from '@/store/modules/workflow'

/**
 * Workflow Store 单元测试
 * 测试: state, getters, actions, 错误处理
 */

// ==================== Mocks ====================

const mockGetDefinitions = vi.fn()
const mockGetInstances = vi.fn()
const mockGetInstance = vi.fn()
const mockStartWorkflow = vi.fn()
const mockApproveNode = vi.fn()
const mockRejectNode = vi.fn()
const mockGetInstanceHistory = vi.fn()
const mockCancelWorkflow = vi.fn()

vi.mock('@/api/workflow', () => ({
  workflowApi: {
    getDefinitions: (p?: number, s?: number, st?: string) => mockGetDefinitions(p, s, st),
    getInstances: (p?: number, s?: number, f?: unknown) => mockGetInstances(p, s, f),
    getInstance: (id: number) => mockGetInstance(id),
    startWorkflow: (id: number, vars?: Record<string, unknown>) => mockStartWorkflow(id, vars),
    approveNode: (iId: number, nId: string, c?: string) => mockApproveNode(iId, nId, c),
    rejectNode: (iId: number, nId: string, c?: string) => mockRejectNode(iId, nId, c),
    getInstanceHistory: (id: number) => mockGetInstanceHistory(id),
    cancelWorkflow: (id: number) => mockCancelWorkflow(id),
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

describe('Workflow Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('definitions 默认为空数组', () => {
      const store = useWorkflowStore()
      expect(store.definitions).toEqual([])
    })

    it('instances 默认为空数组', () => {
      const store = useWorkflowStore()
      expect(store.instances).toEqual([])
    })

    it('currentInstance 默认为 null', () => {
      const store = useWorkflowStore()
      expect(store.currentInstance).toBeNull()
    })

    it('nodeLogs 默认为空数组', () => {
      const store = useWorkflowStore()
      expect(store.nodeLogs).toEqual([])
    })

    it('loading 默认为 false', () => {
      const store = useWorkflowStore()
      expect(store.loading).toBe(false)
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('activeDefinitions 过滤已发布的定义', () => {
      const store = useWorkflowStore()
      store.definitions = [
        { id: 1, name: 'WF1', status: 'PUBLISHED' },
        { id: 2, name: 'WF2', status: 'DRAFT' },
        { id: 3, name: 'WF3', status: 'PUBLISHED' },
      ] as unknown as typeof store.definitions

      expect(store.activeDefinitions).toHaveLength(2)
    })

    it('runningInstances 过滤运行中和待处理的实例', () => {
      const store = useWorkflowStore()
      store.instances = [
        { id: 1, status: 'RUNNING' },
        { id: 2, status: 'COMPLETED' },
        { id: 3, status: 'PENDING' },
        { id: 4, status: 'CANCELLED' },
      ] as unknown as typeof store.instances

      expect(store.runningInstances).toHaveLength(2)
    })
  })

  // ---------- Actions ----------

  describe('fetchDefinitions', () => {
    it('成功获取流程定义（分页格式）', async () => {
      mockGetDefinitions.mockResolvedValue({
        data: {
          records: [
            { id: 1, name: 'WF1', status: 'PUBLISHED' },
            { id: 2, name: 'WF2', status: 'DRAFT' },
          ],
          total: 2,
        },
      })

      const store = useWorkflowStore()
      await store.fetchDefinitions(0, 10)

      expect(store.definitions).toHaveLength(2)
      expect(store.loading).toBe(false)
    })

    it('获取失败时 loading 重置为 false', async () => {
      mockGetDefinitions.mockRejectedValue(new Error('Network Error'))

      const store = useWorkflowStore()
      await expect(store.fetchDefinitions()).rejects.toThrow('Network Error')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchInstances', () => {
    it('成功获取流程实例（分页格式）', async () => {
      mockGetInstances.mockResolvedValue({
        data: {
          records: [{ id: 1, status: 'RUNNING' }],
          total: 1,
        },
      })

      const store = useWorkflowStore()
      await store.fetchInstances(0, 10)

      expect(store.instances).toHaveLength(1)
      expect(store.loading).toBe(false)
    })

    it('支持过滤参数', async () => {
      mockGetInstances.mockResolvedValue({
        data: { records: [], total: 0 },
      })

      const store = useWorkflowStore()
      await store.fetchInstances(0, 10, { status: 'RUNNING' })

      expect(mockGetInstances).toHaveBeenCalledWith(0, 10, { status: 'RUNNING' })
    })

    it('获取失败时 loading 重置为 false', async () => {
      mockGetInstances.mockRejectedValue(new Error('Error'))

      const store = useWorkflowStore()
      await expect(store.fetchInstances()).rejects.toThrow('Error')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchInstanceById', () => {
    it('成功获取实例详情', async () => {
      const mockInstance = { id: 1, status: 'RUNNING', name: 'Test' }
      mockGetInstance.mockResolvedValue({ data: mockInstance })

      const store = useWorkflowStore()
      await store.fetchInstanceById(1)

      expect(store.currentInstance).toEqual(mockInstance)
      expect(store.loading).toBe(false)
    })

    it('获取失败时 loading 重置为 false', async () => {
      mockGetInstance.mockRejectedValue(new Error('Not found'))

      const store = useWorkflowStore()
      await expect(store.fetchInstanceById(999)).rejects.toThrow('Not found')
      expect(store.loading).toBe(false)
    })
  })

  describe('startWorkflow', () => {
    it('成功启动流程并添加到实例列表', async () => {
      const newInstance = { id: 10, status: 'RUNNING', name: 'New WF' }
      mockStartWorkflow.mockResolvedValue({ data: newInstance })

      const store = useWorkflowStore()
      const result = await store.startWorkflow(1, { input: 'test' })

      expect(store.instances[0]).toEqual(newInstance)
      expect(result).toEqual(newInstance)
    })

    it('启动失败时抛出错误', async () => {
      mockStartWorkflow.mockRejectedValue(new Error('Start failed'))

      const store = useWorkflowStore()
      await expect(store.startWorkflow(1)).rejects.toThrow('Start failed')
      expect(store.instances).toHaveLength(0)
    })
  })

  describe('approveNode', () => {
    it('批准节点', async () => {
      const mockLog = { id: 1, action: 'APPROVED', comment: 'OK' }
      mockApproveNode.mockResolvedValue({ data: mockLog })

      const store = useWorkflowStore()
      const result = await store.approveNode(1, 'node1', true, 'OK')

      expect(result).toEqual(mockLog)
      expect(mockApproveNode).toHaveBeenCalledWith(1, 'node1', 'OK')
    })

    it('拒绝节点', async () => {
      const mockLog = { id: 2, action: 'REJECTED', comment: 'No' }
      mockRejectNode.mockResolvedValue({ data: mockLog })

      const store = useWorkflowStore()
      const result = await store.approveNode(1, 'node1', false, 'No')

      expect(result).toEqual(mockLog)
      expect(mockRejectNode).toHaveBeenCalledWith(1, 'node1', 'No')
    })

    it('批准失败时抛出错误', async () => {
      mockApproveNode.mockRejectedValue(new Error('Approve failed'))

      const store = useWorkflowStore()
      await expect(store.approveNode(1, 'node1', true)).rejects.toThrow('Approve failed')
    })
  })

  describe('fetchNodeLogs', () => {
    it('成功获取节点日志', async () => {
      const mockLogs = [
        { id: 1, action: 'APPROVED' },
        { id: 2, action: 'REJECTED' },
      ]
      mockGetInstanceHistory.mockResolvedValue({ data: mockLogs })

      const store = useWorkflowStore()
      const result = await store.fetchNodeLogs(1)

      expect(store.nodeLogs).toHaveLength(2)
      expect(result).toEqual(mockLogs)
    })

    it('获取失败时返回空数组', async () => {
      mockGetInstanceHistory.mockRejectedValue(new Error('Error'))

      const store = useWorkflowStore()
      const result = await store.fetchNodeLogs(1)

      expect(result).toEqual([])
    })
  })

  describe('cancelInstance', () => {
    it('成功取消实例', async () => {
      mockCancelWorkflow.mockResolvedValue({})

      const store = useWorkflowStore()
      store.instances = [
        { id: 1, status: 'RUNNING' },
        { id: 2, status: 'RUNNING' },
      ] as unknown as typeof store.instances

      await store.cancelInstance(1)

      expect(store.instances[0].status).toBe('CANCELLED')
      expect(store.instances[1].status).toBe('RUNNING')
    })

    it('取消时更新 currentInstance 如果匹配', async () => {
      mockCancelWorkflow.mockResolvedValue({})

      const store = useWorkflowStore()
      store.currentInstance = { id: 1, status: 'RUNNING' } as unknown as typeof store.currentInstance

      await store.cancelInstance(1)

      expect(store.currentInstance?.status).toBe('CANCELLED')
    })
  })
})
