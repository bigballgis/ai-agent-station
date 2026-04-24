import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/store/modules/notification'

/**
 * Notification Store 单元测试
 * 测试: state, getters, REST actions, 错误处理
 */

// ==================== Mocks ====================

const mockGetNotifications = vi.fn()
const mockMarkAsRead = vi.fn()
const mockMarkAllAsRead = vi.fn()

vi.mock('@/api/notification', () => ({
  notificationApi: {
    getNotifications: (params?: unknown) => mockGetNotifications(params),
    markAsRead: (id: number) => mockMarkAsRead(id),
    markAllAsRead: () => mockMarkAllAsRead(),
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

describe('Notification Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('notifications 默认为空数组', () => {
      const store = useNotificationStore()
      expect(store.notifications).toEqual([])
    })

    it('unreadCount 默认为 0', () => {
      const store = useNotificationStore()
      expect(store.unreadCount).toBe(0)
    })

    it('wsConnected 默认为 false', () => {
      const store = useNotificationStore()
      expect(store.wsConnected).toBe(false)
    })
  })

  // ---------- Getters ----------

  describe('Getters', () => {
    it('unreadNotifications 过滤未读通知', () => {
      const store = useNotificationStore()
      store.notifications = [
        { id: 1, read: false, createdAt: '2024-01-01' },
        { id: 2, read: true, createdAt: '2024-01-02' },
        { id: 3, read: false, createdAt: '2024-01-03' },
      ] as unknown as typeof store.notifications

      expect(store.unreadNotifications).toHaveLength(2)
    })

    it('sortedNotifications 按创建时间降序排列', () => {
      const store = useNotificationStore()
      store.notifications = [
        { id: 1, read: false, createdAt: '2024-01-01T10:00:00' },
        { id: 2, read: false, createdAt: '2024-01-03T10:00:00' },
        { id: 3, read: false, createdAt: '2024-01-02T10:00:00' },
      ] as unknown as typeof store.notifications

      const sorted = store.sortedNotifications
      expect(sorted[0].id).toBe(2)
      expect(sorted[1].id).toBe(3)
      expect(sorted[2].id).toBe(1)
    })
  })

  // ---------- Actions ----------

  describe('fetchNotifications', () => {
    it('成功获取通知（数组格式）', async () => {
      const mockData = [
        { id: 1, read: false, createdAt: '2024-01-01' },
        { id: 2, read: true, createdAt: '2024-01-02' },
      ]
      mockGetNotifications.mockResolvedValue({ data: mockData })

      const store = useNotificationStore()
      await store.fetchNotifications()

      expect(store.notifications).toHaveLength(2)
      expect(store.unreadCount).toBe(1)
    })

    it('成功获取通知（分页格式）', async () => {
      mockGetNotifications.mockResolvedValue({
        data: {
          records: [
            { id: 1, read: false, createdAt: '2024-01-01' },
          ],
        },
      })

      const store = useNotificationStore()
      await store.fetchNotifications({ page: 1, size: 10 })

      expect(store.notifications).toHaveLength(1)
    })

    it('获取失败时返回 undefined', async () => {
      mockGetNotifications.mockRejectedValue(new Error('Network Error'))

      const store = useNotificationStore()
      const result = await store.fetchNotifications()

      expect(result).toBeUndefined()
    })
  })

  describe('markAsRead', () => {
    it('标记单个通知为已读', async () => {
      mockMarkAsRead.mockResolvedValue({})

      const store = useNotificationStore()
      store.notifications = [
        { id: 1, read: false, createdAt: '2024-01-01' },
      ] as unknown as typeof store.notifications
      store.unreadCount = 1

      await store.markAsRead(1)

      expect(store.notifications[0].read).toBe(true)
      expect(store.unreadCount).toBe(0)
    })

    it('已读通知不减少 unreadCount', async () => {
      mockMarkAsRead.mockResolvedValue({})

      const store = useNotificationStore()
      store.notifications = [
        { id: 1, read: true, createdAt: '2024-01-01' },
      ] as unknown as typeof store.notifications
      store.unreadCount = 0

      await store.markAsRead(1)

      expect(store.unreadCount).toBe(0)
    })
  })

  describe('markAllAsRead', () => {
    it('标记所有通知为已读', async () => {
      mockMarkAllAsRead.mockResolvedValue({})

      const store = useNotificationStore()
      store.notifications = [
        { id: 1, read: false, createdAt: '2024-01-01' },
        { id: 2, read: false, createdAt: '2024-01-02' },
        { id: 3, read: true, createdAt: '2024-01-03' },
      ] as unknown as typeof store.notifications
      store.unreadCount = 2

      await store.markAllAsRead()

      expect(store.notifications.every((n) => n.read)).toBe(true)
      expect(store.unreadCount).toBe(0)
    })
  })
})
