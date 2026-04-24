import { createPinia } from 'pinia'
import { markStoresReady } from './utils'

const pinia = createPinia()

// Action logging middleware for development
if (import.meta.env.DEV) {
  pinia.use(({ store }) => {
    store.$onAction(({ name, after, onError }) => {
      const startTime = performance.now()
      after(() => {
        const duration = (performance.now() - startTime).toFixed(1)
        console.debug(`[Store:${store.$id}] Action "${name}" completed in ${duration}ms`)
      })
      onError((error) => {
        console.error(`[Store:${store.$id}] Action "${name}" failed:`, error)
      })
    })
  })
}

// Mark stores as ready after pinia is created
markStoresReady()

export default pinia

// Re-export store modules for convenience
export { useUserStore } from './modules/user'
export { useAgentStore } from './modules/agent'
export { usePermissionStore } from './modules/permission'
export { useWorkflowStore } from './modules/workflow'
export { useNotificationStore } from './modules/notification'
export { useDictStore } from './modules/dict'
export { useAppStore } from './modules/app'
