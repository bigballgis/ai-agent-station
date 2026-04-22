<template>
  <div class="dashboard-page">
    <!-- 页面标题 -->
    <div class="mb-8 animate-fade-in">
      <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
        {{ t('dashboard.title') || 'Dashboard' }}
      </h1>
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
        {{ t('dashboard.subtitle') }}
      </p>
    </div>

    <!-- 统计卡片区域 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
      <div
        v-for="(stat, index) in stats"
        :key="stat.label"
        class="stat-card rounded-2xl p-6 text-white relative overflow-hidden cursor-pointer transition-all duration-300 hover:-translate-y-1 hover:shadow-float group"
        :class="stat.gradient"
        :style="{ animationDelay: `${index * 80}ms` }"
      >
        <!-- 背景装饰圆 -->
        <div class="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/10 group-hover:scale-125 transition-transform duration-500"></div>
        <div class="absolute -right-2 -bottom-6 w-16 h-16 rounded-full bg-white/5"></div>

        <!-- 图标 -->
        <div class="absolute right-5 top-5 w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center">
          <component :is="stat.icon" class="text-xl text-white/90" />
        </div>

        <!-- 内容 -->
        <div class="relative z-10">
          <p class="text-sm text-white/80 font-medium">{{ stat.label }}</p>
          <p class="text-3xl font-bold mt-2 tracking-tight">{{ stat.displayValue }}</p>
          <div class="flex items-center gap-1.5 mt-3">
            <svg
              v-if="stat.trendDirection === 'up'"
              class="w-4 h-4 text-white/80"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
            </svg>
            <svg
              v-else
              class="w-4 h-4 text-white/80"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
            </svg>
            <span class="text-sm text-white/80">{{ stat.trend }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5 mb-8">
      <!-- API 调用趋势折线图 -->
      <div class="lg:col-span-2 bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up">
        <div class="flex items-center justify-between mb-5">
          <div>
            <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('dashboard.apiTrend') }}</h2>
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ t('dashboard.apiTrendDesc') }}</p>
          </div>
          <div class="flex items-center gap-4 text-xs text-neutral-500 dark:text-neutral-400">
            <span class="flex items-center gap-1.5">
              <span class="w-2.5 h-2.5 rounded-full bg-primary-500"></span>
              {{ t('dashboard.callVolume') }}
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-2.5 h-2.5 rounded-full bg-green-400"></span>
              {{ t('dashboard.success') }}
            </span>
          </div>
        </div>
        <div class="chart-container" style="height: 280px;">
          <canvas ref="lineChartRef"></canvas>
        </div>
      </div>

      <!-- Agent 状态分布环形图 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up" style="animation-delay: 100ms;">
        <div class="mb-5">
          <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('dashboard.agentDistribution') }}</h2>
          <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ t('dashboard.agentDistributionDesc') }}</p>
        </div>
        <div class="chart-container flex items-center justify-center" style="height: 280px;">
          <canvas ref="doughnutChartRef"></canvas>
        </div>
      </div>
    </div>

    <!-- 快捷操作 + 最近活动 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 快捷操作 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up" style="animation-delay: 150ms;">
        <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50 mb-5">{{ t('dashboard.quickActions') }}</h2>
        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="flex flex-col items-center gap-2.5 p-4 rounded-xl bg-neutral-50 dark:bg-neutral-800/60 hover:bg-primary-50 dark:hover:bg-primary-950/30 border border-transparent hover:border-primary-200 dark:hover:border-primary-800/40 transition-all duration-200 group cursor-pointer"
          >
            <div
              class="w-11 h-11 rounded-xl flex items-center justify-center transition-colors duration-200"
              :class="action.iconBg"
            >
              <component :is="action.icon" class="text-lg" :class="action.iconColor" />
            </div>
            <span class="text-xs font-medium text-neutral-600 dark:text-neutral-300 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
              {{ action.label }}
            </span>
          </button>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="lg:col-span-2 bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up" style="animation-delay: 200ms;">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('dashboard.recentActivity') }}</h2>
          <button class="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium transition-colors">
            {{ t('dashboard.viewAll') }}
          </button>
        </div>
        <div class="relative">
          <!-- 时间线竖线 -->
          <div class="absolute left-[7px] top-2 bottom-2 w-px bg-neutral-200 dark:bg-neutral-700"></div>

          <div class="space-y-5">
            <div
              v-for="(activity, index) in activities"
              :key="index"
              class="relative flex gap-4 pl-5"
            >
              <!-- 圆点 -->
              <div
                class="absolute left-0 top-1.5 w-[15px] h-[15px] rounded-full border-2 bg-white dark:bg-neutral-900 z-10"
                :class="activity.dotClass"
              ></div>

              <!-- 内容 -->
              <div class="flex-1 min-w-0">
                <p class="text-sm text-neutral-800 dark:text-neutral-200 font-medium leading-snug">
                  {{ activity.title }}
                </p>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-xs text-neutral-400 dark:text-neutral-500">{{ activity.time }}</span>
                  <span class="text-neutral-300 dark:text-neutral-600">|</span>
                  <span class="text-xs text-neutral-400 dark:text-neutral-500">{{ activity.operator }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  RocketOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ApiOutlined,
  PlusOutlined,
  AuditOutlined,
  FileTextOutlined,
  ExperimentOutlined
} from '@ant-design/icons-vue'
import { useTheme } from '@/composables/useTheme'
import { getAllAgents } from '@/api/agent'
import { getToolStats } from '@/api/tool'
import { getAlertStats } from '@/api/alert'
import { testApi } from '@/api/test'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

