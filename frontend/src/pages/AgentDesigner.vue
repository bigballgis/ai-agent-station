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
      :running="execution.isRunning.value"
      :debug-mode="debug.isDebugMode.value"
      :can-step-next="debug.canStepNext.value"
      :can-continue="debug.canContinue.value"
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
      @run="execution.runAgent(route.params.id as string, t)"
      @stop="execution.stopExecution(t)"
      @save="saveAgent"
      @debug-toggle="debug.isDebugMode.value ? debug.exitDebugMode() : debug.enterDebugMode()"
      @debug-step="debug.stepNext()"
      @debug-continue="debug.continueExecution()"
      @debug-stop="debug.stopDebug()"
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
            :multi-selected="selectedNodeIds.has(node.id) && selectedNodeId !== node.id"
            :running="execution.runningNodeIds.value.has(node.id)"
            :completed="execution.completedNodeIds.value.has(node.id)"
            :failed="execution.failedNodeIds.value.has(node.id)"
            :has-breakpoint="debug.hasBreakpoint(node.id)"
            :is-debug-target="debug.currentDebugNodeId.value === node.id"
            :debug-status="debug.getNodeDebugInfo(node.id)?.status"
            @select="onNodeSelect"
            @portmousedown="onPortMouseDown"
            @dblclick="editingNodeId = node.id"
            @contextmenu="onNodeContextMenu"
          />
        </div>

        <!-- Rubber Band Selection -->
        <div
          v-if="isRubberBandSelecting"
          class="rubber-band"
          :style="{
            left: Math.min(rubberBandStart.x, rubberBandEnd.x) + 'px',
            top: Math.min(rubberBandStart.y, rubberBandEnd.y) + 'px',
            width: Math.abs(rubberBandEnd.x - rubberBandStart.x) + 'px',
            height: Math.abs(rubberBandEnd.y - rubberBandStart.y) + 'px',
          }"
        />

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
            <div v-if="debug.isDebugMode.value" class="context-menu-item" @click="handleContextToggleBreakpoint">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ t('designer.debug.breakpoint') }}
              <span v-if="contextMenu.targetNode && debug.hasBreakpoint(contextMenu.targetNode.id)" class="breakpoint-badge">ON</span>
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
        :nodes="nodes"
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
      :flow-state="flowState"
      :debug-info="debug.debugInfo.value"
      :current-debug-node-id="debug.currentDebugNodeId.value"
      :breakpoints="debug.breakpoints.value"
      :is-debug-mode="debug.isDebugMode.value"
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
  useFlowState,
  useDebugMode,
  useExecution,
  useGraphSerializer,
} from '@/composables/designer'
import type { CanvasNode, Connection, ConsoleLog, NodeTypeDefinition } from '@/composables/designer'

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
const { validate } = useGraphValidation()

// Auto Layout (no dependencies)
const { autoLayout } = useAutoLayout()

// Flow State (shared mutable state across nodes)
const {
  state: flowState,
  initializeFromStartNode: _initializeFromStartNode,
  setState: _setFlowState,
  clearState: _clearFlowState,
  resolveReferences: _resolveReferences,
} = useFlowState()

// flowState entries computed when needed for display

// Debug Mode
const debug = useDebugMode()

// ============================================================
// Local State
// ============================================================
const agentName = ref(t('designer.messages.unnamedAgent'))
const leftPanelCollapsed = ref(false)
const bottomPanelCollapsed = ref(true)
const consoleLogs = ref<ConsoleLog[]>([])
const editingNodeId = ref<string | null>(null)
const validationMessage = ref('')
const validationType = ref<'success' | 'error' | 'warning'>('success')
let validationTimer: ReturnType<typeof setTimeout> | null = null
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

// Graph Serializer (depends on nodes, connections, agentName)
const serializer = useGraphSerializer(nodes, connections, agentName)

// Execution (depends on nodes, connections, debug, addLog)
const execution = useExecution(
  nodes,
  connections,
  debug,
  addLog,
  () => { bottomPanelCollapsed.value = false },
)

// Non-reactive drag state
let isDraggingNode = false
let dragStartX = 0
let dragStartY = 0
let dragNodeStartX = 0
let dragNodeStartY = 0
let connectingMouseX = 0
let connectingMouseY = 0

// Grid snapping
function snapToGrid(value: number, gridSize = 20): number {
  return Math.round(value / gridSize) * gridSize
}

