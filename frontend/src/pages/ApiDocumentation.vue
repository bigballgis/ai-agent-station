<template>
  <div class="api-documentation">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">{{ t('routes.apiDocs') }}</h1>
        <p class="text-gray-600 dark:text-gray-400 mt-1">{{ t('apiDocs.desc') }}</p>
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

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      <span class="ml-3 text-gray-600 dark:text-gray-400">加载 OpenAPI 文档中...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6 text-center">
      <p class="text-red-600 dark:text-red-400 mb-2">加载 OpenAPI 文档失败</p>
      <p class="text-red-500 dark:text-red-500 text-sm">{{ loadError }}</p>
      <button
        @click="fetchOpenApiSpec"
        class="mt-4 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
      >
        重试
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div v-else>
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <div class="flex flex-col md:flex-row gap-4">
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('apiDocs.searchPlaceholder')"
            class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white"
          />
          <select v-model="selectedTag" class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white">
            <option value="">{{ t('apiDocs.allAgents') || '所有标签' }}</option>
            <option v-for="tag in tags" :key="tag" :value="tag">
              {{ tag }}
            </option>
          </select>
        </div>
      </div>

      <!-- API列表 -->
      <div class="space-y-6">
        <div v-for="(api, index) in filteredApis" :key="api.operationId || index" class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
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
                <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-1">{{ api.summary }}</h3>
                <p v-if="api.description" class="text-gray-600 dark:text-gray-400">{{ api.description }}</p>
                <div v-if="api.tags && api.tags.length" class="mt-2 flex gap-2">
                  <span v-for="tag in api.tags" :key="tag" class="inline-flex items-center px-2 py-0.5 rounded text-xs bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
                    {{ tag }}
                  </span>
                </div>
              </div>
              <button
                @click="toggleApiDetail(api.operationId || index)"
                class="ml-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              >
                <svg
                  v-if="expandedApis.includes(api.operationId || index)"
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
          <div v-if="expandedApis.includes(api.operationId || index)" class="p-6 border-b border-gray-200 dark:border-gray-700">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <!-- 请求参数 -->
              <div>
                <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">{{ t('apiDocs.requestParams') }}</h4>
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                  <pre class="text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">{{ api.requestExample }}</pre>
                </div>
              </div>
              <!-- 响应示例 -->
              <div>
                <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">{{ t('apiDocs.responseExample') }}</h4>
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                  <pre class="text-sm text-gray-800 dark:text-gray-200 overflow-x-auto">{{ api.responseExample }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getOpenApiSpec } from '@/api/openapi'

const { t } = useI18n()

const searchQuery = ref('')
const selectedTag = ref('')
const expandedApis = ref<string[]>([])
const loading = ref(false)
const loadError = ref('')

interface OpenApiParameter {
  name: string
  in: string
  description?: string
  required?: boolean
  schema?: Record<string, unknown>
  type?: string
}

interface OpenApiSchemaProperty {
  type?: string
  description?: string
  example?: unknown
  properties?: Record<string, OpenApiSchemaProperty>
  items?: OpenApiSchemaProperty
}

interface OpenApiRequestBody {
  content?: Record<string, {
    schema?: {
      $ref?: string
      properties?: Record<string, OpenApiSchemaProperty>
      example?: unknown
    }
    example?: unknown
  }>
}

interface OpenApiResponse {
  content?: Record<string, {
    schema?: {
      $ref?: string
      properties?: Record<string, OpenApiSchemaProperty>
    }
    example?: unknown
  }>
}

interface ParsedApiItem {
  operationId: string
  summary: string
  description: string
  method: string
  path: string
  tags: string[]
  parameters: OpenApiParameter[]
  requestBody?: OpenApiRequestBody
  responses?: Record<string, OpenApiResponse>
  requestExample: string
  responseExample: string
}

const apiList = ref<ParsedApiItem[]>([])
const tags = ref<string[]>([])

function extractSchemaName($ref: string): string {
  const parts = $ref.split('/')
  return parts[parts.length - 1]
}

