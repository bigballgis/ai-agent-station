import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DictType, DictItem } from '@/types'
import request from '@/utils/request'
import { logger } from '@/utils/logger'
import { createStoreCache, withLoading, requireStoreReady } from '../utils'

export const useDictStore = defineStore('dict', () => {
  requireStoreReady('dict')

  // State
  const dictTypes = ref<DictType[]>([])
  const dictItems = ref<Map<string, DictItem[]>>(new Map())
  const loaded = ref(false)
  const loading = ref(false)

  // Cache for dict items (stale-while-revalidate)
  const dictCache = createStoreCache<DictItem[]>({ staleTime: 10 * 60 * 1000 })

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
  async function fetchDictTypes(): Promise<DictType[]> {
    try {
      const res = await request.get<DictType[]>('/v1/dict-types')
      dictTypes.value = res.data || []
      return res.data
    } catch (error) {
      logger.debug('Fetch dict types failed:', error)
      return []
    }
  }

  async function fetchDictItems(dictType: string): Promise<DictItem[]> {
    if (dictItems.value.has(dictType)) {
      return dictItems.value.get(dictType)!
    }

    try {
      const res = await request.get<DictItem[]>(
        `/v1/dict-types/${dictType}/items`
      )
      const items = res.data || []
      dictItems.value.set(dictType, items)
      dictCache.set(dictType, items)
      return items
    } catch (error) {
      logger.debug('Fetch dict items failed:', error)
      return []
    }
  }

  async function getDictItemsByType(dictType: string): Promise<DictItem[]> {
    return fetchDictItems(dictType)
  }

  async function loadAllDicts(): Promise<void> {
    if (loaded.value) return
    await withLoading(loading, async () => {
      const types = await fetchDictTypes()
      await Promise.all(types.map((t) => fetchDictItems(t.dictType)))
      loaded.value = true
    }, 'Load all dicts')
  }

  function clearCache(): void {
    dictItems.value.clear()
    dictCache.clear()
    loaded.value = false
  }

  function removeDictType(dictType: string): void {
    dictItems.value.delete(dictType)
    dictCache.invalidate(dictType)
  }

  function $reset(): void {
    dictTypes.value = []
    dictItems.value.clear()
    dictCache.clear()
    loaded.value = false
    loading.value = false
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
    $reset,
  }
})
