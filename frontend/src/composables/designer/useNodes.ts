/**
 * useNodes - 节点 CRUD、选择和剪贴板 Composable
 *
 * 管理画布节点的创建、删除、更新、选择和复制粘贴操作。
 * 从 AgentDesigner.vue 中提取的节点管理逻辑。
 *
 * 功能：
 * - nodes 响应式节点列表
 * - selectedNodeId / selectedNode 选中状态
 * - generateNodeId() 生成唯一节点 ID
 * - createNode(type, x, y) 创建节点实例
 * - addNode(type, x, y) 添加节点到画布
 * - deleteNode(id) 删除节点（同时移除关联连接）
 * - duplicateNode(node) 复制节点
 * - updateNodePosition / updateNodeConfig / updateNodeLabel 更新节点属性
 * - copyNode / pasteNode 剪贴板操作
 * - getNodeColor(type) 获取节点颜色
 */

import { ref, computed, type Ref } from 'vue'
import type { CanvasNode, Connection } from './types'
import { nodeTypeRegistry, getNodeColor as getRegistryNodeColor } from './nodeRegistry'

/** 节点 ID 计数器 */
let nodeIdCounter = 0

export function useNodes(connections: Ref<Connection[]>) {
  // ============================================================
  // 响应式状态
  // ============================================================

  /** 画布节点列表 */
  const nodes = ref<CanvasNode[]>([])

  /** 当前选中节点的 ID */
  const selectedNodeId = ref<string | null>(null)

  /** 剪贴板中的节点（用于复制粘贴） */
  const clipboardNode = ref<CanvasNode | null>(null)

  // ============================================================
  // 计算属性
  // ============================================================

  /**
   * 当前选中的节点
   *
   * 根据 selectedNodeId 从 nodes 中查找对应的节点实例。
   */
  const selectedNode = computed<CanvasNode | null>(() => {
    if (!selectedNodeId.value) return null
    return nodes.value.find((n) => n.id === selectedNodeId.value) ?? null
  })

  // ============================================================
  // 节点 ID 生成
  // ============================================================

  /**
   * 生成唯一的节点 ID
   *
   * 格式：node_{type}_{timestamp}_{counter}
   *
   * @param type - 节点类型
   * @returns 唯一节点 ID
   */
  function generateNodeId(type: string): string {
    nodeIdCounter++
    return `node_${type}_${Date.now()}_${nodeIdCounter}`
  }

  // ============================================================
  // 节点创建
  // ============================================================

  /**
   * 创建节点实例（不添加到画布）
   *
   * 根据节点类型从注册表中获取默认配置、输入输出端口，创建完整的节点对象。
   *
   * @param type - 节点类型标识
   * @param x - 节点 X 坐标
   * @param y - 节点 Y 坐标
   * @returns 创建的节点实例
   */
  function createNode(type: string, x: number, y: number): CanvasNode {
    const typeDef = nodeTypeRegistry[type]

    const id = generateNodeId(type)
    const label = typeDef?.name ?? type
    const config = typeDef
      ? JSON.parse(JSON.stringify(typeDef.defaultConfig))
      : ({} as Record<string, any>)
    const inputs = typeDef
      ? JSON.parse(JSON.stringify(typeDef.defaultInputs))
      : []
    const outputs = typeDef
      ? JSON.parse(JSON.stringify(typeDef.defaultOutputs))
      : []

    return {
      id,
      type,
      label,
      x,
      y,
      config,
      inputs,
      outputs,
    }
  }

  /**
   * 添加节点到画布
   *
   * 创建节点并添加到 nodes 列表，同时选中该节点。
   *
   * @param type - 节点类型标识
   * @param x - 节点 X 坐标
   * @param y - 节点 Y 坐标
   * @returns 新创建的节点
   */
  function addNode(type: string, x: number, y: number): CanvasNode {
    const node = createNode(type, x, y)
    nodes.value.push(node)
    selectedNodeId.value = node.id
    return node
  }

  // ============================================================
  // 节点删除
  // ============================================================

  /**
   * 删除节点
   *
   * 从 nodes 中移除指定节点，同时删除所有与该节点关联的连接线。
   *
   * @param id - 要删除的节点 ID
   */
  function deleteNode(id: string): void {
    const index = nodes.value.findIndex((n) => n.id === id)
    if (index === -1) return

    // 移除节点
    nodes.value.splice(index, 1)

    // 移除与该节点关联的所有连接
    connections.value = connections.value.filter(
      (conn) => conn.sourceId !== id && conn.targetId !== id,
    )

    // 如果删除的是选中节点，清除选中状态
    if (selectedNodeId.value === id) {
      selectedNodeId.value = null
    }
  }

  // ============================================================
  // 节点复制
  // ============================================================

  /**
   * 复制节点
   *
   * 创建一个偏移 30px 的新节点副本，保留配置但生成新 ID。
   *
   * @param node - 要复制的节点
   * @returns 新创建的节点
   */
  function duplicateNode(node: CanvasNode): CanvasNode {
    const newNode: CanvasNode = {
      ...JSON.parse(JSON.stringify(node)),
      id: generateNodeId(node.type),
      x: node.x + 30,
      y: node.y + 30,
      label: `${node.label} (副本)`,
    }
    nodes.value.push(newNode)
    selectedNodeId.value = newNode.id
    return newNode
  }

  // ============================================================
  // 节点更新
  // ============================================================

  /**
   * 更新节点位置
   *
   * @param id - 节点 ID
   * @param x - 新的 X 坐标
   * @param y - 新的 Y 坐标
   */
  function updateNodePosition(id: string, x: number, y: number): void {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      node.x = x
      node.y = y
    }
  }

  /**
   * 更新节点配置
   *
   * @param id - 节点 ID
   * @param config - 新的配置对象（会合并到现有配置）
   */
  function updateNodeConfig(id: string, config: Record<string, any>): void {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      node.config = { ...node.config, ...config }
    }
  }

  /**
   * 更新节点标签
   *
   * @param id - 节点 ID
   * @param label - 新的显示名称
   */
  function updateNodeLabel(id: string, label: string): void {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      node.label = label
    }
  }

  // ============================================================
  // 节点选择
  // ============================================================

  /**
   * 选中节点
   *
   * @param id - 要选中的节点 ID，传入 null 取消选中
   */
  function selectNode(id: string | null): void {
    selectedNodeId.value = id
  }

  /**
   * 根据坐标查找节点
   *
   * 检查给定坐标是否在某个节点的边界框内。
   *
   * @param x - X 坐标
   * @param y - Y 坐标
   * @returns 命中的节点或 null
   */
  function findNodeAtPosition(x: number, y: number): CanvasNode | null {
    // 节点尺寸常量（与 useCanvas 中的保持一致）
    const NODE_WIDTH = 220
    const NODE_HEIGHT = 80

    // 从后往前遍历（后添加的节点在上层）
    for (let i = nodes.value.length - 1; i >= 0; i--) {
      const node = nodes.value[i]
      if (
        x >= node.x &&
        x <= node.x + NODE_WIDTH &&
        y >= node.y &&
        y <= node.y + NODE_HEIGHT
      ) {
        return node
      }
    }
    return null
  }

  // ============================================================
  // 剪贴板操作
  // ============================================================

  /**
   * 复制节点到剪贴板
   *
   * @param node - 要复制的节点
   */
  function copyNode(node: CanvasNode): void {
    clipboardNode.value = JSON.parse(JSON.stringify(node))
  }

  /**
   * 从剪贴板粘贴节点
   *
   * 创建剪贴板节点的副本，偏移 30px。
   *
   * @returns 新创建的节点，如果剪贴板为空则返回 null
   */
  function pasteNode(): CanvasNode | null {
    if (!clipboardNode.value) return null
    return duplicateNode(clipboardNode.value)
  }

  // ============================================================
  // 节点颜色
  // ============================================================

  /**
   * 获取节点颜色
   *
   * @param type - 节点类型标识
   * @returns 节点颜色值
   */
  function getNodeColor(type: string): string {
    return getRegistryNodeColor(type)
  }

  // ============================================================
  // 工具方法
  // ============================================================

  /**
   * 根据 ID 获取节点
   *
   * @param id - 节点 ID
   * @returns 节点实例或 undefined
   */
  function getNodeById(id: string): CanvasNode | undefined {
    return nodes.value.find((n) => n.id === id)
  }

  /**
   * 获取指定类型的节点数量
   *
   * @param type - 节点类型
   * @returns 该类型节点的数量
   */
  function getNodeCountByType(type: string): number {
    return nodes.value.filter((n) => n.type === type).length
  }

  /**
   * 清空所有节点
   */
  function clearNodes(): void {
    nodes.value = []
    selectedNodeId.value = null
    clipboardNode.value = null
  }

  return {
    // 响应式状态
    nodes,
    selectedNodeId,
    selectedNode,
    clipboardNode,

    // 节点创建
    generateNodeId,
    createNode,
    addNode,

    // 节点删除
    deleteNode,

    // 节点复制
    duplicateNode,

    // 节点更新
    updateNodePosition,
    updateNodeConfig,
    updateNodeLabel,

    // 节点选择
    selectNode,
    findNodeAtPosition,

    // 剪贴板
    copyNode,
    pasteNode,

    // 工具方法
    getNodeColor,
    getNodeById,
    getNodeCountByType,
    clearNodes,
  }
}
