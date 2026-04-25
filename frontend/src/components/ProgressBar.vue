<template>
  <div
    v-if="isVisible"
    class="progress-bar-container"
    :class="{ 'progress-bar-done': isDone }"
    role="progressbar"
    :aria-valuenow="progress"
    aria-valuemin="0"
    aria-valuemax="100"
  >
    <div
      class="progress-bar"
      :style="{ width: `${progress}%`, backgroundColor: color }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import type { Ref } from 'vue'

interface Props {
  /** 进度条颜色 */
  color?: string
  /** 加载状态 */
  loading?: Ref<boolean> | boolean
  /** 自动完成超时时间（毫秒） */
  timeout?: number
}

const props = withDefaults(defineProps<Props>(), {
  color: '#1890ff',
  timeout: 800,
})

const progress = ref(0)
const isVisible = ref(false)
const isDone = ref(false)

let startTimer: ReturnType<typeof setTimeout> | null = null
let trickleTimer: ReturnType<typeof setInterval> | null = null
let completeTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 开始进度条动画
 */
function start(): void {
  reset()
  isVisible.value = true
  isDone.value = false
  progress.value = 0

  // 快速到 30%
  startTimer = setTimeout(() => {
    progress.value = 30
  }, 50)

  // 缓慢递增
  trickleTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 10
      if (progress.value > 90) {
        progress.value = 90
      }
    }
  }, 200)
}

/**
 * 完成进度条
 */
function complete(): void {
  if (startTimer) {
    clearTimeout(startTimer)
    startTimer = null
  }
  if (trickleTimer) {
    clearInterval(trickleTimer)
    trickleTimer = null
  }

  progress.value = 100
  isDone.value = true

  completeTimer = setTimeout(() => {
    isVisible.value = false
    isDone.value = false
    progress.value = 0
  }, 300)
}

/**
 * 重置进度条
 */
function reset(): void {
  if (startTimer) {
    clearTimeout(startTimer)
    startTimer = null
  }
  if (trickleTimer) {
    clearInterval(trickleTimer)
    trickleTimer = null
  }
  if (completeTimer) {
    clearTimeout(completeTimer)
    completeTimer = null
  }
  progress.value = 0
  isVisible.value = false
  isDone.value = false
}

// 监听 loading prop 变化
watch(
  () => (typeof props.loading === 'object' ? props.loading.value : props.loading),
  (newVal) => {
    if (newVal) {
      start()
    } else {
      complete()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  reset()
})

// 暴露方法供外部使用
defineExpose({
  start,
  complete,
  reset,
})
</script>

<style scoped>
.progress-bar-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 9999;
  pointer-events: none;
}

.progress-bar {
  height: 100%;
  transition: width 0.3s ease;
  box-shadow: 0 0 10px rgba(24, 144, 255, 0.3);
}

.progress-bar-done .progress-bar {
  transition: width 0.2s ease, opacity 0.3s ease 0.1s;
  opacity: 0;
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .progress-bar {
    box-shadow: 0 0 10px rgba(24, 144, 255, 0.2);
  }
}
</style>
