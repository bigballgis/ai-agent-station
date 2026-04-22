<template>
  <div class="console-panel" :class="{ collapsed }">
    <!-- Header -->
    <div class="console-header" @click="$emit('toggle-collapse')">
      <div class="console-header-left">
        <svg
          class="console-arrow"
          :class="{ collapsed }"
          fill="currentColor"
          viewBox="0 0 20 20"
          width="12"
          height="12"
        >
          <path
            fill-rule="evenodd"
            d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
            clip-rule="evenodd"
          />
        </svg>
        <span class="console-title">{{ t('designer.console.title') }}</span>
        <span v-if="logs.length > 0" class="console-badge">{{ logs.length }}</span>
      </div>
      <button
        v-if="logs.length > 0"
        class="console-clear-btn"
        @click.stop="$emit('clear')"
        :title="t('designer.console.clear')"
      >
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="12" height="12">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        {{ t('designer.console.clear') }}
      </button>
    </div>

    <!-- Log Entries -->
    <div v-show="!collapsed" ref="logContainerRef" class="console-body">
      <div v-if="logs.length === 0" class="console-empty">
        {{ t('designer.console.empty') }}
      </div>
      <div
        v-for="log in logs"
        :key="log.time + log.message"
        class="log-entry"
        :class="`log-${log.level}`"
      >
        <span class="log-time">{{ log.time }}</span>
        <span class="log-level" :class="`level-${log.level}`">{{ levelLabel(log.level) }}</span>
        <span class="log-message">{{ log.message }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ConsoleLog } from '@/composables/designer/types'

const { t } = useI18n()

const props = defineProps<{
  logs: ConsoleLog[]
  collapsed?: boolean
}>()

defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'clear'): void
}>()

const logContainerRef = ref<HTMLElement>()

// Auto-scroll to bottom when new logs arrive
watch(
  () => props.logs.length,
  async () => {
    if (props.collapsed) return
    await nextTick()
    scrollToBottom()
  }
)

function scrollToBottom() {
  const el = logContainerRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function levelLabel(level: string): string {
  const map: Record<string, string> = {
    info: 'INFO',
    warn: 'WARN',
    error: 'ERROR',
    success: 'OK',
  }
  return map[level] ?? level.toUpperCase()
}
</script>

<style scoped>
.console-panel {
  background: #16162a;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* Header */
.console-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.console-header:hover {
  background: rgba(255, 255, 255, 0.02);
}

.console-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.console-arrow {
  color: rgba(226, 232, 240, 0.3);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.console-arrow.collapsed {
  transform: rotate(-90deg);
}

.console-title {
  font-size: 12px;
  font-weight: 600;
  color: rgba(226, 232, 240, 0.6);
}

.console-badge {
  font-size: 10px;
  font-weight: 600;
  color: #e2e8f0;
  background: rgba(24, 144, 255, 0.3);
  padding: 1px 6px;
  border-radius: 8px;
  min-width: 18px;
  text-align: center;
}

.console-clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: rgba(226, 232, 240, 0.3);
  font-size: 11px;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.console-clear-btn:hover {
  color: rgba(226, 232, 240, 0.6);
  background: rgba(255, 255, 255, 0.04);
}

/* Body */
.console-body {
  max-height: 200px;
  overflow-y: auto;
  padding: 6px 0;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.console-empty {
  padding: 16px;
  text-align: center;
  color: rgba(226, 232, 240, 0.2);
  font-size: 12px;
  font-family: inherit;
}

/* Log Entry */
.log-entry {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 2px 12px;
  transition: background 0.1s ease;
}

.log-entry:hover {
  background: rgba(255, 255, 255, 0.02);
}

.log-time {
  color: rgba(226, 232, 240, 0.25);
  flex-shrink: 0;
  font-size: 11px;
}

.log-level {
  font-size: 9px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  flex-shrink: 0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  line-height: 1.5;
}

.level-info {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.12);
}

.level-warn {
  color: #faad14;
  background: rgba(250, 173, 20, 0.12);
}

.level-error {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.12);
}

.level-success {
  color: #52c41a;
  background: rgba(82, 196, 26, 0.12);
}

.log-message {
  color: rgba(226, 232, 240, 0.7);
  word-break: break-all;
  flex: 1;
  min-width: 0;
}

/* Level-specific message colors */
.log-info .log-message {
  color: rgba(226, 232, 240, 0.7);
}

.log-warn .log-message {
  color: rgba(250, 173, 20, 0.8);
}

.log-error .log-message {
  color: rgba(255, 77, 79, 0.85);
}

.log-success .log-message {
  color: rgba(82, 196, 26, 0.85);
}

/* Scrollbar */
.console-body::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.console-body::-webkit-scrollbar-track {
  background: transparent;
}

.console-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
}

.console-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.15);
}
</style>
