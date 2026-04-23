import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PageRequest, PageResult } from '@/types'
import { agentApi, type Agent } from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  // State
  const agents = ref<Agent[]>([])
  const currentAgent = ref<Agent | null>(null)
  const loading = ref(false)
  const filters = ref<{
    keyword?: string
    status?: string
    type?: string
    category?: string
  }>({})

  // Getters
  const activeAgents = computed(() =>
    agents.value.filter((a) => a.isActive !== false)
  )

  const agentById = computed(() => {
    const map = new Map<string | number, Agent>()
    agents.value.forEach((a) => map.set(a.id!, a))
    return (id: string | number) => map.get(id)
  })

  const filteredAgents = computed(() => {
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
  async function fetchAgents(params?: PageRequest & typeof filters.value) {
    loading.value = true
    try {
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
    } finally {
      loading.value = false
    }
  }

  async function fetchAgentById(id: string | number) {
    loading.value = true
    try {
      const res = await agentApi.getAgentById(id)
      currentAgent.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function createAgent(data: Partial<Agent>) {
    const res = await agentApi.createAgent(data as Record<string, unknown>)
    agents.value.push(res.data as Agent)
    return res.data
  }

  async function updateAgent(id: string | number, data: Partial<Agent>) {
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
  }

  async function deleteAgent(id: string | number) {
    await agentApi.deleteAgent(id)
    agents.value = agents.value.filter((a) => a.id !== id)
    if (currentAgent.value?.id === id) {
      currentAgent.value = null
    }
  }

  function setFilters(newFilters: typeof filters.value) {
    filters.value = { ...filters.value, ...newFilters }
  }

  function resetFilters() {
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
  }
})
