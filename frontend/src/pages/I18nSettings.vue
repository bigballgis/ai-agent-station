<template>
  <div class="i18n-settings-page" aria-label="国际化设置">
    <!-- 页面头部 -->
    <PageHeader
      :title="t('i18n.settings')"
      :subtitle="t('menu.i18n')"
    >
      <template #actions>
        <!-- 语言切换器 -->
        <a-radio-group
          :value="currentLocale"
          button-style="solid"
          size="large"
          @change="handleLocaleChange"
        >
          <a-radio-button value="zh-CN">
            {{ t('i18n.chinese') }}
          </a-radio-button>
          <a-radio-button value="en-US">
            {{ t('i18n.english') }}
          </a-radio-button>
        </a-radio-group>
      </template>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6 animate-slide-up">
      <!-- 总键数 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-neutral-900 dark:text-neutral-100">{{ totalKeys }}</div>
            <div class="text-sm text-neutral-500 dark:text-neutral-400">{{ t('i18n.total') }} {{ t('i18n.keys') }}</div>
          </div>
        </div>
      </div>

      <!-- zh-CN 键数 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-50 dark:bg-green-900/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5h12M9 3v2m1.048 9.5A18.022 18.022 0 016.412 9m6.088 9h7M11 21l5-10 5 10M12.751 5C11.783 10.77 8.07 15.61 3 18.129" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-neutral-900 dark:text-neutral-100">{{ zhKeyCount }}</div>
            <div class="text-sm text-neutral-500 dark:text-neutral-400">zh-CN {{ t('i18n.keys') }}</div>
          </div>
        </div>
      </div>

      <!-- en-US 键数 -->
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-purple-50 dark:bg-purple-900/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5h12M9 3v2m1.048 9.5A18.022 18.022 0 016.412 9m6.088 9h7M11 21l5-10 5 10M12.751 5C11.783 10.77 8.07 15.61 3 18.129" />
            </svg>
          </div>
          <div>
            <div class="text-2xl font-bold text-neutral-900 dark:text-neutral-100">{{ enKeyCount }}</div>
            <div class="text-sm text-neutral-500 dark:text-neutral-400">en-US {{ t('i18n.keys') }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 翻译表格 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden animate-slide-up">
      <!-- 搜索和筛选栏 -->
      <div class="p-5 border-b border-neutral-100 dark:border-neutral-800">
        <div class="flex flex-wrap items-center gap-3">
          <!-- 搜索框 -->
          <div class="relative flex-1 min-w-[240px] max-w-md">
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 dark:text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="searchQuery"
              type="text"
              :placeholder="t('i18n.searchPlaceholder')"
              class="w-full pl-10 pr-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>

          <!-- 模块筛选 -->
          <select
            v-model="filterModule"
            class="px-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
          >
            <option value="">{{ t('common.filter') }}</option>
            <option v-for="mod in modules" :key="mod" :value="mod">{{ mod }}</option>
          </select>

          <!-- 仅显示缺失翻译 -->
          <label class="inline-flex items-center gap-2 px-3 py-2 rounded-xl text-sm cursor-pointer select-none transition-colors duration-200"
            :class="showMissingOnly
              ? 'bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-800'
              : 'bg-neutral-50 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400 border border-neutral-200 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-700'"
          >
            <input
              v-model="showMissingOnly"
              type="checkbox"
              class="sr-only"
            />
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            {{ t('i18n.missingTranslation') }}
          </label>
        </div>
      </div>

      <!-- 表格 -->
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/50">
              <th class="text-left px-5 py-3 font-semibold text-neutral-600 dark:text-neutral-400 whitespace-nowrap">
                {{ t('i18n.settings') }} Key
              </th>
              <th class="text-left px-5 py-3 font-semibold text-neutral-600 dark:text-neutral-400 whitespace-nowrap min-w-[200px]">
                zh-CN
              </th>
              <th class="text-left px-5 py-3 font-semibold text-neutral-600 dark:text-neutral-400 whitespace-nowrap min-w-[200px]">
                en-US
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in filteredTranslations"
              :key="item.key"
              class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors duration-150"
            >
              <!-- 翻译键 -->
              <td class="px-5 py-3">
                <code class="text-xs font-mono px-2 py-1 rounded-md bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300">
                  {{ item.key }}
                </code>
              </td>

              <!-- zh-CN 值 -->
              <td class="px-5 py-3">
                <span
                  :class="[
                    'text-sm',
                    item.zhMissing
                      ? 'text-red-500 dark:text-red-400 font-medium'
                      : 'text-neutral-700 dark:text-neutral-300'
                  ]"
                >
                  {{ item.zhMissing ? t('i18n.missingTranslation') : item.zhValue }}
                </span>
              </td>

              <!-- en-US 值 -->
              <td class="px-5 py-3">
                <span
                  :class="[
                    'text-sm',
                    item.enMissing
                      ? 'text-red-500 dark:text-red-400 font-medium'
                      : 'text-neutral-700 dark:text-neutral-300'
                  ]"
                >
                  {{ item.enMissing ? t('i18n.missingTranslation') : item.enValue }}
                </span>
              </td>
            </tr>

            <!-- 空状态 -->
            <tr v-if="filteredTranslations.length === 0">
              <td colspan="3" class="px-5 py-12 text-center">
                <div class="flex flex-col items-center gap-2">
                  <svg class="w-12 h-12 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span class="text-neutral-400 dark:text-neutral-500">{{ t('common.noData') }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 底部分页信息 -->
      <div v-if="filteredTranslations.length > 0" class="px-5 py-3 border-t border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
        <span class="text-xs text-neutral-500 dark:text-neutral-400">
          {{ t('i18n.total') }} {{ filteredTranslations.length }} / {{ totalKeys }} {{ t('i18n.keys') }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { PageHeader } from '@/components'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'

const { t, locale } = useI18n()

// 当前语言
const currentLocale = ref(locale.value)

// 搜索和筛选
const searchQuery = ref('')
const filterModule = ref('')
const showMissingOnly = ref(false)

// 递归提取所有翻译键值对
interface TranslationEntry {
  key: string
  zhValue: string
  enValue: string
  zhMissing: boolean
  enMissing: boolean
  module: string
}

function flattenMessages(
  obj: Record<string, unknown>,
  prefix: string = ''
): Record<string, string> {
  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (typeof value === 'string') {
      result[fullKey] = value
    } else if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      Object.assign(result, flattenMessages(value as Record<string, unknown>, fullKey))
    }
  }
  return result
}

