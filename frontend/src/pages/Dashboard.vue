<template>
  <div class="dashboard-page" aria-label="仪表盘">
    <!-- 页面标题 -->
    <PageHeader :title="t('dashboard.title') || 'Dashboard'" :subtitle="t('dashboard.subtitle')" />

    <!-- 加载状态 -->
    <div v-if="dashboardLoading" class="flex items-center justify-center py-20">
      <a-spin size="large" />
    </div>

    <template v-else>

    <!-- 统计卡片区域 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
      <StatCard
        v-for="(stat, index) in statCards"
        :key="stat.title"
        :title="stat.title"
        :value="stat.value"
        :icon="stat.icon"
        :trend="stat.trend"
        :trend-value="stat.trendValue"
        :color="stat.color"
        :decimals="stat.decimals"
        :suffix="stat.suffix"
        class="animate-slide-up"
        :style="{ animationDelay: `${index * 80}ms` }"
      />
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
        <ChartContainer type="line" :data="lineChartData" :options="lineChartOptions" :height="280" />
      </div>

      <!-- Agent 状态分布环形图 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up" style="animation-delay: 100ms;">
        <div class="mb-5">
          <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('dashboard.agentDistribution') }}</h2>
          <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ t('dashboard.agentDistributionDesc') }}</p>
        </div>
        <ChartContainer type="doughnut" :data="doughnutChartData" :options="doughnutChartOptions" :height="280" />
      </div>
    </div>

    <!-- 快捷操作 + 最近活动 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 快捷操作 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card p-6 animate-slide-up" style="animation-delay: 150ms;" aria-label="快捷操作">
        <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50 mb-5">{{ t('dashboard.quickActions') }}</h2>
        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="flex flex-col items-center gap-2.5 p-4 rounded-xl bg-neutral-50 dark:bg-neutral-800/60 hover:bg-primary-50 dark:hover:bg-primary-950/30 border border-transparent hover:border-primary-200 dark:hover:border-primary-800/40 transition-all duration-200 group cursor-pointer"
            @click="router.push(action.route)"
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
          <button
            class="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium transition-colors"
            @click="router.push('/system/log')"
          >
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

          <!-- 空状态 -->
          <div v-if="activities.length === 0" class="text-center py-8">
            <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('dashboard.noActivity') }}</p>
          </div>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
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
import { getAlertStats, getAlertRecords } from '@/api/alert'
import { testApi } from '@/api/test'
import { getLogs } from '@/api/log'
import { PageHeader, StatCard, ChartContainer } from '@/components'

const { t, locale } = useI18n()
const { isDark } = useTheme()
const router = useRouter()

// ============ 统计卡片数据 ============
const totalAgents = ref(0)
const passRate = ref(0)
const apiCalls = ref(0)
const activeAlerts = ref(0)
const dashboardLoading = ref(false)

// StatCard 配置（响应式）
const statCards = computed(() => [
  {
    title: t('dashboard.totalAgents'),
    value: totalAgents.value,
    icon: RocketOutlined,
    trend: 'up' as const,
    trendValue: `${t('dashboard.comparedLastMonth')} N/A`,
    color: 'blue' as const,
    decimals: 0,
    suffix: '',
  },
  {
    title: t('dashboard.passRate'),
    value: passRate.value,
    icon: CheckCircleOutlined,
    trend: 'up' as const,
    trendValue: `${t('dashboard.comparedLastMonth')} N/A`,
    color: 'green' as const,
    decimals: 1,
    suffix: '%',
  },
  {
    title: t('dashboard.apiCalls'),
    value: apiCalls.value,
    icon: ApiOutlined,
    trend: 'down' as const,
    trendValue: `${t('dashboard.comparedYesterday')} N/A`,
    color: 'purple' as const,
    decimals: 0,
    suffix: '',
  },
  {
    title: t('dashboard.activeUsers'),
    value: activeAlerts.value,
    icon: ClockCircleOutlined,
    trend: 'up' as const,
    trendValue: `${t('dashboard.comparedLastMonth')} N/A`,
    color: 'orange' as const,
    decimals: 0,
    suffix: '',
  },
])

