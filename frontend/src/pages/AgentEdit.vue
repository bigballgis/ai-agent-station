<template>
  <div class="agent-edit-page">
    <div class="edit-header">
      <button @click="goBack" class="btn btn-secondary">返回</button>
      <h1>编辑Agent</h1>
      <div class="header-actions">
        <button @click="submitForApproval" class="btn btn-secondary">提交审批</button>
        <button @click="deployAgent" class="btn btn-success">发布</button>
        <button @click="saveAgent" class="btn btn-primary">保存</button>
      </div>
    </div>

    <div class="info-section">
      <div class="info-item">
        <label>名称:</label>
        <input v-model="agent.name" placeholder="请输入Agent名称" />
      </div>
      <div class="info-item">
        <label>描述:</label>
        <textarea v-model="agent.description" placeholder="请输入Agent描述" rows="2"></textarea>
      </div>
      <div class="info-item">
        <label>状态:</label>
        <select v-model="agent.isActive">
          <option :value="true">启用</option>
          <option :value="false">禁用</option>
        </select>
      </div>
      <div class="info-item">
        <label>当前状态:</label>
        <span class="status-tag" :class="getAgentStatusClass(agent.status || '')">{{ getAgentStatusText(agent.status || '') }}</span>
      </div>
    </div>

    <div class="canvas-section">
      <div class="designer-redirect">
        <p>请使用可视化设计器编辑 Agent 工作流</p>
        <router-link :to="`/agents/design/${route.params.id}`" class="btn btn-primary">
          打开设计器
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { agentApi, type Agent } from '@/api/agent'
import { approvalApi } from '@/api/approval'
import { deploymentApi } from '@/api/deployment'

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
    message.error('加载Agent失败')
  } finally {
    loading.value = false
  }
}

async function saveAgent() {
  if (!agent.value.name) {
    message.error('请输入Agent名称')
    return
  }

  loading.value = true
  try {
    await agentApi.updateAgent(route.params.id as string, agent.value)
    message.success('保存成功')
  } catch (error) {
    message.error('保存失败')
  } finally {
    loading.value = false
  }
}

async function submitForApproval() {
  Modal.confirm({
    title: '提交审批',
    content: '确定要提交此Agent进行审批吗？',
    onOk: async () => {
      loading.value = true
      try {
        await approvalApi.submitForApproval(Number(route.params.id), agent.value.latestVersionId || 0, '提交审批')
        message.success('提交审批成功')
        // 重新加载Agent状态
        await loadAgent()
      } catch (error) {
        message.error('提交审批失败')
      } finally {
        loading.value = false
      }
    }
  })
}

async function deployAgent() {
  Modal.confirm({
    title: '发布Agent',
    content: '确定要发布此Agent吗？',
    onOk: async () => {
      loading.value = true
      try {
        await deploymentApi.deploy(Number(route.params.id), agent.value.latestVersionId || 0, false, 100, '发布Agent')
        message.success('发布成功')
        // 重新加载Agent状态
        await loadAgent()
      } catch (error) {
        message.error('发布失败')
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
      return '草稿'
    case 'PENDING_APPROVAL':
      return '待审批'
    case 'APPROVED':
      return '已审批'
    case 'PUBLISHED':
      return '已发布'
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
