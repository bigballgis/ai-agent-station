<template>
  <div class="api-documentation">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-100">{{ t('routes.apiDocs') }}</h1>
        <p class="text-neutral-600 dark:text-neutral-400 mt-1">{{ t('apiDocs.desc') }}</p>
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
          {{ t('apiDocs.openSwagger') }}
        </a>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      <span class="ml-3 text-neutral-600 dark:text-neutral-400">{{ t('apiDocs.loading') }}</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-6 text-center">
      <p class="text-red-600 dark:text-red-400 mb-2">{{ t('apiDocs.loadFailed') }}</p>
      <p class="text-red-500 dark:text-red-500 text-sm">{{ loadError }}</p>
      <button
        @click="fetchOpenApiSpec"
        class="mt-4 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
      >
        {{ t('apiDocs.retry') }}
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div v-else>
      <div class="bg-white dark:bg-neutral-900 rounded-lg shadow-sm border border-neutral-200 dark:border-neutral-700 p-6 mb-6">
        <div class="flex flex-col md:flex-row gap-4">
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('apiDocs.searchPlaceholder')"
            class="flex-1 px-4 py-2 border border-neutral-300 dark:border-neutral-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-neutral-800 dark:text-neutral-100"
          />
          <select v-model="selectedTag" class="px-4 py-2 border border-neutral-300 dark:border-neutral-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:bg-neutral-800 dark:text-neutral-100">
            <option value="">{{ t('apiDocs.allTags') }}</option>
            <option v-for="tag in tags" :key="tag" :value="tag">
              {{ tag }}
            </option>
          </select>
        </div>
      </div>

      <!-- API列表 -->
      <div class="space-y-6">
        <div v-for="(api, index) in filteredApis" :key="api.operationId || index" class="bg-white dark:bg-neutral-900 rounded-lg shadow-sm border border-neutral-200 dark:border-neutral-700 overflow-hidden">
          <div class="p-6 border-b border-neutral-200 dark:border-neutral-700">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <div class="flex items-center space-x-3 mb-2">
                  <span :class="[
                    'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium',
                    getMethodColor(api.method)
                  ]">
                    {{ api.method }}
                  </span>
                  <code class="text-sm text-neutral-800 dark:text-neutral-200 font-mono bg-neutral-100 dark:bg-neutral-800 px-3 py-1 rounded">
                    {{ api.path }}
                  </code>
                </div>
                <h3 class="text-lg font-medium text-neutral-900 dark:text-neutral-100 mb-1">{{ api.summary }}</h3>
                <p v-if="api.description" class="text-neutral-600 dark:text-neutral-400">{{ api.description }}</p>
                <div v-if="api.tags && api.tags.length" class="mt-2 flex gap-2">
                  <span v-for="tag in api.tags" :key="tag" class="inline-flex items-center px-2 py-0.5 rounded text-xs bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
                    {{ tag }}
                  </span>
                </div>
              </div>
              <button
                @click="toggleApiDetail(api.operationId || String(index))"
                class="ml-4 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
              >
                <svg
                  v-if="expandedApis.includes(api.operationId || String(index))"
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
          <div v-if="expandedApis.includes(api.operationId || String(index))" class="p-6 border-b border-neutral-200 dark:border-neutral-700">
            <!-- 认证与限流信息 -->
            <div v-if="hasSecurityOrRateLimit(api)" class="mb-4 flex flex-wrap gap-3">
              <span v-if="api.security && api.security.length" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200">
                <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                {{ formatSecurity(api.security) }}
              </span>
              <span v-if="api.rateLimit" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200">
                <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                {{ api.rateLimit }}
              </span>
              <span v-if="api.deprecated" class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200">
                Deprecated
              </span>
            </div>

            <!-- 请求参数列表 -->
            <div v-if="api.parameters && api.parameters.length" class="mb-4">
              <h4 class="text-sm font-medium text-neutral-900 dark:text-neutral-100 mb-2">{{ t('apiDocs.pathParams') }}</h4>
              <div class="overflow-x-auto">
                <table class="min-w-full text-sm">
                  <thead>
                    <tr class="border-b border-neutral-200 dark:border-neutral-600">
                      <th class="text-left py-1.5 px-3 text-neutral-500 dark:text-neutral-400 font-medium">{{ t('apiMgmt.paramCol') }}</th>
                      <th class="text-left py-1.5 px-3 text-neutral-500 dark:text-neutral-400 font-medium">{{ t('apiMgmt.paramLocation') }}</th>
                      <th class="text-left py-1.5 px-3 text-neutral-500 dark:text-neutral-400 font-medium">{{ t('apiMgmt.paramType') }}</th>
                      <th class="text-left py-1.5 px-3 text-neutral-500 dark:text-neutral-400 font-medium">{{ t('apiMgmt.paramRequired') }}</th>
                      <th class="text-left py-1.5 px-3 text-neutral-500 dark:text-neutral-400 font-medium">{{ t('apiMgmt.paramDescription') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="param in api.parameters" :key="param.name" class="border-b border-neutral-100 dark:border-neutral-700">
                      <td class="py-1.5 px-3 font-mono text-neutral-800 dark:text-neutral-200">{{ param.name }}</td>
                      <td class="py-1.5 px-3"><span class="px-2 py-0.5 rounded text-xs bg-neutral-100 dark:bg-neutral-700 text-neutral-600 dark:text-neutral-300">{{ param.in }}</span></td>
                      <td class="py-1.5 px-3 text-neutral-600 dark:text-neutral-400">{{ param.type || (param.schema && param.schema.type) || '-' }}</td>
                      <td class="py-1.5 px-3">
                        <span v-if="param.required" class="text-red-500 font-medium">{{ t('apiMgmt.yes') }}</span>
                        <span v-else class="text-neutral-400">{{ t('apiMgmt.no') }}</span>
                      </td>
                      <td class="py-1.5 px-3 text-neutral-600 dark:text-neutral-400">{{ param.description || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 请求/响应 Schema 和示例 -->
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <!-- 请求参数 -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <h4 class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ t('apiDocs.requestParams') }}</h4>
                  <div v-if="api.requestSchema" class="flex gap-2">
                    <button
                      @click="toggleSchemaView(api.operationId || String(index), 'request')"
                      class="text-xs px-2 py-0.5 rounded border border-neutral-300 dark:border-neutral-600 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-700"
                    >
                      {{ (schemaViewMode[api.operationId || String(index)] || {}).request === 'schema' ? 'Example' : 'Schema' }}
                    </button>
                  </div>
                </div>
                <div class="bg-neutral-50 dark:bg-neutral-800 rounded-lg p-4">
                  <pre v-if="!api.requestSchema || (schemaViewMode[api.operationId || String(index)] || {}).request !== 'schema'" class="text-sm text-neutral-800 dark:text-neutral-200 overflow-x-auto">{{ api.requestExample }}</pre>
                  <pre v-else class="text-sm text-neutral-800 dark:text-neutral-200 overflow-x-auto">{{ api.requestSchema }}</pre>
                </div>
              </div>
              <!-- 响应示例 -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <h4 class="text-sm font-medium text-neutral-900 dark:text-neutral-100">{{ t('apiDocs.responseExample') }}</h4>
                  <div v-if="api.responseSchema" class="flex gap-2">
                    <button
                      @click="toggleSchemaView(api.operationId || String(index), 'response')"
                      class="text-xs px-2 py-0.5 rounded border border-neutral-300 dark:border-neutral-600 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-700"
                    >
                      {{ (schemaViewMode[api.operationId || String(index)] || {}).response === 'schema' ? 'Example' : 'Schema' }}
                    </button>
                  </div>
                </div>
                <div class="bg-neutral-50 dark:bg-neutral-800 rounded-lg p-4">
                  <pre v-if="!api.responseSchema || (schemaViewMode[api.operationId || String(index)] || {}).response !== 'schema'" class="text-sm text-neutral-800 dark:text-neutral-200 overflow-x-auto">{{ api.responseExample }}</pre>
                  <pre v-else class="text-sm text-neutral-800 dark:text-neutral-200 overflow-x-auto">{{ api.responseSchema }}</pre>
                </div>
              </div>
            </div>

            <!-- 错误响应 -->
            <div v-if="api.errorResponses && api.errorResponses.length" class="mt-4">
              <h4 class="text-sm font-medium text-neutral-900 dark:text-neutral-100 mb-2">{{ t('apiDocs.errorResponses') }}</h4>
              <div class="space-y-2">
                <div v-for="err in api.errorResponses" :key="err.code" class="flex items-start gap-3 bg-red-50 dark:bg-red-900/20 rounded-lg p-3">
                  <span :class="[
                    'inline-flex items-center px-2 py-0.5 rounded text-xs font-bold',
                    err.code >= 500 ? 'bg-red-200 text-red-800 dark:bg-red-800 dark:text-red-200' : 'bg-yellow-200 text-yellow-800 dark:bg-yellow-800 dark:text-yellow-200'
                  ]">{{ err.code }}</span>
                  <div>
                    <p class="text-sm font-medium text-neutral-800 dark:text-neutral-200">{{ err.description || '' }}</p>
                    <p v-if="err.example" class="text-xs text-neutral-500 dark:text-neutral-400 mt-1 font-mono">{{ err.example }}</p>
                  </div>
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
const schemaViewMode = ref<Record<string, { request?: string; response?: string }>>({})

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
  $ref?: string
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
  requestSchema: string
  responseSchema: string
  security: Record<string, string[]>[]
  rateLimit: string
  deprecated: boolean
  errorResponses: { code: number; description: string; example: string }[]
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
      loadError.value = t('apiDocs.invalidFormat')
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
            requestSchema: extractSchemaString(requestBody, componentsSchemas),
            responseSchema: extractSchemaString(successResp, componentsSchemas),
            security: (op.security as Record<string, string[]>[]) || [],
            rateLimit: extractRateLimit(detail as Record<string, unknown>, spec as Record<string, unknown>),
            deprecated: (op.deprecated as boolean) || false,
            errorResponses: extractErrorResponses(responses, componentsSchemas),
          })
        }
      }
    }

    apiList.value = parsedApis
    tags.value = Array.from(tagSet).sort()
  } catch (error: unknown) {
    const err = error instanceof Error ? error : new Error(String(error))
    loadError.value = err.message || t('apiDocs.unknownError')
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
    default: return 'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200'
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

function toggleSchemaView(apiId: string, type: 'request' | 'response') {
  if (!schemaViewMode.value[apiId]) {
    schemaViewMode.value[apiId] = {}
  }
  schemaViewMode.value[apiId][type] = schemaViewMode.value[apiId][type] === 'schema' ? 'example' : 'schema'
}

function hasSecurityOrRateLimit(api: ParsedApiItem): boolean {
  return (api.security && api.security.length > 0) || !!api.rateLimit || !!api.deprecated
}

function formatSecurity(security: Record<string, string[]>[]): string {
  if (!security || !security.length) return ''
  const schemes = security.map(s => Object.keys(s).join(', ')).join(', ')
  return 'Auth: ' + schemes
}

function extractSchemaString(
  contentEntry: { schema?: { $ref?: string; properties?: Record<string, OpenApiSchemaProperty> }; example?: unknown } | undefined,
  componentsSchemas: Record<string, OpenApiSchemaProperty>
): string {
  if (!contentEntry?.schema) return ''
  let schema = contentEntry.schema
  if (schema.$ref && componentsSchemas) {
    const name = extractSchemaName(schema.$ref)
    schema = componentsSchemas[name] || schema
  }
  if (schema.properties) {
    const props: string[] = []
    for (const [key, prop] of Object.entries(schema.properties)) {
      const typeStr = prop.type || (prop.$ref ? extractSchemaName(prop.$ref) : 'object')
      const req = (schema as unknown as { required?: string[] }).required?.includes(key) ? ' (required)' : ''
      const desc = prop.description ? ` - ${prop.description}` : ''
      props.push(`  ${key}: ${typeStr}${req}${desc}`)
    }
    return '{\n' + props.join('\n') + '\n}'
  }
  return ''
}

function extractRateLimit(
  opDetail: Record<string, unknown>,
  spec: Record<string, unknown>
): string {
  // Check x-rateLimit extension on operation
  const opLimit = opDetail['x-rateLimit'] || opDetail['x-rate-limit']
  if (opLimit) return String(opLimit)
  // Check global security / extensions
  const globalLimit = spec['x-rateLimit'] || spec['x-rate-limit']
  if (globalLimit) return String(globalLimit)
  return ''
}

function extractErrorResponses(
  responses: Record<string, OpenApiResponse> | undefined,
  componentsSchemas: Record<string, OpenApiSchemaProperty>
): { code: number; description: string; example: string }[] {
  if (!responses) return []
  const errors: { code: number; description: string; example: string }[] = []
  const errorCodes = ['400', '401', '403', '404', '409', '422', '429', '500', '502', '503']
  for (const code of errorCodes) {
    const resp = responses[code]
    if (resp) {
      let example = ''
      if (resp.content) {
        const entry = resp.content['application/json'] || Object.values(resp.content)[0]
        if (entry?.example) {
          example = JSON.stringify(entry.example)
        } else if (entry?.schema) {
          const ex = generateExampleFromSchema(entry.schema as OpenApiSchemaProperty, componentsSchemas)
          example = JSON.stringify(ex)
        }
      }
      errors.push({
        code: parseInt(code),
        description: (resp as unknown as { description?: string }).description || '',
        example: example.length > 200 ? example.substring(0, 200) + '...' : example,
      })
    }
  }
  return errors
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
