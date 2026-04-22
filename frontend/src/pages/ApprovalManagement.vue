<template>
  <div class="approval-management-page">
    <div class="page-header">
      <h1>审批管理</h1>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="pending" tab="待审批">
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
                  查看测试结果
                </a-button>
                <a-button type="primary" size="small" @click="showApproveModal(record)">
                  通过
                </a-button>
                <a-button danger size="small" @click="showRejectModal(record)">
                  驳回
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="all" tab="全部审批">
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
      title="通过审批"
      @ok="handleApprove"
      @cancel="showApproveModalFlag = false"
    >
      <a-form layout="vertical">
        <a-form-item label="审批备注">
          <a-textarea
            v-model:value="approvalRemark"
            placeholder="请输入审批备注（可选）"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showRejectModalFlag"
      title="驳回审批"
      @ok="handleReject"
      @cancel="showRejectModalFlag = false"
    >
      <a-form layout="vertical">
        <a-form-item label="驳回原因">
          <a-textarea
            v-model:value="approvalRemark"
            placeholder="请输入驳回原因"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showTestResults"
      title="测试结果"
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
              {{ record.status === 'passed' ? '通过' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { approvalApi, type Approval } from '@/api/approval'
import { testApi } from '@/api/test'
import type { TestResult } from '@/api/test'

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
  { title: '版本 ID', dataIndex: 'agentVersionId', key: 'agentVersionId', width: 100 },
  { title: '提交人', dataIndex: 'submitterId', key: 'submitterId', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt', width: 180, customRender: ({ text }: { text: string }) => formatDate(text) },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 180 }
]

const columnsAll = [
  ...columns.slice(0, -1),
  { title: '审批人', dataIndex: 'approverId', key: 'approverId', width: 100 },
  { title: '审批备注', dataIndex: 'approvalRemark', key: 'approvalRemark' },
  { title: '审批时间', dataIndex: 'approvedAt', key: 'approvedAt', width: 180, customRender: ({ text }: { text: string }) => text ? formatDate(text) : '-' }
]

const testResultColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '测试用例', dataIndex: 'testCaseName', key: 'testCaseName' },
  { title: '状态', key: 'status', width: 100 },
  { title: '执行时间(ms)', dataIndex: 'executionTime', key: 'executionTime', width: 120 },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, customRender: ({ text }: { text: string }) => text ? formatDate(text) : '-' }
]

async function loadPendingApprovals() {
  loading.value = true
  try {
    const res = await approvalApi.getPendingApprovals()
    pendingApprovals.value = res.data?.data?.content || []
  } catch (error) {
    message.error('加载待审批列表失败')
  } finally {
    loading.value = false
  }
}

async function loadAllApprovals(page = 1, pageSize = 10) {
  loading.value = true
  try {
    const res = await approvalApi.getApprovals(page - 1, pageSize)
    allApprovals.value = res.data?.data?.content || []
    pagination.value.total = res.data?.data?.totalElements || 0
  } catch (error) {
    message.error('加载审批列表失败')
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
    message.success('审批通过')
    showApproveModalFlag.value = false
    loadPendingApprovals()
    loadAllApprovals(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error('审批失败')
  }
}

async function handleReject() {
  if (!currentApproval.value) return

  try {
    await approvalApi.reject(currentApproval.value.id, approvalRemark.value)
    message.success('已驳回')
    showRejectModalFlag.value = false
    loadPendingApprovals()
    loadAllApprovals(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error('操作失败')
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
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回'
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
      message.error('加载测试结果失败')
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
