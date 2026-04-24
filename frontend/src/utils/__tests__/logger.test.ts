import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/**
 * Logger 工具测试
 * 测试不同环境下的日志输出行为及前缀格式
 */

describe('logger 工具', () => {
  let debugSpy: ReturnType<typeof vi.spyOn>
  let infoSpy: ReturnType<typeof vi.spyOn>
  let warnSpy: ReturnType<typeof vi.spyOn>
  let errorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    infoSpy = vi.spyOn(console, 'info').mockImplementation(() => {})
    warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllEnvs()
  })

  describe('开发环境 (DEV=true)', () => {
    beforeEach(() => {
      vi.stubEnv('DEV', true)
      vi.resetModules()
    })

    it('debug 在开发环境下输出日志', async () => {
      const { logger } = await import('@/utils/logger')
      logger.debug('test message')
      expect(debugSpy).toHaveBeenCalledTimes(1)
      expect(debugSpy).toHaveBeenCalledWith('[DEBUG]', 'test message')
    })

    it('info 在开发环境下输出日志', async () => {
      const { logger } = await import('@/utils/logger')
      logger.info('test info')
      expect(infoSpy).toHaveBeenCalledTimes(1)
      expect(infoSpy).toHaveBeenCalledWith('[INFO]', 'test info')
    })

    it('debug 使用 [DEBUG] 前缀', async () => {
      const { logger } = await import('@/utils/logger')
      logger.debug('hello', 'world')
      expect(debugSpy).toHaveBeenCalledWith('[DEBUG]', 'hello', 'world')
    })

    it('info 使用 [INFO] 前缀', async () => {
      const { logger } = await import('@/utils/logger')
      logger.info('hello', 'world')
      expect(infoSpy).toHaveBeenCalledWith('[INFO]', 'hello', 'world')
    })
  })

  describe('生产环境 (DEV=false)', () => {
    beforeEach(() => {
      vi.stubEnv('DEV', false)
      vi.resetModules()
    })

    it('debug 在生产环境下不输出日志', async () => {
      const { logger } = await import('@/utils/logger')
      logger.debug('should not appear')
      expect(debugSpy).not.toHaveBeenCalled()
    })

    it('info 在生产环境下不输出日志', async () => {
      const { logger } = await import('@/utils/logger')
      logger.info('should not appear')
      expect(infoSpy).not.toHaveBeenCalled()
    })
  })

  describe('warn 和 error 在所有环境下都输出', () => {
    it('warn 始终输出日志', async () => {
      vi.stubEnv('DEV', false)
      vi.resetModules()
      const { logger } = await import('@/utils/logger')
      logger.warn('warning message')
      expect(warnSpy).toHaveBeenCalledTimes(1)
      expect(warnSpy).toHaveBeenCalledWith('[WARN]', 'warning message')
    })

    it('error 始终输出日志', async () => {
      vi.stubEnv('DEV', false)
      vi.resetModules()
      const { logger } = await import('@/utils/logger')
      logger.error('error message')
      expect(errorSpy).toHaveBeenCalledTimes(1)
      expect(errorSpy).toHaveBeenCalledWith('[ERROR]', 'error message')
    })

    it('warn 使用 [WARN] 前缀', async () => {
      const { logger } = await import('@/utils/logger')
      logger.warn('caution', { detail: true })
      expect(warnSpy).toHaveBeenCalledWith('[WARN]', 'caution', { detail: true })
    })

    it('error 使用 [ERROR] 前缀', async () => {
      const { logger } = await import('@/utils/logger')
      logger.error('oops', new Error('test'))
      expect(errorSpy).toHaveBeenCalledWith('[ERROR]', 'oops', expect.any(Error))
    })

    it('warn 在开发环境下也正常输出', async () => {
      vi.stubEnv('DEV', true)
      vi.resetModules()
      const { logger } = await import('@/utils/logger')
      logger.warn('dev warning')
      expect(warnSpy).toHaveBeenCalledTimes(1)
      expect(warnSpy).toHaveBeenCalledWith('[WARN]', 'dev warning')
    })

    it('error 在开发环境下也正常输出', async () => {
      vi.stubEnv('DEV', true)
      vi.resetModules()
      const { logger } = await import('@/utils/logger')
      logger.error('dev error')
      expect(errorSpy).toHaveBeenCalledTimes(1)
      expect(errorSpy).toHaveBeenCalledWith('[ERROR]', 'dev error')
    })
  })

  describe('多参数支持', () => {
    it('debug 支持多个参数', async () => {
      vi.stubEnv('DEV', true)
      vi.resetModules()
      const { logger } = await import('@/utils/logger')
      logger.debug('arg1', 'arg2', 'arg3', { key: 'value' })
      expect(debugSpy).toHaveBeenCalledWith('[DEBUG]', 'arg1', 'arg2', 'arg3', { key: 'value' })
    })

    it('error 支持多个参数', async () => {
      const { logger } = await import('@/utils/logger')
      logger.error('err1', 'err2', 42)
      expect(errorSpy).toHaveBeenCalledWith('[ERROR]', 'err1', 'err2', 42)
    })
  })
})
