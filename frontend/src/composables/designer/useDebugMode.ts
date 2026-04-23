import { ref, computed } from 'vue'
import type { CanvasNode } from './types'

export interface Breakpoint {
  nodeId: string
  enabled: boolean
}

export interface NodeDebugInfo {
  nodeId: string
  label: string
  status: 'pending' | 'running' | 'paused' | 'completed' | 'failed' | 'skipped'
  input: unknown
  output: unknown
  duration: number
  error?: string
}

export function useDebugMode() {
  const isDebugMode = ref(false)
  const isPaused = ref(false)
  const isStepping = ref(false)
  const breakpoints = ref<Map<string, boolean>>(new Map())
  const debugInfo = ref<Map<string, NodeDebugInfo>>(new Map())
  const currentDebugNodeId = ref<string | null>(null)
  const debugExecutionOrder = ref<string[]>([])
  const currentStepIndex = ref(-1)

  // Toggle breakpoint on a node
  function toggleBreakpoint(nodeId: string) {
    const current = breakpoints.value.get(nodeId) || false
    breakpoints.value.set(nodeId, !current)
  }

  function hasBreakpoint(nodeId: string): boolean {
    return breakpoints.value.get(nodeId) || false
  }

  function clearAllBreakpoints() {
    breakpoints.value.clear()
  }

  // Initialize debug info for all nodes
  function initDebugInfo(nodes: CanvasNode[]) {
    debugInfo.value.clear()
    nodes.forEach(node => {
      debugInfo.value.set(node.id, {
        nodeId: node.id,
        label: node.label,
        status: 'pending',
        input: null,
        output: null,
        duration: 0,
      })
    })
  }

  // Update node debug info
  function updateNodeDebugInfo(nodeId: string, updates: Partial<NodeDebugInfo>) {
    const existing = debugInfo.value.get(nodeId)
    if (existing) {
      debugInfo.value.set(nodeId, { ...existing, ...updates })
    }
  }

  // Get debug info for a node
  function getNodeDebugInfo(nodeId: string): NodeDebugInfo | undefined {
    return debugInfo.value.get(nodeId)
  }

  // Enter debug mode
  function enterDebugMode() {
    isDebugMode.value = true
    isPaused.value = false
    isStepping.value = false
    currentStepIndex.value = -1
    currentDebugNodeId.value = null
  }

  // Exit debug mode
  function exitDebugMode() {
    isDebugMode.value = false
    isPaused.value = false
    isStepping.value = false
    currentDebugNodeId.value = null
    debugInfo.value.clear()
  }

  // Step to next node
  function stepNext() {
    if (currentStepIndex.value < debugExecutionOrder.value.length - 1) {
      currentStepIndex.value++
      currentDebugNodeId.value = debugExecutionOrder.value[currentStepIndex.value]
      isPaused.value = true
    } else {
      // Execution complete
      isPaused.value = false
      currentDebugNodeId.value = null
    }
  }

  // Continue execution (run until next breakpoint or end)
  function continueExecution() {
    isPaused.value = false
    isStepping.value = true
  }

  // Stop debugging
  function stopDebug() {
    isDebugMode.value = false
    isPaused.value = false
    isStepping.value = false
    currentDebugNodeId.value = null
    currentStepIndex.value = -1
  }

  const canStepNext = computed(() =>
    isDebugMode.value && isPaused.value &&
    currentStepIndex.value < debugExecutionOrder.value.length - 1
  )

  const canContinue = computed(() => isDebugMode.value && isPaused.value)

  return {
    isDebugMode,
    isPaused,
    isStepping,
    breakpoints,
    debugInfo,
    currentDebugNodeId,
    debugExecutionOrder,
    currentStepIndex,
    canStepNext,
    canContinue,
    toggleBreakpoint,
    hasBreakpoint,
    clearAllBreakpoints,
    initDebugInfo,
    updateNodeDebugInfo,
    getNodeDebugInfo,
    enterDebugMode,
    exitDebugMode,
    stepNext,
    continueExecution,
    stopDebug,
  }
}
