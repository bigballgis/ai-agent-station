import { ref, onUnmounted, type Ref } from 'vue'

// ==================== Type Definitions ====================

/** Typed WebSocket event types matching backend WebSocketEventDTO.EventType */
export type WsEventType =
  | 'AGENT_STATUS_CHANGED'
  | 'WORKFLOW_STATUS_CHANGED'
  | 'ALERT_FIRED'
  | 'APPROVAL_PENDING'
  | 'SYSTEM_NOTIFICATION'

/** Typed WebSocket event DTO matching backend WebSocketEventDTO */
export interface WsEvent {
  eventType: WsEventType
  title: string
  content: string
  timestamp: string
  payload: Record<string, unknown>
}

/** Legacy WebSocket message (backward compatible with existing WebSocketMessage) */
export interface WsMessage {
  type: string
  title: string
  content: string
  timestamp: string
  data?: Record<string, unknown>
  level?: string
}

/** Callback type for event listeners */
type EventCallback = (event: WsEvent) => void

/** Callback type for raw message listeners */
type RawMessageCallback = (data: WsMessage | WsEvent) => void

// ==================== Composable ====================

const MAX_RECONNECT_DELAY = 30000
const INITIAL_RECONNECT_DELAY = 1000
const HEARTBEAT_INTERVAL = 30000
const MAX_OFFLINE_QUEUE_SIZE = 100

/**
 * WebSocket composable with auto-connect, exponential backoff reconnect,
 * event listener registration by type, and offline message queue.
 *
 * @param getToken - function that returns the current auth token
 */
