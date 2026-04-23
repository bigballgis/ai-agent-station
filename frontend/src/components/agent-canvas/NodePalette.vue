<template>
  <div class="node-panel">
    <h3 class="panel-title">{{ t('canvas.nodePalette.title') }}</h3>
    <div v-for="cat in categories" :key="cat.key" class="node-category">
      <div class="category-header" @click="$emit('toggleCategory', cat.key)">
        <span class="category-arrow" :class="{ collapsed: collapsedCategories[cat.key] }">&#9660;</span>
        <span>{{ cat.label }}</span>
      </div>
      <div v-show="!collapsedCategories[cat.key]" class="node-list">
        <div
          v-for="node in getNodeTypesByCategory(cat.key)"
          :key="node.type"
          class="node-item"
          draggable="true"
          @dragstart="$emit('dragStart', $event, node.type)"
          :title="node.label"
        >
          <span class="node-icon">{{ node.icon }}</span>
          <span class="node-label">{{ node.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

export interface NodeType {
  type: string
  label: string
  icon: string
  category: string
}

export interface Category {
  key: string
  label: string
}

defineProps<{
  categories: Category[]
  collapsedCategories: Record<string, boolean>
  getNodeTypesByCategory: (category: string) => NodeType[]
}>()

defineEmits<{
  (e: 'toggleCategory', key: string): void
  (e: 'dragStart', event: DragEvent, type: string): void
}>()
</script>

<style scoped>
.node-panel {
  width: 220px;
  background: #16213e;
  border-right: 1px solid #2a2a4a;
  padding: 16px 12px;
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

.node-category {
  margin-bottom: 8px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #8888aa;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  cursor: pointer;
  border-radius: 4px;
  user-select: none;
}

.category-header:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #b0b0cc;
}

.category-arrow {
  font-size: 10px;
  transition: transform 0.2s;
  display: inline-block;
}

.category-arrow.collapsed {
  transform: rotate(-90deg);
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s;
  font-size: 13px;
}

.node-item:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.3);
  color: #fff;
}

.node-item:active {
  cursor: grabbing;
}

.node-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.node-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
