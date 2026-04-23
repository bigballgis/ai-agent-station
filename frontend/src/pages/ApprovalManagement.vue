<template>
  <div class="approval-management-page">
    <div class="page-header">
      <h1>{{ t('approval.management') }}</h1>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="pending" :tab="t('approval.pending')">
        <a-table
          :columns="columns"
          :data-source="pendingApprovals"
          :loading="loading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space>
                <a-button size="small" @click="viewTestResults(record.agentId!)">
                  {{ t('approval.viewTestResults') }}
                </a-button>
                <a-button type="primary" size="small" @click="showApproveModal(record)">
                  {{ t('approval.approve') }}
                </a-button>
                <a-button danger size="small" @click="showRejectModal(record)">
                  {{ t('approval.reject') }}
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="all" :tab="t('approval.allApprovals')">
        <a-table
          :columns="columnsAll"
          :data-source="allApprovals"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="showApproveModalFlag"
      :title="t('approval.approveModalTitle')"
      @ok="handleApprove"
      @cancel="showApproveModalFlag = false"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('approval.remarkLabel')">
          <a-textarea
            v-model:value="approvalRemark"
            :placeholder="t('approval.remarkPlaceholder')"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showRejectModalFlag"
      :title="t('approval.rejectModalTitle')"
      @ok="handleReject"
      @cancel="showRejectModalFlag = false"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('approval.rejectReasonLabel')">
          <a-textarea
            v-model:value="approvalRemark"
            :placeholder="t('approval.rejectReasonPlaceholder')"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showTestResults"
      :title="t('approval.testResultsTitle')"
      @cancel="showTestResults = false"
      :width="800"
    >
      <a-table
        :columns="testResultColumns"
        :data-source="testResults"
        row-key="id"
        pagination
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'passed' ? 'green' : 'red'">
              {{ record.status === 'passed' ? t('approval.passed') : t('approval.failed') }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { approvalApi, type Approval } from '@/api/approval'
import { testApi } from '@/api/test'
import type { TestResult } from '@/api/test'

const { t } = useI18n()

const activeTab = ref('pending')
const pendingApprovals = ref<Approval[]>([])
const allApprovals = ref<Approval[]>([])
const loading = ref(false)
const showApproveModalFlag = ref(false)
const showRejectModalFlag = ref(false)
const currentApproval = ref<Approval | null>(null)
const approvalRemark = ref('')
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})
const testResults = ref<TestResult[]>([])
const showTestResults = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: 'Agent ID', dataIndex: 'agentId', key: 'agentId', width: 100 },
  { title: t('approval.versionId'), dataIndex: 'agentVersionId', key: 'agentVersionId', width: 100 },
  { title: t('approval.submitter'), dataIndex: 'submitterId', key: 'submitterId', width: 100 },
  { title: t('approval.remark'), dataIndex: 'remark', key: 'remark' },
  { title: t('approval.submittedAt'), dataIndex: 'submittedAt', key: 'submittedAt', width: 180, customRender: ({ text }: { text: string }) => formatDate(text) },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('common.actions'), key: 'actions', width: 180 }
]

const columnsAll = [
  ...columns.slice(0, -1),
  { title: t('approval.approver'), dataIndex: 'approverId', key: 'approverId', width: 100 },
  { title: t('approval.approvalRemarkCol'), dataIndex: 'approvalRemark', key: 'approvalRemark' },
  { title: t('approval.approvedAt'), dataIndex: 'approvedAt', key: 'approvedAt', width: 180, customRender: ({ text }: { text: string }) => text ? formatDate(text) : '-' }
]

const testResultColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('approval.testCaseName'), dataIndex: 'testCaseName', key: 'testCaseName' },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('approval.executionTime'), dataIndex: 'executionTime', key: 'executionTime', width: 120 },
  { title: t('approval.errorMessage'), dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: t('common.createdAt'), dataIndex: 'createdAt', key: 'createdAt', width: 180, customRender: ({ text }: { text: string }) => text ? formatDate(text) : '-' }
]

async function loadPendingApprovals() {
  loading.value = true
  try {
    const res = await approvalApi.getPendingApprovals()
    pendingApprovals.value = res.data?.data?.records || []
  } catch (error) {
    message.error(t('approval.loadPendingFailed'))
  } finally {
    loading.value = false
  }
}

async function loadAllApprovals(page = 1, pageSize = 10) {
  loading.value = true
  try {
    const res = await approvalApi.getApprovals(page - 1, pageSize)
    allApprovals.value = res.data?.data?.records || []
    pagination.value.total = res.data?.data?.total || 0
  } catch (error) {
    message.error(t('approval.loadAllFailed'))
  } finally {
    loading.value = false
  }
}

function showApproveModal(approval: Approval) {
  currentApproval.value = approval
  approvalRemark.value = ''
  showApproveModalFlag.value = true
}

function showRejectModal(approval: Approval) {
  currentApproval.value = approval
  approvalRemark.value = ''
  showRejectModalFlag.value = true
}

async function handleApprove() {
  if (!currentApproval.value) return

  try {
    await approvalApi.approve(currentApproval.value.id, approvalRemark.value)
    message.success(t('approval.approveSuccess'))
    showApproveModalFlag.value = false
    loadPendingApprovals()
    loadAllApprovals(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error(t('approval.approveFailed'))
  }
}

async function handleReject() {
  if (!currentApproval.value) return

  try {
    await approvalApi.reject(currentApproval.value.id, approvalRemark.value)
    message.success(t('approval.rejectSuccess'))
    showRejectModalFlag.value = false
    loadPendingApprovals()
    loadAllApprovals(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error(t('approval.operationFailed'))
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadAllApprovals(pag.current, pag.pageSize)
}

function getStatusColor(status: string) {
  const colors: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red'
  }
  return colors[status] || 'default'
}

function getStatusText(status: string) {
  const texts: Record<string, string> = {
    PENDING: t('approval.pending'),
    APPROVED: t('approval.approved'),
    REJECTED: t('approval.rejected')
  }
  return texts[status] || status
}

function viewTestResults(agentId: number) {
  showTestResults.value = true
  testApi.getTestResults({ agentId, status: 'passed' })
    .then(response => {
      testResults.value = response.data
    })
    .catch(_error => {
      message.error(t('approval.loadTestResultsFailed'))
    })
}

function formatDate(date: string) {
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadPendingApprovals()
  loadAllApprovals()
})
</script>

<style scoped>
.approval-management-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}
</style>
