/**
 * Agent Designer Composables 统一导出
 *
 * 从此模块导入所有 designer 相关的 composables、类型和工具函数。
 *
 * 使用示例：
 * ```typescript
 * import {
 *   useCanvas,
 *   useNodes,
 *   useConnections,
 *   useHistory,
 *   useGraphValidation,
 *   useAutoLayout,
 *   nodeTypeRegistry,
 *   getNodeColor,
 * } from '@/composables/designer'
 *
 * import type {
 *   CanvasNode,
 *   Connection,
 *   GraphData,
 *   ValidationResult,
 * } from '@/composables/designer'
 * ```
 */

// ============================================================
// Composables
// ============================================================
export { useCanvas } from './useCanvas'
export { useNodes } from './useNodes'
export { useConnections } from './useConnections'
export { useHistory } from './useHistory'
export { useGraphValidation } from './useGraphValidation'
export { useAutoLayout } from './useAutoLayout'

// ============================================================
// 类型
// ============================================================
export type {
  PortDefinition,
  CanvasNode,
  Connection,
  ConsoleLog,
  HistoryEntry,
  NodeTypeDefinition,
  ConfigFieldSchema,
  GraphData,
  ValidationResult,
  ConnectingFrom,
  NodeRegistry,
  PortPosition,
  CanvasTransform,
} from './types'

// ============================================================
// 节点注册表
// ============================================================
export {
  nodeTypeRegistry,
  getNodeTypeDefinition,
  getNodeColor,
  getNodeIcon,
  getAllNodeTypes,
  getNodeTypesByCategory,
  getConfigSchema,
  nodeTypeNames,
} from './nodeRegistry'
