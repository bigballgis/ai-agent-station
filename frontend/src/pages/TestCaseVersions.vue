<template>
  <div class="test-case-versions">
    <div class="page-header">
      <h1>{{ $t('test.testCaseVersions') }}</h1>
      <a-button @click="handleBack">
        {{ $t('common.back') }}
      </a-button>
    </div>

    <a-card>
      <a-table
        :columns="columns"
        :data-source="versions"
        row-key="versionNumber"
        :loading="loading"
      >
        <template #action="{ record }">
          <a-space>
            <a-button @click="handleViewVersion(record)">
              {{ $t('common.view') }}
            </a-button>
            <a-button type="primary" @click="handleRollback(record.versionNumber)">
              {{ $t('test.rollback') }}
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { testApi } from '@/api/test'
import type { TestCaseVersion } from '@/api/test'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const versions = ref<TestCaseVersion[]>([])

const columns = [
  {
    title: 'Version',
    dataIndex: 'versionNumber',
    key: 'versionNumber'
  },
  {
    title: 'Change Log',
    dataIndex: 'changeLog',
    key: 'changeLog'
  },
  {
    title: 'Created By',
    dataIndex: 'createdBy',
    key: 'createdBy'
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

const fetchVersions = async () => {
  const testCaseId = Number(route.params.id)
  loading.value = true
  try {
    const response = await testApi.getTestCaseVersions(testCaseId)
    versions.value = response.data
  } catch (error) {
    message.error('Failed to fetch test case versions')
  } finally {
    loading.value = false
  }
}

const handleViewVersion = (version: TestCaseVersion) => {
  Modal.info({
    title: `Version ${version.versionNumber}`,
    content: `
      <div>
        <p><strong>Change Log:</strong> ${version.changeLog}</p>
        <p><strong>Config:</strong></p>
        <pre style="white-space: pre-wrap;">
          ${JSON.stringify(version.config, null, 2)}
        </pre>
      </div>
    `
  })
}

const handleRollback = (versionNumber: number) => {
  Modal.confirm({
    title: 'Rollback to Version',
    content: `Are you sure you want to rollback to version ${versionNumber}?`,
    onOk: () => {
      const testCaseId = Number(route.params.id)
      testApi.rollbackToVersion(testCaseId, versionNumber)
        .then(() => {
          message.success('Rollback successful')
          router.push('/test-cases')
        })
        .catch(() => {
          message.error('Failed to rollback')
        })
    }
  })
}

const handleBack = () => {
  router.push('/test-cases')
}

onMounted(() => {
  fetchVersions()
})
</script>

<style scoped>
.test-case-versions {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
