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
          :placeholder="$t('common.search')"
          style="width: 300px"
          allow-clear
        />
        <a-select v-model:value="statusFilter" :placeholder="$t('testExecution.filterByStatus')" style="width: 200px; margin-left: 10px;">
          <a-select-option value="">{{ $t('testExecution.all') }}</a-select-option>
          <a-select-option value="pending">{{ $t('testExecution.pending') }}</a-select-option>
          <a-select-option value="running">{{ $t('testExecution.running') }}</a-select-option>
          <a-select-option value="completed">{{ $t('testExecution.completed') }}</a-select-option>
          <a-select-option value="failed">{{ $t('testExecution.failed') }}</a-select-option>
          <a-select-option value="canceled">{{ $t('testExecution.canceled') }}</a-select-option>
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
      :title="$t('testExecution.batchRunTitle')"
      @ok="handleBatchRunConfirm"
      @cancel="batchRunModalVisible = false"
    >
      <a-select
        v-model:value="selectedTestCases"
        mode="multiple"
        style="width: 100%"
        :placeholder="$t('testExecution.selectTestCases')"
      >
        <a-select-option v-for="testCase in availableTestCases" :key="testCase.id" :value="testCase.id">
          {{ testCase.name }}
        </a-select-option>
      </a-select>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestExecution, TestCase } from '@/api/test'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const executions = ref<TestExecution[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const batchRunModalVisible = ref(false)
const selectedTestCases = ref<number[]>([])
const availableTestCases = ref<TestCase[]>([])

const columns = computed(() => [
  {
    title: t('testExecution.id'),
    dataIndex: 'id',
    key: 'id'
  },
  {
    title: t('testExecution.testCaseNameCol'),
    dataIndex: 'testCaseName',
    key: 'testCaseName'
  },
  {
    title: t('testExecution.status'),
    dataIndex: 'status',
    key: 'status',
    slots: { customRender: 'status' }
  },
  {
    title: t('testExecution.startTimeCol'),
    dataIndex: 'startTime',
    key: 'startTime'
  },
  {
    title: t('testExecution.endTimeCol'),
    dataIndex: 'endTime',
    key: 'endTime'
  },
  {
    title: t('testExecution.errorMessageCol'),
    dataIndex: 'errorMessage',
    key: 'errorMessage',
    ellipsis: true
  },
  {
    title: t('testExecution.actions'),
    key: 'action',
    slots: { customRender: 'action' }
  }
])

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
    message.error(t('testExecution.fetchFailed'))
  } finally {
    loading.value = false
  }
}

const fetchTestCases = async () => {
  try {
    const response = await testApi.getAllTestCases()
    availableTestCases.value = response.data
  } catch (error) {
    message.error(t('testExecution.fetchTestCasesFailed'))
  }
}

const handleViewResults = (id: number) => {
  router.push(`/test-results/${id}`)
}

const handleCancelExecution = (id: number) => {
  Modal.confirm({
    title: t('testExecution.cancelExecutionTitle'),
    content: t('testExecution.cancelExecutionContent'),
    onOk: () => {
      testApi.cancelTestExecution(id)
        .then(() => {
          message.success(t('testExecution.cancelSuccess'))
          fetchExecutions()
        })
        .catch(() => {
          message.error(t('testExecution.cancelFailed'))
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
    message.error(t('testExecution.selectAtLeastOne'))
    return
  }

  testApi.createBatchTestExecutions(selectedTestCases.value)
    .then(() => {
      message.success(t('testExecution.batchStartSuccess'))
      batchRunModalVisible.value = false
      selectedTestCases.value = []
      fetchExecutions()
    })
    .catch(() => {
      message.error(t('testExecution.batchStartFailed'))
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
