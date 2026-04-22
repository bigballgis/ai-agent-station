<template>
  <div class="agent-designer-page">
    <!-- Top Toolbar -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <button class="toolbar-btn" title="返回" @click="goBack">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div class="toolbar-divider" />
        <button class="toolbar-btn" title="撤销 (Ctrl+Z)" :disabled="!canUndo" @click="undo">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a5 5 0 015 5v2M3 10l4-4M3 10l4 4" />
          </svg>
        </button>
        <button class="toolbar-btn" title="重做 (Ctrl+Y)" :disabled="!canRedo" @click="redo">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 10H11a5 5 0 00-5 5v2M21 10l-4-4M21 10l-4 4" />
          </svg>
        </button>
        <div class="toolbar-divider" />
        <button class="toolbar-btn" title="缩小" @click="zoomOut">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM13 10H7" />
          </svg>
        </button>
        <span class="zoom-label">{{ Math.round(canvas.zoom * 100) }}%</span>
        <button class="toolbar-btn" title="放大" @click="zoomIn">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v6m3-3H7" />
          </svg>
        </button>
        <button class="toolbar-btn" title="重置缩放" @click="resetZoom">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
        </button>
      </div>
      <div class="toolbar-center">
        <input
          v-model="agentName"
          class="agent-name-input"
          placeholder="输入 Agent 名称..."
          @blur="handleNameBlur"
        />
      </div>
      <div class="toolbar-right">
        <button class="toolbar-btn" title="导入 JSON" @click="triggerImport">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
        </button>
        <button class="toolbar-btn" title="导出 JSON" @click="exportJson">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept=".json"
          style="display: none"
          @change="handleImport"
        />
        <div class="toolbar-divider" />
        <button class="toolbar-btn btn-run" title="运行测试" @click="runAgent">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          运行
        </button>
        <button class="toolbar-btn btn-save" title="保存 (Ctrl+S)" @click="saveAgent">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
          </svg>
          保存
        </button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="designer-body">
      <!-- Left Panel - Node Palette -->
      <div class="left-panel" :class="{ collapsed: leftPanelCollapsed }">
        <div class="panel-toggle" @click="leftPanelCollapsed = !leftPanelCollapsed">
          <svg
            class="w-4 h-4 transition-transform duration-200"
            :class="{ 'rotate-180': leftPanelCollapsed }"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </div>
        <div v-show="!leftPanelCollapsed" class="panel-content">
          <div class="panel-title">节点面板</div>
          <div class="node-palette">
            <div
              v-for="nodeType in nodeTypes"
              :key="nodeType.type"
              class="palette-item"
              :style="{ '--node-color': nodeType.color }"
              draggable="true"
              @dragstart="onDragStart($event, nodeType)"
            >
              <div class="palette-icon" :style="{ background: nodeType.color + '20', color: nodeType.color }">
                <component :is="nodeType.iconComponent" />
              </div>
              <div class="palette-info">
                <div class="palette-name">{{ nodeType.name }}</div>
                <div class="palette-desc">{{ nodeType.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Center Panel - Canvas -->
      <div
        ref="canvasContainerRef"
        class="center-panel"
        @mousedown="onCanvasMouseDown"
        @mousemove="onCanvasMouseMove"
        @mouseup="onCanvasMouseUp"
        @wheel.prevent="onCanvasWheel"
        @dragover.prevent
        @drop="onCanvasDrop"
        @contextmenu.prevent="onCanvasContextMenu"
      >
        <div
          ref="canvasRef"
          class="canvas-viewport"
          :style="canvasTransformStyle"
        >
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
              <marker
                id="arrowhead"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#6366f1" />
              </marker>
              <marker
                id="arrowhead-selected"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
                markerUnits="strokeWidth"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#f59e0b" />
              </marker>
            </defs>
            <!-- Existing connections -->
            <path
              v-for="conn in connections"
              :key="conn.id"
              :d="getConnectionPath(conn)"
              class="connection-line"
              :class="{ selected: selectedConnectionId === conn.id }"
              @click.stop="selectConnection(conn.id)"
              @contextmenu.stop="onConnectionContextMenu($event, conn.id)"
            />
            <!-- Temp connection while dragging -->
            <path
              v-if="tempConnection"
              :d="tempConnection.path"
              class="connection-line temp"
            />
          </svg>

          <!-- Nodes -->
          <div
            v-for="node in nodes"
            :key="node.id"
            class="canvas-node"
            :class="{
              selected: selectedNodeId === node.id,
              running: runningNodeIds.has(node.id),
              completed: completedNodeIds.has(node.id),
              failed: failedNodeIds.has(node.id),
            }"
            :style="{
              left: node.x + 'px',
              top: node.y + 'px',
              '--node-color': getNodeColor(node.type),
            }"
            @mousedown.stop="onNodeMouseDown($event, node)"
            @contextmenu.stop="onNodeContextMenu($event, node)"
          >
            <!-- Input Ports -->
            <div
              v-for="port in node.inputs"
              :key="'in-' + port.name"
              class="node-port input-port"
              :style="{ top: getInputPortTop(node, port.index) + 'px' }"
              @mousedown.stop="onPortMouseDown($event, node, port, 'input')"
            >
              <div class="port-dot" />
              <span class="port-label">{{ port.label }}</span>
            </div>

            <!-- Node Body -->
            <div class="node-body" @dblclick.stop="editNodeName(node)">
              <div class="node-header">
                <div class="node-type-icon">
                  <component :is="getNodeTypeConfig(node.type)?.iconComponent" />
                </div>
                <span v-if="editingNodeId !== node.id" class="node-label">{{ node.label }}</span>
                <input
                  v-else
                  v-model="node.label"
                  class="node-label-input"
                  @blur="editingNodeId = null"
                  @keydown.enter="editingNodeId = null"
                  @click.stop
                />
              </div>
              <div class="node-type-name">{{ getNodeTypeConfig(node.type)?.name }}</div>
            </div>

            <!-- Output Ports -->
            <div
              v-for="port in node.outputs"
              :key="'out-' + port.name"
              class="node-port output-port"
              :style="{ top: getOutputPortTop(node, port.index) + 'px' }"
              @mousedown.stop="onPortMouseDown($event, node, port, 'output')"
            >
              <div class="port-dot" />
              <span class="port-label">{{ port.label }}</span>
            </div>
          </div>
        </div>

        <!-- Context Menu -->
        <div
          v-if="contextMenu.visible"
          class="context-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        >
          <template v-if="contextMenu.type === 'node'">
            <div class="context-menu-item" @click="editNode(contextMenu.targetNode!)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
              编辑配置
            </div>
            <div class="context-menu-item" @click="duplicateNode(contextMenu.targetNode!)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
              复制节点
            </div>
            <div class="context-menu-divider" />
            <div class="context-menu-item danger" @click="deleteNode(contextMenu.targetNode!.id)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              删除节点
            </div>
          </template>
          <template v-else-if="contextMenu.type === 'connection'">
            <div class="context-menu-item danger" @click="deleteConnection(contextMenu.targetConnectionId!)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              删除连线
            </div>
          </template>
          <template v-else>
            <div class="context-menu-item" @click="pasteNode">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>
              粘贴节点
            </div>
            <div class="context-menu-item" @click="fitView">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" /></svg>
              适应画布
            </div>
          </template>
        </div>
      </div>

      <!-- Right Panel - Node Configuration -->
      <div class="right-panel" :class="{ collapsed: !selectedNodeId }">
        <div v-if="selectedNodeId && selectedNode" class="config-panel">
          <div class="config-header">
            <div class="config-title">
              <div
                class="config-type-icon"
                :style="{ background: getNodeColor(selectedNode.type) + '20', color: getNodeColor(selectedNode.type) }"
              >
                <component :is="getNodeTypeConfig(selectedNode.type)?.iconComponent" />
              </div>
              <div>
                <div class="config-node-name">{{ selectedNode.label }}</div>
                <div class="config-node-type">{{ getNodeTypeConfig(selectedNode.type)?.name }}</div>
              </div>
            </div>
            <button class="config-close" @click="deselectNode">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="config-body">
            <!-- LLM Node Config -->
            <template v-if="selectedNode.type === 'llm'">
              <div class="config-section">
                <div class="config-section-title">模型设置</div>
                <div class="config-field">
                  <label>模型</label>
                  <select v-model="selectedNode.config.model">
                    <option value="gpt-4">GPT-4</option>
                    <option value="gpt-4-turbo">GPT-4 Turbo</option>
                    <option value="gpt-3.5-turbo">GPT-3.5 Turbo</option>
                    <option value="claude-3-opus">Claude 3 Opus</option>
                    <option value="claude-3-sonnet">Claude 3 Sonnet</option>
                    <option value="qwen-max">通义千问 Max</option>
                    <option value="qwen-plus">通义千问 Plus</option>
                  </select>
                </div>
                <div class="config-field">
                  <label>温度</label>
                  <div class="slider-row">
                    <input type="range" v-model.number="selectedNode.config.temperature" min="0" max="2" step="0.1" />
                    <span class="slider-value">{{ selectedNode.config.temperature }}</span>
                  </div>
                </div>
                <div class="config-field">
                  <label>最大 Token 数</label>
                  <input type="number" v-model.number="selectedNode.config.maxTokens" min="256" max="128000" step="256" />
                </div>
              </div>
              <div class="config-section">
                <div class="config-section-title">提示词</div>
                <div class="config-field">
                  <label>系统提示词</label>
                  <textarea v-model="selectedNode.config.systemPrompt" rows="4" placeholder="输入系统提示词..." />
                </div>
              </div>
            </template>

            <!-- Tool Node Config -->
            <template v-else-if="selectedNode.type === 'tool'">
              <div class="config-section">
                <div class="config-section-title">工具设置</div>
                <div class="config-field">
                  <label>工具类型</label>
                  <select v-model="selectedNode.config.toolType">
                    <option value="web-search">网络搜索</option>
                    <option value="code-executor">代码执行</option>
                    <option value="file-reader">文件读取</option>
                    <option value="database-query">数据库查询</option>
                    <option value="api-caller">API 调用</option>
                    <option value="image-generator">图片生成</option>
                  </select>
                </div>
                <div class="config-field">
                  <label>参数 (JSON)</label>
                  <textarea v-model="selectedNode.config.parameters" rows="4" placeholder='{"key": "value"}' />
                </div>
              </div>
            </template>

            <!-- Condition Node Config -->
            <template v-else-if="selectedNode.type === 'condition'">
              <div class="config-section">
                <div class="config-section-title">条件设置</div>
                <div class="config-field">
                  <label>条件表达式</label>
                  <textarea v-model="selectedNode.config.expression" rows="3" placeholder="例如: input.score > 80" />
                </div>
                <div class="config-field">
                  <label>True 输出标签</label>
                  <input type="text" v-model="selectedNode.config.trueLabel" placeholder="是" />
                </div>
                <div class="config-field">
                  <label>False 输出标签</label>
                  <input type="text" v-model="selectedNode.config.falseLabel" placeholder="否" />
                </div>
              </div>
            </template>

            <!-- Start Node Config -->
            <template v-else-if="selectedNode.type === 'start'">
              <div class="config-section">
                <div class="config-section-title">输入变量</div>
                <div class="config-field">
                  <label>变量定义 (JSON)</label>
                  <textarea v-model="selectedNode.config.inputVariables" rows="4" placeholder='{"query": "用户输入", "context": "上下文"}' />
                </div>
              </div>
            </template>

            <!-- End Node Config -->
            <template v-else-if="selectedNode.type === 'end'">
              <div class="config-section">
                <div class="config-section-title">输出设置</div>
                <div class="config-field">
                  <label>输出格式</label>
                  <select v-model="selectedNode.config.outputFormat">
                    <option value="text">纯文本</option>
                    <option value="json">JSON</option>
                    <option value="markdown">Markdown</option>
                  </select>
                </div>
                <div class="config-field">
                  <label>输出模板</label>
                  <textarea v-model="selectedNode.config.outputTemplate" rows="3" placeholder="可选的输出模板..." />
                </div>
              </div>
            </template>

            <!-- Delay Node Config -->
            <template v-else-if="selectedNode.type === 'delay'">
              <div class="config-section">
                <div class="config-section-title">延迟设置</div>
                <div class="config-field">
                  <label>延迟时长 (秒)</label>
                  <input type="number" v-model.number="selectedNode.config.duration" min="0" step="1" />
                </div>
              </div>
            </template>

            <!-- Notification Node Config -->
            <template v-else-if="selectedNode.type === 'notification'">
              <div class="config-section">
                <div class="config-section-title">通知设置</div>
                <div class="config-field">
                  <label>通知方式</label>
                  <select v-model="selectedNode.config.notifyType">
                    <option value="email">邮件</option>
                    <option value="webhook">Webhook</option>
                    <option value="sms">短信</option>
                    <option value="in-app">站内通知</option>
                  </select>
                </div>
                <div class="config-field">
                  <label>通知目标</label>
                  <input type="text" v-model="selectedNode.config.target" placeholder="输入通知目标..." />
                </div>
                <div class="config-field">
                  <label>通知内容</label>
                  <textarea v-model="selectedNode.config.message" rows="3" placeholder="输入通知内容..." />
                </div>
              </div>
            </template>
          </div>
        </div>
        <div v-else class="config-placeholder">
          <svg class="w-10 h-10 text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          <span>选择节点查看配置</span>
        </div>
      </div>
    </div>

    <!-- Bottom Panel - Console -->
    <div class="bottom-panel" :class="{ collapsed: bottomPanelCollapsed }">
      <div class="bottom-panel-header" @click="bottomPanelCollapsed = !bottomPanelCollapsed">
        <div class="bottom-panel-title">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          控制台
          <span v-if="consoleLogs.length" class="log-count">{{ consoleLogs.length }}</span>
        </div>
        <div class="bottom-panel-actions">
          <button class="bottom-action-btn" title="清空日志" @click.stop="clearLogs">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
          <svg
            class="w-4 h-4 transition-transform duration-200"
            :class="{ 'rotate-180': bottomPanelCollapsed }"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>
      <div v-show="!bottomPanelCollapsed" class="bottom-panel-body">
        <div v-if="consoleLogs.length === 0" class="console-empty">
          暂无日志，点击运行按钮测试 Agent
        </div>
        <div
          v-for="(log, index) in consoleLogs"
          :key="index"
          class="console-log"
          :class="'log-' + log.level"
        >
          <span class="log-time">{{ log.time }}</span>
          <span class="log-level" :class="'level-' + log.level">{{ log.level.toUpperCase() }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
// ============================================================
// Types
// ============================================================

interface PortDefinition {
  name: string
  label: string
  index: number
}

interface CanvasNode {
  id: string
  type: string
  label: string
  x: number
  y: number
  config: Record<string, any>
  inputs: PortDefinition[]
  outputs: PortDefinition[]
}

interface Connection {
  id: string
  fromNodeId: string
  fromPort: string
  toNodeId: string
  toPort: string
}

interface ConsoleLog {
  time: string
  level: 'info' | 'warn' | 'error' | 'success'
  message: string
}

interface HistoryEntry {
  nodes: CanvasNode[]
  connections: Connection[]
}

interface NodeTypeDefinition {
  type: string
  name: string
  description: string
  color: string
  iconComponent: ReturnType<typeof defineComponent>
  defaultConfig: Record<string, any>
  defaultInputs: PortDefinition[]
  defaultOutputs: PortDefinition[]
}

// ============================================================
// Icon Components (inline SVG)
// ============================================================

const IconLLM = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z' })
    ])
  }
})

