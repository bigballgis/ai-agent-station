import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, type Notification } from '@/api/notification'
import { logger } from '@/utils/logger'
import { requireStoreReady } from '../utils'

// ==================== Notification Enhancement Types ====================

/** Notification category */
export type NotificationCategory = 'info' | 'warning' | 'error' | 'success'

/** Notification priority levels */
export type NotificationPriority = 'low' | 'normal' | 'high' | 'urgent'

/** Enhanced notification item for the real-time notification center */
export interface RealtimeNotification {
  /** Unique ID (from backend or generated client-side) */
  id: string | number
  /** Notification title */
  title: string
  /** Notification content/message */
  content: string
  /** Notification category */
  category: NotificationCategory
  /** Notification priority */
  priority: NotificationPriority
  /** Whether the notification has been read */
  read: boolean
  /** Source event type that generated this notification */
  eventType?: string
  /** Timestamp (ISO string) */
  timestamp: string
  /** Optional link to navigate to on click */
  link?: string
  /** Optional payload data */
  payload?: Record<string, unknown>
}

/** User notification preferences */
export interface NotificationPreferences {
  /** Whether to receive agent status notifications */
  agentStatus: boolean
  /** Whether to receive workflow notifications */
  workflow: boolean
  /** Whether to receive alert notifications */
  alerts: boolean
  /** Whether to receive approval notifications */
  approvals: boolean
  /** Whether to receive tenant notifications */
  tenant: boolean
  /** Whether to receive system announcements */
  system: boolean
  /** Whether to receive collaboration events */
  collaboration: boolean
}

/** Default notification preferences */
const DEFAULT_PREFERENCES: NotificationPreferences = {
  agentStatus: true,
  workflow: true,
  alerts: true,
  approvals: true,
  tenant: true,
  system: true,
  collaboration: true,
}

/** Category filter options */
export type CategoryFilter = 'all' | NotificationCategory