// Multi-select state
const selectedNodeIds = ref<Set<string>>(new Set())
const isRubberBandSelecting = ref(false)
const rubberBandStart = reactive({ x: 0, y: 0 })
const rubberBandEnd = reactive({ x: 0, y: 0 })

// Space+drag panning
let isSpaceHeld = false

// Multi-node drag tracking
let isDraggingMultiple = false
let dragNodeStartPositions = new Map<string, { x: number; y: number }>()

// Arrow key debounce state: track whether arrow keys are being held down
// to avoid pushing a history snapshot on every keydown event.
let isArrowKeyMoving = false

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

// ============================================================
// Event Handlers - Canvas
// ============================================================
function onCanvasMouseDown(event: MouseEvent) {
  hideContextMenu()

  // Space+drag for panning
  if (isSpaceHeld) {
    startPan(event, canvasContainerRef)
    return
  }

  // Start rubber band selection on empty canvas
  selectedNodeId.value = null
  selectedConnectionId.value = null
  selectedNodeIds.value.clear()

  isRubberBandSelecting.value = true
  const rect = canvasContainerRef.value?.getBoundingClientRect()
  if (rect) {
    rubberBandStart.x = event.clientX - rect.left
    rubberBandStart.y = event.clientY - rect.top
    rubberBandEnd.x = rubberBandStart.x
    rubberBandEnd.y = rubberBandStart.y
  }
}

