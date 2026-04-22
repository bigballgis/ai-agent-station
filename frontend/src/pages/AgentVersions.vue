<template>
  <div class="agent-versions-page">
    <div class="page-header">
      <button @click="goBack" class="btn btn-secondary">返回</button>
      <h1>版本管理 - {{ agentName }}</h1>
    </div>

    <div class="versions-list">
      <div
        v-for="version in versions"
        :key="version.id"
        class="version-card"
      >
        <div class="version-header">
          <div class="version-number">v{{ version.versionNumber }}</div>
          <div class="version-date">{{ formatDate(version.createdAt) }}</div>
        </div>
        <div class="version-content">
          <p class="change-log">{{ version.changeLog || '暂无变更记录' }}</p>
        </div>
        <div class="version-actions">
          <button @click="rollbackToVersion(version)" class="btn btn-secondary">回滚到此版本</button>
          <button @click="submitForApproval(version)" class="btn btn-primary">提交审批</button>
        </div>
      </div>
    </div>

    <div v-if="versions.length === 0" class="empty-state">
      <p>暂无版本记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { agentApi, type AgentVersion } from '@/api/agent'
import { approvalApi } from '@/api/approval'
import { testApi } from '@/api/test'

const router = useRouter()
const route = useRoute()
const versions = ref<AgentVersion[]>([])
const agentName = ref('')
const loading = ref(false)

async function loadVersions() {
  loading.value = true
  try {
    const [agentRes, versionsRes] = await Promise.all([
      agentApi.getAgentById(route.params.id as string),
      agentApi.getAgentVersions(route.params.id as string)
    ])
    agentName.value = agentRes.data.name
    versions.value = versionsRes.data || []
  } catch (error) {
    message.error('加载版本失败')
  } finally {
    loading.value = false
  }
}

function rollbackToVersion(version: AgentVersion) {
  Modal.confirm({
    title: '确认回滚',
    content: `确定要回滚到版本 v${version.versionNumber} 吗？`,
    onOk: async () => {
      try {
        await agentApi.rollbackToVersion(route.params.id as string, version.versionNumber)
        message.success('回滚成功')
        loadVersions()
      } catch (error) {
        message.error('回滚失败')
      }
    }
  })
}

function formatDate(date: string | undefined) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function submitForApproval(version: AgentVersion) {
  Modal.confirm({
    title: '提交审批',
    content: '确定要提交此版本进行审批吗？系统将检查测试结果。',
    onOk: async () => {
      try {
        // 检查测试结果
        const testResults = await testApi.getTestResults({
          agentId: Number(route.params.id),
          status: 'passed'
        })
        
        if (testResults.data.length === 0) {
          message.warning('请先运行测试并确保测试通过后再提交审批')
          return
        }
        
        // 提交审批
        await approvalApi.submitForApproval(Number(route.params.id), version.versionNumber)
        message.success('提交审批成功')
      } catch (error) {
        message.error('提交审批失败')
      }
    }
  })
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadVersions()
})
</script>

<style scoped>
.agent-versions-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.versions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 800px;
}

.version-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s;
}

.version-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.version-number {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}

.version-date {
  color: #999;
  font-size: 14px;
}

.change-log {
  color: #666;
  margin: 0 0 16px 0;
  font-size: 14px;
}

.version-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: #999;
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
</style>
