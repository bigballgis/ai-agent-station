<template>
  <div class="json-editor" :class="{ 'json-editor-readonly': readonly }">
    <!-- 工具栏 -->
    <div v-if="!readonly" class="json-editor-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">JSON</span>
        <a-tag v-if="isValid !== null" :color="isValid ? 'success' : 'error'" class="validation-tag">
          {{ isValid ? t('common.jsonValid') : t('common.jsonInvalid') }}
        </a-tag>
      </div>
      <div class="toolbar-right">
        <a-button type="text" size="small" @click="handleFormat">
          <template #icon>
            <AlignLeftOutlined />
          </template>
          {{ t('common.format') }}
        </a-button>
        <a-button type="text" size="small" @click="handleCopy">
          <template #icon>
            <CopyOutlined />
          </template>
          {{ t('common.copy') }}
        </a-button>
        <a-button type="text" size="small" @click="handleClear">
          <template #icon>
            <DeleteOutlined />
          </template>
          {{ t('common.clear') }}
        </a-button>
      </div>
    </div>

    <!-- 编辑区域 -->
    <div class="json-editor-body" :style="{ height: height ? `${height}px` : '300px' }">
      <!-- 行号 -->
      <div class="json-editor-lines" ref="linesRef">
        <div
          v-for="line in lineCount"
          :key="line"
          class="json-editor-line-number"
        >
          {{ line }}
        </div>
      </div>

      <!-- 文本区域 -->
      <textarea
        ref="textareaRef"
        v-model="internalValue"
        class="json-editor-textarea"
        :readonly="readonly"
        spellcheck="false"
        @input="handleInput"
        @scroll="syncScroll"
      />
    </div>

    <!-- 错误信息 -->
    <div v-if="errorMessage" class="json-editor-error">
      <WarningOutlined />
      <span>{{ errorMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  AlignLeftOutlined,
  CopyOutlined,
  DeleteOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'

const { t } = useI18n()

/**
 * JsonEditor 组件
 * JSON 编辑器/查看器组件
 * 支持语法高亮（通过行号）、格式化、验证、复制
 * 用于 AgentEdit（图定义）、ApiManagement（请求/响应体）
 */

interface Props {
  /** JSON 内容（双向绑定） */
  modelValue: string
  /** 是否只读 */
  readonly?: boolean
  /** 编辑器高度 */
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'error', message: string): void
}>()

const internalValue = ref(props.modelValue)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const linesRef = ref<HTMLDivElement | null>(null)
const isValid = ref<boolean | null>(null)
const errorMessage = ref('')

// 计算行数
const lineCount = computed(() => {
  if (!internalValue.value) return 1
  return internalValue.value.split('\n').length
})

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  internalValue.value = val
  validateJson()
})

/** 输入处理 */
function handleInput() {
  emit('update:modelValue', internalValue.value)
  validateJson()
}

/** 验证 JSON 格式 */
function validateJson() {
  if (!internalValue.value.trim()) {
    isValid.value = null
    errorMessage.value = ''
    return
  }
  try {
    JSON.parse(internalValue.value)
    isValid.value = true
    errorMessage.value = ''
  } catch (e: any) {
    isValid.value = false
    errorMessage.value = e.message || t('common.jsonParseError')
    emit('error', errorMessage.value)
  }
}

/** 格式化 JSON */
function handleFormat() {
  try {
    const parsed = JSON.parse(internalValue.value)
    internalValue.value = JSON.stringify(parsed, null, 2)
    emit('update:modelValue', internalValue.value)
    isValid.value = true
    errorMessage.value = ''
    message.success(t('common.formatSuccess'))
  } catch {
    message.error(t('common.jsonFormatError'))
  }
}

/** 复制到剪贴板 */
async function handleCopy() {
  try {
    await navigator.clipboard.writeText(internalValue.value)
    message.success(t('common.copiedToClipboard'))
  } catch {
    message.error(t('common.copyFailed'))
  }
}

/** 清空内容 */
function handleClear() {
  internalValue.value = ''
  emit('update:modelValue', '')
  isValid.value = null
  errorMessage.value = ''
}

/** 同步行号滚动 */
function syncScroll() {
  if (textareaRef.value && linesRef.value) {
    linesRef.value.scrollTop = textareaRef.value.scrollTop
  }
}

// 初始验证
nextTick(validateJson)
</script>

<style scoped>
.json-editor {
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  overflow: hidden;
  background: #fafafa;
}

.json-editor-readonly {
  background: #f5f5f5;
}

/* 工具栏 */
.json-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: white;
  border-bottom: 1px solid #e5e5e5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  font-size: 12px;
  font-weight: 600;
  color: #737373;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.validation-tag {
  font-size: 11px;
  margin: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* 编辑器主体 */
.json-editor-body {
  display: flex;
  overflow: hidden;
}

.json-editor-lines {
  flex-shrink: 0;
  width: 48px;
  padding: 12px 0;
  background: #f0f0f0;
  overflow: hidden;
  text-align: right;
  user-select: none;
}

.json-editor-line-number {
  padding: 0 12px 0 8px;
  font-size: 12px;
  line-height: 20px;
  color: #a3a3a3;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}

.json-editor-textarea {
  flex: 1;
  padding: 12px 16px;
  border: none;
  outline: none;
  resize: none;
  font-size: 13px;
  line-height: 20px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  color: #171717;
  background: transparent;
  tab-size: 2;
}

.json-editor-textarea:focus {
  background: white;
}

.json-editor-textarea:read-only {
  color: #525252;
  cursor: default;
}

/* 错误信息 */
.json-editor-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #dc2626;
  background: #fef2f2;
  border-top: 1px solid #fecaca;
}

/* 暗色模式 */
:global(.dark) .json-editor {
  border-color: #262626;
  background: #171717;
}

:global(.dark) .json-editor-readonly {
  background: #141414;
}

:global(.dark) .json-editor-toolbar {
  background: #1a1a1a;
  border-bottom-color: #262626;
}

:global(.dark) .json-editor-lines {
  background: #1a1a1a;
}

:global(.dark) .json-editor-line-number {
  color: #525252;
}

:global(.dark) .json-editor-textarea {
  color: #e5e5e5;
}

:global(.dark) .json-editor-textarea:focus {
  background: #1a1a1a;
}

:global(.dark) .json-editor-textarea:read-only {
  color: #a3a3a3;
}

:global(.dark) .json-editor-error {
  color: #f87171;
  background: rgba(239, 68, 68, 0.1);
  border-top-color: rgba(239, 68, 68, 0.2);
}
</style>
