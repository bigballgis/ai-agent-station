<template>
  <div class="designer-toolbar">
    <!-- Left Section -->
    <div class="toolbar-left">
      <button class="toolbar-btn" :title="t('designer.toolbar.back')" @click="$emit('back')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      <div class="toolbar-divider" />

      <button class="toolbar-btn" :title="t('designer.toolbar.undo') + ' (Ctrl+Z)'" :disabled="!canUndo" @click="$emit('undo')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a5 5 0 015 5v2M3 10l4-4M3 10l4 4" />
        </svg>
      </button>
      <button class="toolbar-btn" :title="t('designer.toolbar.redo') + ' (Ctrl+Y)'" :disabled="!canRedo" @click="$emit('redo')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 10H11a5 5 0 00-5 5v2M21 10l-4-4M21 10l-4 4" />
        </svg>
      </button>

      <div class="toolbar-divider" />

      <button class="toolbar-btn" :title="t('designer.toolbar.zoomOut')" @click="$emit('zoom-out')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM13 10H7" />
        </svg>
      </button>
      <span class="zoom-label">{{ Math.round(zoom * 100) }}%</span>
      <button class="toolbar-btn" :title="t('designer.toolbar.zoomIn')" @click="$emit('zoom-in')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v6m3-3H7" />
        </svg>
      </button>
      <button class="toolbar-btn" :title="t('designer.toolbar.resetZoom')" @click="$emit('zoom-reset')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
        </svg>
      </button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-center">
      <input
        class="agent-name-input"
        type="text"
        :value="agentName"
        @input="$emit('update-name', ($event.target as HTMLInputElement).value)"
        :placeholder="t('designer.toolbar.agentNamePlaceholder')"
      />
    </div>

    <!-- Right Section -->
    <div class="toolbar-right">
      <button class="toolbar-btn" :title="t('designer.toolbar.import')" @click="$emit('import')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
        </svg>
      </button>
      <button class="toolbar-btn" :title="t('designer.toolbar.export')" @click="$emit('export')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
        </svg>
      </button>

      <div class="toolbar-divider" />

      <button class="toolbar-btn" :title="t('designer.toolbar.validate')" @click="$emit('validate')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </button>
      <button class="toolbar-btn" :title="t('designer.toolbar.autoLayout')" @click="$emit('auto-layout')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
        </svg>
      </button>

      <div class="toolbar-divider" />

      <button class="toolbar-btn btn-run" :title="t('designer.toolbar.run')" @click="$emit('run')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ t('designer.toolbar.run') }}
      </button>
      <button class="toolbar-btn btn-save" :title="t('designer.toolbar.save') + ' (Ctrl+S)'" @click="$emit('save')">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
        </svg>
        {{ t('designer.toolbar.save') }}
      </button>
    </div>

    <!-- Validation Message -->
    <div v-if="validationMessage" class="validation-message" :class="`validation-${validationType}`">
      {{ validationMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  agentName: string
  canUndo: boolean
  canRedo: boolean
  zoom: number
  validationMessage?: string
  validationType?: 'success' | 'error' | 'warning'
}>()

defineEmits<{
  (e: 'back'): void
  (e: 'undo'): void
  (e: 'redo'): void
  (e: 'zoom-in'): void
  (e: 'zoom-out'): void
  (e: 'zoom-reset'): void
  (e: 'update-name', name: string): void
  (e: 'import'): void
  (e: 'export'): void
  (e: 'validate'): void
  (e: 'auto-layout'): void
  (e: 'run'): void
  (e: 'save'): void
}>()
</script>

<style scoped>
.designer-toolbar {
  display: flex;
  align-items: center;
  height: 48px;
  background: #16162a;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 12px;
  flex-shrink: 0;
  gap: 4px;
  position: relative;
}

/* Left / Right Sections */
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* Center Section */
.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 16px;
  min-width: 0;
}

.agent-name-input {
  width: 100%;
  max-width: 300px;
  padding: 5px 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.agent-name-input::placeholder {
  color: rgba(226, 232, 240, 0.25);
}

.agent-name-input:focus {
  border-color: rgba(24, 144, 255, 0.4);
  background: rgba(255, 255, 255, 0.06);
}

/* Divider */
.toolbar-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.08);
  margin: 0 4px;
  flex-shrink: 0;
}

/* Zoom Label */
.zoom-label {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.4);
  min-width: 40px;
  text-align: center;
  user-select: none;
}

/* Buttons */
.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 8px;
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  color: rgba(226, 232, 240, 0.6);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  user-select: none;
}

.toolbar-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.toolbar-btn:active:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
}

.toolbar-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* Run Button */
.toolbar-btn.btn-run {
  background: rgba(82, 196, 26, 0.12);
  border-color: rgba(82, 196, 26, 0.2);
  color: #52c41a;
  padding: 6px 12px;
}

.toolbar-btn.btn-run:hover:not(:disabled) {
  background: rgba(82, 196, 26, 0.2);
  border-color: rgba(82, 196, 26, 0.35);
  color: #73d13d;
}

/* Save Button */
.toolbar-btn.btn-save {
  background: rgba(24, 144, 255, 0.12);
  border-color: rgba(24, 144, 255, 0.2);
  color: #1890ff;
  padding: 6px 12px;
}

.toolbar-btn.btn-save:hover:not(:disabled) {
  background: rgba(24, 144, 255, 0.2);
  border-color: rgba(24, 144, 255, 0.35);
  color: #40a9ff;
}

/* Validation Message */
.validation-message {
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%) translateY(100%);
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 0 0 6px 6px;
  white-space: nowrap;
  z-index: 50;
  pointer-events: none;
  animation: validation-fade-in 0.2s ease;
}

@keyframes validation-fade-in {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(100%) translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(100%);
  }
}

.validation-success {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
  border: 1px solid rgba(82, 196, 26, 0.25);
  border-top: none;
}

.validation-error {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.25);
  border-top: none;
}

.validation-warning {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
  border: 1px solid rgba(250, 173, 20, 0.25);
  border-top: none;
}
</style>