function onCanvasMouseMove(event: MouseEvent) {
  // Handle canvas panning
  if (isPanning.value) {
    updatePan(event)
    return
  }

  // Handle rubber band selection
  if (isRubberBandSelecting.value) {
    const rect = canvasContainerRef.value?.getBoundingClientRect()
    if (rect) {
      rubberBandEnd.x = event.clientX - rect.left
      rubberBandEnd.y = event.clientY - rect.top
    }
    return
  }

  // Handle multi-node dragging
  if (isDraggingMultiple) {
    const dx = (event.clientX - dragStartX) / zoom.value
    const dy = (event.clientY - dragStartY) / zoom.value
    dragNodeStartPositions.forEach((startPos, id) => {
      updateNodePosition(id, startPos.x + dx, startPos.y + dy)
    })
    return
  }

  // Handle single node dragging
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

  // End rubber band selection
  if (isRubberBandSelecting.value) {
    isRubberBandSelecting.value = false
    // Select nodes within the rubber band
    const rect = canvasContainerRef.value?.getBoundingClientRect()
    if (rect) {
      const bandLeft = Math.min(rubberBandStart.x, rubberBandEnd.x)
      const bandTop = Math.min(rubberBandStart.y, rubberBandEnd.y)
      const bandRight = Math.max(rubberBandStart.x, rubberBandEnd.x)
      const bandBottom = Math.max(rubberBandStart.y, rubberBandEnd.y)

      // Only select if the band has meaningful size (not just a click)
      if (bandRight - bandLeft > 3 || bandBottom - bandTop > 3) {
        for (const node of nodes.value) {
          const nodeLeft = node.x * zoom.value + panX.value
          const nodeTop = node.y * zoom.value + panY.value
          const nodeRight = nodeLeft + 180 * zoom.value
          const nodeBottom = nodeTop + 80 * zoom.value

          if (nodeRight > bandLeft && nodeLeft < bandRight && nodeBottom > bandTop && nodeTop < bandBottom) {
            selectedNodeIds.value.add(node.id)
          }
        }
      }
    }
    return
  }

  // End multi-node dragging with grid snapping
  if (isDraggingMultiple) {
    isDraggingMultiple = false
    pushHistory(nodes.value, connections.value)
    dragNodeStartPositions.forEach((_, id) => {
      const node = nodes.value.find(n => n.id === id)
      if (node) {
        updateNodePosition(id, snapToGrid(node.x), snapToGrid(node.y))
      }
    })
    dragNodeStartPositions.clear()
    return
  }

  // End single node dragging with grid snapping
  if (isDraggingNode) {
    isDraggingNode = false
    if (selectedNodeId.value) {
      const node = nodes.value.find(n => n.id === selectedNodeId.value)
      if (node) {
        updateNodePosition(node.id, snapToGrid(node.x), snapToGrid(node.y))
      }
    }
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

  const x = snapToGrid((event.clientX - rect.left - panX.value) / zoom.value)
  const y = snapToGrid((event.clientY - rect.top - panY.value) / zoom.value)

  const node = addNode(nodeType, x - 90, y - 30)
  pushHistory(nodes.value, connections.value)
  addLog('info', `${t('designer.messages.addNode')}: ${node.label}`)
  hideContextMenu()
}

// ============================================================
// Event Handlers - Node & Port
// ============================================================
function onPortMouseDown(_event: MouseEvent, node: CanvasNode, port: { name: string }, portType: string) {
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

  if (event.ctrlKey || event.metaKey) {
    // Ctrl+Click: toggle node in multi-selection
    if (selectedNodeIds.value.has(node.id)) {
      selectedNodeIds.value.delete(node.id)
    } else {
      selectedNodeIds.value.add(node.id)
    }
    selectedNodeId.value = node.id
    return
  }

  // If clicking on a node that is already in multi-selection, start multi-drag
  if (selectedNodeIds.value.has(node.id)) {
    selectedNodeId.value = node.id
    isDraggingMultiple = true
    dragStartX = event.clientX
    dragStartY = event.clientY
    dragNodeStartPositions.clear()
    selectedNodeIds.value.forEach(id => {
      const n = nodes.value.find(n => n.id === id)
      if (n) {
        dragNodeStartPositions.set(id, { x: n.x, y: n.y })
      }
    })
    return
  }

  // Normal click: select only this node
  selectedNodeId.value = node.id
  selectedConnectionId.value = null
  selectedNodeIds.value.clear()

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

function handleContextToggleBreakpoint() {
  if (contextMenu.targetNode) {
    debug.toggleBreakpoint(contextMenu.targetNode.id)
    const bpState = debug.hasBreakpoint(contextMenu.targetNode.id) ? 'ON' : 'OFF'
    addLog('info', `${t('designer.debug.breakpoint')}: ${contextMenu.targetNode.label} [${bpState}]`)
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
  clearTimeout(validationTimer)
  validationTimer = window.setTimeout(() => {
    validationMessage.value = ''
  }, 5000)
}

function handleAutoLayout() {
  autoLayout(nodes.value, connections.value)
  // Snap all node positions to grid after auto-layout
  nodes.value.forEach(n => {
    updateNodePosition(n.id, snapToGrid(n.x), snapToGrid(n.y))
  })
  pushHistory(nodes.value, connections.value)
  addLog('info', t('designer.messages.autoLayoutComplete'))
  nextTick(() => fitView(nodes.value, canvasContainerRef))
}

function handleUpdateConfig(nodeId: string, key: string, value: unknown) {
  pushHistory(nodes.value, connections.value)
  updateNodeConfig(nodeId, { [key]: value })
}

function handleUpdateLabel(nodeId: string, label: string) {
  pushHistory(nodes.value, connections.value)
  updateNodeLabel(nodeId, label)
}

function handleDeleteNode(nodeId: string) {
  const node = nodes.value.find(n => n.id === nodeId)
  const label = node?.label || nodeId
  deleteNode(nodeId)
  pushHistory(nodes.value, connections.value)
  addLog('info', `${t('designer.messages.deleteNode')}: ${label}`)
}

function handlePaletteDragStart(_nodeType: NodeTypeDefinition) {
  // The NodePalette component handles setting drag data internally
}

function handleMinimapNavigate(newPanX: number, newPanY: number) {
  panX.value = newPanX
  panY.value = newPanY
}

// ============================================================
// Save / Export / Import
// ============================================================
function saveAgent() {
  const graphData = serializer.toGraphData()

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
  serializer.exportJson()
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
      serializer.fromGraphData(data)
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
// Console Helpers
// ============================================================
function addLog(level: ConsoleLog['level'], msg: string, withTimestamp = true) {
  const MAX_CONSOLE_LOGS = 500
  if (consoleLogs.value.length >= MAX_CONSOLE_LOGS) {
    consoleLogs.value.splice(0, consoleLogs.value.length - MAX_CONSOLE_LOGS + 50)
  }
  consoleLogs.value.push({
    time: withTimestamp ? new Date().toLocaleTimeString('zh-CN', { hour12: false }) : '',
    level,
    message: msg,
  })
}

// ============================================================
// Keyboard Shortcuts
// ============================================================
function onKeyDown(event: KeyboardEvent) {
  // Don't handle when editing text
  const isEditing = editingNodeId.value !== null
  if (isEditing) return
  const tag = (event.target as HTMLElement).tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

  // Space held down for panning mode
  if (event.key === ' ' && !isEditing) {
    event.preventDefault()
    isSpaceHeld = true
    canvasContainerRef.value?.classList.add('panning-mode')
    return
  }

  // Ctrl+A: Select all nodes
  if (event.ctrlKey && event.key === 'a' && !isEditing) {
    event.preventDefault()
    selectedNodeIds.value.clear()
    nodes.value.forEach(n => selectedNodeIds.value.add(n.id))
    return
  }

  // Ctrl+D: Duplicate selected nodes
  if (event.ctrlKey && event.key === 'd' && !isEditing) {
    event.preventDefault()
    if (selectedNodeIds.value.size > 0) {
      pushHistory(nodes.value, connections.value)
      const newIds = new Set<string>()
      selectedNodeIds.value.forEach(id => {
        const node = nodes.value.find(n => n.id === id)
        if (node) {
          const newNode = duplicateNode(node)
          if (newNode) newIds.add(newNode.id)
        }
      })
      selectedNodeIds.value = newIds
    }
    return
  }

  // Delete / Backspace: delete all selected nodes or connections
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedNodeIds.value.size > 0) {
      pushHistory(nodes.value, connections.value)
      selectedNodeIds.value.forEach(id => {
        const node = nodes.value.find(n => n.id === id)
        const label = node?.label || id
        deleteNode(id)
        addLog('info', `${t('designer.messages.deleteNode')}: ${label}`)
      })
      selectedNodeIds.value.clear()
      selectedNodeId.value = null
    } else if (selectedNodeId.value) {
      handleDeleteNode(selectedNodeId.value)
    } else if (selectedConnectionId.value) {
      deleteConnection(selectedConnectionId.value)
      pushHistory(nodes.value, connections.value)
      addLog('info', t('designer.messages.deleteConnection'))
    }
    return
  }

  // Arrow keys: nudge selected nodes (debounced - only push history on keyup)
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key) && !isEditing) {
    event.preventDefault()
    const delta = event.shiftKey ? 10 : 1 // Shift+Arrow = 10px nudge
    const dx = event.key === 'ArrowLeft' ? -delta : event.key === 'ArrowRight' ? delta : 0
    const dy = event.key === 'ArrowUp' ? -delta : event.key === 'ArrowDown' ? delta : 0

    // On first keydown, save history snapshot before moving
    if (!isArrowKeyMoving) {
      isArrowKeyMoving = true
      pushHistory(nodes.value, connections.value)
    }

    if (selectedNodeIds.value.size > 0) {
      selectedNodeIds.value.forEach(id => {
        const node = nodes.value.find(n => n.id === id)
        if (node) {
          updateNodePosition(id, node.x + dx, node.y + dy)
        }
      })
    } else if (selectedNodeId.value) {
      const node = nodes.value.find(n => n.id === selectedNodeId.value)
      if (node) {
        updateNodePosition(selectedNodeId.value, node.x + dx, node.y + dy)
      }
    }
    return
  }

  // Ctrl+Plus/Minus: Zoom
  if (event.ctrlKey && (event.key === '=' || event.key === '+')) {
    event.preventDefault()
    zoomIn()
    return
  }
  if (event.ctrlKey && event.key === '-') {
    event.preventDefault()
    zoomOut()
    return
  }

  // Ctrl+0: Reset zoom
  if (event.ctrlKey && event.key === '0') {
    event.preventDefault()
    resetZoom()
    return
  }

  // Ctrl+F: Focus on selected node (zoom to fit selected)
  if (event.ctrlKey && event.key === 'f' && !isEditing) {
    event.preventDefault()
    const targetNodes = selectedNodeIds.value.size > 0
      ? nodes.value.filter(n => selectedNodeIds.value.has(n.id))
      : selectedNodeId.value
        ? nodes.value.filter(n => n.id === selectedNodeId.value)
        : nodes.value
    if (targetNodes.length > 0 && canvasContainerRef.value) {
      fitView(targetNodes, canvasContainerRef)
    }
    return
  }

  // Tab: Cycle through nodes
  if (event.key === 'Tab' && !isEditing) {
    event.preventDefault()
    const allIds = nodes.value.map(n => n.id)
    if (allIds.length === 0) return
    const currentIdx = allIds.indexOf(selectedNodeId.value || '')
    const nextIdx = event.shiftKey
      ? (currentIdx - 1 + allIds.length) % allIds.length
      : (currentIdx + 1) % allIds.length
    selectedNodeId.value = allIds[nextIdx]
    selectedNodeIds.value.clear()
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
    selectedNodeIds.value.clear()
    if (isConnecting.value) cancelConnecting()
    return
  }

  // F5 - Continue execution (debug mode)
  if (event.key === 'F5' && !event.shiftKey && debug.isDebugMode.value) {
    event.preventDefault()
    if (debug.canContinue.value) {
      debug.continueExecution()
    }
    return
  }

  // Shift+F5 - Stop debugging
  if (event.key === 'F5' && event.shiftKey && debug.isDebugMode.value) {
    event.preventDefault()
    debug.stopDebug()
    if (execution.isRunning.value) {
      execution.stopExecution(t)
    }
    return
  }

  // F10 - Step over (debug mode)
  if (event.key === 'F10' && debug.isDebugMode.value) {
    event.preventDefault()
    if (debug.canStepNext.value) {
      debug.stepNext()
    }
    return
  }

  // F9 - Toggle breakpoint on selected node
  if (event.key === 'F9' && debug.isDebugMode.value && selectedNodeId.value) {
    event.preventDefault()
    debug.toggleBreakpoint(selectedNodeId.value)
    const node = nodes.value.find(n => n.id === selectedNodeId.value)
    const bpState = debug.hasBreakpoint(selectedNodeId.value) ? 'ON' : 'OFF'
    addLog('info', `${t('designer.debug.breakpoint')}: ${node?.label || selectedNodeId.value} [${bpState}]`)
    return
  }
}

