<template>
  <div class="properties-panel">
    <h3 class="panel-title">
      属性配置
      <button class="btn-close" @click="$emit('close')">&times;</button>
    </h3>

    <!-- 通用属性 -->
    <div class="property-group">
      <label>节点名称</label>
      <input :value="node.label" @input="$emit('updateLabel', ($event.target as HTMLInputElement).value)" />
    </div>

    <!-- start 节点 -->
    <template v-if="node.type === 'start'">
      <div class="property-hint">开始节点，无特殊配置</div>
    </template>

    <!-- end 节点 -->
    <template v-if="node.type === 'end'">
      <div class="property-hint">结束节点，无特殊配置</div>
    </template>

    <!-- llm 节点 -->
    <template v-if="node.type === 'llm'">
      <div class="property-group">
        <label>Provider</label>
        <select :value="node.data.provider" @change="updateData('provider', ($event.target as HTMLSelectElement).value)">
          <option value="openai">OpenAI</option>
          <option value="anthropic">Anthropic</option>
          <option value="google">Google</option>
          <option value="ollama">Ollama</option>
          <option value="azure">Azure OpenAI</option>
        </select>
      </div>
      <div class="property-group">
        <label>模型</label>
        <input :value="node.data.model" @input="updateData('model', ($event.target as HTMLInputElement).value)" placeholder="gpt-4 / claude-3" />
      </div>
      <div class="property-group">
        <label>Temperature: {{ node.data.temperature ?? 0.7 }}</label>
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
        <label>Top P: {{ node.data.topP ?? 1.0 }}</label>
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
        <label>Max Tokens</label>
        <input
          type="number"
          :value="node.data.maxTokens"
          @input="updateData('maxTokens', Number(($event.target as HTMLInputElement).value))"
          placeholder="2048"
        />
      </div>
      <div class="property-group">
        <label>System Prompt</label>
        <textarea
          :value="node.data.systemPrompt"
          @input="updateData('systemPrompt', ($event.target as HTMLTextAreaElement).value)"
          placeholder="系统提示词..."
          rows="3"
          class="textarea"
        ></textarea>
      </div>
      <div class="property-group">
        <label>Prompt (支持 &#123;&#123;变量&#125;&#125; 模板)</label>
        <textarea
          :value="node.data.prompt"
          @input="updateData('prompt', ($event.target as HTMLTextAreaElement).value)"
          placeholder="请根据以下内容回答: {{input}}"
          rows="3"
          class="textarea"
        ></textarea>
      </div>
    </template>

    <!-- condition 节点 -->
    <template v-if="node.type === 'condition'">
      <div class="property-group">
        <label>表达式</label>
        <input :value="node.data.expression" @input="updateData('expression', ($event.target as HTMLInputElement).value)" placeholder="value > 0" />
      </div>
      <div class="property-group">
        <label>变量</label>
        <input :value="node.data.variable" @input="updateData('variable', ($event.target as HTMLInputElement).value)" placeholder="result" />
      </div>
    </template>

    <!-- tool 节点 -->
    <template v-if="node.type === 'tool'">
      <div class="property-group">
        <label>工具 ID</label>
        <select :value="node.data.toolId" @change="updateData('toolId', ($event.target as HTMLSelectElement).value)">
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
        <input :value="node.data.toolName" @input="updateData('toolName', ($event.target as HTMLInputElement).value)" placeholder="my_tool" />
      </div>
      <div class="property-group">
        <label>输入映射 (Key-Value)</label>
        <div v-for="(kv, idx) in (node.data.inputMapping || [])" :key="idx" class="kv-row">
          <input :value="kv.key" @input="updateKV(idx, 'key', ($event.target as HTMLInputElement).value)" placeholder="key" class="kv-input" />
          <input :value="kv.value" @input="updateKV(idx, 'value', ($event.target as HTMLInputElement).value)" placeholder="value" class="kv-input" />
          <button class="btn btn-remove" @click="$emit('removeKV', idx)">-</button>
        </div>
        <button class="btn btn-add-kv" @click="$emit('addKV')">+ 添加映射</button>
      </div>
    </template>

    <!-- memory 节点 -->
    <template v-if="node.type === 'memory'">
      <div class="property-group">
        <label>操作</label>
        <select :value="node.data.action" @change="updateData('action', ($event.target as HTMLSelectElement).value)">
          <option value="load">加载 (Load)</option>
          <option value="save">保存 (Save)</option>
        </select>
      </div>
      <div class="property-group">
        <label>记忆类型</label>
        <select :value="node.data.memoryType" @change="updateData('memoryType', ($event.target as HTMLSelectElement).value)">
          <option value="SHORT_TERM">短期记忆</option>
          <option value="LONG_TERM">长期记忆</option>
          <option value="BUSINESS">业务记忆</option>
        </select>
      </div>
      <div class="property-group">
        <label>查询</label>
        <input :value="node.data.query" @input="updateData('query', ($event.target as HTMLInputElement).value)" placeholder="查询内容..." />
      </div>
      <div class="property-group">
        <label>摘要</label>
        <textarea
          :value="node.data.summary"
          @input="updateData('summary', ($event.target as HTMLTextAreaElement).value)"
          placeholder="记忆摘要..."
          rows="2"
          class="textarea"
        ></textarea>
      </div>
    </template>

    <!-- retriever 节点 -->
    <template v-if="node.type === 'retriever'">
      <div class="property-group">
        <label>查询</label>
        <input :value="node.data.query" @input="updateData('query', ($event.target as HTMLInputElement).value)" placeholder="检索查询..." />
      </div>
      <div class="property-group">
        <label>检索类型</label>
        <select :value="node.data.retrieverType" @change="updateData('retrieverType', ($event.target as HTMLSelectElement).value)">
          <option value="memory">记忆检索</option>
          <option value="vector_db">向量数据库</option>
        </select>
      </div>
    </template>

    <!-- variable 节点 -->
    <template v-if="node.type === 'variable'">
      <div class="property-group">
        <label>变量名</label>
        <input :value="node.data.name" @input="updateData('name', ($event.target as HTMLInputElement).value)" placeholder="variable_name" />
      </div>
      <div class="property-group">
        <label>值</label>
        <input :value="node.data.value" @input="updateData('value', ($event.target as HTMLInputElement).value)" placeholder="value" />
      </div>
      <div class="property-group">
        <label>来源</label>
        <input :value="node.data.source" @input="updateData('source', ($event.target as HTMLInputElement).value)" placeholder="来源节点/表达式" />
      </div>
    </template>

    <!-- exception 节点 -->
    <template v-if="node.type === 'exception'">
      <div class="property-group">
        <label>处理方式</label>
        <select :value="node.data.action" @change="updateData('action', ($event.target as HTMLSelectElement).value)">
          <option value="log">记录日志</option>
          <option value="retry">重试</option>
          <option value="fallback">降级处理</option>
        </select>
      </div>
      <div class="property-group">
        <label>降级值</label>
        <input :value="node.data.fallbackValue" @input="updateData('fallbackValue', ($event.target as HTMLInputElement).value)" placeholder="默认降级值" />
      </div>
    </template>

    <!-- http 节点 -->
    <template v-if="node.type === 'http'">
      <div class="property-group">
        <label>URL</label>
        <input :value="node.data.url" @input="updateData('url', ($event.target as HTMLInputElement).value)" placeholder="https://api.example.com" />
      </div>
      <div class="property-group">
        <label>Method</label>
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
        <label>语言</label>
        <select :value="node.data.language" @change="updateData('language', ($event.target as HTMLSelectElement).value)">
          <option value="javascript">JavaScript</option>
          <option value="python">Python</option>
        </select>
      </div>
      <div class="property-group">
        <label>代码</label>
        <textarea
          :value="node.data.code"
          @input="updateData('code', ($event.target as HTMLTextAreaElement).value)"
          placeholder="// 在此编写代码..."
          rows="8"
          class="textarea code-area"
        ></textarea>
      </div>
    </template>

    <!-- delay 节点 -->
    <template v-if="node.type === 'delay'">
      <div class="property-group">
        <label>等待时间 (秒)</label>
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
      <button @click="$emit('deleteNode')" class="btn btn-danger">删除节点</button>
    </div>
  </div>
</template>

<script setup lang="ts">
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
