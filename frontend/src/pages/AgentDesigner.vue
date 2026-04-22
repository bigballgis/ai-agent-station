<template>
  <div class="agent-designer-page">
    <!-- Top Toolbar -->
    <DesignerToolbar
      :agent-name="agentName"
      :can-undo="canUndo"
      :can-redo="canRedo"
      :zoom="zoom"
      :validation-message="validationMessage"
      :validation-type="validationType"
      @back="goBack"
      @undo="handleUndo"
      @redo="handleRedo"
      @zoom-in="zoomIn"
      @zoom-out="zoomOut"
      @zoom-reset="resetZoom"
      @update-name="agentName = $event"
      @import="triggerImport"
      @export="exportJson"
      @validate="handleValidate"
      @auto-layout="handleAutoLayout"
      @run="runAgent"
      @save="saveAgent"
    />

    <!-- Main Body -->
    <div class="designer-body">
      <!-- Left Panel: Node Palette -->
      <NodePalette
        :collapsed="leftPanelCollapsed"
        @toggle-collapse="leftPanelCollapsed = !leftPanelCollapsed"
        @dragstart="handlePaletteDragStart"
      />

      <!-- Center: Canvas -->
      <div
        ref="canvasContainerRef"
        class="canvas-container"
        @mousedown="onCanvasMouseDown"
        @mousemove="onCanvasMouseMove"
        @mouseup="onCanvasMouseUp"
        @wheel.prevent="onCanvasWheel"
        @dragover.prevent
        @drop="onCanvasDrop"
        @contextmenu.prevent
      >
        <div ref="canvasRef" class="canvas-viewport" :style="canvasTransformStyle">
          <!-- Grid Background -->
          <svg class="canvas-grid" width="5000" height="5000" :style="gridOffsetStyle">
            <defs>
              <pattern id="grid-small" width="20" height="20" patternUnits="userSpaceOnUse">
                <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="0.5" />
              </pattern>
              <pattern id="grid-large" width="100" height="100" patternUnits="userSpaceOnUse">
                <rect width="100" height="100" fill="url(#grid-small)" />
                <path d="M 100 0 L 0 0 0 100" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="0.5" />
              </pattern>
            </defs>
            <rect width="5000" height="5000" fill="url(#grid-large)" />
          </svg>

          <!-- SVG Connections Layer -->
          <svg class="connections-layer" width="5000" height="5000">
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto" markerUnits="strokeWidth">
                <polygon points="0 0, 10 3.5, 0 7" fill="#6366f1" />
              </marker>
              <marker id="arrowhead-selected" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto" markerUnits="strokeWidth">
                <polygon points="0 0, 10 3.5, 0 7" fill="#f59e0b" />
              </marker>
              <marker id="arrowhead-true" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto" markerUnits="strokeWidth">
                <polygon points="0 0, 10 3.5, 0 7" fill="#22c55e" />
              </marker>
              <marker id="arrowhead-false" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto" markerUnits="strokeWidth">
                <polygon points="0 0, 10 3.5, 0 7" fill="#ef4444" />
              </marker>
            </defs>
            <!-- Existing connections -->
            <path
              v-for="conn in connections"
              :key="conn.id"
              :d="getConnectionPath(conn, nodes)"
              class="connection-line"
              :class="{
                selected: selectedConnectionId === conn.id,
                'conn-true': conn.sourcePort === 'true',
                'conn-false': conn.sourcePort === 'false',
              }"
              :marker-end="getMarker(conn)"
              @click.stop="selectedConnectionId = conn.id"
            />
            <!-- Temp connection while dragging -->
            <path
              v-if="isConnecting && tempConnectionPath"
              :d="tempConnectionPath"
              class="connection-line temp"
            />
          </svg>

          <!-- Nodes -->
          <DesignerNode
            v-for="node in nodes"
            :key="node.id"
            :node="node"
            :selected="selectedNodeId === node.id"
            :running="runningNodeIds.has(node.id)"
            :completed="completedNodeIds.has(node.id)"
            :failed="failedNodeIds.has(node.id)"
            @select="onNodeSelect"
            @portmousedown="onPortMouseDown"
            @dblclick="editingNodeId = node.id"
            @contextmenu="onNodeContextMenu"
          />
        </div>

        <!-- Context Menu -->
        <div
          v-if="contextMenu.visible"
          class="context-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        >
          <template v-if="contextMenu.type === 'node'">
            <div class="context-menu-item" @click="handleContextEdit">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              {{ t('designer.contextMenu.editConfig') }}
            </div>
            <div class="context-menu-item" @click="handleContextDuplicate">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              {{ t('designer.contextMenu.copyNode') }}
            </div>
            <div class="context-menu-divider" />
            <div class="context-menu-item danger" @click="handleContextDeleteNode">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              {{ t('designer.contextMenu.deleteNode') }}
            </div>
          </template>
          <template v-else-if="contextMenu.type === 'connection'">
            <div class="context-menu-item danger" @click="handleContextDeleteConnection">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              {{ t('designer.contextMenu.deleteConnection') }}
            </div>
          </template>
          <template v-else>
            <div class="context-menu-item" @click="handleContextPaste">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              {{ t('designer.contextMenu.pasteNode') }}
            </div>
            <div class="context-menu-item" @click="handleFitView">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
              {{ t('designer.contextMenu.fitView') }}
            </div>
          </template>
        </div>

        <!-- MiniMap -->
        <MiniMap
          :nodes="nodes"
          :connections="connections"
          :canvas="{ zoom: zoom, panX: panX, panY: panY }"
          :container-size="{ width: containerWidth, height: containerHeight }"
          @navigate="handleMinimapNavigate"
        />
      </div>

      <!-- Right Panel: Config -->
      <ConfigPanel
        :node="selectedNode"
        @close="selectedNodeId = null"
        @update-config="handleUpdateConfig"
        @update-label="handleUpdateLabel"
        @delete-node="handleDeleteNode"
      />
    </div>

    <!-- Bottom Panel: Console -->
    <ConsolePanel
      :logs="consoleLogs"
      :collapsed="bottomPanelCollapsed"
      @toggle-collapse="bottomPanelCollapsed = !bottomPanelCollapsed"
      @clear="consoleLogs = []"
    />

    <!-- Hidden file input for import -->
    <input ref="fileInputRef" type="file" accept=".json" style="display: none" @change="handleImport" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'

