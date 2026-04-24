import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PageRequest, PageResult } from '@/types'
import { agentApi, type Agent } from '@/api/agent'
import { logger } from '@/utils/logger'
import { withLoading, requireStoreReady } from '../utils'

interface AgentFilters {
  keyword?: string
  status?: string
  type?: string
  category?: string
}

export const useAgentStore = defineStore('agent', () => {
  requireStoreReady('agent')

  // State
  const agents = ref<Agent[]>([])
  const currentAgent = ref<Agent | null>(null)
  const loading = ref(false)
  const filters = ref<AgentFilters>({})

  // Getters
  const activeAgents = computed<Agent[]>(() =>
    agents.value.filter((a) => a.isActive !== false)
  )

  const agentById = computed(() => {
    const map = new Map<string | number, Agent>()
    agents.value.forEach((a) => map.set(a.id!, a))
    return (id: string | number) => map.get(id)
  })

  const filteredAgents = computed<Agent[]>(() => {
    let result = agents.value
    const { keyword, status, type, category } = filters.value

    if (keyword) {
      const kw = keyword.toLowerCase()
      result = result.filter(
        (a) =>
          a.name.toLowerCase().includes(kw) ||
          a.description?.toLowerCase().includes(kw)
      )
    }
    if (status) {
      result = result.filter((a) => a.status === status)
    }
    if (type) {
      result = result.filter((a) => a.type === type)
    }
    if (category) {
      result = result.filter((a) => a.category === category)
    }

    return result
  })

  // Actions - delegate API calls to api/agent.ts
  async function fetchAgents(params?: PageRequest & AgentFilters): Promise<unknown> {
    return withLoading(loading, async () => {
      if (params) {
        filters.value = { ...filters.value, ...params }
      }
      const res = await agentApi.getAllAgents()
      const data = res.data
      // Handle both paginated and array responses
      if (Array.isArray(data)) {
        agents.value = data
      } else if (data && 'records' in data) {
        agents.value = (data as PageResult<Agent>).records
      } else {
        agents.value = []
      }
      return data
    }, 'Fetch agents')
  }

  async function fetchAgentById(id: string | number): Promise<Agent> {
    return withLoading(loading, async () => {
      const res = await agentApi.getAgentById(id)
      currentAgent.value = res.data
      return res.data
    }, 'Fetch agent by id')
  }

  async function createAgent(data: Partial<Agent>): Promise<Agent> {
    try {
      const res = await agentApi.createAgent(data as Record<string, unknown>)
      agents.value.push(res.data as Agent)
      return res.data
    } catch (error) {
      logger.debug('Create agent failed:', error)
      throw error
    }
  }

  async function updateAgent(id: string | number, data: Partial<Agent>): Promise<Agent> {
    try {
      const res = await agentApi.updateAgent(id, data as Record<string, unknown>)
      const updated = res.data as Agent
      const index = agents.value.findIndex((a) => a.id === id)
      if (index !== -1) {
        agents.value[index] = updated
      }
      if (currentAgent.value?.id === id) {
        currentAgent.value = updated
      }
      return updated
    } catch (error) {
      logger.debug('Update agent failed:', error)
      throw error
    }
  }

  async function deleteAgent(id: string | number): Promise<void> {
    await agentApi.deleteAgent(id)
    agents.value = agents.value.filter((a) => a.id !== id)
    if (currentAgent.value?.id === id) {
      currentAgent.value = null
    }
  }

  function setFilters(newFilters: AgentFilters): void {
    filters.value = { ...filters.value, ...newFilters }
  }

  function resetFilters(): void {
    filters.value = {}
  }

  function $reset(): void {
    agents.value = []
    currentAgent.value = null
    loading.value = false
    filters.value = {}
  }

  return {
    // State
    agents,
    currentAgent,
    loading,
    filters,
    // Getters
    activeAgents,
    agentById,
    filteredAgents,
    // Actions
    fetchAgents,
    fetchAgentById,
    createAgent,
    updateAgent,
    deleteAgent,
    setFilters,
    resetFilters,
    $reset,
  }
})
