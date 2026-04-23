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

describe('Dict API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('获取字典列表 - 调用 GET /v1/dict-types', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { records: [{ id: 1, dictName: '用户状态', dictType: 'user_status' }], total: 1 }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/dict-types', { params: { page: 0, size: 10 } })

    expect(mockGet).toHaveBeenCalledWith('/v1/dict-types', { params: { page: 0, size: 10 } })
    expect(result.code).toBe(200)
    expect(result.data.records).toHaveLength(1)
  })

  it('获取字典详情 - 调用 GET /v1/dict-types/:id', async () => {
    mockGet.mockResolvedValue({ code: 200, data: { id: 1, dictName: '用户状态' } })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/v1/dict-types/1')

    expect(mockGet).toHaveBeenCalledWith('/v1/dict-types/1')
    expect(result.data.id).toBe(1)
  })

  it('创建字典 - 调用 POST /v1/dict-types', async () => {
    const newDict = { dictName: '订单状态', dictType: 'order_status' }
    mockPost.mockResolvedValue({ code: 200, data: { id: 2, ...newDict } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/v1/dict-types', newDict)

    expect(mockPost).toHaveBeenCalledWith('/v1/dict-types', newDict)
    expect(result.data.dictName).toBe('订单状态')
  })

  it('更新字典 - 调用 PUT /v1/dict-types/:id', async () => {
    mockPut.mockResolvedValue({ code: 200, data: { id: 1, dictName: '用户状态(已更新)' } })

    const request = (await import('@/utils/request')).default
    const result = await request.put('/v1/dict-types/1', { dictName: '用户状态(已更新)' })

    expect(mockPut).toHaveBeenCalledWith('/v1/dict-types/1', { dictName: '用户状态(已更新)' })
    expect(result.data.dictName).toBe('用户状态(已更新)')
  })

  it('删除字典 - 调用 DELETE /v1/dict-types/:id', async () => {
    mockDelete.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.delete('/v1/dict-types/1')

    expect(mockDelete).toHaveBeenCalledWith('/v1/dict-types/1')
    expect(result.code).toBe(200)
  })
})
