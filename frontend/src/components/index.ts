/**
 * 组件库统一导出文件
 * AI Agent Station 可复用组件库
 *
 * 使用方式：
 * import { ProTable, SearchBar, StatusBadge } from '@/components'
 */

// 数据展示组件
export { default as ProTable } from './ProTable.vue'
export { default as SearchBar } from './SearchBar.vue'
export { default as PageHeader } from './PageHeader.vue'
export { default as StatusBadge } from './StatusBadge.vue'
export { default as DescriptionList } from './DescriptionList.vue'

// 反馈组件
export { default as ConfirmModal } from './ConfirmModal.vue'
export { default as DetailDrawer } from './DetailDrawer.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as LoadingSkeleton } from './LoadingSkeleton.vue'
export { default as ChangePasswordModal } from './ChangePasswordModal.vue'

// 数据可视化组件
export { default as StatCard } from './StatCard.vue'
export { default as ChartContainer } from './ChartContainer.vue'
export { default as CountUp } from './CountUp.vue'

// 表单/输入组件
export { default as JsonEditor } from './JsonEditor.vue'
export { default as FileUpload } from './FileUpload.vue'
export { default as TreeSelect } from './TreeSelect.vue'
export { default as CodeEditor } from './CodeEditor.vue'
export { default as TimeRangePicker } from './TimeRangePicker.vue'
export { default as FormBuilder } from './FormBuilder.vue'

// 导航/布局组件
export { default as AvatarGroup } from './AvatarGroup.vue'
export { default as TabNav } from './TabNav.vue'
export { default as BreadcrumbNav } from './BreadcrumbNav.vue'
export { default as NotificationBadge } from './NotificationBadge.vue'
export { default as PermissionWrapper } from './PermissionWrapper.vue'

// 工具组件
export { default as MarkdownRenderer } from './MarkdownRenderer.vue'

// 类型导出
export type { ProTableColumn, SearchField, PaginationConfig } from './ProTable.vue'
export type { SearchField as SearchBarField } from './SearchBar.vue'
export type { BreadcrumbItem as PageHeaderBreadcrumb } from './PageHeader.vue'
export type { DescriptionItem } from './DescriptionList.vue'
export type { TreeNode } from './TreeSelect.vue'
export type { FormField } from './FormBuilder.vue'
export type { TabItem } from './TabNav.vue'
export type { BreadcrumbItem } from './BreadcrumbNav.vue'
export type { NotificationItem } from './NotificationBadge.vue'
export type { AvatarUser } from './AvatarGroup.vue'
