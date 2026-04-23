import { describe, it, expect, vi, beforeEach } from 'vitest'

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

  it('获取工作流列表 - 调用 GET /v1/workflows', async () => {
    mockGet.mockResolvedValue({ code: 200, data: [{ id: 1, name: 'test', status: 'ACTIVE' }] })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/workflows')

    expect(mockGet).toHaveBeenCalledWith('/v1/workflows')
    expect(result.code).toBe(200)
    expect(result.data).toHaveLength(1)
  })

  it('获取工作流详情 - 调用 GET /v1/workflows/:id', async () => {
    mockGet.mockResolvedValue({ code: 200, data: { id: 1, name: 'test' } })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/workflows/1')

    expect(mockGet).toHaveBeenCalledWith('/v1/workflows/1')
    expect(result.data.id).toBe(1)
  })

  it('启动工作流 - 调用 POST /v1/workflows/:id/start', async () => {
    mockPost.mockResolvedValue({ code: 200, data: { executionId: 'exec-001', status: 'RUNNING' } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/workflows/1/start', { input: { query: 'test' } })

    expect(mockPost).toHaveBeenCalledWith('/v1/workflows/1/start', { input: { query: 'test' } })
    expect(result.data.status).toBe('RUNNING')
  })

  it('取消工作流 - 调用 POST /v1/workflows/:id/cancel', async () => {
    mockPost.mockResolvedValue({ code: 200, data: { status: 'CANCELLED' } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/workflows/1/cancel')

    expect(mockPost).toHaveBeenCalledWith('/v1/workflows/1/cancel')
    expect(result.data.status).toBe('CANCELLED')
  })

  it('获取执行历史 - 调用 GET /v1/workflows/:id/executions', async () => {
    mockGet.mockResolvedValue({ code: 200, data: [{ executionId: 'exec-001' }] })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/workflows/1/executions')

    expect(mockGet).toHaveBeenCalledWith('/v1/workflows/1/executions')
    expect(result.data).toHaveLength(1)
  })

  it('API 调用失败 - 网络错误', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'))

    const request = (await import('@/utils/request')).default
    await expect(request.get('/v1/workflows')).rejects.toThrow('Network Error')
  })
})
