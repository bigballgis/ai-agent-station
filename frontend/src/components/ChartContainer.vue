<template>
  <div class="chart-container" :style="{ height: height ? `${height}px` : '300px' }">
    <!-- 加载状态 -->
    <div v-if="loading" class="chart-loading">
      <a-spin size="large" />
      <span class="chart-loading-text">{{ t('component.loading') }}</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="isEmpty" class="chart-empty">
      <EmptyState type="noData" :description="t('component.noChartData')" />
    </div>

    <!-- 图表画布 -->
    <canvas
      v-show="!loading && !isEmpty"
      ref="canvasRef"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTheme } from '@/composables/useTheme'
import EmptyState from './EmptyState.vue'

const { t } = useI18n()
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  RadialLinearScale,
  Title,
  Tooltip,
  Legend,
  Filler,
  type ChartData,
  type ChartOptions,
} from 'chart.js'

// 注册 Chart.js 组件
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  RadialLinearScale,
  Title,
  Tooltip,
  Legend,
  Filler,
)

/**
 * ChartContainer 组件
 * Chart.js 图表封装组件
 * 支持折线图、柱状图、环形图、饼图、雷达图
 * 自动适配暗色模式，支持窗口缩放自适应
 */

interface Props {
  /** 图表类型 */
  type: 'line' | 'bar' | 'doughnut' | 'pie' | 'radar'
  /** 图表数据 */
  data: ChartData
  /** 图表配置 */
  options?: ChartOptions
  /** 图表高度（px） */
  height?: number
  /** 加载状态 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  loading: false,
})

const { isDark } = useTheme()
const canvasRef = ref<HTMLCanvasElement | null>(null)
let chartInstance: InstanceType<typeof ChartJS> | null = null

// 判断数据是否为空
const isEmpty = computed(() => {
  if (!props.data || !props.data.datasets) return true
  return props.data.datasets.every(dataset => {
    const data = dataset.data as number[]
    return !data || data.length === 0 || data.every(v => v === 0)
  })
})

// 获取图表文字颜色
const getTextColor = () => isDark.value ? '#a3a3a3' : '#737373'
const getGridColor = () => isDark.value ? 'rgba(163,163,163,0.1)' : 'rgba(0,0,0,0.06)'

// 默认配置
const getDefaultOptions = (): ChartOptions => {
  const isCartesian = props.type === 'line' || props.type === 'bar'
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false,
    },
    plugins: {
      legend: {
        display: props.type !== 'line',
        position: 'bottom',
        labels: {
          color: getTextColor(),
          padding: 16,
          usePointStyle: true,
          pointStyle: 'circle',
          font: { size: 12 },
        },
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
      },
    },
    ...(isCartesian ? {
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: getTextColor(), font: { size: 12 } },
          border: { display: false },
        },
        y: {
          grid: { color: getGridColor() },
          ticks: { color: getTextColor(), font: { size: 12 }, padding: 8 },
          border: { display: false },
          beginAtZero: true,
        },
      },
    } : {}),
    ...(props.type === 'doughnut' ? { cutout: '68%' } : {}),
  }
}

// 创建/更新图表
function createChart() {
  if (!canvasRef.value || isEmpty.value) return

  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = canvasRef.value.getContext('2d')
  if (!ctx) return

  const mergedOptions = {
    ...getDefaultOptions(),
    ...props.options,
  }

  chartInstance = new ChartJS(ctx, {
    type: props.type,
    data: JSON.parse(JSON.stringify(props.data)),
    options: mergedOptions,
  })
}

// 窗口缩放处理
function handleResize() {
  chartInstance?.resize()
}

// 监听数据变化
watch(
  () => [props.data, props.type, isDark.value],
  () => {
    nextTick(createChart)
  },
  { deep: true },
)

onMounted(() => {
  nextTick(createChart)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chartInstance?.destroy()
  chartInstance = null
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chart-container {
  position: relative;
  width: 100%;
}

.chart-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.chart-loading-text {
  font-size: 14px;
  color: #a3a3a3;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 确保 canvas 正确渲染 */
:deep(canvas) {
  max-width: 100%;
}
</style>
