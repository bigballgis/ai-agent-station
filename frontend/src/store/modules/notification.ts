import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, type Notification } from '@/api/notification'
import { logger } from '@/utils/logger'
import { requireStoreReady } from '../utils'

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

  // Getters
  const unreadNotifications = computed<Notification[]>(() =>
    notifications.value.filter((n) => !n.read)
  )

  const sortedNotifications = computed<Notification[]>(() =>
    [...notifications.value].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
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

  function $reset(): void {
    notifications.value = []
    unreadCount.value = 0
    wsConnected.value = false
    disconnectWebSocket()
  }

  return {
    // State
    notifications,
    unreadCount,
    wsConnected,
    // Getters
    unreadNotifications,
    sortedNotifications,
    // Actions
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    connectWebSocket,
    disconnectWebSocket,
    $reset,
  }
})
