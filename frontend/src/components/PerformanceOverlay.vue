<template>
  <Teleport to="body">
    <div
      v-if="isVisible"
      class="perf-overlay"
      :class="{ 'perf-overlay-collapsed': isCollapsed }"
    >
      <!-- 切换按钮 -->
      <button
        class="perf-toggle"
        :title="isCollapsed ? 'Show Performance Overlay' : 'Hide Performance Overlay'"
        @click="isCollapsed = !isCollapsed"
      >
        <span v-if="isCollapsed">PERF</span>
        <span v-else>&times;</span>
      </button>

      <!-- 展开的面板 -->
      <div v-if="!isCollapsed" class="perf-panel">
        <div class="perf-panel-header">
          <span class="perf-title">Performance Monitor</span>
          <div class="perf-header-actions">
            <button class="perf-btn" @click="refresh" title="Refresh">R</button>
            <button class="perf-btn" @click="clear" title="Clear">C</button>
          </div>
        </div>

        <!-- Web Vitals 实时指标 -->
        <div class="perf-section">
          <div class="perf-section-title">Web Vitals</div>
          <div class="perf-vitals">
            <div
              v-for="vital in vitalEntries"
              :key="vital.name"
              class="perf-vital"
              :class="`perf-vital-${vital.rating}`"
            >
              <span class="perf-vital-name">{{ vital.name }}</span>
              <span class="perf-vital-value">{{ vital.displayValue }}</span>
            </div>
          </div>
        </div>

        <!-- 最近性能测量 -->
        <div class="perf-section">
          <div class="perf-section-title">Recent Marks (last 10)</div>
          <div class="perf-marks">
            <div
              v-for="(mark, idx) in recentMarks"
              :key="idx"
              class="perf-mark"
            >
              <span class="perf-mark-name" :title="mark.name">{{ truncate(mark.name, 30) }}</span>
              <span class="perf-mark-duration">{{ mark.duration.toFixed(1) }}ms</span>
              <span class="perf-mark-category">{{ mark.category }}</span>
            </div>
            <div v-if="recentMarks.length === 0" class="perf-empty">
              No marks recorded
            </div>
          </div>
        </div>

        <!-- FPS 计数器 -->
        <div class="perf-section">
          <div class="perf-section-title">FPS</div>
          <div class="perf-fps" :class="fpsClass">{{ fps }}</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebVitals } from '@/composables/useWebVitals'
import { getGlobalMeasurements, type PerformanceMeasurement } from '@/composables/usePerformanceMark'

// 仅在开发模式下显示
const isVisible = ref(import.meta.env.DEV)
const isCollapsed = ref(true)

const { getSummary } = useWebVitals({ reportToBackend: false })
const recentMarks = ref<PerformanceMeasurement[]>([])

// FPS 追踪
const fps = ref(0)
let frameCount = 0
let lastFpsTime = performance.now()
let fpsRafId: number | null = null

function updateFps() {
  frameCount++
  const now = performance.now()
  if (now - lastFpsTime >= 1000) {
    fps.value = frameCount
    frameCount = 0
    lastFpsTime = now
  }
  fpsRafId = requestAnimationFrame(updateFps)
}

const fpsClass = computed(() => {
  if (fps.value >= 50) return 'fps-good'
  if (fps.value >= 30) return 'fps-warning'
  return 'fps-poor'
})

/** Web Vitals 展示数据 */
const vitalEntries = computed(() => {
  const summary = getSummary()
  return Object.entries(summary).map(([name, metric]) => {
    if (!metric) {
      return { name, displayValue: '--', rating: 'needs-improvement' as const }
    }
    return {
      name,
      displayValue: name === 'CLS' ? metric.value.toFixed(3) : `${metric.value}ms`,
      rating: metric.rating,
    }
  })
})

/** 截断字符串 */
function truncate(str: string, maxLen: number): string {
  return str.length > maxLen ? str.slice(0, maxLen) + '...' : str
}

/** 刷新数据 */
function refresh() {
  recentMarks.value = getGlobalMeasurements().slice(-10).reverse()
}

/** 清除数据 */
function clear() {
  recentMarks.value = []
}

onMounted(() => {
  refresh()
  fpsRafId = requestAnimationFrame(updateFps)
})

onUnmounted(() => {
  if (fpsRafId !== null) {
    cancelAnimationFrame(fpsRafId)
  }
})
</script>

<style scoped>
.perf-overlay {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 99999;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 11px;
  color: #e0e0e0;
  pointer-events: auto;
}

.perf-overlay-collapsed {
  bottom: 16px;
  right: 16px;
}

.perf-toggle {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 100000;
  background: rgba(0, 0, 0, 0.85);
  color: #52c41a;
  border: 1px solid #52c41a;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  font-family: inherit;
  font-size: 11px;
  font-weight: bold;
  letter-spacing: 1px;
}

.perf-toggle:hover {
  background: rgba(82, 196, 26, 0.2);
}

.perf-panel {
  background: rgba(0, 0, 0, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  width: 320px;
  max-height: 400px;
  overflow-y: auto;
  backdrop-filter: blur(10px);
}

.perf-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  position: sticky;
  top: 0;
  background: rgba(0, 0, 0, 0.95);
  z-index: 1;
}

.perf-title {
  font-weight: bold;
  color: #52c41a;
  font-size: 12px;
}

.perf-header-actions {
  display: flex;
  gap: 4px;
}

.perf-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ccc;
  border-radius: 3px;
  padding: 2px 6px;
  cursor: pointer;
  font-family: inherit;
  font-size: 10px;
}

.perf-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.perf-section {
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.perf-section:last-child {
  border-bottom: none;
}

.perf-section-title {
  color: #888;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.perf-vitals {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.perf-vital {
  display: flex;
  justify-content: space-between;
  padding: 3px 6px;
  border-radius: 3px;
}

.perf-vital-good {
  background: rgba(82, 196, 26, 0.1);
}

.perf-vital-needs-improvement {
  background: rgba(250, 173, 20, 0.1);
}

.perf-vital-poor {
  background: rgba(245, 34, 45, 0.1);
}

.perf-vital-name {
  color: #999;
}

.perf-vital-value {
  font-weight: bold;
}

.perf-marks {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.perf-mark {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 0;
}

.perf-mark-name {
  color: #bbb;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.perf-mark-duration {
  color: #52c41a;
  font-weight: bold;
  margin-left: 8px;
  min-width: 60px;
  text-align: right;
}

.perf-mark-category {
  color: #666;
  margin-left: 8px;
  min-width: 50px;
  text-align: right;
  font-size: 10px;
}

.perf-empty {
  color: #555;
  text-align: center;
  padding: 8px;
}

.perf-fps {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  padding: 4px;
}

.fps-good {
  color: #52c41a;
}

.fps-warning {
  color: #faad14;
}

.fps-poor {
  color: #f5222d;
}
</style>
