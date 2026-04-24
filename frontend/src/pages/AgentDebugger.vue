<template>
  <div class="agent-debugger-page h-full flex flex-col -m-6">
    <!-- 左右分栏 -->
    <div class="flex flex-1 overflow-hidden">
      <!-- 左侧: Agent 选择器 + 输入面板 -->
      <div class="w-96 flex-shrink-0 flex flex-col border-r border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-900">
        <!-- Agent 选择器 -->
        <div class="p-4 border-b border-neutral-100 dark:border-neutral-800">
          <label for="agent-debugger-select" class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('debugger.selectAgent') }}</label>
          <select
            id="agent-debugger-select"
            v-model="selectedAgent"
            class="w-full px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option v-for="agent in agentList" :key="agent.id" :value="agent.id">{{ agent.name }}</option>
          </select>
        </div>

        <!-- 对话历史 -->
        <div class="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="flex"
            :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[80%] px-4 py-2.5 rounded-2xl text-sm leading-relaxed"
              :class="msg.role === 'user'
                ? 'bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-br-md'
                : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 rounded-bl-md'"
            >
              {{ msg.content }}
            </div>
          </div>
          <div v-if="isExecuting" class="flex justify-start">
            <div class="bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400 px-4 py-2.5 rounded-2xl rounded-bl-md text-sm">
              <span class="inline-flex items-center gap-1.5">
                <span class="flex gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-neutral-400 dark:bg-neutral-500 animate-bounce" style="animation-delay: 0ms" />
                  <span class="w-1.5 h-1.5 rounded-full bg-neutral-400 dark:bg-neutral-500 animate-bounce" style="animation-delay: 150ms" />
                  <span class="w-1.5 h-1.5 rounded-full bg-neutral-400 dark:bg-neutral-500 animate-bounce" style="animation-delay: 300ms" />
                </span>
                {{ t('debugger.thinking') }}
              </span>
            </div>
          </div>
        </div>

        <!-- 输入面板 -->
        <div class="p-4 border-t border-neutral-100 dark:border-neutral-800">
          <div class="flex items-center justify-between mb-2">
            <span class="text-xs text-neutral-400 dark:text-neutral-500">
              {{ messages.length > 0 ? t('debugger.messageCount', { count: messages.length }) : '' }}
            </span>
            <button
              v-if="messages.length > 0"
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-neutral-500 dark:text-neutral-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
              @click="clearHistory"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              {{ t('debugger.clearHistory') }}
            </button>
          </div>
          <textarea
            v-model="inputMessage"
            :placeholder="t('debugger.inputPlaceholder')"
            rows="3"
            class="w-full px-3 py-2.5 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 resize-none"
            @keydown.ctrl.enter="sendMessage"
          />
          <button
            class="mt-2 w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!inputMessage.trim() || isExecuting"
            @click="sendMessage"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
            {{ t('debugger.send') }}
            <span class="text-xs opacity-70 ml-1">Ctrl+Enter</span>
          </button>
        </div>
      </div>

      <!-- 右侧: 执行详情面板 -->
      <div class="flex-1 flex flex-col bg-neutral-50 dark:bg-neutral-950 overflow-hidden">
        <!-- Tab 切换 -->
        <div class="flex-shrink-0 border-b border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-900 px-4">
          <div class="flex items-center gap-1">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="px-4 py-3 text-sm font-medium transition-all duration-200 cursor-pointer relative"
              :class="activeTab === tab.key
                ? 'text-primary-600 dark:text-primary-400'
                : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300'"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
              <div
                v-if="activeTab === tab.key"
                class="absolute bottom-0 left-2 right-2 h-0.5 rounded-full bg-primary-500"
              />
            </button>
          </div>
        </div>

        <!-- Tab 内容 -->
        <div class="flex-1 overflow-y-auto p-4 scrollbar-thin">
          <!-- 执行链路 Tab -->
          <div v-if="activeTab === 'chain'">
            <div v-if="executionChain.length === 0" class="flex flex-col items-center justify-center h-full text-neutral-400 dark:text-neutral-500">
              <svg class="w-12 h-12 mb-3 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <p class="text-sm">{{ t('debugger.noChain') }}</p>
            </div>
            <div v-else class="relative">
              <!-- 时间线竖线 -->
              <div class="absolute left-[15px] top-2 bottom-2 w-px bg-neutral-200 dark:bg-neutral-700" />
              <div class="space-y-4">
                <div
                  v-for="(node, index) in executionChain"
                  :key="index"
                  class="relative flex gap-4 pl-10"
                >
                  <!-- 状态圆点 -->
                  <div
                    class="absolute left-0 top-1.5 w-[31px] h-[31px] rounded-full flex items-center justify-center z-10"
                    :class="getNodeStatusClass(node.status)"
                  >
                    <svg v-if="node.status === 'completed'" class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <svg v-else-if="node.status === 'failed'" class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                    <svg v-else-if="node.status === 'running'" class="w-4 h-4 text-white animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <div v-else class="w-2 h-2 rounded-full bg-white/60" />
                  </div>
                  <!-- 节点内容 -->
                  <div class="flex-1 bg-white dark:bg-neutral-900 rounded-xl p-4 shadow-sm border border-neutral-100 dark:border-neutral-800">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-sm font-semibold text-neutral-800 dark:text-neutral-200">{{ node.name }}</span>
                      <span
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                        :class="getNodeBadgeClass(node.status)"
                      >
                        {{ getNodeStatusLabel(node.status) }}
                      </span>
                    </div>
                    <p class="text-xs text-neutral-500 dark:text-neutral-400 mb-2">{{ node.output }}</p>
                    <div class="flex items-center gap-3 text-xs text-neutral-400 dark:text-neutral-500">
                      <span>{{ t('debugger.duration') }} {{ node.duration }}ms</span>
                      <span>{{ t('debugger.nodeType') }} {{ node.type }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- MCP 工具调用 Tab -->
          <div v-if="activeTab === 'mcp'">
            <div v-if="mcpCalls.length === 0" class="flex flex-col items-center justify-center h-full text-neutral-400 dark:text-neutral-500">
              <svg class="w-12 h-12 mb-3 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <p class="text-sm">{{ t('debugger.noMcp') }}</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="(call, index) in mcpCalls"
                :key="index"
                class="bg-white dark:bg-neutral-900 rounded-xl p-4 shadow-sm border border-neutral-100 dark:border-neutral-800"
              >
                <div class="flex items-center justify-between mb-3">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 rounded-lg bg-green-100 dark:bg-green-900/40 flex items-center justify-center">
                      <svg class="w-3.5 h-3.5 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <span class="text-sm font-semibold text-neutral-800 dark:text-neutral-200">{{ call.toolName }}</span>
                  </div>
                  <span class="text-xs text-neutral-400 dark:text-neutral-500">{{ call.duration }}ms</span>
                </div>
                <div class="space-y-2">
                  <div>
                    <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('debugger.requestParams') }}</span>
                    <pre class="mt-1 text-xs text-neutral-600 dark:text-neutral-400 bg-neutral-50 dark:bg-neutral-800 rounded-lg p-2 overflow-x-auto">{{ call.request }}</pre>
                  </div>
                  <div>
                    <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('debugger.responseResult') }}</span>
                    <pre class="mt-1 text-xs text-neutral-600 dark:text-neutral-400 bg-neutral-50 dark:bg-neutral-800 rounded-lg p-2 overflow-x-auto">{{ call.response }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 记忆读写 Tab -->
          <div v-if="activeTab === 'memory'">
            <div v-if="memoryOps.length === 0" class="flex flex-col items-center justify-center h-full text-neutral-400 dark:text-neutral-500">
              <svg class="w-12 h-12 mb-3 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              <p class="text-sm">{{ t('debugger.noMemory') }}</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="(op, index) in memoryOps"
                :key="index"
                class="bg-white dark:bg-neutral-900 rounded-xl p-4 shadow-sm border border-neutral-100 dark:border-neutral-800"
              >
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <span
                      class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                      :class="op.type === 'load'
                        ? 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400'
                        : 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'"
                    >
                      {{ op.type === 'load' ? t('debugger.load') : t('debugger.save') }}
                    </span>
                    <span class="text-xs text-neutral-400 dark:text-neutral-500">{{ op.time }}</span>
                  </div>
                </div>
                <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ op.content }}</p>
              </div>
            </div>
          </div>

          <!-- 变量状态 Tab -->
          <div v-if="activeTab === 'variables'">
            <div v-if="variables.length === 0" class="flex flex-col items-center justify-center h-full text-neutral-400 dark:text-neutral-500">
              <svg class="w-12 h-12 mb-3 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
              </svg>
              <p class="text-sm">{{ t('debugger.noVariables') }}</p>
            </div>
            <div v-else class="bg-white dark:bg-neutral-900 rounded-xl shadow-sm border border-neutral-100 dark:border-neutral-800 overflow-hidden">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/60">
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('debugger.variableName') }}</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('debugger.variableType') }}</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('debugger.variableValue') }}</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('debugger.variableUpdatedAt') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(v, index) in variables"
                    :key="index"
                    class="border-b border-neutral-50 dark:border-neutral-800/50 last:border-0"
                  >
                    <td class="px-4 py-3 font-mono text-sm text-primary-600 dark:text-primary-400">{{ v.name }}</td>
                    <td class="px-4 py-3">
                      <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400">
                        {{ v.type }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-neutral-700 dark:text-neutral-300 max-w-[300px] truncate">{{ v.value }}</td>
                    <td class="px-4 py-3 text-xs text-neutral-400 dark:text-neutral-500">{{ v.updatedAt }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 底部状态栏 -->
        <div class="flex-shrink-0 border-t border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-900 px-4 py-2.5 flex items-center justify-between">
          <div class="flex items-center gap-4 text-xs text-neutral-500 dark:text-neutral-400">
            <span class="flex items-center gap-1.5">
              <span
                class="w-2 h-2 rounded-full"
                :class="executionStatus === 'running' ? 'bg-blue-500 animate-pulse' : executionStatus === 'completed' ? 'bg-green-500' : executionStatus === 'failed' ? 'bg-red-500' : 'bg-neutral-300 dark:bg-neutral-600'"
              />
              {{ executionStatusLabel }}
            </span>
            <span>{{ t('debugger.totalDuration') }}: {{ totalDuration }}ms</span>
          </div>
          <div class="flex items-center gap-4 text-xs text-neutral-500 dark:text-neutral-400">
            <span>{{ t('debugger.tokenUsage') }}: {{ tokenUsage }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { getAllAgents } from '@/api/agent'
import { streamAgentExecution } from '@/api/stream'
import { getExecutionHistory, deleteExecutionHistory } from '@/api/executionHistory'

const { t, locale } = useI18n()

// ==================== 类型定义 ====================

interface ExecutionHistoryItem {
  id: number
  agentId: number
  role: string
  message: string
  createdAt?: string
  timestamp?: string
}

interface SSEEventData {
  type: string
  name?: string
  step?: string
  status?: string
  duration?: number
  output?: string
  nodeType?: string
  toolName?: string
  request?: unknown
  response?: unknown
  action?: string
  content?: string
  text?: string
  delta?: string
  varType?: string
  value?: unknown
  usage?: number
  count?: number
}

interface AgentItem {
  id: number | string
  name: string
}

// ============ 对话历史持久化 ============
const HISTORY_PREFIX = 'debug_history_'
const MAX_HISTORY_MESSAGES = 50

function getHistoryKey(agentId: string): string {
  return `${HISTORY_PREFIX}${agentId}`
}

function saveHistory(agentId: string, msgs: typeof messages.value) {
  if (!agentId) return
  try {
    const toSave = msgs.slice(-MAX_HISTORY_MESSAGES)
    localStorage.setItem(getHistoryKey(agentId), JSON.stringify(toSave))
  } catch {
    // localStorage may be full or unavailable
  }
}

function loadHistory(agentId: string): typeof messages.value {
  if (!agentId) return []
  // Try localStorage first for immediate display
  try {
    const raw = localStorage.getItem(getHistoryKey(agentId))
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch {
    // invalid data, ignore
  }
  return []
}

async function loadHistoryFromBackend(agentId: string) {
  if (!agentId) return
  try {
    const res = await getExecutionHistory(Number(agentId))
    if (res.code === 200 && res.data && Array.isArray(res.data) && res.data.length > 0) {
      // Backend returns newest first, reverse to get chronological order
      const historyMsgs = res.data
        .slice()
        .reverse()
        .map((item: ExecutionHistoryItem) => ({
          role: item.role === 'user' ? 'user' as const : 'agent' as const,
          content: item.message,
        }))
      messages.value = historyMsgs
      // Sync to localStorage as offline fallback
      saveHistory(agentId, messages.value)
      message.info(t('debugger.historyLoaded'))
    }
    // If backend returns empty, keep whatever localStorage had (if anything)
  } catch {
    // Backend unavailable, localStorage data already loaded by loadHistory()
  }
}

function clearHistoryStorage(agentId: string) {
  if (!agentId) return
  localStorage.removeItem(getHistoryKey(agentId))
}

// Agent 列表
const agentList = ref<{ id: string; name: string }[]>([])
const selectedAgent = ref('')
let sseConnection: { close: () => void } | null = null

// Tab
const tabs = computed(() => [
  { key: 'chain', label: t('debugger.tabs.chain') },
  { key: 'mcp', label: t('debugger.tabs.mcp') },
  { key: 'memory', label: t('debugger.tabs.memory') },
  { key: 'variables', label: t('debugger.tabs.variables') },
])
const activeTab = ref('chain')

// 对话
const messages = ref<{ role: 'user' | 'agent'; content: string }[]>([])
const inputMessage = ref('')
const isExecuting = ref(false)

// 执行状态
const executionStatus = ref<'idle' | 'running' | 'completed' | 'failed'>('idle')
const totalDuration = ref(0)
const tokenUsage = ref(0)

const executionStatusLabel = computed(() => {
  const map: Record<string, string> = {
    idle: t('debugger.status.idle'),
    running: t('debugger.status.running'),
    completed: t('debugger.status.completed'),
    failed: t('debugger.status.failed'),
  }
  return map[executionStatus.value]
})

// 执行链路
const executionChain = ref<{
  name: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  duration: number
  output: string
  type: string
}[]>([])

// MCP 工具调用
const mcpCalls = ref<{
  toolName: string
  request: string
  response: string
  duration: number
}[]>([])

// 记忆读写
const memoryOps = ref<{
  type: 'load' | 'save'
  content: string
  time: string
}[]>([])

// 变量状态
const variables = ref<{
  name: string
  type: string
  value: string
  updatedAt: string
}[]>([])

function getNodeStatusClass(status: string): string {
  const map: Record<string, string> = {
    pending: 'bg-neutral-300 dark:bg-neutral-600',
    running: 'bg-blue-500',
    completed: 'bg-green-500',
    failed: 'bg-red-500',
  }
  return map[status] || map.pending
}

function getNodeBadgeClass(status: string): string {
  const map: Record<string, string> = {
    pending: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400',
    running: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    completed: 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400',
    failed: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
  }
  return map[status] || map.pending
}

function getNodeStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: t('debugger.nodeStatus.pending'),
    running: t('debugger.nodeStatus.running'),
    completed: t('debugger.nodeStatus.completed'),
    failed: t('debugger.nodeStatus.failed'),
  }
  return map[status] || status
}

async function sendMessage() {
  if (!inputMessage.value.trim() || isExecuting.value) return

  const userMsg = inputMessage.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  saveHistory(selectedAgent.value, messages.value)
  inputMessage.value = ''
  isExecuting.value = true
  executionStatus.value = 'running'
  activeTab.value = 'chain'

  // Reset execution data
  executionChain.value = []
  mcpCalls.value = []
  memoryOps.value = []
  variables.value = []
  totalDuration.value = 0
  tokenUsage.value = 0

  const agentId = Number(selectedAgent.value)
  if (!agentId) {
    message.error(t('debugger.selectValidAgent'))
    isExecuting.value = false
    executionStatus.value = 'failed'
    return
  }

  // Close any existing SSE connection
  if (sseConnection) {
    sseConnection.close()
    sseConnection = null
  }

  const startTime = Date.now()
  let fullContent = ''

  try {
    sseConnection = streamAgentExecution(agentId, { message: userMsg }, {
      onMessage: (data: unknown) => {
        const evt = data as SSEEventData
        // Handle different event types from SSE
        if (evt.type === 'chain' || evt.type === 'step') {
          // Execution chain step
          const node = executionChain.value.find(n => n.name === evt.name)
          if (node) {
            node.status = evt.status === 'running' ? 'running' : 'completed'
            node.duration = evt.duration || 0
            node.output = evt.output || ''
          } else {
            executionChain.value.push({
              name: evt.name || evt.step || t('debugger.unknownStep'),
              status: evt.status === 'running' ? 'running' : 'completed',
              duration: evt.duration || 0,
              output: evt.output || '',
              type: evt.type === 'chain' ? 'LLM' : (evt.nodeType || 'Tool'),
            })
          }
        } else if (evt.type === 'tool_call' || evt.type === 'mcp') {
          // MCP tool call
          mcpCalls.value.push({
            toolName: evt.toolName || evt.name || 'unknown',
            request: typeof evt.request === 'string' ? evt.request : JSON.stringify(evt.request || {}, null, 2),
            response: typeof evt.response === 'string' ? evt.response : JSON.stringify(evt.response || {}, null, 2),
            duration: evt.duration || 0,
          })
        } else if (evt.type === 'memory') {
          // Memory operation
          memoryOps.value.push({
            type: evt.action === 'save' ? 'save' : 'load',
            content: evt.content || '',
            time: formatTime(new Date()),
          })
        } else if (evt.type === 'variable') {
          // Variable update
          variables.value.push({
            name: evt.name || '',
            type: evt.varType || evt.type || 'string',
            value: typeof evt.value === 'string' ? evt.value : JSON.stringify(evt.value),
            updatedAt: formatTime(new Date()),
          })
        } else if (evt.type === 'token') {
          // Token usage
          tokenUsage.value = evt.usage || evt.count || 0
        } else if (evt.type === 'content' || evt.type === 'delta') {
          // Streaming content
          fullContent += evt.content || evt.text || evt.delta || ''
        } else if (evt.type === 'done' || evt.type === 'complete') {
          // Stream complete
          totalDuration.value = Date.now() - startTime
          executionStatus.value = 'completed'
          isExecuting.value = false

          if (fullContent) {
            messages.value.push({ role: 'agent', content: fullContent })
          } else if (evt.content || evt.text) {
            messages.value.push({ role: 'agent', content: evt.content || evt.text || '' })
          }

          saveHistory(selectedAgent.value, messages.value)

          if (sseConnection) {
            sseConnection.close()
            sseConnection = null
          }
        }
      },
      onError: (_error: Error) => {
        totalDuration.value = Date.now() - startTime
        executionStatus.value = 'failed'
        isExecuting.value = false

        if (fullContent) {
          messages.value.push({ role: 'agent', content: fullContent })
        }

        saveHistory(selectedAgent.value, messages.value)

        if (sseConnection) {
          sseConnection.close()
          sseConnection = null
        }
      },
    })
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : String(error)
    message.error(t('debugger.executionFailed', { message: errMsg }))
    executionStatus.value = 'failed'
    isExecuting.value = false
  }
}

function clearHistory() {
  Modal.confirm({
    title: t('debugger.clearHistory'),
    content: t('debugger.clearHistoryConfirm'),
    okText: t('common.confirm'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    onOk: async () => {
      clearHistoryStorage(selectedAgent.value)
      messages.value = []
      // 同时清空执行数据
      executionChain.value = []
      mcpCalls.value = []
      memoryOps.value = []
      variables.value = []
      executionStatus.value = 'idle'
      totalDuration.value = 0
      tokenUsage.value = 0
      // Try to delete from backend
      try {
        if (selectedAgent.value) {
          await deleteExecutionHistory(Number(selectedAgent.value))
        }
      } catch {
        // Backend delete failed, localStorage already cleared
      }
      message.success(t('debugger.clearHistorySuccess'))
    },
  })
}

// 监听 Agent 切换，加载对应历史记录
watch(selectedAgent, (newAgentId) => {
  if (newAgentId) {
    // Immediately load from localStorage for fast display
    messages.value = loadHistory(newAgentId)
    // Then try to load from backend (may override with more complete data)
    loadHistoryFromBackend(newAgentId)
  } else {
    messages.value = []
  }
})

function formatTime(date: Date): string {
  return date.toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function fetchAgentList() {
  try {
    const res = await getAllAgents()
    const data = res?.data || res || []
    const list: AgentItem[] = Array.isArray(data) ? data : []
    agentList.value = list.map((item: AgentItem) => ({
      id: String(item.id),
      name: item.name || `Agent ${item.id}`,
    }))
    if (agentList.value.length > 0 && !selectedAgent.value) {
      selectedAgent.value = agentList.value[0].id
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : String(error)
    message.error(t('debugger.fetchAgentListFailed', { message: errMsg }))
  }
}

onMounted(() => {
  fetchAgentList()
})

onUnmounted(() => {
  if (sseConnection) {
    sseConnection.close()
    sseConnection = null
  }
})
</script>

<style scoped>
.agent-debugger-page {
  min-height: 100%;
}

.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 4px;
}
.dark .scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
}
</style>
