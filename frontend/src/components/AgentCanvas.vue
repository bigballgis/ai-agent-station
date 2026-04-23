<template>
  <div class="agent-canvas-container">
    <!-- 左侧节点面板 -->
    <NodePalette
      :categories="categories"
      :collapsedCategories="collapsedCategories"
      :getNodeTypesByCategory="getNodeTypesByCategory"
      @toggleCategory="toggleCategory"
      @dragStart="handleDragStart"
    />

    <!-- 中间画布区域 -->
    <div class="canvas-wrapper">
      <!-- 工具栏 -->
      <CanvasToolbar
        :scale="scale"
        :undoStackLength="undoStack.length"
        :validationMessage="validationMessage"
        :validationType="validationType"
        @save="handleSave"
        @clear="handleClear"
        @undo="handleUndo"
        @zoomIn="handleZoomIn"
        @zoomOut="handleZoomOut"
        @zoomReset="handleZoomReset"
        @autoLayout="handleAutoLayout"
        @validate="handleValidate"
      />

      <!-- 画布 -->
      <div
        class="canvas"
        ref="canvasRef"
        @dragover.prevent
        @drop="handleDrop"
        @mousedown="handleCanvasMouseDown"
        @mousemove="handleCanvasMouseMove"
        @mouseup="handleCanvasMouseUp"
        @wheel.prevent="handleWheel"
        :style="{ transform: `scale(${scale})`, transformOrigin: 'top left' }"
      >
        <!-- 连接线层 -->
        <svg class="connections-layer" ref="connectionsRef">
          <defs>
            <marker
              v-for="colorDef in arrowMarkerColors"
              :key="colorDef.id"
              :id="'arrow-' + colorDef.id"
              viewBox="0 0 10 10"
              refX="10"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path :d="colorDef.path" :fill="colorDef.color" />
            </marker>
          </defs>
          <path
            v-for="(connection, index) in connections"
            :key="'conn-' + index"
            class="connection-line"
            :class="getConnectionClass(connection)"
            :d="getConnectionPath(connection)"
            :stroke="getConnectionColor(connection)"
            :marker-end="getConnectionMarker(connection)"
          />
        </svg>

        <!-- 临时连接线 -->
        <svg class="temp-connection-layer" v-if="tempConnection">
          <path
            class="temp-connection"
            :d="getTempConnectionPath()"
            :stroke="getTempConnectionColor()"
          />
        </svg>

        <!-- 节点 -->
        <div
          v-for="node in nodes"
          :key="node.id"
          class="canvas-node"
          :class="[`node-${node.type}`, { selected: selectedNodeId === node.id }]"
          :style="{ left: node.x + 'px', top: node.y + 'px' }"
          :data-node-id="node.id"
          @mousedown="handleNodeMouseDown($event, node.id)"
          @mouseenter="hoveredNodeId = node.id"
          @mouseleave="hoveredNodeId = null"
        >
          <!-- 工具提示 -->
          <div v-if="hoveredNodeId === node.id" class="node-tooltip">
            {{ getNodeTooltip(node) }}
          </div>

          <div class="node-header">
            <span class="node-header-icon">{{ getNodeIcon(node.type) }}</span>
            <span class="node-header-label">{{ node.label }}</span>
          </div>
          <div class="node-body">
            <div class="node-content">
              {{ getNodeSummary(node) }}
            </div>
          </div>

          <!-- 端口 -->
          <div class="node-ports">
            <!-- 输入端口 -->
            <div
              v-if="node.type !== 'start'"
              class="port port-input"
              @mousedown.stop="handlePortMouseDown($event, node.id, 'input')"
              :data-port="'input'"
            ></div>

            <!-- 输出端口 -->
            <div
              v-if="node.type !== 'end'"
              class="port port-output"
              :class="{ 'port-true': node.type === 'condition', 'port-false': false }"
              @mousedown.stop="handlePortMouseDown($event, node.id, 'output')"
              :data-port="'output'"
            ></div>

            <!-- 条件分支节点: true/false 双输出端口 -->
            <template v-if="node.type === 'condition'">
              <div
                class="port port-output port-true"
                :data-port="'output-true'"
                @mousedown.stop="handlePortMouseDown($event, node.id, 'output-true')"
                title="True"
              >
                <span class="port-label port-label-true">T</span>
              </div>
              <div
                class="port port-output port-false"
                :data-port="'output-false'"
                @mousedown.stop="handlePortMouseDown($event, node.id, 'output-false')"
                title="False"
              >
                <span class="port-label port-label-false">F</span>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧属性面板 -->
    <NodePropertyPanel
      v-if="selectedNode"
      :node="selectedNode"
      @close="selectedNodeId = null"
      @updateLabel="handleUpdateLabel"
      @updateData="handleUpdateData"
      @addKV="addKV"
      @removeKV="removeKV"
      @deleteNode="deleteSelectedNode"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * @deprecated 此组件已被 designer 模块替代。
 * 新代码请使用 /pages/AgentDesigner.vue 和 /composables/designer/ 下的 composables。
 * 保留此文件仅为向后兼容（AgentEdit.vue 历史引用）。
 * 计划在下一版本中移除。
 */