const IconTool = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' }),
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z' })
    ])
  }
})

const IconCondition = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M8 9l4-4 4 4m0 6l-4 4-4-4' })
    ])
  }
})

const IconStart = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { d: 'M8 5v14l11-7z' })
    ])
  }
})

const IconEnd = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'currentColor', viewBox: '0 0 24 24' }, [
      h('rect', { x: '6', y: '6', width: '12', height: '12', rx: '2' })
    ])
  }
})

const IconDelay = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' })
    ])
  }
})

const IconNotification = defineComponent({
  render() {
    return h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9' })
    ])
  }
})

// ============================================================
// Node Type Definitions
// ============================================================

const nodeTypes: NodeTypeDefinition[] = [
  {
    type: 'start',
    name: '开始节点',
    description: '工作流的起始点，定义输入变量',
    color: '#22c55e',
    iconComponent: IconStart,
    defaultConfig: { inputVariables: '{}' },
    defaultInputs: [],
    defaultOutputs: [{ name: 'output', label: '输出', index: 0 }],
  },
  {
    type: 'llm',
    name: 'LLM 节点',
    description: '调用 AI 大语言模型处理',
    color: '#3b82f6',
    iconComponent: IconLLM,
    defaultConfig: { model: 'gpt-4', temperature: 0.7, maxTokens: 2048, systemPrompt: '' },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [{ name: 'output', label: '输出', index: 0 }],
  },
  {
    type: 'tool',
    name: '工具节点',
    description: '调用外部工具或服务',
    color: '#10b981',
    iconComponent: IconTool,
    defaultConfig: { toolType: 'web-search', parameters: '{}' },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [{ name: 'output', label: '输出', index: 0 }],
  },
  {
    type: 'condition',
    name: '条件节点',
    description: '根据条件进行分支判断',
    color: '#f59e0b',
    iconComponent: IconCondition,
    defaultConfig: { expression: '', trueLabel: '是', falseLabel: '否' },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [
      { name: 'true', label: '是', index: 0 },
      { name: 'false', label: '否', index: 1 },
    ],
  },
  {
    type: 'delay',
    name: '延迟节点',
    description: '等待指定时间后继续执行',
    color: '#8b5cf6',
    iconComponent: IconDelay,
    defaultConfig: { duration: 5 },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [{ name: 'output', label: '输出', index: 0 }],
  },
  {
    type: 'notification',
    name: '通知节点',
    description: '发送通知消息',
    color: '#06b6d4',
    iconComponent: IconNotification,
    defaultConfig: { notifyType: 'email', target: '', message: '' },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [{ name: 'output', label: '输出', index: 0 }],
  },
  {
    type: 'end',
    name: '结束节点',
    description: '工作流的结束点，定义输出格式',
    color: '#ef4444',
    iconComponent: IconEnd,
    defaultConfig: { outputFormat: 'text', outputTemplate: '' },
    defaultInputs: [{ name: 'input', label: '输入', index: 0 }],
    defaultOutputs: [],
  },
]

