import request from '@/utils/request'

// ==================== Types ====================

export interface WorkflowNode {
  id: string
  type: 'START' | 'END' | 'APPROVAL' | 'CONDITION' | 'NOTIFY' | 'AGENT' | 'HTTP' | 'DELAY' | 'PARALLEL'
  name: string
  config?: Record<string, any>
  [key: string]: any
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  [key: string]: any
}

export interface WorkflowDefinition {
  id: number
  name: string
  description?: string
  version: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  nodes: { nodes: WorkflowNode[] } | null
  edges: { edges: WorkflowEdge[] } | null
  triggers: Record<string, any> | null
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
  variables: Record<string, any> | null
  input: Record<string, any> | null
  output: Record<string, any> | null
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
  input: Record<string, any> | null
  output: Record<string, any> | null
  error?: string
  startedAt?: string
  completedAt?: string
  duration?: number
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// ==================== API ====================

export const workflowApi = {
  // Definition APIs
  getDefinitions: (page = 0, size = 10, status?: string) => {
    const params: Record<string, any> = { page, size }
    if (status) params.status = status
    return request.get<ApiResponse<PageResult<WorkflowDefinition>>>('/workflows/definitions', { params })
  },

  getDefinition: (id: number) => {
    return request.get<ApiResponse<WorkflowDefinition>>(`/workflows/definitions/${id}`)
  },

  createDefinition: (data: {
    name: string
    description?: string
    nodes?: Record<string, any>
    edges?: Record<string, any>
    triggers?: Record<string, any>
  }) => {
    return request.post<ApiResponse<WorkflowDefinition>>('/workflows/definitions', data)
  },

  updateDefinition: (id: number, data: {
    name?: string
    description?: string
    nodes?: Record<string, any>
    edges?: Record<string, any>
    triggers?: Record<string, any>
  }) => {
    return request.put<ApiResponse<WorkflowDefinition>>(`/workflows/definitions/${id}`, data)
  },

  deleteDefinition: (id: number) => {
    return request.delete<ApiResponse<void>>(`/workflows/definitions/${id}`)
  },

  publishDefinition: (id: number) => {
    return request.post<ApiResponse<WorkflowDefinition>>(`/workflows/definitions/${id}/publish`)
  },

  // Instance APIs
  getInstances: (page = 0, size = 10, filters?: { status?: string; definitionId?: number }) => {
    const params: Record<string, any> = { page, size }
    if (filters?.status) params.status = filters.status
    if (filters?.definitionId) params.definitionId = filters.definitionId
    return request.get<ApiResponse<PageResult<WorkflowInstance>>>('/workflows/instances', { params })
  },

  getInstance: (id: number) => {
    return request.get<ApiResponse<WorkflowInstance>>(`/workflows/instances/${id}`)
  },

  startWorkflow: (definitionId: number, variables?: Record<string, any>) => {
    return request.post<ApiResponse<WorkflowInstance>>('/workflows/instances/start', {
      definitionId,
      variables
    })
  },

  getInstanceHistory: (id: number) => {
    return request.get<ApiResponse<WorkflowNodeLog[]>>(`/workflows/instances/${id}/history`)
  },

  cancelWorkflow: (id: number, reason?: string) => {
    return request.post<ApiResponse<WorkflowInstance>>(`/workflows/instances/${id}/cancel`, { reason })
  },

  approveNode: (instanceId: number, nodeId: string, comment?: string) => {
    return request.post<ApiResponse<WorkflowNodeLog>>(
      `/workflows/instances/${instanceId}/nodes/${nodeId}/approve`,
      { comment }
    )
  },

  rejectNode: (instanceId: number, nodeId: string, comment?: string) => {
    return request.post<ApiResponse<WorkflowNodeLog>>(
      `/workflows/instances/${instanceId}/nodes/${nodeId}/reject`,
      { comment }
    )
  }
}
