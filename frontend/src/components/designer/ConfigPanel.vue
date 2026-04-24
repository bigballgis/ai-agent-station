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
      <button class="config-close-btn" @click="$emit('close')" :aria-label="t('designer.config.close')" :title="t('designer.config.close')">
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

      <!-- ========== Schema-driven fields ========== -->
      <template v-for="field in schemaFields" :key="field.key">
        <!-- Text input -->
        <div v-if="field.type === 'text'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <input
            class="form-input"
            type="text"
            :value="node.config[field.key]"
            @input="updateConfig(field.key, ($event.target as HTMLInputElement).value)"
            :placeholder="field.placeholder"
          />
        </div>

        <!-- Textarea -->
        <div v-else-if="field.type === 'textarea'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <textarea
            class="form-textarea"
            rows="3"
            :value="(node.config[field.key] as string) ?? ''"
            @input="updateConfig(field.key, ($event.target as HTMLTextAreaElement).value)"
            :placeholder="field.placeholder"
          />
        </div>

        <!-- Number input -->
        <div v-else-if="field.type === 'number'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <input
            class="form-input"
            type="number"
            :value="node.config[field.key]"
            @input="updateConfig(field.key, Number(($event.target as HTMLInputElement).value))"
            :min="field.min"
            :max="field.max"
            :placeholder="field.placeholder"
          />
        </div>

        <!-- Select -->
        <div v-else-if="field.type === 'select'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <select
            class="form-select"
            :value="node.config[field.key]"
            @change="updateConfig(field.key, ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>

        <!-- Slider -->
        <div v-else-if="field.type === 'slider'" class="form-group">
          <label class="form-label">
            {{ field.label }}: {{ node.config[field.key] ?? field.defaultValue }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <input
            class="form-slider"
            type="range"
            :min="field.min"
            :max="field.max"
            :step="field.step || 0.1"
            :value="node.config[field.key] ?? field.defaultValue"
            @input="updateConfig(field.key, Number(($event.target as HTMLInputElement).value))"
          />
        </div>

        <!-- JSON textarea -->
        <div v-else-if="field.type === 'json'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <textarea
            class="form-textarea code-textarea"
            rows="3"
            :value="typeof node.config[field.key] === 'object'
              ? JSON.stringify(node.config[field.key], null, 2)
              : ((node.config[field.key] as string) ?? '')"
            @input="updateConfig(field.key, ($event.target as HTMLTextAreaElement).value)"
            :placeholder="field.placeholder"
          />
        </div>

        <!-- Key-Value editor (generic, works for any field.key) -->
        <div v-else-if="field.type === 'key-value'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <div v-for="(kv, idx) in (node.config[field.key] as Array<{key: string, value: string}> || [])" :key="idx" class="kv-row">
            <input
              class="kv-input"
              type="text"
              :value="kv.key"
              @input="updateKV(field.key, idx, 'key', ($event.target as HTMLInputElement).value)"
              placeholder="key"
            />
            <input
              class="kv-input"
              type="text"
              :value="kv.value"
              @input="updateKV(field.key, idx, 'value', ($event.target as HTMLInputElement).value)"
              placeholder="value"
            />
            <button class="kv-remove-btn" @click="removeKV(field.key, idx)" :aria-label="t('designer.config.delete')" :title="t('designer.config.delete')">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="12" height="12">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button class="kv-add-btn" @click="addKV(field.key)">{{ t('designer.config.addMapping') }}</button>
        </div>

        <!-- Code editor -->
        <div v-else-if="field.type === 'code'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <textarea
            class="form-textarea code-textarea"
            rows="8"
            :value="(node.config[field.key] as string) ?? ''"
            @input="updateConfig(field.key, ($event.target as HTMLTextAreaElement).value)"
            :placeholder="field.placeholder"
          />
        </div>

        <!-- Switch cases editor (custom override for switch node) -->
        <div v-else-if="field.type === 'switch-cases'" class="form-group">
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.tooltip" class="form-hint" :title="field.tooltip">ⓘ</span>
          </label>
          <div v-for="(c, i) in (node.config.cases as Array<{expression: string, label: string, outputPort: string}> || [])" :key="i" class="kv-row">
            <input
              class="kv-input"
              type="text"
              :value="c.expression"
              @input="updateCase(i, 'expression', ($event.target as HTMLInputElement).value)"
              :placeholder="t('designer.config.expressionPlaceholder')"
            />
            <input
              class="kv-input"
              type="text"
              :value="c.label"
              @input="updateCase(i, 'label', ($event.target as HTMLInputElement).value)"
              :placeholder="t('designer.config.branchNamePlaceholder')"
            />
            <button class="kv-remove-btn" @click="removeCase(i)" :aria-label="t('designer.config.deleteBranch')" :title="t('designer.config.deleteBranch')">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="12" height="12">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button class="kv-add-btn" @click="addCase">{{ t('designer.config.addBranch') }}</button>
        </div>
      </template>

      <!-- Variable hint for LLM prompt -->
      <div v-if="node.type === 'llm' && availableNodes.length > 0" class="variable-hint">
        <span class="hint-label">{{ t('designer.config.availableVars') }}</span>
        <span
          class="hint-text"
          v-for="avNode in availableNodes"
          :key="avNode.id"
          @click="insertVariable(avNode.id)"
          :title="`点击插入 {{${avNode.id}}.output}}`"
        >
          &#123;&#123;{{ avNode.id }}.output&#125;&#125;
        </span>
      </div>
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
  nodes?: CanvasNode[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update-config', nodeId: string, key: string, value: unknown): void
  (e: 'delete-node', nodeId: string): void
  (e: 'update-label', nodeId: string, label: string): void
}>()

const nodeDef = computed(() => (props.node ? getNodeTypeDefinition(props.node.type) : null))

const nodeIcon = computed(() => nodeDef.value?.icon ?? '📦')

const typeName = computed(() => nodeDef.value?.name ?? '')

// Schema-driven field list from nodeRegistry
const schemaFields = computed(() => {
  if (!props.node) return []
  const typeDef = getNodeTypeDefinition(props.node.type)
  return typeDef?.configSchema || []
})

// Nodes available for variable reference (exclude current node and start node)
const availableNodes = computed(() => {
  if (!props.node || !props.nodes) return []
  return props.nodes.filter(n => n.id !== props.node!.id && n.type !== 'start')
})

// Insert a variable reference into the currently focused textarea
function insertVariable(nodeId: string) {
  const refText = `{{${nodeId}.output}}`
  // Find the last focused textarea in the config panel
  const panel = document.querySelector('.config-panel')
  if (panel) {
    const textarea = panel.querySelector('textarea:focus') as HTMLTextAreaElement
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const value = textarea.value
      textarea.value = value.substring(0, start) + refText + value.substring(end)
      textarea.selectionStart = textarea.selectionEnd = start + refText.length
      textarea.dispatchEvent(new Event('input', { bubbles: true }))
      return
    }
  }
  // Fallback: insert into the prompt field if node is LLM
  if (props.node?.type === 'llm') {
    const currentPrompt = props.node.config.prompt || ''
    const newPrompt = currentPrompt + refText
    emit('update-config', props.node.id, 'prompt', newPrompt)
  }
}

