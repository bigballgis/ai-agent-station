/**
 * useConnections - 连接线 CRUD、路径计算和端口管理 Composable
 *
 * 管理节点间连接线的创建、删除、路径计算和临时连接状态。
 * 从 AgentDesigner.vue 中提取的连接管理逻辑。
 *
 * 重要：Connection 接口使用 sourceId/targetId 格式，与后端 GraphParser.java 保持一致。
 *
 * 功能：
 * - connections 响应式连接列表
 * - selectedConnectionId 选中连接
 * - generateConnectionId() 生成唯一连接 ID
 * - createConnection(sourceId, sourcePort, targetId, targetPort) 带验证的连接创建
 * - deleteConnection(id) 删除连接
 * - getPortPosition(node, portName, portType) 计算端口位置
 * - getConnectionPath(conn, nodes) 计算 SVG 贝塞尔曲线路径
 * - getTempConnectionPath(fromNode, fromPort, mouseX, mouseY) 计算临时连接路径
 * - isConnecting / connectingFrom 连接状态管理
 * - startConnecting / endConnecting 连接操作
 */

import { ref, computed } from 'vue'
import type { CanvasNode, Connection, ConnectingFrom, PortPosition } from './types'

/** 连接 ID 计数器 */
let connectionIdCounter = 0

/** 节点尺寸常量（与 useCanvas 保持一致） */
const NODE_WIDTH = 220

/** 端口尺寸常量 */
const PORT_START_Y = 36 // 第一个端口的 Y 偏移
const PORT_GAP = 24 // 端口间距

/** 贝塞尔曲线控制点偏移量 */
const BEZIER_OFFSET = 80

