<template>
  <div class="memory-management-page">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            {{ t('routes.memoryManagement') }}
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            {{ t('memory.managementDesc') }}
          </p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800/40 hover:bg-red-100 dark:hover:bg-red-950/50 transition-all duration-200 cursor-pointer"
          @click="cleanExpiredMemories"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          {{ t('memory.cleanExpired') }}
        </button>
      </div>
    </div>

    <!-- Agent 选择器 + 筛选栏 -->
    <div class="mb-6 flex flex-wrap items-center gap-3 animate-slide-up">
      <!-- Agent 选择器 -->
      <select
        v-model="selectedAgent"
        aria-label="Agent"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[160px]"
      >
        <option value="">{{ t('memory.allAgents') }}</option>
        <option v-for="agent in agentList" :key="agent.id" :value="agent.id">{{ agent.name }}</option>
      </select>

      <!-- 记忆类型筛选 -->
      <div class="flex items-center gap-1.5 bg-neutral-100 dark:bg-neutral-800/60 rounded-xl p-1">
        <button
          v-for="type in memoryTypes"
          :key="type.value"
          class="px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer"
          :class="activeType === type.value
            ? 'bg-white dark:bg-neutral-700 text-neutral-900 dark:text-neutral-100 shadow-sm'
            : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300'"
          @click="activeType = type.value"
        >
          {{ type.label }}
        </button>
      </div>

      <!-- 搜索框 -->
      <div class="relative flex-1 min-w-[200px] max-w-sm">
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
          :placeholder="t('memory.searchPlaceholder')"
          class="w-full pl-10 pr-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
        />
      </div>

      <!-- 时间范围 -->
      <select
        v-model="timeRange"
        aria-label="Time range"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
      >
        <option value="">{{ t('memory.allTime') }}</option>
        <option value="1h">{{ t('memory.last1Hour') }}</option>
        <option value="24h">{{ t('memory.last24Hours') }}</option>
        <option value="7d">{{ t('memory.last7Days') }}</option>
        <option value="30d">{{ t('memory.last30Days') }}</option>
      </select>
    </div>

    <!-- 空状态 -->
    <div
      v-if="loading"
      class="py-8"
    >
      <LoadingSkeleton type="card" :rows="3" />
    </div>
    <div
      v-else-if="filteredMemories.length === 0"
      class="flex flex-col items-center justify-center py-20 animate-fade-in"
    >
      <div class="w-20 h-20 rounded-2xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center mb-5">
        <svg class="w-10 h-10 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
      <h3 class="text-base font-semibold text-neutral-700 dark:text-neutral-300 mb-1">{{ t('memory.noMemory') }}</h3>
      <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('memory.noMemoryHint') }}</p>
    </div>

    <!-- 记忆列表 -->
    <div v-else class="space-y-3">
      <div
        v-for="(memory, index) in paginatedMemories"
        :key="memory.id"
        class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-5 hover:-translate-y-0.5 hover:shadow-float transition-all duration-200 animate-slide-up"
        :style="{ animationDelay: `${index * 50}ms` }"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <!-- 记忆内容 -->
            <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed mb-3 line-clamp-2">
              {{ memory.content }}
            </p>

            <!-- 标签行 -->
            <div class="flex flex-wrap items-center gap-3">
              <!-- 类型标签 -->
              <span
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="getTypeBadgeClass(memory.type)"
              >
                {{ getTypeLabel(memory.type) }}
              </span>

              <!-- 重要性评分 -->
              <div class="flex items-center gap-0.5">
                <svg
                  v-for="star in 5"
                  :key="star"
                  class="w-3.5 h-3.5"
                  :class="star <= memory.importance ? 'text-amber-400' : 'text-neutral-200 dark:text-neutral-700'"
                  fill="currentColor" viewBox="0 0 20 20"
                >
                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                </svg>
                <span class="text-xs text-neutral-400 dark:text-neutral-500 ml-1">{{ memory.importance }}</span>
              </div>

              <!-- 访问次数 -->
              <span class="flex items-center gap-1 text-xs text-neutral-400 dark:text-neutral-500">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ t('memory.times', { count: memory.accessCount }) }}
              </span>

              <!-- 创建时间 -->
              <span class="text-xs text-neutral-400 dark:text-neutral-500">
                {{ memory.createdAt }}
              </span>

              <!-- Agent -->
              <span class="text-xs text-neutral-400 dark:text-neutral-500">
                {{ memory.agentName }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center gap-1 flex-shrink-0">
            <button
              class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
              :title="t('common.view')"
              @click="viewMemoryDetail(memory)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
            <button
              class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
              :title="t('common.delete')"
              @click="deleteMemory(memory)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div
      v-if="filteredMemories.length > pageSize"
      class="mt-8 flex items-center justify-center gap-2 animate-fade-in"
    >
      <button
        class="px-3.5 py-2 rounded-xl text-sm font-medium border transition-all duration-200 cursor-pointer"
        :class="currentPage > 1
          ? 'border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 hover:bg-neutral-50 dark:hover:bg-neutral-800'
          : 'border-neutral-100 dark:border-neutral-800 text-neutral-300 dark:text-neutral-600 cursor-not-allowed'"
        :disabled="currentPage <= 1"
        @click="currentPage--"
      >
        {{ t('common.prevPage') }}
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
        {{ t('common.nextPage') }}
      </button>
    </div>

    <!-- 记忆详情弹窗 -->
    <a-modal
      v-model:open="showDetailModal"
      :title="t('memory.detail')"
      :footer="null"
      width="640px"
    >
      <div v-if="detailMemory" class="mt-4 space-y-4">
        <div>
          <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-2">{{ t('memory.content') }}</h4>
          <div class="bg-neutral-50 dark:bg-neutral-800/60 rounded-xl p-4">
            <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-wrap">{{ detailMemory.content }}</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.type') }}</h4>
            <span
              class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
              :class="getTypeBadgeClass(detailMemory.type)"
            >
              {{ getTypeLabel(detailMemory.type) }}
            </span>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.agent') }}</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailMemory.agentName }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.importance') }}</h4>
            <div class="flex items-center gap-0.5">
              <svg
                v-for="star in 5"
                :key="star"
                class="w-4 h-4"
                :class="star <= detailMemory.importance ? 'text-amber-400' : 'text-neutral-200 dark:text-neutral-700'"
                fill="currentColor" viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
              <span class="text-sm text-neutral-600 dark:text-neutral-400 ml-1">{{ detailMemory.importance }}/5</span>
            </div>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.accessCount') }}</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ t('memory.times', { count: detailMemory.accessCount }) }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.createdAt') }}</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailMemory.createdAt }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.lastAccessedAt') }}</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailMemory.lastAccessedAt }}</p>
          </div>
        </div>
        <div>
          <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('memory.tags') }}</h4>
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in detailMemory.tags"
              :key="tag"
              class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400"
            >
              {{ tag }}
            </span>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { getAgentMemories, deleteMemory as deleteMemoryApi, cleanupAgentMemories } from '@/api/memory'
