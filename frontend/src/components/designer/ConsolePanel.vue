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
      <div class="console-header-right">
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
    </div>

    <!-- Tab Bar -->
    <div v-show="!collapsed" class="console-tabs">
      <button
        class="console-tab"
        :class="{ active: activeTab === 'console' }"
        @click.stop="activeTab = 'console'"
      >
        {{ t('designer.console.tabConsole') }}
      </button>
      <button
        class="console-tab"
        :class="{ active: activeTab === 'state' }"
        @click.stop="activeTab = 'state'"
      >
        {{ t('designer.console.tabState') }}
        <span v-if="flowStateKeys.length > 0" class="console-badge">{{ flowStateKeys.length }}</span>
      </button>
      <button
        v-if="isDebugMode"
        class="console-tab"
        :class="{ active: activeTab === 'debug' }"
        @click.stop="activeTab = 'debug'"
      >
        {{ t('designer.console.tabDebug') }}
        <span v-if="breakpointCount > 0" class="console-badge debug-badge">{{ breakpointCount }}</span>
      </button>
    </div>

    <!-- Console Tab Content -->
    <div v-show="!collapsed && activeTab === 'console'" ref="logContainerRef" class="console-body">
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

    <!-- Flow State Tab Content -->
    <div v-show="!collapsed && activeTab === 'state'" class="console-body state-body">
      <div v-if="flowStateKeys.length === 0" class="console-empty">
        {{ t('designer.console.stateEmpty') }}
      </div>
      <div
        v-for="key in flowStateKeys"
        :key="key"
        class="state-entry"
      >
        <span class="state-key">{{ key }}</span>
        <span class="state-separator">:</span>
        <span class="state-value" :title="String((flowState || {})[key])">{{ formatStateValue((flowState || {})[key]) }}</span>
      </div>
    </div>

    <!-- Debug Tab Content -->
    <div v-show="!collapsed && activeTab === 'debug'" class="console-body debug-body">
      <div v-if="!currentDebugInfo" class="console-empty">
        {{ t('designer.debug.noBreakpoint') }}
      </div>
      <template v-else>
        <!-- Current Node Info -->
        <div class="debug-section">
          <div class="debug-section-header">
            <span class="debug-node-label">{{ currentDebugInfo.label }}</span>
            <span class="debug-status-badge" :class="`status-${currentDebugInfo.status}`">
              {{ statusLabel(currentDebugInfo.status) }}
            </span>
          </div>
        </div>

        <!-- Input -->
        <div class="debug-section">
          <div class="debug-section-title">{{ t('designer.debug.input') }}</div>
          <pre class="debug-json">{{ formatJson(currentDebugInfo.input) }}</pre>
        </div>

        <!-- Output -->
        <div v-if="currentDebugInfo.output" class="debug-section">
          <div class="debug-section-title">{{ t('designer.debug.output') }}</div>
          <pre class="debug-json">{{ formatJson(currentDebugInfo.output) }}</pre>
        </div>

        <!-- Duration -->
        <div class="debug-section">
          <div class="debug-section-title">{{ t('designer.debug.duration') }}</div>
          <span class="debug-duration">{{ currentDebugInfo.duration }}ms</span>
        </div>

        <!-- Error -->
        <div v-if="currentDebugInfo.error" class="debug-section">
          <div class="debug-section-title">Error</div>
          <pre class="debug-json debug-error">{{ currentDebugInfo.error }}</pre>
        </div>
      </template>

      <!-- Breakpoints List -->
      <div v-if="breakpointCount > 0" class="debug-section debug-breakpoints-section">
        <div class="debug-section-title">{{ t('designer.debug.breakpoint') }} ({{ breakpointCount }})</div>
        <div
          v-for="[nodeId] in breakpoints"
          :key="nodeId"
          class="debug-breakpoint-entry"
        >
          <svg width="10" height="10" viewBox="0 0 12 12">
            <circle cx="6" cy="6" r="5" fill="#ef4444" />
          </svg>
          <span class="debug-bp-node-id">{{ nodeId }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ConsoleLog } from '@/composables/designer/types'
import type { NodeDebugInfo } from '@/composables/designer/useDebugMode'

const { t } = useI18n()

const props = defineProps<{
  logs: ConsoleLog[]
  collapsed?: boolean
  flowState?: Record<string, any>
  debugInfo?: Map<string, NodeDebugInfo>
  currentDebugNodeId?: string | null
  breakpoints?: Map<string, boolean>
  isDebugMode?: boolean
}>()

defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'clear'): void
}>()

const activeTab = ref<'console' | 'state' | 'debug'>('console')
const logContainerRef = ref<HTMLElement>()

// Flow state keys
const flowStateKeys = computed(() => {
  if (!props.flowState) return []
  return Object.keys(props.flowState as Record<string, unknown>)
})

// Breakpoint count
const breakpointCount = computed(() => {
  if (!props.breakpoints) return 0
  return Array.from(props.breakpoints.values()).filter(v => v).length
})

// Current debug info for the node being debugged
const currentDebugInfo = computed(() => {
  if (!props.currentDebugNodeId || !props.debugInfo) return null
  return props.debugInfo.get(props.currentDebugNodeId) || null
})

// Auto-switch to debug tab when debug starts
watch(() => props.isDebugMode, (val) => {
  if (val) {
    activeTab.value = 'debug'
  }
})

// Auto-switch to debug tab when paused on a node
watch(() => props.currentDebugNodeId, (val) => {
  if (val && props.isDebugMode) {
    activeTab.value = 'debug'
  }
})

// Format state value for display
function formatStateValue(value: any): string {
  if (value === null || value === undefined) return 'null'
  const str = String(value)
  if (str.length > 120) return str.substring(0, 120) + '...'
  return str
}

// Format JSON for display
function formatJson(data: any): string {
  if (data === null || data === undefined) return 'null'
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

// Status label
function statusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: t('designer.console.statusPending'),
    running: t('designer.console.statusRunning'),
    paused: t('designer.console.statusPaused'),
    completed: t('designer.console.statusCompleted'),
    failed: t('designer.console.statusFailed'),
    skipped: t('designer.console.statusSkipped'),
  }
  return map[status] ?? status
}

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

/* Tab Bar */
.console-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  flex-shrink: 0;
}

.console-tab {
  padding: 5px 14px;
  background: none;
  border: none;
  color: rgba(226, 232, 240, 0.35);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border-bottom: 2px solid transparent;
  display: flex;
  align-items: center;
  gap: 6px;
}

.console-tab:hover {
  color: rgba(226, 232, 240, 0.6);
}

.console-tab.active {
  color: rgba(226, 232, 240, 0.8);
  border-bottom-color: #6366f1;
}

/* Header Right */
.console-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Flow State Entries */
.state-body {
  padding: 8px 12px;
}

.state-entry {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 3px 0;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.state-entry:hover {
  background: rgba(255, 255, 255, 0.02);
}

.state-key {
  color: #818cf8;
  flex-shrink: 0;
  font-weight: 500;
}

.state-separator {
  color: rgba(226, 232, 240, 0.25);
  flex-shrink: 0;
}

.state-value {
  color: rgba(226, 232, 240, 0.7);
  word-break: break-all;
  flex: 1;
  min-width: 0;
}

/* Debug Tab */
.debug-body {
  padding: 8px 12px;
  max-height: 280px;
}

.debug-section {
  margin-bottom: 12px;
}

.debug-section:last-child {
  margin-bottom: 0;
}

.debug-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-node-label {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
}

.debug-status-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.debug-status-badge.status-pending {
  color: rgba(226, 232, 240, 0.5);
  background: rgba(255, 255, 255, 0.06);
}

.debug-status-badge.status-running {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.12);
}

.debug-status-badge.status-paused {
  color: #eab308;
  background: rgba(234, 179, 8, 0.15);
}

.debug-status-badge.status-completed {
  color: #52c41a;
  background: rgba(82, 196, 26, 0.12);
}

.debug-status-badge.status-failed {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.12);
}

.debug-status-badge.status-skipped {
  color: rgba(226, 232, 240, 0.4);
  background: rgba(255, 255, 255, 0.04);
}

.debug-section-title {
  font-size: 11px;
  font-weight: 600;
  color: rgba(226, 232, 240, 0.4);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.debug-json {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  line-height: 1.5;
  color: rgba(226, 232, 240, 0.7);
  background: rgba(0, 0, 0, 0.2);
  padding: 8px 10px;
  border-radius: 6px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
  overflow-y: auto;
}

.debug-json.debug-error {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.08);
}

.debug-duration {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #818cf8;
}

.debug-breakpoints-section {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 10px;
  margin-top: 12px;
}

.debug-breakpoint-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  font-size: 11px;
}

.debug-bp-node-id {
  color: rgba(226, 232, 240, 0.5);
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
}

.debug-badge {
  background: rgba(234, 179, 8, 0.3) !important;
  color: #eab308 !important;
}
</style>
