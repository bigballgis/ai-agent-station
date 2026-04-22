<template>
  <div class="log-center-page">
    <!-- 页面头部 -->
    <PageHeader title="日志中心" subtitle="查看系统操作日志、API 调用日志和异常日志，追踪系统运行状态" />

    <!-- Tab 切换 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden animate-slide-up">
      <!-- Tab 栏 -->
      <div class="px-6 pt-5 pb-0 border-b border-neutral-100 dark:border-neutral-800">
        <TabNav :tabs="tabItems" :active-key="activeTab" @change="activeTab = $event" />
      </div>

      <!-- 操作日志 Tab -->
      <div v-if="activeTab === 'operation'" class="p-6">
        <!-- 筛选栏 -->
        <div class="flex flex-wrap items-center gap-3 mb-5">
          <TimeRangePicker @change="handleOperationTimeRangeChange" />
          <select
            v-model="operationFilters.module"
            class="px-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
          >
            <option value="">全部模块</option>
            <option value="agent">Agent 管理</option>
            <option value="approval">审批管理</option>
            <option value="deployment">发布管理</option>
            <option value="api">API 管理</option>
            <option value="system">系统设置</option>
            <option value="tenant">租户管理</option>
          </select>
          <div class="relative flex-1 min-w-[200px] max-w-xs">
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 dark:text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="operationFilters.operator"
              type="text"
              placeholder="搜索操作人..."
              class="w-full pl-10 pr-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>
          <button
            class="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="resetOperationFilters"
          >
            重置
          </button>
        </div>

        <!-- 操作日志表格 -->
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800">
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">时间</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">操作人</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">模块</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">操作类型</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">IP 地址</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">详情</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="log in paginatedOperationLogs"
                :key="log.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 whitespace-nowrap text-xs">{{ log.time }}</td>
                <td class="py-3 px-4">
                  <div class="flex items-center gap-2">
                    <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-[10px] font-medium">
                      {{ log.operator.charAt(0) }}
                    </div>
                    <span class="text-neutral-800 dark:text-neutral-200 font-medium">{{ log.operator }}</span>
                  </div>
                </td>
                <td class="py-3 px-4">
                  <span class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-300">
                    {{ log.module }}
                  </span>
                </td>
                <td class="py-3 px-4">
                  <span
                    :class="[
                      'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium',
                      log.type === '创建' ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400' :
                      log.type === '删除' ? 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400' :
                      log.type === '审批' ? 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400' :
                      'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
                    ]"
                  >
                    {{ log.type }}
                  </span>
                </td>
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 font-mono text-xs">{{ log.ip }}</td>
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 text-xs max-w-[200px] truncate">{{ log.detail }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-neutral-100 dark:border-neutral-800">
          <span class="text-xs text-neutral-400 dark:text-neutral-500">共 {{ filteredOperationLogs.length }} 条记录</span>
          <div class="flex items-center gap-1">
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', operationPage <= 1 ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="operationPage <= 1"
              @click="operationPage--"
            >
              上一页
            </button>
            <span class="px-3 py-1.5 text-xs text-neutral-500 dark:text-neutral-400">{{ operationPage }} / {{ operationTotalPages }}</span>
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', operationPage >= operationTotalPages ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="operationPage >= operationTotalPages"
              @click="operationPage++"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      <!-- 调用日志 Tab -->
      <div v-if="activeTab === 'api'" class="p-6">
        <!-- 筛选栏 -->
        <div class="flex flex-wrap items-center gap-3 mb-5">
          <TimeRangePicker @change="handleApiTimeRangeChange" />
          <select
            v-model="apiFilters.agent"
            class="px-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
          >
            <option value="">全部 Agent</option>
            <option value="智能客服">智能客服</option>
            <option value="文档助手">文档助手</option>
            <option value="数据分析">数据分析</option>
            <option value="代码生成">代码生成</option>
          </select>
          <select
            v-model="apiFilters.status"
            class="px-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
          >
            <option value="">全部状态</option>
            <option value="success">成功</option>
            <option value="failed">失败</option>
            <option value="timeout">超时</option>
          </select>
          <button
            class="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="resetApiFilters"
          >
            重置
          </button>
        </div>

        <!-- 调用日志表格 -->
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800">
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">时间</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">Agent</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">调用方</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">请求参数</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">响应时间</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="log in paginatedApiLogs"
                :key="log.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 whitespace-nowrap text-xs">{{ log.time }}</td>
                <td class="py-3 px-4 text-neutral-800 dark:text-neutral-200 font-medium">{{ log.agent }}</td>
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 text-xs">{{ log.caller }}</td>
                <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 text-xs max-w-[180px] truncate font-mono">{{ log.params }}</td>
                <td class="py-3 px-4">
                  <span
                    :class="[
                      'text-xs font-medium',
                      log.responseTime < 500 ? 'text-green-600 dark:text-green-400' :
                      log.responseTime < 2000 ? 'text-amber-600 dark:text-amber-400' :
                      'text-red-600 dark:text-red-400',
                    ]"
                  >
                    {{ log.responseTime }}ms
                  </span>
                </td>
                <td class="py-3 px-4">
                  <span
                    :class="[
                      'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium',
                      log.status === 'success' ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400' :
                      log.status === 'failed' ? 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400' :
                      'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
                    ]"
                  >
                    {{ log.status === 'success' ? '成功' : log.status === 'failed' ? '失败' : '超时' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-neutral-100 dark:border-neutral-800">
          <span class="text-xs text-neutral-400 dark:text-neutral-500">共 {{ filteredApiLogs.length }} 条记录</span>
          <div class="flex items-center gap-1">
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', apiPage <= 1 ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="apiPage <= 1"
              @click="apiPage--"
            >
              上一页
            </button>
            <span class="px-3 py-1.5 text-xs text-neutral-500 dark:text-neutral-400">{{ apiPage }} / {{ apiTotalPages }}</span>
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', apiPage >= apiTotalPages ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="apiPage >= apiTotalPages"
              @click="apiPage++"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      <!-- 异常日志 Tab -->
      <div v-if="activeTab === 'error'" class="p-6">
        <!-- 异常日志表格 -->
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800">
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400 w-8"></th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">时间</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">级别</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">模块</th>
                <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">异常信息</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="log in paginatedErrorLogs" :key="log.id">
                <tr
                  class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors cursor-pointer"
                  @click="toggleErrorExpand(log.id)"
                >
                  <td class="py-3 px-4">
                    <svg
                      :class="['w-4 h-4 text-neutral-400 dark:text-neutral-500 transition-transform duration-200', expandedErrors.has(log.id) ? 'rotate-90' : '']"
                      fill="none" stroke="currentColor" viewBox="0 0 24 24"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </td>
                  <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400 whitespace-nowrap text-xs">{{ log.time }}</td>
                  <td class="py-3 px-4">
                    <span
                      :class="[
                        'inline-flex items-center px-2 py-0.5 rounded-md text-xs font-bold',
                        log.level === 'ERROR'
                          ? 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400'
                          : 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
                      ]"
                    >
                      {{ log.level }}
                    </span>
                  </td>
                  <td class="py-3 px-4">
                    <span class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-300">
                      {{ log.module }}
                    </span>
                  </td>
                  <td class="py-3 px-4 text-neutral-700 dark:text-neutral-300 text-xs max-w-[400px] truncate">{{ log.message }}</td>
                </tr>
                <!-- 展开的堆栈信息 -->
                <tr v-if="expandedErrors.has(log.id)">
                  <td colspan="5" class="px-4 py-3 bg-neutral-50 dark:bg-neutral-800/30">
                    <pre class="text-xs text-neutral-500 dark:text-neutral-400 font-mono whitespace-pre-wrap overflow-x-auto max-h-48 overflow-y-auto p-3 rounded-xl bg-neutral-100 dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700">{{ log.stack }}</pre>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-neutral-100 dark:border-neutral-800">
          <span class="text-xs text-neutral-400 dark:text-neutral-500">共 {{ errorLogs.length }} 条记录</span>
          <div class="flex items-center gap-1">
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', errorPage <= 1 ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="errorPage <= 1"
              @click="errorPage--"
            >
              上一页
            </button>
            <span class="px-3 py-1.5 text-xs text-neutral-500 dark:text-neutral-400">{{ errorPage }} / {{ errorTotalPages }}</span>
            <button
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', errorPage >= errorTotalPages ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
              :disabled="errorPage >= errorTotalPages"
              @click="errorPage++"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getLogs, getLogsByDateRange, getLogsByModule } from '@/api/log'
