<template>
  <div class="deployment-management-page">
    <div class="page-header">
      <h1>发布管理</h1>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="deploy" tab="发布操作">
        <a-card title="选择Agent和版本进行发布" style="margin-bottom: 24px;">
          <a-form layout="vertical" :model="deployForm">
            <a-form-item label="Agent ID">
              <a-input-number v-model:value="deployForm.agentId" style="width: 100%;" placeholder="请输入Agent ID" />
            </a-form-item>
            <a-form-item label="版本 ID">
              <a-input-number v-model:value="deployForm.versionId" style="width: 100%;" placeholder="请输入版本 ID" />
            </a-form-item>
            <a-form-item label="灰度发布">
              <a-switch v-model:checked="deployForm.isCanary" />
            </a-form-item>
            <a-form-item v-if="deployForm.isCanary" label="灰度比例 (%)">
              <a-input-number v-model:value="deployForm.canaryPercentage" :min="1" :max="100" style="width: 100%;" />
            </a-form-item>
            <a-form-item label="发布备注">
              <a-textarea v-model:value="deployForm.remark" placeholder="请输入发布备注（可选）" :rows="3" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleDeploy" :loading="deploying">
                发布
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="history" tab="发布历史">
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
              <a-tag v-if="record.isCanary" color="blue">灰度 {{ record.canaryPercentage }}%</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space v-if="record.status === 'SUCCESS'">
                <a-button type="primary" size="small" danger @click="handleRollback(record)">
                  回滚
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="compare" tab="版本对比">
        <a-card title="选择两个版本进行对比" style="margin-bottom: 24px;">
          <a-form layout="vertical" :model="compareForm">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="版本 ID 1">
                  <a-input-number v-model:value="compareForm.versionId1" style="width: 100%;" placeholder="请输入版本 ID 1" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="版本 ID 2">
                  <a-input-number v-model:value="compareForm.versionId2" style="width: 100%;" placeholder="请输入版本 ID 2" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" @click="handleCompare" :loading="comparing">
                对比
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card v-if="comparisonResult" title="对比结果">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="新增配置" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.added, null, 2) }}</pre>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="删除配置" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.removed, null, 2) }}</pre>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="修改配置" size="small">
                <pre>{{ JSON.stringify(comparisonResult.configDiff.modified, null, 2) }}</pre>
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="showRollbackModal"
      title="确认回滚"
      @ok="confirmRollback"
      @cancel="showRollbackModal = false"
    >
      <p>确定要回滚到上一个版本吗？</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { deploymentApi, type Deployment, type VersionComparison } from '@/api/deployment'

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
  { title: '版本', dataIndex: 'version', key: 'version', width: 120 },
  { title: '灰度', key: 'isCanary', width: 120 },
  { title: '发布人', dataIndex: 'deployerId', key: 'deployerId', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, customRender: ({ text }: { text: string }) => formatDate(text) },
  { title: '操作', key: 'actions', width: 100 }
]

async function loadDeployments(page = 1, pageSize = 10) {
  loading.value = true
  try {
    const res = await deploymentApi.getDeployments(page - 1, pageSize)
    deployments.value = res.data?.data?.content || []
    pagination.value.total = res.data?.data?.totalElements || 0
  } catch (error) {
    message.error('加载发布历史失败')
  } finally {
    loading.value = false
  }
}

async function handleDeploy() {
  if (!deployForm.value.agentId || !deployForm.value.versionId) {
    message.error('请输入Agent ID和版本 ID')
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
    message.success('发布成功')
    deployForm.value = { agentId: null, versionId: null, isCanary: false, canaryPercentage: 100, remark: '' }
    loadDeployments(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error('发布失败')
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
    message.success('回滚成功')
    showRollbackModal.value = false
    loadDeployments(pagination.value.current, pagination.value.pageSize)
  } catch (error) {
    message.error('回滚失败')
  }
}

async function handleCompare() {
  if (!compareForm.value.versionId1 || !compareForm.value.versionId2) {
    message.error('请输入两个版本 ID')
    return
  }

  comparing.value = true
  try {
    const res = await deploymentApi.compareVersions(compareForm.value.versionId1, compareForm.value.versionId2)
    comparisonResult.value = res.data?.data
    message.success('对比完成')
  } catch (error) {
    message.error('对比失败')
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
    PENDING: '待发布',
    DEPLOYING: '发布中',
    SUCCESS: '发布成功',
    FAILED: '发布失败',
    ROLLED_BACK: '已回滚'
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
