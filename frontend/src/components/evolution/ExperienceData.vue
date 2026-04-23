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
import { getExperiences } from '@/api/experience'

const selectedAgent = ref<string>('1')
const dateRange = ref<any>(null)

const experienceChart = ref<HTMLCanvasElement | null>(null)
const categoryChart = ref<HTMLCanvasElement | null>(null)

const agents = ref<Array<{ id: string; name: string }>>([])

const experienceData = ref<any[]>([])

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

async function fetchExperienceData() {
  try {
    const params: Record<string, unknown> = {}
    if (selectedAgent.value) {
      params.agentId = selectedAgent.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res = await getExperiences(params)
    experienceData.value = res.data?.data || res.data || []
  } catch (e) {
    console.error('获取经验数据失败:', e)
  }
}

onMounted(async () => {
  await fetchExperienceData()
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