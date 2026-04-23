<template>
  <div class="space-y-6">
    <!-- 系统概览 -->
    <div class="bg-white rounded-lg shadow-sm p-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">系统概览</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Agent数量 -->
        <div class="bg-blue-50 rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-600">Agent数量</p>
              <h3 class="text-2xl font-bold text-blue-600">{{ agentCount }}</h3>
            </div>
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
              <span class="text-blue-600 text-xl">🤖</span>
            </div>
          </div>
        </div>
        
        <!-- API调用次数 -->
        <div class="bg-green-50 rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-600">API调用次数</p>
              <h3 class="text-2xl font-bold text-green-600">{{ apiCallCount.toLocaleString() }}</h3>
            </div>
            <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
              <span class="text-green-600 text-xl">🔌</span>
            </div>
          </div>
        </div>
        
        <!-- 租户数量 -->
        <div class="bg-purple-50 rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-600">租户数量</p>
              <h3 class="text-2xl font-bold text-purple-600">5</h3>
            </div>
            <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
              <span class="text-purple-600 text-xl">🏢</span>
            </div>
          </div>
        </div>
        
        <!-- 待审批 -->
        <div class="bg-yellow-50 rounded-lg p-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-600">待审批</p>
              <h3 class="text-2xl font-bold text-yellow-600">{{ pendingApprovalCount }}</h3>
            </div>
            <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center">
              <span class="text-yellow-600 text-xl">✅</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 快速入口 -->
    <div class="bg-white rounded-lg shadow-sm p-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">快速入口</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <router-link to="/agents/design" class="bg-blue-50 hover:bg-blue-100 transition-colors rounded-lg p-4 text-center">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-2">
            <span class="text-blue-600 text-xl">🎨</span>
          </div>
          <p class="text-sm font-medium text-gray-900">新建Agent</p>
        </router-link>
        
        <router-link to="/agents/approval" class="bg-green-50 hover:bg-green-100 transition-colors rounded-lg p-4 text-center">
          <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-2">
            <span class="text-green-600 text-xl">✅</span>
          </div>
          <p class="text-sm font-medium text-gray-900">审批中心</p>
        </router-link>
        
        <router-link to="/api/manage" class="bg-purple-50 hover:bg-purple-100 transition-colors rounded-lg p-4 text-center">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center mx-auto mb-2">
            <span class="text-purple-600 text-xl">🔌</span>
          </div>
          <p class="text-sm font-medium text-gray-900">API管理</p>
        </router-link>
        
        <router-link to="/tenant" class="bg-yellow-50 hover:bg-yellow-100 transition-colors rounded-lg p-4 text-center">
          <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center mx-auto mb-2">
            <span class="text-yellow-600 text-xl">🏢</span>
          </div>
          <p class="text-sm font-medium text-gray-900">租户管理</p>
        </router-link>
      </div>
    </div>
    
    <!-- 最近活动 -->
    <div class="bg-white rounded-lg shadow-sm p-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">最近活动</h2>
      <div class="space-y-4">
        <div class="flex items-start">
          <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center mr-3">
            <span class="text-blue-600">📋</span>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">Agent "客服助手" 已发布</p>
            <p class="text-xs text-gray-500">2026-04-21 10:30</p>
          </div>
        </div>
        <div class="flex items-start">
          <div class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center mr-3">
            <span class="text-green-600">✅</span>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">Agent "销售助手" 审批通过</p>
            <p class="text-xs text-gray-500">2026-04-21 09:15</p>
          </div>
        </div>
        <div class="flex items-start">
          <div class="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center mr-3">
            <span class="text-purple-600">🤖</span>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">创建新Agent "技术支持"</p>
            <p class="text-xs text-gray-500">2026-04-20 16:45</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAllAgents } from '@/api/agent'
import { getAlertStats } from '@/api/alert'
import { getToolStats } from '@/api/tool'

const agentCount = ref(0)
const apiCallCount = ref(0)
const pendingApprovalCount = ref(0)

async function fetchHomeData() {
  try {
    // Fetch agents
    const agentsRes: any = await getAllAgents()
    const agentsData = agentsRes?.data || agentsRes || []
    const agentsList = Array.isArray(agentsData) ? agentsData : []
    agentCount.value = agentsList.length

    // Fetch tool stats for API calls
    try {
      const toolRes: any = await getToolStats()
      const toolData = toolRes?.data || toolRes || {}
      apiCallCount.value = toolData.totalCalls || toolData.apiCalls || 0
    } catch {
      apiCallCount.value = 0
    }

    // Fetch alert stats for pending approvals
    try {
      const alertRes: any = await getAlertStats()
      const alertData = alertRes?.data || alertRes || {}
      pendingApprovalCount.value = alertData.pendingApprovals || alertData.pendingCount || 0
    } catch {
      pendingApprovalCount.value = 0
    }
  } catch (error: any) {
    message.error('获取首页数据失败: ' + (error.message || '未知错误'))
  }
}

onMounted(() => {
  fetchHomeData()
})
</script>

<style scoped>
/* Add your scoped styles here */
</style>
