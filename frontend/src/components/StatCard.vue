<template>
  <div
    class="stat-card"
    :class="[`stat-card-${color}`, { 'stat-card-clickable': clickable }]"
    :aria-label="`${title}: ${value}${trendValue ? ', ' + trendValue : ''}`"
    @click="$emit('click', $event)"
  >
    <!-- 背景装饰 -->
    <div class="stat-card-bg-circle stat-card-bg-circle-1" aria-hidden="true" />
    <div class="stat-card-bg-circle stat-card-bg-circle-2" aria-hidden="true" />

    <!-- 图标 -->
    <div v-if="icon" class="stat-card-icon">
      <component :is="icon" />
    </div>

    <!-- 内容 -->
    <div class="stat-card-content">
      <p class="stat-card-label">{{ title }}</p>
      <div class="stat-card-value-row">
        <p class="stat-card-value">
          <span v-if="prefix" class="stat-card-prefix">{{ prefix }}</span>
          <CountUp
            v-if="animate"
            :end-value="Number(value)"
            :duration="1500"
            :decimals="decimals"
          />
          <span v-else>{{ formattedValue }}</span>
          <span v-if="suffix" class="stat-card-suffix">{{ suffix }}</span>
        </p>
      </div>

      <!-- 趋势 -->
      <div v-if="trend !== 'none' && trendValue" class="stat-card-trend" :class="`trend-${trend}`">
        <svg v-if="trend === 'up'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
        </svg>
        <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
        </svg>
        <span>{{ trendValue }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import CountUp from './CountUp.vue'

/**
 * StatCard 组件
 * 统计卡片组件，用于 Dashboard 和 HomePage 展示关键指标
 * 支持图标、趋势、数值动画、多种颜色主题
 */

interface Props {
  /** 卡片标题 */
  title: string
  /** 卡片数值 */
  value: string | number
  /** 图标组件 */
  icon?: Component
  /** 趋势方向 */
  trend?: 'up' | 'down' | 'none'
  /** 趋势值文本 */
  trendValue?: string
  /** 颜色主题 */
  color?: 'blue' | 'green' | 'purple' | 'orange' | 'red' | 'cyan'
  /** 数值前缀 */
  prefix?: string
  /** 数值后缀 */
  suffix?: string
  /** 小数位数 */
  decimals?: number
  /** 是否启用数值动画 */
  animate?: boolean
  /** 是否可点击 */
  clickable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  trend: 'none',
  color: 'blue',
  decimals: 0,
  animate: true,
  clickable: false,
})

defineEmits<{
  (e: 'click', event: Event): void
}>()

// 格式化数值
const formattedValue = computed(() => {
  const num = Number(props.value)
  if (isNaN(num)) return String(props.value)
  if (props.decimals > 0) return num.toFixed(props.decimals)
  return num.toLocaleString()
})
</script>

<style scoped>
.stat-card {
  position: relative;
  border-radius: 16px;
  padding: 24px;
  color: white;
  overflow: hidden;
  cursor: default;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.stat-card-clickable {
  cursor: pointer;
}

.stat-card-clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px -8px rgba(0, 0, 0, 0.15);
}

.stat-card-clickable:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.8);
  outline-offset: 2px;
}

/* 颜色主题 */
.stat-card-blue { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-card-green { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-card-purple { background: linear-gradient(135deg, #a855f7, #9333ea); }
.stat-card-orange { background: linear-gradient(135deg, #f97316, #ea580c); }
.stat-card-red { background: linear-gradient(135deg, #ef4444, #dc2626); }
.stat-card-cyan { background: linear-gradient(135deg, #06b6d4, #0891b2); }

/* 背景装饰圆 */
.stat-card-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  transition: transform 0.5s;
}

.stat-card-bg-circle-1 {
  width: 96px;
  height: 96px;
  right: -16px;
  top: -16px;
}

.stat-card-bg-circle-2 {
  width: 64px;
  height: 64px;
  right: -8px;
  bottom: -24px;
  background: rgba(255, 255, 255, 0.05);
}

.stat-card-clickable:hover .stat-card-bg-circle-1 {
  transform: scale(1.25);
}

/* 图标 */
.stat-card-icon {
  position: absolute;
  right: 20px;
  top: 20px;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

/* 内容 */
.stat-card-content {
  position: relative;
  z-index: 1;
}

.stat-card-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  margin: 0;
}

.stat-card-value {
  font-size: 30px;
  font-weight: 700;
  margin: 8px 0 0;
  letter-spacing: -0.025em;
  line-height: 1.2;
}

.stat-card-prefix,
.stat-card-suffix {
  font-size: 16px;
  font-weight: 500;
  opacity: 0.8;
}

/* 趋势 */
.stat-card-trend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.trend-up svg {
  color: rgba(255, 255, 255, 0.9);
}

.trend-down svg {
  color: rgba(255, 255, 255, 0.9);
}
</style>
