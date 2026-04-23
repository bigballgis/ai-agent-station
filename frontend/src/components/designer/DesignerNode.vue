<template>
  <div
    class="designer-node"
    :class="[
      `node-type-${node.type}`,
      {
        'is-selected': selected,
        'is-running': running,
        'is-completed': completed,
        'is-failed': failed,
        'is-debug-target': isDebugTarget,
        'debug-status-paused': debugStatus === 'paused',
        'debug-status-running': debugStatus === 'running',
        'debug-status-completed': debugStatus === 'completed',
        'debug-status-failed': debugStatus === 'failed',
      },
    ]"
    :style="nodeStyle"
    @mousedown.stop="$emit('select', $event, node)"
    @contextmenu.prevent="$emit('contextmenu', $event, node)"
  >
    <!-- Breakpoint Indicator -->
    <div v-if="hasBreakpoint" class="breakpoint-indicator">
      <svg width="12" height="12" viewBox="0 0 12 12">
        <circle cx="6" cy="6" r="5" fill="#ef4444" stroke="#1a1a2e" stroke-width="1.5" />
      </svg>
    </div>

    <!-- Input Ports -->
    <div
      v-for="port in node.inputs"
      :key="port.name"
      class="node-port input-port"
      @mousedown.stop="$emit('portmousedown', $event, node, port, 'input')"
    >
      <div class="port-dot" />
      <span class="port-label">{{ port.name }}</span>
    </div>

    <!-- Node Body -->
    <div class="node-body" @dblclick.stop="$emit('dblclick', node)">
      <div class="node-type-icon">{{ icon }}</div>
      <span class="node-label" :title="node.label">{{ node.label }}</span>
      <div class="node-type-name">{{ typeName }}</div>
    </div>

    <!-- Output Ports -->
    <div
      v-for="port in node.outputs"
      :key="port.name"
      class="node-port output-port"
      :class="getOutputPortClass(port)"
      @mousedown.stop="$emit('portmousedown', $event, node, port, 'output')"
    >
      <div class="port-dot" />
      <span class="port-label">{{ port.name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasNode, PortDefinition } from '@/composables/designer/types'
import { getNodeTypeDefinition } from '@/composables/designer/nodeRegistry'

const props = defineProps<{
  node: CanvasNode
  selected?: boolean
  running?: boolean
  completed?: boolean
  failed?: boolean
  hasBreakpoint?: boolean
  isDebugTarget?: boolean
  debugStatus?: string
}>()

defineEmits<{
  (e: 'select', event: MouseEvent, node: CanvasNode): void
  (e: 'dragstart', event: MouseEvent, node: CanvasNode): void
  (e: 'portmousedown', event: MouseEvent, node: CanvasNode, port: PortDefinition, portType: string): void
  (e: 'dblclick', node: CanvasNode): void
  (e: 'contextmenu', event: MouseEvent, node: CanvasNode): void
}>()

const nodeDef = computed(() => getNodeTypeDefinition(props.node.type))

const icon = computed(() => nodeDef.value?.icon ?? '📦')

const typeName = computed(() => nodeDef.value?.name ?? props.node.type)

const color = computed(() => nodeDef.value?.color ?? '#1890ff')

const nodeStyle = computed(() => ({
  left: `${props.node.x}px`,
  top: `${props.node.y}px`,
  '--node-color': color.value,
}))

function getOutputPortClass(port: PortDefinition): Record<string, boolean> {
  if (props.node.type === 'condition') {
    if (port.name === 'true') return { 'port-true': true }
    if (port.name === 'false') return { 'port-false': true }
  }
  if (props.node.type === 'human_approval') {
    if (port.name === 'approved') return { 'port-true': true }
    if (port.name === 'rejected') return { 'port-false': true }
  }
  return {}
}
</script>

<style scoped>
.designer-node {
  /* Width must match NODE_WIDTH in constants.ts */
  position: absolute;
  width: 180px;
  background: #1a1a2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: move;
  user-select: none;
  z-index: 3;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.designer-node:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

/* Selected state */
.designer-node.is-selected {
  border-color: var(--node-color, #1890ff);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--node-color, #1890ff) 40%, transparent),
    0 4px 16px rgba(0, 0, 0, 0.4);
}

/* Multi-selected state (secondary selection) */
.designer-node.is-multi-selected {
  border-color: #6366f1;
  box-shadow: 0 0 0 1px #6366f1, 0 4px 16px rgba(0, 0, 0, 0.4);
}

/* Running state with pulse animation */
.designer-node.is-running {
  animation: node-pulse 1.5s ease-in-out infinite;
}

@keyframes node-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--node-color, #1890ff) 50%, transparent);
  }
  50% {
    box-shadow: 0 0 0 8px color-mix(in srgb, var(--node-color, #1890ff) 0%, transparent);
  }
}

/* Completed state */
.designer-node.is-completed {
  border-color: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3), 0 4px 16px rgba(0, 0, 0, 0.4);
}

/* Failed state */
.designer-node.is-failed {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.3), 0 4px 16px rgba(0, 0, 0, 0.4);
}

/* Debug Target state */
.designer-node.is-debug-target {
  box-shadow: 0 0 0 2px #eab308, 0 0 20px rgba(234, 179, 8, 0.3);
  z-index: 4;
}

.designer-node.is-debug-target.debug-status-paused {
  box-shadow: 0 0 0 2px #eab308, 0 0 24px rgba(234, 179, 8, 0.4);
  animation: debug-pulse 1.5s ease-in-out infinite;
}

@keyframes debug-pulse {
  0%, 100% {
    box-shadow: 0 0 0 2px #eab308, 0 0 20px rgba(234, 179, 8, 0.3);
  }
  50% {
    box-shadow: 0 0 0 4px #eab308, 0 0 30px rgba(234, 179, 8, 0.5);
  }
}

/* Breakpoint Indicator */
.breakpoint-indicator {
  position: absolute;
  top: -6px;
  left: -6px;
  width: 12px;
  height: 12px;
  z-index: 10;
  cursor: pointer;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.5));
}

