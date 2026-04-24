import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/store/modules/notification'

/**
 * Notification Store 单元测试
 * 测试: state, getters, REST actions, enhanced real-time notifications, preferences
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
    localStorage.clear()
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

    it('realtimeNotifications 默认为空数组', () => {
      const store = useNotificationStore()
      expect(store.realtimeNotifications).toEqual([])
    })

    it('realtimeUnreadCount 默认为 0', () => {
      const store = useNotificationStore()
      expect(store.realtimeUnreadCount).toBe(0)
    })

    it('totalUnreadCount 默认为 0', () => {
      const store = useNotificationStore()
      expect(store.totalUnreadCount).toBe(0)
    })

    it('categoryFilter 默认为 all', () => {
      const store = useNotificationStore()
      expect(store.categoryFilter).toBe('all')
    })

    it('preferences 默认所有类别启用', () => {
      const store = useNotificationStore()
      expect(store.preferences.agentStatus).toBe(true)
      expect(store.preferences.workflow).toBe(true)
      expect(store.preferences.alerts).toBe(true)
      expect(store.preferences.approvals).toBe(true)
      expect(store.preferences.tenant).toBe(true)
      expect(store.preferences.system).toBe(true)
      expect(store.preferences.collaboration).toBe(true)
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

    it('totalUnreadCount 合并 REST 和实时通知未读数', () => {
      const store = useNotificationStore()
      store.unreadCount = 3
      store.realtimeUnreadCount = 2
      expect(store.totalUnreadCount).toBe(5)
    })
  })

  // ---------- REST Actions ----------

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

  // ---------- Enhanced Real-time Notification Actions ----------

  describe('addRealtimeNotification', () => {
    it('添加实时通知并增加未读数', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-1',
        title: 'Test',
        content: 'Test content',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
        eventType: 'SYSTEM_NOTIFICATION',
      })

      expect(store.realtimeNotifications).toHaveLength(1)
      expect(store.realtimeUnreadCount).toBe(1)
    })

    it('已读通知不增加未读数', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-2',
        title: 'Test',
        content: 'Test content',
        category: 'info',
        priority: 'normal',
        read: true,
        timestamp: '2024-01-01T00:00:00Z',
      })

      expect(store.realtimeUnreadCount).toBe(0)
    })

    it('根据偏好过滤通知', () => {
      const store = useNotificationStore()
      store.updatePreferences({ alerts: false })

      store.addRealtimeNotification({
        id: 'rt-3',
        title: 'Alert',
        content: 'Alert content',
        category: 'warning',
        priority: 'high',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
        eventType: 'ALERT_FIRED',
      })

      expect(store.realtimeNotifications).toHaveLength(0)
    })

    it('超过200条时自动清理旧通知', () => {
      const store = useNotificationStore()
      for (let i = 0; i < 210; i++) {
        store.addRealtimeNotification({
          id: `rt-${i}`,
          title: `Notification ${i}`,
          content: `Content ${i}`,
          category: 'info',
          priority: 'normal',
          read: false,
          timestamp: new Date(Date.now() - i * 1000).toISOString(),
        })
      }

      expect(store.realtimeNotifications.length).toBeLessThanOrEqual(200)
    })
  })

  describe('markRealtimeAsRead', () => {
    it('标记实时通知为已读', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-4',
        title: 'Test',
        content: 'Test',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })

      store.markRealtimeAsRead('rt-4')
      expect(store.realtimeUnreadCount).toBe(0)
    })
  })

  describe('markAllRealtimeAsRead', () => {
    it('标记所有实时通知为已读', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-5',
        title: 'Test 1',
        content: 'Test',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })
      store.addRealtimeNotification({
        id: 'rt-6',
        title: 'Test 2',
        content: 'Test',
        category: 'warning',
        priority: 'high',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })

      store.markAllRealtimeAsRead()
      expect(store.realtimeUnreadCount).toBe(0)
      expect(store.realtimeNotifications.every((n) => n.read)).toBe(true)
    })
  })

  describe('removeRealtimeNotification', () => {
    it('移除实时通知并更新未读数', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-7',
        title: 'Test',
        content: 'Test',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })

      store.removeRealtimeNotification('rt-7')
      expect(store.realtimeNotifications).toHaveLength(0)
      expect(store.realtimeUnreadCount).toBe(0)
    })
  })

  describe('categoryFilter', () => {
    it('按类别过滤实时通知', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-8',
        title: 'Info',
        content: 'Info',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })
      store.addRealtimeNotification({
        id: 'rt-9',
        title: 'Warning',
        content: 'Warning',
        category: 'warning',
        priority: 'high',
        read: false,
        timestamp: '2024-01-01T00:00:01Z',
      })

      store.setCategoryFilter('warning')
      expect(store.filteredRealtimeNotifications).toHaveLength(1)
      expect(store.filteredRealtimeNotifications[0].id).toBe('rt-9')
    })
  })

  describe('preferences', () => {
    it('更新偏好设置', () => {
      const store = useNotificationStore()
      store.updatePreferences({ alerts: false, collaboration: false })

      expect(store.preferences.alerts).toBe(false)
      expect(store.preferences.collaboration).toBe(false)
      expect(store.preferences.agentStatus).toBe(true)
    })

    it('偏好设置持久化到 localStorage', () => {
      const store = useNotificationStore()
      store.updatePreferences({ system: false })

      const stored = JSON.parse(localStorage.getItem('notificationPreferences')!)
      expect(stored.system).toBe(false)
    })

    it('从 localStorage 加载偏好设置', () => {
      localStorage.setItem('notificationPreferences', JSON.stringify({ alerts: false, workflow: false }))

      const store = useNotificationStore()
      expect(store.preferences.alerts).toBe(false)
      expect(store.preferences.workflow).toBe(false)
      expect(store.preferences.agentStatus).toBe(true) // default
    })
  })

  describe('shouldShowNotification', () => {
    it('根据事件类型判断是否显示通知', () => {
      const store = useNotificationStore()
      store.updatePreferences({ alerts: false })

      expect(store.shouldShowNotification('ALERT_FIRED')).toBe(false)
      expect(store.shouldShowNotification('AGENT_STATUS_CHANGED')).toBe(true)
      expect(store.shouldShowNotification('AGENT_STATUS_CHANGE')).toBe(true)
      expect(store.shouldShowNotification(undefined)).toBe(true)
    })
  })

  describe('$reset', () => {
    it('重置所有状态', () => {
      const store = useNotificationStore()
      store.addRealtimeNotification({
        id: 'rt-10',
        title: 'Test',
        content: 'Test',
        category: 'info',
        priority: 'normal',
        read: false,
        timestamp: '2024-01-01T00:00:00Z',
      })
      store.setCategoryFilter('warning')
      store.updatePreferences({ alerts: false })

      store.$reset()

      expect(store.realtimeNotifications).toEqual([])
      expect(store.realtimeUnreadCount).toBe(0)
      expect(store.categoryFilter).toBe('all')
      expect(store.preferences.alerts).toBe(true) // reset to default
    })
  })
})