function handleLabelUpdate(value: string) {
  if (!props.node) return
  emit('update-label', props.node.id, value)
}

function updateConfig(key: string, value: unknown) {
  if (!props.node) return
  emit('update-config', props.node.id, key, value)
}

// Generic KV editor methods (work for any field.key)
function updateKV(fieldKey: string, idx: number, field: 'key' | 'value', value: string) {
  if (!props.node) return
  const arr = [...(props.node.config[fieldKey] as Array<{key: string, value: string}> || [])]
  if (!arr[idx]) return
  arr[idx] = { ...arr[idx], [field]: value }
  emit('update-config', props.node.id, fieldKey, arr)
}

function addKV(fieldKey: string) {
  if (!props.node) return
  const arr = [...(props.node.config[fieldKey] as Array<{key: string, value: string}> || []), { key: '', value: '' }]
  emit('update-config', props.node.id, fieldKey, arr)
}

function removeKV(fieldKey: string, idx: number) {
  if (!props.node) return
  const arr = [...(props.node.config[fieldKey] as Array<{key: string, value: string}> || [])]
  arr.splice(idx, 1)
  emit('update-config', props.node.id, fieldKey, arr)
}

// Switch node case management (custom override for switch-cases type)
function updateCase(index: number, field: string, value: string) {
  if (!props.node) return
  const cases = [...(props.node.config.cases as Array<{expression: string, label: string, outputPort: string}> || [])]
  cases[index] = { ...cases[index], [field]: value }
  emit('update-config', props.node.id, 'cases', cases)
}

function addCase() {
  if (!props.node) return
  const cases = [...(props.node.config.cases as Array<{expression: string, label: string, outputPort: string}> || [])]
  const portIndex = cases.length + 1
  cases.push({ expression: '', label: t('designer.config.branchDefault', { index: portIndex }), outputPort: `case_${portIndex}` })
  emit('update-config', props.node.id, 'cases', cases)
}

function removeCase(index: number) {
  if (!props.node) return
  const cases = [...(props.node.config.cases as Array<{expression: string, label: string, outputPort: string}> || [])]
  cases.splice(index, 1)
  emit('update-config', props.node.id, 'cases', cases)
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

/* Variable Hint */
.variable-hint {
  margin-top: 4px;
  padding: 6px 8px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.hint-label {
  color: #64748b;
  margin-right: 4px;
}

.hint-text {
  cursor: pointer;
  color: #818cf8;
  margin-right: 8px;
  font-family: monospace;
}

.hint-text:hover {
  color: #a5b4fc;
  text-decoration: underline;
}
</style>