// ============================================================
// Composables
// ============================================================
import {
  useCanvas,
  useNodes,
  useConnections,
  useHistory,
  useGraphValidation,
  useAutoLayout,
  getNodeTypeDefinition,
} from '@/composables/designer'
import type { CanvasNode, Connection, ConsoleLog, GraphData } from '@/composables/designer'

// ============================================================
// Sub-components
// ============================================================
import DesignerToolbar from '@/components/designer/DesignerToolbar.vue'
import NodePalette from '@/components/designer/NodePalette.vue'
import DesignerNode from '@/components/designer/DesignerNode.vue'
import ConfigPanel from '@/components/designer/ConfigPanel.vue'
import ConsolePanel from '@/components/designer/ConsolePanel.vue'
import MiniMap from '@/components/designer/MiniMap.vue'

// ============================================================
// API
// ============================================================
import { agentApi } from '@/api/agent'

// ============================================================
// Router
// ============================================================
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

function goBack() {
  router.push('/agents')
}

// ============================================================
// Composable Initialization
// ============================================================

// Canvas (no dependencies)
const {
  zoom,
  panX,
  panY,
  isPanning,
  canvasTransformStyle,
  gridOffsetStyle,
  zoomIn,
  zoomOut,
  resetZoom,
  fitView,
  handleWheel,
  startPan,
  updatePan,
  endPan,
} = useCanvas()

// Connections (no dependencies)
const {
  connections,
  selectedConnectionId,
  isConnecting,
  connectingFrom,
  createConnection,
  deleteConnection,
  getPortPosition,
  getConnectionPath,
  getTempConnectionPath,
  startConnecting,
  cancelConnecting,
} = useConnections()

