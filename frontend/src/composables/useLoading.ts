import { ref, computed, type Ref } from 'vue'

/**
 * useLoading composable
 *
 * Provides reactive loading state management with support for
 * multiple concurrent loading states keyed by string identifiers.
 *
 * @example
 * // Simple single loading state
 * const { loading, setLoading, withLoading } = useLoading()
 *
 * // Multiple concurrent loading states
 * const { loading, isLoading, setLoading, withLoading } = useLoading()
 * await withLoading('fetchData', async () => { ... })
 * isLoading('fetchData') // => boolean
 */
export function useLoading() {
  const loadingStates = ref<Record<string, boolean>>({})

  /** Default loading state (no key) */
  const loading = computed({
    get: () => !!loadingStates.value[''],
    set: (val: boolean) => {
      loadingStates.value[''] = val
    },
  })

  /**
   * Check if a specific key is loading
   */
  function isLoading(key: string): boolean {
    return !!loadingStates.value[key]
  }

  /**
   * Set loading state for a specific key
   */
  function setLoading(key: string | undefined, value: boolean): void {
    const k = key || ''
    loadingStates.value[k] = value
  }

  /**
   * Wrap an async function with automatic loading state management.
   * Automatically resets loading to false on error.
   *
   * @param key - Optional key for concurrent loading states
   * @param fn - The async function to wrap
   * @returns The return value of the async function
   */
  async function withLoading<T>(
    keyOrFn: string | (() => Promise<T>),
    maybeFn?: () => Promise<T>,
  ): Promise<T> {
    let key: string | undefined
    let fn: () => Promise<T>

    if (typeof keyOrFn === 'function') {
      key = undefined
      fn = keyOrFn
    } else {
      key = keyOrFn
      fn = maybeFn!
    }

    const k = key || ''
    loadingStates.value[k] = true
    try {
      return await fn()
    } catch {
      // Auto-reset on error
      loadingStates.value[k] = false
      throw new Error('useLoading: withLoading error')
    } finally {
      loadingStates.value[k] = false
    }
  }

  return {
    loading,
    isLoading,
    setLoading,
    withLoading,
  }
}

export type UseLoadingReturn = ReturnType<typeof useLoading>