const { t, locale } = useI18n()
const { isDark } = useTheme()

// ============ 统计卡片数据 ============
const totalAgents = ref(0)
const passRate = ref(0)
const apiCalls = ref(0)
const activeUsers = ref(0)

const stats = ref([
  {
    label: t('dashboard.totalAgents'),
    value: 0,
    displayValue: '0',
    trend: `${t('dashboard.comparedLastMonth')} +12%`,
    trendDirection: 'up' as const,
    gradient: 'bg-gradient-to-br from-blue-500 to-blue-600',
    icon: RocketOutlined
  },
  {
    label: t('dashboard.passRate'),
    value: 0,
    displayValue: '0%',
    trend: `${t('dashboard.comparedLastMonth')} +3.2%`,
    trendDirection: 'up' as const,
    gradient: 'bg-gradient-to-br from-green-500 to-green-600',
    icon: CheckCircleOutlined
  },
  {
    label: t('dashboard.apiCalls'),
    value: 0,
    displayValue: '0',
    trend: `${t('dashboard.comparedYesterday')} -5%`,
    trendDirection: 'down' as const,
    gradient: 'bg-gradient-to-br from-purple-500 to-purple-600',
    icon: ApiOutlined
  },
  {
    label: t('dashboard.activeUsers'),
    value: 0,
    displayValue: '0',
    trend: `${t('dashboard.comparedLastMonth')} +8%`,
    trendDirection: 'up' as const,
    gradient: 'bg-gradient-to-br from-orange-500 to-orange-600',
    icon: ClockCircleOutlined
  }
])

function updateStatsDisplay() {
  stats.value = [
    {
      label: t('dashboard.totalAgents'),
      value: totalAgents.value,
      displayValue: String(totalAgents.value),
      trend: `${t('dashboard.comparedLastMonth')} +12%`,
      trendDirection: 'up' as const,
      gradient: 'bg-gradient-to-br from-blue-500 to-blue-600',
      icon: RocketOutlined
    },
    {
      label: t('dashboard.passRate'),
      value: passRate.value,
      displayValue: passRate.value.toFixed(1) + '%',
      trend: `${t('dashboard.comparedLastMonth')} +3.2%`,
      trendDirection: 'up' as const,
      gradient: 'bg-gradient-to-br from-green-500 to-green-600',
      icon: CheckCircleOutlined
    },
    {
      label: t('dashboard.apiCalls'),
      value: apiCalls.value,
      displayValue: apiCalls.value.toLocaleString(),
      trend: `${t('dashboard.comparedYesterday')} -5%`,
      trendDirection: 'down' as const,
      gradient: 'bg-gradient-to-br from-purple-500 to-purple-600',
      icon: ApiOutlined
    },
    {
      label: t('dashboard.activeUsers'),
      value: activeUsers.value,
      displayValue: String(activeUsers.value),
      trend: `${t('dashboard.comparedLastMonth')} +8%`,
      trendDirection: 'up' as const,
      gradient: 'bg-gradient-to-br from-orange-500 to-orange-600',
      icon: ClockCircleOutlined
    }
  ]
}

