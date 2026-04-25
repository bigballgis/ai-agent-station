/**
 * 路由守卫增强模块
 * 提供权限检查、角色检查、数据预取、路由变化日志、token 过期处理等功能
 */
import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { logger } from '@/utils/logger'

/** 扩展路由 meta 类型 */
declare module 'vue-router' {
  interface RouteMeta {
    /** 是否需要认证，默认 true */
    requiresAuth?: boolean
    /** 页面标题 i18n key */
    title?: string
    /** 需要的角色列表（满足任一即可） */
    roles?: string[]
    /** 需要的权限标识（满足任一即可） */
    requiresPermission?: string | string[]
    /** 需要的权限标识（必须全部满足） */
    requiresAllPermissions?: string[]
    /** 需要的角色（必须全部满足） */
    requiresAllRoles?: string[]
    /** 页面过渡动画名称 */
    transition?: string
    /** 是否在路由进入时预取数据 */
    prefetch?: boolean
  }
}

/**
 * 检查 token 是否过期
 * @returns true 表示 token 已过期或无效
 */
export function isTokenExpired(token: string): boolean {
  if (!token) return true
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      return true
    }
    return false
  } catch {
    return true
  }
}

/**
 * 检查用户是否拥有指定权限（满足任一即可）
 */
export function hasAnyPermission(userPermissions: string[], required: string[]): boolean {
  if (required.length === 0) return true
  return required.some(p => userPermissions.includes(p))
}

/**
 * 检查用户是否拥有所有指定权限
 */
export function hasAllPermissions(userPermissions: string[], required: string[]): boolean {
  if (required.length === 0) return true
  return required.every(p => userPermissions.includes(p))
}

/**
 * 检查用户是否拥有指定角色（满足任一即可）
 */
export function hasAnyRole(userRoles: string[], required: string[]): boolean {
  if (required.length === 0) return true
  return required.some(r => userRoles.includes(r))
}

/**
 * 检查用户是否拥有所有指定角色
 */
export function hasAllRoles(userRoles: string[], required: string[]): boolean {
  if (required.length === 0) return true
  return required.every(r => userRoles.includes(r))
}

/**
 * 路由权限检查
 * 检查用户是否有权限访问目标路由
 * @returns true 表示允许访问，false 表示拒绝
 */
export function checkRoutePermission(to: RouteLocationNormalized): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.userInfo?.permissions || []
  const userRoles = userStore.userInfo?.roles || []

  // 检查 requiresPermission（满足任一即可）
  const requiredPermissions = to.meta.requiresPermission
  if (requiredPermissions) {
    const perms = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]
    if (!hasAnyPermission(userPermissions, perms)) {
      return false
    }
  }

  // 检查 requiresAllPermissions（必须全部满足）
  const allPermissions = to.meta.requiresAllPermissions
  if (allPermissions && allPermissions.length > 0) {
    if (!hasAllPermissions(userPermissions, allPermissions)) {
      return false
    }
  }

  // 检查 roles（满足任一即可，向后兼容）
  const roles = to.meta.roles
  if (roles && roles.length > 0) {
    if (!hasAnyRole(userRoles, roles)) {
      return false
    }
  }

  // 检查 requiresAllRoles（必须全部满足）
  const allRoles = to.meta.requiresAllRoles
  if (allRoles && allRoles.length > 0) {
    if (!hasAllRoles(userRoles, allRoles)) {
      return false
    }
  }

  return true
}

/**
 * 路由变化日志（仅开发模式）
 */
export function logRouteChange(
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  action: 'navigate' | 'redirect' | 'abort' = 'navigate'
): void {
  if (import.meta.env.DEV) {
    logger.debug(`[Route ${action}]`, from.fullPath, '->', to.fullPath)
    if (to.meta.requiresPermission) {
      logger.debug('[Route] requiresPermission:', to.meta.requiresPermission)
    }
    if (to.meta.roles) {
      logger.debug('[Route] roles:', to.meta.roles)
    }
  }
}

/**
 * 处理 token 过期：登出并重定向到登录页
 */
export function handleTokenExpired(to: RouteLocationNormalized): RouteLocationRaw {
  const userStore = useUserStore()
  logger.warn('[Route Guard] Token expired, redirecting to login')
  userStore.logout()
  return { path: '/login', query: { redirect: to.fullPath, expired: '1' } }
}

/**
 * 处理无权限访问：重定向到仪表板
 */
export function handleNoPermission(to: RouteLocationNormalized): RouteLocationRaw {
  logger.warn('[Route Guard] No permission for route:', to.fullPath)
  return { path: '/dashboard', query: { noPermission: '1' } }
}
