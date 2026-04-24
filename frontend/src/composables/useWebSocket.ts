import { ref, onUnmounted, type Ref } from 'vue'

// ==================== Type Definitions ====================

/** Typed WebSocket event types matching backend WebSocketEventDTO.EventType */
export type WsEventType =
  | 'AGENT_STATUS_CHANGED'
  | 'WORKFLOW_STATUS_CHANGED'
  | 'ALERT_FIRED'
  | 'APPROVAL_PENDING'
  | 'SYSTEM_NOTIFICATION'
  | 'AGENT_STATUS_CHANGE'
  | 'WORKFLOW_PROGRESS'
  | 'TENANT_NOTIFICATION'
  | 'SYSTEM_ANNOUNCEMENT'
  | 'COLLABORATION'

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

// ==================== Event Payload Types ====================

/** Payload for AGENT_STATUS_CHANGED events */
export interface AgentStatusChangedPayload {
  agentName: string
  status: string
  [key: string]: unknown
}

/** Payload for AGENT_STATUS_CHANGE events (granular lifecycle) */
export interface AgentStatusChangePayload {
  agentId: string
  agentName: string
  status: 'running' | 'stopped' | 'error' | 'idle' | 'starting'
  detail?: string
  [key: string]: unknown
}

/** Payload for WORKFLOW_STATUS_CHANGED events */
export interface WorkflowStatusChangedPayload {
  workflowName: string
  instanceId: number
  status: string
  [key: string]: unknown
}

/** Payload for WORKFLOW_PROGRESS events */
export interface WorkflowProgressPayload {
  instanceId: number
  workflowName: string
  currentStep: number
  totalSteps: number
  percentage: number
  [key: string]: unknown
}

/** Payload for ALERT_FIRED events */
export interface AlertFiredPayload {
  ruleName: string
  severity: string
  [key: string]: unknown
}

/** Payload for APPROVAL_PENDING events */
export interface ApprovalPendingPayload {
  agentName: string
  approvalId: number
  submitter: string
  [key: string]: unknown
}

/** Payload for TENANT_NOTIFICATION events */
export interface TenantNotificationPayload {
  tenantName: string
  category: 'quota_warning' | 'quota_exceeded' | 'tenant_announcement' | 'plan_change'
  [key: string]: unknown
}

/** Payload for SYSTEM_ANNOUNCEMENT events */
export interface SystemAnnouncementPayload {
  severity: 'info' | 'warning' | 'critical'
  [key: string]: unknown
}

/** Payload for COLLABORATION events */
export interface CollaborationPayload {
  collaborationType: 'cursor_move' | 'editing' | 'presence' | 'selection'
  userId: number
  username: string
  [key: string]: unknown
}

/** Mapping from event type to its strongly-typed payload */
export interface WsEventPayloadMap {
  AGENT_STATUS_CHANGED: AgentStatusChangedPayload
  AGENT_STATUS_CHANGE: AgentStatusChangePayload
  WORKFLOW_STATUS_CHANGED: WorkflowStatusChangedPayload
  WORKFLOW_PROGRESS: WorkflowProgressPayload
  ALERT_FIRED: AlertFiredPayload
  APPROVAL_PENDING: ApprovalPendingPayload
  TENANT_NOTIFICATION: TenantNotificationPayload
  SYSTEM_ANNOUNCEMENT: SystemAnnouncementPayload
  COLLABORATION: CollaborationPayload
  SYSTEM_NOTIFICATION: Record<string, unknown>
}

/** Typed event with strongly-typed payload based on event type */
export type TypedWsEvent<T extends WsEventType> = WsEvent & {
  eventType: T
  payload: WsEventPayloadMap[T]
}

/** Callback type for typed event listeners */
type TypedEventCallback<T extends WsEventType> = (event: TypedWsEvent<T>) => void

/** Callback type for generic event listeners */
type EventCallback = (event: WsEvent) => void

/** Callback type for raw message listeners */
type RawMessageCallback = (data: WsMessage | WsEvent) => void

/** Connection state enum */
export type WsConnectionState = 'connecting' | 'connected' | 'disconnecting' | 'disconnected'

/** Connection quality based on latency */
export type WsConnectionQuality = 'good' | 'medium' | 'poor' | 'unknown'

// ==================== Composable ====================

const MAX_RECONNECT_DELAY = 30000
const INITIAL_RECONNECT_DELAY = 1000
const HEARTBEAT_INTERVAL = 30000
const MAX_OFFLINE_QUEUE_SIZE = 100
const MAX_MESSAGE_SIZE = 256 * 1024 // 256KB max message size
const LATENCY_SAMPLE_COUNT = 5

/**
 * WebSocket composable with auto-connect, exponential backoff reconnect,
 * event listener registration by type, and offline message queue.
 *
 * @param getToken - function that returns the current auth token
 */
