/**
 * Agent Designer 模块共享类型定义
 *
 * 所有 composables 共用的 TypeScript 接口和类型。
 * 注意：Connection 使用 sourceId/targetId 格式，与后端 GraphParser.java 保持一致。
 */

/** 端口定义 */
export interface PortDefinition {
  /** 端口唯一标识 */
  name: string
  /** 端口显示名称 */
  label: string
  /** 端口索引（用于计算端口位置） */
  index: number
}

/** 画布节点 */
export interface CanvasNode {
  /** 节点唯一标识 */
  id: string
  /** 节点类型（对应 nodeRegistry 中的 type） */
  type: string
  /** 节点显示名称 */
  label: string
  /** 节点 X 坐标（画布像素坐标） */
  x: number
  /** 节点 Y 坐标（画布像素坐标） */
  y: number
  /** 节点配置（与后端 GraphExecutor 期望的字段一致） */
  config: Record<string, unknown>
  /** 输入端口列表 */
  inputs: PortDefinition[]
  /** 输出端口列表 */
  outputs: PortDefinition[]
}

/** 连接线定义（后端兼容格式） */
export interface Connection {
  /** 连接唯一标识 */
  id: string
  /** 源节点 ID（后端兼容：对应 fromNodeId） */
  sourceId: string
  /** 源端口名称（后端兼容：对应 fromPort） */
  sourcePort: string
  /** 目标节点 ID（后端兼容：对应 toNodeId） */
  targetId: string
  /** 目标端口名称（后端兼容：对应 toPort） */
  targetPort: string
}

/** 控制台日志条目 */
export interface ConsoleLog {
  /** 日志时间戳 */
  time: string
  /** 日志级别 */
  level: 'info' | 'warn' | 'error' | 'success'
  /** 日志消息 */
  message: string
}

/** 历史记录条目（用于撤销/重做） */
export interface HistoryEntry {
  /** 快照时的节点列表 */
  nodes: CanvasNode[]
  /** 快照时的连接列表 */
  connections: Connection[]
}

/** 节点类型定义（用于节点注册表） */
export interface NodeTypeDefinition {
  /** 节点类型标识 */
  type: string
  /** 节点显示名称 */
  name: string
  /** 节点描述 */
  description: string
  /** 节点颜色（CSS 颜色值） */
  color: string
  /** 节点图标（emoji） */
  icon: string
  /** 节点分类 */
  category: 'flow' | 'ai' | 'integration' | 'advanced'
  /** 默认配置（与后端 GraphExecutor 期望的字段一致） */
  defaultConfig: Record<string, unknown>
  /** 默认输入端口 */
  defaultInputs: PortDefinition[]
  /** 默认输出端口 */
  defaultOutputs: PortDefinition[]
  /** 配置字段 Schema（用于 ConfigPanel 渲染） */
  configSchema: ConfigFieldSchema[]
}

/** 配置字段 Schema（用于 ConfigPanel 渲染） */
export interface ConfigFieldSchema {
  /** 字段名 */
  key: string
  /** 显示标签 */
  label: string
  /** 字段类型 */
  type: 'text' | 'textarea' | 'number' | 'select' | 'slider' | 'json' | 'key-value' | 'code' | 'switch-cases'
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue: unknown
  /** 选项列表（select 类型使用） */
  options?: Array<{ label: string; value: string }>
  /** 占位提示文本 */
  placeholder?: string
  /** 字段说明 */
  tooltip?: string
  /** 最小值（number 类型） */
  min?: number
  /** 最大值（number/slider 类型） */
  max?: number
  /** 步进值（number/slider 类型） */
  step?: number
}

/** 导出的图数据格式（与后端 API 对接） */
export interface GraphData {
  /** 节点列表 */
  nodes: Array<{
    id: string
    type: string
    label: string
    config: Record<string, unknown>
    position: { x: number; y: number }
  }>
  /** 连接列表 */
  connections: Array<{
    sourceId: string
    sourcePort: string
    targetId: string
    targetPort: string
  }>
  /** 入口节点 ID（start 节点的 id） */
  entryNodeId: string
}

/** 图验证结果 */
export interface ValidationResult {
  /** 是否通过验证 */
  valid: boolean
  /** 错误列表 */
  errors: string[]
  /** 警告列表 */
  warnings: string[]
}

/** 连接中的源信息（临时连接状态） */
export interface ConnectingFrom {
  /** 源节点 ID */
  nodeId: string
  /** 源端口名称 */
  portName: string
  /** 源端口类型 */
  portType: 'input' | 'output'
}

/** 节点注册表类型 */
export interface NodeRegistry {
  [type: string]: NodeTypeDefinition
}

/** 端口位置信息 */
export interface PortPosition {
  /** 端口 X 坐标 */
  x: number
  /** 端口 Y 坐标 */
  y: number
}

/** 画布变换参数 */
export interface CanvasTransform {
  /** 缩放比例 */
  zoom: number
  /** X 轴平移 */
  panX: number
  /** Y 轴平移 */
  panY: number
}
