<template>
  <span
    class="status-badge"
    :class="badgeClass"
  >
    <span v-if="dot" class="status-dot" :class="dotClass" />
    {{ displayLabel }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

/**
 * StatusBadge 组件
 * 统一的状态标签组件，将状态字符串映射到颜色和本地化标签
 * 替代各页面重复的状态渲染逻辑（AgentList, AlertNotification, ApiManagement 等）
 */

// Agent 状态映射
const agentStatusMap: Record<string, { label: string; color: string }> = {
  DRAFT: { label: t('status.draft'), color: 'neutral' },
  PENDING_APPROVAL: { label: t('status.pendingApproval'), color: 'warning' },
  APPROVED: { label: t('status.approved'), color: 'success' },
  PUBLISHED: { label: t('status.published'), color: 'primary' },
  ARCHIVED: { label: t('status.archived'), color: 'neutral' },
  RUNNING: { label: t('status.running'), color: 'success' },
  STOPPED: { label: t('status.stopped'), color: 'neutral' },
  ERROR: { label: t('status.error'), color: 'danger' },
}

// 测试状态映射
const testStatusMap: Record<string, { label: string; color: string }> = {
  PASSED: { label: t('status.passed'), color: 'success' },
  FAILED: { label: t('status.failed'), color: 'danger' },
  RUNNING: { label: t('status.running'), color: 'primary' },
  PENDING: { label: t('status.pending'), color: 'warning' },
  SKIPPED: { label: t('status.skipped'), color: 'neutral' },
  passed: { label: t('status.passed'), color: 'success' },
  failed: { label: t('status.failed'), color: 'danger' },
  running: { label: t('status.running'), color: 'primary' },
  pending: { label: t('status.pending'), color: 'warning' },
}

// 审批状态映射
const approvalStatusMap: Record<string, { label: string; color: string }> = {
  PENDING: { label: t('status.pendingApproval'), color: 'warning' },
  APPROVED: { label: t('status.approved'), color: 'success' },
  REJECTED: { label: t('status.rejected'), color: 'danger' },
  CANCELLED: { label: t('status.cancelled'), color: 'neutral' },
}

// 发布状态映射
const deploymentStatusMap: Record<string, { label: string; color: string }> = {
  DEPLOYING: { label: t('status.deploying'), color: 'primary' },
  DEPLOYED: { label: t('status.deployed'), color: 'success' },
  FAILED: { label: t('status.deployFailed'), color: 'danger' },
  ROLLBACK: { label: t('status.rollback'), color: 'warning' },
}

// 告警状态映射
const alertStatusMap: Record<string, { label: string; color: string }> = {
  firing: { label: t('status.firing'), color: 'danger' },
  resolved: { label: t('status.resolved'), color: 'success' },
  critical: { label: t('status.critical'), color: 'danger' },
  warning: { label: t('status.warning'), color: 'warning' },
  info: { label: t('status.info'), color: 'primary' },
}

// 通用状态映射
const userStatusMap: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: t('status.active'), color: 'success' },
  INACTIVE: { label: t('status.inactive'), color: 'neutral' },
  DISABLED: { label: t('status.disabled'), color: 'danger' },
  ENABLED: { label: t('status.enabled'), color: 'success' },
  LOCKED: { label: t('status.locked'), color: 'warning' },
}

// 颜色到样式类映射
const colorClassMap: Record<string, string> = {
  primary: 'badge-primary',
  success: 'badge-success',
  warning: 'badge-warning',
  danger: 'badge-danger',
  neutral: 'badge-neutral',
}

const dotColorMap: Record<string, string> = {
  primary: 'dot-primary',
  success: 'dot-success',
  warning: 'dot-warning',
  danger: 'dot-danger',
  neutral: 'dot-neutral',
}

interface Props {
  /** 状态值 */
  status: string
  /** 状态类型，决定使用哪个映射表 */
  type?: 'agent' | 'test' | 'approval' | 'deployment' | 'alert' | 'user'
  /** 是否显示状态点 */
  dot?: boolean
  /** 自定义标签（优先级最高） */
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'agent',
  dot: false,
})

// 根据类型选择映射表
const statusMap = computed(() => {
  switch (props.type) {
    case 'test': return testStatusMap
    case 'approval': return approvalStatusMap
    case 'deployment': return deploymentStatusMap
    case 'alert': return alertStatusMap
    case 'user': return userStatusMap
    default: return agentStatusMap
  }
})

// 获取状态配置
const statusConfig = computed(() => {
  return statusMap.value[props.status] || { label: props.status, color: 'neutral' }
})

// 显示标签
const displayLabel = computed(() => {
  return props.label || statusConfig.value.label
})

// 徽章样式
const badgeClass = computed(() => {
  return colorClassMap[statusConfig.value.color] || colorClassMap.neutral
})

// 状态点样式
const dotClass = computed(() => {
  return dotColorMap[statusConfig.value.color] || dotColorMap.neutral
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 颜色变体 - 亮色 */
.badge-primary {
  background-color: #eff6ff;
  color: #2563eb;
}
.badge-success {
  background-color: #f0fdf4;
  color: #16a34a;
}
.badge-warning {
  background-color: #fffbeb;
  color: #d97706;
}
.badge-danger {
  background-color: #fef2f2;
  color: #dc2626;
}
.badge-neutral {
  background-color: #f5f5f5;
  color: #737373;
}

.dot-primary { background-color: #3b82f6; }
.dot-success { background-color: #22c55e; }
.dot-warning { background-color: #f59e0b; }
.dot-danger { background-color: #ef4444; }
.dot-neutral { background-color: #a3a3a3; }

/* 暗色模式 */
:global(.dark) .badge-primary {
  background-color: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}
:global(.dark) .badge-success {
  background-color: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
:global(.dark) .badge-warning {
  background-color: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
:global(.dark) .badge-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
:global(.dark) .badge-neutral {
  background-color: rgba(163, 163, 163, 0.1);
  color: #a3a3a3;
}
</style>