// ============ 快捷操作数据 ============
const quickActions = ref([
  {
    label: t('dashboard.createNewAgent'),
    icon: PlusOutlined,
    iconBg: 'bg-blue-100 dark:bg-blue-900/40',
    iconColor: 'text-blue-600 dark:text-blue-400'
  },
  {
    label: t('dashboard.viewApprovals'),
    icon: AuditOutlined,
    iconBg: 'bg-amber-100 dark:bg-amber-900/40',
    iconColor: 'text-amber-600 dark:text-amber-400'
  },
  {
    label: t('dashboard.checkApiUsage'),
    icon: FileTextOutlined,
    iconBg: 'bg-green-100 dark:bg-green-900/40',
    iconColor: 'text-green-600 dark:text-green-400'
  },
  {
    label: t('dashboard.testManagement'),
    icon: ExperimentOutlined,
    iconBg: 'bg-purple-100 dark:bg-purple-900/40',
    iconColor: 'text-purple-600 dark:text-purple-400'
  }
])

// ============ 最近活动数据 ============
const activities = ref([
  {
    title: t('dashboard.activities.0'),
    time: t('dashboard.activityTimes.0'),
    operator: '张三',
    dotClass: 'border-green-500'
  },
  {
    title: t('dashboard.activities.1'),
    time: t('dashboard.activityTimes.1'),
    operator: '李四',
    dotClass: 'border-blue-500'
  },
  {
    title: t('dashboard.activities.2'),
    time: t('dashboard.activityTimes.2'),
    operator: '王五',
    dotClass: 'border-amber-500'
  },
  {
    title: t('dashboard.activities.3'),
    time: t('dashboard.activityTimes.3'),
    operator: '赵六',
    dotClass: 'border-green-500'
  },
  {
    title: t('dashboard.activities.4'),
    time: t('dashboard.activityTimes.4'),
    operator: '张三',
    dotClass: 'border-purple-500'
  }
])

// ============ Chart.js 图表 ============
const lineChartRef = ref<HTMLCanvasElement | null>(null)
const doughnutChartRef = ref<HTMLCanvasElement | null>(null)
let lineChartInstance: InstanceType<typeof ChartJS> | null = null
let doughnutChartInstance: InstanceType<typeof ChartJS> | null = null

// Agent status distribution for doughnut chart
const agentDistribution = ref({ running: 0, stopped: 0, pending: 0, abnormal: 0 })

const getChartTextColor = () => (isDark.value ? '#a3a3a3' : '#737373')
const getChartGridColor = () => (isDark.value ? 'rgba(163,163,163,0.1)' : 'rgba(0,0,0,0.06)')