import { ref, computed, reactive, watch, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import CanvasToolbar from './agent-canvas/CanvasToolbar.vue'
import NodePalette from './agent-canvas/NodePalette.vue'
import NodePropertyPanel from './agent-canvas/NodePropertyPanel.vue'

const { t } = useI18n()

// ========== 类型定义 ==========

export interface CanvasNode {
  id: string
  type: string
  label: string
  x: number
  y: number
  data: Record<string, any>
}

export interface Connection {
  fromNodeId: string
  fromPort: string
  toNodeId: string
  toPort: string
}

interface NodeType {
  type: string
  label: string
  icon: string
  category: string
}

interface Category {
  key: string
  label: string
}

interface UndoState {
  nodes: CanvasNode[]
  connections: Connection[]
}

// ========== Props & Emits ==========

const props = defineProps<{
  modelValue?: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void
  (e: 'save', payload: { graph: { entryNodeId: string; nodes: Record<string, unknown>[]; connections: Record<string, unknown>[] } }): void
}>()

// ========== 节点类型定义 (12种) ==========

const nodeTypes: NodeType[] = [
  { type: 'start', label: t('canvas.nodeTypes.start'), icon: '\u25B6\uFE0F', category: 'flow' },
  { type: 'end', label: t('canvas.nodeTypes.end'), icon: '\u23F9\uFE0F', category: 'flow' },
  { type: 'llm', label: t('canvas.nodeTypes.llm'), icon: '\uD83E\uDDE0', category: 'ai' },
  { type: 'condition', label: t('canvas.nodeTypes.condition'), icon: '\uD83D\uDD00', category: 'flow' },
  { type: 'tool', label: t('canvas.nodeTypes.tool'), icon: '\uD83D\uDD27', category: 'integration' },
  { type: 'memory', label: t('canvas.nodeTypes.memory'), icon: '\uD83D\uDCBE', category: 'ai' },
  { type: 'retriever', label: t('canvas.nodeTypes.retriever'), icon: '\uD83D\uDD0D', category: 'ai' },
  { type: 'variable', label: t('canvas.nodeTypes.variable'), icon: '\uD83D\uDCE6', category: 'flow' },
  { type: 'exception', label: t('canvas.nodeTypes.exception'), icon: '\u26A0\uFE0F', category: 'flow' },
  { type: 'http', label: t('canvas.nodeTypes.http'), icon: '\uD83C\uDF10', category: 'integration' },
  { type: 'code', label: t('canvas.nodeTypes.code'), icon: '\uD83D\uDCBB', category: 'advanced' },
  { type: 'delay', label: t('canvas.nodeTypes.delay'), icon: '\u23F1\uFE0F', category: 'flow' },
]

const categories: Category[] = [
  { key: 'flow', label: t('canvas.categories.flow') },
  { key: 'ai', label: t('canvas.categories.ai') },
  { key: 'integration', label: t('canvas.categories.integration') },
  { key: 'advanced', label: t('canvas.categories.advanced') },
]

// ========== 节点颜色映射 ==========

const nodeColorMap: Record<string, string> = {
  start: '#52c41a',
  end: '#ff4d4f',
  llm: '#1890ff',
  condition: '#faad14',
  tool: '#722ed1',
  memory: '#fa8c16',
  retriever: '#13c2c2',
  variable: '#eb2f96',
  exception: '#ff4d4f',
  http: '#2f54eb',
  code: '#1d1d1d',
  delay: '#8c8c8c',
}

// ========== 响应式状态 ==========

const canvasRef = ref<HTMLElement>()
const connectionsRef = ref<SVGElement>()
const nodes = ref<CanvasNode[]>([])
const connections = ref<Connection[]>([])
const selectedNodeId = ref<string | null>(null)
const hoveredNodeId = ref<string | null>(null)
const scale = ref(1)
const tempConnection = ref<{
  fromNodeId: string
  fromPort: string
  toX: number
  toY: number
} | null>(null)
const isDragging = ref(false)
const dragNodeId = ref<string | null>(null)
const dragOffset = ref({ x: 0, y: 0 })
const collapsedCategories = reactive<Record<string, boolean>>({
  flow: false,
  ai: false,
  integration: false,
  advanced: false,
})
const validationMessage = ref('')
const validationType = ref<'success' | 'error' | 'warning'>('success')
let validationTimer: ReturnType<typeof setTimeout> | null = null

// 撤销栈
const undoStack = ref<UndoState[]>([])
const MAX_UNDO = 30

// ========== 计算属性 ==========

const selectedNode = computed(() => {
  return nodes.value.find(n => n.id === selectedNodeId.value) || null
})

const arrowMarkerColors = computed(() => [
  { id: 'default', color: '#1890ff', path: 'M 0 0 L 10 5 L 0 10 z' },
  { id: 'true', color: '#52c41a', path: 'M 0 0 L 10 5 L 0 10 z' },
  { id: 'false', color: '#ff4d4f', path: 'M 0 0 L 10 5 L 0 10 z' },
])

// ========== Watch ==========

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      nodes.value = val.nodes || []
      connections.value = val.connections || []
    }
  },
  { immediate: true, deep: true }
)