/* Node Body */
.node-body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 60px;
  border-top: 3px solid var(--node-color, #1890ff);
  border-radius: 7px 7px 0 0;
}

.node-type-icon {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.node-label {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  line-height: 1.3;
}

.node-type-name {
  font-size: 10px;
  color: rgba(226, 232, 240, 0.4);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Ports */
.node-port {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: crosshair;
  z-index: 5;
}

.input-port {
  left: -4px;
  top: 50%;
  transform: translateY(-50%);
  flex-direction: row-reverse;
}

.output-port {
  right: -4px;
  top: 50%;
  transform: translateY(-50%);
}

/* Condition node: two output ports vertically stacked */
.node-type-condition .output-port.port-true {
  top: 35%;
  transform: translateY(-50%);
}

.node-type-condition .output-port.port-false {
  top: 65%;
  transform: translateY(-50%);
}

/* Human approval node: two output ports vertically stacked (approved/rejected) */
.node-type-human_approval .output-port.port-true {
  top: 35%;
  transform: translateY(-50%);
}

.node-type-human_approval .output-port.port-false {
  top: 65%;
  transform: translateY(-50%);
}

.port-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a1a2e;
  border: 2px solid var(--node-color, #1890ff);
  transition: transform 0.15s ease, background-color 0.15s ease;
  flex-shrink: 0;
}

.port-dot:hover {
  transform: scale(1.4);
  background: var(--node-color, #1890ff);
}

/* Condition port colors */
.port-true .port-dot {
  border-color: #52c41a;
}

.port-true .port-dot:hover {
  background: #52c41a;
}

.port-false .port-dot {
  border-color: #ff4d4f;
}

.port-false .port-dot:hover {
  background: #ff4d4f;
}

.port-label {
  font-size: 9px;
  color: rgba(226, 232, 240, 0.5);
  white-space: nowrap;
  pointer-events: none;
  line-height: 1;
}

.port-true .port-label {
  color: rgba(82, 196, 26, 0.7);
}

.port-false .port-label {
  color: rgba(255, 77, 79, 0.7);
}
</style>
