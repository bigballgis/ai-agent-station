<template>
  <a-tooltip
    :title="tooltipText"
    :mouse-enter-delay="0.3"
    placement="top"
  >
    <button
      class="connection-status"
      :class="[
        `cs-${connectionState}`,
        { 'cs-clickable': connectionState !== 'connected' },
      ]"
      :aria-label="ariaLabel"
      @click="handleClick"
    >
      <!-- Connection state indicator dot -->
      <span class="cs-dot" :class="`cs-dot-${connectionState}`">
        <span v-if="connectionState === 'connecting'" class="cs-dot-pulse" />
      </span>

      <!-- Latency indicator (only when connected) -->
      <span v-if="connectionState === 'connected' && latency > 0" class="cs-latency">
        <span class="cs-latency-bar" :class="`cs-latency-${quality}`" />
        <span class="cs-latency-text">{{ latency }}ms</span>
      </span>

      <!-- State text -->
      <span class="cs-text">{{ stateText }}</span>
    </button>
  </a-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { WsConnectionState, WsConnectionQuality } from '@/composables/useWebSocket'

/**
 * ConnectionStatus component
 * Displays WebSocket connection state, latency, and quality.
 * Click to manually reconnect when disconnected.
 */

interface Props {
  /** Current WebSocket connection state */
  connectionState: WsConnectionState
  /** Current latency in milliseconds */
  latency: number
  /** Connection quality level */
  quality: WsConnectionQuality
  /** Whether the connection is currently reconnecting */
  reconnecting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  reconnecting: false,
})

const emit = defineEmits<{
  (e: 'reconnect'): void
}>()

const { t } = useI18n()

/** Effective connection state (treat reconnecting as connecting) */
const effectiveState = computed(() => {
  if (props.reconnecting) return 'connecting' as const
  return props.connectionState
})

/** Map the effective state to the template */
const connectionState = computed(() => effectiveState.value)

/** State display text */
const stateText = computed(() => {
  switch (effectiveState.value) {
    case 'connected':
      return t('common.wsConnected')
    case 'connecting':
      return t('common.wsConnecting')
    case 'disconnecting':
      return t('common.wsDisconnected')
    case 'disconnected':
      return t('common.wsDisconnected')
    default:
      return ''
  }
})

/** Quality label */
const quality = computed(() => props.quality)

/** Tooltip text */
const tooltipText = computed(() => {
  if (effectiveState.value === 'connected') {
    if (props.latency > 0) {
      const qualityLabel = t(`common.wsQuality${props.quality.charAt(0).toUpperCase()}${props.quality.slice(1)}` as string)
      return `${t('common.wsLatency', { ms: props.latency })} (${qualityLabel})`
    }
    return t('common.wsConnected')
  }
  if (effectiveState.value === 'connecting') {
    return t('common.wsReconnecting')
  }
  return t('common.wsReconnect')
})

/** Aria label for accessibility */
const ariaLabel = computed(() => {
  if (effectiveState.value === 'connected') {
    return `${t('common.wsConnected')} - ${t('common.wsLatency', { ms: props.latency })}`
  }
  return `${stateText.value} - ${t('common.wsReconnect')}`
})

/** Handle click - reconnect when not connected */
function handleClick(): void {
  if (effectiveState.value !== 'connected') {
    emit('reconnect')
  }
}
</script>

<style scoped>
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 6px;
  border: none;
  background: none;
  cursor: default;
  transition: all 0.2s;
  font-size: 11px;
  color: #737373;
}

.cs-clickable {
  cursor: pointer;
}

.cs-clickable:hover {
  background: rgba(239, 68, 68, 0.08);
}

/* Connection dot */
.cs-dot {
  position: relative;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cs-dot-connected {
  background-color: #22c55e;
}

.cs-dot-connecting {
  background-color: #f59e0b;
}

.cs-dot-disconnecting {
  background-color: #ef4444;
}

.cs-dot-disconnected {
  background-color: #ef4444;
}

.cs-dot-pulse {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background-color: inherit;
  opacity: 0;
  animation: cs-pulse 1.5s ease-in-out infinite;
}

@keyframes cs-pulse {
  0% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.6);
    opacity: 0;
  }
  100% {
    transform: scale(0.8);
    opacity: 0;
  }
}

/* Latency indicator */
.cs-latency {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cs-latency-bar {
  width: 16px;
  height: 3px;
  border-radius: 2px;
  background-color: #d4d4d4;
  overflow: hidden;
  position: relative;
}

.cs-latency-bar::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 2px;
  transition: width 0.3s, background-color 0.3s;
}

.cs-latency-good::after {
  width: 100%;
  background-color: #22c55e;
}

.cs-latency-medium::after {
  width: 60%;
  background-color: #f59e0b;
}

.cs-latency-poor::after {
  width: 30%;
  background-color: #ef4444;
}

.cs-latency-unknown::after {
  width: 0%;
  background-color: #d4d4d4;
}

.cs-latency-text {
  font-variant-numeric: tabular-nums;
  min-width: 28px;
}

/* State text */
.cs-text {
  white-space: nowrap;
}

/* Dark mode */
:global(.dark) .cs-clickable:hover {
  background: rgba(239, 68, 68, 0.12);
}

:global(.dark) .cs-text {
  color: #a3a3a3;
}

:global(.dark) .cs-latency-text {
  color: #a3a3a3;
}

:global(.dark) .cs-latency-bar {
  background-color: #404040;
}
</style>
