import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { workflowApi, type WorkflowDefinition, type WorkflowInstance, type WorkflowNodeLog } from '@/api/workflow'
import type { ApiResponse, PageResult } from '@/types/common'

export const useWorkflowStore = defineStore('workflow', () => {
  // State
  const definitions = ref<WorkflowDefinition[]>([])
  const instances = ref<WorkflowInstance[]>([])
  const currentInstance = ref<WorkflowInstance | null>(null)
  const nodeLogs = ref<WorkflowNodeLog[]>([])
  const loading = ref(false)

  // Getters
  const activeDefinitions = computed(() =>
    definitions.value.filter((d) => d.status === 'PUBLISHED')
  )

  const runningInstances = computed(() =>
    instances.value.filter(
      (i) => i.status === 'RUNNING' || i.status === 'PENDING'
    )
  )

  // Actions - delegate API calls to api/workflow.ts
  // Note: The response interceptor returns response.data (ApiResponse<T>) at runtime,
  // but TypeScript still sees AxiosResponse<ApiResponse<T>>. We cast to ApiResponse<T>.
  async function fetchDefinitions(page = 0, size = 10, status?: string) {
    loading.value = true
    try {
      const res = await workflowApi.getDefinitions(page, size, status) as unknown as ApiResponse<PageResult<WorkflowDefinition>>
      const data = res.data
      if (data && 'records' in data) {
        definitions.value = data.records
      }
      return data
    } finally {
      loading.value = false
    }
  }

  async function fetchInstances(page = 0, size = 10, filters?: { status?: string; definitionId?: number }) {
    loading.value = true
    try {
      const res = await workflowApi.getInstances(page, size, filters) as unknown as ApiResponse<PageResult<WorkflowInstance>>
      const data = res.data
      if (data && 'records' in data) {
        instances.value = data.records
      }
      return data
    } finally {
      loading.value = false
    }
  }

  async function fetchInstanceById(id: number) {
    loading.value = true
    try {
      const res = await workflowApi.getInstance(id) as unknown as ApiResponse<WorkflowInstance>
      currentInstance.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function startWorkflow(
    definitionId: number,
    variables?: Record<string, unknown>
  ) {
    const res = await workflowApi.startWorkflow(definitionId, variables) as unknown as ApiResponse<WorkflowInstance>
    const instance = res.data
    instances.value.unshift(instance)
    return instance
  }

  async function approveNode(
    instanceId: number,
    nodeId: string,
    approved: boolean,
    comment?: string
  ) {
    if (approved) {
      const res = await workflowApi.approveNode(instanceId, nodeId, comment) as unknown as ApiResponse<WorkflowNodeLog>
      return res.data
    } else {
      const res = await workflowApi.rejectNode(instanceId, nodeId, comment) as unknown as ApiResponse<WorkflowNodeLog>
      return res.data
    }
  }

  async function fetchNodeLogs(instanceId: number) {
    const res = await workflowApi.getInstanceHistory(instanceId) as unknown as ApiResponse<WorkflowNodeLog[]>
    nodeLogs.value = res.data || []
    return res.data
  }

  async function cancelInstance(instanceId: number) {
    await workflowApi.cancelWorkflow(instanceId)
    const instance = instances.value.find((i) => i.id === instanceId)
    if (instance) {
      instance.status = 'CANCELLED'
    }
    if (currentInstance.value?.id === instanceId) {
      currentInstance.value.status = 'CANCELLED'
    }
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
  }
})
