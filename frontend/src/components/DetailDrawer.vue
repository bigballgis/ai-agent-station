<template>
  <a-drawer
    :open="visible"
    :title="title"
    :width="width"
    :placement="placement"
    :closable="closable"
    :mask-closable="maskClosable"
    :destroy-on-close="destroyOnClose"
    :class="['detail-drawer', { 'detail-drawer-dark': isDark }]"
    @close="handleClose"
  >
    <!-- 默认内容插槽 -->
    <div class="drawer-body">
      <slot />
    </div>

    <!-- 底部操作栏 -->
    <template v-if="$slots.footer" #footer>
      <div class="drawer-footer">
        <slot name="footer" />
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

/**
 * DetailDrawer 组件
 * 侧滑详情抽屉，用于在不离开列表页的情况下查看记录详情
 * 封装 a-drawer，提供统一的样式和交互
 */

interface Props {
  /** 是否显示 */
  visible: boolean
  /** 抽屉标题 */
  title?: string
  /** 抽屉宽度 */
  width?: number | string
  /** 抽屉方向 */
  placement?: 'left' | 'right' | 'top' | 'bottom'
  /** 是否显示关闭按钮 */
  closable?: boolean
  /** 点击遮罩是否关闭 */
  maskClosable?: boolean
  /** 关闭时是否销毁内容 */
  destroyOnClose?: boolean
}

withDefaults(defineProps<Props>(), {
  title: t('component.detail'),
  width: 640,
  placement: 'right',
  closable: true,
  maskClosable: true,
  destroyOnClose: true,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
}>()

const { isDark } = useTheme()

/** 关闭抽屉 */
function handleClose() {
  emit('update:visible', false)
  emit('close')
}
</script>

<style scoped>
.drawer-body {
  padding: 0 4px;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

/* 覆盖抽屉圆角 */
:deep(.ant-drawer-content) {
  border-radius: 16px 0 0 16px;
}

:deep(.ant-drawer-header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;
}

:deep(.ant-drawer-body) {
  padding: 20px 24px;
}

:deep(.ant-drawer-footer) {
  border-top: 1px solid #f0f0f0;
  padding: 12px 24px;
}

/* 暗色模式 */
:global(.dark) :deep(.ant-drawer-content) {
  background-color: #171717;
}

:global(.dark) :deep(.ant-drawer-header) {
  border-bottom-color: #262626;
  background-color: #171717;
}

:global(.dark) :deep(.ant-drawer-body) {
  background-color: #171717;
}

:global(.dark) :deep(.ant-drawer-footer) {
  border-top-color: #262626;
  background-color: #171717;
}
</style>
