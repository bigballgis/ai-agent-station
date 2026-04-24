<template>
  <div class="test-case-list-page">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            {{ t('test.caseManagement') }}
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            {{ t('test.caseManagementDesc') }}
          </p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="handleCreateTestCase"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ t('test.createCase') }}
        </button>
      </div>
    </div>

    <!-- 搜索/筛选栏 -->
    <div class="mb-6 flex flex-wrap items-center gap-3 animate-slide-up">
      <!-- 搜索框 -->
      <div class="relative flex-1 min-w-[240px] max-w-md">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 dark:text-neutral-500"
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          :placeholder="t('test.searchPlaceholder')"
          class="w-full pl-10 pr-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          @input="handleSearch"
        />
      </div>

      <!-- 状态筛选 -->
      <select
        v-model="statusFilter"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
        @change="handleSearch"
      >
        <option value="">{{ t('test.allStatus') }}</option>
        <option value="active">{{ t('test.enabled') }}</option>
        <option value="inactive">{{ t('test.disabled') }}</option>
      </select>

      <!-- Agent 筛选 -->
      <select
        v-model="agentFilter"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[160px]"
        @change="handleSearch"
      >
        <option value="">{{ t('test.allAgents') }}</option>
        <option
          v-for="agent in agentOptions"
          :key="agent.id"
          :value="agent.id"
        >
          {{ agent.name }}
        </option>
      </select>

      <!-- 测试类型筛选 -->
      <select
        v-model="typeFilter"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
        @change="handleSearch"
      >
        <option value="">{{ t('test.allTypes') }}</option>
        <option value="unit">{{ t('test.typeUnit') }}</option>
        <option value="integration">{{ t('test.typeIntegration') }}</option>
        <option value="e2e">{{ t('test.typeE2e') }}</option>
        <option value="performance">{{ t('test.typePerformance') }}</option>
      </select>
    </div>

    <!-- 表格容器 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card overflow-hidden animate-slide-up">
      <!-- 加载状态 -->
      <div v-if="loading" class="p-6 space-y-4">
        <div v-for="i in 5" :key="i" class="flex items-center gap-4 animate-pulse">
          <div class="h-4 w-16 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 flex-1 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-20 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-16 rounded-full bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-24 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-24 rounded bg-neutral-200 dark:bg-neutral-700"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <div
        v-else-if="filteredTestCases.length === 0"
        class="flex flex-col items-center justify-center py-20"
      >
        <div class="w-20 h-20 rounded-2xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center mb-5">
          <svg class="w-10 h-10 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
        </div>
        <h3 class="text-base font-semibold text-neutral-700 dark:text-neutral-300 mb-1">{{ t('test.noCases') }}</h3>
        <p class="text-sm text-neutral-400 dark:text-neutral-500 mb-5">{{ t('test.createFirstCaseDesc') }}</p>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="handleCreateTestCase"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ t('test.createFirstCase') }}
        </button>
      </div>

      <!-- 表格 -->
      <a-table
        v-else
        :columns="columns"
        :data-source="paginatedTestCases"
        row-key="id"
        :pagination="false"
        :row-class-name="() => 'test-case-row'"
      >
        <!-- 用例名称 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="flex items-center gap-3">
              <div
                class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                :class="getTypeIconBg(record.testType)"
              >
                <svg
                  class="w-4 h-4"
                  :class="getTypeIconColor(record.testType)"
                  fill="none" stroke="currentColor" viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="min-w-0">
                <p class="text-sm font-medium text-neutral-900 dark:text-neutral-50 truncate">
                  {{ record.name }}
                </p>
                <p class="text-xs text-neutral-400 dark:text-neutral-500 truncate">
                  {{ record.description || t('test.noDescription') }}
                </p>
              </div>
            </div>
          </template>

          <!-- 关联 Agent -->
          <template v-else-if="column.key === 'agent'">
            <span class="text-sm text-neutral-600 dark:text-neutral-400">
              {{ getAgentName(record.agentId) }}
            </span>
          </template>

          <!-- 测试类型 -->
          <template v-else-if="column.key === 'testType'">
            <span
              class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
              :class="getTestTypeBadgeClass(record.testType)"
            >
              {{ getTestTypeLabel(record.testType) }}
            </span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <span
              class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium"
              :class="record.isActive
                ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'
                : 'bg-red-50 dark:bg-red-950/30 text-red-500 dark:text-red-400'"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="record.isActive ? 'bg-green-500' : 'bg-red-500'"
              ></span>
              {{ record.isActive ? t('test.enabled') : t('test.disabled') }}
            </span>
          </template>

          <!-- 创建时间 -->
          <template v-else-if="column.key === 'createdAt'">
            <span class="text-sm text-neutral-500 dark:text-neutral-400">
              {{ formatDate(record.createdAt) }}
            </span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <div class="flex items-center gap-1">
              <button
                class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
                :title="t('test.editCase')"
                @click="handleEditTestCase(record.id!)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button
                class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 transition-all duration-200 cursor-pointer"
                :title="t('test.testCaseVersions')"
                @click="handleViewVersions(record.id!)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </button>
              <button
                class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-950/30 transition-all duration-200 cursor-pointer"
                :title="t('test.runTest')"
                @click="handleRunTestCase(record.id!)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </button>
              <button
                class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
                :title="t('common.delete')"
                @click="handleDeleteTestCase(record.id!)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 分页 -->
    <div
      v-if="!loading && filteredTestCases.length > pageSize"
      class="mt-6 flex items-center justify-between animate-fade-in"
    >
      <span class="text-sm text-neutral-500 dark:text-neutral-400">
        {{ t('test.paginationInfo', { total: filteredTestCases.length, current: currentPage, totalPage: totalPages }) }}
      </span>
      <div class="flex items-center gap-2">
        <button
          class="px-3.5 py-2 rounded-xl text-sm font-medium border transition-all duration-200 cursor-pointer"
          :class="currentPage > 1
            ? 'border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 hover:bg-neutral-50 dark:hover:bg-neutral-800'
            : 'border-neutral-100 dark:border-neutral-800 text-neutral-300 dark:text-neutral-600 cursor-not-allowed'"
          :disabled="currentPage <= 1"
          @click="currentPage--"
        >
          {{ t('test.prevPage') }}
        </button>
        <template v-for="page in totalPages" :key="page">
          <button
            v-if="shouldShowPage(page)"
            class="w-9 h-9 rounded-xl text-sm font-medium transition-all duration-200 cursor-pointer"
            :class="page === currentPage
              ? 'bg-primary-500 text-white shadow-sm'
              : 'text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800'"
            @click="currentPage = page"
          >
            {{ page }}
          </button>
        </template>
        <button
          class="px-3.5 py-2 rounded-xl text-sm font-medium border transition-all duration-200 cursor-pointer"
          :class="currentPage < totalPages
            ? 'border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 hover:bg-neutral-50 dark:hover:bg-neutral-800'
            : 'border-neutral-100 dark:border-neutral-800 text-neutral-300 dark:text-neutral-600 cursor-not-allowed'"
          :disabled="currentPage >= totalPages"
          @click="currentPage++"
        >
          {{ t('test.nextPage') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { testApi, type TestCase } from '@/api/test'
import { agentApi, type Agent } from '@/api/agent'
import { logger } from '@/utils/logger'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const testCases = ref<TestCase[]>([])
const agents = ref<Agent[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const agentFilter = ref<number | string>('')
const typeFilter = ref('')

// 分页
const currentPage = ref(1)
const pageSize = 10

// Agent 选项（用于筛选下拉）
const agentOptions = computed(() => agents.value)

// 筛选后的列表
const filteredTestCases = computed(() => {
  let result = testCases.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(
      (tc) =>
        tc.name?.toLowerCase().includes(query) ||
        tc.description?.toLowerCase().includes(query)
    )
  }

  if (statusFilter.value) {
    const isActive = statusFilter.value === 'active'
    result = result.filter((tc) => tc.isActive === isActive)
  }

  if (agentFilter.value !== '') {
    result = result.filter((tc) => tc.agentId === Number(agentFilter.value))
  }

  if (typeFilter.value) {
    result = result.filter((tc) => tc.testType === typeFilter.value)
  }

  return result
})

// 分页数据
const totalPages = computed(() => Math.ceil(filteredTestCases.value.length / pageSize))

const paginatedTestCases = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredTestCases.value.slice(start, start + pageSize)
})

