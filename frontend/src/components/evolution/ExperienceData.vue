<template>
  <div class="experience-data">
    <a-card title="经验数据分析" class="mb-6">
      <div class="flex flex-wrap gap-4 mb-6">
        <a-select v-model:value="selectedAgent" placeholder="选择Agent" class="w-64">
          <a-option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </a-option>
        </a-select>
        <a-date-picker v-model:value="dateRange" range-picker placeholder="选择时间范围" class="w-64" />
        <a-button type="primary" @click="fetchExperienceData">
          <SearchOutlined /> 查询
        </a-button>
      </div>

      <a-card title="经验数据统计" class="mb-6">
        <div class="grid grid-cols-2 gap-6">
          <div class="h-64">
            <canvas ref="experienceChart"></canvas>
          </div>
          <div class="h-64">
            <canvas ref="categoryChart"></canvas>
          </div>
        </div>
      </a-card>

      <a-card title="经验数据列表">
        <a-table :columns="experienceColumns" :data-source="experienceData" :pagination="{ pageSize: 10 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'experienceType'">
              <a-tag :color="getTypeColor(record.experienceType)">{{ record.experienceType }}</a-tag>
            </template>
            <template v-else-if="column.key === 'rating'">
              <a-rate :value="record.rating" disabled />
            </template>
          </template>
        </a-table>
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import Chart from 'chart.js/auto'

const selectedAgent = ref<string>('1')
const dateRange = ref<any>(null)

const experienceChart = ref<HTMLCanvasElement | null>(null)
const categoryChart = ref<HTMLCanvasElement | null>(null)

const agents = [
  { id: '1', name: '客服Agent' },
  { id: '2', name: '销售Agent' },
  { id: '3', name: '技术支持Agent' }
]

const experienceData = [
  {
    id: 1,
    agentId: '1',
    agentName: '客服Agent',
    experienceType: '成功经验',
    content: '成功解决了用户关于产品使用的复杂问题，用户满意度很高',
    rating: 5,
    tags: ['用户服务', '问题解决'],
    createdAt: '2024-04-20 14:30:00'
  },
  {
    id: 2,
    agentId: '1',
    agentName: '客服Agent',
    experienceType: '失败经验',
    content: '未能及时响应用户的紧急请求，导致用户投诉',
    rating: 2,
    tags: ['响应速度', '用户投诉'],
    createdAt: '2024-04-19 10:15:00'
  },
  {
    id: 3,
    agentId: '1',
    agentName: '客服Agent',
    experienceType: '成功经验',
    content: '通过耐心沟通，成功挽回了一位即将流失的客户',
    rating: 5,
    tags: ['客户挽留', '沟通技巧'],
    createdAt: '2024-04-18 16:45:00'
  },
  {
    id: 4,
    agentId: '1',
    agentName: '客服Agent',
    experienceType: '学习经验',
    content: '学习了新的产品知识，能够更好地解答用户问题',
    rating: 4,
    tags: ['知识学习', '自我提升'],
    createdAt: '2024-04-17 09:30:00'
  },
  {
    id: 5,
    agentId: '1',
    agentName: '客服Agent',
    experienceType: '成功经验',
    content: '快速处理了批量用户的咨询，提高了工作效率',
    rating: 4,
    tags: ['效率提升', '批量处理'],
    createdAt: '2024-04-16 11:20:00'
  }
]

const experienceColumns = [
  {
    title: 'Agent名称',
    dataIndex: 'agentName',
    key: 'agentName'
  },
  {
    title: '经验类型',
    dataIndex: 'experienceType',
    key: 'experienceType'
  },
  {
    title: '经验内容',
    dataIndex: 'content',
    key: 'content'
  },
  {
    title: '评分',
    dataIndex: 'rating',
    key: 'rating'
  },
  {
    title: '标签',
    dataIndex: 'tags',
    key: 'tags'
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt'
  }
]

function getTypeColor(type: string) {
  switch (type) {
    case '成功经验': return 'green'
    case '失败经验': return 'red'
    case '学习经验': return 'blue'
    default: return 'default'
  }
}

function fetchExperienceData() {
  // 模拟数据获取
  if (import.meta.env.DEV) {
    console.log('Fetching experience data for agent:', selectedAgent.value)
    console.log('Date range:', dateRange.value)
  }
  // 这里可以添加实际的API调用
}

onMounted(() => {
  // 初始化经验数据趋势图
  if (experienceChart.value) {
    new Chart(experienceChart.value, {
      type: 'bar',
      data: {
        labels: ['1月', '2月', '3月', '4月', '5月', '6月'],
        datasets: [
          {
            label: '成功经验',
            data: [12, 19, 15, 20, 25, 30],
            backgroundColor: 'rgba(82, 196, 26, 0.6)'
          },
          {
            label: '失败经验',
            data: [5, 8, 6, 4, 3, 2],
            backgroundColor: 'rgba(245, 34, 45, 0.6)'
          },
          {
            label: '学习经验',
            data: [8, 12, 10, 15, 18, 22],
            backgroundColor: 'rgba(24, 144, 255, 0.6)'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    })
  }

  // 初始化经验类别分布图
  if (categoryChart.value) {
    new Chart(categoryChart.value, {
      type: 'doughnut',
      data: {
        labels: ['用户服务', '问题解决', '响应速度', '客户挽留', '沟通技巧', '知识学习'],
        datasets: [
          {
            data: [35, 25, 15, 10, 10, 5],
            backgroundColor: [
              '#1890ff',
              '#52c41a',
              '#faad14',
              '#f5222d',
              '#722ed1',
              '#eb2f96'
            ]
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    })
  }
})
</script>

<style scoped>
.experience-data {
  min-height: 600px;
}
</style>