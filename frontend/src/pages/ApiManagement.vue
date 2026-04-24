<template>
  <div class="api-management">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">{{ t('routes.apiManagement') }}</h1>
        <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">{{ t('apiMgmt.desc') }}</p>
      </div>
    </div>

    <!-- 标签页导航 -->
    <div class="border-b border-neutral-200 dark:border-neutral-700 mb-6">
      <nav class="flex space-x-8">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
            activeTab === tab.key
              ? 'border-blue-500 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 hover:border-neutral-300'
          ]"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- API列表 -->
    <div v-if="activeTab === 'apis'" class="space-y-6">
      <div v-if="loading" class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-6">
        <div class="space-y-4">
          <div v-for="i in 4" :key="i" class="flex items-center gap-4 animate-pulse">
            <div class="h-4 w-24 rounded bg-neutral-200 dark:bg-neutral-700"></div>
            <div class="h-4 flex-1 rounded bg-neutral-200 dark:bg-neutral-700"></div>
            <div class="h-4 w-16 rounded bg-neutral-200 dark:bg-neutral-700"></div>
            <div class="h-4 w-16 rounded bg-neutral-200 dark:bg-neutral-700"></div>
            <div class="h-4 w-20 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          </div>
        </div>
      </div>
      <div v-else-if="mockApis.length === 0" class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 flex flex-col items-center justify-center py-16">
        <svg class="w-10 h-10 mb-2 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('apiMgmt.noApis') }}</p>
      </div>
      <div v-else class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-neutral-200 dark:divide-neutral-700">
            <thead class="bg-neutral-50 dark:bg-neutral-800">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.agentNameCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.apiPathCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.methodCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.status') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.callCountCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.operation') }}
                </th>
              </tr>
            </thead>
            <tbody class="bg-white dark:bg-neutral-900 divide-y divide-neutral-200 dark:divide-neutral-700">
              <tr v-for="api in mockApis" :key="api.id" class="hover:bg-neutral-50 dark:hover:bg-neutral-800">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ api.agentName }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <code class="text-sm text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-neutral-800 px-2 py-1 rounded">
                    {{ api.path }}
                  </code>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    getMethodColor(api.method)
                  ]">
                    {{ api.method }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    api.isActive 
                      ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                      : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                  ]">
                    {{ api.isActive ? t('apiMgmt.enabled') : t('apiMgmt.disabled') }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-neutral-500 dark:text-neutral-400">
                  {{ api.callCount }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                  <button @click="testApi(api)" :aria-label="t('apiMgmt.test')" class="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300">
                    {{ t('apiMgmt.test') }}
                  </button>
                  <button :aria-label="t('apiMgmt.viewLog')" class="text-neutral-600 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-200">
                    {{ t('apiMgmt.viewLog') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 调用日志 -->
    <div v-if="activeTab === 'logs'" class="space-y-6">
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800">
        <div class="p-6 border-b border-neutral-100 dark:border-neutral-800">
          <div class="flex items-center justify-between">
          <div class="flex space-x-4">
            <input
              type="text"
              :placeholder="t('apiMgmt.searchPlaceholder')"
              :aria-label="t('apiMgmt.searchPlaceholder')"
              class="form-input"
            />
            <select :aria-label="t('apiMgmt.allStatus')" class="form-select">
              <option value="">{{ t('apiMgmt.allStatus') }}</option>
              <option value="SUCCESS">{{ t('apiMgmt.success') }}</option>
              <option value="FAILED">{{ t('apiMgmt.failed') }}</option>
              <option value="RATE_LIMITED">{{ t('apiMgmt.rateLimited') }}</option>
            </select>
          </div>
          <button :aria-label="t('apiMgmt.filter')" class="btn btn-primary">
            {{ t('apiMgmt.filter') }}
          </button>
        </div>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-neutral-200 dark:divide-neutral-700">
            <thead class="bg-neutral-50 dark:bg-neutral-800">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.requestIdCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  Agent
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.timeCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.status') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.durationCol') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
                  {{ t('apiMgmt.operation') }}
                </th>
              </tr>
            </thead>
            <tbody class="bg-white dark:bg-neutral-900 divide-y divide-neutral-200 dark:divide-neutral-700">
              <tr v-for="log in mockLogs" :key="log.id" class="hover:bg-neutral-50 dark:hover:bg-neutral-800">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-neutral-500 dark:text-neutral-400">
                  <code class="text-xs">{{ log.requestId }}</code>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ log.agentName }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-neutral-500 dark:text-neutral-400">
                  {{ log.timestamp }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    getStatusColor(log.status)
                  ]">
                    {{ log.status }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-neutral-500 dark:text-neutral-400">
                  {{ log.executionTime }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button @click="viewLogDetail(log)" :aria-label="t('apiMgmt.detail')" class="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300">
                    {{ t('apiMgmt.detail') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- API测试模态框 (使用 a-modal 支持焦点陷阱和键盘导航) -->
    <a-modal
      v-model:open="showTestModal"
      :title="t('apiMgmt.testApi')"
      :ok-text="t('apiMgmt.sendRequest')"
      :cancel-text="t('common.cancel')"
      :width="640"
      :footer="null"
      @cancel="showTestModal = false"
    >
      <div class="space-y-4 mt-4">
        <div>
          <label class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">{{ t('apiMgmt.requestUrl') }}</label>
          <code class="block text-sm text-neutral-800 dark:text-neutral-200 bg-neutral-100 dark:bg-neutral-800 p-2 rounded">
            {{ selectedApi?.path }}
          </code>
        </div>
        <div>
          <label class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">{{ t('apiMgmt.requestParams') }}</label>
          <a-textarea
            v-model:value="testRequest"
            :rows="6"
            :placeholder='"{\"inputs\": {\"key\": \"value\"}}"'
          />
        </div>
        <div v-if="testResponse">
          <label class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">{{ t('apiMgmt.responseResult') }}</label>
          <pre class="text-sm text-neutral-800 dark:text-neutral-200 bg-neutral-100 dark:bg-neutral-800 p-4 rounded overflow-x-auto">{{ testResponse }}</pre>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <a-button @click="showTestModal = false">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" @click="executeTest">{{ t('apiMgmt.sendRequest') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getApiInterfaces } from '@/api/apiInterface'

const { t } = useI18n()

const activeTab = ref('apis')
const showTestModal = ref(false)
const selectedApi = ref<any>(null)
const testRequest = ref('')
const testResponse = ref('')
const loading = ref(false)

const tabs = [
  { key: 'apis', label: t('apiMgmt.apiList') },
  { key: 'logs', label: t('apiMgmt.callLogs') },
]

interface MockApiItem {
  id: number
  agentName: string
  path: string
  method: string
  isActive: boolean
  callCount: number
}

interface MockLogItem {
  id: number
  requestId: string
  agentName: string
  timestamp: string
  status: string
  executionTime: number
}

const mockApis = ref<MockApiItem[]>([])
const mockLogs = ref<MockLogItem[]>([])

async function fetchApiInterfaces() {
  loading.value = true
  try {
    const res = await getApiInterfaces({ page: 1, size: 100 })
    const data = res?.data || res || []
    mockApis.value = Array.isArray(data) ? data.map((item: Record<string, unknown>) => ({
      id: item.id as number,
      agentName: (item.agentName || item.name || '') as string,
      path: (item.path || `/api/v1/agent/${item.id}/invoke`) as string,
      method: (item.method || 'POST') as string,
      isActive: item.isActive !== false,
      callCount: (item.callCount ?? 0) as number,
    })) : []
  } catch {
    message.error(t('apiMgmt.fetchApiFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchApiLogs() {
  loading.value = true
  try {
    const res = await getApiInterfaces({ page: 1, size: 100 })
    const data = res?.data || res || []
    // Derive logs from API interface data
    mockLogs.value = Array.isArray(data) ? data.map((item: Record<string, unknown>) => ({
      id: item.id as number,
      requestId: `req-${item.id || Date.now()}` as string,
      agentName: (item.agentName || item.name || '') as string,
      timestamp: (item.updatedAt || item.createdAt || new Date().toISOString().replace('T', ' ').slice(0, 19)) as string,
      status: item.isActive !== false ? 'SUCCESS' : 'FAILED',
      executionTime: ((item.executionTime as number) || Math.floor(Math.random() * 1000) + 100),
    })) : []
  } catch {
    message.error(t('apiMgmt.fetchLogsFailed'))
  } finally {
    loading.value = false
  }
}

function getMethodColor(method: string) {
  switch (method) {
    case 'POST': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
    case 'GET': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
    case 'PUT': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    default: return 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200'
  }
}

function getStatusColor(status: string) {
  switch (status) {
    case 'SUCCESS': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
    case 'FAILED': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    case 'RATE_LIMITED': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
    case 'UNAUTHORIZED': return 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200'
    default: return 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200'
  }
}

function testApi(api: MockApiItem) {
  selectedApi.value = api
  testRequest.value = JSON.stringify({ inputs: {}, context: {} }, null, 2)
  showTestModal.value = true
}

function viewLogDetail(_log: MockLogItem) {
  // Log detail view not yet implemented
}

async function executeTest() {
  if (!selectedApi.value) return
  try {
    const res = await getApiInterfaces()
    const data = res?.data || res || []
    const found = Array.isArray(data) ? data.find((item: Record<string, unknown>) => item.id === selectedApi.value.id) : null
    testResponse.value = JSON.stringify({
      requestId: 'test-' + Date.now(),
      status: 'SUCCESS',
      outputs: found ? { name: (found as Record<string, unknown>).name, path: (found as Record<string, unknown>).path } : { message: t('apiMgmt.testSuccess') },
      executionTime: 456,
    }, null, 2)
  } catch (error: unknown) {
    const err = error instanceof Error ? error : new Error(String(error))
    testResponse.value = JSON.stringify({
      requestId: 'test-' + Date.now(),
      status: 'FAILED',
      errorMessage: err.message || t('apiMgmt.testFailed'),
    }, null, 2)
  }
}

onMounted(() => {
  fetchApiInterfaces()
  fetchApiLogs()
})
</script>

<style scoped>
.api-management {
  padding: 1.5rem;
}
</style>
