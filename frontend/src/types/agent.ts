export interface Agent {
  id: number | string
  name: string
  description?: string
  graphDefinition?: string
  config?: Record<string, any>
  status?: string
  type?: string
  category?: string
  isActive?: boolean
  isTemplate?: boolean
  creator?: string
  usageCount?: number
  rating?: number
  favorited?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface AgentVersion {
  id: number
  agentId: number
  versionNumber: number
  graphDefinition?: string
  config?: Record<string, any>
  comment?: string
  createdAt?: string
}

export interface AgentInvokeRequest {
  message: string
  sessionId?: string
  variables?: Record<string, any>
  stream?: boolean
}

export interface AgentInvokeResponse {
  response: string
  sessionId: string
  tokensUsed?: number
  duration?: number
}
