<template>
  <div class="description-list" :class="{ 'description-list-bordered': bordered }">
    <!-- 标题 -->
    <div v-if="title" class="description-list-title">{{ title }}</div>

    <!-- 描述列表 -->
    <a-row :gutter="gutter">
      <a-col
        v-for="item in items"
        :key="item.label"
        :span="item.span || Math.floor(24 / column)"
      >
        <div class="description-item">
          <dt class="description-label">{{ item.label }}</dt>
          <dd class="description-value">
            <slot :name="`value-${item.key || item.label}`" :item="item">
              {{ item.value ?? '-' }}
            </slot>
          </dd>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
/**
 * DescriptionList 组件
 * 描述列表组件，用于详情展示
 * 替代重复的 key-value 详情展示模式
 * 用于 DetailDrawer、详情弹窗等场景
 */

// 描述项类型
export interface DescriptionItem {
  /** 标签 */
  label: string
  /** 值 */
  value?: string | number | boolean | null
  /** 键名（用于插槽） */
  key?: string
  /** 列宽（24 栅格） */
  span?: number
}

interface Props {
  /** 描述项列表 */
  items: DescriptionItem[]
  /** 标题 */
  title?: string
  /** 是否显示边框 */
  bordered?: boolean
  /** 列数 */
  column?: number
  /** 栅格间距 */
  gutter?: number | [number, number]
}

withDefaults(defineProps<Props>(), {
  bordered: false,
  column: 2,
  gutter: 16,
})
</script>

<style scoped>
.description-list {
  width: 100%;
}

.description-list-title {
  font-size: 15px;
  font-weight: 600;
  color: #171717;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.description-item {
  padding: 12px 0;
}

/* 带边框模式 */
.description-list-bordered .description-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
}

.description-list-bordered .description-item:last-child {
  border-right: none;
}

.description-label {
  font-size: 13px;
  color: #737373;
  margin-bottom: 4px;
  line-height: 1.5;
  font-weight: 500;
}

.description-value {
  font-size: 14px;
  color: #171717;
  line-height: 1.6;
  margin: 0;
  word-break: break-all;
}

/* 响应式：小屏幕单列 */
@media (max-width: 768px) {
  .description-item {
    border-right: none !important;
  }
}

/* 暗色模式 */
:global(.dark) .description-list-title {
  color: #fafafa;
  border-bottom-color: #262626;
}

:global(.dark) .description-list-bordered .description-item {
  border-bottom-color: #262626;
  border-right-color: #262626;
}

:global(.dark) .description-label {
  color: #a3a3a3;
}

:global(.dark) .description-value {
  color: #e5e5e5;
}
</style>