function shouldShowPage(page: number): boolean {
  if (page === 1 || page === totalPages.value) return true
  if (Math.abs(page - currentPage.value) <= 1) return true
  return false
}

// 表格列定义
const columns = computed(() => [
  {
    title: t('test.caseName'),
    key: 'name',
    dataIndex: 'name',
    width: 280
  },
  {
    title: t('test.relatedAgent'),
    key: 'agent',
    dataIndex: 'agentId',
    width: 140
  },
  {
    title: t('test.testType'),
    key: 'testType',
    dataIndex: 'testType',
    width: 120
  },
  {
    title: t('test.status'),
    key: 'status',
    dataIndex: 'isActive',
    width: 100
  },
  {
    title: t('test.createdAt'),
    key: 'createdAt',
    dataIndex: 'createdAt',
    width: 140
  },
  {
    title: t('test.actions'),
    key: 'action',
    width: 180,
    fixed: 'right' as const
  }
])

// 加载数据
async function fetchTestCases() {
  loading.value = true
  try {
    const response = await testApi.getAllTestCases()
    testCases.value = response.data || []
  } catch (error) {
    message.error(t('test.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchAgents() {
  try {
    const res = await agentApi.getAllAgents()
    agents.value = res.data || []
  } catch (error) {
    logger.warn('Secondary operation failed:', error)
  }
}

function handleSearch() {
  currentPage.value = 1
}

// 操作函数
function handleCreateTestCase() {
  router.push('/test-cases/edit')
}

function handleEditTestCase(id: number) {
  router.push(`/test-cases/edit/${id}`)
}

function handleViewVersions(id: number) {
  router.push(`/test-cases/versions/${id}`)
}

function handleDeleteTestCase(id: number) {
  Modal.confirm({
    title: t('test.confirmDelete'),
    content: t('test.confirmDeleteContent'),
    okText: t('test.confirmDelete'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    onOk: () => {
      testApi.deleteTestCase(id)
        .then(() => {
          message.success(t('test.deleteSuccess'))
          fetchTestCases()
        })
        .catch(() => {
          message.error(t('test.deleteFailed'))
        })
    }
  })
}

function handleRunTestCase(id: number) {
  testApi.createTestExecution({ testCaseId: id })
    .then(() => {
      message.success(t('test.testStarted'))
      router.push('/test-executions')
    })
    .catch(() => {
      message.error(t('test.startTestFailed'))
    })
}

// 辅助函数
function formatDate(date: string | undefined) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function getAgentName(agentId: number | undefined): string {
  if (!agentId) return '-'
  const agent = agents.value.find((a) => a.id === agentId)
  return agent?.name || `Agent #${agentId}`
}

function getTestTypeLabel(testType: string | undefined): string {
  const typeMap: Record<string, string> = {
    unit: t('test.typeUnit'),
    integration: t('test.typeIntegration'),
    e2e: t('test.typeE2e'),
    performance: t('test.typePerformance'),
    api: t('test.typeApi'),
    smoke: t('test.typeSmoke')
  }
  return typeMap[testType || ''] || testType || t('test.typeUnknown')
}

function getTestTypeBadgeClass(testType: string | undefined): string {
  const classMap: Record<string, string> = {
    unit: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    integration: 'bg-purple-50 dark:bg-purple-950/30 text-purple-600 dark:text-purple-400',
    e2e: 'bg-orange-50 dark:bg-orange-950/30 text-orange-600 dark:text-orange-400',
    performance: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    api: 'bg-cyan-50 dark:bg-cyan-950/30 text-cyan-600 dark:text-cyan-400',
    smoke: 'bg-rose-50 dark:bg-rose-950/30 text-rose-600 dark:text-rose-400'
  }
  return classMap[testType || ''] || 'bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400'
}

function getTypeIconBg(testType: string | undefined): string {
  const bgMap: Record<string, string> = {
    unit: 'bg-blue-100 dark:bg-blue-900/40',
    integration: 'bg-purple-100 dark:bg-purple-900/40',
    e2e: 'bg-orange-100 dark:bg-orange-900/40',
    performance: 'bg-amber-100 dark:bg-amber-900/40',
    api: 'bg-cyan-100 dark:bg-cyan-900/40',
    smoke: 'bg-rose-100 dark:bg-rose-900/40'
  }
  return bgMap[testType || ''] || 'bg-neutral-100 dark:bg-neutral-800'
}

function getTypeIconColor(testType: string | undefined): string {
  const colorMap: Record<string, string> = {
    unit: 'text-blue-600 dark:text-blue-400',
    integration: 'text-purple-600 dark:text-purple-400',
    e2e: 'text-orange-600 dark:text-orange-400',
    performance: 'text-amber-600 dark:text-amber-400',
    api: 'text-cyan-600 dark:text-cyan-400',
    smoke: 'text-rose-600 dark:text-rose-400'
  }
  return colorMap[testType || ''] || 'text-neutral-500 dark:text-neutral-400'
}

onMounted(() => {
  fetchTestCases()
  fetchAgents()
})
</script>

<style scoped>
.test-case-list-page {
  padding: 0;
}

/* Ant Design 表格样式覆盖 - 统一风格 */
:deep(.ant-table) {
  background: transparent;
}

:deep(.ant-table-thead > tr > th) {
  background: transparent;
  border-bottom: 1px solid #f5f5f5;
  font-weight: 600;
  font-size: 13px;
  color: #737373;
  padding: 14px 16px;
}

:deep(.dark .ant-table-thead > tr > th) {
  border-bottom-color: #262626;
  color: #a3a3a3;
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f5f5f5;
  padding: 14px 16px;
}

:deep(.dark .ant-table-tbody > tr > td) {
  border-bottom-color: #262626;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: #fafafa;
}

:deep(.dark .ant-table-tbody > tr:hover > td) {
  background: rgba(38, 38, 38, 0.5);
}

:deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

:deep(.ant-empty-description) {
  color: #a3a3a3;
}

:deep(.dark .ant-empty-description) {
  color: #525252;
}
</style>
