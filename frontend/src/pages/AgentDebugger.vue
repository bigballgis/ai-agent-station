<template>
  <div class="agent-debugger-page h-full flex flex-col -m-6">
    <!-- 左右分栏 -->
    <div class="flex flex-1 overflow-hidden">
      <!-- 左侧: Agent 选择器 + 输入面板 -->
      <div class="w-96 flex-shrink-0 flex flex-col border-r border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-900">
        <!-- Agent 选择器 -->
        <div class="p-4 border-b border-neutral-100 dark:border-neutral-800">
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('debugger.selectAgent') }}</label>
          <select
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
                正在思考...
              </span>
            </div>
          </div>
        </div>

        <!-- 输入面板 -->
        <div class="p-4 border-t border-neutral-100 dark:border-neutral-800">
          <textarea
            v-model="inputMessage"
            placeholder="输入消息..."
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
              <p class="text-sm">发送消息后查看执行链路</p>
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
                      <span>耗时: {{ node.duration }}ms</span>
                      <span>类型: {{ node.type }}</span>
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
              <p class="text-sm">暂无 MCP 工具调用记录</p>
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
                    <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400">请求参数</span>
                    <pre class="mt-1 text-xs text-neutral-600 dark:text-neutral-400 bg-neutral-50 dark:bg-neutral-800 rounded-lg p-2 overflow-x-auto">{{ call.request }}</pre>
                  </div>
                  <div>
                    <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400">响应结果</span>
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
              <p class="text-sm">暂无记忆读写记录</p>
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
                      {{ op.type === 'load' ? '读取' : '保存' }}
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
              <p class="text-sm">暂无变量数据</p>
            </div>
            <div v-else class="bg-white dark:bg-neutral-900 rounded-xl shadow-sm border border-neutral-100 dark:border-neutral-800 overflow-hidden">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/60">
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">变量名</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">类型</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">当前值</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">更新时间</th>
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
            <span>总耗时: {{ totalDuration }}ms</span>
          </div>
          <div class="flex items-center gap-4 text-xs text-neutral-500 dark:text-neutral-400">
            <span>Token 使用: {{ tokenUsage }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getAllAgents } from '@/api/agent'
import { streamAgentExecution } from '@/api/stream'

const { t } = useI18n()

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
    idle: '等待输入',
    running: '执行中',
    completed: '执行完成',
    failed: '执行失败',
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
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
  }
  return map[status] || status
}

async function sendMessage() {
  if (!inputMessage.value.trim() || isExecuting.value) return

  const userMsg = inputMessage.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
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
    message.error('请选择一个有效的 Agent')
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
      onMessage: (data: any) => {
        // Handle different event types from SSE
        if (data.type === 'chain' || data.type === 'step') {
          // Execution chain step
          const node = executionChain.value.find((n: any) => n.name === data.name)
          if (node) {
            node.status = data.status === 'running' ? 'running' : 'completed'
            node.duration = data.duration || 0
            node.output = data.output || ''
          } else {
            executionChain.value.push({
              name: data.name || data.step || '未知步骤',
              status: data.status === 'running' ? 'running' : 'completed',
              duration: data.duration || 0,
              output: data.output || '',
              type: data.type === 'chain' ? 'LLM' : (data.nodeType || 'Tool'),
            })
          }
        } else if (data.type === 'tool_call' || data.type === 'mcp') {
          // MCP tool call
          mcpCalls.value.push({
            toolName: data.toolName || data.name || 'unknown',
            request: typeof data.request === 'string' ? data.request : JSON.stringify(data.request || {}, null, 2),
            response: typeof data.response === 'string' ? data.response : JSON.stringify(data.response || {}, null, 2),
            duration: data.duration || 0,
          })
        } else if (data.type === 'memory') {
          // Memory operation
          memoryOps.value.push({
            type: data.action === 'save' ? 'save' : 'load',
            content: data.content || '',
            time: formatTime(new Date()),
          })
        } else if (data.type === 'variable') {
          // Variable update
          variables.value.push({
            name: data.name || '',
            type: data.varType || data.type || 'string',
            value: typeof data.value === 'string' ? data.value : JSON.stringify(data.value),
            updatedAt: formatTime(new Date()),
          })
        } else if (data.type === 'token') {
          // Token usage
          tokenUsage.value = data.usage || data.count || 0
        } else if (data.type === 'content' || data.type === 'delta') {
          // Streaming content
          fullContent += data.content || data.text || data.delta || ''
        } else if (data.type === 'done' || data.type === 'complete') {
          // Stream complete
          totalDuration.value = Date.now() - startTime
          executionStatus.value = 'completed'
          isExecuting.value = false

          if (fullContent) {
            messages.value.push({ role: 'agent', content: fullContent })
          } else if (data.content || data.text) {
            messages.value.push({ role: 'agent', content: data.content || data.text })
          }

          if (sseConnection) {
            sseConnection.close()
            sseConnection = null
          }
        }
      },
      onError: (_error: any) => {
        totalDuration.value = Date.now() - startTime
        executionStatus.value = 'failed'
        isExecuting.value = false

        if (fullContent) {
          messages.value.push({ role: 'agent', content: fullContent })
        }

        if (sseConnection) {
          sseConnection.close()
          sseConnection = null
        }
      },
    })
  } catch (error: any) {
    message.error('执行失败: ' + (error.message || '未知错误'))
    executionStatus.value = 'failed'
    isExecuting.value = false
  }
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function fetchAgentList() {
  try {
    const res: any = await getAllAgents()
    const data = res?.data || res || []
    const list = Array.isArray(data) ? data : []
    agentList.value = list.map((item: any) => ({
      id: String(item.id),
      name: item.name || `Agent ${item.id}`,
    }))
    if (agentList.value.length > 0 && !selectedAgent.value) {
      selectedAgent.value = agentList.value[0].id
    }
  } catch (error: any) {
    message.error('获取Agent列表失败: ' + (error.message || '未知错误'))
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
