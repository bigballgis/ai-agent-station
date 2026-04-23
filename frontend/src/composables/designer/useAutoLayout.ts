/**
 * useAutoLayout - 自动布局 Composable
 *
 * 使用 BFS 拓扑分层算法自动排列节点位置，无需外部依赖。
 * 从 AgentCanvas.vue 中提取的自动布局逻辑。
 *
 * 功能：
 * - autoLayout(nodes, connections) 自动布局（直接修改节点位置）
 *   - BFS 拓扑分层
 *   - HORIZONTAL_GAP = 280（水平间距）
 *   - VERTICAL_GAP = 120（垂直间距）
 *   - 每层节点垂直居中
 *   - 处理断开连接的节点（放置在末尾）
 */

import type { CanvasNode, Connection } from './types'
import { useGraphValidation } from './useGraphValidation'
import { LAYOUT_HORIZONTAL_GAP, LAYOUT_VERTICAL_GAP, NODE_WIDTH, NODE_HEIGHT } from './constants'

/** 布局起始 X 坐标 */
const START_X = 80

/** 布局起始 Y 坐标 */
const START_Y = 80

export function useAutoLayout() {
  const { buildAdjacencyList } = useGraphValidation()

  /**
   * 自动布局
   *
   * 使用 BFS 拓扑分层算法重新排列所有节点的位置。
   * 会直接修改传入的 nodes 数组中各节点的 x, y 属性。
   *
   * 算法步骤：
   * 1. 使用拓扑排序确定节点层级
   * 2. 使用 BFS 从 start 节点开始分层
   * 3. 每层节点垂直居中排列
   * 4. 未连接的节点放置在最后
   *
   * @param nodes - 节点列表（会被直接修改）
   * @param connections - 连接列表
   */
  function autoLayout(nodes: CanvasNode[], connections: Connection[]): void {
    if (nodes.length === 0) return

    // ----------------------------------------------------------
    // 1. 构建邻接表和入度表
    // ----------------------------------------------------------
    const adj = buildAdjacencyList(nodes, connections)
    const inDegree = new Map<string, number>()

    for (const node of nodes) {
      inDegree.set(node.id, 0)
    }

    for (const conn of connections) {
      const degree = inDegree.get(conn.targetId) ?? 0
      inDegree.set(conn.targetId, degree + 1)
    }

    // ----------------------------------------------------------
    // 2. BFS 分层
    // ----------------------------------------------------------
    const layers: string[][] = []
    const visited = new Set<string>()
    let queue: string[] = []

    // 优先从 start 节点开始
    const startNodes = nodes.filter((n) => n.type === 'start')

    if (startNodes.length > 0) {
      queue.push(startNodes[0].id)
      visited.add(startNodes[0].id)
    } else {
      // 没有 start 节点时，从入度为 0 的节点开始
      for (const [nodeId, degree] of inDegree) {
        if (degree === 0) {
          queue.push(nodeId)
          visited.add(nodeId)
        }
      }
    }

    while (queue.length > 0) {
      layers.push([...queue])
      const nextQueue: string[] = []

      for (const nodeId of queue) {
        const successors = adj.get(nodeId)
        if (successors) {
          for (const successorId of successors) {
            if (!visited.has(successorId)) {
              visited.add(successorId)
              nextQueue.push(successorId)
            }
          }
        }
      }

      queue = nextQueue
    }

    // ----------------------------------------------------------
    // 3. 处理未访问的节点（断开连接的节点）
    // ----------------------------------------------------------
    const unvisitedNodes = nodes.filter((n) => !visited.has(n.id))
    if (unvisitedNodes.length > 0) {
      const unvisitedIds = unvisitedNodes.map((n) => n.id)
      layers.push(unvisitedIds)
    }

    // ----------------------------------------------------------
    // 4. 计算每层节点的位置
    // ----------------------------------------------------------
    const nodeMap = new Map<string, CanvasNode>()
    for (const node of nodes) {
      nodeMap.set(node.id, node)
    }

    for (let layerIndex = 0; layerIndex < layers.length; layerIndex++) {
      const layer = layers[layerIndex]
      const x = START_X + layerIndex * LAYOUT_HORIZONTAL_GAP

      // 计算该层总高度
      const layerHeight = (layer.length - 1) * LAYOUT_VERTICAL_GAP

      // 计算起始 Y（使该层垂直居中）
      const startY = START_Y + (layerHeight > 0 ? 0 : 0)

      for (let nodeIndex = 0; nodeIndex < layer.length; nodeIndex++) {
        const nodeId = layer[nodeIndex]
        const node = nodeMap.get(nodeId)
        if (node) {
          node.x = x
          node.y = startY + nodeIndex * LAYOUT_VERTICAL_GAP
        }
      }
    }

    // ----------------------------------------------------------
    // 5. 整体居中调整
    // ----------------------------------------------------------
    // 计算布局的边界框
    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity

    for (const node of nodes) {
      minX = Math.min(minX, node.x)
      minY = Math.min(minY, node.y)
      maxX = Math.max(maxX, node.x + NODE_WIDTH) // NODE_WIDTH
      maxY = Math.max(maxY, node.y + NODE_HEIGHT)
    }

    // 将布局居中到 (400, 300) 附近
    const centerX = (minX + maxX) / 2
    const centerY = (minY + maxY) / 2
    const targetCenterX = 500
    const targetCenterY = 300

    const offsetX = targetCenterX - centerX
    const offsetY = targetCenterY - centerY

    for (const node of nodes) {
      node.x = Math.max(20, node.x + offsetX)
      node.y = Math.max(20, node.y + offsetY)
    }
  }

  /**
   * 仅布局选中的子图
   *
   * 从指定节点开始，只布局可达的节点，不影响其他节点位置。
   *
   * @param startNodeId - 起始节点 ID
   * @param nodes - 节点列表（会被直接修改可达节点的位置）
   * @param connections - 连接列表
   */
  function layoutSubgraph(
    startNodeId: string,
    nodes: CanvasNode[],
    connections: Connection[],
  ): void {
    const adj = buildAdjacencyList(nodes, connections)
    const visited = new Set<string>()
    const queue: string[] = [startNodeId]
    visited.add(startNodeId)

    // BFS 收集可达节点
    while (queue.length > 0) {
      const nodeId = queue.shift()!
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

    // 过滤出可达节点的连接
    const subConnections = connections.filter(
      (c) => visited.has(c.sourceId) && visited.has(c.targetId),
    )

    // 过滤出可达节点
    const subNodes = nodes.filter((n) => visited.has(n.id))

    // 对子图进行布局
    autoLayout(subNodes, subConnections)
  }

  return {
    autoLayout,
    layoutSubgraph,
  }
}