// ========== 工具函数 ==========

function getNodeTypesByCategory(category: string): NodeType[] {
  return nodeTypes.filter(n => n.category === category)
}

function getNodeIcon(type: string): string {
  return nodeTypes.find(n => n.type === type)?.icon || ''
}

function getNodeColor(type: string): string {
  return nodeColorMap[type] || '#1890ff'
}

function getNodeSummary(node: CanvasNode): string {
  const d = node.data
  switch (node.type) {
    case 'start': return t('canvas.nodeSummary.startEntry')
    case 'end': return t('canvas.nodeSummary.endExit')
    case 'llm': return `Model: ${d.model || t('canvas.nodeSummary.modelNotSet')} | T: ${d.temperature ?? 0.7}`
    case 'condition': return `Expr: ${d.expression || t('canvas.nodeSummary.expressionNotSet')}`
    case 'tool': return `Tool: ${d.toolName || d.toolId || t('canvas.nodeSummary.toolNotSelected')}`
    case 'memory': return `${d.action === 'save' ? t('canvas.nodeSummary.save') : t('canvas.nodeSummary.load')} | ${d.memoryType || t('canvas.nodeSummary.shortTerm')}`
    case 'retriever': return `Search: ${d.retrieverType || 'memory'}`
    case 'variable': return `${d.name || 'var'} = ${d.value || '...'}`
    case 'exception': return `Action: ${d.action || 'log'}`
    case 'http': return `${d.method || 'GET'} ${d.url || '...'}`
    case 'code': return `${d.language || 'javascript'}`
    case 'delay': return `${d.seconds ?? 1}s`
    default: return ''
  }
}

function getNodeTooltip(node: CanvasNode): string {
  const typeInfo = nodeTypes.find(n => n.type === node.type)
  return typeInfo ? `${typeInfo.label} (${node.id})` : node.label
}

function toggleCategory(key: string) {
  collapsedCategories[key] = !collapsedCategories[key]
}

// ========== 撤销功能 ==========

function pushUndo() {
  undoStack.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    connections: JSON.parse(JSON.stringify(connections.value)),
  })
  if (undoStack.value.length > MAX_UNDO) {
    undoStack.value.shift()
  }
}

function handleUndo() {
  if (undoStack.value.length === 0) return
  const state = undoStack.value.pop()!
  nodes.value = state.nodes
  connections.value = state.connections
  selectedNodeId.value = null
  emitChange()
}

// ========== 数据变更通知 ==========

function emitChange() {
  emit('update:modelValue', {
    nodes: nodes.value,
    connections: connections.value,
  })
}

// ========== 属性面板回调 ==========

function handleUpdateLabel(value: string) {
  if (!selectedNode.value) return
  pushUndo()
  selectedNode.value.label = value
  emitChange()
}

function handleUpdateData(key: string, value: unknown) {
  if (!selectedNode.value) return
  pushUndo()
  selectedNode.value.data[key] = value
  emitChange()
}

