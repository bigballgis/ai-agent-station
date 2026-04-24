<template>
  <div class="experience-data">
    <a-card :title="t('evolution.experience.title')" class="mb-6">
      <div class="flex flex-wrap gap-4 mb-6">
        <a-select v-model:value="selectedAgent" :placeholder="t('evolution.experience.selectAgent')" class="w-64">
          <a-option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </a-option>
        </a-select>
        <a-date-picker v-model:value="dateRange" range-picker :placeholder="t('evolution.experience.selectTimeRange')" class="w-64" />
        <a-button type="primary" @click="fetchExperienceData">
          <SearchOutlined /> {{ t('evolution.experience.query') }}
        </a-button>
      </div>

      <a-card :title="t('evolution.experience.statistics')" class="mb-6">
        <div class="grid grid-cols-2 gap-6">
          <div class="h-64">
            <canvas ref="experienceChart"></canvas>
          </div>
          <div class="h-64">
            <canvas ref="categoryChart"></canvas>
          </div>
        </div>
      </a-card>

      <a-card :title="t('evolution.experience.list')">
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
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined } from '@ant-design/icons-vue'
import Chart from 'chart.js/auto'
import { getExperiences } from '@/api/experience'

const { t } = useI18n()

const selectedAgent = ref<string>('1')
const dateRange = ref<any>(null)

const experienceChart = ref<HTMLCanvasElement | null>(null)
const categoryChart = ref<HTMLCanvasElement | null>(null)

const agents = ref<Array<{ id: string; name: string }>>([])

const experienceData = ref<any[]>([])

const experienceColumns = computed(() => [
  {
    title: t('evolution.experience.agentName'),
    dataIndex: 'agentName',
    key: 'agentName'
  },
  {
    title: t('evolution.experience.experienceType'),
    dataIndex: 'experienceType',
    key: 'experienceType'
  },
  {
    title: t('evolution.experience.content'),
    dataIndex: 'content',
    key: 'content'
  },
  {
    title: t('evolution.experience.rating'),
    dataIndex: 'rating',
    key: 'rating'
  },
  {
    title: t('evolution.experience.tags'),
    dataIndex: 'tags',
    key: 'tags'
  },
  {
    title: t('evolution.experience.createdAt'),
    dataIndex: 'createdAt',
    key: 'createdAt'
  }
])

function getTypeColor(type: string) {
  switch (type) {
    case t('evolution.experience.typeSuccess'): return 'green'
    case t('evolution.experience.typeFail'): return 'red'
    case t('evolution.experience.typeLearn'): return 'blue'
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
    console.error(t('evolution.experience.fetchFailed'), e)
  }
}

onMounted(async () => {
  await fetchExperienceData()
  // 初始化经验数据趋势图
  if (experienceChart.value) {
    new Chart(experienceChart.value, {
      type: 'bar',
      data: {
        labels: (t('evolution.experience.months') as string[]),
        datasets: [
          {
            label: t('evolution.experience.typeSuccess'),
            data: [12, 19, 15, 20, 25, 30],
            backgroundColor: 'rgba(82, 196, 26, 0.6)'
          },
          {
            label: t('evolution.experience.typeFail'),
            data: [5, 8, 6, 4, 3, 2],
            backgroundColor: 'rgba(245, 34, 45, 0.6)'
          },
          {
            label: t('evolution.experience.typeLearn'),
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
        labels: (t('evolution.experience.categories') as string[]),
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