import { PageHeader, TabNav, TimeRangePicker } from '@/components'
import type { TabItem } from '@/components'

// ============ Tab 配置 ============

const tabs = [
  { key: 'operation', label: '操作日志' },
  { key: 'api', label: '调用日志' },
  { key: 'error', label: '异常日志' },
]
const activeTab = ref('operation')
const loading = ref(false)

// TabNav items（响应式）
const tabItems = computed<TabItem[]>(() => tabs.map(tab => ({ key: tab.key, label: tab.label })))

// TimeRangePicker change handlers
function handleOperationTimeRangeChange(range: { start: string; end: string } | null) {
  if (range) {
    operationFilters.value.dateRange = range
  } else {
    operationFilters.value.dateRange = null
  }
  operationPage.value = 1
  fetchOperationLogs()
}

function handleApiTimeRangeChange(range: { start: string; end: string } | null) {
  if (range) {
    apiFilters.value.dateRange = range
  } else {
    apiFilters.value.dateRange = null
  }
  apiPage.value = 1
  fetchApiLogs()
}

// ============ 操作日志 ============

interface OperationLog {
  id: string
  time: string
  operator: string
  module: string
  type: string
  ip: string
  detail: string
}

const operationFilters = ref({
  dateRange: null as any,
  module: '',
  operator: '',
})

