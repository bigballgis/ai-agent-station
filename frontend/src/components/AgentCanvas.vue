<template>
  <div class="agent-canvas-container">
    <!-- 左侧节点面板 -->
    <div class="node-panel">
      <h3 class="panel-title">节点库</h3>
      <div v-for="cat in categories" :key="cat.key" class="node-category">
        <div class="category-header" @click="toggleCategory(cat.key)">
          <span class="category-arrow" :class="{ collapsed: collapsedCategories[cat.key] }">▼</span>
          <span>{{ cat.label }}</span>
        </div>
        <div v-show="!collapsedCategories[cat.key]" class="node-list">
          <div
            v-for="node in getNodeTypesByCategory(cat.key)"
            :key="node.type"
            class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node.type)"
            :title="node.label"
          >
            <span class="node-icon">{{ node.icon }}</span>
            <span class="node-label">{{ node.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间画布区域 -->
    <div class="canvas-wrapper">
      <!-- 工具栏 -->
      <div class="canvas-toolbar">
        <button @click="handleSave" class="btn btn-primary" title="保存">保存</button>
        <button @click="handleClear" class="btn btn-secondary" title="清空画布">清空</button>
        <div class="toolbar-separator"></div>
        <button @click="handleUndo" class="btn btn-secondary" title="撤销" :disabled="undoStack.length === 0">撤销</button>
        <div class="toolbar-separator"></div>
        <button @click="handleZoomIn" class="btn btn-icon" title="放大">+</button>
        <span class="zoom-label">{{ Math.round(scale * 100) }}%</span>
        <button @click="handleZoomOut" class="btn btn-icon" title="缩小">-</button>
        <button @click="handleZoomReset" class="btn btn-secondary" title="重置缩放">重置</button>
        <div class="toolbar-separator"></div>
        <button @click="handleAutoLayout" class="btn btn-secondary" title="自动布局">自动布局</button>
        <button @click="handleValidate" class="btn btn-secondary" title="验证图结构">验证</button>
        <div class="toolbar-spacer"></div>
        <span v-if="validationMessage" class="validation-msg" :class="validationType">
          {{ validationMessage }}
        </span>
      </div>

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
    <div class="properties-panel" v-if="selectedNode">
      <h3 class="panel-title">
        属性配置
        <button class="btn-close" @click="selectedNodeId = null">&times;</button>
      </h3>

      <!-- 通用属性 -->
      <div class="property-group">
        <label>节点名称</label>
        <input v-model="selectedNode.label" @input="pushUndo(); emitChange()" />
      </div>

      <!-- start 节点 -->
      <template v-if="selectedNode.type === 'start'">
        <div class="property-hint">开始节点，无特殊配置</div>
      </template>

      <!-- end 节点 -->
      <template v-if="selectedNode.type === 'end'">
        <div class="property-hint">结束节点，无特殊配置</div>
      </template>

      <!-- llm 节点 -->
      <template v-if="selectedNode.type === 'llm'">
        <div class="property-group">
          <label>Provider</label>
          <select v-model="selectedNode.data.provider" @change="pushUndo(); emitChange()">
            <option value="openai">OpenAI</option>
            <option value="anthropic">Anthropic</option>
            <option value="google">Google</option>
            <option value="ollama">Ollama</option>
            <option value="azure">Azure OpenAI</option>
          </select>
        </div>
        <div class="property-group">
          <label>模型</label>
          <input v-model="selectedNode.data.model" @input="pushUndo(); emitChange()" placeholder="gpt-4 / claude-3" />
        </div>
        <div class="property-group">
          <label>Temperature: {{ selectedNode.data.temperature ?? 0.7 }}</label>
          <input
            type="range"
            min="0"
            max="2"
            step="0.1"
            v-model.number="selectedNode.data.temperature"
            @input="emitChange()"
            class="slider"
          />
        </div>
        <div class="property-group">
          <label>Top P: {{ selectedNode.data.topP ?? 1.0 }}</label>
          <input
            type="range"
            min="0"
            max="1"
            step="0.05"
            v-model.number="selectedNode.data.topP"
            @input="emitChange()"
            class="slider"
          />
        </div>
        <div class="property-group">
          <label>Max Tokens</label>
          <input
            type="number"
            v-model.number="selectedNode.data.maxTokens"
            @input="pushUndo(); emitChange()"
            placeholder="2048"
          />
        </div>
        <div class="property-group">
          <label>System Prompt</label>
          <textarea
            v-model="selectedNode.data.systemPrompt"
            @input="pushUndo(); emitChange()"
            placeholder="系统提示词..."
            rows="3"
            class="textarea"
          ></textarea>
        </div>
        <div class="property-group">
          <label>Prompt (支持 &#123;&#123;变量&#125;&#125; 模板)</label>
          <textarea
            v-model="selectedNode.data.prompt"
            @input="pushUndo(); emitChange()"
            placeholder="请根据以下内容回答: {{input}}"
            rows="3"
            class="textarea"
          ></textarea>
        </div>
      </template>

      <!-- condition 节点 -->
      <template v-if="selectedNode.type === 'condition'">
        <div class="property-group">
          <label>表达式</label>
          <input v-model="selectedNode.data.expression" @input="pushUndo(); emitChange()" placeholder="value > 0" />
        </div>
        <div class="property-group">
          <label>变量</label>
          <input v-model="selectedNode.data.variable" @input="pushUndo(); emitChange()" placeholder="result" />
        </div>
      </template>

      <!-- tool 节点 -->
      <template v-if="selectedNode.type === 'tool'">
        <div class="property-group">
          <label>工具 ID</label>
          <select v-model="selectedNode.data.toolId" @change="pushUndo(); emitChange()">
            <option value="">请选择...</option>
            <option value="web_search">Web 搜索</option>
            <option value="calculator">计算器</option>
            <option value="weather">天气查询</option>
            <option value="database">数据库查询</option>
            <option value="email">邮件发送</option>
            <option value="file_read">文件读取</option>
            <option value="file_write">文件写入</option>
            <option value="custom">自定义工具</option>
          </select>
        </div>
        <div class="property-group">
          <label>工具名称</label>
          <input v-model="selectedNode.data.toolName" @input="pushUndo(); emitChange()" placeholder="my_tool" />
        </div>
        <div class="property-group">
          <label>输入映射 (Key-Value)</label>
          <div v-for="(kv, idx) in (selectedNode.data.inputMapping || [])" :key="idx" class="kv-row">
            <input v-model="kv.key" @input="emitChange()" placeholder="key" class="kv-input" />
            <input v-model="kv.value" @input="emitChange()" placeholder="value" class="kv-input" />
            <button class="btn btn-remove" @click="removeKV(idx)">-</button>
          </div>
          <button class="btn btn-add-kv" @click="addKV">+ 添加映射</button>
        </div>
      </template>

      <!-- memory 节点 -->
      <template v-if="selectedNode.type === 'memory'">
        <div class="property-group">
          <label>操作</label>
          <select v-model="selectedNode.data.action" @change="pushUndo(); emitChange()">
            <option value="load">加载 (Load)</option>
            <option value="save">保存 (Save)</option>
          </select>
        </div>
        <div class="property-group">
          <label>记忆类型</label>
          <select v-model="selectedNode.data.memoryType" @change="pushUndo(); emitChange()">
            <option value="SHORT_TERM">短期记忆</option>
            <option value="LONG_TERM">长期记忆</option>
            <option value="BUSINESS">业务记忆</option>
          </select>
        </div>
        <div class="property-group">
          <label>查询</label>
          <input v-model="selectedNode.data.query" @input="pushUndo(); emitChange()" placeholder="查询内容..." />
        </div>
        <div class="property-group">
          <label>摘要</label>
          <textarea
            v-model="selectedNode.data.summary"
            @input="pushUndo(); emitChange()"
            placeholder="记忆摘要..."
            rows="2"
            class="textarea"
          ></textarea>
        </div>
      </template>

      <!-- retriever 节点 -->
      <template v-if="selectedNode.type === 'retriever'">
        <div class="property-group">
          <label>查询</label>
          <input v-model="selectedNode.data.query" @input="pushUndo(); emitChange()" placeholder="检索查询..." />
        </div>
        <div class="property-group">
          <label>检索类型</label>
          <select v-model="selectedNode.data.retrieverType" @change="pushUndo(); emitChange()">
            <option value="memory">记忆检索</option>
            <option value="vector_db">向量数据库</option>
          </select>
        </div>
      </template>

      <!-- variable 节点 -->
      <template v-if="selectedNode.type === 'variable'">
        <div class="property-group">
          <label>变量名</label>
          <input v-model="selectedNode.data.name" @input="pushUndo(); emitChange()" placeholder="variable_name" />
        </div>
        <div class="property-group">
          <label>值</label>
          <input v-model="selectedNode.data.value" @input="pushUndo(); emitChange()" placeholder="value" />
        </div>
        <div class="property-group">
          <label>来源</label>
          <input v-model="selectedNode.data.source" @input="pushUndo(); emitChange()" placeholder="来源节点/表达式" />
        </div>
      </template>

      <!-- exception 节点 -->
      <template v-if="selectedNode.type === 'exception'">
        <div class="property-group">
          <label>处理方式</label>
          <select v-model="selectedNode.data.action" @change="pushUndo(); emitChange()">
            <option value="log">记录日志</option>
            <option value="retry">重试</option>
            <option value="fallback">降级处理</option>
          </select>
        </div>
        <div class="property-group">
          <label>降级值</label>
          <input v-model="selectedNode.data.fallbackValue" @input="pushUndo(); emitChange()" placeholder="默认降级值" />
        </div>
      </template>

      <!-- http 节点 -->
      <template v-if="selectedNode.type === 'http'">
        <div class="property-group">
          <label>URL</label>
          <input v-model="selectedNode.data.url" @input="pushUndo(); emitChange()" placeholder="https://api.example.com" />
        </div>
        <div class="property-group">
          <label>Method</label>
          <select v-model="selectedNode.data.method" @change="pushUndo(); emitChange()">
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
          </select>
        </div>
      </template>

      <!-- code 节点 -->
      <template v-if="selectedNode.type === 'code'">
        <div class="property-group">
          <label>语言</label>
          <select v-model="selectedNode.data.language" @change="pushUndo(); emitChange()">
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>
        </div>
        <div class="property-group">
          <label>代码</label>
          <textarea
            v-model="selectedNode.data.code"
            @input="pushUndo(); emitChange()"
            placeholder="// 在此编写代码..."
            rows="8"
            class="textarea code-area"
          ></textarea>
        </div>
      </template>

      <!-- delay 节点 -->
      <template v-if="selectedNode.type === 'delay'">
        <div class="property-group">
          <label>等待时间 (秒)</label>
          <input
            type="number"
            v-model.number="selectedNode.data.seconds"
            @input="pushUndo(); emitChange()"
            placeholder="1"
            min="0"
          />
        </div>
      </template>

      <!-- 删除按钮 -->
      <div class="property-actions">
        <button @click="deleteSelectedNode" class="btn btn-danger">删除节点</button>
      </div>
    </div>
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
  (e: 'save', payload: { graph: { entryNodeId: string; nodes: any[]; connections: any[] } }): void
}>()

// ========== 节点类型定义 (12种) ==========

const nodeTypes: NodeType[] = [
  { type: 'start', label: '开始', icon: '\u25B6\uFE0F', category: 'flow' },
  { type: 'end', label: '结束', icon: '\u23F9\uFE0F', category: 'flow' },
  { type: 'llm', label: 'LLM 调用', icon: '\uD83E\uDDE0', category: 'ai' },
  { type: 'condition', label: '条件分支', icon: '\uD83D\uDD00', category: 'flow' },
  { type: 'tool', label: '工具调用', icon: '\uD83D\uDD27', category: 'integration' },
  { type: 'memory', label: '记忆管理', icon: '\uD83D\uDCBE', category: 'ai' },
  { type: 'retriever', label: '信息检索', icon: '\uD83D\uDD0D', category: 'ai' },
  { type: 'variable', label: '变量赋值', icon: '\uD83D\uDCE6', category: 'flow' },
  { type: 'exception', label: '异常处理', icon: '\u26A0\uFE0F', category: 'flow' },
  { type: 'http', label: 'HTTP 请求', icon: '\uD83C\uDF10', category: 'integration' },
  { type: 'code', label: '代码执行', icon: '\uD83D\uDCBB', category: 'advanced' },
  { type: 'delay', label: '延时等待', icon: '\u23F1\uFE0F', category: 'flow' },
]

const categories: Category[] = [
  { key: 'flow', label: '流程控制' },
  { key: 'ai', label: 'AI 能力' },
  { key: 'integration', label: '集成' },
  { key: 'advanced', label: '高级' },
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
    case 'start': return '流程入口'
    case 'end': return '流程出口'
    case 'llm': return `模型: ${d.model || '未设置'} | T: ${d.temperature ?? 0.7}`
    case 'condition': return `表达式: ${d.expression || '未设置'}`
    case 'tool': return `工具: ${d.toolName || d.toolId || '未选择'}`
    case 'memory': return `${d.action === 'save' ? '保存' : '加载'} | ${d.memoryType || 'SHORT_TERM'}`
    case 'retriever': return `检索: ${d.retrieverType || 'memory'}`
    case 'variable': return `${d.name || 'var'} = ${d.value || '...'}`
    case 'exception': return `处理: ${d.action || 'log'}`
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
    label: nodeTypes.find(n => n.type === type)?.label || '节点',
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
  if (confirm('确定要清空画布吗？此操作不可撤销。')) {
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
    validationMessage.value = '图中存在循环依赖，无法自动布局'
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
    validationMessage.value = '画布为空'
    validationType.value = 'warning'
    return
  }

  // 2. 检查是否有 start 节点
  const startNodes = nodes.value.filter(n => n.type === 'start')
  if (startNodes.length === 0) {
    errors.push('缺少开始节点')
  } else if (startNodes.length > 1) {
    errors.push('存在多个开始节点')
  }

  // 3. 检查是否有 end 节点
  const endNodes = nodes.value.filter(n => n.type === 'end')
  if (endNodes.length === 0) {
    warnings.push('缺少结束节点')
  }

  // 4. 检查孤立节点（无连接的节点，除了 start 和 end）
  const connectedNodeIds = new Set<string>()
  connections.value.forEach(c => {
    connectedNodeIds.add(c.fromNodeId)
    connectedNodeIds.add(c.toNodeId)
  })
  nodes.value.forEach(n => {
    if (n.type !== 'start' && n.type !== 'end' && !connectedNodeIds.has(n.id)) {
      warnings.push(`节点 "${n.label}" (${n.id}) 是孤立的`)
    }
  })

  // 5. 检查循环依赖
  const sorted = topologicalSort()
  if (!sorted) {
    errors.push('图中存在循环依赖')
  }

  // 6. 检查 condition 节点的连接
  nodes.value.filter(n => n.type === 'condition').forEach(n => {
    const hasTrue = connections.value.some(c => c.fromNodeId === n.id && c.fromPort === 'output-true')
    const hasFalse = connections.value.some(c => c.fromNodeId === n.id && c.fromPort === 'output-false')
    if (!hasTrue && !hasFalse) {
      warnings.push(`条件分支 "${n.label}" 没有输出连接`)
    } else if (!hasTrue) {
      warnings.push(`条件分支 "${n.label}" 缺少 True 分支`)
    } else if (!hasFalse) {
      warnings.push(`条件分支 "${n.label}" 缺少 False 分支`)
    }
  })

  // 7. 检查 LLM 节点配置
  nodes.value.filter(n => n.type === 'llm').forEach(n => {
    if (!n.data.model) {
      warnings.push(`LLM 节点 "${n.label}" 未配置模型`)
    }
    if (!n.data.prompt && !n.data.systemPrompt) {
      warnings.push(`LLM 节点 "${n.label}" 未配置提示词`)
    }
  })

  // 8. 检查 HTTP 节点配置
  nodes.value.filter(n => n.type === 'http').forEach(n => {
    if (!n.data.url) {
      warnings.push(`HTTP 节点 "${n.label}" 未配置 URL`)
    }
  })

  // 显示结果
  if (errors.length > 0) {
    validationMessage.value = `验证失败: ${errors.join('; ')}`
    validationType.value = 'error'
  } else if (warnings.length > 0) {
    validationMessage.value = `验证通过 (有警告): ${warnings.join('; ')}`
    validationType.value = 'warning'
  } else {
    validationMessage.value = '验证通过，图结构合法'
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

/* ========== 左侧节点面板 ========== */
.node-panel {
  width: 220px;
  background: #16213e;
  border-right: 1px solid #2a2a4a;
  padding: 16px 12px;
  overflow-y: auto;
  flex-shrink: 0;
}

.panel-title {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.node-category {
  margin-bottom: 8px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #8888aa;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  cursor: pointer;
  border-radius: 4px;
  user-select: none;
}

.category-header:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #b0b0cc;
}

.category-arrow {
  font-size: 10px;
  transition: transform 0.2s;
  display: inline-block;
}

.category-arrow.collapsed {
  transform: rotate(-90deg);
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s;
  font-size: 13px;
}

.node-item:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.3);
  color: #fff;
}

.node-item:active {
  cursor: grabbing;
}

.node-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.node-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 画布区域 ========== */
.canvas-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ========== 工具栏 ========== */
.canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #16213e;
  border-bottom: 1px solid #2a2a4a;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.toolbar-separator {
  width: 1px;
  height: 24px;
  background: #2a2a4a;
}

.toolbar-spacer {
  flex: 1;
}

.zoom-label {
  font-size: 12px;
  color: #8888aa;
  min-width: 40px;
  text-align: center;
}

.validation-msg {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.validation-msg.success {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
  border: 1px solid rgba(82, 196, 26, 0.3);
}

.validation-msg.error {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.3);
}

.validation-msg.warning {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
  border: 1px solid rgba(250, 173, 20, 0.3);
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

/* ========== 右侧属性面板 ========== */
.properties-panel {
  width: 300px;
  background: #16213e;
  border-left: 1px solid #2a2a4a;
  padding: 16px;
  overflow-y: auto;
  flex-shrink: 0;
}

.btn-close {
  background: none;
  border: none;
  color: #8888aa;
  font-size: 20px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.btn-close:hover {
  color: #e0e0e0;
}

.property-group {
  margin-bottom: 14px;
}

.property-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #8888aa;
  font-weight: 500;
}

.property-group input[type="text"],
.property-group input[type="number"],
.property-group input[type="url"],
.property-group select {
  width: 100%;
  padding: 7px 10px;
  background: #0f0f23;
  border: 1px solid #3a3a5c;
  border-radius: 4px;
  color: #e0e0e0;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.property-group input:focus,
.property-group select:focus {
  border-color: #1890ff;
}

.property-group input::placeholder {
  color: #555577;
}

.textarea {
  width: 100%;
  padding: 7px 10px;
  background: #0f0f23;
  border: 1px solid #3a3a5c;
  border-radius: 4px;
  color: #e0e0e0;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.textarea:focus {
  border-color: #1890ff;
}

.textarea::placeholder {
  color: #555577;
}

.code-area {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  tab-size: 2;
}

.slider {
  width: 100%;
  -webkit-appearance: none;
  appearance: none;
  height: 4px;
  background: #3a3a5c;
  border-radius: 2px;
  outline: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #1890ff;
  cursor: pointer;
  border: 2px solid #0f0f23;
}

.slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #1890ff;
  cursor: pointer;
  border: 2px solid #0f0f23;
}

.property-hint {
  font-size: 12px;
  color: #555577;
  padding: 12px 0;
  text-align: center;
}

/* Key-Value 编辑器 */
.kv-row {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.kv-input {
  flex: 1;
  padding: 5px 8px;
  background: #0f0f23;
  border: 1px solid #3a3a5c;
  border-radius: 4px;
  color: #e0e0e0;
  font-size: 12px;
  outline: none;
  min-width: 0;
}

.kv-input:focus {
  border-color: #1890ff;
}

.kv-input::placeholder {
  color: #555577;
}

.property-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #2a2a4a;
}

/* ========== 按钮 ========== */
.btn {
  padding: 6px 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.06);
  color: #c0c0d0;
  border: 1px solid #3a3a5c;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: #5a5a7c;
}

.btn-icon {
  background: rgba(255, 255, 255, 0.06);
  color: #c0c0d0;
  border: 1px solid #3a3a5c;
  padding: 6px 10px;
  font-size: 16px;
  font-weight: 600;
}

.btn-icon:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
}

