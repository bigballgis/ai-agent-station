<template>
  <div class="canvas-toolbar">
    <button @click="$emit('save')" class="btn btn-primary" :title="t('canvas.toolbar.save')">{{ t('canvas.toolbar.save') }}</button>
    <button @click="$emit('clear')" class="btn btn-secondary" :title="t('canvas.toolbar.clear')">{{ t('canvas.toolbar.clear') }}</button>
    <div class="toolbar-separator"></div>
    <button @click="$emit('undo')" class="btn btn-secondary" :title="t('canvas.toolbar.undo')" :disabled="undoStackLength === 0">{{ t('canvas.toolbar.undo') }}</button>
    <div class="toolbar-separator"></div>
    <button @click="$emit('zoomIn')" class="btn btn-icon" :title="t('canvas.toolbar.zoomIn')">+</button>
    <span class="zoom-label">{{ Math.round(scale * 100) }}%</span>
    <button @click="$emit('zoomOut')" class="btn btn-icon" :title="t('canvas.toolbar.zoomOut')">-</button>
    <button @click="$emit('zoomReset')" class="btn btn-secondary" :title="t('canvas.toolbar.resetZoom')">{{ t('canvas.toolbar.resetZoom') }}</button>
    <div class="toolbar-separator"></div>
    <button @click="$emit('autoLayout')" class="btn btn-secondary" :title="t('canvas.toolbar.autoLayout')">{{ t('canvas.toolbar.autoLayout') }}</button>
    <button @click="$emit('validate')" class="btn btn-secondary" :title="t('canvas.toolbar.validate')">{{ t('canvas.toolbar.validate') }}</button>
    <div class="toolbar-spacer"></div>
    <span v-if="validationMessage" class="validation-msg" :class="validationType">
      {{ validationMessage }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  scale: number
  undoStackLength: number
  validationMessage: string
  validationType: 'success' | 'error' | 'warning'
}>()

defineEmits<{
  (e: 'save'): void
  (e: 'clear'): void
  (e: 'undo'): void
  (e: 'zoomIn'): void
  (e: 'zoomOut'): void
  (e: 'zoomReset'): void
  (e: 'autoLayout'): void
  (e: 'validate'): void
}>()
</script>

<style scoped>
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
</style>
