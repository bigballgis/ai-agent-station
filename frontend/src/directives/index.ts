/**
 * 全局指令注册入口
 */
import type { App } from 'vue'
import { vPermission } from './permission'
import { vRole } from './role'

export function setupDirectives(app: App): void {
  app.directive('permission', vPermission)
  app.directive('role', vRole)
}
