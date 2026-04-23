<template>
  <div class="deployment-management-page">
    <div class="page-header">
      <h1>{{ t('deployment.management') }}</h1>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="deploy" :tab="t('deployment.deployAction')">
        <a-card :title="t('deployment.selectAgentVersion')" style="margin-bottom: 24px;">
          <a-form layout="vertical" :model="deployForm">
            <a-form-item :label="t('deployment.agentId')">
              <a-input-number v-model:value="deployForm.agentId" style="width: 100%;" :placeholder="t('deployment.agentId')" />
            </a-form-item>
            <a-form-item :label="t('deployment.versionId')">
              <a-input-number v-model:value="deployForm.versionId" style="width: 100%;" :placeholder="t('deployment.versionId')" />
            </a-form-item>
            <a-form-item :label="t('deployment.canarySwitch')">
              <a-switch v-model:checked="deployForm.isCanary" />
            </a-form-item>
            <a-form-item v-if="deployForm.isCanary" :label="t('deployment.canaryPercent')">
              <a-input-number v-model:value="deployForm.canaryPercentage" :min="1" :max="100" style="width: 100%;" />
            </a-form-item>
            <a-form-item :label="t('deployment.deployRemark')">
              <a-textarea v-model:value="deployForm.remark" :placeholder="t('deployment.deployRemarkPlaceholder')" :rows="3" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleDeploy" :loading="deploying">
                {{ t('deployment.deploy') }}
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="history" :tab="t('deployment.deployHistory')">
        <a-table
          :columns="columns"
          :data-source="deployments"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'isCanary'">
              <a-tag v-if="record.isCanary" color="blue">{{ t('deployment.canary') }} {{ record.canaryPercentage }}%</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space v-if="record.status === 'SUCCESS'">
                <a-button type="primary" size="small" danger @click="handleRollback(record)">
                  {{ t('deployment.rollback') }}
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="compare" :tab="t('deployment.versionCompare')">
        <a-card :title="t('deployment.selectVersionCompare')" style="margin-bottom: 24px;">
          <a-form layout="vertical" :model="compareForm">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('deployment.versionId') + ' 1'">
                  <a-input-number v-model:value="compareForm.versionId1" style="width: 100%;" :placeholder="t('deployment.versionId') + ' 1'" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('deployment.versionId') + ' 2'">
                  <a-input-number v-model:value="compareForm.versionId2" style="width: 100%;" :placeholder="t('deployment.versionId') + ' 2'" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" @click="handleCompare" :loading="comparing">
                {{ t('deployment.compare') }}
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card v-if="comparisonResult" :title="t('deployment.compareResult')">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card :title="t('deployment.addedConfig')" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.added, null, 2) }}</pre>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card :title="t('deployment.removedConfig')" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.removed, null, 2) }}</pre>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card :title="t('deployment.modifiedConfig')" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.modified, null, 2) }}</pre>
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="showRollbackModal"
      :title="t('deployment.confirmRollbackTitle')"
      @ok="confirmRollback"
      @cancel="showRollbackModal = false"
    >
      <p>{{ t('deployment.confirmRollbackContent') }}</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { deploymentApi, type Deployment, type VersionComparison } from '@/api/deployment'

const { t } = useI18n()

const activeTab = ref('history')
const deployments = ref<Deployment[]>([])
const loading = ref(false)
const deploying = ref(false)
const comparing = ref(false)
const showRollbackModal = ref(false)
const currentDeployment = ref<Deployment | null>(null)
const comparisonResult = ref<VersionComparison | null>(null)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const deployForm = ref({
  agentId: null as number | null,
  versionId: null as number | null,
  isCanary: false,
  canaryPercentage: 100,
  remark: ''
})

const compareForm = ref({
  versionId1: null as number | null,
  versionId2: null as number | null
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: 'Agent ID', dataIndex: 'agentId', key: 'agentId', width: 100 },
  { title: t('deployment.version'), dataIndex: 'version', key: 'version', width: 120 },
  { title: t('deployment.canaryCol'), key: 'isCanary', width: 120 },
  { title: t('deployment.deployer'), dataIndex: 'deployerId', key: 'deployerId', width: 100 },
  { title: t('deployment.deployRemark'), dataIndex: 'remark', key: 'remark' },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('common.createdAt'), dataIndex: 'createdAt', key: 'createdAt', width: 180, customRender: ({ text }: { text: string }) => formatDate(text) },
  { title: t('common.actions'), key: 'actions', width: 100 }
]

async function loadDeployments(page = 1, pageSize = 10) {
  loading.value = true
  try {
    const res = await deploymentApi.getDeployments(page - 1, pageSize)
    deployments.value = res.data?.data?.content || []
    pagination.value.total = res.data?.data?.totalElements || 0
  } catch (error) {
    message.error(t('deployment.loadDeployFailed'))
  } finally {
    loading.value = false
  }
}

async function handleDeploy() {
  if (!deployForm.value.agentId || !deployForm.value.versionId) {
    message.error(t('deployment.inputAgentAndVersion'))
    return
  }

  deploying.value = true
  try {
    await deploymentApi.deploy(
      deployForm.value.agentId,
      deployForm.value.versionId,
      deployForm.value.isCanary,
      deployForm.value.canaryPercentage,
      deployForm.value.remark
    )
    message.success(t('deployment.deploySuccess'))
    deployForm.value = { agentId: null, versionId: null, isCanary: false, canaryPercentage: 100, remark: '' }
    loadDeployments(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error(t('deployment.deployFailed'))
  } finally {
    deploying.value = false
  }
}

function handleRollback(deployment: Deployment) {
  currentDeployment.value = deployment
  showRollbackModal.value = true
}

async function confirmRollback() {
  if (!currentDeployment.value) return

  try {
    await deploymentApi.rollback(currentDeployment.value.id)
    message.success(t('deployment.rollbackSuccess'))
    showRollbackModal.value = false
    loadDeployments(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error(t('deployment.rollbackFailed'))
  }
}

async function handleCompare() {
  if (!compareForm.value.versionId1 || !compareForm.value.versionId2) {
    message.error(t('deployment.inputTwoVersions'))
    return
  }

  comparing.value = true
  try {
    const res = await deploymentApi.compareVersions(compareForm.value.versionId1, compareForm.value.versionId2)
    comparisonResult.value = res.data?.data
    message.success(t('deployment.compareSuccess'))
  } catch (error) {
    message.error(t('deployment.compareFailed'))
  } finally {
    comparing.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadDeployments(pag.current, pag.pageSize)
}

function getStatusColor(status: string) {
  const colors: Record<string, string> = {
    PENDING: 'orange',
    DEPLOYING: 'blue',
    SUCCESS: 'green',
    FAILED: 'red',
    ROLLED_BACK: 'gray'
  }
  return colors[status] || 'default'
}

function getStatusText(status: string) {
  const texts: Record<string, string> = {
    PENDING: t('deployment.pending'),
    DEPLOYING: t('deployment.deploying'),
    SUCCESS: t('deployment.success'),
    FAILED: t('deployment.failed'),
    ROLLED_BACK: t('deployment.rolledBack')
  }
  return texts[status] || status
}

function formatDate(date: string) {
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadDeployments()
})
</script>

<style scoped>
.deployment-management-page {
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

pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}
</style>