// Nodes (depends on connections ref)
const {
  nodes,
  selectedNodeId,
  selectedNode,
  addNode,
  deleteNode,
  duplicateNode,
  updateNodePosition,
  updateNodeConfig,
  updateNodeLabel,
  copyNode,
  pasteNode,
} = useNodes(connections)

// History (no dependencies)
const { canUndo, canRedo, pushHistory, undo, redo, resetHistory } = useHistory()

// Validation (no dependencies)
const { validate, getExecutionOrder } = useGraphValidation()

// Auto Layout (no dependencies)
const { autoLayout } = useAutoLayout()

// ============================================================
// Local State
// ============================================================
const agentName = ref(t('designer.messages.unnamedAgent'))
const leftPanelCollapsed = ref(false)
const bottomPanelCollapsed = ref(true)
const consoleLogs = ref<ConsoleLog[]>([])
const runningNodeIds = ref<Set<string>>(new Set())
const completedNodeIds = ref<Set<string>>(new Set())
const failedNodeIds = ref<Set<string>>(new Set())
const editingNodeId = ref<string | null>(null)
const validationMessage = ref('')
const validationType = ref<'success' | 'error' | 'warning'>('success')
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  type: 'canvas' as 'canvas' | 'node' | 'connection',
  targetNode: null as CanvasNode | null,
  targetConnectionId: null as string | null,
})
const containerWidth = ref(0)
const containerHeight = ref(0)
const canvasContainerRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// Non-reactive drag state
let isDraggingNode = false
let dragStartX = 0
let dragStartY = 0
let dragNodeStartX = 0
let dragNodeStartY = 0
let connectingMouseX = 0
let connectingMouseY = 0

// ============================================================
// Computed
// ============================================================
const tempConnectionPath = computed(() => {
  if (!isConnecting.value || !connectingFrom.value) return null
  const fromNode = nodes.value.find(n => n.id === connectingFrom.value!.nodeId)
  if (!fromNode) return null

  const portType = connectingFrom.value.portType
  const portDef = portType === 'output'
    ? fromNode.outputs.find(p => p.name === connectingFrom.value!.portName)
    : fromNode.inputs.find(p => p.name === connectingFrom.value!.portName)
  if (!portDef) return null

  const pos = getPortPosition(fromNode, connectingFrom.value.portName, portType)
  if (!pos) return null

  const rect = canvasContainerRef.value?.getBoundingClientRect()
  if (!rect) return null

  // Convert screen-space mouse coordinates to canvas space
  const mouseX = (connectingMouseX - panX.value) / zoom.value
  const mouseY = (connectingMouseY - panY.value) / zoom.value

  return getTempConnectionPath(pos.x, pos.y, mouseX, mouseY, portType)
})

const entryNodeId = computed(() => {
  const startNode = nodes.value.find(n => n.type === 'start')
  return startNode?.id || ''
})

// ============================================================
// Event Handlers - Canvas
// ============================================================
function onCanvasMouseDown(event: MouseEvent) {
  hideContextMenu()
  selectedNodeId.value = null
  selectedConnectionId.value = null

  // Start panning
  startPan(event, canvasContainerRef)
}

function onCanvasMouseMove(event: MouseEvent) {
  // Handle canvas panning
  if (isPanning.value) {
    updatePan(event)
    return
  }

  // Handle node dragging
  if (isDraggingNode && selectedNodeId.value) {
    const node = nodes.value.find(n => n.id === selectedNodeId.value)
    if (node) {
      const dx = (event.clientX - dragStartX) / zoom.value
      const dy = (event.clientY - dragStartY) / zoom.value
      updateNodePosition(node.id, dragNodeStartX + dx, dragNodeStartY + dy)
    }
    return
  }

  // Handle connection dragging
  if (isConnecting.value) {
    const rect = canvasContainerRef.value?.getBoundingClientRect()
    if (rect) {
      connectingMouseX = event.clientX - rect.left
      connectingMouseY = event.clientY - rect.top
    }
    return
  }
}

