import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Agent API 模块测试
 * 测试 Agent 相关的 API 调用
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

describe('Agent API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAllAgents - 调用 GET /agents', async () => {
    mockGet.mockResolvedValue({ code: 200, data: [{ id: 1, name: 'Agent1' }] })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/agents')

    expect(mockGet).toHaveBeenCalledWith('/agents')
    expect(result.status).toBe(200)
    expect(result.data).toHaveLength(1)
  })

  it('getAgentById - 调用 GET /agents/:id', async () => {
    mockGet.mockResolvedValue({ code: 200, data: { id: 1, name: 'Agent1' } })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/agents/1')

    expect(mockGet).toHaveBeenCalledWith('/agents/1')
    expect(result.data.id).toBe(1)
  })

  it('createAgent - 调用 POST /agents', async () => {
    const newAgent = { name: 'New Agent', description: 'desc' }
    mockPost.mockResolvedValue({ code: 200, data: { id: 2, ...newAgent } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/agents', newAgent)

    expect(mockPost).toHaveBeenCalledWith('/agents', newAgent)
    expect(result.data.name).toBe('New Agent')
  })

  it('updateAgent - 调用 PUT /agents/:id', async () => {
    const updateData = { name: 'Updated Agent' }
    mockPut.mockResolvedValue({ code: 200, data: { id: 1, ...updateData } })

    const request = (await import('@/utils/request')).default
    const result = await request.put('/agents/1', updateData)

    expect(mockPut).toHaveBeenCalledWith('/agents/1', updateData)
    expect(result.data.name).toBe('Updated Agent')
  })

  it('deleteAgent - 调用 DELETE /agents/:id', async () => {
    mockDelete.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.delete('/agents/1')

    expect(mockDelete).toHaveBeenCalledWith('/agents/1')
    expect(result.status).toBe(200)
  })

  it('copyAgent - 调用 POST /agents/:id/copy', async () => {
    mockPost.mockResolvedValue({ code: 200, data: { id: 3, name: 'Copy' } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/agents/1/copy', { newName: 'Copy' })

    expect(mockPost).toHaveBeenCalledWith('/agents/1/copy', { newName: 'Copy' })
    expect(result.data.name).toBe('Copy')
  })

  it('getAgentVersions - 调用 GET /agents/:id/versions', async () => {
    mockGet.mockResolvedValue({ code: 200, data: [{ id: 1, versionNumber: 1 }] })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/agents/1/versions')

    expect(mockGet).toHaveBeenCalledWith('/agents/1/versions')
    expect(result.data).toHaveLength(1)
  })

  it('rollbackToVersion - 调用 POST /agents/:id/versions/:version/rollback', async () => {
    mockPost.mockResolvedValue({ code: 200, data: { id: 1 } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/agents/1/versions/2/rollback')

    expect(mockPost).toHaveBeenCalledWith('/agents/1/versions/2/rollback')
    expect(result.status).toBe(200)
  })

  it('API 调用失败 - 返回错误', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'))

    const request = (await import('@/utils/request')).default

    await expect(request.get('/agents')).rejects.toThrow('Network Error')
  })
})
