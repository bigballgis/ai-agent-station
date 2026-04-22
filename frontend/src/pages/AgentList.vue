<template>
  <div class="agent-list-page">
    <!-- 页面头部 -->
    <PageHeader title="Agent管理" subtitle="管理和监控所有 AI Agent，支持创建、编辑、版本管理和发布">
      <template #actions>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="showCreateModal = true"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          创建 Agent
        </button>
      </template>
    </PageHeader>

    <!-- 搜索/筛选栏 -->
    <div class="mb-6 animate-slide-up">
      <SearchBar :fields="searchFields" @search="handleSearch" @reset="handleReset" />
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      <div
        v-for="i in 3"
        :key="i"
        class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-5 animate-pulse"
      >
        <div class="flex items-center justify-between mb-4">
          <div class="w-10 h-10 rounded-xl bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-6 w-16 rounded-full bg-neutral-200 dark:bg-neutral-700"></div>
        </div>
        <div class="h-5 w-3/4 rounded bg-neutral-200 dark:bg-neutral-700 mb-2"></div>
        <div class="h-4 w-full rounded bg-neutral-100 dark:bg-neutral-800 mb-1"></div>
        <div class="h-4 w-2/3 rounded bg-neutral-100 dark:bg-neutral-800 mb-4"></div>
        <div class="flex gap-2 mb-4">
          <div class="h-6 w-14 rounded-full bg-neutral-100 dark:bg-neutral-800"></div>
          <div class="h-6 w-14 rounded-full bg-neutral-100 dark:bg-neutral-800"></div>
        </div>
        <div class="border-t border-neutral-100 dark:border-neutral-800 pt-4 flex items-center justify-between">
          <div class="h-3 w-24 rounded bg-neutral-100 dark:bg-neutral-800"></div>
          <div class="flex gap-2">
            <div class="h-8 w-16 rounded-lg bg-neutral-100 dark:bg-neutral-800"></div>
            <div class="h-8 w-16 rounded-lg bg-neutral-100 dark:bg-neutral-800"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="filteredAgents.length === 0"
      type="noData"
      description="点击下方按钮创建您的第一个 Agent"
      action-text="创建第一个 Agent"
      class="py-20 animate-fade-in"
      @action="showCreateModal = true"
    />

    <!-- Agent 卡片网格 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      <div
        v-for="(agent, index) in paginatedAgents"
        :key="agent.id"
        class="agent-card bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-5 hover:-translate-y-1 hover:shadow-float transition-all duration-200 cursor-pointer group animate-slide-up"
        :style="{ animationDelay: `${index * 60}ms` }"
      >
        <!-- 顶部: 类型图标 + 状态标签 -->
        <div class="flex items-center justify-between mb-4">
          <div
            class="w-10 h-10 rounded-xl flex items-center justify-center transition-colors duration-200"
            :class="getAgentIconBg(agent)"
          >
            <svg
              class="w-5 h-5"
              :class="getAgentIconColor(agent)"
              fill="none" stroke="currentColor" viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <StatusBadge :status="agent.status || (agent.isActive ? 'PUBLISHED' : 'DRAFT')" type="agent" />
        </div>

        <!-- 标题 -->
        <h3 class="text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-1.5 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors duration-200">
          {{ agent.name }}
        </h3>

        <!-- 描述: 两行截断 -->
        <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-4 line-clamp-2 leading-relaxed">
          {{ agent.description || '暂无描述' }}
        </p>

        <!-- 标签列表 -->
        <div v-if="getAgentTags(agent).length" class="flex flex-wrap gap-1.5 mb-4">
          <span
            v-for="tag in getAgentTags(agent)"
            :key="tag"
            class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400"
          >
            {{ tag }}
          </span>
        </div>

        <!-- 底部: 创建时间 + 操作按钮 -->
        <div class="border-t border-neutral-100 dark:border-neutral-800 pt-4 flex items-center justify-between">
          <span class="text-xs text-neutral-400 dark:text-neutral-500">
            {{ formatDate(agent.createdAt) }}
          </span>
          <div class="flex items-center gap-1.5">
            <button
              class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
              title="编辑"
              @click.stop="editAgent(agent)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button
              class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 transition-all duration-200 cursor-pointer"
              title="版本"
              @click.stop="viewVersions(agent)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </button>
            <button
              class="p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
              title="删除"
              @click.stop="deleteAgent(agent)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div
      v-if="!loading && filteredAgents.length > pageSize"
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
        上一页
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
        下一页
      </button>
    </div>

    <!-- 创建 Agent 弹窗 -->
    <a-modal
      v-model:open="showCreateModal"
      title="创建 Agent"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
      @ok="handleCreate"
      @cancel="showCreateModal = false"
    >
      <a-form layout="vertical" class="mt-4">
        <a-form-item label="名称">
          <a-input v-model:value="newAgent.name" placeholder="请输入 Agent 名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="newAgent.description"
            placeholder="请输入 Agent 描述"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 复制 Agent 弹窗 -->
    <a-modal
      v-model:open="showCopyModal"
      title="复制 Agent"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
      @ok="handleCopy"
      @cancel="showCopyModal = false"
    >
      <a-form layout="vertical" class="mt-4">
        <a-form-item label="新名称">
          <a-input v-model:value="copyAgentName" placeholder="请输入新 Agent 名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { agentApi, type Agent } from '@/api/agent'