function onCanvasMouseUp(event: MouseEvent) {
  // End panning
  if (isPanning.value) {
    endPan()
  }

  // End node dragging
  if (isDraggingNode) {
    isDraggingNode = false
    pushHistory(nodes.value, connections.value)
  }

  // End connection
  if (isConnecting.value) {
    handleConnectionEnd(event)
  }
}

function onCanvasWheel(event: WheelEvent) {
  handleWheel(event, canvasContainerRef)
}

function onCanvasDrop(event: DragEvent) {
  const raw = event.dataTransfer?.getData('application/json')
  if (!raw) return

  let nodeType: string
  try {
    const parsed = JSON.parse(raw)
    nodeType = parsed.type || parsed
  } catch {
    nodeType = raw
  }

  if (!nodeType) return

  const rect = canvasContainerRef.value?.getBoundingClientRect()
  if (!rect) return

  const x = (event.clientX - rect.left - panX.value) / zoom.value
  const y = (event.clientY - rect.top - panY.value) / zoom.value

  const node = addNode(nodeType, x - 90, y - 30)
  pushHistory(nodes.value, connections.value)
  addLog('info', `${t('designer.messages.addNode')}: ${node.label}`)
  hideContextMenu()
}

// ============================================================
// Event Handlers - Node & Port
// ============================================================
function onPortMouseDown(_event: MouseEvent, node: CanvasNode, port: any, portType: string) {
  startConnecting(node.id, port.name, portType as 'input' | 'output')

  const rect = canvasContainerRef.value?.getBoundingClientRect()
  if (rect) {
    connectingMouseX = _event.clientX - rect.left
    connectingMouseY = _event.clientY - rect.top
  }
}

function handleConnectionEnd(event: MouseEvent) {
  if (!connectingFrom.value) {
    cancelConnecting()
    return
  }

  const rect = canvasContainerRef.value?.getBoundingClientRect()
  if (!rect) {
    cancelConnecting()
    return
  }

  const mouseX = event.clientX - rect.left
  const mouseY = event.clientY - rect.top
  const canvasX = (mouseX - panX.value) / zoom.value
  const canvasY = (mouseY - panY.value) / zoom.value

  const from = connectingFrom.value
  let created = false

  for (const node of nodes.value) {
    if (node.id === from.nodeId) continue

    // If started from output, look for input ports
    if (from.portType === 'output') {
      for (const port of node.inputs) {
        const pos = getPortPosition(node, port.name, 'input')
        if (!pos) continue
        const dist = Math.sqrt((canvasX - pos.x) ** 2 + (canvasY - pos.y) ** 2)
        if (dist < 15) {
          const conn = createConnection(from.nodeId, from.portName, node.id, port.name, nodes.value)
          if (conn) {
            pushHistory(nodes.value, connections.value)
            addLog('info', `${t('designer.messages.createConnection')}: ${from.nodeId}.${from.portName} -> ${node.id}.${port.name}`)
          }
          created = true
          break
        }
      }
    }

    // If started from input, look for output ports
    if (from.portType === 'input') {
      for (const port of node.outputs) {
        const pos = getPortPosition(node, port.name, 'output')
        if (!pos) continue
        const dist = Math.sqrt((canvasX - pos.x) ** 2 + (canvasY - pos.y) ** 2)
        if (dist < 15) {
          const conn = createConnection(node.id, port.name, from.nodeId, from.portName, nodes.value)
          if (conn) {
            pushHistory(nodes.value, connections.value)
            addLog('info', `${t('designer.messages.createConnection')}: ${node.id}.${port.name} -> ${from.nodeId}.${from.portName}`)
          }
          created = true
          break
        }
      }
    }

    if (created) break
  }

  cancelConnecting()
}

function onNodeSelect(event: MouseEvent, node: CanvasNode) {
  hideContextMenu()
  selectedNodeId.value = node.id
  selectedConnectionId.value = null

  // Start node dragging
  isDraggingNode = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  dragNodeStartX = node.x
  dragNodeStartY = node.y
}