// ============================================================
// Key Up Handler (for Space+drag panning)
// ============================================================
function onKeyUp(event: KeyboardEvent) {
  if (event.key === ' ') {
    isSpaceHeld = false
    canvasContainerRef.value?.classList.remove('panning-mode')
  }

  // Arrow key released: end the move batch (history was already saved on keydown)
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    isArrowKeyMoving = false
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
  window.addEventListener('keyup', onKeyUp)
  window.addEventListener('click', onGlobalClick)

  setupResizeObserver()

  // Try to load from API first, then localStorage
  const agentId = route.params.id as string
  let loaded = false

  if (agentId) {
    try {
      const res = await agentApi.getAgentById(agentId)
      const agentData: Record<string, unknown> = (res as { data?: Record<string, unknown> })?.data || res as Record<string, unknown>
      if (agentData) {
        if (agentData.name) agentName.value = String(agentData.name)
        const graphDef = agentData.graphDefinition as Record<string, unknown> | undefined
        if (graphDef && graphDef.nodes && Array.isArray(graphDef.nodes) && graphDef.nodes.length > 0) {
          serializer.fromGraphData(graphDef)
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
          serializer.fromGraphData(data)
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
  window.removeEventListener('keyup', onKeyUp)
  window.removeEventListener('click', onGlobalClick)
  execution.stopExecution(t)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (validationTimer) {
    clearTimeout(validationTimer)
    validationTimer = null
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

:global(.light) .agent-designer-page,
.agent-designer-page:not(:global(.dark *)) {
  background: #f8fafc;
  color: #1e293b;
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

:global(.light) .canvas-container {
  background: #f1f5f9;
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

:global(.light) .context-menu {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
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

:global(.light) .context-menu-item {
  color: #334155;
}

:global(.light) .context-menu-item:hover {
  background: rgba(0, 0, 0, 0.04);
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

:global(.light) .context-menu-divider {
  background: rgba(0, 0, 0, 0.06);
}

.breakpoint-badge {
  margin-left: auto;
  font-size: 9px;
  font-weight: 700;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
  padding: 1px 6px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

/* ============================================================
   Utility
   ============================================================ */
.rotate-180 {
  transform: rotate(180deg);
}

/* ============================================================
   Rubber Band Selection
   ============================================================ */
.rubber-band {
  position: absolute;
  border: 1px dashed rgba(99, 102, 241, 0.6);
  background: rgba(99, 102, 241, 0.1);
  pointer-events: none;
  z-index: 50;
}

/* ============================================================
   Panning Mode (Space+Drag)
   ============================================================ */
.canvas-container.panning-mode {
  cursor: grab;
}

.canvas-container.panning-mode:active {
  cursor: grabbing;
}
</style>