// ========== 拖拽节点到画布 ==========

function handleDragStart(event: DragEvent, type: string) {
  event.dataTransfer?.setData('nodeType', type)
}

function handleDrop(event: DragEvent) {
  const type = event.dataTransfer?.getData('nodeType') as string
  if (!type) return

  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  const x = (event.clientX - rect.left) / scale.value - 100
  const y = (event.clientY - rect.top) / scale.value - 40

  pushUndo()

  const defaultData = getDefaultNodeData(type)

  const newNode: CanvasNode = {
    id: Date.now().toString() + '_' + Math.random().toString(36).substr(2, 6),
    type,
    label: nodeTypes.find(n => n.type === type)?.label || t('canvas.nodeTypes.node'),
    x,
    y,
    data: defaultData,
  }

  nodes.value.push(newNode)
  emitChange()
}

function getDefaultNodeData(type: string): Record<string, any> {
  switch (type) {
    case 'start': return {}
    case 'end': return {}
    case 'llm': return { provider: 'openai', model: 'gpt-4', temperature: 0.7, topP: 1.0, maxTokens: 2048, systemPrompt: '', prompt: '' }
    case 'condition': return { expression: '', variable: '' }
    case 'tool': return { toolId: '', toolName: '', inputMapping: [] }
    case 'memory': return { action: 'load', memoryType: 'SHORT_TERM', query: '', summary: '' }
    case 'retriever': return { query: '', retrieverType: 'memory' }
    case 'variable': return { name: '', value: '', source: '' }
    case 'exception': return { action: 'log', fallbackValue: '' }
    case 'http': return { url: '', method: 'GET' }
    case 'code': return { language: 'javascript', code: '' }
    case 'delay': return { seconds: 1 }
    default: return {}
  }
}

// ========== 节点拖动 ==========

function handleNodeMouseDown(event: MouseEvent, nodeId: string) {
  event.stopPropagation()
  selectedNodeId.value = nodeId
  isDragging.value = true
  dragNodeId.value = nodeId

  const node = nodes.value.find(n => n.id === nodeId)
  if (node) {
    dragOffset.value = {
      x: event.clientX - node.x * scale.value,
      y: event.clientY - node.y * scale.value,
    }
  }
}

function handleCanvasMouseDown(event: MouseEvent) {
  if ((event.target as HTMLElement).classList.contains('canvas')) {
    selectedNodeId.value = null
  }
}

function handleCanvasMouseMove(event: MouseEvent) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  const x = (event.clientX - rect.left) / scale.value
  const y = (event.clientY - rect.top) / scale.value

  if (isDragging.value && dragNodeId.value) {
    const node = nodes.value.find(n => n.id === dragNodeId.value)
    if (node) {
      node.x = x - dragOffset.value.x / scale.value
      node.y = y - dragOffset.value.y / scale.value
      emitChange()
    }
  }

  if (tempConnection.value) {
    tempConnection.value.toX = x
    tempConnection.value.toY = y
  }
}

function handleCanvasMouseUp() {
  if (isDragging.value) {
    isDragging.value = false
    dragNodeId.value = null
  }
}

// ========== 连接线操作 ==========

function handlePortMouseDown(event: MouseEvent, nodeId: string, port: string) {
  event.stopPropagation()

  // 不允许从 input 端口开始拖拽
  if (port === 'input') return

  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  const x = (event.clientX - rect.left) / scale.value
  const y = (event.clientY - rect.top) / scale.value

  tempConnection.value = {
    fromNodeId: nodeId,
    fromPort: port,
    toX: x,
    toY: y,
  }

  const handleMouseUp = (e: MouseEvent) => {
    const target = e.target as HTMLElement
    if (target.classList.contains('port-input')) {
      const targetNode = target.closest('.canvas-node')
      if (targetNode) {
        const toNodeId = (targetNode as HTMLElement).getAttribute('data-node-id')
        if (toNodeId && toNodeId !== nodeId) {
          // 检查是否已存在相同连接
          const exists = connections.value.some(
            c => c.fromNodeId === nodeId && c.fromPort === port && c.toNodeId === toNodeId
          )
          if (!exists) {
            pushUndo()
            connections.value.push({
              fromNodeId: nodeId,
              fromPort: port,
              toNodeId,
              toPort: 'input',
            })
            emitChange()
          }
        }
      }
    }
    tempConnection.value = null
    document.removeEventListener('mouseup', handleMouseUp)
  }
  document.addEventListener('mouseup', handleMouseUp)
}