// 提取两个语言的扁平化键值对
const zhFlat = computed(() => flattenMessages(zhCN))
const enFlat = computed(() => flattenMessages(enUS))

// 获取所有唯一的键
const allKeys = computed(() => {
  const keys = new Set([...Object.keys(zhFlat.value), ...Object.keys(enFlat.value)])
  return Array.from(keys).sort()
})

// 获取所有模块（顶级键）
const modules = computed(() => {
  const zhModules = Object.keys(zhCN)
  const enModules = Object.keys(enUS)
  return Array.from(new Set([...zhModules, ...enModules])).sort()
})

// 构建翻译条目列表
const translationEntries = computed<TranslationEntry[]>(() => {
  return allKeys.value.map((key) => {
    const zhValue = zhFlat.value[key] || ''
    const enValue = enFlat.value[key] || ''
    const module = key.split('.')[0]
    return {
      key,
      zhValue,
      enValue,
      zhMissing: !zhFlat.value.hasOwnProperty(key),
      enMissing: !enFlat.value.hasOwnProperty(key),
      module
    }
  })
})

// 过滤后的翻译条目
const filteredTranslations = computed(() => {
  let entries = translationEntries.value

  // 按模块筛选
  if (filterModule.value) {
    entries = entries.filter((e) => e.module === filterModule.value)
  }

  // 按搜索关键词筛选
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.trim().toLowerCase()
    entries = entries.filter(
      (e) =>
        e.key.toLowerCase().includes(query) ||
        e.zhValue.toLowerCase().includes(query) ||
        e.enValue.toLowerCase().includes(query)
    )
  }

  // 仅显示缺失翻译
  if (showMissingOnly.value) {
    entries = entries.filter((e) => e.zhMissing || e.enMissing)
  }

  return entries
})

// 统计
const totalKeys = computed(() => allKeys.value.length)
const zhKeyCount = computed(() => Object.keys(zhFlat.value).length)
const enKeyCount = computed(() => Object.keys(enFlat.value).length)

// 切换语言
function handleLocaleChange(e: RadioEvent) {
  const newLocale = e.target.value
  currentLocale.value = newLocale
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}

// 类型定义
interface RadioEvent {
  target: {
    value: string
  }
}

// 同步 locale 变化
watch(locale, (val) => {
  currentLocale.value = val
})
</script>

<style scoped>
.i18n-settings-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slideUp 0.4s ease-out;
}
</style>
