<template>
  <div class="agent-edit-page">
    <div class="edit-header">
      <button @click="goBack" class="btn btn-secondary">{{ t('common.back') }}</button>
      <h1>{{ t('agent.editAgent') }}</h1>
      <div class="header-actions">
        <button @click="submitForApproval" class="btn btn-secondary">{{ t('agent.submitApproval') }}</button>
        <button @click="deployAgent" class="btn btn-success">{{ t('agent.publish') }}</button>
        <button @click="saveAgent" class="btn btn-primary">{{ t('common.save') }}</button>
      </div>
    </div>

    <div class="info-section">
      <div class="info-item">
        <label>{{ t('agent.agentName') }}:</label>
        <input v-model="agent.name" :placeholder="t('agent.inputNamePlaceholder')" />
      </div>
      <div class="info-item">
        <label>{{ t('agent.agentDescription') }}:</label>
        <textarea v-model="agent.description" :placeholder="t('agent.inputDescPlaceholder')" rows="2"></textarea>
      </div>
      <div class="info-item">
        <label>{{ t('common.status') }}:</label>
        <select v-model="agent.isActive">
          <option :value="true">{{ t('agent.statusEnabled') }}</option>
          <option :value="false">{{ t('agent.statusDisabled') }}</option>
        </select>
      </div>
      <div class="info-item">
        <label>{{ t('agentEdit.currentStatus') }}:</label>
        <span class="status-tag" :class="getAgentStatusClass(agent.status || '')">{{ getAgentStatusText(agent.status || '') }}</span>
      </div>
    </div>

    <div class="canvas-section">
      <div class="designer-redirect">
        <p>{{ t('agentEdit.useDesignerHint') }}</p>
        <router-link :to="`/agents/design/${route.params.id}`" class="btn btn-primary">
          {{ t('agentEdit.openDesigner') }}
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { agentApi, type Agent } from '@/api/agent'
import { approvalApi } from '@/api/approval'
import { deploymentApi } from '@/api/deployment'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const agent = ref<Agent>({
  name: '',
  description: '',
  config: {},
  isActive: true,
  status: 'DRAFT'
})
const loading = ref(false)

async function loadAgent() {
  loading.value = true
  try {
    const res = await agentApi.getAgentById(route.params.id as string)
    agent.value = res.data
  } catch (error) {
    message.error(t('agentEdit.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function saveAgent() {
  if (!agent.value.name) {
    message.error(t('agent.inputAgentNameWarning'))
    return
  }

  loading.value = true
  try {
    await agentApi.updateAgent(route.params.id as string, agent.value)
    message.success(t('common.success'))
  } catch (error) {
    message.error(t('agentEdit.saveFailed'))
  } finally {
    loading.value = false
  }
}

async function submitForApproval() {
  Modal.confirm({
    title: t('agent.submitApproval'),
    content: t('agentEdit.submitApprovalConfirm'),
    onOk: async () => {
      loading.value = true
      try {
        await approvalApi.submitForApproval(Number(route.params.id), agent.value.latestVersionId || 0, t('agent.submitApproval'))
        message.success(t('agentEdit.submitApprovalSuccess'))
        // 重新加载Agent状态
        await loadAgent()
      } catch (error) {
        message.error(t('agentEdit.submitApprovalFailed'))
      } finally {
        loading.value = false
      }
    }
  })
}

async function deployAgent() {
  Modal.confirm({
    title: t('agent.publish'),
    content: t('agentEdit.deployConfirm'),
    onOk: async () => {
      loading.value = true
      try {
        await deploymentApi.deploy(Number(route.params.id), agent.value.latestVersionId || 0, false, 100, t('agent.publish'))
        message.success(t('agentEdit.deploySuccess'))
        // 重新加载Agent状态
        await loadAgent()
      } catch (error) {
        message.error(t('agentEdit.deployFailed'))
      } finally {
        loading.value = false
      }
    }
  })
}

function getAgentStatusClass(status: string) {
  switch (status) {
    case 'DRAFT':
      return 'status-draft'
    case 'PENDING_APPROVAL':
      return 'status-pending'
    case 'APPROVED':
      return 'status-approved'
    case 'PUBLISHED':
      return 'status-published'
    default:
      return ''
  }
}

function getAgentStatusText(status: string) {
  switch (status) {
    case 'DRAFT':
      return t('agent.statusDraft')
    case 'PENDING_APPROVAL':
      return t('agent.statusPendingApproval')
    case 'APPROVED':
      return t('agent.statusApproved')
    case 'PUBLISHED':
      return t('agent.statusPublished')
    default:
      return status
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadAgent()
})
</script>

<style scoped>
.agent-edit-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
}

.edit-header h1 {
  margin: 0;
  font-size: 20px;
  flex: 1;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.info-section {
  display: flex;
  gap: 24px;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  align-items: center;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-item label {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.info-item input,
.info-item textarea,
.info-item select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.info-item input {
  width: 200px;
}

.info-item textarea {
  width: 300px;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-draft {
  background: #f0f0f0;
  color: #666;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-approved {
  background: #d4edda;
  color: #155724;
}

.status-published {
  background: #d1ecf1;
  color: #0c5460;
}

.canvas-section {
  flex: 1;
  overflow: hidden;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover {
  background: #40a9ff;
}

.btn-secondary {
  background: #fafafa;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover {
  background: #f5f5f5;
}

.btn-success {
  background: #52c41a;
  color: white;
}

.btn-success:hover {
  background: #73d13d;
}

/* Dark mode overrides */
:global(.dark) .agent-edit-page {
  background-color: #171717;
  color: #e5e5e5;
}

:global(.dark) .edit-header {
  background: #171717;
  border-bottom-color: #262626;
}

:global(.dark) .edit-header h1 {
  color: #e5e5e5;
}

:global(.dark) .info-section {
  background: #171717;
  border-bottom-color: #262626;
}

:global(.dark) .info-item label {
  color: #e5e5e5;
}

:global(.dark) .info-item input,
:global(.dark) .info-item textarea,
:global(.dark) .info-item select {
  background-color: #262626;
  color: #e5e5e5;
  border-color: #404040;
}

:global(.dark) .btn-secondary {
  background: #262626;
  border-color: #404040;
  color: #e5e5e5;
}

:global(.dark) .btn-secondary:hover {
  background: #333333;
}

:global(.dark) .status-draft {
  background: #262626;
  color: #a3a3a3;
}

:global(.dark) .status-pending {
  background: #422006;
  color: #fbbf24;
}

:global(.dark) .status-approved {
  background: #052e16;
  color: #4ade80;
}

:global(.dark) .status-published {
  background: #083344;
  color: #22d3ee;
}
</style>
