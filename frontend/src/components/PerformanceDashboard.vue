<template>
  <div class="performance-dashboard">
    <div class="perf-header">
      <h3>Performance Dashboard</h3>
      <div class="perf-actions">
        <a-button size="small" @click="refreshData">Refresh</a-button>
        <a-button size="small" danger @click="clearData">Clear</a-button>
      </div>
    </div>

    <!-- Web Vitals 摘要 -->
    <a-card size="small" title="Core Web Vitals" class="perf-card">
      <div class="vitals-grid">
        <div
          v-for="vital in vitalItems"
          :key="vital.name"
          class="vital-item"
          :class="`vital-${vital.rating}`"
        >
          <div class="vital-name">{{ vital.name }}</div>
          <div class="vital-value">{{ vital.displayValue }}</div>
          <div class="vital-rating">{{ vital.rating }}</div>
        </div>
      </div>
    </a-card>

    <!-- 性能测量列表 -->
    <a-card size="small" title="Performance Marks" class="perf-card">
      <div v-if="measurements.length === 0" class="empty-state">
        No measurements recorded yet
      </div>
      <div v-else class="measurements-list">
        <div
          v-for="(m, index) in measurements"
          :key="index"
          class="measurement-item"
        >
          <div class="measurement-header">
            <span class="measurement-name">{{ m.name }}</span>
            <a-tag :color="getCategoryColor(m.category)" size="small">
              {{ m.category }}
            </a-tag>
          </div>
          <div class="measurement-duration">
            {{ m.duration.toFixed(2) }}ms
          </div>
        </div>
      </div>
    </a-card>

    <!-- 按分类统计 -->
    <a-card size="small" title="Category Summary" class="perf-card">
      <div class="category-stats">
        <div
          v-for="stat in categoryStats"
          :key="stat.category"
          class="stat-row"
        >
          <span class="stat-category">{{ stat.category }}</span>
          <span class="stat-count">{{ stat.count }} measurements</span>
          <span class="stat-avg">{{ stat.avgDuration.toFixed(2) }}ms avg</span>
          <span class="stat-max">{{ stat.maxDuration.toFixed(2) }}ms max</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Button as AButton, Card as ACard, Tag as ATag } from 'ant-design-vue'
import { useWebVitals } from '@/composables/useWebVitals'
import {
  getGlobalMeasurements,
  type PerformanceMeasurement,
  type PerformanceCategory,
} from '@/composables/usePerformanceMark'

const { flush, getSummary } = useWebVitals({
  reportToBackend: false,
})

const measurements = ref<PerformanceMeasurement[]>([])

/** 刷新数据 */
function refreshData() {
  measurements.value = getGlobalMeasurements()
  flush()
}

/** 清除数据 */
function clearData() {
  measurements.value = []
}

/** Web Vitals 展示项 */
const vitalItems = computed(() => {
  const vitals = getSummary()
  return Object.entries(vitals).map(([name, metric]) => {
    if (!metric) {
      return {
        name,
        displayValue: '--',
        rating: 'needs-improvement' as const,
      }
    }
    let displayValue: string
    if (name === 'CLS') {
      displayValue = metric.value.toFixed(3)
    } else {
      displayValue = `${metric.value}ms`
    }
    return {
      name,
      displayValue,
      rating: metric.rating,
    }
  })
})

/** 按分类统计 */
const categoryStats = computed(() => {
  const stats = new Map<
    PerformanceCategory,
    { count: number; totalDuration: number; maxDuration: number }
  >()

  for (const m of measurements.value) {
    const existing = stats.get(m.category) || {
      count: 0,
      totalDuration: 0,
      maxDuration: 0,
    }
    existing.count++
    existing.totalDuration += m.duration
    existing.maxDuration = Math.max(existing.maxDuration, m.duration)
    stats.set(m.category, existing)
  }

  return Array.from(stats.entries()).map(([category, data]) => ({
    category,
    count: data.count,
    avgDuration: data.totalDuration / data.count,
    maxDuration: data.maxDuration,
  }))
})

/** 获取分类标签颜色 */
function getCategoryColor(category: PerformanceCategory): string {
  const colors: Record<PerformanceCategory, string> = {
    navigation: 'blue',
    api: 'green',
    render: 'orange',
    custom: 'default',
  }
  return colors[category] || 'default'
}

// 初始加载
refreshData()
</script>

<style scoped>
.performance-dashboard {
  padding: 16px;
  font-family: monospace;
  font-size: 12px;
  max-height: 80vh;
  overflow-y: auto;
}

.perf-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.perf-header h3 {
  margin: 0;
  font-size: 14px;
  color: #fff;
}

.perf-actions {
  display: flex;
  gap: 4px;
}

.perf-card {
  margin-bottom: 12px;
}

.vitals-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 8px;
}

.vital-item {
  padding: 8px;
  border-radius: 4px;
  text-align: center;
}

.vital-good {
  background-color: rgba(82, 196, 26, 0.15);
  border: 1px solid rgba(82, 196, 26, 0.3);
}

.vital-needs-improvement {
  background-color: rgba(250, 173, 20, 0.15);
  border: 1px solid rgba(250, 173, 20, 0.3);
}

.vital-poor {
  background-color: rgba(245, 34, 45, 0.15);
  border: 1px solid rgba(245, 34, 45, 0.3);
}

.vital-name {
  font-weight: bold;
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
}

.vital-value {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.vital-rating {
  font-size: 10px;
  color: #666;
  margin-top: 2px;
}

.empty-state {
  color: #666;
  text-align: center;
  padding: 16px;
}

.measurements-list {
  max-height: 300px;
  overflow-y: auto;
}

.measurement-item {
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.measurement-item:last-child {
  border-bottom: none;
}

.measurement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.measurement-name {
  color: #ddd;
  font-size: 11px;
  word-break: break-all;
}

.measurement-duration {
  color: #52c41a;
  font-weight: bold;
  font-size: 13px;
  margin-top: 2px;
}

.category-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.stat-category {
  color: #ddd;
  font-weight: bold;
  min-width: 80px;
}

.stat-count {
  color: #999;
}

.stat-avg {
  color: #52c41a;
}

.stat-max {
  color: #fa8c16;
}
</style>
