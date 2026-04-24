<template>
  <div class="test-result-list">
    <div class="page-header">
      <h1>{{ $t('test.testResultList') }}</h1>
      <a-button @click="handleExport">
        {{ $t('test.export') }}
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
          <a-select-option value="passed">Passed</a-select-option>
          <a-select-option value="failed">Failed</a-select-option>
          <a-select-option value="skipped">Skipped</a-select-option>
        </a-select>
        <a-button @click="handleSearch" style="margin-left: 10px;">
          {{ $t('common.search') }}
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="results"
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
          <a-button @click="handleViewDetails(record.id)">
            {{ $t('common.viewDetails') }}
          </a-button>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestResult } from '@/api/test'

const router = useRouter()
const loading = ref(false)
const results = ref<TestResult[]>([])
const searchQuery = ref('')
const statusFilter = ref('')

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
    title: 'Execution Time (ms)',
    dataIndex: 'executionTime',
    key: 'executionTime'
  },
  {
    title: 'Error Message',
    dataIndex: 'errorMessage',
    key: 'errorMessage',
    ellipsis: true
  },
  {
    title: 'Created At',
    dataIndex: 'createdAt',
    key: 'createdAt'
  },
  {
    title: 'Actions',
    key: 'action',
    slots: { customRender: 'action' }
  }
]

const getStatusColor = (status: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'error'
    case 'SKIPPED': return 'warning'
    default: return 'default'
  }
}

const fetchResults = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const response = await testApi.getTestResults(params)
    results.value = response.data
  } catch (error) {
    message.error('Failed to fetch test results')
  } finally {
    loading.value = false
  }
}

const handleViewDetails = (id: number) => {
  router.push(`/test-results/detail/${id}`)
}

const handleExport = () => {
  testApi.exportTestResults()
    .then((response) => {
      const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `test-results-${new Date().toISOString().slice(0, 10)}.xlsx`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      message.success('Test results exported successfully')
    })
    .catch(() => {
      message.error('Failed to export test results')
    })
}

const handleSearch = () => {
  fetchResults()
}

onMounted(() => {
  fetchResults()
})
</script>

<style scoped>
.test-result-list {
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