// ============================================================
// Router
// ============================================================

const router = useRouter()

function goBack() {
  router.push('/agents')
}

// ============================================================
// State
// ============================================================

const agentName = ref('未命名 Agent')

// Canvas state
const canvas = reactive({
  zoom: 1,
  panX: 0,
  panY: 0,
})

const nodes = ref<CanvasNode[]>([])
const connections = ref<Connection[]>([])
const selectedNodeId = ref<string | null>(null)
const selectedConnectionId = ref<string | null>(null)
const editingNodeId = ref<string | null>(null)

// Panel states
const leftPanelCollapsed = ref(false)
const bottomPanelCollapsed = ref(true)

// Console
const consoleLogs = ref<ConsoleLog[]>([])

// Running state
const runningNodeIds = ref<Set<string>>(new Set())
const completedNodeIds = ref<Set<string>>(new Set())
const failedNodeIds = ref<Set<string>>(new Set())

// Context menu
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  type: 'canvas' as 'canvas' | 'node' | 'connection',
  targetNode: null as CanvasNode | null,
  targetConnectionId: null as string | null,
})

// History (undo/redo)
const history = ref<HistoryEntry[]>([])
const historyIndex = ref(-1)
const maxHistory = 50

// Drag state
let isDraggingNode = false
let isDraggingCanvas = false
let isConnecting = false
let dragStartX = 0
let dragStartY = 0
let dragNodeStartX = 0
let dragNodeStartY = 0
let panStartX = 0
let panStartY = 0
let connectingFromNodeId = ''
let connectingFromPort = ''
let connectingFromType: 'input' | 'output' = 'output'
let connectingMouseX = 0
let connectingMouseY = 0
let clipboardNode: CanvasNode | null = null