import { getAllAgents } from '@/api/agent'
import { LoadingSkeleton } from '@/components'
import { logger } from '@/utils/logger'

const { t } = useI18n()

// Agent 列表
const agentList = ref<{ id: string; name: string }[]>([])
const selectedAgent = ref('')

// 记忆类型
const memoryTypes = [
  { label: t('memory.allTypes'), value: '' },
  { label: t('memory.shortTerm'), value: 'short_term' },
  { label: t('memory.longTerm'), value: 'long_term' },
  { label: t('memory.businessMemory'), value: 'business' },
]
const activeType = ref('')
const searchQuery = ref('')
const timeRange = ref('')

// 分页
const currentPage = ref(1)
const pageSize = 10

interface MemoryItem {
  id: number | string
  content: string
  type: string
  agentName?: string
  tags: string[]
  [key: string]: unknown
}

// 记忆数据
const memories = ref<MemoryItem[]>([])
const loading = ref(false)

async function fetchAgents() {
  try {
    const res = await getAllAgents()
    agentList.value = (res.data || res || []).map((a: Record<string, unknown>) => ({
      id: String(a.id),
      name: (a.name || '') as string,
    }))
  } catch (e) {
    logger.error('获取 Agent 列表失败:', e)
  }
}

async function fetchMemories() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 1, size: 100 }
    if (selectedAgent.value) params.agentId = selectedAgent.value
    if (activeType.value) params.memoryType = activeType.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const res = await getAgentMemories(selectedAgent.value || 'all', params)
    memories.value = res.data?.records || res.data || res || []
  } catch (e: unknown) {
    logger.error('获取记忆列表失败:', e)
    message.error(t('memory.fetchMemoriesFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchAgents()
  await fetchMemories()
})

// 监听筛选条件变化，重新获取数据
watch(selectedAgent, () => {
  currentPage.value = 1
  fetchMemories()
})
watch(activeType, () => {
  currentPage.value = 1
  fetchMemories()
})
watch(searchQuery, () => {
  currentPage.value = 1
  fetchMemories()
})

// 筛选
const filteredMemories = computed(() => {
  let result = memories.value

  if (selectedAgent.value) {
    const agent = agentList.value.find(a => a.id === selectedAgent.value)
    if (agent) {
      result = result.filter(m => m.agentName === agent.name)
    }
  }

  if (activeType.value) {
    result = result.filter(m => m.type === activeType.value)
  }

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(m =>
      m.content.toLowerCase().includes(q) ||
      m.tags.some((t: string) => t.toLowerCase().includes(q))
    )
  }

  return result
})

