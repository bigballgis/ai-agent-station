<template>
  <a-modal
    :open="visible"
    :title="modalTitle"
    :width="width"
    :centered="centered"
    :confirm-loading="confirmLoading"
    :ok-text="okText"
    :cancel-text="cancelText"
    :ok-type="okType"
    :closable="closable"
    :mask-closable="maskClosable"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <!-- 图标 + 内容 -->
    <div class="confirm-content">
      <div v-if="type !== 'info'" class="confirm-icon" :class="iconClass">
        <ExclamationCircleOutlined v-if="type === 'warning'" />
        <CloseCircleOutlined v-else-if="type === 'delete'" />
        <InfoCircleOutlined v-else />
      </div>
      <div class="confirm-body">
        <p class="confirm-text">{{ content }}</p>
        <slot name="extra" />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'

/**
 * ConfirmModal 组件
 * 可复用的确认弹窗，支持删除、警告、信息三种类型
 * 替代各页面重复的 Modal.confirm 调用
 */

interface Props {
  /** 是否显示 */
  visible: boolean
  /** 弹窗标题 */
  title?: string
  /** 弹窗内容 */
  content?: string
  /** 弹窗类型 */
  type?: 'delete' | 'warning' | 'info'
  /** 确认按钮加载状态 */
  confirmLoading?: boolean
  /** 弹窗宽度 */
  width?: number | string
  /** 是否居中 */
  centered?: boolean
  /** 确认按钮文本 */
  okText?: string
  /** 取消按钮文本 */
  cancelText?: string
  /** 是否可关闭 */
  closable?: boolean
  /** 点击遮罩是否关闭 */
  maskClosable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认操作',
  content: '',
  type: 'info',
  confirmLoading: false,
  width: 420,
  centered: true,
  okText: '确认',
  cancelText: '取消',
  closable: true,
  maskClosable: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'ok'): void
  (e: 'cancel'): void
}>()

// 根据类型计算标题
const modalTitle = computed(() => {
  if (props.title) return props.title
  switch (props.type) {
    case 'delete': return '确认删除'
    case 'warning': return '警告'
    default: return '提示'
  }
})

// 根据类型计算确认按钮类型
const okType = computed(() => {
  return props.type === 'delete' ? 'danger' : 'primary'
})

// 根据类型计算图标样式
const iconClass = computed(() => {
  switch (props.type) {
    case 'delete': return 'icon-danger'
    case 'warning': return 'icon-warning'
    default: return 'icon-info'
  }
})

/** 确认操作 */
function handleOk() {
  emit('ok')
}

/** 取消操作 */
function handleCancel() {
  emit('update:visible', false)
  emit('cancel')
}
</script>

<style scoped>
.confirm-content {
  display: flex;
  gap: 16px;
  padding: 8px 0;
}

.confirm-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.icon-danger {
  background-color: #fef2f2;
  color: #dc2626;
}

.icon-warning {
  background-color: #fffbeb;
  color: #d97706;
}

.icon-info {
  background-color: #eff6ff;
  color: #2563eb;
}

.confirm-body {
  flex: 1;
  min-width: 0;
}

.confirm-text {
  font-size: 14px;
  color: #525252;
  line-height: 1.6;
  margin: 4px 0 0;
}

/* 暗色模式 */
:global(.dark) .icon-danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
:global(.dark) .icon-warning {
  background-color: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
:global(.dark) .icon-info {
  background-color: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}
:global(.dark) .confirm-text {
  color: #a3a3a3;
}
</style>
