<template>
  <div class="code-editor" :class="{ 'code-editor-readonly': readonly }">
    <!-- 工具栏 -->
    <div v-if="!readonly" class="code-editor-toolbar">
      <div class="toolbar-left">
        <span class="language-tag">{{ language.toUpperCase() }}</span>
        <a-tag v-if="lineCount > 0" color="default" class="line-count-tag">
          {{ lineCount }} {{ t('common.lines') }}
        </a-tag>
      </div>
      <div class="toolbar-right">
        <a-button type="text" size="small" @click="handleCopy">
          <template #icon>
            <CopyOutlined />
          </template>
          {{ t('common.copy') }}
        </a-button>
        <a-button v-if="!readonly" type="text" size="small" @click="handleFormat">
          <template #icon>
            <AlignLeftOutlined />
          </template>
          {{ t('common.format') }}
        </a-button>
      </div>
    </div>

    <!-- 编辑区域 -->
    <div class="code-editor-body" :style="{ height: height ? `${height}px` : '300px' }">
      <!-- 行号 -->
      <div class="code-editor-lines" ref="linesRef">
        <div
          v-for="line in lineCount"
          :key="line"
          class="code-editor-line-number"
        >
          {{ line }}
        </div>
      </div>

      <!-- 代码区域 -->
      <textarea
        ref="textareaRef"
        v-model="internalValue"
        class="code-editor-textarea"
        :readonly="readonly"
        spellcheck="false"
        @input="handleInput"
        @scroll="syncScroll"
        @keydown="handleKeydown"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CopyOutlined, AlignLeftOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()

/**
 * CodeEditor 组件
 * 简易代码编辑器/查看器
 * 支持行号、语法高亮（通过颜色主题）、Tab 缩进
 * 用于 AgentDebugger、ApiDocumentation
 */

interface Props {
  /** 编程语言 */
  language?: string
  /** 代码内容（双向绑定） */
  modelValue: string
  /** 是否只读 */
  readonly?: boolean
  /** 编辑器高度 */
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  language: 'json',
  readonly: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const internalValue = ref(props.modelValue)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const linesRef = ref<HTMLDivElement | null>(null)

// 行数
const lineCount = computed(() => {
  if (!internalValue.value) return 1
  return internalValue.value.split('\n').length
})

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  internalValue.value = val
})

/** 输入处理 */
function handleInput() {
  emit('update:modelValue', internalValue.value)
}

/** 键盘事件处理（Tab 缩进） */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    e.preventDefault()
    const textarea = textareaRef.value
    if (!textarea) return

    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const value = internalValue.value

    internalValue.value = value.substring(0, start) + '  ' + value.substring(end)
    emit('update:modelValue', internalValue.value)

    nextTick(() => {
      textarea.selectionStart = textarea.selectionEnd = start + 2
    })
  }
}

/** 复制代码 */
async function handleCopy() {
  try {
    await navigator.clipboard.writeText(internalValue.value)
    message.success(t('common.copiedToClipboard'))
  } catch {
    message.error(t('common.copyFailed'))
  }
}

/** 格式化代码（仅 JSON） */
function handleFormat() {
  if (props.language === 'json') {
    try {
      const parsed = JSON.parse(internalValue.value)
      internalValue.value = JSON.stringify(parsed, null, 2)
      emit('update:modelValue', internalValue.value)
      message.success(t('common.formatSuccess'))
    } catch {
      message.error(t('common.jsonFormatError'))
    }
  } else {
    message.info(t('common.formatNotSupported'))
  }
}

/** 同步行号滚动 */
function syncScroll() {
  if (textareaRef.value && linesRef.value) {
    linesRef.value.scrollTop = textareaRef.value.scrollTop
  }
}
</script>

<style scoped>
.code-editor {
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  overflow: hidden;
  background: #1e1e1e;
}

.code-editor-readonly {
  opacity: 0.95;
}

/* 工具栏 */
.code-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.language-tag {
  font-size: 11px;
  font-weight: 600;
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.15);
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.line-count-tag {
  font-size: 11px !important;
  margin: 0 !important;
  color: #a3a3a3 !important;
  background: transparent !important;
  border: none !important;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-right :deep(.ant-btn) {
  color: #a3a3a3 !important;
}

.toolbar-right :deep(.ant-btn:hover) {
  color: #e5e5e5 !important;
  background: rgba(255, 255, 255, 0.05) !important;
}

/* 编辑器主体 */
.code-editor-body {
  display: flex;
  overflow: hidden;
}

.code-editor-lines {
  flex-shrink: 0;
  width: 48px;
  padding: 12px 0;
  background: #252526;
  overflow: hidden;
  text-align: right;
  user-select: none;
}

.code-editor-line-number {
  padding: 0 12px 0 8px;
  font-size: 12px;
  line-height: 20px;
  color: #636363;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Courier New', monospace;
}

.code-editor-textarea {
  flex: 1;
  padding: 12px 16px;
  border: none;
  outline: none;
  resize: none;
  font-size: 13px;
  line-height: 20px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', 'Courier New', monospace;
  color: #d4d4d4;
  background: #1e1e1e;
  tab-size: 2;
  white-space: pre;
  overflow-wrap: normal;
  overflow-x: auto;
}

.code-editor-textarea:focus {
  background: #1e1e1e;
}

.code-editor-textarea:read-only {
  color: #9ca3af;
  cursor: default;
}

/* 滚动条样式 */
.code-editor-textarea::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.code-editor-textarea::-webkit-scrollbar-track {
  background: transparent;
}

.code-editor-textarea::-webkit-scrollbar-thumb {
  background: #424242;
  border-radius: 4px;
}

.code-editor-textarea::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
