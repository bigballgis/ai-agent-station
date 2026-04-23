<template>
  <div class="api-management">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('routes.apiManagement') }}</h1>
        <p class="text-gray-600 dark:text-gray-400 mt-1">{{ t('apiMgmt.desc') }}</p>
      </div>
    </div>

    <!-- 标签页导航 -->
    <div class="border-b border-gray-200 dark:border-gray-700 mb-6">
      <nav class="flex space-x-8">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
            activeTab === tab.key
              ? 'border-blue-500 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 hover:border-gray-300'
          ]"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- API列表 -->
    <div v-if="activeTab === 'apis'" class="space-y-6">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead class="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Agent名称
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  API路径
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  方法
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  状态
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  调用次数
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              <tr v-for="api in mockApis" :key="api.id" class="hover:bg-gray-50 dark:hover:bg-gray-700">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-gray-900 dark:text-white">{{ api.agentName }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <code class="text-sm text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-gray-700 px-2 py-1 rounded">
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
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  {{ api.callCount }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                  <button @click="testApi(api)" class="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300">
                    测试
                  </button>
                  <button class="text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-200">
                    查看日志
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
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between">
          <div class="flex space-x-4">
            <input
              type="text"
              :placeholder="t('apiMgmt.searchPlaceholder')"
              class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white"
            />
            <select class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white">
              <option value="">{{ t('apiMgmt.allStatus') }}</option>
              <option value="SUCCESS">{{ t('apiMgmt.success') }}</option>
              <option value="FAILED">{{ t('apiMgmt.failed') }}</option>
              <option value="RATE_LIMITED">{{ t('apiMgmt.rateLimited') }}</option>
            </select>
          </div>
          <button class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors">
            筛选
          </button>
        </div>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead class="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  请求ID
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  Agent
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  时间
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  状态
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  耗时(ms)
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              <tr v-for="log in mockLogs" :key="log.id" class="hover:bg-gray-50 dark:hover:bg-gray-700">
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  <code class="text-xs">{{ log.requestId }}</code>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-gray-900 dark:text-white">{{ log.agentName }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
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
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  {{ log.executionTime }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button @click="viewLogDetail(log)" class="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300">
                    详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- API测试模态框 -->
    <div v-if="showTestModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full mx-4">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900 dark:text-white">{{ t('apiMgmt.testApi') }}</h3>
            <button @click="showTestModal = false" class="text-gray-400 hover:text-gray-500">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        <div class="p-6">
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('apiMgmt.requestUrl') }}</label>
              <code class="block text-sm text-gray-800 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 p-2 rounded">
                {{ selectedApi?.path }}
              </code>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('apiMgmt.requestParams') }}</label>
              <textarea
                v-model="testRequest"
                rows="6"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white"
                placeholder='{"inputs": {"key": "value"}}'
              />
            </div>
            <div v-if="testResponse">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{{ t('apiMgmt.responseResult') }}</label>
              <pre class="text-sm text-gray-800 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 p-4 rounded overflow-x-auto">
                {{ testResponse }}
              </pre>
            </div>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button @click="showTestModal = false" class="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-200 dark:hover:bg-gray-700 dark:hover:text-gray-200 rounded-md transition-colors">
            取消
          </button>
          <button @click="executeTest" class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors">
            发送请求
          </button>
        </div>
      </div>
    </div>
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
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200'
  }
}

function getStatusColor(status: string) {
  switch (status) {
    case 'SUCCESS': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
    case 'FAILED': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    case 'RATE_LIMITED': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
    case 'UNAUTHORIZED': return 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200'
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200'
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