// 分页
const totalPages = computed(() => Math.ceil(filteredMemories.value.length / pageSize))

const paginatedMemories = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMemories.value.slice(start, start + pageSize)
})

function shouldShowPage(page: number): boolean {
  if (page === 1 || page === totalPages.value) return true
  if (Math.abs(page - currentPage.value) <= 1) return true
  return false
}

// 辅助函数
function getTypeBadgeClass(type: string): string {
  const map: Record<string, string> = {
    short_term: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    long_term: 'bg-purple-50 dark:bg-purple-950/30 text-purple-600 dark:text-purple-400',
    business: 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400',
  }
  return map[type] || ''
}

function getTypeLabel(type: string): string {
  const map: Record<string, string> = {
    short_term: t('memory.shortTerm'),
    long_term: t('memory.longTerm'),
    business: t('memory.businessMemory'),
  }
  return map[type] || type
}

// 操作
const showDetailModal = ref(false)
const detailMemory = ref<typeof memories.value[0] | null>(null)

function viewMemoryDetail(memory: typeof memories.value[0]) {
  detailMemory.value = memory
  showDetailModal.value = true
}

function deleteMemory(memory: typeof memories.value[0]) {
  Modal.confirm({
    title: t('memory.deleteConfirmTitle'),
    content: t('memory.deleteConfirmContent'),
    okText: t('memory.deleteConfirmOk'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await deleteMemoryApi(memory.id)
        message.success(t('memory.deleteSuccess'))
        await fetchMemories()
      } catch (e: unknown) {
        logger.error('删除记忆失败:', e)
        message.error(t('memory.deleteFailed'))
      }
    },
  })
}

function cleanExpiredMemories() {
  Modal.confirm({
    title: t('memory.cleanConfirmTitle'),
    content: t('memory.cleanConfirmContent'),
    okText: t('memory.cleanConfirmOk'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await cleanupAgentMemories(selectedAgent.value || 'all')
        message.success(t('memory.cleanSuccess'))
        await fetchMemories()
      } catch (e: unknown) {
        logger.error('清理过期记忆失败:', e)
        message.error(t('memory.cleanFailed'))
      }
    },
  })
}
</script>

<style scoped>
.memory-management-page {
  padding: 0;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
