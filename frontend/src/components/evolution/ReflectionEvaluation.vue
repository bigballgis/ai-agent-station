<template>
  <div class="reflection-evaluation">
    <a-card :title="t('evolution.reflection.title')" class="mb-6">
      <div class="flex flex-wrap gap-4 mb-6">
        <a-statistic
          v-for="metric in evaluationMetrics"
          :key="metric.key"
          :title="metric.title"
          :value="metric.value"
          :precision="2"
          class="w-64"
        >
          <template #prefix>
            <component :is="metric.icon" :class="metric.iconClass" />
          </template>
        </a-statistic>
      </div>

      <a-card :title="t('evolution.reflection.performanceTrend')" class="mb-6">
        <div class="h-80">
          <canvas ref="trendChart"></canvas>
        </div>
      </a-card>

      <a-card :title="t('evolution.reflection.detailResult')">
        <a-table :columns="evaluationColumns" :data-source="evaluationData" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <a-progress 
                :percent="record.score * 100" 
                :status="getScoreStatus(record.score)"
              />
            </template>
            <template v-else-if="column.key === 'trend'">
              <span :class="record.trend > 0 ? 'text-green-500 dark:text-green-400' : record.trend < 0 ? 'text-red-500 dark:text-red-400' : 'text-gray-500 dark:text-neutral-400'">
                {{ record.trend > 0 ? '↑' : record.trend < 0 ? '↓' : '→' }} {{ Math.abs(record.trend) }}%
              </span>
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
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import Chart from 'chart.js/auto'
import { getReflections, triggerEvaluation } from '@/api/reflection'

const { t } = useI18n()

const trendChart = ref<HTMLCanvasElement | null>(null)

const evaluationMetrics = ref<any[]>([])

const evaluationData = ref<any[]>([])

const evaluationColumns = computed(() => [
  {
    title: t('evolution.reflection.category'),
    dataIndex: 'category',
    key: 'category'
  },
  {
    title: t('evolution.reflection.score'),
    dataIndex: 'score',
    key: 'score',
    render: (score: number) => score * 100 + '%'
  },
  {
    title: t('evolution.reflection.trend'),
    dataIndex: 'trend',
    key: 'trend'
  },
  {
    title: t('evolution.reflection.description'),
    dataIndex: 'description',
    key: 'description'
  }
])

function getScoreStatus(score: number) {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'exception'
}

async function fetchReflectionData() {
  try {
    const res = await getReflections()
    const data = res.data?.data || res.data || {}
    // 从后端响应中提取评估指标和评估数据
    if (data.metrics) {
      evaluationMetrics.value = data.metrics
    }
    if (data.evaluations) {
      evaluationData.value = data.evaluations
    }
  } catch (e) {
    console.error(t('evolution.reflection.fetchFailed'), e)
  }
}

onMounted(async () => {
  await fetchReflectionData()
  if (trendChart.value) {
    new Chart(trendChart.value, {
      type: 'line',
      data: {
        labels: (t('evolution.reflection.months') as string[]),
        datasets: [
          {
            label: t('evolution.reflection.overallScore'),
            data: [72, 78, 82, 80, 83, 85.5],
            borderColor: '#1890ff',
            backgroundColor: 'rgba(24, 144, 255, 0.1)',
            tension: 0.4
          },
          {
            label: t('evolution.reflection.accuracy'),
            data: [85, 88, 90, 91, 91.5, 92.3],
            borderColor: '#52c41a',
            backgroundColor: 'rgba(82, 196, 26, 0.1)',
            tension: 0.4
          },
          {
            label: t('evolution.reflection.efficiency'),
            data: [85, 82, 80, 79, 79.5, 78.9],
            borderColor: '#faad14',
            backgroundColor: 'rgba(250, 173, 20, 0.1)',
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top'
          },
          tooltip: {
            mode: 'index',
            intersect: false
          }
        },
        scales: {
          y: {
            min: 60,
            max: 100,
            ticks: {
              callback: function(value) {
                return value + '%'
              }
            }
          }
        }
      }
    })
  }
})
</script>

<style scoped>
.reflection-evaluation {
  min-height: 600px;
}
</style>