import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Agent, PageRequest, PageResult, ApiResponse } from '@/types'
import request from '@/utils/request'

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
    agents.value.forEach((a) => map.set(a.id, a))
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

  // Actions
  async function fetchAgents(params?: PageRequest & typeof filters.value) {
    loading.value = true
    try {
      if (params) {
        filters.value = { ...filters.value, ...params }
      }
      const res = await request.get<ApiResponse<PageResult<Agent>>>(
        '/api/agents',
        { params: { ...filters.value, ...params } }
      )
      agents.value = res.data.data.content
      return res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchAgentById(id: string | number) {
    loading.value = true
    try {
      const res = await request.get<ApiResponse<Agent>>(`/api/agents/${id}`)
      currentAgent.value = res.data.data
      return res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createAgent(data: Partial<Agent>) {
    const res = await request.post<ApiResponse<Agent>>('/api/agents', data)
    agents.value.push(res.data.data)
    return res.data.data
  }

  async function updateAgent(id: string | number, data: Partial<Agent>) {
    const res = await request.put<ApiResponse<Agent>>(`/api/agents/${id}`, data)
    const index = agents.value.findIndex((a) => a.id === id)
    if (index !== -1) {
      agents.value[index] = res.data.data
    }
    if (currentAgent.value?.id === id) {
      currentAgent.value = res.data.data
    }
    return res.data.data
  }

  async function deleteAgent(id: string | number) {
    await request.delete(`/api/agents/${id}`)
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
