import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * useConfirm composable 单元测试
 * 测试: confirm 成功, confirm 取消, 自定义选项
 */

// Mock ant-design-vue Modal at module level (hoisted by vitest)
let capturedOptions: Record<string, unknown> | null = null
let onOkCallback: (() => void) | undefined
let onCancelCallback: (() => void) | undefined

vi.mock('ant-design-vue', () => ({
  Modal: {
    confirm(options: Record<string, unknown>) {
      capturedOptions = options
      onOkCallback = options.onOk as (() => void) | undefined
      onCancelCallback = options.onCancel as (() => void) | undefined
    },
  },
}))

describe('useConfirm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    capturedOptions = null
    onOkCallback = undefined
    onCancelCallback = undefined
  })

  async function getUseConfirm() {
    const { useConfirm } = await import('@/composables/useConfirm')
    return useConfirm()
  }

  // ---------- confirm 成功 ----------

  describe('confirm 成功', () => {
    it('点击 OK 时 resolve(true)', async () => {
      const { confirm } = await getUseConfirm()
      const promise = confirm({ title: '确认删除?' })

      expect(capturedOptions).not.toBeNull()
      expect(onOkCallback).toBeDefined()

      // 模拟点击 OK
      onOkCallback!()

      const result = await promise
      expect(result).toBe(true)
    })

    it('默认 okType 为 primary', async () => {
      const { confirm } = await getUseConfirm()
      confirm({ title: 'Test' })

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          okType: 'primary',
        }),
      )
    })
  })

  // ---------- confirm 取消 ----------

  describe('confirm 取消', () => {
    it('点击 Cancel 时 resolve(false)', async () => {
      const { confirm } = await getUseConfirm()
      const promise = confirm({ title: '确认?' })

      // 模拟点击取消
      onCancelCallback!()

      const result = await promise
      expect(result).toBe(false)
    })
  })

  // ---------- 自定义选项 ----------

  describe('自定义选项', () => {
    it('传递自定义 title', async () => {
      const { confirm } = await getUseConfirm()
      confirm({ title: '自定义标题' })

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          title: '自定义标题',
        }),
      )
    })

    it('传递自定义 content', async () => {
      const { confirm } = await getUseConfirm()
      confirm({ content: '确认删除此项目?' })

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          content: '确认删除此项目?',
        }),
      )
    })

    it('传递自定义 okText 和 cancelText', async () => {
      const { confirm } = await getUseConfirm()
      confirm({
        title: '删除',
        okText: '确认删除',
        cancelText: '取消操作',
      })

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          okText: '确认删除',
          cancelText: '取消操作',
        }),
      )
    })

    it('传递自定义 okType 为 danger', async () => {
      const { confirm } = await getUseConfirm()
      confirm({
        title: '删除',
        okType: 'danger',
      })

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          okType: 'danger',
        }),
      )
    })

    it('无参数调用时使用默认值', async () => {
      const { confirm } = await getUseConfirm()
      confirm()

      expect(capturedOptions).toEqual(
        expect.objectContaining({
          title: '',
          content: '',
          okType: 'primary',
        }),
      )
    })

    it('每次调用 confirm 都创建新的 Promise', async () => {
      const { confirm } = await getUseConfirm()
      const promise1 = confirm({ title: '第一个' })

      // Save first callback
      const firstOnOk = onOkCallback

      const promise2 = confirm({ title: '第二个' })

      // Resolve first
      firstOnOk!()
      // Resolve second
      onOkCallback!()

      const [r1, r2] = await Promise.all([promise1, promise2])
      expect(r1).toBe(true)
      expect(r2).toBe(true)
    })
  })
})