// Refs
const canvasContainerRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// ============================================================
// Computed
// ============================================================

const selectedNode = computed(() => {
  if (!selectedNodeId.value) return null
  return nodes.value.find(n => n.id === selectedNodeId.value) || null
})

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

const canvasTransformStyle = computed(() => ({
  transform: `translate(${canvas.panX}px, ${canvas.panY}px) scale(${canvas.zoom})`,
  transformOrigin: '0 0',
}))

const gridOffsetStyle = computed(() => ({
  transform: `translate(${(canvas.panX % 100) / canvas.zoom}px, ${(canvas.panY % 100) / canvas.zoom}px)`,
}))

const tempConnection = computed(() => {
  if (!isConnecting) return null
  const fromNode = nodes.value.find(n => n.id === connectingFromNodeId)
  if (!fromNode) return null

  const port = connectingFromType === 'output'
    ? fromNode.outputs.find(p => p.name === connectingFromPort)
    : fromNode.inputs.find(p => p.name === connectingFromPort)
  if (!port) return null

  const nodeW = 180

  let startX: number, startY: number
  if (connectingFromType === 'output') {
    startX = fromNode.x + nodeW
    startY = fromNode.y + getOutputPortTop(fromNode, port.index) + 6
  } else {
    startX = fromNode.x
    startY = fromNode.y + getInputPortTop(fromNode, port.index) + 6
  }

  // Convert mouse coords to canvas coords
  const endX = (connectingMouseX - canvas.panX) / canvas.zoom
  const endY = (connectingMouseY - canvas.panY) / canvas.zoom

  const dx = Math.abs(endX - startX) * 0.5
  return {
    path: `M ${startX} ${startY} C ${startX + dx} ${startY}, ${endX - dx} ${endY}, ${endX} ${endY}`,
  }
})

// ============================================================
// Helpers
// ============================================================

let nodeIdCounter = 0
function generateNodeId(): string {
  nodeIdCounter++
  return `node_${Date.now()}_${nodeIdCounter}`
}

let connectionIdCounter = 0
function generateConnectionId(): string {
  connectionIdCounter++
  return `conn_${Date.now()}_${connectionIdCounter}`
}

function getNodeTypeConfig(type: string): NodeTypeDefinition | undefined {
  return nodeTypes.find(t => t.type === type)
}

function getNodeColor(type: string): string {
  return getNodeTypeConfig(type)?.color || '#6366f1'
}

function getNodeHeight(node: CanvasNode): number {
  const inputPorts = node.inputs.length
  const outputPorts = node.outputs.length
  const maxPorts = Math.max(inputPorts, outputPorts)
  return Math.max(60, 56 + maxPorts * 28)
}

function getInputPortTop(_node: CanvasNode, index: number): number {
  return 28 + index * 28
}

function getOutputPortTop(_node: CanvasNode, index: number): number {
  return 28 + index * 28
}

function getPortPosition(node: CanvasNode, portName: string, portType: 'input' | 'output'): { x: number; y: number } {
  const nodeW = 180
  const ports = portType === 'input' ? node.inputs : node.outputs
  const port = ports.find(p => p.name === portName)
  if (!port) return { x: node.x, y: node.y }

  const top = portType === 'input' ? getInputPortTop(node, port.index) : getOutputPortTop(node, port.index)
  const x = portType === 'input' ? node.x : node.x + nodeW
  const y = node.y + top + 6
  return { x, y }
}

function getConnectionPath(conn: Connection): string {
  const fromNode = nodes.value.find(n => n.id === conn.fromNodeId)
  const toNode = nodes.value.find(n => n.id === conn.toNodeId)
  if (!fromNode || !toNode) return ''

  const start = getPortPosition(fromNode, conn.fromPort, 'output')
  const end = getPortPosition(toNode, conn.toPort, 'input')

  const dx = Math.abs(end.x - start.x) * 0.5
  return `M ${start.x} ${start.y} C ${start.x + dx} ${start.y}, ${end.x - dx} ${end.y}, ${end.x} ${end.y}`
}