// ========== 连接线路径计算 ==========

const NODE_WIDTH = 200
const NODE_HEIGHT = 80

function getPortPosition(node: CanvasNode, port: string): { x: number; y: number } {
  switch (port) {
    case 'input':
      return { x: node.x, y: node.y + NODE_HEIGHT / 2 }
    case 'output':
      return { x: node.x + NODE_WIDTH, y: node.y + NODE_HEIGHT / 2 }
    case 'output-true':
      return { x: node.x + NODE_WIDTH, y: node.y + NODE_HEIGHT * 0.33 }
    case 'output-false':
      return { x: node.x + NODE_WIDTH, y: node.y + NODE_HEIGHT * 0.67 }
    default:
      return { x: node.x + NODE_WIDTH, y: node.y + NODE_HEIGHT / 2 }
  }
}

function getConnectionPath(connection: Connection): string {
  const fromNode = nodes.value.find(n => n.id === connection.fromNodeId)
  const toNode = nodes.value.find(n => n.id === connection.toNodeId)

  if (!fromNode || !toNode) return ''

  const from = getPortPosition(fromNode, connection.fromPort)
  const to = getPortPosition(toNode, connection.toPort)

  const dx = Math.abs(to.x - from.x)
  const controlOffset = Math.max(dx * 0.5, 50)

  return `M ${from.x} ${from.y} C ${from.x + controlOffset} ${from.y}, ${to.x - controlOffset} ${to.y}, ${to.x} ${to.y}`
}

function getTempConnectionPath(): string {
  if (!tempConnection.value) return ''

  const tc = tempConnection.value
  if (!tc) return ''

  const fromNode = nodes.value.find(n => n.id === tc.fromNodeId)
  if (!fromNode) return ''

  const from = getPortPosition(fromNode, tc.fromPort)
  const toX = tc.toX
  const toY = tc.toY

  const dx = Math.abs(toX - from.x)
  const controlOffset = Math.max(dx * 0.5, 50)

  return `M ${from.x} ${from.y} C ${from.x + controlOffset} ${from.y}, ${toX - controlOffset} ${toY}, ${toX} ${toY}`
}

function getConnectionColor(connection: Connection): string {
  if (connection.fromPort === 'output-true') return '#52c41a'
  if (connection.fromPort === 'output-false') return '#ff4d4f'
  const fromNode = nodes.value.find(n => n.id === connection.fromNodeId)
  if (fromNode) return getNodeColor(fromNode.type)
  return '#1890ff'
}

function getConnectionClass(connection: Connection): string {
  if (connection.fromPort === 'output-true') return 'conn-true'
  if (connection.fromPort === 'output-false') return 'conn-false'
  return ''
}

function getConnectionMarker(connection: Connection): string {
  if (connection.fromPort === 'output-true') return 'url(#arrow-true)'
  if (connection.fromPort === 'output-false') return 'url(#arrow-false)'
  return 'url(#arrow-default)'
}

function getTempConnectionColor(): string {
  if (!tempConnection.value) return '#1890ff'
  if (tempConnection.value.fromPort === 'output-true') return '#52c41a'
  if (tempConnection.value.fromPort === 'output-false') return '#ff4d4f'
  return '#1890ff'
}

// ========== Key-Value 编辑器 ==========

function addKV() {
  if (!selectedNode.value) return
  if (!selectedNode.value.data.inputMapping) {
    selectedNode.value.data.inputMapping = []
  }
  selectedNode.value.data.inputMapping.push({ key: '', value: '' })
  pushUndo()
  emitChange()
}

function removeKV(idx: number) {
  if (!selectedNode.value?.data.inputMapping) return
  pushUndo()
  selectedNode.value.data.inputMapping.splice(idx, 1)
  emitChange()
}

// ========== 工具栏操作 ==========

function handleSave() {
  const startNode = nodes.value.find(n => n.type === 'start')
  const entryNodeId = startNode ? startNode.id : (nodes.value.length > 0 ? nodes.value[0].id : '')

  const graphData = {
    graph: {
      entryNodeId,
      nodes: nodes.value.map(n => ({
        id: n.id,
        type: n.type,
        label: n.label,
        x: n.x,
        y: n.y,
        data: { ...n.data },
      })),
      connections: connections.value.map(c => ({
        fromNodeId: c.fromNodeId,
        fromPort: c.fromPort,
        toNodeId: c.toNodeId,
        toPort: c.toPort,
      })),
    },
  }

  emit('save', graphData)
  emitChange()
}

