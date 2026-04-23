<template>
  <div class="time-range-picker">
    <!-- 快捷预设按钮 -->
    <div class="preset-buttons">
      <a-button
        v-for="preset in presets"
        :key="preset.key"
        :type="activePreset === preset.key ? 'primary' : 'default'"
        size="small"
        class="preset-btn"
        @click="selectPreset(preset)"
      >
        {{ typeof preset.label === 'function' ? preset.label() : preset.label }}
      </a-button>
      <a-button
        :type="activePreset === 'custom' ? 'primary' : 'default'"
        size="small"
        class="preset-btn"
        @click="showCustomRange = true"
      >
        {{ t('common.custom') }}
      </a-button>
    </div>

    <!-- 自定义日期范围选择 -->
    <div v-if="showCustomRange" class="custom-range">
      <a-range-picker
        v-model:value="customRange"
        :placeholder="[t('common.startDate'), t('common.endDate')]"
        value-format="YYYY-MM-DD"
        style="width: 100%;"
        @change="handleCustomChange"
      />
    </div>

    <!-- 当前选择显示 -->
    <div v-if="displayRange" class="range-display">
      <CalendarOutlined class="range-icon" />
      <span>{{ displayRange }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CalendarOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'

/**
 * TimeRangePicker 组件
 * 时间范围快捷选择组件
 * 提供常用时间预设：今天、最近7天、最近30天、自定义
 * 用于 LogCenter、Dashboard 等页面
 */

// 预设定义
interface Preset {
  key: string
  label: string | (() => string)
  getValue: () => [string, string]
}

const { t } = useI18n()

const presets: Preset[] = [
  {
    key: 'today',
    label: () => t('timeRange.today'),
    getValue: () => [dayjs().format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')],
  },
  {
    key: 'yesterday',
    label: () => t('timeRange.yesterday'),
    getValue: () => [
      dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
      dayjs().subtract(1, 'day').format('YYYY-MM-DD'),
    ],
  },
  {
    key: '7days',
    label: () => t('timeRange.last7Days'),
    getValue: () => [
      dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ],
  },
  {
    key: '30days',
    label: () => t('timeRange.last30Days'),
    getValue: () => [
      dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ],
  },
  {
    key: '90days',
    label: () => t('timeRange.last90Days'),
    getValue: () => [
      dayjs().subtract(89, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ],
  },
]

interface Props {
  /** 当前选中的时间范围 [开始日期, 结束日期] */
  modelValue?: [string, string]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: [string, string]): void
  (e: 'change', value: [string, string]): void
}>()

const activePreset = ref('7days')
const showCustomRange = ref(false)
const customRange = ref<[Dayjs, Dayjs] | null>(null)

// 显示的范围文本
const displayRange = computed(() => {
  if (activePreset.value === 'custom' && customRange.value) {
    return `${customRange.value[0].format('YYYY-MM-DD')} ~ ${customRange.value[1].format('YYYY-MM-DD')}`
  }
  const preset = presets.find(p => p.key === activePreset.value)
  if (preset) {
    const [start, end] = preset.getValue()
    return `${start} ~ ${end}`
  }
  return ''
})

/** 选择预设 */
function selectPreset(preset: Preset) {
  activePreset.value = preset.key
  showCustomRange.value = false
  const value = preset.getValue()
  emit('update:modelValue', value)
  emit('change', value)
}

/** 自定义范围变化 */
function handleCustomChange(dates: [Dayjs, Dayjs] | null) {
  if (dates) {
    activePreset.value = 'custom'
    customRange.value = dates
    const value: [string, string] = [dates[0].format('YYYY-MM-DD'), dates[1].format('YYYY-MM-DD')]
    emit('update:modelValue', value)
    emit('change', value)
  }
}

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  if (val) {
    // 检查是否匹配某个预设
    const matchedPreset = presets.find(p => {
      const [start, end] = p.getValue()
      return start === val[0] && end === val[1]
    })
    if (matchedPreset) {
      activePreset.value = matchedPreset.key
      showCustomRange.value = false
    } else {
      activePreset.value = 'custom'
      showCustomRange.value = true
      customRange.value = [dayjs(val[0]), dayjs(val[1])]
    }
  }
}, { immediate: true })
</script>

<style scoped>
.time-range-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preset-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preset-btn {
  border-radius: 8px !important;
  font-size: 13px !important;
}

.custom-range {
  width: 100%;
}

:deep(.custom-range .ant-picker) {
  border-radius: 10px !important;
  width: 100% !important;
}

.range-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #525252;
}

.range-icon {
  color: #a3a3a3;
}

/* 暗色模式 */
:global(.dark) .range-display {
  color: #a3a3a3;
}

:global(.dark) .range-icon {
  color: #525252;
}
</style>