const operationLogs = ref<OperationLog[]>([])

const operationPage = ref(1)
const operationPageSize = 8

const filteredOperationLogs = computed(() => {
  let result = operationLogs.value
  if (operationFilters.value.module) {
    result = result.filter(l => l.module === operationFilters.value.module)
  }
  if (operationFilters.value.operator) {
    const q = operationFilters.value.operator.toLowerCase()
    result = result.filter(l => l.operator.toLowerCase().includes(q))
  }
  return result
})

const operationTotalPages = computed(() => Math.max(1, Math.ceil(filteredOperationLogs.value.length / operationPageSize)))

const paginatedOperationLogs = computed(() => {
  const start = (operationPage.value - 1) * operationPageSize
  return filteredOperationLogs.value.slice(start, start + operationPageSize)
})

function resetOperationFilters() {
  operationFilters.value = { dateRange: null, module: '', operator: '' }
  operationPage.value = 1
  fetchOperationLogs()
}

async function fetchOperationLogs() {
  loading.value = true
  try {
    let res: any
    const dr = operationFilters.value.dateRange
    if (dr && (dr.start || (Array.isArray(dr) && dr.length === 2))) {
      const startTime = typeof dr === 'object' && dr.start ? dr.start : (Array.isArray(dr) && dr[0]?.format ? dr[0].format('YYYY-MM-DD HH:mm:ss') : '')
      const endTime = typeof dr === 'object' && dr.end ? dr.end : (Array.isArray(dr) && dr[1]?.format ? dr[1].format('YYYY-MM-DD HH:mm:ss') : '')
      res = await getLogsByDateRange({
        startTime,
        endTime,
        page: operationPage.value,
        size: 100,
      })
    } else if (operationFilters.value.module) {
      res = await getLogsByModule(operationFilters.value.module, {
        page: operationPage.value,
        size: 100,
      })
    } else {
      res = await getLogs({ page: operationPage.value, size: 100 })
    }
    const data = res?.data || res || []
    operationLogs.value = Array.isArray(data) ? data.map((item: any, index: number) => ({
      id: String(item.id || index + 1),
      time: item.time || item.createdAt || item.timestamp || '',
      operator: item.operator || item.createdBy || '',
      module: item.module || item.type || '',
      type: item.type || item.action || item.operationType || '',
      ip: item.ip || item.ipAddress || '',
      detail: item.detail || item.message || item.description || '',
    })) : []
  } catch (error: any) {
    message.error('获取操作日志失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ============ 调用日志 ============

interface ApiLog {
  id: string
  time: string
  agent: string
  caller: string
  params: string
  responseTime: number
  status: string
}

const apiFilters = ref({
  dateRange: null as any,
  agent: '',
  status: '',
})

const apiLogs = ref<ApiLog[]>([])

const apiPage = ref(1)
const apiPageSize = 8

const filteredApiLogs = computed(() => {
  let result = apiLogs.value
  if (apiFilters.value.agent) {
    result = result.filter(l => l.agent === apiFilters.value.agent)
  }
  if (apiFilters.value.status) {
    result = result.filter(l => l.status === apiFilters.value.status)
  }
  return result
})

const apiTotalPages = computed(() => Math.max(1, Math.ceil(filteredApiLogs.value.length / apiPageSize)))

const paginatedApiLogs = computed(() => {
  const start = (apiPage.value - 1) * apiPageSize
  return filteredApiLogs.value.slice(start, start + apiPageSize)
})

function resetApiFilters() {
  apiFilters.value = { dateRange: null, agent: '', status: '' }
  apiPage.value = 1
  fetchApiLogs()
}

async function fetchApiLogs() {
  loading.value = true
  try {
    let res: any
    const dr = apiFilters.value.dateRange
    if (dr && (dr.start || (Array.isArray(dr) && dr.length === 2))) {
      const startTime = typeof dr === 'object' && dr.start ? dr.start : (Array.isArray(dr) && dr[0]?.format ? dr[0].format('YYYY-MM-DD HH:mm:ss') : '')
      const endTime = typeof dr === 'object' && dr.end ? dr.end : (Array.isArray(dr) && dr[1]?.format ? dr[1].format('YYYY-MM-DD HH:mm:ss') : '')
      res = await getLogsByDateRange({
        startTime,
        endTime,
        page: apiPage.value,
        size: 100,
      })
    } else {
      res = await getLogs({ page: apiPage.value, size: 100 })
    }
    const data = res?.data || res || []
    apiLogs.value = Array.isArray(data) ? data.map((item: any, index: number) => ({
      id: String(item.id || index + 1),
      time: item.time || item.createdAt || item.timestamp || '',
      agent: item.agent || item.agentName || '',
      caller: item.caller || item.source || '',
      params: typeof item.params === 'string' ? item.params : JSON.stringify(item.params || {}),
      responseTime: item.responseTime || item.duration || item.executionTime || 0,
      status: item.status || 'success',
    })) : []
  } catch (error: any) {
    message.error('获取调用日志失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ============ 异常日志 ============

interface ErrorLog {
  id: string
  time: string
  level: 'ERROR' | 'WARN'
  module: string
  message: string
  stack: string
}

const errorLogs = ref<ErrorLog[]>([])

const errorPage = ref(1)
const errorPageSize = 5
const expandedErrors = ref<Set<string>>(new Set())

const errorTotalPages = computed(() => Math.max(1, Math.ceil(errorLogs.value.length / errorPageSize)))

const paginatedErrorLogs = computed(() => {
  const start = (errorPage.value - 1) * errorPageSize
  return errorLogs.value.slice(start, start + errorPageSize)
})

function toggleErrorExpand(id: string) {
  if (expandedErrors.value.has(id)) {
    expandedErrors.value.delete(id)
  } else {
    expandedErrors.value.add(id)
  }
}

async function fetchErrorLogs() {
  loading.value = true
  try {
    const res: any = await getLogs({ page: 1, size: 100 })
    const data = res?.data || res || []
    errorLogs.value = Array.isArray(data) ? data
      .filter((item: any) => item.level === 'ERROR' || item.level === 'WARN' || item.type === 'error' || item.type === 'warn')
      .map((item: any, index: number) => ({
        id: String(item.id || `e${index + 1}`),
        time: item.time || item.createdAt || item.timestamp || '',
        level: item.level || 'ERROR',
        module: item.module || item.source || '',
        message: item.message || item.errorMessage || '',
        stack: item.stack || item.stackTrace || '',
      })) : []
  } catch (error: any) {
    message.error('获取异常日志失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ============ Tab 切换时加载数据 ============

watch(activeTab, (newTab) => {
  if (newTab === 'operation') {
    fetchOperationLogs()
  } else if (newTab === 'api') {
    fetchApiLogs()
  } else if (newTab === 'error') {
    fetchErrorLogs()
  }
})

// ============ 初始化 ============

onMounted(() => {
  fetchOperationLogs()
})
</script>

<style scoped>
/* 日期选择器样式覆盖 */
:deep(.log-range-picker .ant-picker) {
  border-radius: 12px !important;
  border-color: #e5e5e5 !important;
  background-color: #fafafa !important;
  height: 36px !important;
}
.dark :deep(.log-range-picker .ant-picker) {
  border-color: #404040 !important;
  background-color: #262626 !important;
}
:deep(.log-range-picker .ant-picker-input > input) {
  font-size: 13px !important;
}
</style>
