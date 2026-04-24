<template>
  <div class="search-bar">
    <div class="flex flex-wrap items-center gap-3">
      <!-- 动态生成的搜索字段 -->
      <template v-for="(field, _index) in visibleFields" :key="field.key">
        <!-- 输入框 -->
        <a-input
          v-if="field.type === 'input'"
          v-model:value="searchValues[field.key]"
          :placeholder="field.placeholder || t('common.pleaseInput', { label: field.label })"
          allow-clear
          style="min-width: 200px; max-width: 280px;"
          @pressEnter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined class="text-neutral-400" />
          </template>
        </a-input>

        <!-- 下拉选择 -->
        <a-select
          v-else-if="field.type === 'select'"
          v-model:value="searchValues[field.key]"
          :placeholder="field.placeholder || t('common.pleaseSelect', { label: field.label })"
          allow-clear
          style="min-width: 140px; max-width: 200px;"
          :options="field.options"
        />

        <!-- 日期范围选择 -->
        <a-range-picker
          v-else-if="field.type === 'dateRange'"
          v-model:value="searchValues[field.key]"
          :placeholder="[t('common.startDate'), t('common.endDate')]"
          style="min-width: 240px;"
          value-format="YYYY-MM-DD"
        />
      </template>

      <!-- 展开/收起按钮 -->
      <a-button
        v-if="fields.length > defaultShowCount"
        type="link"
        size="small"
        @click="expanded = !expanded"
      >
        {{ expanded ? t('common.collapse') : t('common.expand') }}
        <UpOutlined v-if="expanded" />
        <DownOutlined v-else />
      </a-button>

      <!-- 操作按钮 -->
      <div class="flex items-center gap-2">
        <a-button type="primary" @click="handleSearch">
          <template #icon>
            <SearchOutlined />
          </template>
          {{ t('common.search') }}
        </a-button>
        <a-button @click="handleReset">
          <template #icon>
            <ReloadOutlined />
          </template>
          {{ t('common.reset') }}
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined, ReloadOutlined, UpOutlined, DownOutlined } from '@ant-design/icons-vue'

/**
 * SearchBar 组件
 * 可复用的搜索/筛选栏，根据字段配置自动生成表单项
 * 支持输入框、下拉选择、日期范围选择
 * 支持折叠展开（默认显示前 3 个字段）
 * 输入框支持 300ms 防抖自动搜索
 */

export interface SearchField {
  /** 字段标签 */
  label: string
  /** 字段键名 */
  key: string
  /** 字段类型 */
  type: 'input' | 'select' | 'dateRange'
  /** 占位文本 */
  placeholder?: string
  /** 下拉选项（仅 select 类型） */
  options?: Array<{ label: string; value: string | number }>
}

interface Props {
  /** 搜索字段配置 */
  fields: SearchField[]
  /** 默认显示字段数量 */
  defaultShowCount?: number
  /** 输入框防抖延迟（毫秒），默认 300ms */
  debounceDelay?: number
}

const props = withDefaults(defineProps<Props>(), {
  defaultShowCount: 3,
  debounceDelay: 300,
})

const { t } = useI18n()

const emit = defineEmits<{
  /** 搜索事件，返回搜索参数 */
  (e: 'search', params: Record<string, any>): void
  /** 重置事件 */
  (e: 'reset'): void
}>()

// 展开状态
const expanded = ref(false)

// 搜索值
const searchValues = reactive<Record<string, any>>(() => {
  const values: Record<string, any> = {}
  props.fields.forEach(field => {
    values[field.key] = undefined
  })
  return values
})

// 可见字段（折叠时只显示前 N 个）
const visibleFields = computed(() => {
  if (expanded.value) return props.fields
  return props.fields.slice(0, props.defaultShowCount)
})

// 防抖定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

/** 执行搜索 */
function handleSearch() {
  // 清除防抖定时器
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
  // 过滤掉空值
  const params: Record<string, any> = {}
  Object.entries(searchValues).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value
    }
  })
  emit('search', params)
}

/** 防抖搜索（用于输入框实时搜索） */
function debouncedSearch() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    handleSearch()
  }, props.debounceDelay)
}

/** 重置搜索条件 */
function handleReset() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
  props.fields.forEach(field => {
    searchValues[field.key] = undefined
  })
  emit('reset')
}

// 监听输入框值变化，自动触发防抖搜索
watch(searchValues, () => {
  debouncedSearch()
}, { deep: true })
</script>

<style scoped>
.search-bar {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

:deep(.ant-input),
:deep(.ant-select),
:deep(.ant-picker) {
  border-radius: 10px;
}

/* 暗色模式适配 */
:global(.dark) .search-bar {
  background: #171717;
  border-color: #262626;
}
</style>