function getTimestamp(): string {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

// ============================================================
// History (Undo/Redo)
// ============================================================

function pushHistory() {
  const entry: HistoryEntry = {
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    connections: JSON.parse(JSON.stringify(connections.value)),
  }
  // Remove future entries if we are not at the end
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  history.value.push(entry)
  if (history.value.length > maxHistory) {
    history.value.shift()
  }
  historyIndex.value = history.value.length - 1
}

function undo() {
  if (!canUndo.value) return
  historyIndex.value--
  restoreHistory()
}

function redo() {
  if (!canRedo.value) return
  historyIndex.value++
  restoreHistory()
}

function restoreHistory() {
  const entry = history.value[historyIndex.value]
  if (!entry) return
  nodes.value = JSON.parse(JSON.stringify(entry.nodes))
  connections.value = JSON.parse(JSON.stringify(entry.connections))
  selectedNodeId.value = null
  selectedConnectionId.value = null
}

// ============================================================
// Node Operations
// ============================================================

function createNode(type: string, x: number, y: number): CanvasNode {
  const typeDef = getNodeTypeConfig(type)
  if (!typeDef) throw new Error(`Unknown node type: ${type}`)

  return {
    id: generateNodeId(),
    type,
    label: typeDef.name,
    x,
    y,
    config: { ...typeDef.defaultConfig },
    inputs: typeDef.defaultInputs.map(p => ({ ...p })),
    outputs: typeDef.defaultOutputs.map(p => ({ ...p })),
  }
}

function addNode(type: string, x: number, y: number) {
  const node = createNode(type, x, y)
  nodes.value.push(node)
  pushHistory()
  addLog('info', `添加节点: ${node.label}`)
  return node
}

function deleteNode(id: string) {
  const node = nodes.value.find(n => n.id === id)
  if (!node) return
  nodes.value = nodes.value.filter(n => n.id !== id)
  connections.value = connections.value.filter(c => c.fromNodeId !== id && c.toNodeId !== id)
  if (selectedNodeId.value === id) selectedNodeId.value = null
  pushHistory()
  addLog('info', `删除节点: ${node.label}`)
}

function duplicateNode(node: CanvasNode) {
  const newNode = createNode(node.type, node.x + 40, node.y + 40)
  newNode.label = node.label + ' (副本)'
  newNode.config = JSON.parse(JSON.stringify(node.config))
  nodes.value.push(newNode)
  pushHistory()
  addLog('info', `复制节点: ${newNode.label}`)
}

function editNode(node: CanvasNode) {
  selectedNodeId.value = node.id
}

function editNodeName(node: CanvasNode) {
  editingNodeId.value = node.id
}

function deselectNode() {
  selectedNodeId.value = null
  selectedConnectionId.value = null
}

function selectConnection(id: string) {
  selectedConnectionId.value = id
  selectedNodeId.value = null
}

function deleteConnection(id: string) {
  connections.value = connections.value.filter(c => c.id !== id)
  if (selectedConnectionId.value === id) selectedConnectionId.value = null
  pushHistory()
  addLog('info', '删除连线')
}

function pasteNode() {
  if (!clipboardNode) return
  duplicateNode(clipboardNode)
}

// ============================================================
// Canvas Event Handlers
// ============================================================

function onDragStart(event: DragEvent, nodeType: NodeTypeDefinition) {
  event.dataTransfer!.setData('nodeType', nodeType.type)
  event.dataTransfer!.effectAllowed = 'copy'
}

function onCanvasDrop(event: DragEvent) {
  const nodeType = event.dataTransfer!.getData('nodeType')
  if (!nodeType) return

  const rect = canvasContainerRef.value!.getBoundingClientRect()
  const x = (event.clientX - rect.left - canvas.panX) / canvas.zoom
  const y = (event.clientY - rect.top - canvas.panY) / canvas.zoom

  const node = addNode(nodeType, x - 90, y - 30)
  selectedNodeId.value = node.id
  hideContextMenu()
}

function onCanvasMouseDown(event: MouseEvent) {
  hideContextMenu()
  deselectNode()

  // Start panning
  isDraggingCanvas = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  panStartX = canvas.panX
  panStartY = canvas.panY
}

function onCanvasMouseMove(event: MouseEvent) {
  if (isDraggingCanvas) {
    canvas.panX = panStartX + (event.clientX - dragStartX)
    canvas.panY = panStartY + (event.clientY - dragStartY)
    return
  }

  if (isDraggingNode && selectedNodeId.value) {
    const node = nodes.value.find(n => n.id === selectedNodeId.value)
    if (node) {
      node.x = dragNodeStartX + (event.clientX - dragStartX) / canvas.zoom
      node.y = dragNodeStartY + (event.clientY - dragStartY) / canvas.zoom
    }
    return
  }

  if (isConnecting) {
    const rect = canvasContainerRef.value!.getBoundingClientRect()
    connectingMouseX = event.clientX - rect.left
    connectingMouseY = event.clientY - rect.top
    return
  }
}

function onCanvasMouseUp(_event: MouseEvent) {
  if (isDraggingNode && selectedNodeId.value) {
    pushHistory()
  }
  isDraggingCanvas = false
  isDraggingNode = false
}

function onNodeMouseDown(event: MouseEvent, node: CanvasNode) {
  hideContextMenu()
  selectedNodeId.value = node.id
  selectedConnectionId.value = null

  isDraggingNode = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  dragNodeStartX = node.x
  dragNodeStartY = node.y
}

function onPortMouseDown(event: MouseEvent, node: CanvasNode, port: PortDefinition, portType: 'input' | 'output') {
  event.stopPropagation()
  isConnecting = true
  connectingFromNodeId = node.id
  connectingFromPort = port.name
  connectingFromType = portType

  const rect = canvasContainerRef.value!.getBoundingClientRect()
  connectingMouseX = event.clientX - rect.left
  connectingMouseY = event.clientY - rect.top
}

function onCanvasWheel(event: WheelEvent) {
  const rect = canvasContainerRef.value!.getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const mouseY = event.clientY - rect.top

  const oldZoom = canvas.zoom
  const delta = event.deltaY > 0 ? -0.1 : 0.1
  canvas.zoom = Math.min(3, Math.max(0.2, canvas.zoom + delta))

  // Zoom towards mouse position
  canvas.panX = mouseX - (mouseX - canvas.panX) * (canvas.zoom / oldZoom)
  canvas.panY = mouseY - (mouseY - canvas.panY) * (canvas.zoom / oldZoom)
}

// ============================================================
// Connection Completion (on mouseup when connecting)
// ============================================================

function handleConnectionEnd(event: MouseEvent) {
  if (!isConnecting) return

  const rect = canvasContainerRef.value!.getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const mouseY = event.clientY - rect.top

  // Convert to canvas coords
  const canvasX = (mouseX - canvas.panX) / canvas.zoom
  const canvasY = (mouseY - canvas.panY) / canvas.zoom

  // Find target port
  for (const node of nodes.value) {
    if (node.id === connectingFromNodeId) continue

    // Check output ports (if we started from input)
    if (connectingFromType === 'input') {
      for (const port of node.outputs) {
        const pos = getPortPosition(node, port.name, 'output')
        const dist = Math.sqrt((canvasX - pos.x) ** 2 + (canvasY - pos.y) ** 2)
        if (dist < 15) {
          // Create connection: from this node's output to the original input
          createConnection(node.id, port.name, connectingFromNodeId, connectingFromPort)
          break
        }
      }
    }

    // Check input ports (if we started from output)
    if (connectingFromType === 'output') {
      for (const port of node.inputs) {
        const pos = getPortPosition(node, port.name, 'input')
        const dist = Math.sqrt((canvasX - pos.x) ** 2 + (canvasY - pos.y) ** 2)
        if (dist < 15) {
          createConnection(connectingFromNodeId, connectingFromPort, node.id, port.name)
          break
        }
      }
    }
  }

  isConnecting = false
}

function createConnection(fromNodeId: string, fromPort: string, toNodeId: string, toPort: string) {
  // Prevent duplicate connections
  const exists = connections.value.some(
    c => c.fromNodeId === fromNodeId && c.fromPort === fromPort && c.toNodeId === toNodeId && c.toPort === toPort
  )
  if (exists) return

  // Prevent self-connections
  if (fromNodeId === toNodeId) return

  // Prevent connecting to an input that already has a connection (except for condition nodes which have two)
  const toNode = nodes.value.find(n => n.id === toNodeId)
  if (toNode && toNode.type !== 'condition') {
    const existingInput = connections.value.find(c => c.toNodeId === toNodeId && c.toPort === toPort)
    if (existingInput) {
      // Replace existing connection
      connections.value = connections.value.filter(c => c.id !== existingInput.id)
    }
  }

  const conn: Connection = {
    id: generateConnectionId(),
    fromNodeId,
    fromPort,
    toNodeId,
    toPort,
  }
  connections.value.push(conn)
  pushHistory()
  addLog('info', `创建连线: ${fromNodeId}.${fromPort} -> ${toNodeId}.${toPort}`)
}

// ============================================================
// Context Menu
// ============================================================

function onCanvasContextMenu(event: MouseEvent) {
  contextMenu.visible = true
  contextMenu.x = event.offsetX
  contextMenu.y = event.offsetY
  contextMenu.type = 'canvas'
  contextMenu.targetNode = null
  contextMenu.targetConnectionId = null
}

function onNodeContextMenu(event: MouseEvent, node: CanvasNode) {
  contextMenu.visible = true
  contextMenu.x = event.offsetX
  contextMenu.y = event.offsetY
  contextMenu.type = 'node'
  contextMenu.targetNode = node
  contextMenu.targetConnectionId = null
  clipboardNode = node
}

function onConnectionContextMenu(event: MouseEvent, connectionId: string) {
  contextMenu.visible = true
  contextMenu.x = event.offsetX
  contextMenu.y = event.offsetY
  contextMenu.type = 'connection'
  contextMenu.targetNode = null
  contextMenu.targetConnectionId = connectionId
}

function hideContextMenu() {
  contextMenu.visible = false
}

// ============================================================
// Zoom Controls
// ============================================================

function zoomIn() {
  canvas.zoom = Math.min(3, canvas.zoom + 0.15)
}

function zoomOut() {
  canvas.zoom = Math.max(0.2, canvas.zoom - 0.15)
}

function resetZoom() {
  canvas.zoom = 1
  canvas.panX = 0
  canvas.panY = 0
}

function fitView() {
  if (nodes.value.length === 0) {
    resetZoom()
    return
  }
  const container = canvasContainerRef.value
  if (!container) return

  const padding = 80
  const minX = Math.min(...nodes.value.map(n => n.x))
  const minY = Math.min(...nodes.value.map(n => n.y))
  const maxX = Math.max(...nodes.value.map(n => n.x + 180))
  const maxY = Math.max(...nodes.value.map(n => n.y + getNodeHeight(n)))

  const graphW = maxX - minX + padding * 2
  const graphH = maxY - minY + padding * 2

  const scaleX = container.clientWidth / graphW
  const scaleY = container.clientHeight / graphH
  canvas.zoom = Math.min(1.5, Math.max(0.3, Math.min(scaleX, scaleY)))

  canvas.panX = (container.clientWidth - graphW * canvas.zoom) / 2 - (minX - padding) * canvas.zoom
  canvas.panY = (container.clientHeight - graphH * canvas.zoom) / 2 - (minY - padding) * canvas.zoom
}

// ============================================================
// Console
// ============================================================

function addLog(level: ConsoleLog['level'], message: string) {
  consoleLogs.value.push({
    time: getTimestamp(),
    level,
    message,
  })
  // Auto scroll to bottom
  nextTick(() => {
    const body = document.querySelector('.bottom-panel-body') as HTMLElement
    if (body) body.scrollTop = body.scrollHeight
  })
}

function clearLogs() {
  consoleLogs.value = []
}

// ============================================================
// Toolbar Actions
// ============================================================

function handleNameBlur() {
  if (!agentName.value.trim()) {
    agentName.value = '未命名 Agent'
  }
}

function saveAgent() {
  const graphData = {
    name: agentName.value,
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: n.type,
      label: n.label,
      x: Math.round(n.x),
      y: Math.round(n.y),
      config: n.config,
    })),
    connections: connections.value.map(c => ({
      id: c.id,
      fromNodeId: c.fromNodeId,
      fromPort: c.fromPort,
      toNodeId: c.toNodeId,
      toPort: c.toPort,
    })),
  }

  localStorage.setItem('agent_designer_data', JSON.stringify(graphData))
  message.success('Agent 已保存')
  addLog('success', `保存成功: ${agentName.value}`)
}