function handleClear() {
  if (nodes.value.length === 0 && connections.value.length === 0) return
  if (confirm(t('canvas.validation.confirmClear'))) {
    pushUndo()
    nodes.value = []
    connections.value = []
    selectedNodeId.value = null
    validationMessage.value = ''
    emitChange()
  }
}

function handleZoomIn() {
  scale.value = Math.min(scale.value + 0.1, 2)
}

function handleZoomOut() {
  scale.value = Math.max(scale.value - 0.1, 0.3)
}

function handleZoomReset() {
  scale.value = 1
}

function handleWheel(event: WheelEvent) {
  if (event.ctrlKey || event.metaKey) {
    const delta = event.deltaY > 0 ? -0.05 : 0.05
    scale.value = Math.max(0.3, Math.min(2, scale.value + delta))
  }
}

// ========== 自动布局 ==========

function handleAutoLayout() {
  if (nodes.value.length === 0) return

  pushUndo()

  // 按照拓扑排序进行自动布局
  const sorted = topologicalSort()
  if (!sorted) {
    validationMessage.value = t('canvas.validation.autoLayoutCircular')
    validationType.value = 'error'
    return
  }

  const HORIZONTAL_GAP = 280
  const VERTICAL_GAP = 120
  const START_X = 80
  const START_Y = 80

  // 分层布局
  const layers: string[][] = []
  const visited = new Set<string>()
  const inDegree: Record<string, number> = {}

  nodes.value.forEach(n => { inDegree[n.id] = 0 })
  connections.value.forEach(c => {
    if (inDegree[c.toNodeId] !== undefined) {
      inDegree[c.toNodeId]++
    }
  })

  // BFS 分层
  const queue: string[] = []
  nodes.value.forEach(n => {
    if (inDegree[n.id] === 0) queue.push(n.id)
  })

  while (queue.length > 0) {
    const layerSize = queue.length
    const layer: string[] = []
    for (let i = 0; i < layerSize; i++) {
      const nodeId = queue.shift()!
      if (visited.has(nodeId)) continue
      visited.add(nodeId)
      layer.push(nodeId)

      connections.value.forEach(c => {
        if (c.fromNodeId === nodeId && !visited.has(c.toNodeId)) {
          inDegree[c.toNodeId]--
          if (inDegree[c.toNodeId] === 0) {
            queue.push(c.toNodeId)
          }
        }
      })
    }
    if (layer.length > 0) layers.push(layer)
  }

  // 未被访问到的节点放在最后
  nodes.value.forEach(n => {
    if (!visited.has(n.id)) {
      layers.push([n.id])
    }
  })

  // 分配坐标
  layers.forEach((layer, layerIdx) => {
    const totalHeight = layer.length * VERTICAL_GAP
    const startY = START_Y + (Math.max(totalHeight - VERTICAL_GAP, 0)) / 2

    layer.forEach((nodeId, nodeIdx) => {
      const node = nodes.value.find(n => n.id === nodeId)
      if (node) {
        node.x = START_X + layerIdx * HORIZONTAL_GAP
        node.y = startY + nodeIdx * VERTICAL_GAP
      }
    })
  })

  emitChange()
}

function topologicalSort(): string[] | null {
  const inDegree: Record<string, number> = {}
  const adj: Record<string, string[]> = {}

  nodes.value.forEach(n => {
    inDegree[n.id] = 0
    adj[n.id] = []
  })

  connections.value.forEach(c => {
    if (adj[c.fromNodeId]) {
      adj[c.fromNodeId].push(c.toNodeId)
    }
    if (inDegree[c.toNodeId] !== undefined) {
      inDegree[c.toNodeId]++
    }
  })

  const queue: string[] = []
  Object.keys(inDegree).forEach(id => {
    if (inDegree[id] === 0) queue.push(id)
  })

  const result: string[] = []
  while (queue.length > 0) {
    const nodeId = queue.shift()!
    result.push(nodeId)
    adj[nodeId]?.forEach(toId => {
      inDegree[toId]--
      if (inDegree[toId] === 0) {
        queue.push(toId)
      }
    })
  }

  return result.length === nodes.value.length ? result : null
}

