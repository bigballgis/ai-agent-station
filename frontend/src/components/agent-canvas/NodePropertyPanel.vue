<template>
  <div class="properties-panel">
    <h3 class="panel-title">
      {{ t('canvas.propertyPanel.title') }}
      <button class="btn-close" @click="$emit('close')">&times;</button>
    </h3>

    <!-- 通用属性 -->
    <div class="property-group">
      <label>{{ t('canvas.propertyPanel.nodeName') }}</label>
      <input :value="node.label" @input="$emit('updateLabel', ($event.target as HTMLInputElement).value)" />
    </div>

    <!-- start 节点 -->
    <template v-if="node.type === 'start'">
      <div class="property-hint">{{ t('canvas.propertyPanel.startHint') }}</div>
    </template>

    <!-- end 节点 -->
    <template v-if="node.type === 'end'">
      <div class="property-hint">{{ t('canvas.propertyPanel.endHint') }}</div>
    </template>

    <!-- llm 节点 -->
    <template v-if="node.type === 'llm'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.provider') }}</label>
        <select :value="node.data.provider" @change="updateData('provider', ($event.target as HTMLSelectElement).value)">
          <option value="openai">OpenAI</option>
          <option value="anthropic">Anthropic</option>
          <option value="google">Google</option>
          <option value="ollama">Ollama</option>
          <option value="azure">Azure OpenAI</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.model') }}</label>
        <input :value="node.data.model" @input="updateData('model', ($event.target as HTMLInputElement).value)" placeholder="gpt-4 / claude-3" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.temperature') }}: {{ node.data.temperature ?? 0.7 }}</label>
        <input
          type="range"
          min="0"
          max="2"
          step="0.1"
          :value="node.data.temperature ?? 0.7"
          @input="updateData('temperature', Number(($event.target as HTMLInputElement).value))"
          class="slider"
        />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.topP') }}: {{ node.data.topP ?? 1.0 }}</label>
        <input
          type="range"
          min="0"
          max="1"
          step="0.05"
          :value="node.data.topP ?? 1.0"
          @input="updateData('topP', Number(($event.target as HTMLInputElement).value))"
          class="slider"
        />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.maxTokens') }}</label>
        <input
          type="number"
          :value="node.data.maxTokens"
          @input="updateData('maxTokens', Number(($event.target as HTMLInputElement).value))"
          placeholder="2048"
        />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.systemPrompt') }}</label>
        <textarea
          :value="node.data.systemPrompt"
          @input="updateData('systemPrompt', ($event.target as HTMLTextAreaElement).value)"
          :placeholder="t('canvas.propertyPanel.systemPromptPlaceholder')"
          rows="3"
          class="textarea"
        ></textarea>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.prompt') }}</label>
        <textarea
          :value="node.data.prompt"
          @input="updateData('prompt', ($event.target as HTMLTextAreaElement).value)"
          :placeholder="t('canvas.propertyPanel.promptPlaceholder')"
          rows="3"
          class="textarea"
        ></textarea>
      </div>
    </template>

    <!-- condition 节点 -->
    <template v-if="node.type === 'condition'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.expression') }}</label>
        <input :value="node.data.expression" @input="updateData('expression', ($event.target as HTMLInputElement).value)" placeholder="value > 0" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.variable') }}</label>
        <input :value="node.data.variable" @input="updateData('variable', ($event.target as HTMLInputElement).value)" placeholder="result" />
      </div>
    </template>

    <!-- tool 节点 -->
    <template v-if="node.type === 'tool'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.toolId') }}</label>
        <select :value="node.data.toolId" @change="updateData('toolId', ($event.target as HTMLSelectElement).value)">
          <option value="">{{ t('canvas.propertyPanel.toolIdPlaceholder') }}</option>
          <option value="web_search">{{ t('canvas.propertyPanel.webSearch') }}</option>
          <option value="calculator">{{ t('canvas.propertyPanel.calculator') }}</option>
          <option value="weather">{{ t('canvas.propertyPanel.weather') }}</option>
          <option value="database">{{ t('canvas.propertyPanel.database') }}</option>
          <option value="email">{{ t('canvas.propertyPanel.email') }}</option>
          <option value="file_read">{{ t('canvas.propertyPanel.fileRead') }}</option>
          <option value="file_write">{{ t('canvas.propertyPanel.fileWrite') }}</option>
          <option value="custom">{{ t('canvas.propertyPanel.customTool') }}</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.toolName') }}</label>
        <input :value="node.data.toolName" @input="updateData('toolName', ($event.target as HTMLInputElement).value)" placeholder="my_tool" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.inputMapping') }}</label>
        <div v-for="(kv, idx) in (node.data.inputMapping || [])" :key="idx" class="kv-row">
          <input :value="kv.key" @input="updateKV(idx, 'key', ($event.target as HTMLInputElement).value)" placeholder="key" class="kv-input" />
          <input :value="kv.value" @input="updateKV(idx, 'value', ($event.target as HTMLInputElement).value)" placeholder="value" class="kv-input" />
          <button class="btn btn-remove" @click="$emit('removeKV', idx)">-</button>
        </div>
        <button class="btn btn-add-kv" @click="$emit('addKV')">{{ t('canvas.propertyPanel.addMapping') }}</button>
      </div>
    </template>

    <!-- memory 节点 -->
    <template v-if="node.type === 'memory'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.action') }}</label>
        <select :value="node.data.action" @change="updateData('action', ($event.target as HTMLSelectElement).value)">
          <option value="load">{{ t('canvas.propertyPanel.actionLoad') }}</option>
          <option value="save">{{ t('canvas.propertyPanel.actionSave') }}</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.memoryType') }}</label>
        <select :value="node.data.memoryType" @change="updateData('memoryType', ($event.target as HTMLSelectElement).value)">
          <option value="SHORT_TERM">{{ t('canvas.propertyPanel.shortTerm') }}</option>
          <option value="LONG_TERM">{{ t('canvas.propertyPanel.longTerm') }}</option>
          <option value="BUSINESS">{{ t('canvas.propertyPanel.business') }}</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.query') }}</label>
        <input :value="node.data.query" @input="updateData('query', ($event.target as HTMLInputElement).value)" :placeholder="t('canvas.propertyPanel.queryPlaceholder')" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.summary') }}</label>
        <textarea
          :value="node.data.summary"
          @input="updateData('summary', ($event.target as HTMLTextAreaElement).value)"
          :placeholder="t('canvas.propertyPanel.summaryPlaceholder')"
          rows="2"
          class="textarea"
        ></textarea>
      </div>
    </template>

    <!-- retriever 节点 -->
    <template v-if="node.type === 'retriever'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.query') }}</label>
        <input :value="node.data.query" @input="updateData('query', ($event.target as HTMLInputElement).value)" placeholder="..." />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.retrieverType') }}</label>
        <select :value="node.data.retrieverType" @change="updateData('retrieverType', ($event.target as HTMLSelectElement).value)">
          <option value="memory">{{ t('canvas.propertyPanel.memoryRetrieval') }}</option>
          <option value="vector_db">{{ t('canvas.propertyPanel.vectorDb') }}</option>
        </select>
      </div>
    </template>

    <!-- variable 节点 -->
    <template v-if="node.type === 'variable'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.variableName') }}</label>
        <input :value="node.data.name" @input="updateData('name', ($event.target as HTMLInputElement).value)" placeholder="variable_name" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.variableValue') }}</label>
        <input :value="node.data.value" @input="updateData('value', ($event.target as HTMLInputElement).value)" placeholder="value" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.variableSource') }}</label>
        <input :value="node.data.source" @input="updateData('source', ($event.target as HTMLInputElement).value)" :placeholder="t('canvas.propertyPanel.sourcePlaceholder')" />
      </div>
    </template>

    <!-- exception 节点 -->
    <template v-if="node.type === 'exception'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.exceptionAction') }}</label>
        <select :value="node.data.action" @change="updateData('action', ($event.target as HTMLSelectElement).value)">
          <option value="log">{{ t('canvas.propertyPanel.logAction') }}</option>
          <option value="retry">{{ t('canvas.propertyPanel.retryAction') }}</option>
          <option value="fallback">{{ t('canvas.propertyPanel.fallbackAction') }}</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.fallbackValue') }}</label>
        <input :value="node.data.fallbackValue" @input="updateData('fallbackValue', ($event.target as HTMLInputElement).value)" :placeholder="t('canvas.propertyPanel.fallbackValuePlaceholder')" />
      </div>
    </template>

    <!-- http 节点 -->
    <template v-if="node.type === 'http'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.url') }}</label>
        <input :value="node.data.url" @input="updateData('url', ($event.target as HTMLInputElement).value)" placeholder="https://api.example.com" />
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.method') }}</label>
        <select :value="node.data.method" @change="updateData('method', ($event.target as HTMLSelectElement).value)">
          <option value="GET">GET</option>
          <option value="POST">POST</option>
          <option value="PUT">PUT</option>
          <option value="DELETE">DELETE</option>
        </select>
      </div>
    </template>

    <!-- code 节点 -->
    <template v-if="node.type === 'code'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.language') }}</label>
        <select :value="node.data.language" @change="updateData('language', ($event.target as HTMLSelectElement).value)">
          <option value="javascript">JavaScript</option>
          <option value="python">Python</option>
        </select>
      </div>
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.code') }}</label>
        <textarea
          :value="node.data.code"
          @input="updateData('code', ($event.target as HTMLTextAreaElement).value)"
          :placeholder="t('canvas.propertyPanel.codePlaceholder')"
          rows="8"
          class="textarea code-area"
        ></textarea>
      </div>
    </template>

    <!-- delay 节点 -->
    <template v-if="node.type === 'delay'">
      <div class="property-group">
        <label>{{ t('canvas.propertyPanel.waitSeconds') }}</label>
        <input
          type="number"
          :value="node.data.seconds"
          @input="updateData('seconds', Number(($event.target as HTMLInputElement).value))"
          placeholder="1"
          min="0"
        />
      </div>
    </template>

    <!-- 删除按钮 -->
    <div class="property-actions">
      <button @click="$emit('deleteNode')" class="btn btn-danger">{{ t('canvas.propertyPanel.deleteNode') }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

export interface CanvasNode {
  id: string
  type: string
  label: string
  x: number
  y: number
  data: Record<string, any>
}

const props = defineProps<{
  node: CanvasNode
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updateLabel', value: string): void
  (e: 'updateData', key: string, value: unknown): void
  (e: 'addKV'): void
  (e: 'removeKV', idx: number): void
  (e: 'deleteNode'): void
}>()

function updateData(key: string, value: unknown) {
  emit('updateData', key, value)
}

function updateKV(idx: number, field: 'key' | 'value', value: string) {
  const mapping = props.node.data.inputMapping || []
  if (mapping[idx]) {
    mapping[idx][field] = value
    emit('updateData', 'inputMapping', [...mapping])
  }
}
</script>

<style scoped>
.properties-panel {
  width: 300px;
  background: #16213e;
  border-left: 1px solid #2a2a4a;
  padding: 16px;
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

.btn {
  padding: 6px 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  white-space: nowrap;
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

select option {
  background: #16213e;
  color: #e0e0e0;
}
</style>
