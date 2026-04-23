<template>
  <div class="workflow-instance-page">
    <PageHeader :title="t('workflow.instanceMonitor')" :breadcrumbs="[
      { title: t('routes.dashboard'), path: '/dashboard' },
      { title: t('workflow.instances') }
    ]" />

    <!-- Filters -->
    <div class="filter-bar">
      <a-space>
        <a-select
          v-model:value="filters.status"
          :placeholder="t('workflow.statusFilter')"
          allow-clear
          style="width: 160px"
          @change="loadInstances"
        >
          <a-select-option value="PENDING">{{ t('workflow.instanceStatuses.PENDING') }}</a-select-option>
          <a-select-option value="RUNNING">{{ t('workflow.instanceStatuses.RUNNING') }}</a-select-option>
          <a-select-option value="COMPLETED">{{ t('workflow.instanceStatuses.COMPLETED') }}</a-select-option>
          <a-select-option value="FAILED">{{ t('workflow.instanceStatuses.FAILED') }}</a-select-option>
          <a-select-option value="CANCELLED">{{ t('workflow.instanceStatuses.CANCELLED') }}</a-select-option>
          <a-select-option value="SUSPENDED">{{ t('workflow.instanceStatuses.SUSPENDED') }}</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="filters.search"
          :placeholder="t('workflow.searchPlaceholder')"
          style="width: 200px"
          @search="loadInstances"
        />
        <a-button @click="resetFilters">{{ t('workflow.resetFilters') }}</a-button>
      </a-space>
    </div>

    <!-- Instance Table -->
    <a-table
      :columns="columns"
      :data-source="filteredInstances"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" :status-map="statusMap" />
        </template>
        <template v-else-if="column.key === 'startedAt'">
          {{ record.startedAt ? formatDate(record.startedAt) : '-' }}
        </template>
        <template v-else-if="column.key === 'completedAt'">
          {{ record.completedAt ? formatDate(record.completedAt) : '-' }}
        </template>
        <template v-else-if="column.key === 'error'">
          <a-tooltip v-if="record.error" :title="record.error">
            <span class="error-text">{{ truncate(record.error, 50) }}</span>
          </a-tooltip>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button type="link" size="small" @click="viewDetail(record)">{{ t('workflow.detail') }}</a-button>
            <a-button
              v-if="record.status === 'SUSPENDED'"
              type="link"
              size="small"
              @click="showApproveAction(record)"
            >
              {{ t('workflow.approvalAction') }}
            </a-button>
            <a-popconfirm
              v-if="record.status === 'RUNNING' || record.status === 'SUSPENDED' || record.status === 'PENDING'"
              :title="t('workflow.cancelWorkflowConfirm')"
              @confirm="handleCancel(record)"
            >
              <a-button type="link" size="small" danger>{{ t('workflow.cancel') }}</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Instance Detail Drawer -->
    <a-drawer
      v-model:open="showDetail"
      :title="`${t('workflow.instanceDetail.title')} - ${currentInstance?.workflowName || ''}`"
      width="720"
    >
      <template v-if="currentInstance">
        <!-- Instance Info -->
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 20px">
          <a-descriptions-item :label="t('workflow.instanceDetail.instanceId')">{{ currentInstance.id }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.workflowName')">{{ currentInstance.workflowName }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.status')">
            <StatusBadge :status="currentInstance.status" :status-map="statusMap" />
          </a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.currentStep')">{{ currentInstance.currentStep ?? '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.currentNode')">{{ currentInstance.currentNodeId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.startedBy')">{{ currentInstance.startedBy ?? '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.startTime')">{{ currentInstance.startedAt ? formatDate(currentInstance.startedAt) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="t('workflow.instanceDetail.endTime')">{{ currentInstance.completedAt ? formatDate(currentInstance.completedAt) : '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="currentInstance.error" :label="t('workflow.instanceDetail.errorMessage')" :span="2">
            <span class="error-text">{{ currentInstance.error }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <!-- Variables -->
        <a-card :title="t('workflow.instanceDetail.contextVariables')" size="small" style="margin-bottom: 16px">
          <pre class="json-pre">{{ currentInstance.variables ? JSON.stringify(currentInstance.variables, null, 2) : '{}' }}</pre>
        </a-card>

        <!-- Input -->
        <a-card :title="t('workflow.instanceDetail.input')" size="small" style="margin-bottom: 16px">
          <pre class="json-pre">{{ currentInstance.input ? JSON.stringify(currentInstance.input, null, 2) : '{}' }}</pre>
        </a-card>

        <!-- Output -->
        <a-card :title="t('workflow.instanceDetail.output')" size="small" style="margin-bottom: 16px">
          <pre class="json-pre">{{ currentInstance.output ? JSON.stringify(currentInstance.output, null, 2) : '{}' }}</pre>
        </a-card>

        <!-- Node Execution Timeline -->
        <a-card :title="t('workflow.instanceDetail.nodeTimeline')" size="small">
          <a-timeline v-if="nodeLogs.length > 0">
            <a-timeline-item
              v-for="log in nodeLogs"
              :key="log.id"
              :color="getTimelineColor(log.status)"
            >
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="timeline-node-name">{{ log.nodeName || log.nodeId }}</span>
                  <a-tag :color="getNodeStatusColor(log.status)" size="small">{{ log.status }}</a-tag>
                  <span class="timeline-node-type">{{ log.nodeType }}</span>
                </div>
                <div class="timeline-time" v-if="log.startedAt">
                  {{ formatDate(log.startedAt) }}
                  <span v-if="log.duration"> ({{ log.duration }}ms)</span>
                </div>
                <div class="timeline-output" v-if="log.output">
                  <pre class="json-pre-sm">{{ JSON.stringify(log.output, null, 2) }}</pre>
                </div>
                <div class="timeline-error" v-if="log.error">
                  <a-alert :message="log.error" type="error" show-icon />
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else :description="t('workflow.instanceDetail.noExecutionRecords')" />
        </a-card>
      </template>
    </a-drawer>

    <!-- Approve/Reject Modal -->
    <a-modal
      v-model:open="showApproveModal"
      :title="approveAction === 'approve' ? t('workflow.approveModalTitle') : t('workflow.rejectModalTitle')"
      @ok="handleApproveAction"
      :confirm-loading="approving"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('workflow.workflowLabel')">
          {{ currentInstance?.workflowName }}
        </a-form-item>
        <a-form-item :label="t('workflow.currentNodeLabel')">
          {{ currentInstance?.currentNodeId }}
        </a-form-item>
        <a-form-item :label="t('workflow.approvalCommentLabel')">
          <a-textarea
            v-model:value="approveComment"
            :placeholder="t('workflow.approvalCommentPlaceholder')"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PageHeader, StatusBadge } from '@/components'
import { workflowApi, type WorkflowInstance, type WorkflowNodeLog } from '@/api/workflow'

const { t } = useI18n()

const loading = ref(false)
const approving = ref(false)
const instances = ref<WorkflowInstance[]>([])
const nodeLogs = ref<WorkflowNodeLog[]>([])
const currentInstance = ref<WorkflowInstance | null>(null)
const showDetail = ref(false)
const showApproveModal = ref(false)
const approveAction = ref<'approve' | 'reject'>('approve')
const approveComment = ref('')

const filters = ref({
  status: undefined as string | undefined,
  search: ''
})

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statusMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待执行', color: 'default' },
  RUNNING: { text: '运行中', color: 'blue' },
  COMPLETED: { text: '已完成', color: 'green' },
  FAILED: { text: '失败', color: 'red' },
  CANCELLED: { text: '已取消', color: 'orange' },
  SUSPENDED: { text: '已挂起', color: 'purple' }
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '工作流名称', dataIndex: 'workflowName', key: 'workflowName', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '当前节点', dataIndex: 'currentNodeId', key: 'currentNodeId', width: 120, ellipsis: true },
  { title: '步骤', dataIndex: 'currentStep', key: 'currentStep', width: 60 },
  { title: '启动时间', key: 'startedAt', width: 170 },
  { title: '完成时间', key: 'completedAt', width: 170 },
  { title: '错误', key: 'error', ellipsis: true },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' as const }
]

const filteredInstances = computed(() => {
  let result = instances.value
  if (filters.value.search) {
    const search = filters.value.search.toLowerCase()
    result = result.filter(i => i.workflowName.toLowerCase().includes(search))
  }
  return result
})

function formatDate(date: string) {
  return new Date(date).toLocaleString('zh-CN')
}

function truncate(str: string, len: number) {
  return str.length > len ? str.substring(0, len) + '...' : str
}

function getTimelineColor(status: string) {
  const map: Record<string, string> = {
    COMPLETED: 'green',
    RUNNING: 'blue',
    FAILED: 'red',
    SKIPPED: 'gray',
    PENDING: 'orange'
  }
  return map[status] || 'blue'
}

function getNodeStatusColor(status: string) {
  const map: Record<string, string> = {
    COMPLETED: 'green',
    RUNNING: 'blue',
    FAILED: 'red',
    SKIPPED: 'default',
    PENDING: 'orange'
  }
  return map[status] || 'default'
}

async function loadInstances() {
  loading.value = true
  try {
    const res = await workflowApi.getInstances(
      pagination.value.current - 1,
      pagination.value.pageSize,
      { status: filters.value.status }
    )
    instances.value = res.data?.data?.records || []
    pagination.value.total = res.data?.data?.total || 0
  } catch (error) {
    message.error('加载实例列表失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadInstances()
}

function resetFilters() {
  filters.value = { status: undefined, search: '' }
  pagination.value.current = 1
  loadInstances()
}

async function viewDetail(instance: WorkflowInstance) {
  currentInstance.value = instance
  showDetail.value = true
  try {
    const res = await workflowApi.getInstanceHistory(instance.id)
    nodeLogs.value = res.data?.data || []
  } catch (error) {
    message.error('加载执行历史失败')
    nodeLogs.value = []
  }
}

function showApproveAction(instance: WorkflowInstance) {
  currentInstance.value = instance
  approveAction.value = 'approve'
  approveComment.value = ''
  showApproveModal.value = true
}

async function handleApproveAction() {
  if (!currentInstance.value || !currentInstance.value.currentNodeId) {
    message.warning('当前没有待审批节点')
    return
  }
  approving.value = true
  try {
    if (approveAction.value === 'approve') {
      await workflowApi.approveNode(
        currentInstance.value.id,
        currentInstance.value.currentNodeId,
        approveComment.value || undefined
      )
      message.success('审批通过')
    } else {
      await workflowApi.rejectNode(
        currentInstance.value.id,
        currentInstance.value.currentNodeId,
        approveComment.value || undefined
      )
      message.success('已拒绝')
    }
    showApproveModal.value = false
    loadInstances()
    // Refresh detail if open
    if (showDetail.value && currentInstance.value) {
      const res = await workflowApi.getInstance(currentInstance.value.id)
      currentInstance.value = res.data?.data
      const histRes = await workflowApi.getInstanceHistory(currentInstance.value.id)
      nodeLogs.value = histRes.data?.data || []
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    approving.value = false
  }
}

async function handleCancel(instance: WorkflowInstance) {
  try {
    await workflowApi.cancelWorkflow(instance.id, '用户手动取消')
    message.success('工作流已取消')
    loadInstances()
  } catch (error) {
    message.error('取消失败')
  }
}

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
.workflow-instance-page {
  padding: 24px;
}

.filter-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.error-text {
  color: #f5222d;
  font-size: 12px;
}

.json-pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}

.json-pre-sm {
  background: #f9f9f9;
  padding: 8px;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
  max-height: 150px;
  overflow-y: auto;
  margin: 0;
}

.timeline-content {
  padding-bottom: 4px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-node-name {
  font-weight: 500;
  color: #333;
}

.timeline-node-type {
  font-size: 12px;
  color: #999;
}

.timeline-time {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.timeline-error {
  margin-top: 8px;
}
</style>
