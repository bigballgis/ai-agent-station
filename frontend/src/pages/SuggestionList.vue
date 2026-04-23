<template>
  <div class="p-6">
    <h1 class="text-2xl font-bold mb-6">{{ t('suggestion.management') }}</h1>
    
    <div class="mb-6 flex justify-between items-center">
      <div class="flex space-x-4">
        <input 
          v-model="searchKeyword" 
          type="text" 
          :placeholder="t('suggestion.searchPlaceholder')" 
          class="px-4 py-2 border rounded-md"
        />
        <select v-model="filterType" class="px-4 py-2 border rounded-md">
          <option value="">{{ t('suggestion.allTypes') }}</option>
          <option value="process">{{ t('suggestion.typeProcess') }}</option>
          <option value="performance">{{ t('suggestion.typePerformance') }}</option>
          <option value="quality">{{ t('suggestion.typeQuality') }}</option>
        </select>
        <select v-model="filterStatus" class="px-4 py-2 border rounded-md">
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
    
    <div class="bg-white rounded-lg shadow-md overflow-hidden">
      <table class="min-w-full">
        <thead class="bg-gray-100">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.titleCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.typeCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.priorityCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.statusCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.implementationCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.impactCol') }}</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('suggestion.actionsCol') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="suggestion in suggestions" :key="suggestion.id">
            <td class="px-6 py-4 whitespace-nowrap">{{ suggestion.title }}</td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="{
                'px-2 py-1 rounded-full text-xs': true,
                'bg-blue-100 text-blue-800': suggestion.suggestionType === 'process',
                'bg-green-100 text-green-800': suggestion.suggestionType === 'performance',
                'bg-purple-100 text-purple-800': suggestion.suggestionType === 'quality'
              }">
                {{ getTypeLabel(suggestion.suggestionType) }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span :class="{
                'px-2 py-1 rounded-full text-xs': true,
                'bg-red-100 text-red-800': suggestion.priority === 1,
                'bg-yellow-100 text-yellow-800': suggestion.priority === 2,
                'bg-gray-100 text-gray-800': suggestion.priority === 3
              }">
                {{ getPriorityLabel(suggestion.priority) }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">{{ suggestion.status }}</td>
            <td class="px-6 py-4 whitespace-nowrap">{{ suggestion.implementationStatus }}</td>
            <td class="px-6 py-4 whitespace-nowrap">{{ suggestion.expectedImpact }}</td>
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
    
    <!-- 生成建议弹窗 -->
    <div v-if="showGenerateModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96">
        <h2 class="text-xl font-bold mb-4">{{ t('suggestion.generateModalTitle') }}</h2>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">Agent ID</label>
          <input v-model="agentId" type="number" class="w-full px-4 py-2 border rounded-md" />
        </div>
        <div class="flex justify-end space-x-2">
          <button @click="showGenerateModal = false" class="px-4 py-2 border rounded-md">{{ t('common.cancel') }}</button>
          <button @click="generateSuggestions" class="px-4 py-2 bg-blue-500 text-white rounded-md">{{ t('suggestion.generateButtonAction') }}</button>
        </div>
      </div>
    </div>
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

onMounted(() => {
  loadSuggestions()
})

const loadSuggestions = async () => {
  try {
    const response = await suggestionApi.searchSuggestions({
      keyword: searchKeyword.value,
      suggestionType: filterType.value,
      status: filterStatus.value
    })
    suggestions.value = response.data.data.content
  } catch (error) {
    console.warn(t('suggestion.loadFailed'), error)
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

const viewSuggestion = (_suggestion: any) => {
  // 查看建议详情 - TODO: 实现详情页面跳转
}

const editSuggestion = (_suggestion: any) => {
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
