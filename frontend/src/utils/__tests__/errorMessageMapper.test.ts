import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * errorMessageMapper 工具测试
 * 测试 messageCode 到 i18n key 的映射、未知 code 回退、axios response 提取
 */

// Mock @/locales
const mockT = vi.fn()
vi.mock('@/locales', () => ({
  default: {
    global: {
      t: mockT,
    },
  },
}))

describe('errorMessageMapper', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getErrorMessage', () => {
    it('映射已知的 Agent messageCode 到 i18n key', async () => {
      mockT.mockReturnValue('Agent 不存在')
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.agent.not_found', '默认消息')
      expect(mockT).toHaveBeenCalledWith('error.agentNotFound')
      expect(result).toBe('Agent 不存在')
    })

    it('映射已知的认证 messageCode', async () => {
      mockT.mockReturnValue('凭证无效')
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.auth.bad_credentials', '默认消息')
      expect(mockT).toHaveBeenCalledWith('error.auth.badCredentials')
      expect(result).toBe('凭证无效')
    })

    it('映射已知的权限 messageCode', async () => {
      mockT.mockReturnValue('权限不足')
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.permission.denied', '默认消息')
      expect(mockT).toHaveBeenCalledWith('error.permission.denied')
      expect(result).toBe('权限不足')
    })

    it('映射已知的通用 messageCode', async () => {
      mockT.mockReturnValue('操作失败')
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.common.operation_failed', '默认消息')
      expect(mockT).toHaveBeenCalledWith('error.common.operationFailed')
      expect(result).toBe('操作失败')
    })

    it('未知 messageCode 返回 fallback', async () => {
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.unknown.code', '自定义兜底消息')
      expect(mockT).not.toHaveBeenCalled()
      expect(result).toBe('自定义兜底消息')
    })

    it('i18n 翻译结果等于 key 时返回 fallback', async () => {
      // vue-i18n 在找不到 key 时会返回 key 本身
      mockT.mockImplementation((key: string) => key)
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.agent.not_found', 'Agent 未找到')
      expect(result).toBe('Agent 未找到')
    })

    it('映射已知的限流 messageCode', async () => {
      mockT.mockReturnValue('请求过于频繁')
      const { getErrorMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorMessage('error.rate_limit.exceeded', '默认消息')
      expect(mockT).toHaveBeenCalledWith('error.rateLimit.exceeded')
      expect(result).toBe('请求过于频繁')
    })
  })

  describe('getErrorDisplayMessage', () => {
    it('从 axios response 中提取 messageCode 并翻译', async () => {
      mockT.mockReturnValue('Agent 不存在')
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const errorResponse = {
        code: 404,
        message: 'Agent not found',
        messageCode: 'error.agent.not_found',
      }
      const result = getErrorDisplayMessage(errorResponse, '默认错误')
      expect(mockT).toHaveBeenCalledWith('error.agentNotFound')
      expect(result).toBe('Agent 不存在')
    })

    it('无 messageCode 时使用 message 字段', async () => {
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const errorResponse = {
        code: 500,
        message: 'Internal Server Error',
      }
      const result = getErrorDisplayMessage(errorResponse, '默认错误')
      expect(mockT).not.toHaveBeenCalled()
      expect(result).toBe('Internal Server Error')
    })

    it('errorResponse 为 undefined 时返回 fallback', async () => {
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorDisplayMessage(undefined, '网络异常')
      expect(result).toBe('网络异常')
    })

    it('errorResponse 为 null 时返回 fallback', async () => {
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorDisplayMessage(null as unknown as Record<string, unknown>, '网络异常')
      expect(result).toBe('网络异常')
    })

    it('messageCode 存在但翻译失败时使用 message 作为 fallback', async () => {
      // i18n 找不到 key，返回 key 本身
      mockT.mockImplementation((key: string) => key)
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const errorResponse = {
        code: 400,
        message: '参数校验失败',
        messageCode: 'error.validation.format',
      }
      const result = getErrorDisplayMessage(errorResponse, '默认错误')
      // 翻译结果等于 key，回退到 message
      expect(result).toBe('参数校验失败')
    })

    it('messageCode 和 message 都不存在时返回 fallback', async () => {
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const errorResponse = {
        code: 500,
      }
      const result = getErrorDisplayMessage(errorResponse, '服务器错误')
      expect(result).toBe('服务器错误')
    })

    it('空对象作为 errorResponse 时返回 fallback', async () => {
      const { getErrorDisplayMessage } = await import('@/utils/errorMessageMapper')
      const result = getErrorDisplayMessage({}, '默认错误')
      expect(result).toBe('默认错误')
    })
  })
})
