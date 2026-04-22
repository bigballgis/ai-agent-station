import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DictType, DictItem, ApiResponse } from '@/types'
import request from '@/utils/request'

export const useDictStore = defineStore('dict', () => {
  // State
  const dictTypes = ref<DictType[]>([])
  const dictItems = ref<Map<string, DictItem[]>>(new Map())
  const loaded = ref(false)
  const loading = ref(false)

  // Getters
  const dictItemsByType = computed(() => {
    return (dictType: string): DictItem[] => dictItems.value.get(dictType) || []
  })

  const dictLabel = computed(() => {
    return (dictType: string, dictValue: string): string => {
      const items = dictItems.value.get(dictType) || []
      const item = items.find((i) => i.dictValue === dictValue)
      return item?.dictLabel || dictValue
    }
  })

  // Actions
  async function fetchDictTypes() {
    const res = await request.get<ApiResponse<DictType[]>>('/api/dict/types')
    dictTypes.value = res.data.data || []
    return res.data.data
  }

  async function fetchDictItems(dictType: string) {
    if (dictItems.value.has(dictType)) {
      return dictItems.value.get(dictType)!
    }

    const res = await request.get<ApiResponse<DictItem[]>>(
      `/api/dict/items/${dictType}`
    )
    const items = res.data.data || []
    dictItems.value.set(dictType, items)
    return items
  }

  async function getDictItemsByType(dictType: string): Promise<DictItem[]> {
    return fetchDictItems(dictType)
  }

  async function loadAllDicts() {
    if (loaded.value) return
    loading.value = true
    try {
      const types = await fetchDictTypes()
      await Promise.all(types.map((t) => fetchDictItems(t.dictType)))
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  function clearCache() {
    dictItems.value.clear()
    loaded.value = false
  }

  function removeDictType(dictType: string) {
    dictItems.value.delete(dictType)
  }

  return {
    // State
    dictTypes,
    dictItems,
    loaded,
    loading,
    // Getters
    dictItemsByType,
    dictLabel,
    // Actions
    fetchDictTypes,
    fetchDictItems,
    getDictItemsByType,
    loadAllDicts,
    clearCache,
    removeDictType,
  }
})