const createLineChart = () => {
  if (!lineChartRef.value) return

  if (lineChartInstance) {
    lineChartInstance.destroy()
  }

  const ctx = lineChartRef.value.getContext('2d')
  if (!ctx) return

  // 创建渐变填充
  const gradient = ctx.createLinearGradient(0, 0, 0, 280)
  gradient.addColorStop(0, 'rgba(59, 130, 246, 0.15)')
  gradient.addColorStop(1, 'rgba(59, 130, 246, 0)')

  const gradient2 = ctx.createLinearGradient(0, 0, 0, 280)
  gradient2.addColorStop(0, 'rgba(34, 197, 94, 0.12)')
  gradient2.addColorStop(1, 'rgba(34, 197, 94, 0)')

  lineChartInstance = new ChartJS(ctx, {
    type: 'line',
    data: {
      labels: (t('dashboard.weekDays') as unknown) as string[],
      datasets: [
        {
          label: t('dashboard.callVolume'),
          data: [3200, 4100, 3800, 5200, 4800, 3900, apiCalls.value || 4321],
          borderColor: '#3b82f6',
          backgroundColor: gradient,
          borderWidth: 2.5,
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 6,
          pointHoverBackgroundColor: '#3b82f6',
          pointHoverBorderColor: '#ffffff',
          pointHoverBorderWidth: 2
        },
        {
          label: t('dashboard.success'),
          data: [3000, 3900, 3600, 5000, 4600, 3750, Math.floor((apiCalls.value || 4321) * (passRate.value || 94.8) / 100)],
          borderColor: '#22c55e',
          backgroundColor: gradient2,
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 5,
          pointHoverBackgroundColor: '#22c55e',
          pointHoverBorderColor: '#ffffff',
          pointHoverBorderWidth: 2
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: isDark.value ? '#262626' : '#ffffff',
          titleColor: isDark.value ? '#e5e5e5' : '#171717',
          bodyColor: isDark.value ? '#a3a3a3' : '#737373',
          borderColor: isDark.value ? '#404040' : '#e5e5e5',
          borderWidth: 1,
          cornerRadius: 12,
          padding: 12,
          boxPadding: 4,
          usePointStyle: true,
          titleFont: { size: 13, weight: 600 as const },
          bodyFont: { size: 12 }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: {
            color: getChartTextColor(),
            font: { size: 12 }
          },
          border: { display: false }
        },
        y: {
          grid: {
            color: getChartGridColor(),
          },
          ticks: {
            color: getChartTextColor(),
            font: { size: 12 },
            padding: 8
          },
          border: { display: false },
          beginAtZero: true
        }
      }
    }
  })
}

const createDoughnutChart = () => {
  if (!doughnutChartRef.value) return

  if (doughnutChartInstance) {
    doughnutChartInstance.destroy()
  }

  const ctx = doughnutChartRef.value.getContext('2d')
  if (!ctx) return

  const dist = agentDistribution.value

  doughnutChartInstance = new ChartJS(ctx, {
    type: 'doughnut',
    data: {
      labels: [
        t('dashboard.running'),
        t('dashboard.stopped'),
        t('dashboard.pendingApprovals'),
        t('dashboard.abnormal')
      ],
      datasets: [
        {
          data: [dist.running, dist.stopped, dist.pending, dist.abnormal],
          backgroundColor: ['#3b82f6', '#a3a3a3', '#f59e0b', '#ef4444'],
          borderWidth: 0,
          hoverOffset: 6
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '68%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: getChartTextColor(),
            padding: 16,
            usePointStyle: true,
            pointStyle: 'circle',
            font: { size: 12 }
          }
        },
        tooltip: {
          backgroundColor: isDark.value ? '#262626' : '#ffffff',
          titleColor: isDark.value ? '#e5e5e5' : '#171717',
          bodyColor: isDark.value ? '#a3a3a3' : '#737373',
          borderColor: isDark.value ? '#404040' : '#e5e5e5',
          borderWidth: 1,
          cornerRadius: 12,
          padding: 12,
          boxPadding: 4,
          usePointStyle: true,
          titleFont: { size: 13, weight: 600 as const },
          bodyFont: { size: 12 },
          callbacks: {
            label: function (context) {
              const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0)
              const value = context.parsed as number
              const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0'
              return ` ${context.label}: ${value} (${percentage}%)`
            }
          }
        }
      }
    }
  })
}

const initCharts = () => {
  nextTick(() => {
    createLineChart()
    createDoughnutChart()
  })
}

// ============ 获取仪表盘数据 ============