function exportJson() {
  const graphData = {
    name: agentName.value,
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: n.type,
      label: n.label,
      x: Math.round(n.x),
      y: Math.round(n.y),
      config: n.config,
    })),
    connections: connections.value.map(c => ({
      id: c.id,
      fromNodeId: c.fromNodeId,
      fromPort: c.fromPort,
      toNodeId: c.toNodeId,
      toPort: c.toPort,
    })),
  }

  const blob = new Blob([JSON.stringify(graphData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${agentName.value || 'agent-design'}.json`
  a.click()
  URL.revokeObjectURL(url)
  addLog('info', '导出 JSON 文件')
}

function triggerImport() {
  fileInputRef.value?.click()
}

function handleImport(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader: FileReader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target?.result as string)
      if (data.name) agentName.value = data.name
      if (data.nodes) {
        nodes.value = data.nodes.map((n: any) => {
          const typeDef = getNodeTypeConfig(n.type)
          return {
            id: n.id || generateNodeId(),
            type: n.type,
            label: n.label || typeDef?.name || n.type,
            x: n.x || 100,
            y: n.y || 100,
            config: n.config || typeDef?.defaultConfig || {},
            inputs: typeDef?.defaultInputs.map(p => ({ ...p })) || [],
            outputs: typeDef?.defaultOutputs.map(p => ({ ...p })) || [],
          }
        })
      }
      if (data.connections) {
        connections.value = data.connections.map((c: any) => ({
          id: c.id || generateConnectionId(),
          fromNodeId: c.fromNodeId,
          fromPort: c.fromPort,
          toNodeId: c.toNodeId,
          toPort: c.toPort,
        }))
      }
      pushHistory()
      addLog('success', '导入成功')
      message.success('导入成功')
      fitView()
    } catch {
      message.error('JSON 格式错误')
      addLog('error', '导入失败: JSON 格式错误')
    }
  }
  reader.readAsText(file)
  // Reset input
  input.value = ''
}

async function runAgent() {
  if (nodes.value.length === 0) {
    message.warning('请先添加节点')
    return
  }

  // Check for start node
  const startNode = nodes.value.find(n => n.type === 'start')
  if (!startNode) {
    message.warning('请添加开始节点')
    addLog('warn', '运行失败: 缺少开始节点')
    return
  }

  bottomPanelCollapsed.value = false
  runningNodeIds.value.clear()
  completedNodeIds.value.clear()
  failedNodeIds.value.clear()

  addLog('info', '开始运行 Agent...')
  addLog('info', `节点数量: ${nodes.value.length}, 连线数量: ${connections.value.length}`)

  // Simulate execution
  const executionOrder = getExecutionOrder(startNode.id)
  for (const nodeId of executionOrder) {
    const node = nodes.value.find(n => n.id === nodeId)
    if (!node) continue

    runningNodeIds.value.add(nodeId)
    addLog('info', `执行节点: ${node.label} (${node.type})`)

    await new Promise(resolve => setTimeout(resolve, 800 + Math.random() * 700))

    runningNodeIds.value.delete(nodeId)
    // Simulate occasional failure
    if (Math.random() < 0.05) {
      failedNodeIds.value.add(nodeId)
      addLog('error', `节点执行失败: ${node.label}`)
    } else {
      completedNodeIds.value.add(nodeId)
      addLog('success', `节点完成: ${node.label}`)
    }
  }

  const endNode = nodes.value.find(n => n.type === 'end')
  if (endNode && completedNodeIds.value.has(endNode.id)) {
    addLog('success', 'Agent 运行完成!')
    message.success('运行完成')
  } else if (failedNodeIds.value.size > 0) {
    addLog('error', 'Agent 运行失败')
    message.error('运行失败')
  } else {
    addLog('warn', 'Agent 运行结束（未到达结束节点）')
    message.warning('运行结束（未到达结束节点）')
  }
}

function getExecutionOrder(startNodeId: string): string[] {
  const order: string[] = []
  const visited = new Set<string>()

  function dfs(nodeId: string) {
    if (visited.has(nodeId)) return
    visited.add(nodeId)
    order.push(nodeId)

    const outConns = connections.value.filter(c => c.fromNodeId === nodeId)
    for (const conn of outConns) {
      dfs(conn.toNodeId)
    }
  }

  dfs(startNodeId)
  return order
}

// ============================================================
// Keyboard Shortcuts
// ============================================================

function onKeyDown(event: KeyboardEvent) {
  // Don't handle shortcuts when editing text
  if (editingNodeId.value) return
  if ((event.target as HTMLElement).tagName === 'INPUT' || (event.target as HTMLElement).tagName === 'TEXTAREA' || (event.target as HTMLElement).tagName === 'SELECT') return

  // Delete
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedNodeId.value) {
      deleteNode(selectedNodeId.value)
    } else if (selectedConnectionId.value) {
      deleteConnection(selectedConnectionId.value)
    }
    return
  }

  // Ctrl+Z - Undo
  if (event.ctrlKey && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    undo()
    return
  }

  // Ctrl+Y or Ctrl+Shift+Z - Redo
  if ((event.ctrlKey && event.key === 'y') || (event.ctrlKey && event.shiftKey && event.key === 'z')) {
    event.preventDefault()
    redo()
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
    if (node) clipboardNode = node
    return
  }

  // Ctrl+V - Paste node
  if (event.ctrlKey && event.key === 'v') {
    if (clipboardNode) pasteNode()
    return
  }

  // Escape
  if (event.key === 'Escape') {
    hideContextMenu()
    deselectNode()
    if (isConnecting) isConnecting = false
    return
  }
}

// ============================================================
// Global mouseup for connection completion
// ============================================================

function onGlobalMouseUp(event: MouseEvent) {
  if (isConnecting) {
    handleConnectionEnd(event)
  }
}

function onGlobalClick(event: MouseEvent) {
  if (contextMenu.visible) {
    const menu = document.querySelector('.context-menu')
    if (menu && !menu.contains(event.target as Node)) {
      hideContextMenu()
    }
  }
}

// ============================================================
// Lifecycle
// ============================================================

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('mouseup', onGlobalMouseUp)
  window.addEventListener('click', onGlobalClick)

  // Try to load saved data
  try {
    const saved = localStorage.getItem('agent_designer_data')
    if (saved) {
      const data = JSON.parse(saved)
      if (data.name) agentName.value = data.name
      if (data.nodes && data.nodes.length > 0) {
        nodes.value = data.nodes.map((n: any) => {
          const typeDef = getNodeTypeConfig(n.type)
          return {
            id: n.id || generateNodeId(),
            type: n.type,
            label: n.label || typeDef?.name || n.type,
            x: n.x || 100,
            y: n.y || 100,
            config: n.config || typeDef?.defaultConfig || {},
            inputs: typeDef?.defaultInputs.map(p => ({ ...p })) || [],
            outputs: typeDef?.defaultOutputs.map(p => ({ ...p })) || [],
          }
        })
        if (data.connections) {
          connections.value = data.connections.map((c: any) => ({
            id: c.id || generateConnectionId(),
            fromNodeId: c.fromNodeId,
            fromPort: c.fromPort,
            toNodeId: c.toNodeId,
            toPort: c.toPort,
          }))
        }
        pushHistory()
        nextTick(() => fitView())
      } else {
        // Create default start + end nodes
        createDefaultGraph()
      }
    } else {
      createDefaultGraph()
    }
  } catch {
    createDefaultGraph()
  }
})

function createDefaultGraph() {
  addNode('start', 200, 200)
  addNode('end', 700, 200)
  // Don't auto-connect; let user build their graph
  pushHistory()
}

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('mouseup', onGlobalMouseUp)
  window.removeEventListener('click', onGlobalClick)
})
</script>

<style scoped>
/* ============================================================
// Layout
// ============================================================ */

.agent-designer-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0f0f1a;
  color: #e2e8f0;
  overflow: hidden;
  user-select: none;
}

/* ============================================================
// Toolbar
// ============================================================ */

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: #16162a;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  z-index: 10;
}

.toolbar-left,
.toolbar-center,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-center {
  flex: 1;
  justify-content: center;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 6px;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.toolbar-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.toolbar-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.toolbar-btn.btn-save {
  background: #3b82f6;
  color: white;
  font-weight: 500;
}

.toolbar-btn.btn-save:hover {
  background: #2563eb;
}

.toolbar-btn.btn-run {
  background: #22c55e;
  color: white;
  font-weight: 500;
}

.toolbar-btn.btn-run:hover {
  background: #16a34a;
}

.zoom-label {
  font-size: 12px;
  color: #64748b;
  min-width: 40px;
  text-align: center;
}

.agent-name-input {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 6px 12px;
  color: #e2e8f0;
  font-size: 15px;
  font-weight: 600;
  text-align: center;
  width: 240px;
  transition: all 0.15s;
}

.agent-name-input:hover {
  border-color: rgba(255, 255, 255, 0.1);
}

.agent-name-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}

/* ============================================================
// Body Layout
// ============================================================ */

.designer-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ============================================================
// Left Panel - Node Palette
// ============================================================ */

.left-panel {
  width: 260px;
  min-width: 260px;
  background: #16162a;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  transition: width 0.2s, min-width 0.2s;
}

.left-panel.collapsed {
  width: 40px;
  min-width: 40px;
}

.panel-toggle {
  position: absolute;
  top: 12px;
  right: -14px;
  width: 28px;
  height: 28px;
  background: #1e1e3a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 5;
  color: #64748b;
  transition: all 0.15s;
}

.panel-toggle:hover {
  background: #2a2a4a;
  color: #e2e8f0;
}

.panel-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.panel-title {
  padding: 16px 16px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.node-palette {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 16px;
}

.node-palette::-webkit-scrollbar {
  width: 4px;
}

.node-palette::-webkit-scrollbar-track {
  background: transparent;
}

.node-palette::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.15s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.palette-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

.palette-item:active {
  cursor: grabbing;
  transform: scale(0.98);
}

.palette-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.palette-info {
  min-width: 0;
}

.palette-name {
  font-size: 13px;
  font-weight: 500;
  color: #e2e8f0;
}

.palette-desc {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ============================================================
// Center Panel - Canvas
// ============================================================ */

.center-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #1a1a2e;
  cursor: grab;
}

.center-panel:active {
  cursor: grabbing;
}

.canvas-viewport {
  position: absolute;
  top: 0;
  left: 0;
  width: 5000px;
  height: 5000px;
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

.connection-line.temp {
  stroke: #6366f1;
  stroke-width: 2;
  stroke-dasharray: 6 4;
  opacity: 0.6;
  pointer-events: none;
}

/* ============================================================
// Canvas Nodes
// ============================================================ */

.canvas-node {
  position: absolute;
  width: 180px;
  z-index: 2;
  cursor: move;
  transition: box-shadow 0.15s;
}

.canvas-node.selected {
  z-index: 3;
}

.node-body {
  background: #1e1e3a;
  border: 2px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 10px 14px;
  transition: all 0.15s;
  min-height: 48px;
}

.canvas-node.selected .node-body {
  border-color: var(--node-color);
  box-shadow: 0 0 0 2px var(--node-color), 0 4px 20px rgba(0, 0, 0, 0.3);
}

.canvas-node.running .node-body {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px #3b82f6, 0 0 20px rgba(59, 130, 246, 0.3);
  animation: pulse-running 1.5s ease-in-out infinite;
}

.canvas-node.completed .node-body {
  border-color: #22c55e;
  box-shadow: 0 0 0 2px #22c55e, 0 0 12px rgba(34, 197, 94, 0.2);
}

.canvas-node.failed .node-body {
  border-color: #ef4444;
  box-shadow: 0 0 0 2px #ef4444, 0 0 12px rgba(239, 68, 68, 0.2);
}

@keyframes pulse-running {
  0%, 100% { box-shadow: 0 0 0 2px #3b82f6, 0 0 20px rgba(59, 130, 246, 0.3); }
  50% { box-shadow: 0 0 0 4px #3b82f6, 0 0 30px rgba(59, 130, 246, 0.5); }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-type-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--node-color);
  color: white;
  font-size: 12px;
}

.node-label {
  font-size: 13px;
  font-weight: 500;
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.node-label-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--node-color);
  border-radius: 4px;
  padding: 2px 6px;
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  outline: none;
  min-width: 0;
}

.node-type-name {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
}

/* ============================================================
// Ports
// ============================================================ */

.node-port {
  position: absolute;
  display: flex;
  align-items: center;
  cursor: crosshair;
  z-index: 4;
}

.input-port {
  left: -8px;
  flex-direction: row;
}

.output-port {
  right: -8px;
  flex-direction: row-reverse;
}

.port-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #1e1e3a;
  border: 2px solid #475569;
  transition: all 0.15s;
  flex-shrink: 0;
}

.node-port:hover .port-dot {
  border-color: var(--node-color);
  background: var(--node-color);
  transform: scale(1.3);
}

.port-label {
  font-size: 10px;
  color: #64748b;
  margin: 0 4px;
  white-space: nowrap;
  pointer-events: none;
}

/* ============================================================
// Context Menu
// ============================================================ */

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
// Right Panel - Configuration
// ============================================================ */

.right-panel {
  width: 300px;
  min-width: 300px;
  background: #16162a;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s, min-width 0.2s;
  overflow: hidden;
}

.right-panel.collapsed {
  width: 0;
  min-width: 0;
  border-left: none;
}

.config-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.config-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.config-type-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.config-node-name {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.config-node-type {
  font-size: 12px;
  color: #64748b;
}

.config-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.config-close:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.config-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.config-body::-webkit-scrollbar {
  width: 4px;
}

.config-body::-webkit-scrollbar-track {
  background: transparent;
}

.config-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.config-section {
  margin-bottom: 20px;
}

.config-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}

.config-field {
  margin-bottom: 14px;
}

.config-field label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
  font-weight: 500;
}

.config-field input[type="text"],
.config-field input[type="number"],
.config-field select,
.config-field textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 8px 10px;
  color: #e2e8f0;
  font-size: 13px;
  transition: border-color 0.15s;
  font-family: inherit;
}

.config-field input:focus,
.config-field select:focus,
.config-field textarea:focus {
  outline: none;
  border-color: #3b82f6;
}

.config-field textarea {
  resize: vertical;
  min-height: 60px;
}

.config-field select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 30px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.slider-row input[type="range"] {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  outline: none;
  border: none;
  padding: 0;
}

.slider-row input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
}

.slider-value {
  font-size: 13px;
  color: #94a3b8;
  min-width: 30px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.config-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #475569;
  font-size: 13px;
  padding: 20px;
  text-align: center;
}

/* ============================================================
// Bottom Panel - Console
// ============================================================ */

.bottom-panel {
  background: #12122a;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  transition: height 0.2s;
  overflow: hidden;
}

.bottom-panel.collapsed {
  height: 36px !important;
}

.bottom-panel:not(.collapsed) {
  height: 200px;
}

.bottom-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 36px;
  cursor: pointer;
  flex-shrink: 0;
}

.bottom-panel-header:hover {
  background: rgba(255, 255, 255, 0.02);
}

.bottom-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.log-count {
  background: #3b82f6;
  color: white;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
}

.bottom-panel-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.bottom-action-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.bottom-action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.bottom-panel-body {
  height: calc(100% - 36px);
  overflow-y: auto;
  padding: 8px 12px;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.bottom-panel-body::-webkit-scrollbar {
  width: 4px;
}

.bottom-panel-body::-webkit-scrollbar-track {
  background: transparent;
}

.bottom-panel-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.console-empty {
  color: #475569;
  text-align: center;
  padding: 20px;
  font-family: inherit;
}

.console-log {
  display: flex;
  gap: 10px;
  padding: 2px 0;
  border-radius: 3px;
}

.console-log:hover {
  background: rgba(255, 255, 255, 0.02);
}

.log-time {
  color: #475569;
  flex-shrink: 0;
}

.log-level {
  flex-shrink: 0;
  font-weight: 600;
  width: 52px;
}

.level-info { color: #3b82f6; }
.level-warn { color: #f59e0b; }
.level-error { color: #ef4444; }
.level-success { color: #22c55e; }

.log-message {
  color: #94a3b8;
  word-break: break-all;
}

.log-info .log-message { color: #94a3b8; }
.log-warn .log-message { color: #fbbf24; }
.log-error .log-message { color: #fca5a5; }
.log-success .log-message { color: #86efac; }

/* ============================================================
// Utility
// ============================================================ */

.rotate-180 {
  transform: rotate(180deg);
}
</style>
