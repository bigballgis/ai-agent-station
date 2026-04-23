<template>
  <div class="test-result-detail">
    <div class="page-header">
      <h1>{{ $t('test.testResultDetail') }}</h1>
      <a-space>
        <a-button type="primary" @click="generateReport">
          生成报告
        </a-button>
        <a-button @click="handleBack">
          {{ $t('common.back') }}
        </a-button>
      </a-space>
    </div>

    <a-card>
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="Test Case Name">
          {{ testResult.testCaseName }}
        </a-descriptions-item>
        <a-descriptions-item label="Status">
          <a-tag :color="getStatusColor(testResult.status)">
            {{ testResult.status }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="Execution Time">
          {{ testResult.executionTime }} ms
        </a-descriptions-item>
        <a-descriptions-item label="Created At">
          {{ testResult.createdAt }}
        </a-descriptions-item>
        <a-descriptions-item label="Error Message" v-if="testResult.errorMessage">
          <pre>{{ testResult.errorMessage }}</pre>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>{{ t('testResultDetail.details') }}</a-divider>
      <pre v-if="testResult.details">{{ JSON.stringify(testResult.details, null, 2) }}</pre>
      <p v-else>{{ t('testResultDetail.noDetails') }}</p>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestResult } from '@/api/test'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const testResult = ref<TestResult>({
  id: 0,
  testExecutionId: 0,
  testCaseId: 0,
  status: 'passed',
  executionTime: 0
})

const getStatusColor = (status: string) => {
  switch (status) {
    case 'passed': return 'success'
    case 'failed': return 'error'
    case 'skipped': return 'warning'
    default: return 'default'
  }
}

const fetchTestResult = async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const response = await testApi.getTestResultById(id)
    testResult.value = response.data
  } catch (_error) {
    message.error('Failed to fetch test result')
  } finally {
    loading.value = false
  }
}

const generateReport = () => {
  testApi.exportTestResults({ testResultId: testResult.value.id })
    .then(response => {
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `test-report-${testResult.value.id}.pdf`
      link.click()
      URL.revokeObjectURL(url)
    })
    .catch(() => {
      message.error('Failed to generate report')
    })
}

const handleBack = () => {
  router.push('/test-results')
}

onMounted(() => {
  fetchTestResult()
})
</script>

<style scoped>
.test-result-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
