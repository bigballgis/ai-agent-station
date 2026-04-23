<template>
  <div class="optimization-suggestion">
    <a-card :title="t('optimizationSuggestion.management')" class="mb-6">
      <div class="flex flex-wrap gap-4 mb-6">
        <a-select v-model:value="selectedAgent" :placeholder="t('optimizationSuggestion.selectAgent')" class="w-64">
          <a-option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </a-option>
        </a-select>
        <a-select v-model:value="suggestionStatus" :placeholder="t('optimizationSuggestion.suggestionStatus')" class="w-64">
          <a-option value="all">{{ t('optimizationSuggestion.all') }}</a-option>
          <a-option value="pending">{{ t('optimizationSuggestion.pending') }}</a-option>
          <a-option value="applied">{{ t('optimizationSuggestion.applied') }}</a-option>
          <a-option value="rejected">{{ t('optimizationSuggestion.rejected') }}</a-option>
        </a-select>
        <a-button type="primary" @click="fetchSuggestions">
          <SearchOutlined /> {{ t('optimizationSuggestion.query') }}
        </a-button>
      </div>

      <a-card :title="t('optimizationSuggestion.list')">
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
                v-if="record.status === t('optimizationSuggestion.pending')"
                type="primary"
                size="small"
                @click="applySuggestion(record)"
              >
                {{ t('optimizationSuggestion.apply') }}
              </a-button>
              <a-button
                v-if="record.status === t('optimizationSuggestion.pending')"
                size="small"
                @click="rejectSuggestion(record)"
              >
                {{ t('optimizationSuggestion.reject') }}
              </a-button>
              <a-button
                size="small"
                @click="viewDetails(record)"
              >
                {{ t('optimizationSuggestion.detail') }}
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-card>

    <!-- 建议详情模态框 -->
    <a-modal
      v-model:open="showDetailModal"
      :title="t('optimizationSuggestion.detailTitle')"
      :width="600"
    >
      <div v-if="selectedSuggestion">
        <p><strong>{{ t('optimizationSuggestion.suggestionId') }}:</strong> {{ selectedSuggestion.id }}</p>
        <p><strong>{{ t('optimizationSuggestion.agentName') }}:</strong> {{ selectedSuggestion.agentName }}</p>
        <p><strong>{{ t('optimizationSuggestion.suggestionType') }}:</strong> {{ selectedSuggestion.type }}</p>
        <p><strong>{{ t('optimizationSuggestion.priority') }}:</strong> <a-tag :color="getPriorityColor(selectedSuggestion.priority)">{{ selectedSuggestion.priority }}</a-tag></p>
        <p><strong>{{ t('optimizationSuggestion.suggestionContent') }}:</strong> {{ selectedSuggestion.content }}</p>
        <p><strong>{{ t('optimizationSuggestion.expectedEffect') }}:</strong> {{ selectedSuggestion.expectedEffect }}</p>
        <p><strong>{{ t('optimizationSuggestion.createdAt') }}:</strong> {{ selectedSuggestion.createdAt }}</p>
        <p><strong>{{ t('common.status') }}:</strong> <a-tag :color="getStatusColor(selectedSuggestion.status)">{{ selectedSuggestion.status }}</a-tag></p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const { t } = useI18n()

const selectedAgent = ref<string>('1')
const suggestionStatus = ref<string>('all')
const showDetailModal = ref(false)
const selectedSuggestion = ref<any>(null)

const agents = [
  { id: '1', name: 'Customer Service Agent' },
  { id: '2', name: 'Sales Agent' },
  { id: '3', name: 'Tech Support Agent' }
]

const suggestions = [
  {
    id: 1,
    agentId: '1',
    agentName: 'Customer Service Agent',
    type: t('optimizationSuggestion.performanceOptimization'),
    priority: t('optimizationSuggestion.high'),
    content: t('optimizationSuggestion.systemStability'),
    expectedEffect: 'Response time reduced by 30%',
    status: t('optimizationSuggestion.pending'),
    createdAt: '2024-04-20 14:30:00'
  },
  {
    id: 2,
    agentId: '1',
    agentName: 'Customer Service Agent',
    type: t('optimizationSuggestion.knowledgeUpdate'),
    priority: t('optimizationSuggestion.medium'),
    content: 'Update product knowledge base for better accuracy',
    expectedEffect: 'Answer accuracy improved by 20%',
    status: t('optimizationSuggestion.applied'),
    createdAt: '2024-04-19 10:15:00'
  },
  {
    id: 3,
    agentId: '1',
    agentName: 'Customer Service Agent',
    type: t('optimizationSuggestion.processOptimization'),
    priority: t('optimizationSuggestion.medium'),
    content: 'Optimize processing flow for better efficiency',
    expectedEffect: 'Processing efficiency improved by 25%',
    status: t('optimizationSuggestion.pending'),
    createdAt: '2024-04-18 16:45:00'
  },
  {
    id: 4,
    agentId: '1',
    agentName: 'Customer Service Agent',
    type: t('optimizationSuggestion.scriptOptimization'),
    priority: t('optimizationSuggestion.low'),
    content: 'Optimize communication scripts for better UX',
    expectedEffect: 'User satisfaction improved by 15%',
    status: t('optimizationSuggestion.rejected'),
    createdAt: '2024-04-17 09:30:00'
  },
  {
    id: 5,
    agentId: '1',
    agentName: 'Customer Service Agent',
    type: t('optimizationSuggestion.performanceOptimization'),
    priority: t('optimizationSuggestion.high'),
    content: t('optimizationSuggestion.systemStability'),
    expectedEffect: 'System crash rate reduced by 50%',
    status: t('optimizationSuggestion.pending'),
    createdAt: '2024-04-16 11:20:00'
  }
]

const suggestionColumns = computed(() => [
  {
    title: t('optimizationSuggestion.suggestionId'),
    dataIndex: 'id',
    key: 'id'
  },
  {
    title: t('optimizationSuggestion.agentName'),
    dataIndex: 'agentName',
    key: 'agentName'
  },
  {
    title: t('optimizationSuggestion.suggestionType'),
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: t('optimizationSuggestion.priority'),
    dataIndex: 'priority',
    key: 'priority'
  },
  {
    title: t('optimizationSuggestion.suggestionContent'),
    dataIndex: 'content',
    key: 'content'
  },
  {
    title: t('optimizationSuggestion.expectedEffect'),
    dataIndex: 'expectedEffect',
    key: 'expectedEffect'
  },
  {
    title: t('common.status'),
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: t('common.createdAt'),
    dataIndex: 'createdAt',
    key: 'createdAt'
  },
  {
    title: t('common.actions'),
    key: 'actions',
    width: 150
  }
])

function getPriorityColor(priority: string) {
  if (priority === t('optimizationSuggestion.high')) return 'red'
  if (priority === t('optimizationSuggestion.medium')) return 'orange'
  if (priority === t('optimizationSuggestion.low')) return 'blue'
  return 'default'
}

function getStatusColor(status: string) {
  if (status === t('optimizationSuggestion.pending')) return 'blue'
  if (status === t('optimizationSuggestion.applied')) return 'green'
  if (status === t('optimizationSuggestion.rejected')) return 'red'
  return 'default'
}

function fetchSuggestions() {
  if (import.meta.env.DEV) {
    console.debug('Fetching suggestions for agent:', selectedAgent.value)
    console.debug('Status:', suggestionStatus.value)
  }
}

function applySuggestion(_suggestion: any) {
  message.success(t('optimizationSuggestion.applySuccess'))
}

function rejectSuggestion(_suggestion: any) {
  message.warning(t('optimizationSuggestion.rejectSuccess'))
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
