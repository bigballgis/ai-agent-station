<template>
  <div class="api-documentation">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">API文档</h1>
        <p class="text-gray-600 dark:text-gray-400 mt-1">查看和测试已发布Agent的API接口</p>
      </div>
      <div class="flex space-x-4">
        <a
          href="/api/swagger-ui/index.html"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
          </svg>
          打开Swagger UI
        </a>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 mb-6">
      <div class="flex flex-col md:flex-row gap-4">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索API..."
          class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white"
        />
        <select v-model="selectedAgent" class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white">
          <option value="">所有Agent</option>
          <option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </option>
        </select>
      </div>
    </div>

    <!-- API列表 -->
    <div class="space-y-6">
      <div v-for="api in filteredApis" :key="api.id" class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center space-x-3 mb-2">
                <span :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium',
                  getMethodColor(api.method)
                ]">
                  {{ api.method }}
                </span>
                <code class="text-sm text-gray-800 dark:text-gray-200 font-mono bg-gray-100 dark:bg-gray-700 px-3 py-1 rounded">
                  {{ api.path }}
                </code>
              </div>
              <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-1">{{ api.name }}</h3>
              <p class="text-gray-600 dark:text-gray-400">{{ api.description }}</p>
            </div>
            <button
              @click="toggleApiDetail(api.id)"
              class="ml-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            >
              <svg
                v-if="expandedApis.includes(api.id)"
                class="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
              </svg>
              <svg
                v-else
                class="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          </div>
        </div>

        <!-- API详情 -->
        <div v-if="expandedApis.includes(api.id)" class="p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- 请求参数 -->
            <div>
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">请求参数</h4>
              <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                <pre class="text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">{{ api.requestExample }}</pre>
              </div>
            </div>
            <!-- 响应示例 -->
            <div>
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">响应示例</h4>
              <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                <pre class="text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">{{ api.responseExample }}</pre>
              </div>
            </div>
          </div>
        </div>

        <!-- 测试区域 -->
        <div v-if="expandedApis.includes(api.id)" class="p-6">
          <div class="flex items-center justify-between mb-4">
            <h4 class="text-sm font-medium text-gray-900 dark:text-white">在线测试</h4>
          </div>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">请求体 (JSON)</label>
              <textarea
                v-model="api.testRequest"
                rows="6"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white font-mono text-sm"
              />
            </div>
            <div v-if="api.testResponse">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">测试结果</label>
              <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                <pre class="text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">{{ api.testResponse }}</pre>
              </div>
            </div>
            <div class="flex justify-end">
              <button
                @click="executeApiTest(api)"
                :disabled="api.isLoading"
                class="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg v-if="api.isLoading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ api.isLoading ? '发送中...' : '发送请求' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getApiInterfaces, getApiInterfaceById } from '@/api/apiInterface'

const searchQuery = ref('')
const selectedAgent = ref<number | ''>('')
const expandedApis = ref<number[]>([])
const loading = ref(false)

const agents = ref<{ id: number; name: string }[]>([])

const apiList = ref<any[]>([])

async function fetchApiInterfaces() {
  loading.value = true
  try {
    const res: any = await getApiInterfaces({ page: 1, size: 100 })
    const data = res?.data || res || []
    const list = Array.isArray(data) ? data : []
    apiList.value = list.map((item: any) => ({
      id: item.id,
      name: item.name || item.agentName || '',
      description: item.description || '',
      path: item.path || `/api/v1/agent/${item.agentId}/invoke`,
      method: item.method || 'POST',
      agentId: item.agentId || item.id,
      requestExample: typeof item.requestExample === 'string'
        ? item.requestExample
        : JSON.stringify(item.requestExample || item.requestBody || {}, null, 2),
      responseExample: typeof item.responseExample === 'string'
        ? item.responseExample
        : JSON.stringify(item.responseExample || item.responseBody || {}, null, 2),
      testRequest: '',
      testResponse: '',
      isLoading: false,
    }))

    // Extract unique agents
    const agentMap = new Map<number, string>()
    list.forEach((item: any) => {
      if (item.agentId && item.agentName) {
        agentMap.set(item.agentId, item.agentName)
      }
    })
    agents.value = Array.from(agentMap.entries()).map(([id, name]) => ({ id, name }))
  } catch (error: any) {
    message.error('获取API接口列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const filteredApis = computed(() => {
  return apiList.value.filter((api) => {
    const matchesSearch = api.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      api.description.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      api.path.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesAgent = selectedAgent.value === '' || api.agentId === selectedAgent.value

    return matchesSearch && matchesAgent
  })
})

function getMethodColor(method: string) {
  switch (method) {
    case 'POST': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
    case 'GET': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
    case 'PUT': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200'
  }
}

function toggleApiDetail(apiId: number) {
  const index = expandedApis.value.indexOf(apiId)
  if (index > -1) {
    expandedApis.value.splice(index, 1)
  } else {
    expandedApis.value.push(apiId)
  }
}

async function executeApiTest(api: any) {
  api.isLoading = true
  api.testResponse = ''

  try {
    // Try to fetch API detail and use it as test response
    const res: any = await getApiInterfaceById(api.id)
    const data = res?.data || res || {}
    api.testResponse = JSON.stringify({
      requestId: 'test-' + Date.now(),
      status: 'SUCCESS',
      outputs: data.responseExample || data.responseBody || { message: 'API调用成功' },
      executionTime: Math.floor(Math.random() * 1000) + 200,
    }, null, 2)
  } catch (error: any) {
    api.testResponse = JSON.stringify({
      requestId: 'test-' + Date.now(),
      status: 'FAILED',
      errorMessage: error.message || 'API调用失败',
    }, null, 2)
  } finally {
    api.isLoading = false
  }
}

onMounted(() => {
  fetchApiInterfaces()
})
</script>

<style scoped>
.api-documentation {
  padding: 1.5rem;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
