/**
 * useHistory - 撤销/重做 Composable
 *
 * 管理画布操作的撤销和重做历史记录，采用命令缓冲区模式。
 * 从 AgentDesigner.vue 中提取的历史管理逻辑。
 *
 * 功能：
 * - history 历史记录栈
 * - historyIndex 当前历史位置
 * - pushHistory(nodes, connections) 推入新快照
 * - undo() 撤销操作
 * - redo() 重做操作
 * - canUndo / canRedo 计算属性
 * - clearHistory() 清空历史
 *
 * 使用方式：
 * 在每次修改 nodes 或 connections 后调用 pushHistory() 保存快照。
 * undo() 返回上一个快照，redo() 返回下一个快照。
 */

import { ref, computed } from 'vue'
import type { CanvasNode, Connection, HistoryEntry } from './types'
import { MAX_HISTORY } from './constants'

/**
 * 根据节点数量动态计算历史记录上限。
 * 节点越多，快照越大，需要限制历史深度以控制内存占用。
 * - 节点数 <= 50: 使用默认 MAX_HISTORY (50)
 * - 节点数 > 50: MAX_HISTORY = 20
 * - 节点数 > 100: MAX_HISTORY = 10
 */
function getDynamicMaxHistory(nodeCount: number): number {
  if (nodeCount > 100) return 10
  if (nodeCount > 50) return 20
  return MAX_HISTORY
}

export function useHistory() {
  // ============================================================
  // 响应式状态
  // ============================================================

  /** 历史记录栈 */
  const history = ref<HistoryEntry[]>([])

  /** 当前历史位置索引（指向当前显示的状态） */
  const historyIndex = ref<number>(-1)

  // ============================================================
  // 计算属性
  // ============================================================

  /**
   * 是否可以撤销
   *
   * 当 historyIndex > 0 时可以撤销。
   */
  const canUndo = computed<boolean>(() => {
    return historyIndex.value > 0
  })

  /**
   * 是否可以重做
   *
   * 当 historyIndex < history.length - 1 时可以重做。
   */
  const canRedo = computed<boolean>(() => {
    return historyIndex.value < history.value.length - 1
  })

  /**
   * 当前历史记录条数
   */
  const historyCount = computed<number>(() => {
    return history.value.length
  })

  // ============================================================
  // 快照管理
  // ============================================================

  /**
   * 推入新的历史快照
   *
   * 将当前 nodes 和 connections 的深拷贝保存到历史栈。
   * 如果当前不在历史栈末尾，会丢弃后续记录（新操作覆盖重做历史）。
   *
   * @param nodes - 当前节点列表
   * @param connections - 当前连接列表
   */
  function pushHistory(nodes: CanvasNode[], connections: Connection[]): void {
    // 深拷贝当前状态
    const snapshot: HistoryEntry = {
      nodes: structuredClone(nodes),
      connections: structuredClone(connections),
    }

    // 如果不在历史栈末尾，丢弃后续记录
    if (historyIndex.value < history.value.length - 1) {
      history.value = history.value.slice(0, historyIndex.value + 1)
    }

    // 推入新快照
    history.value.push(snapshot)

    // 超出动态最大历史数量时，移除最早的记录
    const dynamicMax = getDynamicMaxHistory(nodes.length)
    if (history.value.length > dynamicMax) {
      history.value.shift()
    }

    // 更新索引到最新
    historyIndex.value = history.value.length - 1
  }

  // ============================================================
  // 撤销/重做
  // ============================================================

  /**
   * 撤销操作
   *
   * 回退到上一个历史快照。
   *
   * @returns 上一个快照的 {nodes, connections}，如果无法撤销则返回 null
   */
  function undo(): { nodes: CanvasNode[]; connections: Connection[] } | null {
    if (!canUndo.value) return null

    historyIndex.value--
    const entry = history.value[historyIndex.value]

    // 返回深拷贝，避免外部修改影响历史记录
    return {
      nodes: structuredClone(entry.nodes),
      connections: structuredClone(entry.connections),
    }
  }

  /**
   * 重做操作
   *
   * 前进到下一个历史快照。
   *
   * @returns 下一个快照的 {nodes, connections}，如果无法重做则返回 null
   */
  function redo(): { nodes: CanvasNode[]; connections: Connection[] } | null {
    if (!canRedo.value) return null

    historyIndex.value++
    const entry = history.value[historyIndex.value]

    // 返回深拷贝
    return {
      nodes: structuredClone(entry.nodes),
      connections: structuredClone(entry.connections),
    }
  }

  // ============================================================
  // 历史管理
  // ============================================================

  /**
   * 清空历史记录
   */
  function clearHistory(): void {
    history.value = []
    historyIndex.value = -1
  }

  /**
   * 重置历史到指定快照
   *
   * 清空现有历史，将给定状态作为初始快照。
   *
   * @param nodes - 初始节点列表
   * @param connections - 初始连接列表
   */
  function resetHistory(nodes: CanvasNode[], connections: Connection[]): void {
    clearHistory()
    pushHistory(nodes, connections)
  }

  return {
    // 响应式状态
    history,
    historyIndex,

    // 计算属性
    canUndo,
    canRedo,
    historyCount,

    // 快照管理
    pushHistory,

    // 撤销/重做
    undo,
    redo,

    // 历史管理
    clearHistory,
    resetHistory,
  }
}
