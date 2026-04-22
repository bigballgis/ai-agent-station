import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Dict API 模块测试
 * 测试字典相关的 API 调用
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

describe('Dict API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('获取字典类型列表 - 调用 GET /dict-types', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { total: 2, records: [{ id: 1, dictName: '用户状态', dictType: 'user_status' }] }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/dict-types', { params: { page: 0, size: 10 } })

    expect(mockGet).toHaveBeenCalledWith('/dict-types', { params: { page: 0, size: 10 } })
    expect(result.status).toBe(200)
  })

  it('创建字典类型 - 调用 POST /dict-types', async () => {
    const newDict = { dictName: '订单状态', dictType: 'order_status' }
    mockPost.mockResolvedValue({ code: 200, data: { id: 2, ...newDict } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/dict-types', newDict)

    expect(mockPost).toHaveBeenCalledWith('/dict-types', newDict)
    expect(result.data.dictType).toBe('order_status')
  })

  it('更新字典类型 - 调用 PUT /dict-types/:id', async () => {
    const updateData = { dictName: '用户状态（更新）' }
    mockPut.mockResolvedValue({ code: 200, data: { id: 1, ...updateData } })

    const request = (await import('@/utils/request')).default
    const result = await request.put('/dict-types/1', updateData)

    expect(mockPut).toHaveBeenCalledWith('/dict-types/1', updateData)
    expect(result.data.dictName).toBe('用户状态（更新）')
  })

  it('删除字典类型 - 调用 DELETE /dict-types/:id', async () => {
    mockDelete.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.delete('/dict-types/1')

    expect(mockDelete).toHaveBeenCalledWith('/dict-types/1')
    expect(result.status).toBe(200)
  })

  it('获取字典项列表 - 调用 GET /dict-types/:dictType/items', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: [{ id: 1, dictLabel: '启用', dictValue: '1' }]
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/dict-types/user_status/items')

    expect(mockGet).toHaveBeenCalledWith('/dict-types/user_status/items')
    expect(result.data).toHaveLength(1)
    expect(result.data[0].dictLabel).toBe('启用')
  })

  it('批量获取字典项 - 调用 GET /dict-types/dict-items/batch', async () => {
    mockGet.mockResolvedValue({
      code: 200,
      data: { user_status: [{ dictLabel: '启用' }] }
    })

    const request = (await import('@/utils/request')).default
    const result = await request.get('/dict-types/dict-items/batch', {
      params: { dictTypes: 'user_status,order_status' }
    })

    expect(mockGet).toHaveBeenCalled()
    expect(result.data).toHaveProperty('user_status')
  })

  it('创建字典项 - 调用 POST /dict-types/dict-items', async () => {
    const newItem = { dictType: 'user_status', dictLabel: '禁用', dictValue: '0' }
    mockPost.mockResolvedValue({ code: 200, data: { id: 10, ...newItem } })

    const request = (await import('@/utils/request')).default
    const result = await request.post('/dict-types/dict-items', newItem)

    expect(mockPost).toHaveBeenCalledWith('/dict-types/dict-items', newItem)
    expect(result.data.dictLabel).toBe('禁用')
  })

  it('删除字典项 - 调用 DELETE /dict-types/dict-items/:id', async () => {
    mockDelete.mockResolvedValue({ code: 200, data: null })

    const request = (await import('@/utils/request')).default
    const result = await request.delete('/dict-types/dict-items/10')

    expect(mockDelete).toHaveBeenCalledWith('/dict-types/dict-items/10')
    expect(result.status).toBe(200)
  })
})