export function useWebSocket(_getToken: () => string | null) {
  const connected: Ref<boolean> = ref(false)
  const reconnecting: Ref<boolean> = ref(false)
  const connectionState: Ref<WsConnectionState> = ref('disconnected')
  const latency: Ref<number> = ref(0)
  const connectionQuality: Ref<WsConnectionQuality> = ref('unknown')
  const compressionEnabled: Ref<boolean> = ref(false)

  let ws: WebSocket | null = null
  let reconnectAttempts = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let manualClose = false
  let lastPingTime = 0
  const latencySamples: number[] = []

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
   * Update connection quality based on recent latency samples.
   */
  function updateConnectionQuality(): void {
    if (latencySamples.length === 0) {
      connectionQuality.value = 'unknown'
      return
    }
    const avgLatency = latencySamples.reduce((a, b) => a + b, 0) / latencySamples.length
    latency.value = Math.round(avgLatency)

    if (avgLatency < 100) {
      connectionQuality.value = 'good'
    } else if (avgLatency < 500) {
      connectionQuality.value = 'medium'
    } else {
      connectionQuality.value = 'poor'
    }
  }

  /**
   * Connect to WebSocket server.
   */
  function connect(): void {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    manualClose = false
    connectionState.value = 'connecting'
    const url = buildWsUrl()

    try {
      ws = new WebSocket(url)
    } catch (e) {
      console.warn('[WebSocket] Failed to create connection:', e)
      connectionState.value = 'disconnected'
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      connected.value = true
      reconnecting.value = false
      connectionState.value = 'connected'
      reconnectAttempts = 0
      latencySamples.length = 0
      startHeartbeat()

      // Flush offline queue
      flushOfflineQueue()
    }

    ws.onmessage = (event) => {
      try {
        // Validate message size
        if (event.data && typeof event.data === 'string' && event.data.length > MAX_MESSAGE_SIZE) {
          console.warn(`[WebSocket] Message exceeds max size (${MAX_MESSAGE_SIZE} bytes), ignoring`)
          return
        }

        const data = JSON.parse(event.data)

        // Handle pong heartbeat response - measure latency
        if (data === 'pong' || data.type === 'pong') {
          if (lastPingTime > 0) {
            const roundTrip = Date.now() - lastPingTime
            latencySamples.push(roundTrip)
            if (latencySamples.length > LATENCY_SAMPLE_COUNT) {
              latencySamples.shift()
            }
            updateConnectionQuality()
          }
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
      connectionState.value = 'disconnected'
      stopHeartbeat()
      if (!manualClose) {
        scheduleReconnect()
      }
    }

    ws.onerror = () => {
      connected.value = false
      connectionState.value = 'disconnected'
      if (ws) {
        ws.close()
      }
    }
  }

  /**
   * Disconnect from WebSocket server.
   */
  function disconnect(): void {
    connectionState.value = 'disconnecting'
    manualClose = true
    stopHeartbeat()
    clearReconnectTimer()
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
    reconnecting.value = false
    connectionState.value = 'disconnected'
    reconnectAttempts = 0
    eventListeners.clear()
    rawListeners.clear()
    offlineQueue.length = 0
    latencySamples.length = 0
    latency.value = 0
    connectionQuality.value = 'unknown'
  }

  /**
   * Manually reconnect to WebSocket server.
   */
  function reconnect(): void {
    disconnect()
    // Use a small timeout to allow the disconnect state to settle
    setTimeout(() => {
      connect()
    }, 100)
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
    connectionState.value = 'connecting'
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
        lastPingTime = Date.now()
        ws.send('ping')
      }
    }, HEARTBEAT_INTERVAL)
  }

  function stopHeartbeat(): void {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    lastPingTime = 0
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
   * Register a typed event listener for a specific event type with strongly-typed payload.
   * Returns an unsubscribe function.
   */
  function onTyped<T extends WsEventType>(type: T, callback: TypedEventCallback<T>): () => void {
    const wrapper: EventCallback = (event) => {
      callback(event as TypedWsEvent<T>)
    }
    return on(type, wrapper)
  }

  /**
   * Register an event listener filtered by specific event types.
   * Only events matching the provided types will trigger the callback.
   * Returns an unsubscribe function.
   */
  function onFiltered(types: WsEventType[], callback: EventCallback): () => void {
    const typeSet = new Set(types)
    const wrapper: EventCallback = (event) => {
      if (typeSet.has(event.eventType)) {
        callback(event)
      }
    }
    return on('*', wrapper)
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

  /**
   * Send a message through the WebSocket connection.
   * Validates message size before sending.
   */
  function send(data: string | Record<string, unknown>): boolean {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      return false
    }

    const message = typeof data === 'string' ? data : JSON.stringify(data)

    // Validate message size
    if (message.length > MAX_MESSAGE_SIZE) {
      console.warn(`[WebSocket] Message exceeds max size (${MAX_MESSAGE_SIZE} bytes)`)
      return false
    }

    try {
      ws.send(message)
      return true
    } catch (e) {
      console.warn('[WebSocket] Failed to send message:', e)
      return false
    }
  }

  // Auto-cleanup on component unmount
  onUnmounted(() => {
    disconnect()
  })

  return {
    // State
    connected,
    reconnecting,
    connectionState,
    latency,
    connectionQuality,
    compressionEnabled,
    // Connection
    connect,
    disconnect,
    reconnect,
    // Event listeners
    on,
    onTyped,
    onFiltered,
    once,
    off,
    onRawMessage,
    // Messaging
    send,
    // Utilities
    getOfflineQueueSize,
  }
}