function onNodeContextMenu(event: MouseEvent, node: CanvasNode) {
  contextMenu.visible = true
  contextMenu.x = event.offsetX
  contextMenu.y = event.offsetY
  contextMenu.type = 'node'
  contextMenu.targetNode = node
  contextMenu.targetConnectionId = null
  copyNode(node)
}

// ============================================================
// Context Menu Actions
// ============================================================
function hideContextMenu() {
  contextMenu.visible = false
}

function handleContextEdit() {
  if (contextMenu.targetNode) {
    selectedNodeId.value = contextMenu.targetNode.id
    selectedConnectionId.value = null
  }
  hideContextMenu()
}

function handleContextDuplicate() {
  if (contextMenu.targetNode) {
    duplicateNode(contextMenu.targetNode)
    pushHistory(nodes.value, connections.value)
    addLog('info', `${t('designer.messages.copyNode')}: ${contextMenu.targetNode.label}`)
  }
  hideContextMenu()
}

function handleContextDeleteNode() {
  if (contextMenu.targetNode) {
    const label = contextMenu.targetNode.label
    deleteNode(contextMenu.targetNode.id)
    pushHistory(nodes.value, connections.value)
    addLog('info', `${t('designer.messages.deleteNode')}: ${label}`)
  }
  hideContextMenu()
}

function handleContextDeleteConnection() {
  if (contextMenu.targetConnectionId) {
    deleteConnection(contextMenu.targetConnectionId)
    pushHistory(nodes.value, connections.value)
    addLog('info', t('designer.messages.deleteConnection'))
  }
  hideContextMenu()
}

function handleContextPaste() {
  const pasted = pasteNode()
  if (pasted) {
    pushHistory(nodes.value, connections.value)
    addLog('info', `${t('designer.messages.pasteNode')}: ${pasted.label}`)
  }
  hideContextMenu()
}

function handleFitView() {
  fitView(nodes.value, canvasContainerRef)
  hideContextMenu()
}

// ============================================================
// Connection Marker
// ============================================================
function getMarker(conn: Connection): string {
  if (selectedConnectionId.value === conn.id) return 'url(#arrowhead-selected)'
  if (conn.sourcePort === 'true') return 'url(#arrowhead-true)'
  if (conn.sourcePort === 'false') return 'url(#arrowhead-false)'
  return 'url(#arrowhead)'
}

// ============================================================
// Actions
// ============================================================
function handleUndo() {
  const snapshot = undo()
  if (snapshot) {
    nodes.value = snapshot.nodes
    connections.value = snapshot.connections
    selectedNodeId.value = null
    selectedConnectionId.value = null
  }
}

function handleRedo() {
  const snapshot = redo()
  if (snapshot) {
    nodes.value = snapshot.nodes
    connections.value = snapshot.connections
    selectedNodeId.value = null
    selectedConnectionId.value = null
  }
}

function handleValidate() {
  const result = validate(nodes.value, connections.value)
  if (result.valid) {
    if (result.warnings.length > 0) {
      validationMessage.value = result.warnings[0]
      validationType.value = 'warning'
    } else {
      validationMessage.value = t('designer.validation.passed')
      validationType.value = 'success'
    }
    addLog('success', t('designer.messages.validatePassed'))
  } else {
    validationMessage.value = result.errors[0]
    validationType.value = 'error'
    addLog('error', `${t('designer.messages.validateFailed')}: ${result.errors.join('; ')}`)
    if (result.warnings.length > 0) {
      for (const w of result.warnings) {
        addLog('warn', `${t('designer.messages.warning')}: ${w}`)
      }
    }
  }
  // Auto-hide validation message after 5s
  setTimeout(() => {
    validationMessage.value = ''
  }, 5000)
}

function handleAutoLayout() {
  autoLayout(nodes.value, connections.value)
  pushHistory(nodes.value, connections.value)
  addLog('info', t('designer.messages.autoLayoutComplete'))
  nextTick(() => fitView(nodes.value, canvasContainerRef))
}

