/**
 * v-permission 指令
 * 根据用户权限控制元素的显示/隐藏
 *
 * 用法：
 *   v-permission="'agent:create'"           - 用户拥有该权限时显示
 *   v-permission="['agent:create', 'agent:update']"  - 用户拥有任一权限时显示
 *   v-permission.all="['agent:create', 'agent:update']"  - 用户拥有所有权限时显示
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/modules/user'

function checkPermission(binding: DirectiveBinding): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.userInfo?.permissions || []

  const value = binding.value
  if (!value) return true

  const requiredPermissions = Array.isArray(value) ? value : [value]
  if (requiredPermissions.length === 0) return true

  // v-permission.all 修饰符：需要拥有所有权限
  if (binding.modifiers?.all) {
    return requiredPermissions.every(p => userPermissions.includes(p))
  }

  // 默认：拥有任一权限即可
  return requiredPermissions.some(p => userPermissions.includes(p))
}

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkPermission(binding)) {
      el.style.display = 'none'
      el.setAttribute('data-permission-hidden', 'true')
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkPermission(binding)) {
      el.style.display = 'none'
      el.setAttribute('data-permission-hidden', 'true')
    } else {
      el.style.display = ''
      el.removeAttribute('data-permission-hidden')
    }
  },
}