async function fetchDashboardData() {
  try {
    // Fetch agents
    const agentsRes: any = await getAllAgents()
    const agentsData = agentsRes?.data || agentsRes || []
    const agentsList = Array.isArray(agentsData) ? agentsData : []
    totalAgents.value = agentsList.length

    // Calculate agent status distribution
    let running = 0, stopped = 0, pending = 0, abnormal = 0
    agentsList.forEach((agent: any) => {
      const status = agent.status || agent.isActive
      if (status === 'PUBLISHED' || status === true) running++
      else if (status === 'DRAFT' || status === false) stopped++
      else if (status === 'PENDING_APPROVAL') pending++
      else abnormal++
    })
    agentDistribution.value = { running, stopped, pending, abnormal }

    // Fetch test results for pass rate
    const testRes: any = await testApi.getTestResults({ page: 1, size: 1000 })
    const testData = testRes?.data || testRes || []
    const testList = Array.isArray(testData) ? testData : []
    if (testList.length > 0) {
      const passed = testList.filter((r: any) => r.status === 'passed').length
      passRate.value = (passed / testList.length) * 100
    } else {
      passRate.value = 0
    }

    // Fetch tool stats for API calls
    try {
      const toolRes: any = await getToolStats()
      const toolData = toolRes?.data || toolRes || {}
      apiCalls.value = toolData.totalCalls || toolData.apiCalls || 0
    } catch {
      apiCalls.value = 0
    }

    // Fetch alert stats for active users
    try {
      const alertRes: any = await getAlertStats()
      const alertData = alertRes?.data || alertRes || {}
      activeUsers.value = alertData.activeUsers || alertData.totalAlerts || 0
    } catch {
      activeUsers.value = 0
    }

    // Update display
    updateStatsDisplay()
    initCharts()
  } catch (error: any) {
    message.error('获取仪表盘数据失败: ' + (error.message || '未知错误'))
    // Fallback to zero values
    updateStatsDisplay()
    initCharts()
  }
}

// 监听暗色模式变化，重建图表以适配主题
watch(isDark, () => {
  initCharts()
})

// 监听语言变化，重建图表以适配翻译
watch(locale, () => {
  // 更新响应式数据
  updateStatsDisplay()

  quickActions.value = [
    {
      label: t('dashboard.createNewAgent'),
      icon: PlusOutlined,
      iconBg: 'bg-blue-100 dark:bg-blue-900/40',
      iconColor: 'text-blue-600 dark:text-blue-400'
    },
    {
      label: t('dashboard.viewApprovals'),
      icon: AuditOutlined,
      iconBg: 'bg-amber-100 dark:bg-amber-900/40',
      iconColor: 'text-amber-600 dark:text-amber-400'
    },
    {
      label: t('dashboard.checkApiUsage'),
      icon: FileTextOutlined,
      iconBg: 'bg-green-100 dark:bg-green-900/40',
      iconColor: 'text-green-600 dark:text-green-400'
    },
    {
      label: t('dashboard.testManagement'),
      icon: ExperimentOutlined,
      iconBg: 'bg-purple-100 dark:bg-purple-900/40',
      iconColor: 'text-purple-600 dark:text-purple-400'
    }
  ]

  activities.value = [
    {
      title: t('dashboard.activities.0'),
      time: t('dashboard.activityTimes.0'),
      operator: '张三',
      dotClass: 'border-green-500'
    },
    {
      title: t('dashboard.activities.1'),
      time: t('dashboard.activityTimes.1'),
      operator: '李四',
      dotClass: 'border-blue-500'
    },
    {
      title: t('dashboard.activities.2'),
      time: t('dashboard.activityTimes.2'),
      operator: '王五',
      dotClass: 'border-amber-500'
    },
    {
      title: t('dashboard.activities.3'),
      time: t('dashboard.activityTimes.3'),
      operator: '赵六',
      dotClass: 'border-green-500'
    },
    {
      title: t('dashboard.activities.4'),
      time: t('dashboard.activityTimes.4'),
      operator: '张三',
      dotClass: 'border-purple-500'
    }
  ]

  initCharts()
})

onMounted(() => {
  fetchDashboardData()
})

onUnmounted(() => {
  if (lineChartInstance) lineChartInstance.destroy()
  if (doughnutChartInstance) doughnutChartInstance.destroy()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.stat-card {
  animation: slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.chart-container {
  position: relative;
  width: 100%;
}

/* 暗色模式下图表容器确保 canvas 正确渲染 */
:deep(canvas) {
  max-width: 100%;
}
</style>
