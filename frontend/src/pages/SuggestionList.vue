<template>
  <div class="p-4 sm:p-6">
    <h1 class="text-2xl font-bold mb-6">{{ t('suggestion.management') }}</h1>
    
    <div class="mb-6 flex flex-wrap justify-between items-center gap-3">
      <div class="flex flex-wrap gap-3">
        <input 
          v-model="searchKeyword" 
          type="text" 
          :placeholder="t('suggestion.searchPlaceholder')" 
          :aria-label="t('suggestion.searchPlaceholder')"
          class="px-4 py-2 border rounded-md dark:bg-neutral-800 dark:border-neutral-700 dark:text-neutral-200"
        />
        <select v-model="filterType" :aria-label="t('suggestion.allTypes')" class="px-4 py-2 border rounded-md dark:bg-neutral-800 dark:border-neutral-700 dark:text-neutral-200">
          <option value="">{{ t('suggestion.allTypes') }}</option>
          <option value="process">{{ t('suggestion.typeProcess') }}</option>
          <option value="performance">{{ t('suggestion.typePerformance') }}</option>
          <option value="quality">{{ t('suggestion.typeQuality') }}</option>
        </select>
        <select v-model="filterStatus" :aria-label="t('suggestion.allStatus')" class="px-4 py-2 border rounded-md dark:bg-neutral-800 dark:border-neutral-700 dark:text-neutral-200">
          <option value="">{{ t('suggestion.allStatus') }}</option>
          <option value="pending">{{ t('suggestion.statusPending') }}</option>
          <option value="approved">{{ t('suggestion.statusApproved') }}</option>
          <option value="rejected">{{ t('suggestion.statusRejected') }}</option>
        </select>
      </div>
      <button @click="handleGenerateSuggestions" class="bg-blue-500 text-white px-4 py-2 rounded-md hover:bg-blue-600">
        {{ t('suggestion.generateButton') }}
      </button>
    </div>
    
    <div class="bg-white dark:bg-neutral-900 rounded-lg shadow-md overflow-hidden">
      <!-- 加载状态 -->
      <div v-if="loading" class="p-6 space-y-4">
        <div v-for="i in 5" :key="i" class="flex items-center gap-4 animate-pulse">
          <div class="h-4 w-32 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-20 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-16 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-20 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-24 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-24 rounded bg-neutral-200 dark:bg-neutral-700"></div>
          <div class="h-4 w-28 rounded bg-neutral-200 dark:bg-neutral-700"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <div
        v-else-if="suggestions.length === 0"
        class="flex flex-col items-center justify-center py-16"
      >
        <div class="w-20 h-20 rounded-2xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center mb-5">
          <svg class="w-10 h-10 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
        </div>
        <h3 class="text-base font-semibold text-neutral-700 dark:text-neutral-300 mb-1">{{ t('suggestion.noSuggestions') }}</h3>
        <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('suggestion.noSuggestionsHint') }}</p>
      </div>

      <!-- 表格 -->
      <div v-else class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-neutral-100 dark:bg-neutral-800">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.titleCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.typeCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.priorityCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.statusCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.implementationCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.impactCol') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">{{ t('suggestion.actionsCol') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-neutral-200 dark:divide-neutral-700">
            <tr v-for="suggestion in suggestions" :key="suggestion.id">
              <td class="px-6 py-4 whitespace-nowrap text-neutral-900 dark:text-neutral-100">{{ suggestion.title }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="{
                  'px-2 py-1 rounded-full text-xs': true,
                  'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300': suggestion.suggestionType === 'process',
                  'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300': suggestion.suggestionType === 'performance',
                  'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300': suggestion.suggestionType === 'quality'
                }">
                  {{ getTypeLabel(suggestion.suggestionType) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="{
                  'px-2 py-1 rounded-full text-xs': true,
                  'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300': suggestion.priority === 1,
                  'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300': suggestion.priority === 2,
                  'bg-neutral-100 text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200': suggestion.priority === 3
                }">
                  {{ getPriorityLabel(suggestion.priority) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-neutral-900 dark:text-neutral-100">{{ suggestion.status }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-neutral-900 dark:text-neutral-100">{{ suggestion.implementationStatus }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-neutral-900 dark:text-neutral-100">{{ suggestion.expectedImpact }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex space-x-2">
                  <button @click="viewSuggestion(suggestion)" class="text-blue-600 hover:text-blue-800">{{ t('suggestion.viewButton') }}</button>
                  <button @click="editSuggestion(suggestion)" class="text-green-600 hover:text-green-800">{{ t('suggestion.editButton') }}</button>
                  <button @click="deleteSuggestion(suggestion.id)" class="text-red-600 hover:text-red-800">{{ t('suggestion.deleteButton') }}</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <!-- 生成建议弹窗 (使用 a-modal 替代自定义弹窗，支持焦点陷阱和键盘导航) -->
    <a-modal
      v-model:open="showGenerateModal"
      :title="t('suggestion.generateModalTitle')"
      :ok-text="t('suggestion.generateButtonAction')"
      :cancel-text="t('common.cancel')"
      :width="400"
      :mask-closable="false"
      @ok="generateSuggestions"
      @cancel="showGenerateModal = false"
    >
      <div class="mt-4">
        <label class="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">Agent ID</label>
        <a-input-number v-model:value="agentId" :min="1" class="w-full" />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import * as suggestionApi from '../api/suggestion'

const { t } = useI18n()
const suggestions = ref<any[]>([])
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref('')
const showGenerateModal = ref(false)
const agentId = ref(1)
const loading = ref(false)

onMounted(() => {
  loadSuggestions()
})

const loadSuggestions = async () => {
  loading.value = true
  try {
    const response = await suggestionApi.searchSuggestions({
      keyword: searchKeyword.value,
      suggestionType: filterType.value,
      status: filterStatus.value
    })
    suggestions.value = response.data.data.records
  } catch (error) {
    console.warn(t('suggestion.loadFailed'), error)
  } finally {
    loading.value = false
  }
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    'process': t('suggestion.typeProcess'),
    'performance': t('suggestion.typePerformance'),
    'quality': t('suggestion.typeQuality')
  }
  return labels[type] || type
}

const getPriorityLabel = (priority: number) => {
  const labels: Record<number, string> = {
    1: t('suggestion.priorityHigh'),
    2: t('suggestion.priorityMedium'),
    3: t('suggestion.priorityLow')
  }
  return labels[priority] || priority
}

interface SuggestionItem {
  id: number
  [key: string]: unknown
}

const viewSuggestion = (_suggestion: SuggestionItem) => {
  // 查看建议详情 - TODO: 实现详情页面跳转
}

const editSuggestion = (_suggestion: SuggestionItem) => {
  // 编辑建议 - TODO: 实现编辑页面跳转
}

const deleteSuggestion = async (id: number) => {
  if (confirm(t('suggestion.deleteConfirm'))) {
    try {
      await suggestionApi.deleteSuggestion(id)
      loadSuggestions()
    } catch (error) {
      console.warn(t('suggestion.deleteFailed'), error)
    }
  }
}

const handleGenerateSuggestions = () => {
  showGenerateModal.value = true
}

const generateSuggestions = async () => {
  try {
    await suggestionApi.generateSuggestions(agentId.value)
    showGenerateModal.value = false
    loadSuggestions()
  } catch (error) {
    console.warn(t('suggestion.generateFailed'), error)
  }
}
</script>