// ============ 快捷操作数据 ============
const quickActions = ref([
  {
    label: t('dashboard.createNewAgent'),
    icon: PlusOutlined,
    iconBg: 'bg-blue-100 dark:bg-blue-900/40',
    iconColor: 'text-blue-600 dark:text-blue-400',
    route: '/agents'
  },
  {
    label: t('dashboard.viewApprovals'),
    icon: AuditOutlined,
    iconBg: 'bg-amber-100 dark:bg-amber-900/40',
    iconColor: 'text-amber-600 dark:text-amber-400',
    route: '/approvals'
  },
  {
    label: t('dashboard.checkApiUsage'),
    icon: FileTextOutlined,
    iconBg: 'bg-green-100 dark:bg-green-900/40',
    iconColor: 'text-green-600 dark:text-green-400',
    route: '/logs'
  },
  {
    label: t('dashboard.testManagement'),
    icon: ExperimentOutlined,
    iconBg: 'bg-purple-100 dark:bg-purple-900/40',
    iconColor: 'text-purple-600 dark:text-purple-400',
    route: '/tests'
  }
])

// ============ 最近活动数据 ============
const activities = ref<Array<{
  title: string
  time: string
  operator: string
  dotClass: string
}>>([])

// ============ Chart.js 图表数据（供 ChartContainer 使用） ============
const agentDistribution = ref({ running: 0, stopped: 0, pending: 0, abnormal: 0 })

const lineChartData = computed(() => ({
  labels: (t('dashboard.weekDays') as unknown) as string[],
  datasets: [
    {
      label: t('dashboard.callVolume'),
      data: [3200, 4100, 3800, 5200, 4800, 3900, apiCalls.value || 4321],
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59, 130, 246, 0.15)',
      borderWidth: 2.5,
      fill: true,
      tension: 0.4,
      pointRadius: 0,
      pointHoverRadius: 6,
      pointHoverBackgroundColor: '#3b82f6',
      pointHoverBorderColor: '#ffffff',
      pointHoverBorderWidth: 2,
    },
    {
      label: t('dashboard.success'),
      data: [3000, 3900, 3600, 5000, 4600, 3750, Math.floor((apiCalls.value || 4321) * (passRate.value || 94.8) / 100)],
      borderColor: '#22c55e',
      backgroundColor: 'rgba(34, 197, 94, 0.12)',
      borderWidth: 2,
      fill: true,
      tension: 0.4,
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBackgroundColor: '#22c55e',
      pointHoverBorderColor: '#ffffff',
      pointHoverBorderWidth: 2,
    },
  ],
}))

const lineChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: { display: false },
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: { color: isDark.value ? '#a3a3a3' : '#737373', font: { size: 12 } },
      border: { display: false },
    },
    y: {
      grid: { color: isDark.value ? 'rgba(163,163,163,0.1)' : 'rgba(0,0,0,0.06)' },
      ticks: { color: isDark.value ? '#a3a3a3' : '#737373', font: { size: 12 }, padding: 8 },
      border: { display: false },
      beginAtZero: true,
    },
  },
}))

const doughnutChartData = computed(() => ({
  labels: [
    t('dashboard.running'),
    t('dashboard.stopped'),
    t('dashboard.pendingApprovals'),
    t('dashboard.abnormal'),
  ],
  datasets: [
    {
      data: [agentDistribution.value.running, agentDistribution.value.stopped, agentDistribution.value.pending, agentDistribution.value.abnormal],
      backgroundColor: ['#3b82f6', '#a3a3a3', '#f59e0b', '#ef4444'],
      borderWidth: 0,
      hoverOffset: 6,
    },
  ],
}))

const doughnutChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '68%',
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: {
        color: isDark.value ? '#a3a3a3' : '#737373',
        padding: 16,
        usePointStyle: true,
        pointStyle: 'circle',
        font: { size: 12 },
      },
    },
    tooltip: {
      callbacks: {
        label: function (context: any) {
          const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0)
          const value = context.parsed as number
          const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0'
          return ` ${context.label}: ${value} (${percentage}%)`
        },
      },
    },
  },
}))

// ============ 获取仪表盘数据 ============

const moduleDotColors: Record<string, string> = {
  agent: 'border-blue-500',
  approval: 'border-amber-500',
  test: 'border-green-500',
  api: 'border-purple-500',
  system: 'border-red-500',
  default: 'border-neutral-400',
}

function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return t('dashboard.activityTimes.0').replace(/\d+/, '1')
  if (minutes < 60) return `${minutes} ${locale.value === 'zh-CN' ? '分钟前' : 'min ago'}`
  if (hours < 24) return `${hours} ${locale.value === 'zh-CN' ? '小时前' : 'hours ago'}`
  return `${days} ${locale.value === 'zh-CN' ? '天前' : 'days ago'}`
}

