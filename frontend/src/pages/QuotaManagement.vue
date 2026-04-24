<template>
  <div class="quota-management">
    <!-- 页面头部 -->
    <div class="mb-6 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            {{ t('quota.title') }}
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            {{ t('quota.subtitle') }}
          </p>
        </div>
        <a-button type="primary" @click="refreshData" :loading="loading">
          {{ t('quota.refreshData') }}
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8 animate-slide-up">
      <a-card
        class="rounded-2xl border border-neutral-100 dark:border-neutral-800 shadow-card"
        :body-style="{ padding: '20px' }"
      >
        <a-statistic
          :title="t('quota.agentCount')"
          :value="stats.agentUsed"
          :suffix="`/ ${stats.agentLimit}`"
          class="text-neutral-900 dark:text-neutral-50"
        >
          <template #prefix>
            <span class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-blue-500 dark:text-blue-400 mr-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </span>
          </template>
        </a-statistic>
        <a-progress
          :percent="Math.round((stats.agentUsed / stats.agentLimit) * 100)"
          :stroke-color="getProgressColor(stats.agentUsed, stats.agentLimit)"
          :show-info="false"
          size="small"
          class="mt-3"
        />
      </a-card>

      <a-card
        class="rounded-2xl border border-neutral-100 dark:border-neutral-800 shadow-card"
        :body-style="{ padding: '20px' }"
      >
        <a-statistic
          :title="t('quota.apiCalls')"
          :value="stats.apiUsed"
          :suffix="`/ ${formatNumber(stats.apiLimit)}`"
          class="text-neutral-900 dark:text-neutral-50"
        >
          <template #prefix>
            <span class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-green-50 dark:bg-green-900/30 text-green-500 dark:text-green-400 mr-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
          </template>
        </a-statistic>
        <a-progress
          :percent="Math.round((stats.apiUsed / stats.apiLimit) * 100)"
          :stroke-color="getProgressColor(stats.apiUsed, stats.apiLimit)"
          :show-info="false"
          size="small"
          class="mt-3"
        />
      </a-card>

      <a-card
        class="rounded-2xl border border-neutral-100 dark:border-neutral-800 shadow-card"
        :body-style="{ padding: '20px' }"
      >
        <a-statistic
          :title="t('quota.tokenUsage')"
          :value="stats.tokenUsed"
          :suffix="`/ ${formatNumber(stats.tokenLimit)}`"
          class="text-neutral-900 dark:text-neutral-50"
        >
          <template #prefix>
            <span class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-purple-50 dark:bg-purple-900/30 text-purple-500 dark:text-purple-400 mr-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
            </span>
          </template>
        </a-statistic>
        <a-progress
          :percent="Math.round((stats.tokenUsed / stats.tokenLimit) * 100)"
          :stroke-color="getProgressColor(stats.tokenUsed, stats.tokenLimit)"
          :show-info="false"
          size="small"
          class="mt-3"
        />
      </a-card>

      <a-card
        class="rounded-2xl border border-neutral-100 dark:border-neutral-800 shadow-card"
        :body-style="{ padding: '20px' }"
      >
        <a-statistic
          :title="t('quota.storage')"
          :value="stats.storageUsed"
          :suffix="`/ ${stats.storageLimit} GB`"
          class="text-neutral-900 dark:text-neutral-50"
        >
          <template #prefix>
            <span class="inline-flex items-center justify-center w-8 h-8 rounded-lg bg-orange-50 dark:bg-orange-900/30 text-orange-500 dark:text-orange-400 mr-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
              </svg>
            </span>
          </template>
        </a-statistic>
        <a-progress
          :percent="Math.round((stats.storageUsed / stats.storageLimit) * 100)"
          :stroke-color="getProgressColor(stats.storageUsed, stats.storageLimit)"
          :show-info="false"
          size="small"
          class="mt-3"
        />
      </a-card>
    </div>

    <!-- 租户配额表格 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden animate-slide-up">
      <a-table
        :columns="columns"
        :data-source="filteredTenants"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (total: number) => t('quota.totalRecords', { total }) }"
        row-key="id"
        class="quota-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tenantName'">
            <div class="flex items-center gap-3">
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center text-white text-sm font-bold"
                :style="{ background: record.color }"
              >
                {{ record.name.charAt(0) }}
              </div>
              <span class="text-neutral-800 dark:text-neutral-200 font-semibold">{{ record.name }}</span>
            </div>
          </template>

          <template v-if="column.key === 'agentCount'">
            <span class="text-neutral-700 dark:text-neutral-300">
              {{ record.agentUsed }} / {{ record.agentLimit }}
            </span>
          </template>

          <template v-if="column.key === 'apiCalls'">
            <span class="text-neutral-700 dark:text-neutral-300">
              {{ formatNumber(record.apiUsed) }} / {{ formatNumber(record.apiLimit) }}
            </span>
          </template>

          <template v-if="column.key === 'tokenUsage'">
            <span class="text-neutral-700 dark:text-neutral-300">
              {{ formatNumber(record.tokenUsed) }} / {{ formatNumber(record.tokenLimit) }}
            </span>
          </template>

          <template v-if="column.key === 'storage'">
            <span class="text-neutral-700 dark:text-neutral-300">
              {{ record.storageUsed }} GB / {{ record.storageLimit }} GB
            </span>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record)">
              {{ getStatusText(record) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openEditModal(record)">
              {{ t('quota.editQuotaBtn') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 编辑配额弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      :title="t('quota.editQuota')"
      :confirm-loading="submitting"
      @ok="handleEditSubmit"
      @cancel="editModalVisible = false"
      :ok-text="t('common.save')"
      :cancel-text="t('common.cancel')"
      width="560px"
    >
      <a-form
        :model="editForm"
        :label-col="{ span: 8 }"
        :wrapper-col="{ span: 16 }"
        class="mt-4"
        :label-col-responsive="{ xs: 24, sm: 8 }"
        :wrapper-col-responsive="{ xs: 24, sm: 16 }"
      >
        <a-form-item :label="t('quota.agentLimit')">
          <a-input-number v-model:value="editForm.agentLimit" :min="1" :max="1000" class="w-full" />
        </a-form-item>
        <a-form-item :label="t('quota.apiCallLimit')">
          <a-input-number v-model:value="editForm.apiLimit" :min="1000" :max="10000000" :step="1000" class="w-full" />
        </a-form-item>
        <a-form-item :label="t('quota.tokenLimit')">
          <a-input-number v-model:value="editForm.tokenLimit" :min="10000" :max="100000000" :step="10000" class="w-full" />
        </a-form-item>
        <a-form-item :label="t('quota.storageLimit')">
          <a-input-number v-model:value="editForm.storageLimit" :min="1" :max="10000" class="w-full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { updateTenantQuota } from '@/api/quota'
import { getTenants } from '@/api/tenant'
import { logger } from '@/utils/logger'

interface TenantQuota {
  id: number
  name: string
  color: string
  agentUsed: number
  agentLimit: number
  apiUsed: number
  apiLimit: number
  tokenUsed: number
  tokenLimit: number
  storageUsed: number
  storageLimit: number
}

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const editModalVisible = ref(false)
const currentEditId = ref<number | null>(null)

const editForm = reactive({
  agentLimit: 0,
  apiLimit: 0,
  tokenLimit: 0,
  storageLimit: 0,
})

const tenants = ref<TenantQuota[]>([])

async function fetchQuotas() {
  loading.value = true
  try {
    const res = await getTenants()
    tenants.value = res.data || res || []
  } catch (e) {
    logger.error('获取租户配额失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchQuotas()
})

const stats = computed(() => {
  const list = tenants.value
  return {
    agentUsed: list.reduce((sum, t) => sum + t.agentUsed, 0),
    agentLimit: list.reduce((sum, t) => sum + t.agentLimit, 0),
    apiUsed: list.reduce((sum, t) => sum + t.apiUsed, 0),
    apiLimit: list.reduce((sum, t) => sum + t.apiLimit, 0),
    tokenUsed: list.reduce((sum, t) => sum + t.tokenUsed, 0),
    tokenLimit: list.reduce((sum, t) => sum + t.tokenLimit, 0),
    storageUsed: list.reduce((sum, t) => sum + t.storageUsed, 0),
    storageLimit: list.reduce((sum, t) => sum + t.storageLimit, 0),
  }
})

const filteredTenants = computed(() => tenants.value)

const columns = [
  { title: t('quota.tenantName'), key: 'tenantName', dataIndex: 'name', width: 180 },
  { title: t('quota.agentCount'), key: 'agentCount', width: 140 },
  { title: t('quota.apiCalls'), key: 'apiCalls', width: 180 },
  { title: t('quota.tokenUsage'), key: 'tokenUsage', width: 200 },
  { title: t('quota.storage'), key: 'storage', width: 160 },
  { title: t('common.status'), key: 'status', width: 100, align: 'center' as const },
  { title: t('common.actions'), key: 'action', width: 120, align: 'center' as const },
]

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return String(num)
}

function getProgressColor(used: number, limit: number): string {
  const ratio = used / limit
  if (ratio >= 0.9) return '#EF4444'
  if (ratio >= 0.7) return '#F59E0B'
  return '#10B981'
}

function getStatusColor(record: TenantQuota): string {
  const checks = [
    record.agentUsed / record.agentLimit,
    record.apiUsed / record.apiLimit,
    record.tokenUsed / record.tokenLimit,
    record.storageUsed / record.storageLimit,
  ]
  const maxRatio = Math.max(...checks)
  if (maxRatio >= 1) return 'red'
  if (maxRatio >= 0.7) return 'orange'
  return 'green'
}

function getStatusText(record: TenantQuota): string {
  const checks = [
    record.agentUsed / record.agentLimit,
    record.apiUsed / record.apiLimit,
    record.tokenUsed / record.tokenLimit,
    record.storageUsed / record.storageLimit,
  ]
  const maxRatio = Math.max(...checks)
  if (maxRatio >= 1) return t('quota.exceeded')
  if (maxRatio >= 0.7) return t('quota.warning')
  return t('quota.normal')
}

function openEditModal(record: TenantQuota) {
  currentEditId.value = record.id
  editForm.agentLimit = record.agentLimit
  editForm.apiLimit = record.apiLimit
  editForm.tokenLimit = record.tokenLimit
  editForm.storageLimit = record.storageLimit
  editModalVisible.value = true
}

async function handleEditSubmit() {
  submitting.value = true
  try {
    if (currentEditId.value !== null) {
      await updateTenantQuota(currentEditId.value, { ...editForm })
      message.success(t('quota.updateSuccess'))
      editModalVisible.value = false
      await fetchQuotas()
    }
  } catch (e) {
    logger.error('配额更新失败:', e)
    message.error(t('quota.updateFailed'))
  } finally {
    submitting.value = false
  }
}

async function refreshData() {
  await fetchQuotas()
  message.success(t('quota.refreshSuccess'))
}
</script>
