<template>
  <div class="tab-nav">
    <div class="tab-nav-list">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-nav-item"
        :class="{ 'tab-nav-item-active': activeKey === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <!-- 图标 -->
        <component v-if="tab.icon" :is="tab.icon" class="tab-nav-icon" />

        <!-- 标签文本 -->
        <span class="tab-nav-label">{{ tab.label }}</span>

        <!-- 徽标计数 -->
        <span
          v-if="tab.count !== undefined && tab.count > 0"
          class="tab-nav-badge"
          :class="{ 'tab-nav-badge-active': activeKey === tab.key }"
        >
          {{ tab.count > 99 ? '99+' : tab.count }}
        </span>

        <!-- 激活指示条 -->
        <span v-if="activeKey === tab.key" class="tab-nav-indicator" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

/**
 * TabNav 组件
 * 标签页导航组件
 * 支持图标、徽标计数、激活指示条
 * 用于 AlertNotification、LogCenter 等页面
 */

// Tab 项类型
export interface TabItem {
  /** 唯一标识 */
  key: string
  /** 标签文本 */
  label: string
  /** 图标组件 */
  icon?: Component
  /** 徽标计数 */
  count?: number
}

interface Props {
  /** Tab 列表 */
  tabs: TabItem[]
  /** 当前激活的 Tab */
  activeKey: string
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:activeKey', key: string): void
}>()

/** Tab 切换处理 */
function handleTabChange(key: string) {
  emit('update:activeKey', key)
}
</script>

<style scoped>
.tab-nav {
  width: 100%;
}

.tab-nav-list {
  display: flex;
  align-items: center;
  gap: 4px;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 4px;
}

.tab-nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #737373;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-nav-item:hover {
  color: #404040;
}

.tab-nav-item-active {
  color: #2563eb;
}

.tab-nav-icon {
  font-size: 16px;
}

.tab-nav-label {
  line-height: 1;
}

/* 徽标计数 */
.tab-nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  background: #f0f0f0;
  color: #737373;
  line-height: 1;
}

.tab-nav-badge-active {
  background: rgba(59, 130, 246, 0.15);
  color: #2563eb;
}

/* 激活指示条 */
.tab-nav-indicator {
  position: absolute;
  bottom: -1px;
  left: 16px;
  right: 16px;
  height: 2px;
  border-radius: 1px;
  background: #3b82f6;
}

/* 暗色模式 */
:global(.dark) .tab-nav-list {
  border-bottom-color: #262626;
}

:global(.dark) .tab-nav-item {
  color: #a3a3a3;
}

:global(.dark) .tab-nav-item:hover {
  color: #d4d4d4;
}

:global(.dark) .tab-nav-item-active {
  color: #60a5fa;
}

:global(.dark) .tab-nav-badge {
  background: #333;
  color: #a3a3a3;
}

:global(.dark) .tab-nav-badge-active {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

:global(.dark) .tab-nav-indicator {
  background: #60a5fa;
}
</style>
