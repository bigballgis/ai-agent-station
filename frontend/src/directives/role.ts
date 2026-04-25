/**
 * v-role 指令
 * 根据用户角色控制元素的显示/隐藏
 *
 * 用法：
 *   v-role="'ADMIN'"                              - 用户拥有该角色时显示
 *   v-role="['ADMIN', 'TENANT_ADMIN']"            - 用户拥有任一角色时显示
 *   v-role.all="['ADMIN', 'TENANT_ADMIN']"        - 用户拥有所有角色时显示
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store/modules/user'

function checkRole(binding: DirectiveBinding): boolean {
  const userStore = useUserStore()
  const userRoles = userStore.userInfo?.roles || []

  const value = binding.value
  if (!value) return true

  const requiredRoles = Array.isArray(value) ? value : [value]
  if (requiredRoles.length === 0) return true

  // v-role.all 修饰符：需要拥有所有角色
  if (binding.modifiers?.all) {
    return requiredRoles.every(r => userRoles.includes(r))
  }

  // 默认：拥有任一角色即可
  return requiredRoles.some(r => userRoles.includes(r))
}

export const vRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkRole(binding)) {
      el.style.display = 'none'
      el.setAttribute('data-role-hidden', 'true')
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    if (!checkRole(binding)) {
      el.style.display = 'none'
      el.setAttribute('data-role-hidden', 'true')
    } else {
      el.style.display = ''
      el.removeAttribute('data-role-hidden')
    }
  },
}
