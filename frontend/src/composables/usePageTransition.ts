/**
 * usePageTransition composable
 * 提供页面过渡动画和导航加载状态
 */
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Router, RouteLocationNormalized } from 'vue-router'

/** 导航状态 */
const isNavigating = ref(false)
/** 过渡方向 */
const transitionDirection = ref<'forward' | 'back' | 'none'>('none')
/** 上一个路由路径 */
let previousPath = ''

/**
 * 根据路由路径深度比较确定过渡方向
 */
function getDirection(from: string, to: string): 'forward' | 'back' | 'none' {
  const fromDepth = from.split('/').filter(Boolean).length
  const toDepth = to.split('/').filter(Boolean).length

  if (toDepth > fromDepth) return 'forward'
  if (toDepth < fromDepth) return 'back'
  return 'none'
}

/**
 * usePageTransition composable
 */
export function usePageTransition(router?: Router) {
  const routerInstance = router || useRouter()

  /**
   * 根据过渡方向计算过渡名称
   */
  const transitionName = computed(() => {
    switch (transitionDirection.value) {
      case 'forward':
        return 'page-slide-left'
      case 'back':
        return 'page-slide-right'
      default:
        return 'page-fade'
    }
  })

  /**
   * 开始导航
   */
  function startNavigation(): void {
    isNavigating.value = true
  }

  /**
   * 结束导航
   */
  function endNavigation(): void {
    // 延迟结束以等待过渡完成
    setTimeout(() => {
      isNavigating.value = false
    }, 100)
  }

  // 监听路由变化
  const removeBeforeEach = routerInstance.beforeEach((to: RouteLocationNormalized) => {
    if (previousPath) {
      transitionDirection.value = getDirection(previousPath, to.path)
    }
    startNavigation()
  })

  const removeAfterEach = routerInstance.afterEach((to: RouteLocationNormalized) => {
    previousPath = to.path
    endNavigation()
  })

  onUnmounted(() => {
    removeBeforeEach()
    removeAfterEach()
  })

  return {
    transitionName,
    isNavigating,
    startNavigation,
    endNavigation,
  }
}
