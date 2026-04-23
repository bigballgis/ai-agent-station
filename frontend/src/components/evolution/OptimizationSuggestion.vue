<template>
  <div class="optimization-suggestion">
    <a-card title="优化建议管理" class="mb-6">
      <div class="flex flex-wrap gap-4 mb-6">
        <a-select v-model:value="selectedAgent" placeholder="选择Agent" class="w-64">
          <a-option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </a-option>
        </a-select>
        <a-select v-model:value="suggestionStatus" placeholder="建议状态" class="w-64">
          <a-option value="all">全部</a-option>
          <a-option value="pending">待处理</a-option>
          <a-option value="applied">已应用</a-option>
          <a-option value="rejected">已拒绝</a-option>
        </a-select>
        <a-button type="primary" @click="fetchSuggestions">
          <SearchOutlined /> 查询
        </a-button>
      </div>

      <a-card title="优化建议列表">
        <a-table :columns="suggestionColumns" :data-source="suggestions" :pagination="{ pageSize: 10 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'priority'">
              <a-tag :color="getPriorityColor(record.priority)">{{ record.priority }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button 
                v-if="record.status === '待处理'" 
                type="primary" 
                size="small" 
                @click="applySuggestion(record)"
              >
                应用
              </a-button>
              <a-button 
                v-if="record.status === '待处理'" 
                size="small" 
                @click="rejectSuggestion(record)"
              >
                拒绝
              </a-button>
              <a-button 
                size="small" 
                @click="viewDetails(record)"
              >
                详情
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-card>

    <!-- 建议详情模态框 -->
    <a-modal
      v-model:open="showDetailModal"
      title="建议详情"
      :width="600"
    >
      <div v-if="selectedSuggestion">
        <p><strong>建议ID:</strong> {{ selectedSuggestion.id }}</p>
        <p><strong>Agent名称:</strong> {{ selectedSuggestion.agentName }}</p>
        <p><strong>建议类型:</strong> {{ selectedSuggestion.type }}</p>
        <p><strong>优先级:</strong> <a-tag :color="getPriorityColor(selectedSuggestion.priority)">{{ selectedSuggestion.priority }}</a-tag></p>
        <p><strong>建议内容:</strong> {{ selectedSuggestion.content }}</p>
        <p><strong>预期效果:</strong> {{ selectedSuggestion.expectedEffect }}</p>
        <p><strong>创建时间:</strong> {{ selectedSuggestion.createdAt }}</p>
        <p><strong>状态:</strong> <a-tag :color="getStatusColor(selectedSuggestion.status)">{{ selectedSuggestion.status }}</a-tag></p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const selectedAgent = ref<string>('1')
const suggestionStatus = ref<string>('all')
const showDetailModal = ref(false)
const selectedSuggestion = ref<any>(null)

const agents = [
  { id: '1', name: '客服Agent' },
  { id: '2', name: '销售Agent' },
  { id: '3', name: '技术支持Agent' }
]

const suggestions = [
  {
    id: 1,
    agentId: '1',
    agentName: '客服Agent',
    type: '性能优化',
    priority: '高',
    content: '优化响应速度，减少用户等待时间',
    expectedEffect: '响应时间减少30%，用户满意度提升',
    status: '待处理',
    createdAt: '2024-04-20 14:30:00'
  },
  {
    id: 2,
    agentId: '1',
    agentName: '客服Agent',
    type: '知识更新',
    priority: '中',
    content: '更新产品知识库，提高回答准确性',
    expectedEffect: '回答准确率提升20%',
    status: '已应用',
    createdAt: '2024-04-19 10:15:00'
  },
  {
    id: 3,
    agentId: '1',
    agentName: '客服Agent',
    type: '流程优化',
    priority: '中',
    content: '优化处理流程，提高工作效率',
    expectedEffect: '处理效率提升25%',
    status: '待处理',
    createdAt: '2024-04-18 16:45:00'
  },
  {
    id: 4,
    agentId: '1',
    agentName: '客服Agent',
    type: '话术优化',
    priority: '低',
    content: '优化沟通话术，提升用户体验',
    expectedEffect: '用户满意度提升15%',
    status: '已拒绝',
    createdAt: '2024-04-17 09:30:00'
  },
  {
    id: 5,
    agentId: '1',
    agentName: '客服Agent',
    type: '性能优化',
    priority: '高',
    content: '优化系统稳定性，减少崩溃率',
    expectedEffect: '系统崩溃率降低50%',
    status: '待处理',
    createdAt: '2024-04-16 11:20:00'
  }
]

const suggestionColumns = [
  {
    title: '建议ID',
    dataIndex: 'id',
    key: 'id'
  },
  {
    title: 'Agent名称',
    dataIndex: 'agentName',
    key: 'agentName'
  },
  {
    title: '建议类型',
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    key: 'priority'
  },
  {
    title: '建议内容',
    dataIndex: 'content',
    key: 'content'
  },
  {
    title: '预期效果',
    dataIndex: 'expectedEffect',
    key: 'expectedEffect'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt'
  },
  {
    title: '操作',
    key: 'actions',
    width: 150
  }
]

function getPriorityColor(priority: string) {
  switch (priority) {
    case '高': return 'red'
    case '中': return 'orange'
    case '低': return 'blue'
    default: return 'default'
  }
}

function getStatusColor(status: string) {
  switch (status) {
    case '待处理': return 'blue'
    case '已应用': return 'green'
    case '已拒绝': return 'red'
    default: return 'default'
  }
}

function fetchSuggestions() {
  // 模拟数据获取
  if (import.meta.env.DEV) {
    console.debug('Fetching suggestions for agent:', selectedAgent.value)
    console.debug('Status:', suggestionStatus.value)
  }
  // 这里可以添加实际的API调用
}

function applySuggestion(_suggestion: any) {
  message.success('建议已应用')
  // 这里可以添加实际的API调用
}

function rejectSuggestion(_suggestion: any) {
  message.warning('建议已拒绝')
  // 这里可以添加实际的API调用
}

function viewDetails(suggestion: any) {
  selectedSuggestion.value = suggestion
  showDetailModal.value = true
}
</script>

<style scoped>
.optimization-suggestion {
  min-height: 600px;
}
</style>