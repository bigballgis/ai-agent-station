/**
 * useExecution - Agent 执行逻辑 Composable
 *
 * 从 AgentDesigner.vue 中提取的所有执行相关逻辑：
 * - runAgent(): 通过 SSE 后端执行，失败时回退到模拟执行
 * - runAgentSimulated(): 模拟执行（无后端时使用）
 * - stopExecution(): 停止当前执行
 * - simulateNodeInput(): 模拟节点输入数据
 * - simulateNodeOutput(): 模拟节点输出数据
 * - waitForDebugResume(): 等待调试恢复（单步/继续）
 */

import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { CanvasNode, Connection, ConsoleLog } from './types'
import { useGraphValidation } from './useGraphValidation'
import { executeAgentStream } from '@/api/agent'

// Get execution order helper from graph validation
const { getExecutionOrder } = useGraphValidation()

export function useExecution(
  nodes: { value: CanvasNode[] },
  connections: { value: Connection[] },
  debug: {
    isDebugMode: { value: boolean }
    isPaused: { value: boolean }
    isStepping: { value: boolean }
    currentDebugNodeId: { value: string | null }
    debugExecutionOrder: { value: string[] }
    currentStepIndex: { value: number }
    initDebugInfo: (nodes: CanvasNode[]) => void
    updateNodeDebugInfo: (nodeId: string, updates: any) => void
    hasBreakpoint: (nodeId: string) => boolean
    stopDebug: () => void
  },
  addLog: (level: ConsoleLog['level'], message: string, withTimestamp?: boolean) => void,
  onBeforeRun?: () => void,
) {
  const isRunning = ref(false)
  const runningNodeIds = ref<Set<string>>(new Set())
  const completedNodeIds = ref<Set<string>>(new Set())
  const failedNodeIds = ref<Set<string>>(new Set())
  let executionCancel: { cancel: () => void } | null = null

  /**
   * Simulate node input data based on connected upstream nodes
   */
  function simulateNodeInput(node: CanvasNode, conns: Connection[], allNodes: CanvasNode[]): any {
    const input: any = {}
    for (const conn of conns) {
      if (conn.targetId === node.id) {
        const sourceNode = allNodes.find(n => n.id === conn.sourceId)
        if (sourceNode) {
          input[conn.targetPort] = {
            from: sourceNode.label,
            type: sourceNode.type,
            data: `模拟数据来自 ${sourceNode.label}`,
          }
        }
      }
    }
    return Object.keys(input).length > 0 ? input : { message: 'Hello World' }
  }

  /**
   * Simulate node output data
   */
  function simulateNodeOutput(node: CanvasNode): any {
    switch (node.type) {
      case 'llm':
        return { response: `模拟 LLM 响应来自 ${node.label}`, tokens: Math.floor(Math.random() * 500) + 100 }
      case 'condition':
        return { result: Math.random() > 0.5, branch: Math.random() > 0.5 ? 'true' : 'false' }
      case 'tool':
        return { result: `模拟工具执行结果`, success: true }
      case 'code':
        return { output: `模拟代码输出`, exitCode: 0 }
      case 'delay':
        return { waited: node.config.delay || '1s' }
      case 'notify':
        return { sent: true, channel: node.config.channel || 'default' }
      default:
        return { status: 'completed', label: node.label }
    }
  }

  /**
   * Wait for debug resume (step or continue) using a polling approach
   */
  function waitForDebugResume(): Promise<void> {
    return new Promise<void>((resolve) => {
      const check = () => {
        if (!debug.isPaused.value || !isRunning.value || !debug.isDebugMode.value) {
          resolve()
          return
        }
        requestAnimationFrame(check)
      }
      check()
    })
  }

  /**
   * Simulated execution fallback - used when backend SSE is unavailable
   */
  async function runAgentSimulated(t: (key: string) => string) {
    const startNode = nodes.value.find(n => n.type === 'start')
    if (!startNode) {
      addLog('warn', `${t('designer.messages.runFailed')}: missing start node`)
      return
    }

    addLog('info', `节点数量: ${nodes.value.length}, 连线数量: ${connections.value.length}`)

    const order = getExecutionOrder(startNode.id, nodes.value, connections.value)

    // Debug mode: initialize debug info and set execution order
    if (debug.isDebugMode.value) {
      debug.initDebugInfo(nodes.value)
      debug.debugExecutionOrder.value = order
      debug.currentStepIndex.value = -1
      debug.isStepping.value = false
      addLog('info', t('designer.debug.running'))
    }

    for (let i = 0; i < order.length; i++) {
      if (!isRunning.value) break // Check if stopped
      const nodeId = order[i]
      const node = nodes.value.find(n => n.id === nodeId)
      if (!node) continue

      // Debug mode: check for breakpoint or stepping
      if (debug.isDebugMode.value) {
        debug.currentStepIndex.value = i
        debug.currentDebugNodeId.value = nodeId
        debug.updateNodeDebugInfo(nodeId, {
          status: 'running',
          input: simulateNodeInput(node, connections.value, nodes.value),
        })

        // Simulate input data for the node
        const simulatedInput = simulateNodeInput(node, connections.value, nodes.value)
        debug.updateNodeDebugInfo(nodeId, { input: simulatedInput })

        const hasBp = debug.hasBreakpoint(nodeId)

        if (hasBp || debug.isStepping.value) {
          debug.isPaused.value = true
          debug.updateNodeDebugInfo(nodeId, { status: 'paused' })
          addLog('info', `${t('designer.debug.paused')}: ${node.label}`)

          // Wait for user to step or continue
          await waitForDebugResume()

          if (!isRunning.value || !debug.isDebugMode.value) break

          debug.isPaused.value = false
          debug.updateNodeDebugInfo(nodeId, { status: 'running' })
        }
      }

      runningNodeIds.value.add(nodeId)
      addLog('info', `${t('designer.messages.nodeRunning')}: ${node.label} (${node.type})`)

      const startTime = Date.now()
      await new Promise(resolve => setTimeout(resolve, 800 + Math.random() * 700))
      const duration = Date.now() - startTime

      if (!isRunning.value) break // Check again after delay

      runningNodeIds.value.delete(nodeId)

      if (Math.random() < 0.05) {
        failedNodeIds.value.add(nodeId)
        addLog('error', `${t('designer.messages.nodeFailed')}: ${node.label}`)
        if (debug.isDebugMode.value) {
          debug.updateNodeDebugInfo(nodeId, {
            status: 'failed',
            duration,
            output: { error: 'Simulated failure' },
            error: 'Simulated failure',
          })
        }
      } else {
        completedNodeIds.value.add(nodeId)
        addLog('success', `${t('designer.messages.nodeComplete')}: ${node.label}`)
        if (debug.isDebugMode.value) {
          const simulatedOutput = simulateNodeOutput(node)
          debug.updateNodeDebugInfo(nodeId, {
            status: 'completed',
            duration,
            output: simulatedOutput,
          })
        }
      }
    }

    if (!isRunning.value) {
      if (debug.isDebugMode.value) {
        debug.stopDebug()
      }
      return
    }

    const endNode = nodes.value.find(n => n.type === 'end')
    if (endNode && completedNodeIds.value.has(endNode.id)) {
      addLog('success', t('designer.messages.runComplete'))
      message.success(t('designer.messages.runComplete'))
    } else if (failedNodeIds.value.size > 0) {
      addLog('error', t('designer.messages.runFailed'))
      message.error(t('designer.messages.runFailed'))
    } else {
      addLog('warn', t('designer.messages.runIncomplete'))
      message.warning(t('designer.messages.runIncomplete'))
    }

    if (debug.isDebugMode.value) {
      debug.currentDebugNodeId.value = null
      debug.isPaused.value = false
      debug.isStepping.value = false
    }

    isRunning.value = false
    executionCancel = null
  }

  /**
   * Run agent via real backend SSE, falling back to simulation on failure
   */
  function runAgent(routeId: string | undefined, t: (key: string) => string) {
    if (isRunning.value) return

    if (nodes.value.length === 0) {
      message.warning(t('designer.messages.noNodes'))
      return
    }

    const startNode = nodes.value.find(n => n.type === 'start')
    if (!startNode) {
      message.warning(t('designer.messages.noStartNode'))
      addLog('warn', `${t('designer.messages.runFailed')}: missing start node`)
      return
    }

    // Clear previous state
    runningNodeIds.value.clear()
    completedNodeIds.value.clear()
    failedNodeIds.value.clear()
    isRunning.value = true
    onBeforeRun?.()

    addLog('info', t('designer.messages.runStart'))

    const agentId = routeId
    const message_text = 'Hello' // Default message for agent execution

    // Try real SSE execution first
    if (agentId) {
      addLog('info', `连接后端 SSE 端点 (agentId: ${agentId})...`)

      executionCancel = executeAgentStream(
        agentId,
        message_text,
        // onMessage
        (event) => {
          switch (event.type) {
            case 'node_start': {
              const nodeId = event.data.nodeId
              const nodeType = event.data.nodeType
              runningNodeIds.value.add(nodeId)
              const node = nodes.value.find(n => n.id === nodeId)
              const label = node?.label || nodeId
              addLog('info', `${t('designer.messages.nodeRunning')}: ${label} (${nodeType})`)
              break
            }
            case 'node_end': {
              const nodeId = event.data.nodeId
              const status = event.data.status
              runningNodeIds.value.delete(nodeId)
              const node = nodes.value.find(n => n.id === nodeId)
              const label = node?.label || nodeId
              if (status === 'failed') {
                failedNodeIds.value.add(nodeId)
                addLog('error', `${t('designer.messages.nodeFailed')}: ${label} - ${event.data.error || ''}`)
              } else {
                completedNodeIds.value.add(nodeId)
                addLog('success', `${t('designer.messages.nodeComplete')}: ${label}`)
              }
              break
            }
            case 'token': {
              // LLM streaming token
              addLog('info', event.data.content || '', false)
              break
            }
            case 'done': {
              addLog('success', t('designer.messages.runComplete'))
              message.success(t('designer.messages.runComplete'))
              isRunning.value = false
              executionCancel = null
              break
            }
            case 'error': {
              addLog('error', `${t('designer.messages.runFailed')}: ${event.data.content || event.data.error || 'Unknown error'}`)
              message.error(t('designer.messages.runFailed'))
              isRunning.value = false
              executionCancel = null
              break
            }
          }
        },
        // onError
        (error) => {
          addLog('warn', `后端 SSE 连接失败: ${error.message}，切换到模拟执行...`)
          // Fall back to simulation
          isRunning.value = false
          executionCancel = null
          runAgentSimulated(t)
        },
        // onComplete
        () => {
          if (isRunning.value) {
            addLog('success', t('designer.messages.runComplete'))
            isRunning.value = false
            executionCancel = null
          }
        }
      )
    } else {
      // No agent ID - use simulation directly
      addLog('info', '未关联 Agent ID，使用模拟执行...')
      runAgentSimulated(t)
    }
  }

  /**
   * Stop the current execution
   */
  function stopExecution(t: (key: string) => string) {
    if (executionCancel) {
      executionCancel.cancel()
      executionCancel = null
    }
    isRunning.value = false
    runningNodeIds.value.clear()
    addLog('warn', t('designer.messages.executionStopped'))
  }

  return {
    isRunning,
    runningNodeIds,
    completedNodeIds,
    failedNodeIds,
    runAgent,
    runAgentSimulated,
    stopExecution,
  }
}