// ========== 图验证 ==========

function handleValidate() {
  const errors: string[] = []
  const warnings: string[] = []

  // 1. 检查是否有节点
  if (nodes.value.length === 0) {
    validationMessage.value = t('canvas.validation.canvasEmpty')
    validationType.value = 'warning'
    return
  }

  // 2. 检查是否有 start 节点
  const startNodes = nodes.value.filter(n => n.type === 'start')
  if (startNodes.length === 0) {
    errors.push(t('canvas.validation.missingStartNode'))
  } else if (startNodes.length > 1) {
    errors.push(t('canvas.validation.multipleStartNodes'))
  }

  // 3. 检查是否有 end 节点
  const endNodes = nodes.value.filter(n => n.type === 'end')
  if (endNodes.length === 0) {
    warnings.push(t('canvas.validation.missingEndNode'))
  }

  // 4. 检查孤立节点（无连接的节点，除了 start 和 end）
  const connectedNodeIds = new Set<string>()
  connections.value.forEach(c => {
    connectedNodeIds.add(c.fromNodeId)
    connectedNodeIds.add(c.toNodeId)
  })
  nodes.value.forEach(n => {
    if (n.type !== 'start' && n.type !== 'end' && !connectedNodeIds.has(n.id)) {
      warnings.push(t('canvas.validation.isolatedNode', { label: n.label, id: n.id }))
    }
  })

  // 5. 检查循环依赖
  const sorted = topologicalSort()
  if (!sorted) {
    errors.push(t('canvas.validation.circularDependency'))
  }

  // 6. 检查 condition 节点的连接
  nodes.value.filter(n => n.type === 'condition').forEach(n => {
    const hasTrue = connections.value.some(c => c.fromNodeId === n.id && c.fromPort === 'output-true')
    const hasFalse = connections.value.some(c => c.fromNodeId === n.id && c.fromPort === 'output-false')
    if (!hasTrue && !hasFalse) {
      warnings.push(t('canvas.validation.conditionNoOutput', { label: n.label }))
    } else if (!hasTrue) {
      warnings.push(t('canvas.validation.conditionMissingTrue', { label: n.label }))
    } else if (!hasFalse) {
      warnings.push(t('canvas.validation.conditionMissingFalse', { label: n.label }))
    }
  })

  // 7. 检查 LLM 节点配置
  nodes.value.filter(n => n.type === 'llm').forEach(n => {
    if (!n.data.model) {
      warnings.push(t('canvas.validation.llmNoModel', { label: n.label }))
    }
    if (!n.data.prompt && !n.data.systemPrompt) {
      warnings.push(t('canvas.validation.llmNoPrompt', { label: n.label }))
    }
  })

  // 8. 检查 HTTP 节点配置
  nodes.value.filter(n => n.type === 'http').forEach(n => {
    if (!n.data.url) {
      warnings.push(t('canvas.validation.httpNoUrl', { label: n.label }))
    }
  })

  // 显示结果
  if (errors.length > 0) {
    validationMessage.value = `${t('canvas.validation.validationFailed')}: ${errors.join('; ')}`
    validationType.value = 'error'
  } else if (warnings.length > 0) {
    validationMessage.value = `${t('canvas.validation.validationPassedWithWarnings')}: ${warnings.join('; ')}`
    validationType.value = 'warning'
  } else {
    validationMessage.value = t('canvas.validation.validationPassed')
    validationType.value = 'success'
  }

  // 5秒后自动清除
  clearTimeout(validationTimer)
  validationTimer = window.setTimeout(() => {
    validationMessage.value = ''
  }, 5000)
}

// ========== 节点操作 ==========

function deleteSelectedNode() {
  if (!selectedNodeId.value) return
  pushUndo()
  nodes.value = nodes.value.filter(n => n.id !== selectedNodeId.value)
  connections.value = connections.value.filter(
    c => c.fromNodeId !== selectedNodeId.value && c.toNodeId !== selectedNodeId.value
  )
  selectedNodeId.value = null
  emitChange()
}

onUnmounted(() => {
  if (validationTimer) {
    clearTimeout(validationTimer)
    validationTimer = null
  }
})
</script>

<style scoped>
/* ========== 容器布局 ========== */
.agent-canvas-container {
  display: flex;
  height: 100vh;
  background-color: #1a1a2e;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
}