function handleUpdateConfig(nodeId: string, key: string, value: any) {
  updateNodeConfig(nodeId, { [key]: value })
}

function handleUpdateLabel(nodeId: string, label: string) {
  updateNodeLabel(nodeId, label)
}

function handleDeleteNode(nodeId: string) {
  const node = nodes.value.find(n => n.id === nodeId)
  const label = node?.label || nodeId
  deleteNode(nodeId)
  pushHistory(nodes.value, connections.value)
  addLog('info', `${t('designer.messages.deleteNode')}: ${label}`)
}

function handlePaletteDragStart(_nodeType: any) {
  // The NodePalette component handles setting drag data internally
}

function handleMinimapNavigate(newPanX: number, newPanY: number) {
  panX.value = newPanX
  panY.value = newPanY
}

// ============================================================
// GraphData Export/Import (Backend Compatible)
// ============================================================
function toGraphData(): GraphData {
  return {
    entryNodeId: entryNodeId.value,
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: n.type,
      label: n.label,
      config: { ...n.config },
      position: { x: Math.round(n.x), y: Math.round(n.y) },
    })),
    connections: connections.value.map(c => ({
      sourceId: c.sourceId,
      sourcePort: c.sourcePort,
      targetId: c.targetId,
      targetPort: c.targetPort,
    })),
  }
}

function fromGraphData(data: any): void {
  // Handle both old (fromNodeId) and new (sourceId) formats
  nodes.value = (data.nodes || []).map((n: any) => {
    const typeDef = getNodeTypeDefinition(n.type)
    const x = n.position?.x ?? n.x ?? 100
    const y = n.position?.y ?? n.y ?? 100
    return {
      id: n.id || `node_${n.type}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      type: n.type,
      label: n.label || typeDef?.name || n.type,
      x,
      y,
      config: n.config || typeDef?.defaultConfig || {},
      inputs: typeDef?.defaultInputs.map(p => ({ ...p })) || [],
      outputs: typeDef?.defaultOutputs.map(p => ({ ...p })) || [],
    }
  })
  connections.value = (data.connections || []).map((c: any) => ({
    id: c.id || `conn_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
    sourceId: c.sourceId || c.fromNodeId,
    sourcePort: c.sourcePort || c.fromPort || 'output',
    targetId: c.targetId || c.toNodeId,
    targetPort: c.targetPort || c.toPort || 'input',
  }))
}

// ============================================================
// Save / Export / Import
// ============================================================
function saveAgent() {
  const graphData = toGraphData()

  // Try API save first if we have an agent ID
  const agentId = route.params.id as string
  if (agentId) {
    agentApi.updateAgent(agentId, {
      name: agentName.value,
      graphDefinition: graphData,
    }).then(() => {
      message.success(t('designer.messages.saveSuccess'))
      addLog('success', `${t('designer.messages.saveSuccess')}: ${agentName.value}`)
    }).catch(() => {
      // Fallback to localStorage
      localStorage.setItem('agent_designer_data', JSON.stringify({ ...graphData, name: agentName.value }))
      message.success(t('designer.messages.saveSuccessLocal'))
      addLog('success', `${t('designer.messages.saveSuccessLocal')}: ${agentName.value}`)
    })
  } else {
    // No agent ID, save to localStorage
    localStorage.setItem('agent_designer_data', JSON.stringify({ ...graphData, name: agentName.value }))
    message.success(t('designer.messages.saveSuccess'))
    addLog('success', `${t('designer.messages.saveSuccess')}: ${agentName.value}`)
  }
}

