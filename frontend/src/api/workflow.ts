import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'

// ==================== Types ====================

export interface WorkflowNode {
  id: string
  type: 'START' | 'END' | 'APPROVAL' | 'CONDITION' | 'NOTIFY' | 'AGENT' | 'HTTP' | 'DELAY' | 'PARALLEL'
  name: string
  config?: Record<string, unknown>
  [key: string]: unknown
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  [key: string]: unknown
}

export interface WorkflowDefinition {
  id: number
  name: string
  description?: string
  version: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  nodes: { nodes: WorkflowNode[] } | null
  edges: { edges: WorkflowEdge[] } | null
  triggers: Record<string, unknown> | null
  tenantId: number
  createdAt: string
  updatedAt: string
}

export interface WorkflowInstance {
  id: number
  workflowDefinitionId: number
  workflowName: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'SUSPENDED'
  currentNodeId?: string
  currentStep?: number
  variables: Record<string, unknown> | null
  input: Record<string, unknown> | null
  output: Record<string, unknown> | null
  startedBy?: number
  startedAt?: string
  completedAt?: string
  error?: string
  tenantId: number
}

export interface WorkflowNodeLog {
  id: number
  instanceId: number
  nodeId: string
  nodeName?: string
  nodeType: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
  input: Record<string, unknown> | null
  output: Record<string, unknown> | null
  error?: string
  startedAt?: string
  completedAt?: string
  duration?: number
}

// ==================== API ====================

export const workflowApi = {
  // Definition APIs
  getDefinitions: (page = 0, size = 10, status?: string) => {
    const params: Record<string, unknown> = { page, size }
    if (status) params.status = status
    return request.get<ApiResponse<PageResult<WorkflowDefinition>>>('/v1/workflows/definitions', { params })
  },

  getDefinition: (id: number) => {
    return request.get<ApiResponse<WorkflowDefinition>>(`/v1/workflows/definitions/${id}`)
  },

  createDefinition: (data: {
    name: string
    description?: string
    nodes?: Record<string, unknown>
    edges?: Record<string, unknown>
    triggers?: Record<string, unknown>
  }) => {
    return request.post<ApiResponse<WorkflowDefinition>>('/v1/workflows/definitions', data)
  },

  updateDefinition: (id: number, data: {
    name?: string
    description?: string
    nodes?: Record<string, unknown>
    edges?: Record<string, unknown>
    triggers?: Record<string, unknown>
  }) => {
    return request.put<ApiResponse<WorkflowDefinition>>(`/v1/workflows/definitions/${id}`, data)
  },

  deleteDefinition: (id: number) => {
    return request.delete<ApiResponse<void>>(`/v1/workflows/definitions/${id}`)
  },

  publishDefinition: (id: number) => {
    return request.post<ApiResponse<WorkflowDefinition>>(`/v1/workflows/definitions/${id}/publish`)
  },

  // Instance APIs
  getInstances: (page = 0, size = 10, filters?: { status?: string; definitionId?: number }) => {
    const params: Record<string, unknown> = { page, size }
    if (filters?.status) params.status = filters.status
    if (filters?.definitionId) params.definitionId = filters.definitionId
    return request.get<ApiResponse<PageResult<WorkflowInstance>>>('/v1/workflows/instances', { params })
  },

  getInstance: (id: number) => {
    return request.get<ApiResponse<WorkflowInstance>>(`/v1/workflows/instances/${id}`)
  },

  startWorkflow: (definitionId: number, variables?: Record<string, unknown>) => {
    return request.post<ApiResponse<WorkflowInstance>>('/v1/workflows/instances/start', {
      definitionId,
      variables
    })
  },

  getInstanceHistory: (id: number) => {
    return request.get<ApiResponse<WorkflowNodeLog[]>>(`/v1/workflows/instances/${id}/history`)
  },

  cancelWorkflow: (id: number, reason?: string) => {
    return request.post<ApiResponse<WorkflowInstance>>(`/v1/workflows/instances/${id}/cancel`, { reason })
  },

  approveNode: (instanceId: number, nodeId: string, comment?: string) => {
    return request.post<ApiResponse<WorkflowNodeLog>>(
      `/v1/workflows/instances/${instanceId}/nodes/${nodeId}/approve`,
      { comment }
    )
  },

  rejectNode: (instanceId: number, nodeId: string, comment?: string) => {
    return request.post<ApiResponse<WorkflowNodeLog>>(
      `/v1/workflows/instances/${instanceId}/nodes/${nodeId}/reject`,
      { comment }
    )
  },

  // Export/Import APIs
  exportDefinition: (id: number) => {
    return request.get(`/v1/workflows/definitions/${id}/export`, { responseType: 'blob' })
  },

  importDefinition: (data: Record<string, unknown>) => {
    return request.post<ApiResponse<WorkflowDefinition>>('/v1/workflows/definitions/import', data)
  },
}