export function useConnections() {
  // ============================================================
  // 响应式状态
  // ============================================================

  /** 连接线列表 */
  const connections = ref<Connection[]>([])

  /** 当前选中连接的 ID */
  const selectedConnectionId = ref<string | null>(null)

  /** 是否正在创建连接 */
  const isConnecting = ref<boolean>(false)

  /** 连接起始信息 */
  const connectingFrom = ref<ConnectingFrom | null>(null)

  // ============================================================
  // 计算属性
  // ============================================================

  /**
   * 当前选中的连接
   */
  const selectedConnection = computed<Connection | null>(() => {
    if (!selectedConnectionId.value) return null
    return (
      connections.value.find((c) => c.id === selectedConnectionId.value) ?? null
    )
  })

  // ============================================================
  // 连接 ID 生成
  // ============================================================

  /**
   * 生成唯一的连接 ID
   *
   * @returns 唯一连接 ID
   */
  function generateConnectionId(): string {
    connectionIdCounter++
    return `conn_${Date.now()}_${connectionIdCounter}`
  }

  // ============================================================
  // 连接创建
  // ============================================================

  /**
   * 创建连接（带验证）
   *
   * 验证规则：
   * 1. 不允许自连接（sourceId === targetId）
   * 2. 不允许重复连接
   * 3. 除 condition 节点外，每个输入端口只允许一条连接（替换已有连接）
   *
   * @param sourceId - 源节点 ID
   * @param sourcePort - 源端口名称
   * @param targetId - 目标节点 ID
   * @param targetPort - 目标端口名称
   * @param nodes - 当前节点列表（用于判断 condition 节点）
   * @returns 新创建的连接，如果验证失败则返回 null
   */
  function createConnection(
    sourceId: string,
    sourcePort: string,
    targetId: string,
    targetPort: string,
    nodes: CanvasNode[],
  ): Connection | null {
    // 验证 1：不允许自连接
    if (sourceId === targetId) {
      console.warn('[useConnections] 不允许自连接')
      return null
    }

    // 验证 2：检查重复连接
    const isDuplicate = connections.value.some(
      (conn) =>
        conn.sourceId === sourceId &&
        conn.sourcePort === sourcePort &&
        conn.targetId === targetId &&
        conn.targetPort === targetPort,
    )
    if (isDuplicate) {
      console.warn('[useConnections] 连接已存在')
      return null
    }

    // 验证 3：检查目标端口是否已有连接
    // condition 节点允许 true/false 端口各有一条连接
    // merge 节点允许每个输入端口各有一条连接（多输入合并）
    // human_approval 节点只有一个输入端口，使用默认替换逻辑即可
    const targetNode = nodes.find((n) => n.id === targetId)
    const isConditionTarget = targetNode?.type === 'condition'
    const isMergeTarget = targetNode?.type === 'merge'
    const isSwitchTarget = targetNode?.type === 'switch'

    if (isMergeTarget) {
      // merge 节点：仅替换同一输入端口上的已有连接，允许不同端口各有连接
      const existingOnPort = connections.value.findIndex(
        (conn) => conn.targetId === targetId && conn.targetPort === targetPort,
      )
      if (existingOnPort !== -1) {
        connections.value.splice(existingOnPort, 1)
      }
    } else if (!isConditionTarget && !isSwitchTarget) {
      // 非 condition/merge/switch 节点：替换目标端口的已有连接
      const existingIndex = connections.value.findIndex(
        (conn) => conn.targetId === targetId && conn.targetPort === targetPort,
      )
      if (existingIndex !== -1) {
        connections.value.splice(existingIndex, 1)
      }
    }

    // 创建新连接
    const connection: Connection = {
      id: generateConnectionId(),
      sourceId,
      sourcePort,
      targetId,
      targetPort,
    }

    connections.value.push(connection)
    return connection
  }

  // ============================================================
  // 连接删除
  // ============================================================

  /**
   * 删除连接
   *
   * @param id - 连接 ID
   */
  function deleteConnection(id: string): void {
    const index = connections.value.findIndex((c) => c.id === id)
    if (index !== -1) {
      connections.value.splice(index, 1)
    }

    if (selectedConnectionId.value === id) {
      selectedConnectionId.value = null
    }
  }

  /**
   * 删除与指定节点相关的所有连接
   *
   * @param nodeId - 节点 ID
   */
  function deleteConnectionsByNodeId(nodeId: string): void {
    connections.value = connections.value.filter(
      (conn) => conn.sourceId !== nodeId && conn.targetId !== nodeId,
    )
  }

  // ============================================================
  // 端口位置计算
  // ============================================================

  /**
   * 计算端口在画布上的位置
   *
   * 输入端口在节点左侧，输出端口在节点右侧。
   *
   * @param node - 节点实例
   * @param portName - 端口名称
   * @param portType - 端口类型（'input' 或 'output'）
   * @returns 端口位置 {x, y}，如果端口不存在则返回 null
   */
  function getPortPosition(
    node: CanvasNode,
    portName: string,
    portType: 'input' | 'output',
  ): PortPosition | null {
    const ports = portType === 'input' ? node.inputs : node.outputs
    const port = ports.find((p) => p.name === portName)

    if (!port) return null

    const x =
      portType === 'input' ? node.x : node.x + NODE_WIDTH
    const y = node.y + PORT_START_Y + port.index * PORT_GAP

    return { x, y }
  }

  // ============================================================
  // 连接路径计算
  // ============================================================

  /**
   * 计算连接线的 SVG 贝塞尔曲线路径
   *
   * 从源节点的输出端口到目标节点的输入端口，使用三次贝塞尔曲线。
   *
   * @param conn - 连接定义
   * @param nodes - 节点列表
   * @returns SVG path 字符串，如果找不到端口则返回空字符串
   */
  function getConnectionPath(conn: Connection, nodes: CanvasNode[]): string {
    const sourceNode = nodes.find((n) => n.id === conn.sourceId)
    const targetNode = nodes.find((n) => n.id === conn.targetId)

    if (!sourceNode || !targetNode) return ''

    const sourcePos = getPortPosition(sourceNode, conn.sourcePort, 'output')
    const targetPos = getPortPosition(targetNode, conn.targetPort, 'input')

    if (!sourcePos || !targetPos) return ''

    return buildBezierPath(sourcePos.x, sourcePos.y, targetPos.x, targetPos.y)
  }

  /**
   * 计算临时连接线的 SVG 路径
   *
   * 从端口位置到鼠标当前位置的贝塞尔曲线。
   *
   * @param fromX - 起始 X 坐标
   * @param fromY - 起始 Y 坐标
   * @param mouseX - 鼠标 X 坐标
   * @param mouseY - 鼠标 Y 坐标
   * @param portType - 端口类型（决定曲线方向）
   * @returns SVG path 字符串
   */
  function getTempConnectionPath(
    fromX: number,
    fromY: number,
    mouseX: number,
    mouseY: number,
    portType: 'input' | 'output',
  ): string {
    if (portType === 'output') {
      return buildBezierPath(fromX, fromY, mouseX, mouseY)
    } else {
      // 从输入端口拖出时，反向绘制
      return buildBezierPath(mouseX, mouseY, fromX, fromY)
    }
  }

  /**
   * 构建贝塞尔曲线路径
   *
   * @param x1 - 起点 X
   * @param y1 - 起点 Y
   * @param x2 - 终点 X
   * @param y2 - 终点 Y
   * @returns SVG path d 属性字符串
   */
  function buildBezierPath(x1: number, y1: number, x2: number, y2: number): string {
    const dx = Math.abs(x2 - x1)

    // 根据水平距离动态调整控制点偏移
    const offset = Math.max(BEZIER_OFFSET, dx * 0.4)

    const cx1 = x1 + offset
    const cy1 = y1
    const cx2 = x2 - offset
    const cy2 = y2

    return `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`
  }

  // ============================================================
  // 连接状态管理
  // ============================================================

  /**
   * 开始创建连接
   *
   * @param nodeId - 起始节点 ID
   * @param portName - 起始端口名称
   * @param portType - 起始端口类型
   */
  function startConnecting(
    nodeId: string,
    portName: string,
    portType: 'input' | 'output',
  ): void {
    isConnecting.value = true
    connectingFrom.value = { nodeId, portName, portType }
  }

  /**
   * 结束连接创建
   *
   * 如果提供了目标端口信息，则尝试创建连接。
   *
   * @param targetNodeId - 目标节点 ID（可选）
   * @param targetPortName - 目标端口名称（可选）
   * @param nodes - 节点列表
   * @returns 创建的连接，如果未完成则返回 null
   */
  function endConnecting(
    targetNodeId?: string,
    targetPortName?: string,
    nodes?: CanvasNode[],
  ): Connection | null {
    if (!connectingFrom.value || !targetNodeId || !targetPortName || !nodes) {
      isConnecting.value = false
      connectingFrom.value = null
      return null
    }

    const from = connectingFrom.value
    let connection: Connection | null = null

    if (from.portType === 'output') {
      // 从输出端口拖到输入端口
      connection = createConnection(
        from.nodeId,
        from.portName,
        targetNodeId,
        targetPortName,
        nodes,
      )
    } else {
      // 从输入端口拖到输出端口（反向）
      connection = createConnection(
        targetNodeId,
        targetPortName,
        from.nodeId,
        from.portName,
        nodes,
      )
    }

    isConnecting.value = false
    connectingFrom.value = null
    return connection
  }

  /**
   * 取消连接创建
   */
  function cancelConnecting(): void {
    isConnecting.value = false
    connectingFrom.value = null
  }

  // ============================================================
  // 连接选择
  // ============================================================

  /**
   * 选中连接
   *
   * @param id - 连接 ID，传入 null 取消选中
   */
  function selectConnection(id: string | null): void {
    selectedConnectionId.value = id
  }

  /**
   * 根据坐标检测是否命中连接线
   *
   * 通过计算点到贝塞尔曲线的近似距离来判断。
   *
   * @param x - X 坐标
   * @param y - Y 坐标
   * @param nodes - 节点列表
   * @param threshold - 命中阈值（像素），默认 8
   * @returns 命中的连接或 null
   */
  function findConnectionAtPosition(
    x: number,
    y: number,
    nodes: CanvasNode[],
    threshold: number = 8,
  ): Connection | null {
    for (const conn of connections.value) {
      const sourceNode = nodes.find((n) => n.id === conn.sourceId)
      const targetNode = nodes.find((n) => n.id === conn.targetId)

      if (!sourceNode || !targetNode) continue

      const sourcePos = getPortPosition(sourceNode, conn.sourcePort, 'output')
      const targetPos = getPortPosition(targetNode, conn.targetPort, 'input')

      if (!sourcePos || !targetPos) continue

      // 采样贝塞尔曲线上的点进行近似距离检测
      if (isPointNearBezierCurve(x, y, sourcePos, targetPos, threshold)) {
        return conn
      }
    }
    return null
  }

  /**
   * 检测点是否靠近贝塞尔曲线
   *
   * 通过采样曲线上的多个点，计算最小距离。
   *
   * @param px - 点 X
   * @param py - 点 Y
   * @param from - 起点
   * @param to - 终点
   * @param threshold - 阈值
   * @returns 是否命中
   */
  function isPointNearBezierCurve(
    px: number,
    py: number,
    from: PortPosition,
    to: PortPosition,
    threshold: number,
  ): boolean {
    const dx = Math.abs(to.x - from.x)
    const offset = Math.max(BEZIER_OFFSET, dx * 0.4)

    const cx1 = from.x + offset
    const cy1 = from.y
    const cx2 = to.x - offset
    const cy2 = to.y

    // 采样 20 个点
    const steps = 20
    for (let i = 0; i <= steps; i++) {
      const t = i / steps
      const t2 = t * t
      const t3 = t2 * t
      const mt = 1 - t
      const mt2 = mt * mt
      const mt3 = mt2 * mt

      const bx =
        mt3 * from.x + 3 * mt2 * t * cx1 + 3 * mt * t2 * cx2 + t3 * to.x
      const by =
        mt3 * from.y + 3 * mt2 * t * cy1 + 3 * mt * t2 * cy2 + t3 * to.y

      const dist = Math.sqrt((px - bx) ** 2 + (py - by) ** 2)
      if (dist <= threshold) return true
    }

    return false
  }

  // ============================================================
  // 工具方法
  // ============================================================

  /**
   * 获取节点的所有输入连接
   *
   * @param nodeId - 节点 ID
   * @returns 输入连接列表
   */
  function getInputConnections(nodeId: string): Connection[] {
    return connections.value.filter((c) => c.targetId === nodeId)
  }

  /**
   * 获取节点的所有输出连接
   *
   * @param nodeId - 节点 ID
   * @returns 输出连接列表
   */
  function getOutputConnections(nodeId: string): Connection[] {
    return connections.value.filter((c) => c.sourceId === nodeId)
  }

  /**
   * 清空所有连接
   */
  function clearConnections(): void {
    connections.value = []
    selectedConnectionId.value = null
    isConnecting.value = false
    connectingFrom.value = null
  }

  return {
    // 响应式状态
    connections,
    selectedConnectionId,
    selectedConnection,
    isConnecting,
    connectingFrom,

    // 连接创建
    generateConnectionId,
    createConnection,

    // 连接删除
    deleteConnection,
    deleteConnectionsByNodeId,

    // 端口位置
    getPortPosition,

    // 路径计算
    getConnectionPath,
    getTempConnectionPath,

    // 连接状态
    startConnecting,
    endConnecting,
    cancelConnecting,

    // 连接选择
    selectConnection,
    findConnectionAtPosition,

    // 工具方法
    getInputConnections,
    getOutputConnections,
    clearConnections,
  }
}