.btn-danger {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.3);
  width: 100%;
}

.btn-danger:hover {
  background: rgba(255, 77, 79, 0.25);
}

.btn-remove {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.2);
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 3px;
  cursor: pointer;
  flex-shrink: 0;
}

.btn-remove:hover {
  background: rgba(255, 77, 79, 0.2);
}

.btn-add-kv {
  background: rgba(24, 144, 255, 0.1);
  color: #1890ff;
  border: 1px solid rgba(24, 144, 255, 0.2);
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 3px;
  cursor: pointer;
  width: 100%;
  margin-top: 4px;
}

.btn-add-kv:hover {
  background: rgba(24, 144, 255, 0.2);
}

/* ========== 滚动条 ========== */
.node-panel::-webkit-scrollbar,
.properties-panel::-webkit-scrollbar,
.canvas::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.node-panel::-webkit-scrollbar-track,
.properties-panel::-webkit-scrollbar-track,
.canvas::-webkit-scrollbar-track {
  background: transparent;
}

.node-panel::-webkit-scrollbar-thumb,
.properties-panel::-webkit-scrollbar-thumb,
.canvas::-webkit-scrollbar-thumb {
  background: #3a3a5c;
  border-radius: 3px;
}

.node-panel::-webkit-scrollbar-thumb:hover,
.properties-panel::-webkit-scrollbar-thumb:hover,
.canvas::-webkit-scrollbar-thumb:hover {
  background: #5a5a7c;
}

/* ========== select 下拉样式 ========== */
select option {
  background: #16213e;
  color: #e0e0e0;
}
</style>