export function useWebSocket(_getToken: () => string | null) {
  const connected: Ref<boolean> = ref(false)
  const reconnecting: Ref<boolean> = ref(false)

  let ws: WebSocket | null = null
  let reconnectAttempts = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let manualClose = false

  // Event listeners by type
  const eventListeners = new Map<WsEventType | '*', Set<EventCallback>>()
  // Raw message listeners
  const rawListeners = new Set<RawMessageCallback>()
  // Offline message queue
  const offlineQueue: WsEvent[] = []

  // ==================== Connection Management ====================

  /**
   * Build WebSocket URL with userId and tenantId query params.
   */
  function buildWsUrl(): string {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = location.host
    const base = `${protocol}//${host}/ws/notifications`

    // Extract userId and tenantId from localStorage or token
    const params = new URLSearchParams()
    try {
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        const parsed = JSON.parse(userInfo)
        if (parsed.id) params.set('userId', String(parsed.id))
        if (parsed.tenantId) params.set('tenantId', String(parsed.tenantId))
      }
    } catch {
      // ignore parse errors
    }

    const queryString = params.toString()
    return queryString ? `${base}?${queryString}` : base
  }

  /**
   * Connect to WebSocket server.
   */
  function connect(): void {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    manualClose = false
    const url = buildWsUrl()

    try {
      ws = new WebSocket(url)
    } catch (e) {
      console.warn('[WebSocket] Failed to create connection:', e)
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      connected.value = true
      reconnecting.value = false
      reconnectAttempts = 0
      startHeartbeat()

      // Flush offline queue
      flushOfflineQueue()
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        // Handle pong heartbeat response
        if (data === 'pong' || data.type === 'pong') {
          return
        }

        // Determine if this is a typed event (new format) or legacy message
        if (data.eventType) {
          const wsEvent = data as WsEvent
          dispatchEvent(wsEvent)
        }

        // Also notify raw message listeners for backward compatibility
        rawListeners.forEach((cb) => {
          try {
            cb(data)
          } catch (e) {
            console.warn('[WebSocket] Raw listener error:', e)
          }
        })
      } catch {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      connected.value = false
      stopHeartbeat()
      if (!manualClose) {
        scheduleReconnect()
      }
    }

    ws.onerror = () => {
      connected.value = false
      if (ws) {
        ws.close()
      }
    }
  }

  /**
   * Disconnect from WebSocket server.
   */
  function disconnect(): void {
    manualClose = true
    stopHeartbeat()
    clearReconnectTimer()
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
    reconnecting.value = false
    reconnectAttempts = 0
    eventListeners.clear()
    rawListeners.clear()
    offlineQueue.length = 0
  }

  // ==================== Reconnection with Exponential Backoff ====================

  /**
   * Calculate reconnect delay: 1s, 2s, 4s, 8s, ... max 30s
   */
  function getReconnectDelay(): number {
    const delay = INITIAL_RECONNECT_DELAY * Math.pow(2, reconnectAttempts)
    return Math.min(delay, MAX_RECONNECT_DELAY)
  }

  /**
   * Schedule a reconnection attempt.
   */
  function scheduleReconnect(): void {
    if (manualClose) return
    if (reconnectTimer) return

    reconnecting.value = true
    const delay = getReconnectDelay()
    reconnectAttempts++

    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${reconnectAttempts})`)

    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, delay)
  }

  function clearReconnectTimer(): void {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  // ==================== Heartbeat ====================

  function startHeartbeat(): void {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send('ping')
      }
    }, HEARTBEAT_INTERVAL)
  }

  function stopHeartbeat(): void {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  // ==================== Event Dispatch ====================

  /**
   * Dispatch a typed event to registered listeners.
   */
  function dispatchEvent(event: WsEvent): void {
    // If offline, queue the event
    if (!connected.value) {
      if (offlineQueue.length < MAX_OFFLINE_QUEUE_SIZE) {
        offlineQueue.push(event)
      }
      return
    }

    // Notify type-specific listeners
    const specificListeners = eventListeners.get(event.eventType)
    if (specificListeners) {
      specificListeners.forEach((cb) => {
        try {
          cb(event)
        } catch (e) {
          console.warn(`[WebSocket] Listener error for ${event.eventType}:`, e)
        }
      })
    }

    // Notify wildcard listeners
    const wildcardListeners = eventListeners.get('*')
    if (wildcardListeners) {
      wildcardListeners.forEach((cb) => {
        try {
          cb(event)
        } catch (e) {
          console.warn('[WebSocket] Wildcard listener error:', e)
        }
      })
    }
  }

  /**
   * Flush offline message queue when reconnected.
   */
  function flushOfflineQueue(): void {
    if (offlineQueue.length === 0) return

    console.log(`[WebSocket] Flushing ${offlineQueue.length} queued events`)
    const events = [...offlineQueue]
    offlineQueue.length = 0

    events.forEach((event) => {
      dispatchEvent(event)
    })
  }

  // ==================== Public API ====================

  /**
   * Register an event listener for a specific event type.
   * Returns an unsubscribe function.
   */
  function on(type: WsEventType | '*', callback: EventCallback): () => void {
    if (!eventListeners.has(type)) {
      eventListeners.set(type, new Set())
    }
    eventListeners.get(type)!.add(callback)

    return () => {
      eventListeners.get(type)?.delete(callback)
    }
  }

  /**
   * Register a one-time event listener that auto-unsubscribes after first call.
   */
  function once(type: WsEventType | '*', callback: EventCallback): () => void {
    const wrapper: EventCallback = (event) => {
      unsubscribe()
      callback(event)
    }
    const unsubscribe = on(type, wrapper)
    return unsubscribe
  }

  /**
   * Remove all listeners for a specific event type.
   */
  function off(type: WsEventType | '*'): void {
    eventListeners.delete(type)
  }

  /**
   * Register a raw message listener (receives all messages).
   * Returns an unsubscribe function.
   */
  function onRawMessage(callback: RawMessageCallback): () => void {
    rawListeners.add(callback)
    return () => {
      rawListeners.delete(callback)
    }
  }

  /**
   * Get the current offline queue size.
   */
  function getOfflineQueueSize(): number {
    return offlineQueue.length
  }

  // Auto-cleanup on component unmount
  onUnmounted(() => {
    disconnect()
  })

  return {
    // State
    connected,
    reconnecting,
    // Connection
    connect,
    disconnect,
    // Event listeners
    on,
    once,
    off,
    onRawMessage,
    // Utilities
    getOfflineQueueSize,
  }
}
