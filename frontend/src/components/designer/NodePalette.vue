<template>
  <div class="node-palette" :class="{ collapsed: collapsed }">
    <!-- Toggle Button -->
    <div class="palette-toggle" @click="$emit('toggle-collapse')">
      <svg
        class="toggle-icon"
        :class="{ rotated: collapsed }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        width="14"
        height="14"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
      <span v-if="!collapsed" class="palette-title">节点面板</span>
    </div>

    <!-- Content -->
    <div v-show="!collapsed" class="palette-content">
      <!-- Search -->
      <div class="search-wrapper">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="14" height="14">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <input
          v-model="searchText"
          class="search-input"
          type="text"
          placeholder="搜索节点..."
        />
        <button v-if="searchText" class="search-clear" @click="searchText = ''">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="12" height="12">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Category Groups -->
      <div v-for="category in filteredCategories" :key="category.key" class="category-group">
        <div class="category-header" @click="toggleCategory(category.key)">
          <svg
            class="category-arrow"
            :class="{ collapsed: collapsedCategories[category.key] }"
            fill="currentColor"
            viewBox="0 0 20 20"
            width="12"
            height="12"
          >
            <path
              fill-rule="evenodd"
              d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
              clip-rule="evenodd"
            />
          </svg>
          <span class="category-icon">{{ category.icon }}</span>
          <span class="category-label">{{ category.label }}</span>
          <span class="category-count">{{ getCategoryNodeCount(category.key) }}</span>
        </div>

        <transition name="slide">
          <div v-show="!collapsedCategories[category.key]" class="node-list">
            <div
              v-for="nodeType in getFilteredNodesByCategory(category.key)"
              :key="nodeType.type"
              class="node-item"
              draggable="true"
              :title="nodeType.description"
              @dragstart="handleDragStart($event, nodeType)"
            >
              <span class="node-item-icon">{{ nodeType.icon }}</span>
              <div class="node-item-info">
                <span class="node-item-name">{{ nodeType.name }}</span>
                <span class="node-item-desc">{{ nodeType.description }}</span>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <!-- Empty state -->
      <div v-if="filteredCategories.length === 0" class="empty-state">
        未找到匹配的节点
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import type { NodeTypeDefinition } from '@/composables/designer/types'
import { getNodeTypesByCategory } from '@/composables/designer/nodeRegistry'

const nodeCategories = [
  { key: 'flow', label: '流程控制', icon: '🔀' },
  { key: 'ai', label: 'AI 能力', icon: '🤖' },
  { key: 'integration', label: '集成', icon: '🔌' },
  { key: 'advanced', label: '高级', icon: '⚙️' },
]

defineProps<{
  collapsed?: boolean
}>()

defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'dragstart', nodeType: NodeTypeDefinition): void
}>()

const searchText = ref('')
const collapsedCategories = reactive<Record<string, boolean>>({
  flow: false,
  ai: false,
  integration: false,
  advanced: false,
})

const filteredCategories = computed(() => {
  if (!searchText.value.trim()) return nodeCategories
  const keyword = searchText.value.trim().toLowerCase()
  return nodeCategories.filter(cat => {
    const nodes = getNodeTypesByCategory(cat.key as NodeTypeDefinition['category'])
    return nodes.some(
      n =>
        n.name.toLowerCase().includes(keyword) ||
        n.type.toLowerCase().includes(keyword) ||
        n.description.toLowerCase().includes(keyword)
    )
  })
})

function toggleCategory(key: string) {
  collapsedCategories[key] = !collapsedCategories[key]
}

function getFilteredNodesByCategory(category: string): NodeTypeDefinition[] {
  const nodes = getNodeTypesByCategory(category as NodeTypeDefinition['category'])
  if (!searchText.value.trim()) return nodes
  const keyword = searchText.value.trim().toLowerCase()
  return nodes.filter(
    n =>
      n.name.toLowerCase().includes(keyword) ||
      n.type.toLowerCase().includes(keyword) ||
      n.description.toLowerCase().includes(keyword)
  )
}

function getCategoryNodeCount(category: string): number {
  return getFilteredNodesByCategory(category).length
}

function handleDragStart(event: DragEvent, nodeType: NodeTypeDefinition) {
  event.dataTransfer?.setData('application/json', JSON.stringify(nodeType))
  event.dataTransfer!.effectAllowed = 'copy'
}
</script>

<style scoped>
.node-palette {
  width: 240px;
  background: #16162a;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.2s ease;
}

.node-palette.collapsed {
  width: 40px;
}

/* Toggle */
.palette-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  user-select: none;
  flex-shrink: 0;
}

.palette-toggle:hover {
  background: rgba(255, 255, 255, 0.03);
}

.toggle-icon {
  color: rgba(226, 232, 240, 0.5);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.palette-title {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
}

/* Content */
.palette-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* Search */
.search-wrapper {
  position: relative;
  padding: 8px 10px;
  flex-shrink: 0;
}

.search-icon {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(226, 232, 240, 0.3);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 6px 28px 6px 30px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 12px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
}

.search-input::placeholder {
  color: rgba(226, 232, 240, 0.3);
}

.search-input:focus {
  border-color: rgba(24, 144, 255, 0.5);
}

.search-clear {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: rgba(226, 232, 240, 0.3);
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
}

.search-clear:hover {
  color: rgba(226, 232, 240, 0.6);
}

/* Category */
.category-group {
  flex-shrink: 0;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s ease;
}

.category-header:hover {
  background: rgba(255, 255, 255, 0.03);
}

.category-arrow {
  color: rgba(226, 232, 240, 0.3);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.category-arrow.collapsed {
  transform: rotate(-90deg);
}

.category-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.category-label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(226, 232, 240, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex: 1;
}

.category-count {
  font-size: 10px;
  color: rgba(226, 232, 240, 0.25);
  background: rgba(255, 255, 255, 0.05);
  padding: 1px 6px;
  border-radius: 8px;
}

/* Node List */
.node-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 2px 8px 8px;
}

.node-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 6px;
  cursor: grab;
  transition: all 0.15s ease;
}

.node-item:hover {
  background: rgba(24, 144, 255, 0.08);
  border-color: rgba(24, 144, 255, 0.2);
}

.node-item:active {
  cursor: grabbing;
  opacity: 0.8;
}

.node-item-icon {
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 1px;
}

.node-item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.node-item-name {
  font-size: 12px;
  font-weight: 500;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-item-desc {
  font-size: 10px;
  color: rgba(226, 232, 240, 0.35);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

/* Empty state */
.empty-state {
  padding: 24px 12px;
  text-align: center;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.3);
}

/* Slide transition */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
}

.slide-enter-to,
.slide-leave-from {
  opacity: 1;
  max-height: 500px;
}

/* Scrollbar */
.palette-content::-webkit-scrollbar {
  width: 4px;
}

.palette-content::-webkit-scrollbar-track {
  background: transparent;
}

.palette-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
}

.palette-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.15);
}
</style>