export const useNotificationStore = defineStore('notification', () => {
  requireStoreReady('notification')

  // State
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const wsConnected = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT_DELAY = 60000

  // Enhanced real-time notification state
  const realtimeNotifications = ref<RealtimeNotification[]>([])
  const realtimeUnreadCount = ref(0)
  const preferences = ref<NotificationPreferences>({ ...DEFAULT_PREFERENCES })
  const categoryFilter = ref<CategoryFilter>('all')

  // Getters
  const unreadNotifications = computed<Notification[]>(() =>
    notifications.value.filter((n) => !n.read)
  )

  const sortedNotifications = computed<Notification[]>(() =>
    [...notifications.value].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  )

  /** Filtered real-time notifications based on category filter */
  const filteredRealtimeNotifications = computed<RealtimeNotification[]>(() => {
    const filtered = categoryFilter.value === 'all'
      ? realtimeNotifications.value
      : realtimeNotifications.value.filter((n) => n.category === categoryFilter.value)
    return [...filtered].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    )
  })

  /** Unread real-time notifications */
  const unreadRealtimeNotifications = computed<RealtimeNotification[]>(() =>
    realtimeNotifications.value.filter((n) => !n.read)
  )

  /** Total unread count (REST + real-time) */
  const totalUnreadCount = computed<number>(() =>
    unreadCount.value + realtimeUnreadCount.value
  )

  // Actions - delegate REST API calls to api/notification.ts
  async function fetchNotifications(params?: { page?: number; size?: number }): Promise<unknown> {
    try {
      const res = await notificationApi.getNotifications(params)
      // 兼容分页和非分页两种返回格式
      const data = res.data
      if (Array.isArray(data)) {
        notifications.value = data
      } else {
        notifications.value = (data as { records?: Notification[] })?.records || []
      }
      unreadCount.value = notifications.value.filter((n) => !n.read).length
      return res.data
    } catch (error) {
      logger.debug('Fetch notifications failed:', error)
      return undefined
    }
  }

  async function markAsRead(id: number): Promise<void> {
    await notificationApi.markAsRead(id)
    const notification = notifications.value.find((n) => n.id === id)
    if (notification && !notification.read) {
      notification.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function markAllAsRead(): Promise<void> {
    await notificationApi.markAllAsRead()
    notifications.value.forEach((n) => {
      n.read = true
    })
    unreadCount.value = 0
  }

  function connectWebSocket(token: string): void {
    if (ws && ws.readyState === WebSocket.OPEN) return

    const wsUrl = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/notifications`

    ws = new WebSocket(wsUrl, [token])

    ws.onopen = () => {
      wsConnected.value = true
      reconnectAttempts = 0
      startHeartbeat()
    }

    ws.onmessage = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data as string)
        if (data.type === 'notification') {
          notifications.value.unshift(data.payload)
          if (!data.payload.read) {
            unreadCount.value++
          }
        } else if (data.type === 'pong') {
          // heartbeat response
        }
      } catch {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      wsConnected.value = false
      stopHeartbeat()
      scheduleReconnect(token)
    }

    ws.onerror = () => {
      wsConnected.value = false
      ws?.close()
    }
  }

  function disconnectWebSocket(): void {
    stopHeartbeat()
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
    wsConnected.value = false
  }

  function startHeartbeat(): void {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
  }

  function stopHeartbeat(): void {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function getReconnectDelay(): number {
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY)
    reconnectAttempts++
    return delay
  }

  function scheduleReconnect(token: string): void {
    if (reconnectTimer) return
    const delay = getReconnectDelay()
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connectWebSocket(token)
    }, delay)
  }

  // ==================== Enhanced Real-time Notification Actions ====================

  /**
   * Add a real-time notification from a WebSocket event.
   * Respects user notification preferences.
   */
  function addRealtimeNotification(notification: RealtimeNotification): void {
    // Check user preferences before adding
    if (!shouldShowNotification(notification.eventType)) {
      return
    }

    realtimeNotifications.value.unshift(notification)
    if (!notification.read) {
      realtimeUnreadCount.value++
    }

    // Keep only the most recent 200 notifications to prevent memory leaks
    if (realtimeNotifications.value.length > 200) {
      const removed = realtimeNotifications.value.pop()
      if (removed && !removed.read) {
        realtimeUnreadCount.value = Math.max(0, realtimeUnreadCount.value - 1)
      }
    }
  }

  /**
   * Mark a real-time notification as read by ID.
   */
  function markRealtimeAsRead(id: string | number): void {
    const notification = realtimeNotifications.value.find((n) => n.id === id)
    if (notification && !notification.read) {
      notification.read = true
      realtimeUnreadCount.value = Math.max(0, realtimeUnreadCount.value - 1)
    }
  }

  /**
   * Mark all real-time notifications as read.
   */
  function markAllRealtimeAsRead(): void {
    realtimeNotifications.value.forEach((n) => {
      n.read = true
    })
    realtimeUnreadCount.value = 0
  }

  /**
   * Remove a real-time notification by ID.
   */
  function removeRealtimeNotification(id: string | number): void {
    const index = realtimeNotifications.value.findIndex((n) => n.id === id)
    if (index !== -1) {
      const removed = realtimeNotifications.value[index]
      if (removed && !removed.read) {
        realtimeUnreadCount.value = Math.max(0, realtimeUnreadCount.value - 1)
      }
      realtimeNotifications.value.splice(index, 1)
    }
  }

  /**
   * Clear all real-time notifications.
   */
  function clearRealtimeNotifications(): void {
    realtimeNotifications.value = []
    realtimeUnreadCount.value = 0
  }

  /**
   * Set the category filter for the notification center.
   */
  function setCategoryFilter(filter: CategoryFilter): void {
    categoryFilter.value = filter
  }

  /**
   * Update notification preferences.
   */
  function updatePreferences(newPrefs: Partial<NotificationPreferences>): void {
    preferences.value = { ...preferences.value, ...newPrefs }
    // Persist to localStorage
    try {
      localStorage.setItem('notificationPreferences', JSON.stringify(preferences.value))
    } catch {
      // ignore storage errors
    }
  }

  /**
   * Load notification preferences from localStorage.
   */
  function loadPreferences(): void {
    try {
      const stored = localStorage.getItem('notificationPreferences')
      if (stored) {
        preferences.value = { ...DEFAULT_PREFERENCES, ...JSON.parse(stored) }
      }
    } catch {
      // ignore parse errors
    }
  }

  /**
   * Check if a notification should be shown based on user preferences.
   */
  function shouldShowNotification(eventType?: string): boolean {
    if (!eventType) return true
    switch (eventType) {
      case 'AGENT_STATUS_CHANGED':
      case 'AGENT_STATUS_CHANGE':
        return preferences.value.agentStatus
      case 'WORKFLOW_STATUS_CHANGED':
      case 'WORKFLOW_PROGRESS':
        return preferences.value.workflow
      case 'ALERT_FIRED':
        return preferences.value.alerts
      case 'APPROVAL_PENDING':
        return preferences.value.approvals
      case 'TENANT_NOTIFICATION':
        return preferences.value.tenant
      case 'SYSTEM_NOTIFICATION':
      case 'SYSTEM_ANNOUNCEMENT':
        return preferences.value.system
      case 'COLLABORATION':
        return preferences.value.collaboration
      default:
        return true
    }
  }

  function $reset(): void {
    notifications.value = []
    unreadCount.value = 0
    wsConnected.value = false
    disconnectWebSocket()
    realtimeNotifications.value = []
    realtimeUnreadCount.value = 0
    categoryFilter.value = 'all'
    preferences.value = { ...DEFAULT_PREFERENCES }
  }

  // Load preferences on store initialization
  loadPreferences()

  return {
    // State
    notifications,
    unreadCount,
    wsConnected,
    // Enhanced state
    realtimeNotifications,
    realtimeUnreadCount,
    preferences,
    categoryFilter,
    // Getters
    unreadNotifications,
    sortedNotifications,
    filteredRealtimeNotifications,
    unreadRealtimeNotifications,
    totalUnreadCount,
    // Actions
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    connectWebSocket,
    disconnectWebSocket,
    // Enhanced actions
    addRealtimeNotification,
    markRealtimeAsRead,
    markAllRealtimeAsRead,
    removeRealtimeNotification,
    clearRealtimeNotifications,
    setCategoryFilter,
    updatePreferences,
    loadPreferences,
    shouldShowNotification,
    $reset,
  }
})
