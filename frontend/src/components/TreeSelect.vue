<template>
  <div class="tree-select-wrapper">
    <!-- 搜索框 -->
    <div v-if="searchable" class="tree-search">
      <a-input
        v-model:value="searchKeyword"
        placeholder="搜索权限..."
        allow-clear
        size="small"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
    </div>

    <!-- 全选/取消全选 -->
    <div v-if="checkable" class="tree-actions">
      <a-button type="link" size="small" @click="handleCheckAll">
        全选
      </a-button>
      <a-button type="link" size="small" @click="handleUncheckAll">
        取消全选
      </a-button>
    </div>

    <!-- 树组件 -->
    <div class="tree-content">
      <a-tree
        v-model:checkedKeys="internalCheckedKeys"
        v-model:expandedKeys="expandedKeys"
        :tree-data="filteredTreeData"
        :checkable="checkable"
        :selectable="!checkable"
        :check-strictly="checkStrictly"
        :default-expand-all="defaultExpandAll"
        class="permission-tree"
        @check="handleCheck"
        @select="handleSelect"
      >
        <template #title="{ title, key }">
          <span class="tree-node-title">
            <span class="tree-node-label">{{ title }}</span>
            <span v-if="checkable" class="tree-node-key">{{ key }}</span>
          </span>
        </template>
      </a-tree>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredTreeData.length === 0 && searchKeyword" class="tree-empty">
      <span>未找到匹配的权限</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'

/**
 * TreeSelect 组件
 * 增强版树形选择组件，用于权限管理
 * 支持搜索过滤、全选/取消全选、勾选
 */

// 树节点数据类型
export interface TreeNode {
  title: string
  key: string
  value?: string
  children?: TreeNode[]
  disabled?: boolean
  icon?: any
}

interface Props {
  /** 树数据 */
  treeData: TreeNode[]
  /** 是否可勾选 */
  checkable?: boolean
  /** 已勾选的节点 key */
  checkedKeys?: string[]
  /** 是否支持搜索 */
  searchable?: boolean
  /** 是否严格勾选（不关联父子） */
  checkStrictly?: boolean
  /** 是否默认展开所有 */
  defaultExpandAll?: boolean
  /** 占位文本 */
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  checkable: true,
  searchable: true,
  checkStrictly: false,
  defaultExpandAll: false,
  placeholder: '请选择',
})

const emit = defineEmits<{
  (ev: 'update:checkedKeys', keys: string[]): void
  (ev: 'check', checkedKeys: string[], event: any): void
  (ev: 'select', selectedKeys: string[], event: any): void
}>()

// 内部状态
const searchKeyword = ref('')
const expandedKeys = ref<string[]>([])
const internalCheckedKeys = ref<string[]>(props.checkedKeys || [])

// 监听外部 checkedKeys 变化
watch(() => props.checkedKeys, (val) => {
  internalCheckedKeys.value = val || []
}, { deep: true })

/**
 * 递归搜索过滤树节点
 * 如果子节点匹配，则保留父节点路径
 */
function filterTree(nodes: TreeNode[], keyword: string): TreeNode[] {
  if (!keyword) return nodes
  const lowerKeyword = keyword.toLowerCase()

  return nodes.reduce<TreeNode[]>((acc, node) => {
    const children = node.children ? filterTree(node.children, keyword) : []
    const titleMatch = node.title.toLowerCase().includes(lowerKeyword) ||
                       node.key.toLowerCase().includes(lowerKeyword)

    if (titleMatch || children.length > 0) {
      acc.push({
        ...node,
        children: children.length > 0 ? children : node.children,
      })
    }
    return acc
  }, [])
}

// 过滤后的树数据
const filteredTreeData = computed(() => {
  return filterTree(props.treeData, searchKeyword.value)
})

/**
 * 获取所有叶子节点的 key
 */
function getAllLeafKeys(nodes: TreeNode[]): string[] {
  const keys: string[] = []
  function traverse(items: TreeNode[]) {
    items.forEach(item => {
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      } else {
        keys.push(item.key)
      }
    })
  }
  traverse(nodes)
  return keys
}

/**
 * 获取所有节点的 key
 */
function getAllKeys(nodes: TreeNode[]): string[] {
  const keys: string[] = []
  function traverse(items: TreeNode[]) {
    items.forEach(item => {
      keys.push(item.key)
      if (item.children) traverse(item.children)
    })
  }
  traverse(nodes)
  return keys
}

/** 全选 */
function handleCheckAll() {
  const allKeys = props.checkStrictly ? getAllLeafKeys(props.treeData) : getAllKeys(props.treeData)
  internalCheckedKeys.value = allKeys
  emit('update:checkedKeys', allKeys)
}

/** 取消全选 */
function handleUncheckAll() {
  internalCheckedKeys.value = []
  emit('update:checkedKeys', [])
}

/** 勾选变化 */
function handleCheck(checkedKeys: any, e: any) {
  const keys = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked || []
  internalCheckedKeys.value = keys
  emit('update:checkedKeys', keys)
  emit('check', keys, e)
}

/** 选中变化 */
function handleSelect(selectedKeys: string[], e: any) {
  emit('select', selectedKeys, e)
}
</script>

<style scoped>
.tree-select-wrapper {
  width: 100%;
}

/* 搜索框 */
.tree-search {
  margin-bottom: 12px;
}

/* 操作按钮 */
.tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

/* 树内容区域 */
.tree-content {
  max-height: 400px;
  overflow-y: auto;
}

.tree-empty {
  text-align: center;
  padding: 24px;
  color: #a3a3a3;
  font-size: 14px;
}

/* 树节点样式 */
.tree-node-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-node-label {
  font-size: 14px;
  color: #404040;
}

.tree-node-key {
  font-size: 11px;
  color: #a3a3a3;
  font-family: 'SF Mono', 'Consolas', monospace;
}

/* 权限树样式覆盖 */
:deep(.permission-tree .ant-tree-node-content-wrapper) {
  padding: 4px 8px !important;
  border-radius: 8px !important;
  transition: background-color 0.2s;
}

:deep(.permission-tree .ant-tree-node-content-wrapper:hover) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}

:deep(.permission-tree .ant-tree-checkbox) {
  margin-right: 8px;
}

:deep(.permission-tree .ant-tree-treenode) {
  padding: 2px 0 !important;
}

/* 暗色模式 */
:global(.dark) .tree-actions {
  border-bottom-color: #262626;
}

:global(.dark) .tree-node-label {
  color: #d4d4d4;
}

:global(.dark) .tree-node-key {
  color: #525252;
}

:global(.dark) .tree-empty {
  color: #525252;
}

:global(.dark) :deep(.permission-tree .ant-tree-node-content-wrapper:hover) {
  background-color: rgba(255, 255, 255, 0.03) !important;
}
</style>
