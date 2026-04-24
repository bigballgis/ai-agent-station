import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { workflowApi, type WorkflowDefinition, type WorkflowInstance, type WorkflowNodeLog } from '@/api/workflow'
import type { ApiResponse, PageResult } from '@/types/common'
import { logger } from '@/utils/logger'
import { withLoading, requireStoreReady } from '../utils'

export const useWorkflowStore = defineStore('workflow', () => {
  requireStoreReady('workflow')

  // State
  const definitions = ref<WorkflowDefinition[]>([])
  const instances = ref<WorkflowInstance[]>([])
  const currentInstance = ref<WorkflowInstance | null>(null)
  const nodeLogs = ref<WorkflowNodeLog[]>([])
  const loading = ref(false)

  // Getters
  const activeDefinitions = computed<WorkflowDefinition[]>(() =>
    definitions.value.filter((d) => d.status === 'PUBLISHED')
  )

  const runningInstances = computed<WorkflowInstance[]>(() =>
    instances.value.filter(
      (i) => i.status === 'RUNNING' || i.status === 'PENDING'
    )
  )

  // Actions - delegate API calls to api/workflow.ts
  // Note: The response interceptor returns response.data (ApiResponse<T>) at runtime,
  // but TypeScript still sees AxiosResponse<ApiResponse<T>>. We cast to ApiResponse<T>.
  async function fetchDefinitions(page = 0, size = 10, status?: string): Promise<PageResult<WorkflowDefinition> | undefined> {
    return withLoading(loading, async () => {
      const res = await workflowApi.getDefinitions(page, size, status) as unknown as ApiResponse<PageResult<WorkflowDefinition>>
      const data = res.data
      if (data && 'records' in data) {
        definitions.value = data.records
      }
      return data
    }, 'Fetch definitions')
  }

  async function fetchInstances(page = 0, size = 10, filters?: { status?: string; definitionId?: number }): Promise<PageResult<WorkflowInstance> | undefined> {
    return withLoading(loading, async () => {
      const res = await workflowApi.getInstances(page, size, filters) as unknown as ApiResponse<PageResult<WorkflowInstance>>
      const data = res.data
      if (data && 'records' in data) {
        instances.value = data.records
      }
      return data
    }, 'Fetch instances')
  }

  async function fetchInstanceById(id: number): Promise<WorkflowInstance> {
    return withLoading(loading, async () => {
      const res = await workflowApi.getInstance(id) as unknown as ApiResponse<WorkflowInstance>
      currentInstance.value = res.data
      return res.data
    }, 'Fetch instance by id')
  }

  async function startWorkflow(
    definitionId: number,
    variables?: Record<string, unknown>
  ): Promise<WorkflowInstance> {
    try {
      const res = await workflowApi.startWorkflow(definitionId, variables) as unknown as ApiResponse<WorkflowInstance>
      const instance = res.data
      instances.value.unshift(instance)
      return instance
    } catch (error) {
      logger.debug('Start workflow failed:', error)
      throw error
    }
  }

  async function approveNode(
    instanceId: number,
    nodeId: string,
    approved: boolean,
    comment?: string
  ): Promise<WorkflowNodeLog> {
    try {
      if (approved) {
        const res = await workflowApi.approveNode(instanceId, nodeId, comment) as unknown as ApiResponse<WorkflowNodeLog>
        return res.data
      } else {
        const res = await workflowApi.rejectNode(instanceId, nodeId, comment) as unknown as ApiResponse<WorkflowNodeLog>
        return res.data
      }
    } catch (error) {
      logger.debug('Approve node failed:', error)
      throw error
    }
  }

  async function fetchNodeLogs(instanceId: number): Promise<WorkflowNodeLog[]> {
    try {
      const res = await workflowApi.getInstanceHistory(instanceId) as unknown as ApiResponse<WorkflowNodeLog[]>
      nodeLogs.value = res.data || []
      return res.data
    } catch (error) {
      logger.debug('Fetch node logs failed:', error)
      return []
    }
  }

  async function cancelInstance(instanceId: number): Promise<void> {
    await workflowApi.cancelWorkflow(instanceId)
    const instance = instances.value.find((i) => i.id === instanceId)
    if (instance) {
      instance.status = 'CANCELLED'
    }
    if (currentInstance.value?.id === instanceId) {
      currentInstance.value.status = 'CANCELLED'
    }
  }

  function $reset(): void {
    definitions.value = []
    instances.value = []
    currentInstance.value = null
    nodeLogs.value = []
    loading.value = false
  }

  return {
    // State
    definitions,
    instances,
    currentInstance,
    nodeLogs,
    loading,
    // Getters
    activeDefinitions,
    runningInstances,
    // Actions
    fetchDefinitions,
    fetchInstances,
    fetchInstanceById,
    startWorkflow,
    approveNode,
    fetchNodeLogs,
    cancelInstance,
    $reset,
  }
})