function exportJson() {
  const graphData = { name: agentName.value, ...toGraphData() }
  const blob = new Blob([JSON.stringify(graphData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${agentName.value || 'agent-design'}.json`
  a.click()
  URL.revokeObjectURL(url)
  addLog('info', t('designer.messages.exportSuccess'))
}

function triggerImport() {
  fileInputRef.value?.click()
}

function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target?.result as string)
      if (data.name) agentName.value = data.name
      fromGraphData(data)
      resetHistory(nodes.value, connections.value)
      addLog('success', t('designer.messages.importSuccess'))
      message.success(t('designer.messages.importSuccess'))
      nextTick(() => fitView(nodes.value, canvasContainerRef))
    } catch {
      message.error(t('designer.messages.importFailed'))
      addLog('error', `${t('designer.messages.importFailed')}`)
    }
  }
  reader.readAsText(file)
  input.value = ''
}

// ============================================================
// Run Agent (Simulated Execution)
// ============================================================
async function runAgent() {
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

  bottomPanelCollapsed.value = false
  runningNodeIds.value.clear()
  completedNodeIds.value.clear()
  failedNodeIds.value.clear()

  addLog('info', t('designer.messages.runStart'))
  addLog('info', `节点数量: ${nodes.value.length}, 连线数量: ${connections.value.length}`)

  const order = getExecutionOrder(startNode.id, nodes.value, connections.value)
  for (const nodeId of order) {
    const node = nodes.value.find(n => n.id === nodeId)
    if (!node) continue

    runningNodeIds.value.add(nodeId)
    addLog('info', `${t('designer.messages.nodeRunning')}: ${node.label} (${node.type})`)

    await new Promise(resolve => setTimeout(resolve, 800 + Math.random() * 700))

    runningNodeIds.value.delete(nodeId)
    if (Math.random() < 0.05) {
      failedNodeIds.value.add(nodeId)
      addLog('error', `${t('designer.messages.nodeFailed')}: ${node.label}`)
    } else {
      completedNodeIds.value.add(nodeId)
      addLog('success', `${t('designer.messages.nodeComplete')}: ${node.label}`)
    }
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
}

// ============================================================
// Console Helpers
// ============================================================
function addLog(level: ConsoleLog['level'], msg: string) {
  consoleLogs.value.push({
    time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    level,
    message: msg,
  })
}

// ============================================================
// Keyboard Shortcuts
// ============================================================
function onKeyDown(event: KeyboardEvent) {
  // Don't handle when editing text
  if (editingNodeId.value) return
  const tag = (event.target as HTMLElement).tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

  // Delete / Backspace
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedNodeId.value) {
      handleDeleteNode(selectedNodeId.value)
    } else if (selectedConnectionId.value) {
      deleteConnection(selectedConnectionId.value)
      pushHistory(nodes.value, connections.value)
      addLog('info', t('designer.messages.deleteConnection'))
    }
    return
  }

  // Ctrl+Z - Undo
  if (event.ctrlKey && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    handleUndo()
    return
  }

  // Ctrl+Y or Ctrl+Shift+Z - Redo
  if ((event.ctrlKey && event.key === 'y') || (event.ctrlKey && event.shiftKey && event.key === 'z')) {
    event.preventDefault()
    handleRedo()
    return
  }

  // Ctrl+S - Save
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    saveAgent()
    return
  }

  // Ctrl+C - Copy node
  if (event.ctrlKey && event.key === 'c' && selectedNodeId.value) {
    const node = nodes.value.find(n => n.id === selectedNodeId.value)
    if (node) copyNode(node)
    return
  }

  // Ctrl+V - Paste node
  if (event.ctrlKey && event.key === 'v') {
    const pasted = pasteNode()
    if (pasted) {
      pushHistory(nodes.value, connections.value)
      addLog('info', `${t('designer.messages.pasteNode')}: ${pasted.label}`)
    }
    return
  }

  // Escape
  if (event.key === 'Escape') {
    hideContextMenu()
    selectedNodeId.value = null
    selectedConnectionId.value = null
    if (isConnecting.value) cancelConnecting()
    return
  }
}

// ============================================================
// Global Click (close context menu)
// ============================================================
function onGlobalClick(event: MouseEvent) {
  if (contextMenu.visible) {
    const menu = document.querySelector('.context-menu')
    if (menu && !menu.contains(event.target as Node)) {
      hideContextMenu()
    }
  }
}

// ============================================================
// Container Resize Observer
// ============================================================
let resizeObserver: ResizeObserver | null = null

function setupResizeObserver() {
  if (!canvasContainerRef.value) return
  resizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      containerWidth.value = entry.contentRect.width
      containerHeight.value = entry.contentRect.height
    }
  })
  resizeObserver.observe(canvasContainerRef.value)
}

// ============================================================
// Default Graph
// ============================================================
function createDefaultGraph() {
  addNode('start', 200, 200)
  addNode('end', 700, 200)
  resetHistory(nodes.value, connections.value)
}

// ============================================================
// Lifecycle
// ============================================================
onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('click', onGlobalClick)

  setupResizeObserver()

  // Try to load from API first, then localStorage
  const agentId = route.params.id as string
  let loaded = false

  if (agentId) {
    try {
      const res: any = await agentApi.getAgentById(agentId)
      const agentData = res?.data || res
      if (agentData) {
        if (agentData.name) agentName.value = agentData.name
        const graphDef = agentData.graphDefinition
        if (graphDef && graphDef.nodes && graphDef.nodes.length > 0) {
          fromGraphData(graphDef)
          resetHistory(nodes.value, connections.value)
          loaded = true
          nextTick(() => fitView(nodes.value, canvasContainerRef))
        }
      }
    } catch {
      // API failed, try localStorage
    }
  }

  if (!loaded) {
    try {
      const saved = localStorage.getItem('agent_designer_data')
      if (saved) {
        const data = JSON.parse(saved)
        if (data.name) agentName.value = data.name
        if (data.nodes && data.nodes.length > 0) {
          fromGraphData(data)
          resetHistory(nodes.value, connections.value)
          loaded = true
          nextTick(() => fitView(nodes.value, canvasContainerRef))
        }
      }
    } catch {
      // localStorage failed
    }
  }

  if (!loaded) {
    createDefaultGraph()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('click', onGlobalClick)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped>
/* ============================================================
   Layout
   ============================================================ */
.agent-designer-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0f0f1a;
  color: #e2e8f0;
  overflow: hidden;
  user-select: none;
}

.designer-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ============================================================
   Canvas
   ============================================================ */
.canvas-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #1a1a2e;
  cursor: grab;
}

.canvas-container:active {
  cursor: grabbing;
}

.canvas-viewport {
  position: absolute;
  top: 0;
  left: 0;
  width: 5000px;
  height: 5000px;
  transform-origin: 0 0;
}

.canvas-grid {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.connections-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

/* ============================================================
   Connection Lines
   ============================================================ */
.connection-line {
  fill: none;
  stroke: #6366f1;
  stroke-width: 2;
  pointer-events: stroke;
  cursor: pointer;
  transition: stroke 0.15s;
}

.connection-line:hover {
  stroke: #818cf8;
  stroke-width: 3;
}

.connection-line.selected {
  stroke: #f59e0b;
  stroke-width: 2.5;
}

.connection-line.conn-true {
  stroke: #22c55e;
}

.connection-line.conn-false {
  stroke: #ef4444;
}

.connection-line.temp {
  stroke: #6366f1;
  stroke-width: 2;
  stroke-dasharray: 6 4;
  opacity: 0.6;
  pointer-events: none;
}

/* ============================================================
   Context Menu
   ============================================================ */
.context-menu {
  position: absolute;
  background: #1e1e3a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 4px;
  min-width: 160px;
  z-index: 100;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
  animation: context-menu-in 0.12s ease-out;
}

@keyframes context-menu-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #e2e8f0;
  cursor: pointer;
  transition: background 0.1s;
}

.context-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.context-menu-item.danger {
  color: #f87171;
}

.context-menu-item.danger:hover {
  background: rgba(239, 68, 68, 0.1);
}

.context-menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 4px 8px;
}

/* ============================================================
   Utility
   ============================================================ */
.rotate-180 {
  transform: rotate(180deg);
}
</style>
