import { reactive } from 'vue'
import type { CanvasNode } from './types'

/**
 * Flow State composable - shared mutable state across all nodes
 * Inspired by Flowise Agentflow V2's $flow.state
 *
 * Provides:
 * - A reactive key-value store for shared variables
 * - {{node_id.output}} variable reference resolution
 * - {{state.variableName}} flow state variable resolution
 * - Deep config object resolution
 */
export function useFlowState() {
  // Shared state key-value store
  const state = reactive<Record<string, any>>({})

  // Initialize state from start node's inputVariables
  function initializeFromStartNode(nodes: CanvasNode[]) {
    const startNode = nodes.find(n => n.type === 'start')
    if (startNode?.config?.inputVariables) {
      try {
        const vars = typeof startNode.config.inputVariables === 'string'
          ? JSON.parse(startNode.config.inputVariables)
          : startNode.config.inputVariables
        Object.entries(vars).forEach(([key, value]) => {
          state[key] = value
        })
      } catch { /* ignore parse errors */ }
    }
  }

  // Set a state variable
  function setState(key: string, value: any) {
    state[key] = value
  }

  // Get a state variable
  function getState(key: string): any {
    return state[key]
  }

  // Clear all state
  function clearState() {
    Object.keys(state).forEach(key => delete state[key])
  }

  /**
   * Resolve {{node_id.output}} references in a string
   * Supports: {{node_123.output}}, {{node_123.config.someField}}
   * Also supports: {{state.variableName}} for flow state variables
   */
  function resolveReferences(template: string, nodes: CanvasNode[], nodeOutputs: Record<string, any>): string {
    if (!template || typeof template !== 'string') return template

    const DANGEROUS_PROPS = ['__proto__', 'constructor', 'prototype', '__defineGetter__', '__defineSetter__', '__lookupGetter__', '__lookupSetter__']

    return template.replace(/\{\{([^}]+)\}\}/g, (match, expression) => {
      const trimmed = expression.trim()

      // 表达式长度限制
      if (trimmed.length > 256) {
        return match // 表达式过长，不替换
      }

      // Handle state.variable references
      if (trimmed.startsWith('state.')) {
        const varName = trimmed.substring(6)
        return state[varName] !== undefined ? String(state[varName]) : match
      }

      // Handle node_id.output references
      const parts = trimmed.split('.')
      // 原型链污染防护：检查危险属性
      if (parts.some((p: string) => DANGEROUS_PROPS.includes(p.trim()))) {
        return match // 不替换，保留原始模板
      }
      if (parts.length >= 2) {
        const nodeId = parts[0]
        const field = parts.slice(1).join('.')

        // Check node outputs (from execution results)
        if (nodeOutputs[nodeId]) {
          const value = field === 'output'
            ? nodeOutputs[nodeId]
            : nodeOutputs[nodeId]?.[field]
          if (value !== undefined) return String(value)
        }

        // Check node config (for referencing other node's config)
        const node = nodes.find(n => n.id === nodeId)
        if (node) {
          const value = field === 'label'
            ? node.label
            : node.config?.[field]
          if (value !== undefined) return String(value)
        }
      }

      return match // Return original if not resolved
    })
  }

  // Resolve references in an entire config object (deep)
  function resolveConfigReferences(config: Record<string, any>, nodes: CanvasNode[], nodeOutputs: Record<string, any>): Record<string, any> {
    const resolved: Record<string, any> = {}
    for (const [key, value] of Object.entries(config)) {
      if (typeof value === 'string') {
        resolved[key] = resolveReferences(value, nodes, nodeOutputs)
      } else if (Array.isArray(value)) {
        resolved[key] = value.map(item =>
          typeof item === 'string' ? resolveReferences(item, nodes, nodeOutputs) : item
        )
      } else if (typeof value === 'object' && value !== null) {
        resolved[key] = resolveConfigReferences(value, nodes, nodeOutputs)
      } else {
        resolved[key] = value
      }
    }
    return resolved
  }

  return {
    state,
    initializeFromStartNode,
    setState,
    getState,
    clearState,
    resolveReferences,
    resolveConfigReferences,
  }
}
