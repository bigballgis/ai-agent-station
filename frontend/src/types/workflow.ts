export interface WorkflowDefinition {
  id: number
  name: string
  description?: string
  version: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  nodes?: WorkflowNode[]
  edges?: WorkflowEdge[]
  triggers?: any
  tenantId?: number
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowNode {
  id: string
  name: string
  type: 'START' | 'END' | 'AGENT' | 'APPROVAL' | 'CONDITION' | 'NOTIFY' | 'HTTP' | 'DELAY' | 'PARALLEL'
  config?: Record<string, any>
  nextNodes?: string[]
  position?: { x: number; y: number }
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  condition?: Record<string, any>
}

export interface WorkflowInstance {
  id: number
  workflowDefinitionId: number
  workflowName: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'SUSPENDED'
  currentNodeId?: string
  variables?: Record<string, any>
  startedBy?: string
  startedAt?: string
  completedAt?: string
}

export interface WorkflowNodeLog {
  id: number
  instanceId: number
  nodeId: string
  nodeName: string
  nodeType: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
  input?: any
  output?: any
  error?: string
  startedAt?: string
  completedAt?: string
  duration?: number
}