import { PageHeader, SearchBar, StatusBadge, EmptyState, ConfirmModal } from '@/components'
import type { SearchField } from '@/components'

const router = useRouter()
const agents = ref<Agent[]>([])
const loading = ref(false)
const showCreateModal = ref(false)
const showCopyModal = ref(false)
const newAgent = ref<Partial<Agent>>({
  name: '',
  description: '',
  config: {}
})
const copyAgentName = ref('')
const currentAgent = ref<Agent | null>(null)

// 搜索与筛选
const searchQuery = ref('')
const statusFilter = ref('')
const activeFilter = ref('')

// SearchBar 字段配置
const searchFields: SearchField[] = [
  {
    label: 'Agent 名称',
    key: 'searchQuery',
    type: 'input',
    placeholder: '搜索 Agent 名称或描述...',
  },
  {
    label: '状态',
    key: 'statusFilter',
    type: 'select',
    placeholder: '全部状态',
    options: [
      { label: '草稿', value: 'DRAFT' },
      { label: '待审批', value: 'PENDING_APPROVAL' },
      { label: '已审批', value: 'APPROVED' },
      { label: '已发布', value: 'PUBLISHED' },
      { label: '已归档', value: 'ARCHIVED' },
    ],
  },
  {
    label: '类型',
    key: 'activeFilter',
    type: 'select',
    placeholder: '全部类型',
    options: [
      { label: '已启用', value: 'true' },
      { label: '已禁用', value: 'false' },
    ],
  },
]

// 分页
const currentPage = ref(1)
const pageSize = 9

// 筛选后的列表
const filteredAgents = computed(() => {
  let result = agents.value

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(
      (a) =>
        a.name?.toLowerCase().includes(query) ||
        a.description?.toLowerCase().includes(query)
    )
  }

  if (statusFilter.value) {
    result = result.filter((a) => a.status === statusFilter.value)
  }

  if (activeFilter.value !== '') {
    const isActive = activeFilter.value === 'true'
    result = result.filter((a) => a.isActive === isActive)
  }

  return result
})

// 分页数据
const totalPages = computed(() => Math.ceil(filteredAgents.value.length / pageSize))

const paginatedAgents = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAgents.value.slice(start, start + pageSize)
})

function shouldShowPage(page: number): boolean {
  if (page === 1 || page === totalPages.value) return true
  if (Math.abs(page - currentPage.value) <= 1) return true
  return false
}