function generateExampleFromSchema(schema: OpenApiSchemaProperty, componentsSchemas?: Record<string, OpenApiSchemaProperty>): unknown {
  if (schema.example !== undefined) return schema.example
  if (schema.properties) {
    const obj: Record<string, unknown> = {}
    for (const [key, prop] of Object.entries(schema.properties)) {
      obj[key] = generateExampleFromSchema(prop, componentsSchemas)
    }
    return obj
  }
  if (schema.$ref && componentsSchemas) {
    const name = extractSchemaName(schema.$ref)
    const resolved = componentsSchemas[name]
    if (resolved) return generateExampleFromSchema(resolved, componentsSchemas)
  }
  switch (schema.type) {
    case 'string': return 'string'
    case 'integer': case 'number': return 0
    case 'boolean': return true
    case 'array': return schema.items ? [generateExampleFromSchema(schema.items, componentsSchemas)] : []
    default: return null
  }
}

async function fetchOpenApiSpec() {
  loading.value = true
  loadError.value = ''

  try {
    const res = await getOpenApiSpec()
    const spec = res?.data || res

    if (!spec || !spec.paths) {
      loadError.value = '返回的 OpenAPI 文档格式无效'
      return
    }

    const componentsSchemas = spec.components?.schemas || {}
    const parsedApis: ParsedApiItem[] = []
    const tagSet = new Set<string>()

    for (const [path, methods] of Object.entries(spec.paths)) {
      for (const [method, detail] of Object.entries(methods as Record<string, Record<string, unknown>>)) {
        if (['get', 'post', 'put', 'delete', 'patch'].includes(method.toLowerCase())) {
          const op = detail as Record<string, unknown>
          const opTags = (op.tags as string[]) || []
          opTags.forEach(tag => tagSet.add(tag))

          // Extract request example
          let requestExample = '{}'
          const requestBody = op.requestBody as OpenApiRequestBody | undefined
          if (requestBody?.content) {
            const contentEntry = requestBody.content['application/json'] || Object.values(requestBody.content)[0]
            if (contentEntry?.example) {
              requestExample = JSON.stringify(contentEntry.example, null, 2)
            } else if (contentEntry?.schema) {
              let schema = contentEntry.schema
              if (schema.$ref && componentsSchemas) {
                const name = extractSchemaName(schema.$ref)
                schema = componentsSchemas[name] || schema
              }
              const example = generateExampleFromSchema(schema as OpenApiSchemaProperty, componentsSchemas)
              requestExample = JSON.stringify(example, null, 2)
            }
          }

          // Extract response example
          let responseExample = '{}'
          const responses = op.responses as Record<string, OpenApiResponse> | undefined
          if (responses) {
            const successResp = responses['200'] || responses['201'] || Object.values(responses)[0]
            if (successResp?.content) {
              const contentEntry = successResp.content['application/json'] || Object.values(successResp.content)[0]
              if (contentEntry?.example) {
                responseExample = JSON.stringify(contentEntry.example, null, 2)
              } else if (contentEntry?.schema) {
                let schema = contentEntry.schema
                if (schema.$ref && componentsSchemas) {
                  const name = extractSchemaName(schema.$ref)
                  schema = componentsSchemas[name] || schema
                }
                const example = generateExampleFromSchema(schema as OpenApiSchemaProperty, componentsSchemas)
                responseExample = JSON.stringify(example, null, 2)
              }
            }
          }

          parsedApis.push({
            operationId: (op.operationId as string) || `${method}_${path}`,
            summary: (op.summary as string) || '',
            description: (op.description as string) || '',
            method: method.toUpperCase(),
            path,
            tags: opTags,
            parameters: (op.parameters as OpenApiParameter[]) || [],
            requestBody,
            responses,
            requestExample,
            responseExample,
          })
        }
      }
    }

    apiList.value = parsedApis
    tags.value = Array.from(tagSet).sort()
  } catch (error: unknown) {
    const err = error instanceof Error ? error : new Error(String(error))
    loadError.value = err.message || '未知错误'
    message.error(t('apiDocs.fetchFailed'))
  } finally {
    loading.value = false
  }
}

const filteredApis = computed(() => {
  return apiList.value.filter((api) => {
    const matchesSearch = api.summary.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      api.description.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      api.path.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesTag = selectedTag.value === '' || api.tags.includes(selectedTag.value)

    return matchesSearch && matchesTag
  })
})

function getMethodColor(method: string) {
  switch (method) {
    case 'POST': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
    case 'GET': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
    case 'PUT': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
    case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
    case 'PATCH': return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200'
    default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200'
  }
}

function toggleApiDetail(apiId: string) {
  const index = expandedApis.value.indexOf(apiId)
  if (index > -1) {
    expandedApis.value.splice(index, 1)
  } else {
    expandedApis.value.push(apiId)
  }
}

onMounted(() => {
  fetchOpenApiSpec()
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
