import { ref, computed, watch, type Ref } from 'vue'

export interface UsePaginationOptions {
  /** Initial page number (default: 1) */
  initialPage?: number
  /** Initial page size (default: 10) */
  initialPageSize?: number
  /** Total item count (can be updated reactively) */
  total?: Ref<number> | number
  /** When these refs change, auto-reset to page 1 */
  resetTriggers?: Ref<unknown>[]
}

/**
 * usePagination composable
 *
 * Manages pagination state including page, pageSize, and total count.
 * Computes derived values like totalPages, hasPrev, hasNext.
 * Supports auto-reset to page 1 when filter dependencies change.
 *
 * @example
 * const { currentPage, pageSize, total, onPageChange, onPageSizeChange, reset } = usePagination({
 *   initialPageSize: 10,
 *   total: itemsTotal,
 *   resetTriggers: [searchQuery, statusFilter],
 * })
 */
export function usePagination(options: UsePaginationOptions = {}) {
  const {
    initialPage = 1,
    initialPageSize = 10,
    total: totalRef,
    resetTriggers = [],
  } = options

  const currentPage = ref(initialPage)
  const pageSize = ref(initialPageSize)
  const total = typeof totalRef === 'number'
    ? ref(totalRef)
    : (totalRef ?? ref(0))

  /** Total number of pages */
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

  /** Whether there is a previous page */
  const hasPrev = computed(() => currentPage.value > 1)

  /** Whether there is a next page */
  const hasNext = computed(() => currentPage.value < totalPages.value)

  /**
   * Navigate to a specific page
   */
  function onPageChange(page: number) {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  /**
   * Change page size and reset to page 1
   */
  function onPageSizeChange(size: number) {
    pageSize.value = size
    currentPage.value = 1
  }

  /**
   * Reset to page 1
   */
  function reset() {
    currentPage.value = 1
  }

  // Auto-reset to page 1 when any trigger ref changes
  for (const trigger of resetTriggers) {
    watch(trigger, () => {
      currentPage.value = 1
    })
  }

  /**
   * Helper to compute paginated slice from a filtered list
   */
  function paginatedSlice<T>(items: Ref<T[]>): Ref<T[]> {
    return computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      return items.value.slice(start, start + pageSize.value)
    })
  }

  return {
    currentPage,
    pageSize,
    total,
    totalPages,
    hasPrev,
    hasNext,
    onPageChange,
    onPageSizeChange,
    reset,
    paginatedSlice,
  }
}

export type UsePaginationReturn = ReturnType<typeof usePagination>
