<template>
  <div class="minimap" @mousedown="handleMinimapClick">
    <svg
      class="minimap-svg"
      :viewBox="viewBoxString"
      :width="MAP_WIDTH"
      :height="MAP_HEIGHT"
    >
      <!-- Connections -->
      <line
        v-for="conn in connections"
        :key="conn.id"
        :x1="scaleX(getNodeCenter(conn.sourceId).x)"
        :y1="scaleY(getNodeCenter(conn.sourceId).y)"
        :x2="scaleX(getNodeCenter(conn.targetId).x)"
        :y2="scaleY(getNodeCenter(conn.targetId).y)"
        class="minimap-connection"
      />

      <!-- Nodes -->
      <rect
        v-for="node in nodes"
        :key="node.id"
        :x="scaleX(node.x)"
        :y="scaleY(node.y)"
        :width="scaleX(NODE_WIDTH)"
        :height="scaleY(NODE_HEIGHT)"
        :rx="1"
        :ry="1"
        :fill="getNodeColor(node.type)"
        class="minimap-node"
      />

      <!-- Viewport rectangle -->
      <rect
        :x="viewportRect.x"
        :y="viewportRect.y"
        :width="viewportRect.width"
        :height="viewportRect.height"
        fill="none"
        stroke="rgba(255, 255, 255, 0.4)"
        stroke-width="1"
        rx="1"
        ry="1"
        class="minimap-viewport"
      />
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasNode, Connection } from '@/composables/designer/types'
import { getNodeTypeDefinition } from '@/composables/designer/nodeRegistry'

const NODE_WIDTH = 180
const NODE_HEIGHT = 60
const MAP_WIDTH = 160
const MAP_HEIGHT = 120
const PADDING = 20

const props = defineProps<{
  nodes: CanvasNode[]
  connections: Connection[]
  canvas: { zoom: number; panX: number; panY: number }
  containerSize: { width: number; height: number }
}>()

const emit = defineEmits<{
  (e: 'navigate', panX: number, panY: number): void
}>()

// Calculate the bounding box of all nodes
const bounds = computed(() => {
  if (props.nodes.length === 0) {
    return { minX: 0, minY: 0, maxX: 1000, maxY: 800 }
  }

  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity

  for (const node of props.nodes) {
    minX = Math.min(minX, node.x)
    minY = Math.min(minY, node.y)
    maxX = Math.max(maxX, node.x + NODE_WIDTH)
    maxY = Math.max(maxY, node.y + NODE_HEIGHT)
  }

  // Add padding
  minX -= PADDING
  minY -= PADDING
  maxX += PADDING
  maxY += PADDING

  return { minX, minY, maxX, maxY }
})

const viewBoxString = computed(() => {
  const b = bounds.value
  return `${b.minX} ${b.minY} ${b.maxX - b.minX} ${b.maxY - b.minY}`
})

// Scale functions: map canvas coordinates to minimap coordinates
function scaleX(x: number): number {
  const b = bounds.value
  const range = b.maxX - b.minX
  if (range <= 0) return 0
  return ((x - b.minX) / range) * MAP_WIDTH
}

function scaleY(y: number): number {
  const b = bounds.value
  const range = b.maxY - b.minY
  if (range <= 0) return 0
  return ((y - b.minY) / range) * MAP_HEIGHT
}

// Viewport rectangle in minimap coordinates
const viewportRect = computed(() => {
  const b = bounds.value
  const rangeX = b.maxX - b.minX
  const rangeY = b.maxY - b.minY
  if (rangeX <= 0 || rangeY <= 0) return { x: 0, y: 0, width: MAP_WIDTH, height: MAP_HEIGHT }

  const { zoom, panX, panY } = props.canvas
  const cw = props.containerSize.width
  const ch = props.containerSize.height

  // Visible area in canvas coordinates
  const visibleX = -panX / zoom
  const visibleY = -panY / zoom
  const visibleW = cw / zoom
  const visibleH = ch / zoom

  return {
    x: scaleX(visibleX),
    y: scaleY(visibleY),
    width: Math.max((visibleW / rangeX) * MAP_WIDTH, 4),
    height: Math.max((visibleH / rangeY) * MAP_HEIGHT, 4),
  }
})

function getNodeCenter(nodeId: string): { x: number; y: number } {
  const node = props.nodes.find(n => n.id === nodeId)
  if (!node) return { x: 0, y: 0 }
  return { x: node.x + NODE_WIDTH / 2, y: node.y + NODE_HEIGHT / 2 }
}

function getNodeColor(type: string): string {
  const def = getNodeTypeDefinition(type)
  return def?.color ?? '#1890ff'
}

function handleMinimapClick(event: MouseEvent) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const clickX = event.clientX - rect.left
  const clickY = event.clientY - rect.top

  const b = bounds.value
  const rangeX = b.maxX - b.minX
  const rangeY = b.maxY - b.minY
  if (rangeX <= 0 || rangeY <= 0) return

  // Convert minimap click position to canvas coordinates
  const canvasX = (clickX / MAP_WIDTH) * rangeX + b.minX
  const canvasY = (clickY / MAP_HEIGHT) * rangeY + b.minY

  // Calculate pan to center the viewport on the clicked point
  const { zoom } = props.canvas
  const cw = props.containerSize.width
  const ch = props.containerSize.height

  const newPanX = -(canvasX * zoom - cw / 2)
  const newPanY = -(canvasY * zoom - ch / 2)

  emit('navigate', newPanX, newPanY)
}
</script>

<style scoped>
.minimap {
  position: fixed;
  bottom: 16px;
  right: 16px;
  width: 160px;
  height: 120px;
  background: rgba(22, 22, 42, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  z-index: 100;
  overflow: hidden;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.minimap:hover {
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
}

.minimap-svg {
  display: block;
}

.minimap-connection {
  stroke: rgba(255, 255, 255, 0.15);
  stroke-width: 0.5;
}

.minimap-node {
  opacity: 0.7;
}

.minimap-viewport {
  pointer-events: none;
}
</style>