async function loadRecentActivities() {
  try {
    const res: any = await getLogs({ page: 1, size: 5 })
    const logsData = res?.data || res || []
    const logsList = Array.isArray(logsData) ? logsData : []
    if (logsList.length > 0) {
      activities.value = logsList.map((log: any) => ({
        title: log.action || log.description || log.module || t('common.noData'),
        time: formatRelativeTime(log.createdAt || log.createTime),
        operator: log.operator || log.username || log.createdBy || (locale.value === 'zh-CN' ? '系统' : 'System'),
        dotClass: moduleDotColors[log.module?.toLowerCase()] || moduleDotColors.default,
      }))
    } else {
      activities.value = []
    }
  } catch {
    activities.value = []
  }
}

async function fetchDashboardData() {
  dashboardLoading.value = true
  try {
    const [agentsRes, testRes, alertRecordsRes] = await Promise.all([
      getAllAgents().catch(() => null),
      testApi.getTestResults({ page: 1, size: 1000 }).catch(() => null),
      getAlertRecords({ page: 1, size: 1 }).catch(() => null),
    ])

    // Agent count and distribution
    if (agentsRes) {
      const agentsData = agentsRes?.data || agentsRes || []
      const agentsList = Array.isArray(agentsData) ? agentsData : []
      totalAgents.value = agentsList.length

      let running = 0, stopped = 0, pending = 0, abnormal = 0
      agentsList.forEach((agent: any) => {
        const status = agent.status || agent.isActive
        if (status === 'PUBLISHED' || status === true) running++
        else if (status === 'DRAFT' || status === false) stopped++
        else if (status === 'PENDING_APPROVAL') pending++
        else abnormal++
      })
      agentDistribution.value = { running, stopped, pending, abnormal }
    }

    // Test pass rate
    if (testRes) {
      const testData = testRes?.data || testRes || []
      const testList = Array.isArray(testData) ? testData : []
      if (testList.length > 0) {
        const passed = testList.filter((r: any) => r.status === 'passed').length
        passRate.value = (passed / testList.length) * 100
      } else {
        passRate.value = 0
      }
    }

    // Active alerts count
    if (alertRecordsRes) {
      const alertData = alertRecordsRes?.data || alertRecordsRes || {}
      const alertList = Array.isArray(alertData) ? alertData : []
      activeAlerts.value = alertList.length
    }

    // Fetch tool stats for API calls (non-critical)
    try {
      const toolRes: any = await getToolStats()
      const toolData = toolRes?.data || toolRes || {}
      apiCalls.value = toolData.totalCalls || toolData.apiCalls || 0
    } catch {
      apiCalls.value = 0
    }

    // Load recent activities
    await loadRecentActivities()
  } catch (error: any) {
    message.error(t('dashboard.loadFailed') + ': ' + (error.message || ''))
  } finally {
    dashboardLoading.value = false
  }
}

// 监听语言变化，更新快捷操作
watch(locale, () => {
  // 更新响应式数据 - statCards 和 chart data are reactive via computed
  quickActions.value = [
    {
      label: t('dashboard.createNewAgent'),
      icon: PlusOutlined,
      iconBg: 'bg-blue-100 dark:bg-blue-900/40',
      iconColor: 'text-blue-600 dark:text-blue-400',
      route: '/agents'
    },
    {
      label: t('dashboard.viewApprovals'),
      icon: AuditOutlined,
      iconBg: 'bg-amber-100 dark:bg-amber-900/40',
      iconColor: 'text-amber-600 dark:text-amber-400',
      route: '/approvals'
    },
    {
      label: t('dashboard.checkApiUsage'),
      icon: FileTextOutlined,
      iconBg: 'bg-green-100 dark:bg-green-900/40',
      iconColor: 'text-green-600 dark:text-green-400',
      route: '/logs'
    },
    {
      label: t('dashboard.testManagement'),
      icon: ExperimentOutlined,
      iconBg: 'bg-purple-100 dark:bg-purple-900/40',
      iconColor: 'text-purple-600 dark:text-purple-400',
      route: '/tests'
    }
  ]

  // Re-format activity times for new locale
  loadRecentActivities()
})

onMounted(() => {
  fetchDashboardData()
})

onUnmounted(() => {
  // ChartContainer handles its own cleanup
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}
</style>
