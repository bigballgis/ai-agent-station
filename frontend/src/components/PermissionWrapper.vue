<template>
  <component
    :is="wrapperTag"
    v-if="hasPermission"
    v-bind="wrapperAttrs"
  >
    <slot />
  </component>
  <component
    :is="wrapperTag"
    v-else-if="mode === 'disabled'"
    v-bind="wrapperAttrs"
    class="permission-disabled"
  >
    <slot />
  </component>
  <!-- mode === 'hidden' 时什么都不渲染 -->
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/user'

/**
 * PermissionWrapper 组件
 * 权限校验包装组件
 * 根据用户权限/角色决定是否显示或禁用内容
 * 用于按钮、菜单、区域等需要权限控制的场景
 */

interface Props {
  /** 需要的权限标识 */
  permission?: string | string[]
  /** 需要的角色 */
  role?: string | string[]
  /** 权限不足时的处理模式 */
  mode?: 'disabled' | 'hidden'
  /** 包装标签 */
  tag?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'hidden',
  tag: 'div',
})

const userStore = useUserStore()

// 包装标签
const wrapperTag = computed(() => props.tag)

// 包装属性
const wrapperAttrs = computed(() => ({}))

// 判断是否有权限
const hasPermission = computed(() => {
  const userInfo = userStore.userInfo
  const userPermissions = userInfo.permissions || []
  const userRoles = userInfo.roles || []

  // 检查权限
  if (props.permission) {
    const requiredPermissions = Array.isArray(props.permission)
      ? props.permission
      : [props.permission]

    const hasAllPermissions = requiredPermissions.every(p =>
      userPermissions.includes(p)
    )

    if (!hasAllPermissions) return false
  }

  // 检查角色
  if (props.role) {
    const requiredRoles = Array.isArray(props.role)
      ? props.role
      : [props.role]

    const hasAnyRole = requiredRoles.some(r =>
      userRoles.includes(r)
    )

    if (!hasAnyRole) return false
  }

  return true
})
</script>

<style scoped>
.permission-disabled {
  opacity: 0.5;
  pointer-events: none;
  cursor: not-allowed;
  filter: grayscale(0.3);
}
</style>
