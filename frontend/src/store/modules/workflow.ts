import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowNodeLog,
  PageRequest,
  PageResult,
} from '@/types'
import request from '@/utils/request'

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

  // Actions
  async function fetchDefinitions(params?: PageRequest) {
    loading.value = true
    try {
      const res = await request.get<PageResult<WorkflowDefinition>>(
        '/v1/workflows/definitions',
        { params }
      )
      definitions.value = res.data.records
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchInstances(params?: PageRequest & { definitionId?: number }) {
    loading.value = true
    try {
      const res = await request.get<PageResult<WorkflowInstance>>(
        '/v1/workflows/instances',
        { params }
      )
      instances.value = res.data.records
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchInstanceById(id: number) {
    loading.value = true
    try {
      const res = await request.get<WorkflowInstance>(
        `/v1/workflows/instances/${id}`
      )
      currentInstance.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function startWorkflow(
    definitionId: number,
    variables?: Record<string, any>
  ) {
    const res = await request.post<WorkflowInstance>(
      '/v1/workflows/instances/start',
      { definitionId, variables }
    )
    instances.value.unshift(res.data)
    return res.data
  }

  async function approveNode(
    instanceId: number,
    nodeId: string,
    approved: boolean,
    comment?: string
  ) {
    const res = await request.post<WorkflowNodeLog>(
      `/v1/workflows/instances/${instanceId}/nodes/${nodeId}/approve`,
      { approved, comment }
    )
    return res.data
  }

  async function fetchNodeLogs(instanceId: number) {
    const res = await request.get<WorkflowNodeLog[]>(
      `/v1/workflows/instances/${instanceId}/logs`
    )
    nodeLogs.value = res.data || []
    return res.data
  }

  async function cancelInstance(instanceId: number) {
    await request.post(`/v1/workflows/instances/${instanceId}/cancel`)
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
