<template>
  <div v-if="node" class="config-panel">
    <!-- Header -->
    <div class="config-header">
      <div class="config-header-info">
        <span class="config-header-icon">{{ nodeIcon }}</span>
        <div class="config-header-text">
          <span class="config-header-name">{{ node.label }}</span>
          <span class="config-header-type">{{ typeName }}</span>
        </div>
      </div>
      <button class="config-close-btn" @click="$emit('close')" title="关闭">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Config Body -->
    <div class="config-body">
      <!-- Common: Label -->
      <div class="form-group">
        <label class="form-label">{{ t('designer.config.nodeName') }}</label>
        <input
          class="form-input"
          type="text"
          :value="node.label"
          @input="handleLabelUpdate(($event.target as HTMLInputElement).value)"
          :placeholder="t('designer.config.nodeNamePlaceholder')"
        />
      </div>

      <!-- ========== Type-specific fields ========== -->

      <!-- LLM Node -->
      <template v-if="node.type === 'llm'">
        <div class="form-group">
          <label class="form-label">Provider</label>
          <select class="form-select" :value="node.config.provider" @change="updateConfig('provider', ($event.target as HTMLSelectElement).value)">
            <option value="openai">OpenAI</option>
            <option value="anthropic">Anthropic</option>
            <option value="google">Google</option>
            <option value="ollama">Ollama</option>
            <option value="azure">Azure OpenAI</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">模型</label>
          <input class="form-input" type="text" :value="node.config.model" @input="updateConfig('model', ($event.target as HTMLInputElement).value)" placeholder="gpt-4 / claude-3" />
        </div>
        <div class="form-group">
          <label class="form-label">Temperature: {{ node.config.temperature ?? 0.7 }}</label>
          <input class="form-slider" type="range" min="0" max="2" step="0.1" :value="node.config.temperature ?? 0.7" @input="updateConfig('temperature', Number(($event.target as HTMLInputElement).value))" />
        </div>
        <div class="form-group">
          <label class="form-label">Top P: {{ node.config.topP ?? 1.0 }}</label>
          <input class="form-slider" type="range" min="0" max="1" step="0.05" :value="node.config.topP ?? 1.0" @input="updateConfig('topP', Number(($event.target as HTMLInputElement).value))" />
        </div>
        <div class="form-group">
          <label class="form-label">Max Tokens</label>
          <input class="form-input" type="number" :value="node.config.maxTokens" @input="updateConfig('maxTokens', Number(($event.target as HTMLInputElement).value))" placeholder="2048" />
        </div>
        <div class="form-group">
          <label class="form-label">System Prompt</label>
          <textarea class="form-textarea" rows="3" :value="node.config.systemPrompt" @input="updateConfig('systemPrompt', ($event.target as HTMLTextAreaElement).value)" placeholder="系统提示词..." />
        </div>
        <div class="form-group">
          <label class="form-label">Prompt <span class="form-hint">支持 &#123;&#123;变量&#125;&#125; 模板</span></label>
          <textarea class="form-textarea" rows="3" :value="node.config.prompt" @input="updateConfig('prompt', ($event.target as HTMLTextAreaElement).value)" placeholder="请根据以下内容回答..." />
        </div>
      </template>

      <!-- Condition Node -->
      <template v-if="node.type === 'condition'">
        <div class="form-group">
          <label class="form-label">表达式</label>
          <input class="form-input" type="text" :value="node.config.expression" @input="updateConfig('expression', ($event.target as HTMLInputElement).value)" placeholder="value > 0" />
        </div>
        <div class="form-group">
          <label class="form-label">变量</label>
          <input class="form-input" type="text" :value="node.config.variable" @input="updateConfig('variable', ($event.target as HTMLInputElement).value)" placeholder="result" />
        </div>
      </template>

      <!-- Tool Node -->
      <template v-if="node.type === 'tool'">
        <div class="form-group">
          <label class="form-label">工具 ID</label>
          <select class="form-select" :value="node.config.toolId" @change="updateConfig('toolId', ($event.target as HTMLSelectElement).value)">
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
        <div class="form-group">
          <label class="form-label">工具名称</label>
          <input class="form-input" type="text" :value="node.config.toolName" @input="updateConfig('toolName', ($event.target as HTMLInputElement).value)" placeholder="my_tool" />
        </div>
        <div class="form-group">
          <label class="form-label">输入映射 (Key-Value)</label>
          <div v-for="(kv, idx) in (node.config.inputMapping || [])" :key="idx" class="kv-row">
            <input class="kv-input" type="text" :value="kv.key" @input="updateKV(idx, 'key', ($event.target as HTMLInputElement).value)" placeholder="key" />
            <input class="kv-input" type="text" :value="kv.value" @input="updateKV(idx, 'value', ($event.target as HTMLInputElement).value)" placeholder="value" />
            <button class="kv-remove-btn" @click="removeKV(idx)" title="删除">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="12" height="12">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button class="kv-add-btn" @click="addKV">+ 添加映射</button>
        </div>
      </template>

      <!-- Memory Node -->
      <template v-if="node.type === 'memory'">
        <div class="form-group">
          <label class="form-label">操作</label>
          <select class="form-select" :value="node.config.action" @change="updateConfig('action', ($event.target as HTMLSelectElement).value)">
            <option value="load">加载 (Load)</option>
            <option value="save">保存 (Save)</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">记忆类型</label>
          <select class="form-select" :value="node.config.memoryType" @change="updateConfig('memoryType', ($event.target as HTMLSelectElement).value)">
            <option value="SHORT_TERM">短期记忆</option>
            <option value="LONG_TERM">长期记忆</option>
            <option value="BUSINESS">业务记忆</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">查询</label>
          <input class="form-input" type="text" :value="node.config.query" @input="updateConfig('query', ($event.target as HTMLInputElement).value)" placeholder="查询内容..." />
        </div>
        <div class="form-group">
          <label class="form-label">摘要</label>
          <textarea class="form-textarea" rows="2" :value="node.config.summary" @input="updateConfig('summary', ($event.target as HTMLTextAreaElement).value)" placeholder="记忆摘要..." />
        </div>
      </template>

      <!-- Variable Node -->
      <template v-if="node.type === 'variable'">
        <div class="form-group">
          <label class="form-label">变量名</label>
          <input class="form-input" type="text" :value="node.config.name" @input="updateConfig('name', ($event.target as HTMLInputElement).value)" placeholder="variable_name" />
        </div>
        <div class="form-group">
          <label class="form-label">值</label>
          <input class="form-input" type="text" :value="node.config.value" @input="updateConfig('value', ($event.target as HTMLInputElement).value)" placeholder="value" />
        </div>
        <div class="form-group">
          <label class="form-label">来源</label>
          <input class="form-input" type="text" :value="node.config.source" @input="updateConfig('source', ($event.target as HTMLInputElement).value)" placeholder="来源节点/表达式" />
        </div>
      </template>

      <!-- Retriever Node -->
      <template v-if="node.type === 'retriever'">
        <div class="form-group">
          <label class="form-label">查询</label>
          <input class="form-input" type="text" :value="node.config.query" @input="updateConfig('query', ($event.target as HTMLInputElement).value)" placeholder="检索查询..." />
        </div>
        <div class="form-group">
          <label class="form-label">检索类型</label>
          <select class="form-select" :value="node.config.retrieverType" @change="updateConfig('retrieverType', ($event.target as HTMLSelectElement).value)">
            <option value="memory">记忆检索</option>
            <option value="vector_db">向量数据库</option>
          </select>
        </div>
      </template>

      <!-- Exception Node -->
      <template v-if="node.type === 'exception'">
        <div class="form-group">
          <label class="form-label">处理方式</label>
          <select class="form-select" :value="node.config.action" @change="updateConfig('action', ($event.target as HTMLSelectElement).value)">
            <option value="log">记录日志</option>
            <option value="retry">重试</option>
            <option value="fallback">降级处理</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">降级值</label>
          <input class="form-input" type="text" :value="node.config.fallbackValue" @input="updateConfig('fallbackValue', ($event.target as HTMLInputElement).value)" placeholder="默认降级值" />
        </div>
      </template>

      <!-- HTTP Node -->
      <template v-if="node.type === 'http'">
        <div class="form-group">
          <label class="form-label">URL</label>
          <input class="form-input" type="text" :value="node.config.url" @input="updateConfig('url', ($event.target as HTMLInputElement).value)" placeholder="https://api.example.com" />
        </div>
        <div class="form-group">
          <label class="form-label">Method</label>
          <select class="form-select" :value="node.config.method" @change="updateConfig('method', ($event.target as HTMLSelectElement).value)">
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Headers <span class="form-hint">JSON 格式</span></label>
          <textarea class="form-textarea code-textarea" rows="3" :value="node.config.headers" @input="updateConfig('headers', ($event.target as HTMLTextAreaElement).value)" placeholder='{"Content-Type": "application/json"}' />
        </div>
        <div class="form-group">
          <label class="form-label">Body</label>
          <textarea class="form-textarea" rows="3" :value="node.config.body" @input="updateConfig('body', ($event.target as HTMLTextAreaElement).value)" placeholder="请求体..." />
        </div>
      </template>

      <!-- Code Node -->
      <template v-if="node.type === 'code'">
        <div class="form-group">
          <label class="form-label">语言</label>
          <select class="form-select" :value="node.config.language" @change="updateConfig('language', ($event.target as HTMLSelectElement).value)">
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">代码</label>
          <textarea class="form-textarea code-textarea" rows="8" :value="node.config.code" @input="updateConfig('code', ($event.target as HTMLTextAreaElement).value)" placeholder="// 在此编写代码..." />
        </div>
      </template>

      <!-- Delay Node -->
      <template v-if="node.type === 'delay'">
        <div class="form-group">
          <label class="form-label">等待时间 (秒)</label>
          <input class="form-input" type="number" :value="node.config.seconds" @input="updateConfig('seconds', Number(($event.target as HTMLInputElement).value))" placeholder="1" min="0" />
        </div>
      </template>

      <!-- Start Node -->
      <template v-if="node.type === 'start'">
        <div class="form-group">
          <label class="form-label">输入变量 <span class="form-hint">JSON 格式</span></label>
          <textarea class="form-textarea code-textarea" rows="4" :value="node.config.inputVariables" @input="updateConfig('inputVariables', ($event.target as HTMLTextAreaElement).value)" placeholder='{"message": "string"}' />
        </div>
      </template>

      <!-- End Node -->
      <template v-if="node.type === 'end'">
        <div class="form-group">
          <label class="form-label">输出格式</label>
          <select class="form-select" :value="node.config.outputFormat" @change="updateConfig('outputFormat', ($event.target as HTMLSelectElement).value)">
            <option value="text">纯文本</option>
            <option value="json">JSON</option>
            <option value="markdown">Markdown</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">输出模板</label>
          <textarea class="form-textarea" rows="3" :value="node.config.outputTemplate" @input="updateConfig('outputTemplate', ($event.target as HTMLTextAreaElement).value)" placeholder="输出模板..." />
        </div>
      </template>
    </div>

    <!-- Delete Button -->
    <div class="config-footer">
      <button class="delete-btn" @click="$emit('delete-node', node.id)">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="14" height="14">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        {{ t('designer.config.deleteNode') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CanvasNode } from '@/composables/designer/types'
import { getNodeTypeDefinition } from '@/composables/designer/nodeRegistry'

const { t } = useI18n()

const props = defineProps<{
  node: CanvasNode | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update-config', nodeId: string, key: string, value: any): void
  (e: 'delete-node', nodeId: string): void
  (e: 'update-label', nodeId: string, label: string): void
}>()

const nodeDef = computed(() => (props.node ? getNodeTypeDefinition(props.node.type) : null))

const nodeIcon = computed(() => nodeDef.value?.icon ?? '📦')

const typeName = computed(() => nodeDef.value?.name ?? '')

function handleLabelUpdate(value: string) {
  if (!props.node) return
  emit('update-label', props.node.id, value)
}

function updateConfig(key: string, value: any) {
  if (!props.node) return
  emit('update-config', props.node.id, key, value)
}

function updateKV(idx: number, field: 'key' | 'value', value: string) {
  if (!props.node) return
  const mapping = [...(props.node.config.inputMapping || [])]
  if (!mapping[idx]) return
  mapping[idx] = { ...mapping[idx], [field]: value }
  emit('update-config', props.node.id, 'inputMapping', mapping)
}

function addKV() {
  if (!props.node) return
  const mapping = [...(props.node.config.inputMapping || []), { key: '', value: '' }]
  emit('update-config', props.node.id, 'inputMapping', mapping)
}

function removeKV(idx: number) {
  if (!props.node) return
  const mapping = [...(props.node.config.inputMapping || [])]
  mapping.splice(idx, 1)
  emit('update-config', props.node.id, 'inputMapping', mapping)
}
</script>

<style scoped>
.config-panel {
  width: 300px;
  background: #16162a;
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

/* Header */
.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.config-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.config-header-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.config-header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.config-header-name {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.config-header-type {
  font-size: 11px;
  color: rgba(226, 232, 240, 0.4);
}

.config-close-btn {
  background: none;
  border: none;
  color: rgba(226, 232, 240, 0.4);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.config-close-btn:hover {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.06);
}

/* Body */
.config-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

/* Form Elements */
.form-group {
  margin-bottom: 14px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: rgba(226, 232, 240, 0.6);
  margin-bottom: 6px;
}

.form-hint {
  font-weight: 400;
  color: rgba(226, 232, 240, 0.3);
  font-size: 10px;
}

.form-input,
.form-select {
  width: 100%;
  padding: 7px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
}

.form-input::placeholder {
  color: rgba(226, 232, 240, 0.2);
}

.form-input:focus,
.form-select:focus {
  border-color: rgba(24, 144, 255, 0.5);
}

.form-select {
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg fill='none' stroke='%23888' viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 14px;
  padding-right: 28px;
}

.form-select option {
  background: #16162a;
  color: #e2e8f0;
}

.form-textarea {
  width: 100%;
  padding: 7px 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.2s ease;
}

.form-textarea::placeholder {
  color: rgba(226, 232, 240, 0.2);
}

.form-textarea:focus {
  border-color: rgba(24, 144, 255, 0.5);
}

.code-textarea {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  tab-size: 2;
}

.form-slider {
  width: 100%;
  -webkit-appearance: none;
  appearance: none;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}

.form-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1890ff;
  cursor: pointer;
  border: 2px solid #16162a;
}

.form-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #1890ff;
  cursor: pointer;
  border: 2px solid #16162a;
}

/* Key-Value Editor */
.kv-row {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
  align-items: center;
}

.kv-input {
  flex: 1;
  padding: 5px 8px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  color: #e2e8f0;
  font-size: 12px;
  outline: none;
  min-width: 0;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
}

.kv-input::placeholder {
  color: rgba(226, 232, 240, 0.2);
}

.kv-input:focus {
  border-color: rgba(24, 144, 255, 0.5);
}

.kv-remove-btn {
  background: rgba(255, 77, 79, 0.1);
  border: 1px solid rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.kv-remove-btn:hover {
  background: rgba(255, 77, 79, 0.2);
}

.kv-add-btn {
  width: 100%;
  padding: 5px 10px;
  background: rgba(24, 144, 255, 0.08);
  border: 1px solid rgba(24, 144, 255, 0.15);
  border-radius: 4px;
  color: #1890ff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-top: 4px;
}

.kv-add-btn:hover {
  background: rgba(24, 144, 255, 0.15);
}

/* Footer */
.config-footer {
  padding: 12px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.delete-btn {
  width: 100%;
  padding: 8px 12px;
  background: rgba(255, 77, 79, 0.08);
  border: 1px solid rgba(255, 77, 79, 0.15);
  border-radius: 6px;
  color: #ff4d4f;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s ease;
}

.delete-btn:hover {
  background: rgba(255, 77, 79, 0.15);
  border-color: rgba(255, 77, 79, 0.3);
}

/* Scrollbar */
.config-body::-webkit-scrollbar {
  width: 4px;
}

.config-body::-webkit-scrollbar-track {
  background: transparent;
}

.config-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
}

.config-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.15);
}
</style>
