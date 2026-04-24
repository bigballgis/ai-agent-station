import { createPinia } from 'pinia'

const pinia = createPinia()

export default pinia

// Re-export store modules for convenience
export { useUserStore } from './modules/user'
export { useAgentStore } from './modules/agent'
export { usePermissionStore } from './modules/permission'
export { useWorkflowStore } from './modules/workflow'
export { useNotificationStore } from './modules/notification'
export { useDictStore } from './modules/dict'
export { useAppStore } from './modules/app'
