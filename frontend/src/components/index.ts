/**
 * 组件库统一导出文件
 * AI Agent Station 可复用组件库
 *
 * 使用方式：
 * import { PageHeader, SearchBar, StatusBadge } from '@/components'
 */

// 数据展示组件
export { default as SearchBar } from './SearchBar.vue'
export { default as PageHeader } from './PageHeader.vue'
export { default as StatusBadge } from './StatusBadge.vue'

// 反馈组件
export { default as ConfirmModal } from './ConfirmModal.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as LoadingSkeleton } from './LoadingSkeleton.vue'
export { default as ChunkLoadErrorFallback } from './ChunkLoadErrorFallback.vue'

// 数据可视化组件
export { default as StatCard } from './StatCard.vue'
export { default as ChartContainer } from './ChartContainer.vue'

// 表单/输入组件
export { default as TimeRangePicker } from './TimeRangePicker.vue'

// 导航/布局组件
export { default as TabNav } from './TabNav.vue'

// 类型导出
export type { SearchField } from './SearchBar.vue'
export type { TabItem } from './TabNav.vue'
