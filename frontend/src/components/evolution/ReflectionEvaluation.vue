<template>
  <div class="reflection-evaluation">
    <a-card title="反思评估结果" class="mb-6">
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

      <a-card title="性能趋势" class="mb-6">
        <div class="h-80">
          <canvas ref="trendChart"></canvas>
        </div>
      </a-card>

      <a-card title="详细评估结果">
        <a-table :columns="evaluationColumns" :data-source="evaluationData" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <a-progress 
                :percent="record.score * 100" 
                :status="getScoreStatus(record.score)"
              />
            </template>
            <template v-else-if="column.key === 'trend'">
              <span :class="record.trend > 0 ? 'text-green-500' : record.trend < 0 ? 'text-red-500' : 'text-gray-500'">
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
import { ref, onMounted } from 'vue'
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import Chart from 'chart.js/auto'

const trendChart = ref<HTMLCanvasElement | null>(null)

const evaluationMetrics = [
  {
    key: 'overallScore',
    title: '总体评分',
    value: 85.5,
    icon: CheckCircleOutlined,
    iconClass: 'text-green-500'
  },
  {
    key: 'accuracy',
    title: '准确率',
    value: 92.3,
    icon: CheckCircleOutlined,
    iconClass: 'text-green-500'
  },
  {
    key: 'efficiency',
    title: '效率',
    value: 78.9,
    icon: WarningOutlined,
    iconClass: 'text-yellow-500'
  },
  {
    key: 'reliability',
    title: '可靠性',
    value: 88.7,
    icon: CheckCircleOutlined,
    iconClass: 'text-green-500'
  }
]

const evaluationData = [
  {
    id: 1,
    category: '任务完成度',
    score: 0.92,
    trend: 5.2,
    description: '任务完成率较高，能够准确理解用户需求'
  },
  {
    id: 2,
    category: '响应速度',
    score: 0.75,
    trend: -2.1,
    description: '响应速度有所下降，需要优化处理逻辑'
  },
  {
    id: 3,
    category: '准确性',
    score: 0.88,
    trend: 3.5,
    description: '回答准确性有明显提升'
  },
  {
    id: 4,
    category: '稳定性',
    score: 0.95,
    trend: 1.2,
    description: '系统运行稳定，无异常情况'
  },
  {
    id: 5,
    category: '用户满意度',
    score: 0.82,
    trend: 4.7,
    description: '用户反馈积极，满意度提升'
  }
]

const evaluationColumns = [
  {
    title: '评估类别',
    dataIndex: 'category',
    key: 'category'
  },
  {
    title: '评分',
    dataIndex: 'score',
    key: 'score',
    render: (score: number) => score * 100 + '%'
  },
  {
    title: '趋势',
    dataIndex: 'trend',
    key: 'trend'
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description'
  }
]

function getScoreStatus(score: number) {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'exception'
}

onMounted(() => {
  if (trendChart.value) {
    new Chart(trendChart.value, {
      type: 'line',
      data: {
        labels: ['1月', '2月', '3月', '4月', '5月', '6月'],
        datasets: [
          {
            label: '总体评分',
            data: [72, 78, 82, 80, 83, 85.5],
            borderColor: '#1890ff',
            backgroundColor: 'rgba(24, 144, 255, 0.1)',
            tension: 0.4
          },
          {
            label: '准确率',
            data: [85, 88, 90, 91, 91.5, 92.3],
            borderColor: '#52c41a',
            backgroundColor: 'rgba(82, 196, 26, 0.1)',
            tension: 0.4
          },
          {
            label: '效率',
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