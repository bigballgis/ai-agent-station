import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'

/**
 * usePagination composable 单元测试
 * 测试: page/pageSize/total, totalPages, nextPage/prevPage, reset, filter triggers, paginatedSlice
 */

describe('usePagination', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  async function getUsePagination(options?: Parameters<typeof import('@/composables/usePagination').usePagination>[0]) {
    const { usePagination } = await import('@/composables/usePagination')
    return usePagination(options)
  }

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('默认 page 为 1', async () => {
      const { currentPage } = await getUsePagination()
      expect(currentPage.value).toBe(1)
    })

    it('默认 pageSize 为 10', async () => {
      const { pageSize } = await getUsePagination()
      expect(pageSize.value).toBe(10)
    })

    it('默认 total 为 0', async () => {
      const { total } = await getUsePagination()
      expect(total.value).toBe(0)
    })

    it('支持自定义 initialPage', async () => {
      const { currentPage } = await getUsePagination({ initialPage: 3 })
      expect(currentPage.value).toBe(3)
    })

    it('支持自定义 initialPageSize', async () => {
      const { pageSize } = await getUsePagination({ initialPageSize: 20 })
      expect(pageSize.value).toBe(20)
    })

    it('支持传入 total 数字', async () => {
      const { total } = await getUsePagination({ total: 100 })
      expect(total.value).toBe(100)
    })

    it('支持传入 total Ref', async () => {
      const totalRef = ref(50)
      const { total } = await getUsePagination({ total: totalRef })
      expect(total.value).toBe(50)
      totalRef.value = 75
      expect(total.value).toBe(75)
    })
  })

  // ---------- totalPages ----------

  describe('totalPages', () => {
    it('total=0 时 totalPages 为 1', async () => {
      const { totalPages } = await getUsePagination({ total: 0 })
      expect(totalPages.value).toBe(1)
    })

    it('total=25, pageSize=10 时 totalPages 为 3', async () => {
      const { totalPages } = await getUsePagination({ total: 25, initialPageSize: 10 })
      expect(totalPages.value).toBe(3)
    })

    it('total=100, pageSize=10 时 totalPages 为 10', async () => {
      const { totalPages } = await getUsePagination({ total: 100 })
      expect(totalPages.value).toBe(10)
    })

    it('total=1, pageSize=10 时 totalPages 为 1', async () => {
      const { totalPages } = await getUsePagination({ total: 1 })
      expect(totalPages.value).toBe(1)
    })
  })

  // ---------- hasPrev / hasNext ----------

  describe('hasPrev / hasNext', () => {
    it('第 1 页时 hasPrev 为 false', async () => {
      const { hasPrev } = await getUsePagination({ total: 100 })
      expect(hasPrev.value).toBe(false)
    })

    it('第 2 页时 hasPrev 为 true', async () => {
      const { hasPrev, onPageChange } = await getUsePagination({ total: 100 })
      onPageChange(2)
      expect(hasPrev.value).toBe(true)
    })

    it('最后一页时 hasNext 为 false', async () => {
      const { hasNext, onPageChange } = await getUsePagination({ total: 20 })
      onPageChange(2)
      expect(hasNext.value).toBe(false)
    })

    it('非最后一页时 hasNext 为 true', async () => {
      const { hasNext } = await getUsePagination({ total: 100 })
      expect(hasNext.value).toBe(true)
    })
  })

  // ---------- onPageChange ----------

  describe('onPageChange', () => {
    it('可以导航到有效页码', async () => {
      const { currentPage, onPageChange } = await getUsePagination({ total: 100 })
      onPageChange(5)
      expect(currentPage.value).toBe(5)
    })

    it('无效页码（小于 1）不改变当前页', async () => {
      const { currentPage, onPageChange } = await getUsePagination({ total: 100 })
      onPageChange(0)
      expect(currentPage.value).toBe(1)
    })

    it('无效页码（大于 totalPages）不改变当前页', async () => {
      const { currentPage, onPageChange } = await getUsePagination({ total: 20 })
      onPageChange(999)
      expect(currentPage.value).toBe(1)
    })

    it('导航到第 1 页有效', async () => {
      const { currentPage, onPageChange } = await getUsePagination({ total: 100 })
      onPageChange(3)
      onPageChange(1)
      expect(currentPage.value).toBe(1)
    })
  })

  // ---------- onPageSizeChange ----------

  describe('onPageSizeChange', () => {
    it('修改 pageSize 并重置到第 1 页', async () => {
      const { currentPage, pageSize, onPageSizeChange, onPageChange } = await getUsePagination({ total: 100 })
      onPageChange(5)
      onPageSizeChange(25)
      expect(pageSize.value).toBe(25)
      expect(currentPage.value).toBe(1)
    })

    it('修改 pageSize 后 totalPages 正确更新', async () => {
      const { totalPages, onPageSizeChange } = await getUsePagination({ total: 100 })
      onPageSizeChange(50)
      expect(totalPages.value).toBe(2)
    })
  })

  // ---------- reset ----------

  describe('reset', () => {
    it('重置到第 1 页', async () => {
      const { currentPage, onPageChange, reset } = await getUsePagination({ total: 100 })
      onPageChange(5)
      reset()
      expect(currentPage.value).toBe(1)
    })

    it('重置后 hasPrev 为 false', async () => {
      const { hasPrev, onPageChange, reset } = await getUsePagination({ total: 100 })
      onPageChange(3)
      reset()
      expect(hasPrev.value).toBe(false)
    })
  })

  // ---------- resetTriggers ----------

  describe('resetTriggers', () => {
    it('当 trigger ref 变化时自动重置到第 1 页', async () => {
      const searchQuery = ref('')
      const { currentPage, onPageChange } = await getUsePagination({
        total: 100,
        resetTriggers: [searchQuery],
      })

      onPageChange(5)
      expect(currentPage.value).toBe(5)

      searchQuery.value = 'new query'
      await nextTick()
      expect(currentPage.value).toBe(1)
    })

    it('多个 trigger 中任一变化都触发重置', async () => {
      const filter1 = ref('a')
      const filter2 = ref('b')
      const { currentPage, onPageChange } = await getUsePagination({
        total: 100,
        resetTriggers: [filter1, filter2],
      })

      onPageChange(4)

      filter2.value = 'c'
      await nextTick()
      expect(currentPage.value).toBe(1)
    })
  })

  // ---------- paginatedSlice ----------

  describe('paginatedSlice', () => {
    it('返回当前页的数据切片', async () => {
      const items = ref([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
      const { paginatedSlice, onPageChange } = await getUsePagination({
        initialPageSize: 5,
        total: 12,
      })

      const page1 = paginatedSlice(items)
      expect(page1.value).toEqual([1, 2, 3, 4, 5])

      onPageChange(2)
      const page2 = paginatedSlice(items)
      expect(page2.value).toEqual([6, 7, 8, 9, 10])

      onPageChange(3)
      const page3 = paginatedSlice(items)
      expect(page3.value).toEqual([11, 12])
    })

    it('空数组返回空切片', async () => {
      const items = ref<number[]>([])
      const { paginatedSlice } = await getUsePagination({ total: 0 })
      expect(paginatedSlice(items).value).toEqual([])
    })
  })
})
