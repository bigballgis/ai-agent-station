<template>
  <div class="pro-table">
    <!-- 搜索栏 -->
    <div v-if="searchFields && searchFields.length > 0" class="mb-4">
      <SearchBar :fields="searchFields" @search="handleSearch" @reset="handleReset" />
    </div>

    <!-- 工具栏 -->
    <div v-if="$slots.toolbar" class="mb-4 flex items-center justify-between">
      <slot name="toolbar" />
      <div class="flex items-center gap-2">
        <a-button
          type="text"
          size="small"
          @click="handleRefresh"
        >
          <template #icon>
            <ReloadOutlined :spin="loading" />
          </template>
          {{ t('common.refresh') }}
        </a-button>
      </div>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="false"
      :scroll="scroll"
      :row-key="rowKey"
      :size="size"
      bordered
      class="pro-table-wrapper"
    >
      <!-- 透传所有具名插槽 -->
      <template v-for="(_, name) in $slots" #[name]="slotData">
        <slot v-if="name !== 'toolbar'" :name="name" v-bind="slotData || {}" />
      </template>
    </a-table>

    <!-- 分页 -->
    <div v-if="pagination !== false" class="mt-4 flex items-center justify-end">
      <a-pagination
        v-model:current="currentPage"
        v-model:page-size="currentPageSize"
        :total="total"
        :show-size-changer="showSizeChanger"
        :show-quick-jumper="showQuickJumper"
        :show-total="showTotal ? (total: number) => t('common.totalRecords', { total }) : undefined"
        :page-sizes="pageSizes"
        size="small"
        @change="handlePageChange"
        @showSizeChange="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined } from '@ant-design/icons-vue'
import SearchBar from './SearchBar.vue'
import type { TableProps } from 'ant-design-vue'

/**
 * ProTable 组件
 * 封装 a-table，集成搜索栏、工具栏、分页、刷新等功能
 * 替代项目中 15+ 页面重复的表格模式
 */

// 列定义类型
export interface ProTableColumn {
  title: string
  dataIndex: string
  key?: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  ellipsis?: boolean
  fixed?: 'left' | 'right' | boolean
  sorter?: boolean | ((a: any, b: any) => number)
  customRender?: any
  [key: string]: any
}

// 搜索字段类型
export interface SearchField {
  label: string
  key: string
  type: 'input' | 'select' | 'dateRange'
  placeholder?: string
  options?: Array<{ label: string; value: string | number }>
}

// 分页配置类型
export interface PaginationConfig {
  current?: number
  pageSize?: number
  total?: number
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  showTotal?: boolean
  pageSizes?: number[]
}

interface Props {
  /** 表格列定义 */
  columns: ProTableColumn[]
  /** 数据源 */
  dataSource: any[]
  /** 加载状态 */
  loading?: boolean
  /** 分页配置，false 表示不分页 */
  pagination?: PaginationConfig | false
  /** 行选择配置 */
  rowSelection?: TableProps['rowSelection']
  /** 搜索字段配置 */
  searchFields?: SearchField[]
  /** 行 key */
  rowKey?: string | ((record: any) => string)
  /** 表格尺寸 */
  size?: 'small' | 'middle' | 'large'
  /** 滚动配置 */
  scroll?: { x?: number | string; y?: number | string }
  /** 是否显示快速跳转 */
  showQuickJumper?: boolean
  /** 是否显示总数 */
  showTotal?: boolean
  /** 是否显示页大小切换 */
  showSizeChanger?: boolean
  /** 页大小选项 */
  pageSizes?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  rowKey: 'id',
  size: 'middle',
  showQuickJumper: true,
  showTotal: true,
  showSizeChanger: true,
  pageSizes: () => [10, 20, 50, 100],
})

const { t } = useI18n()

const emit = defineEmits<{
  /** 搜索事件 */
  (e: 'search', params: Record<string, any>): void
  /** 重置事件 */
  (e: 'reset'): void
  /** 刷新事件 */
  (e: 'refresh'): void
  /** 分页变化事件 */
  (e: 'pageChange', current: number, pageSize: number): void
  /** 页大小变化事件 */
  (e: 'sizeChange', current: number, pageSize: number): void
}>()

// 内部分页状态
const currentPage = ref(props.pagination !== false ? (props.pagination as PaginationConfig).current || 1 : 1)
const currentPageSize = ref(props.pagination !== false ? (props.pagination as PaginationConfig).pageSize || 10 : 10)

// 计算总数
const total = computed(() => {
  if (props.pagination === false) return 0
  return (props.pagination as PaginationConfig).total ?? props.dataSource.length
})

// 监听外部分页变化
watch(() => props.pagination, (val) => {
  if (val !== false) {
    currentPage.value = (val as PaginationConfig).current || 1
    currentPageSize.value = (val as PaginationConfig).pageSize || 10
  }
}, { deep: true })

/** 搜索处理 */
function handleSearch(params: Record<string, any>) {
  currentPage.value = 1
  emit('search', params)
}

/** 重置处理 */
function handleReset() {
  currentPage.value = 1
  emit('reset')
}

/** 刷新处理 */
function handleRefresh() {
  emit('refresh')
}

/** 分页变化 */
function handlePageChange(page: number, pageSize: number) {
  currentPage.value = page
  currentPageSize.value = pageSize
  emit('pageChange', page, pageSize)
}

/** 页大小变化 */
function handleSizeChange(current: number, size: number) {
  currentPage.value = 1
  currentPageSize.value = size
  emit('sizeChange', current, size)
}
</script>

<style scoped>
.pro-table {
  width: 100%;
}

/* 覆盖 ant-design 表格圆角样式 */
:deep(.pro-table-wrapper .ant-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.pro-table-wrapper .ant-table-thead > tr > th) {
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

:deep(.pro-table-wrapper .ant-table-tbody > tr > td) {
  font-size: 14px;
}
</style>