/* ========== 画布区域 ========== */
.canvas-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ========== 画布 ========== */
.canvas {
  flex: 1;
  position: relative;
  overflow: auto;
  background-color: #0f0f23;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
  min-height: 2000px;
  min-width: 3000px;
}

/* ========== 连接线 ========== */
.connections-layer,
.temp-connection-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2;
}

.connection-line {
  fill: none;
  stroke-width: 2;
  opacity: 0.8;
}

.connection-line:hover {
  stroke-width: 3;
  opacity: 1;
}

.conn-true {
  stroke: #52c41a !important;
}

.conn-false {
  stroke: #ff4d4f !important;
}

.temp-connection {
  fill: none;
  stroke-width: 2;
  stroke-dasharray: 8, 4;
  opacity: 0.6;
}

/* ========== 节点 ========== */
.canvas-node {
  position: absolute;
  width: 200px;
  background: #1e1e3a;
  border: 2px solid #3a3a5c;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  cursor: move;
  transition: border-color 0.2s, box-shadow 0.2s;
  z-index: 3;
  user-select: none;
}

.canvas-node:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.canvas-node.selected {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.5), 0 6px 20px rgba(0, 0, 0, 0.4);
}

/* 节点类型颜色边框 */
.node-start { border-top: 4px solid #52c41a; }
.node-start.selected { border-color: #52c41a; }

.node-end { border-top: 4px solid #ff4d4f; }
.node-end.selected { border-color: #ff4d4f; }

.node-llm { border-top: 4px solid #1890ff; }
.node-llm.selected { border-color: #1890ff; }

.node-condition { border-top: 4px solid #faad14; }
.node-condition.selected { border-color: #faad14; }

.node-tool { border-top: 4px solid #722ed1; }
.node-tool.selected { border-color: #722ed1; }

.node-memory { border-top: 4px solid #fa8c16; }
.node-memory.selected { border-color: #fa8c16; }

.node-retriever { border-top: 4px solid #13c2c2; }
.node-retriever.selected { border-color: #13c2c2; }

.node-variable { border-top: 4px solid #eb2f96; }
.node-variable.selected { border-color: #eb2f96; }

.node-exception { border-top: 4px solid #ff4d4f; }
.node-exception.selected { border-color: #ff4d4f; }

.node-http { border-top: 4px solid #2f54eb; }
.node-http.selected { border-color: #2f54eb; }

.node-code { border-top: 4px solid #595959; }
.node-code.selected { border-color: #595959; }

.node-delay { border-top: 4px solid #8c8c8c; }
.node-delay.selected { border-color: #8c8c8c; }

/* 节点头部 */
.node-header {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-weight: 600;
  font-size: 13px;
  border-radius: 6px 6px 0 0;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-header-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-header-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 节点内容 */
.node-body {
  padding: 8px 12px;
  font-size: 11px;
  color: #8888aa;
  min-height: 32px;
}

.node-content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 节点工具提示 */
.node-tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: #2a2a4a;
  color: #e0e0e0;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
  z-index: 100;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  border: 1px solid #3a3a5c;
}

.node-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #2a2a4a;
}

/* ========== 端口 ========== */
.node-ports {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.port {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #1e1e3a;
  border: 2px solid #1890ff;
  border-radius: 50%;
  cursor: crosshair;
  pointer-events: auto;
  transition: transform 0.2s, background-color 0.2s;
  z-index: 5;
}

.port:hover {
  transform: scale(1.4);
  background: #1890ff;
}

.port-input {
  left: -7px;
  top: 33px;
}

.port-output {
  right: -7px;
  top: 33px;
}

/* 条件分支端口 */
.port-true {
  border-color: #52c41a !important;
  right: -7px;
  top: 20px;
}

.port-true:hover {
  background: #52c41a !important;
}

.port-false {
  border-color: #ff4d4f !important;
  right: -7px;
  top: 50px;
}

.port-false:hover {
  background: #ff4d4f !important;
}

.port-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 8px;
  font-weight: 700;
  pointer-events: none;
  line-height: 1;
}

.port-label-true {
  color: #52c41a;
}

.port-label-false {
  color: #ff4d4f;
}

/* ========== 滚动条 ========== */
.canvas::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.canvas::-webkit-scrollbar-track {
  background: transparent;
}

.canvas::-webkit-scrollbar-thumb {
  background: #3a3a5c;
  border-radius: 3px;
}

.canvas::-webkit-scrollbar-thumb:hover {
  background: #5a5a7c;
}
</style>
