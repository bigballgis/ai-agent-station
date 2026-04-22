<template>
  <div class="empty-state" :class="`empty-state-${type}`">
    <!-- 插画区域 -->
    <div class="empty-illustration">
      <!-- 无数据 -->
      <svg v-if="type === 'noData'" viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="40" y="30" width="120" height="100" rx="12" fill="currentColor" opacity="0.06" />
        <rect x="55" y="50" width="60" height="6" rx="3" fill="currentColor" opacity="0.15" />
        <rect x="55" y="65" width="90" height="6" rx="3" fill="currentColor" opacity="0.1" />
        <rect x="55" y="80" width="75" height="6" rx="3" fill="currentColor" opacity="0.1" />
        <rect x="55" y="95" width="50" height="6" rx="3" fill="currentColor" opacity="0.08" />
        <circle cx="100" cy="20" r="4" fill="currentColor" opacity="0.2" />
      </svg>

      <!-- 无搜索结果 -->
      <svg v-else-if="type === 'noSearch'" viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="85" cy="75" r="35" stroke="currentColor" stroke-width="4" opacity="0.15" />
        <line x1="112" y1="102" x2="140" y2="130" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.15" />
        <line x1="70" y1="65" x2="100" y2="65" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.2" />
        <line x1="70" y1="78" x2="90" y2="78" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.15" />
        <line x1="70" y1="91" x2="95" y2="91" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.1" />
      </svg>

      <!-- 无权限 -->
      <svg v-else-if="type === 'noPermission'" viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M100 25L160 55V95C160 120 133 142 100 150C67 142 40 120 40 95V55L100 25Z" stroke="currentColor" stroke-width="4" fill="currentColor" opacity="0.05" />
        <path d="M100 25L160 55V95C160 120 133 142 100 150C67 142 40 120 40 95V55L100 25Z" stroke="currentColor" stroke-width="3" opacity="0.15" fill="none" />
        <line x1="80" y1="85" x2="120" y2="85" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.2" />
        <circle cx="100" cy="70" r="3" fill="currentColor" opacity="0.2" />
      </svg>

      <!-- 错误 -->
      <svg v-else-if="type === 'error'" viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="100" cy="80" r="45" stroke="currentColor" stroke-width="4" opacity="0.12" />
        <line x1="85" y1="65" x2="115" y2="95" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.2" />
        <line x1="115" y1="65" x2="85" y2="95" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.2" />
      </svg>

      <!-- 网络错误 -->
      <svg v-else viewBox="0 0 200 160" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="30" y="40" width="140" height="80" rx="12" stroke="currentColor" stroke-width="3" opacity="0.12" />
        <line x1="30" y1="70" x2="170" y2="70" stroke="currentColor" stroke-width="2" opacity="0.08" />
        <circle cx="45" cy="55" r="5" fill="currentColor" opacity="0.15" />
        <circle cx="60" cy="55" r="5" fill="currentColor" opacity="0.15" />
        <circle cx="75" cy="55" r="5" fill="currentColor" opacity="0.15" />
        <path d="M90 100L110 100" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.15" />
        <path d="M95 95L100 100L95 105" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.12" />
      </svg>
    </div>

    <!-- 文字描述 -->
    <h3 class="empty-title">{{ defaultTitle }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>

    <!-- 操作按钮 -->
    <a-button
      v-if="actionText"
      type="primary"
      class="empty-action"
      @click="$emit('action')"
    >
      {{ actionText }}
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * EmptyState 组件
 * 增强版空状态组件，替代基础的 Empty 组件
 * 提供多种类型的插画和描述
 */

interface Props {
  /** 空状态类型 */
  type?: 'noData' | 'noSearch' | 'noPermission' | 'error' | 'network'
  /** 自定义描述 */
  description?: string
  /** 操作按钮文本 */
  actionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'noData',
})

defineEmits<{
  (e: 'action'): void
}>()

// 默认标题
const defaultTitle = computed(() => {
  switch (props.type) {
    case 'noData': return '暂无数据'
    case 'noSearch': return '未找到匹配结果'
    case 'noPermission': return '暂无访问权限'
    case 'error': return '出了点问题'
    case 'network': return '网络连接异常'
    default: return '暂无数据'
  }
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-illustration {
  width: 160px;
  height: 128px;
  margin-bottom: 20px;
  color: #a3a3a3;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #525252;
  margin: 0 0 8px;
}

.empty-description {
  font-size: 14px;
  color: #a3a3a3;
  margin: 0 0 20px;
  max-width: 320px;
  line-height: 1.5;
}

.empty-action {
  border-radius: 10px;
}

/* 暗色模式 */
:global(.dark) .empty-illustration {
  color: #525252;
}

:global(.dark) .empty-title {
  color: #d4d4d4;
}

:global(.dark) .empty-description {
  color: #525252;
}
</style>