async function loadAgents() {
  loading.value = true
  try {
    const res = await agentApi.getAllAgents()
    agents.value = res.data || []
  } catch (error) {
    message.error('加载 Agent 列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch(params?: Record<string, any>) {
  if (params) {
    searchQuery.value = params.searchQuery || ''
    statusFilter.value = params.statusFilter || ''
    activeFilter.value = params.activeFilter || ''
  }
  currentPage.value = 1
}

function handleReset() {
  searchQuery.value = ''
  statusFilter.value = ''
  activeFilter.value = ''
  currentPage.value = 1
}

function editAgent(agent: Agent) {
  router.push(`/agents/${agent.id}/edit`)
}

function viewVersions(agent: Agent) {
  router.push(`/agents/${agent.id}/versions`)
}

async function handleCopy() {
  if (!currentAgent.value || !copyAgentName.value) return

  try {
    await agentApi.copyAgent(currentAgent.value.id!, copyAgentName.value)
    message.success('复制成功')
    showCopyModal.value = false
    loadAgents()
  } catch (error) {
    message.error('复制失败')
  }
}

function deleteAgent(agent: Agent) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 Agent "${agent.name}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await agentApi.deleteAgent(agent.id!)
        message.success('删除成功')
        loadAgents()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

async function handleCreate() {
  if (!newAgent.value.name) {
    message.error('请输入 Agent 名称')
    return
  }

  try {
    await agentApi.createAgent(newAgent.value)
    message.success('创建成功')
    showCreateModal.value = false
    newAgent.value = { name: '', description: '', config: {} }
    loadAgents()
  } catch (error) {
    message.error('创建失败')
  }
}

function formatDate(date: string | undefined) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 状态相关辅助函数
function getStatusLabel(agent: Agent): string {
  const statusMap: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已审批',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return statusMap[agent.status || ''] || agent.isActive ? '启用' : '禁用'
}

function getStatusBadgeClass(agent: Agent): string {
  const classMap: Record<string, string> = {
    DRAFT: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400',
    PENDING_APPROVAL: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    APPROVED: 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400',
    PUBLISHED: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    ARCHIVED: 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-500'
  }
  return classMap[agent.status || ''] || (agent.isActive
    ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'
    : 'bg-red-50 dark:bg-red-950/30 text-red-500 dark:text-red-400')
}

function getAgentIconBg(agent: Agent): string {
  const bgMap: Record<string, string> = {
    DRAFT: 'bg-neutral-100 dark:bg-neutral-800',
    PENDING_APPROVAL: 'bg-amber-100 dark:bg-amber-900/40',
    APPROVED: 'bg-green-100 dark:bg-green-900/40',
    PUBLISHED: 'bg-blue-100 dark:bg-blue-900/40',
    ARCHIVED: 'bg-neutral-100 dark:bg-neutral-800'
  }
  return bgMap[agent.status || ''] || 'bg-blue-100 dark:bg-blue-900/40'
}

function getAgentIconColor(agent: Agent): string {
  const colorMap: Record<string, string> = {
    DRAFT: 'text-neutral-500 dark:text-neutral-400',
    PENDING_APPROVAL: 'text-amber-600 dark:text-amber-400',
    APPROVED: 'text-green-600 dark:text-green-400',
    PUBLISHED: 'text-blue-600 dark:text-blue-400',
    ARCHIVED: 'text-neutral-400 dark:text-neutral-500'
  }
  return colorMap[agent.status || ''] || 'text-blue-600 dark:text-blue-400'
}

function getAgentTags(agent: Agent): string[] {
  const tags: string[] = []
  if (agent.status) {
    const statusLabels: Record<string, string> = {
      DRAFT: '草稿',
      PENDING_APPROVAL: '待审批',
      APPROVED: '已审批',
      PUBLISHED: '已发布',
      ARCHIVED: '已归档'
    }
    tags.push(statusLabels[agent.status] || agent.status)
  }
  if (agent.isActive) {
    tags.push('已启用')
  }
  // 从 config 中提取类型标签
  if (agent.config?.type) {
    tags.push(String(agent.config.type))
  }
  return tags
}

onMounted(() => {
  loadAgents()
})
</script>

<style scoped>
.agent-list-page {
  padding: 0;
}

/* 两行截断 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
