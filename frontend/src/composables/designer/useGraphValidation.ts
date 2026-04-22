/**
 * useGraphValidation - 图结构验证 Composable
 *
 * 验证工作流图的结构完整性，确保图可以被后端 GraphExecutor 正确执行。
 * 从 AgentCanvas.vue 和 GraphDefinition.java validate() 中提取的验证逻辑。
 *
 * 功能：
 * - validate(nodes, connections) 综合验证
 *   - 检查空图
 *   - 检查 start 节点存在且唯一
 *   - 检查 end 节点存在
 *   - 检查孤立节点（无连接）
 *   - 检查循环依赖（拓扑排序）
 *   - 检查 condition 节点的 true/false 分支
 *   - 检查 LLM 节点的 model 配置
 *   - 检查 HTTP 节点的 URL 配置
 *   - 检查 tool 节点的 toolId 配置
 * - topologicalSort(nodes, connections) 拓扑排序
 * - getExecutionOrder(startNodeId, nodes, connections) 获取执行顺序
 */

import type { CanvasNode, Connection, ValidationResult } from './types'

export function useGraphValidation() {
  // ============================================================
  // 邻接表构建
  // ============================================================

  /**
   * 构建邻接表
   *
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 邻接表 Map<nodeId, Set<targetNodeId>>
   */
  function buildAdjacencyList(
    nodes: CanvasNode[],
    connections: Connection[],
  ): Map<string, Set<string>> {
    const adj = new Map<string, Set<string>>()

    // 初始化所有节点的邻接表
    for (const node of nodes) {
      adj.set(node.id, new Set())
    }

    // 填充连接关系
    for (const conn of connections) {
      const targets = adj.get(conn.sourceId)
      if (targets) {
        targets.add(conn.targetId)
      }
    }

    return adj
  }

  /**
   * 构建反向邻接表（入度表）
   *
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 入度表 Map<nodeId, number>
   */
  function buildInDegreeMap(
    nodes: CanvasNode[],
    connections: Connection[],
  ): Map<string, number> {
    const inDegree = new Map<string, number>()

    for (const node of nodes) {
      inDegree.set(node.id, 0)
    }

    for (const conn of connections) {
      const degree = inDegree.get(conn.targetId) ?? 0
      inDegree.set(conn.targetId, degree + 1)
    }

    return inDegree
  }

  // ============================================================
  // 拓扑排序
  // ============================================================

  /**
   * 拓扑排序（Kahn 算法）
   *
   * 对有向无环图进行拓扑排序。如果图中存在环，返回 null。
   *
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 拓扑排序后的节点 ID 数组，如果存在环则返回 null
   */
  function topologicalSort(
    nodes: CanvasNode[],
    connections: Connection[],
  ): string[] | null {
    if (nodes.length === 0) return []

    const adj = buildAdjacencyList(nodes, connections)
    const inDegree = buildInDegreeMap(nodes, connections)

    // 找到所有入度为 0 的节点
    const queue: string[] = []
    for (const [nodeId, degree] of inDegree) {
      if (degree === 0) {
        queue.push(nodeId)
      }
    }

    const result: string[] = []
    let processedCount = 0

    while (queue.length > 0) {
      const nodeId = queue.shift()!
      result.push(nodeId)
      processedCount++

      // 减少后继节点的入度
      const successors = adj.get(nodeId)
      if (successors) {
        for (const successorId of successors) {
          const newDegree = (inDegree.get(successorId) ?? 1) - 1
          inDegree.set(successorId, newDegree)
          if (newDegree === 0) {
            queue.push(successorId)
          }
        }
      }
    }

    // 如果处理的节点数不等于总节点数，说明存在环
    if (processedCount !== nodes.length) {
      return null
    }

    return result
  }

  // ============================================================
  // 执行顺序
  // ============================================================

  /**
   * 获取从指定节点开始的执行顺序
   *
   * 基于 BFS 遍历，返回从 startNodeId 开始可达的所有节点的执行顺序。
   *
   * @param startNodeId - 起始节点 ID
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 可达节点的执行顺序数组
   */
  function getExecutionOrder(
    startNodeId: string,
    nodes: CanvasNode[],
    connections: Connection[],
  ): string[] {
    const adj = buildAdjacencyList(nodes, connections)
    const visited = new Set<string>()
    const order: string[] = []
    const queue: string[] = [startNodeId]

    visited.add(startNodeId)

    while (queue.length > 0) {
      const nodeId = queue.shift()!
      order.push(nodeId)

      const successors = adj.get(nodeId)
      if (successors) {
        for (const successorId of successors) {
          if (!visited.has(successorId)) {
            visited.add(successorId)
            queue.push(successorId)
          }
        }
      }
    }

    return order
  }

  // ============================================================
  // 综合验证
  // ============================================================

  /**
   * 验证图结构
   *
   * 执行完整的图结构验证，返回所有错误和警告。
   *
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 验证结果
   */
  function validate(nodes: CanvasNode[], connections: Connection[]): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []

    // ----------------------------------------------------------
    // 1. 检查空图
    // ----------------------------------------------------------
    if (nodes.length === 0) {
      errors.push('图中没有任何节点')
      return { valid: false, errors, warnings }
    }

    // ----------------------------------------------------------
    // 2. 检查 start 节点存在且唯一
    // ----------------------------------------------------------
    const startNodes = nodes.filter((n) => n.type === 'start')
    if (startNodes.length === 0) {
      errors.push('缺少开始节点（start），工作流必须有一个入口')
    } else if (startNodes.length > 1) {
      errors.push(`存在 ${startNodes.length} 个开始节点（start），工作流只能有一个入口`)
    }

    // ----------------------------------------------------------
    // 3. 检查 end 节点存在
    // ----------------------------------------------------------
    const endNodes = nodes.filter((n) => n.type === 'end')
    if (endNodes.length === 0) {
      errors.push('缺少结束节点（end），工作流必须有一个出口')
    }

    // ----------------------------------------------------------
    // 4. 检查孤立节点（无任何连接）
    // ----------------------------------------------------------
    const connectedNodeIds = new Set<string>()
    for (const conn of connections) {
      connectedNodeIds.add(conn.sourceId)
      connectedNodeIds.add(conn.targetId)
    }

    for (const node of nodes) {
      if (!connectedNodeIds.has(node.id)) {
        // start 节点没有输入连接是正常的
        if (node.type === 'start') continue
        // end 节点没有输出连接是正常的
        if (node.type === 'end') continue
        // variable 节点可能不需要输入连接
        if (node.type === 'variable') continue

        warnings.push(`节点「${node.label}」（${node.id}）没有连接，可能不会被执行`)
      }
    }

    // ----------------------------------------------------------
    // 5. 检查循环依赖（拓扑排序）
    // ----------------------------------------------------------
    const sortedIds = topologicalSort(nodes, connections)
    if (sortedIds === null) {
      errors.push('图中存在循环依赖，工作流无法执行')
    }

    // ----------------------------------------------------------
    // 6. 检查 condition 节点的 true/false 分支
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'condition') continue

      const outputConnections = connections.filter(
        (c) => c.sourceId === node.id,
      )

      const hasTrueBranch = outputConnections.some(
        (c) => c.sourcePort === 'true',
      )
      const hasFalseBranch = outputConnections.some(
        (c) => c.sourcePort === 'false',
      )

      if (!hasTrueBranch) {
        errors.push(
          `条件节点「${node.label}」缺少 True 分支连接`,
        )
      }
      if (!hasFalseBranch) {
        errors.push(
          `条件节点「${node.label}」缺少 False 分支连接`,
        )
      }
    }

    // ----------------------------------------------------------
    // 7. 检查 LLM 节点的 model 配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'llm') continue

      if (!node.config.model || node.config.model.trim() === '') {
        errors.push(
          `LLM 节点「${node.label}」未配置模型名称（model）`,
        )
      }

      if (!node.config.prompt || node.config.prompt.trim() === '') {
        warnings.push(
          `LLM 节点「${node.label}」未配置用户提示词（prompt）`,
        )
      }
    }

    // ----------------------------------------------------------
    // 8. 检查 HTTP 节点的 URL 配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'http') continue

      if (!node.config.url || node.config.url.trim() === '') {
        errors.push(
          `HTTP 节点「${node.label}」未配置请求 URL（url）`,
        )
      }
    }

    // ----------------------------------------------------------
    // 9. 检查 tool 节点的 toolId 配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'tool') continue

      if (!node.config.toolId || node.config.toolId.trim() === '') {
        errors.push(
          `工具节点「${node.label}」未配置工具 ID（toolId）`,
        )
      }
    }

    // ----------------------------------------------------------
    // 10. 检查 code 节点的代码配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'code') continue

      if (!node.config.code || node.config.code.trim() === '') {
        errors.push(
          `代码节点「${node.label}」未配置代码内容（code）`,
        )
      }
    }

    // ----------------------------------------------------------
    // 11. 检查 delay 节点的 seconds 配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'delay') continue

      if (
        node.config.seconds === undefined ||
        node.config.seconds === null ||
        node.config.seconds <= 0
      ) {
        errors.push(
          `延迟节点「${node.label}」的延迟秒数（seconds）必须大于 0`,
        )
      }
    }

    // ----------------------------------------------------------
    // 12. 检查 retriever 节点的 query 配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'retriever') continue

      if (!node.config.query || node.config.query.trim() === '') {
        warnings.push(
          `检索器节点「${node.label}」未配置检索查询（query）`,
        )
      }
    }

    // ----------------------------------------------------------
    // 13. 检查 memory 节点配置
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'memory') continue

      if (node.config.action === 'save') {
        if (!node.config.summary || node.config.summary.trim() === '') {
          warnings.push(
            `记忆节点「${node.label}」的操作为保存，但未配置摘要内容（summary）`,
          )
        }
      }

      if (node.config.action === 'load') {
        if (!node.config.query || node.config.query.trim() === '') {
          warnings.push(
            `记忆节点「${node.label}」的操作为加载，但未配置查询内容（query）`,
          )
        }
      }
    }

    // ----------------------------------------------------------
    // 14. 检查 exception 节点降级值
    // ----------------------------------------------------------
    for (const node of nodes) {
      if (node.type !== 'exception') continue

      if (
        node.config.action === 'fallback' &&
        (!node.config.fallbackValue || node.config.fallbackValue.trim() === '')
      ) {
        warnings.push(
          `异常处理节点「${node.label}」的降级处理未配置降级值（fallbackValue）`,
        )
      }
    }

    return {
      valid: errors.length === 0,
      errors,
      warnings,
    }
  }

  // ============================================================
  // 快速验证（仅检查关键错误）
  // ============================================================

  /**
   * 快速验证图结构（仅检查致命错误）
   *
   * 只检查空图、start 节点、end 节点和循环依赖。
   * 用于实时编辑时的轻量级验证。
   *
   * @param nodes - 节点列表
   * @param connections - 连接列表
   * @returns 验证结果
   */
  function quickValidate(
    nodes: CanvasNode[],
    connections: Connection[],
  ): ValidationResult {
    const errors: string[] = []
    const warnings: string[] = []

    if (nodes.length === 0) {
      errors.push('图中没有任何节点')
      return { valid: false, errors, warnings }
    }

    const startNodes = nodes.filter((n) => n.type === 'start')
    if (startNodes.length === 0) {
      errors.push('缺少开始节点（start）')
    } else if (startNodes.length > 1) {
      errors.push(`存在 ${startNodes.length} 个开始节点（start）`)
    }

    const endNodes = nodes.filter((n) => n.type === 'end')
    if (endNodes.length === 0) {
      errors.push('缺少结束节点（end）')
    }

    const sortedIds = topologicalSort(nodes, connections)
    if (sortedIds === null) {
      errors.push('图中存在循环依赖')
    }

    return {
      valid: errors.length === 0,
      errors,
      warnings,
    }
  }

  return {
    // 综合验证
    validate,
    quickValidate,

    // 拓扑排序
    topologicalSort,

    // 执行顺序
    getExecutionOrder,

    // 邻接表（供其他 composable 使用）
    buildAdjacencyList,
    buildInDegreeMap,
  }
}
