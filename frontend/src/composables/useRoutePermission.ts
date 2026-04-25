/**
 * useRoutePermission composable
 * 提供路由级别的权限检查功能
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import {
  hasAnyPermission,
  hasAllPermissions,
  hasAnyRole,
  hasAllRoles,
} from '@/router/guards'

/**
 * 检查用户是否可以访问指定路由
 * @param route - 路由记录或路由名称
 */
export function canAccessRoute(route: RouteRecordRaw): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.userInfo?.permissions || []
  const userRoles = userStore.userInfo?.roles || []
  const meta = route.meta ?? ({} as any)

  // 检查权限（满足任一即可）
  if (meta.requiresPermission) {
    const perms = Array.isArray(meta.requiresPermission) ? meta.requiresPermission : [meta.requiresPermission]
    if (!hasAnyPermission(userPermissions, perms)) {
      return false
    }
  }

  // 检查全部权限
  if (meta.requiresAllPermissions && meta.requiresAllPermissions.length > 0) {
    if (!hasAllPermissions(userPermissions, meta.requiresAllPermissions)) {
      return false
    }
  }

  // 检查角色（满足任一即可）
  if (meta.roles && meta.roles.length > 0) {
    if (!hasAnyRole(userRoles, meta.roles)) {
      return false
    }
  }

  // 检查全部角色
  if (meta.requiresAllRoles && meta.requiresAllRoles.length > 0) {
    if (!hasAllRoles(userRoles, meta.requiresAllRoles)) {
      return false
    }
  }

  return true
}

/**
 * useRoutePermission composable
 */
export function useRoutePermission() {
  const router = useRouter()
  const userStore = useUserStore()

  /**
   * 获取用户可访问的路由列表
   */
  function getAccessibleRoutes(): RouteRecordRaw[] {
    const allRoutes = router.getRoutes()
    return allRoutes.filter((route) => {
      // 跳过无名称的路由（如通配符）
      if (!route.name) return false
      // 跳过不需要认证的路由
      if (route.meta.requiresAuth === false) return false
      return canAccessRoute(route as unknown as RouteRecordRaw)
    }) as RouteRecordRaw[]
  }

  /**
   * 获取用户不可访问的路由列表
   */
  function getForbiddenRoutes(): RouteRecordRaw[] {
    const allRoutes = router.getRoutes()
    return allRoutes.filter((route) => {
      // 跳过无名称的路由
      if (!route.name) return false
      // 跳过不需要认证的路由
      if (route.meta.requiresAuth === false) return false
      return !canAccessRoute(route as unknown as RouteRecordRaw)
    }) as RouteRecordRaw[]
  }

  /**
   * 检查用户是否拥有指定权限
   */
  const hasPermission = computed(() => {
    return (permission: string | string[]): boolean => {
      const userPermissions = userStore.userInfo?.permissions || []
      const perms = Array.isArray(permission) ? permission : [permission]
      return hasAnyPermission(userPermissions, perms)
    }
  })

  /**
   * 检查用户是否拥有所有指定权限
   */
  const hasAllPerm = computed(() => {
    return (permissions: string[]): boolean => {
      const userPermissions = userStore.userInfo?.permissions || []
      return hasAllPermissions(userPermissions, permissions)
    }
  })

  /**
   * 检查用户是否拥有指定角色
   */
  const hasRole = computed(() => {
    return (role: string | string[]): boolean => {
      const userRoles = userStore.userInfo?.roles || []
      const roles = Array.isArray(role) ? role : [role]
      return hasAnyRole(userRoles, roles)
    }
  })

  return {
    canAccessRoute,
    getAccessibleRoutes,
    getForbiddenRoutes,
    hasPermission,
    hasAllPerm,
    hasRole,
  }
}
