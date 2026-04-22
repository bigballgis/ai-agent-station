<template>
  <div class="test-execution-list">
    <div class="page-header">
      <h1>{{ $t('test.testExecutionList') }}</h1>
      <a-button type="primary" @click="handleBatchRun">
        {{ $t('test.batchRun') }}
      </a-button>
    </div>

    <a-card>
      <div class="search-bar">
        <a-input
          v-model:value="searchQuery"
          placeholder="{{ $t('common.search') }}"
          style="width: 300px"
          allow-clear
        />
        <a-select v-model:value="statusFilter" placeholder="Filter by status" style="width: 200px; margin-left: 10px;">
          <a-select-option value="">All</a-select-option>
          <a-select-option value="pending">Pending</a-select-option>
          <a-select-option value="running">Running</a-select-option>
          <a-select-option value="completed">Completed</a-select-option>
          <a-select-option value="failed">Failed</a-select-option>
          <a-select-option value="canceled">Canceled</a-select-option>
        </a-select>
        <a-button @click="handleSearch" style="margin-left: 10px;">
          {{ $t('common.search') }}
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="executions"
        row-key="id"
        :loading="loading"
        pagination
      >
        <template #status="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            {{ record.status }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <a-space>
            <a-button @click="handleViewResults(record.id)">
              {{ $t('test.viewResults') }}
            </a-button>
            <a-button 
              v-if="record.status === 'running' || record.status === 'pending'"
              danger 
              @click="handleCancelExecution(record.id)"
            >
              {{ $t('test.cancel') }}
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <!-- Batch Run Modal -->
    <a-modal
      v-model:open="batchRunModalVisible"
      title="Batch Run Test Cases"
      @ok="handleBatchRunConfirm"
      @cancel="batchRunModalVisible = false"
    >
      <a-select
        v-model:value="selectedTestCases"
        mode="multiple"
        style="width: 100%"
        placeholder="Select test cases"
      >
        <a-select-option v-for="testCase in availableTestCases" :key="testCase.id" :value="testCase.id">
          {{ testCase.name }}
        </a-select-option>
      </a-select>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestExecution, TestCase } from '@/api/test'

const router = useRouter()
const loading = ref(false)
const executions = ref<TestExecution[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const batchRunModalVisible = ref(false)
const selectedTestCases = ref<number[]>([])
const availableTestCases = ref<TestCase[]>([])

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id'
  },
  {
    title: 'Test Case',
    dataIndex: 'testCaseName',
    key: 'testCaseName'
  },
  {
    title: 'Status',
    dataIndex: 'status',
    key: 'status',
    slots: { customRender: 'status' }
  },
  {
    title: 'Start Time',
    dataIndex: 'startTime',
    key: 'startTime'
  },
  {
    title: 'End Time',
    dataIndex: 'endTime',
    key: 'endTime'
  },
  {
    title: 'Error Message',
    dataIndex: 'errorMessage',
    key: 'errorMessage',
    ellipsis: true
  },
  {
    title: 'Actions',
    key: 'action',
    slots: { customRender: 'action' }
  }
]

const getStatusColor = (status: string) => {
  switch (status) {
    case 'pending': return 'blue'
    case 'running': return 'processing'
    case 'completed': return 'success'
    case 'failed': return 'error'
    case 'canceled': return 'default'
    default: return 'default'
  }
}

const fetchExecutions = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const response = await testApi.getTestExecutions(params)
    executions.value = response.data
  } catch (error) {
    message.error('Failed to fetch test executions')
  } finally {
    loading.value = false
  }
}

const fetchTestCases = async () => {
  try {
    const response = await testApi.getAllTestCases()
    availableTestCases.value = response.data
  } catch (error) {
    message.error('Failed to fetch test cases')
  }
}

const handleViewResults = (id: number) => {
  router.push(`/test-results/${id}`)
}

const handleCancelExecution = (id: number) => {
  Modal.confirm({
    title: 'Cancel Test Execution',
    content: 'Are you sure you want to cancel this test execution?',
    onOk: () => {
      testApi.cancelTestExecution(id)
        .then(() => {
          message.success('Test execution canceled')
          fetchExecutions()
        })
        .catch(() => {
          message.error('Failed to cancel test execution')
        })
    }
  })
}

const handleBatchRun = () => {
  fetchTestCases().then(() => {
    batchRunModalVisible.value = true
  })
}

const handleBatchRunConfirm = () => {
  if (selectedTestCases.value.length === 0) {
    message.error('Please select at least one test case')
    return
  }

  testApi.createBatchTestExecutions(selectedTestCases.value)
    .then(() => {
      message.success('Batch test execution started')
      batchRunModalVisible.value = false
      selectedTestCases.value = []
      fetchExecutions()
    })
    .catch(() => {
      message.error('Failed to start batch test execution')
    })
}

const handleSearch = () => {
  fetchExecutions()
}

// 定时刷新执行状态
let interval: number | undefined

onMounted(() => {
  fetchExecutions()
  // 每5秒刷新一次状态
  interval = window.setInterval(fetchExecutions, 5000)
})

// 清理定时器
onBeforeUnmount(() => {
  if (interval) {
    clearInterval(interval)
  }
})
</script>

<style scoped>
.test-execution-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-bar {
  margin-bottom: 20px;
}
</style>
