<template>
  <div class="count-up" :class="{ 'count-up-active': hasStarted }">
    {{ displayValue }}
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

/**
 * CountUp 组件
 * 数字滚动动画组件
 * 用于 Dashboard 和 HomePage 统计卡片中数值的动态展示
 */

interface Props {
  /** 目标数值 */
  endValue: number
  /** 动画持续时间（毫秒） */
  duration?: number
  /** 前缀 */
  prefix?: string
  /** 后缀 */
  suffix?: string
  /** 小数位数 */
  decimals?: number
  /** 千分位分隔符 */
  separator?: string
}

const props = withDefaults(defineProps<Props>(), {
  duration: 1500,
  decimals: 0,
  separator: ',',
})

const displayValue = ref('')
const hasStarted = ref(false)
let animationFrame: number | null = null

/**
 * 执行数字滚动动画
 * 使用 requestAnimationFrame 实现平滑过渡
 */
function animateCount(start: number, end: number, duration: number) {
  const startTime = performance.now()

  function update(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)

    // 使用 easeOutExpo 缓动函数
    const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
    const currentValue = start + (end - start) * eased

    // 格式化数值
    displayValue.value = formatNumber(currentValue)

    if (progress < 1) {
      animationFrame = requestAnimationFrame(update)
    }
  }

  animationFrame = requestAnimationFrame(update)
}

/**
 * 格式化数字
 * 处理小数位和千分位分隔
 */
function formatNumber(num: number): string {
  let result = ''

  // 添加前缀
  if (props.prefix) {
    result += props.prefix
  }

  // 格式化数值部分
  const fixed = num.toFixed(props.decimals)
  const [intPart, decPart] = fixed.split('.')

  // 添加千分位分隔符
  if (props.separator) {
    result += intPart.replace(/\B(?=(\d{3})+(?!\d))/g, props.separator)
  } else {
    result += intPart
  }

  // 添加小数部分
  if (decPart) {
    result += `.${decPart}`
  }

  // 添加后缀
  if (props.suffix) {
    result += props.suffix
  }

  return result
}

// 监听目标值变化
watch(
  () => props.endValue,
  (newVal, oldVal) => {
    animateCount(oldVal || 0, newVal, props.duration)
  },
)

onMounted(() => {
  hasStarted.value = true
  animateCount(0, props.endValue, props.duration)
})

onUnmounted(() => {
  if (animationFrame) {
    cancelAnimationFrame(animationFrame)
  }
})
</script>

<style scoped>
.count-up {
  font-variant-numeric: tabular-nums;
  display: inline-block;
}
</style>
