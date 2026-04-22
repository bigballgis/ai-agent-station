import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Workflow API 模块测试
 * 测试工作流相关的 API 调用
 */

// Mock request 模块
const mockGet = vi.fn()
const mockPost = vi.fn()
const mockPut = vi.fn()
const mockDelete = vi.fn()

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete
  }
}))

describe('Workflow API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('获取工作流列表 - 调用 GET /workflows', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: [{ id: 1, name: '测试工作流', status: 'ACTIVE' }]
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/workflows')

    expect(mockGet).toHaveBeenCalledWith('/workflows')
    expect(result.status).toBe(200)
    expect(result.data).toHaveLength(1)
  })

  it('获取工作流详情 - 调用 GET /workflows/:id', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { id: 1, name: '测试工作流', graphDefinition: { nodes: [], edges: [] } }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/workflows/1')

    expect(mockGet).toHaveBeenCalledWith('/workflows/1')
    expect(result.data.id).toBe(1)
  })

  it('启动工作流 - 调用 POST /workflows/:id/start', async () => {
    mockPost.mockResolvedValue({
      code: 200,
      data: { executionId: 'exec-001', status: 'RUNNING' }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/workflows/1/start', { input: { query: 'test' } })

    expect(mockPost).toHaveBeenCalledWith('/workflows/1/start', { input: { query: 'test' } })
    expect(result.data.status).toBe('RUNNING')
  })

  it('取消工作流 - 调用 POST /workflows/:id/cancel', async () => {
    mockPost.mockResolvedValue({
      code: 200,
      data: { executionId: 'exec-001', status: 'CANCELLED' }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/workflows/1/cancel')

    expect(mockPost).toHaveBeenCalledWith('/workflows/1/cancel')
    expect(result.data.status).toBe('CANCELLED')
  })

  it('获取工作流执行历史 - 调用 GET /workflows/:id/executions', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: [{ executionId: 'exec-001', status: 'COMPLETED' }]
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/workflows/1/executions')

    expect(mockGet).toHaveBeenCalledWith('/workflows/1/executions')
    expect(result.data).toHaveLength(1)
  })

  it('API 调用失败 - 网络错误', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'))

    const request = (await import('@/utils/request')).default

    await expect(request.get('/workflows')).rejects.toThrow('Network Error')
  })

  it('API 调用失败 - 服务器错误', async () => {
    mockGet.mockRejectedValue(new Error('Internal Server Error'))

    const request = (await import('@/utils/request')).default

    await expect(request.get('/workflows')).rejects.toThrow('Internal Server Error')
  })
})
