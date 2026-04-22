<template>
  <div class="workflow-designer-page">
    <PageHeader title="工作流设计器" :breadcrumbs="[
      { title: '首页', path: '/dashboard' },
      { title: '工作流设计器' }
    ]" />

    <div class="designer-layout">
      <!-- Left Panel: Definition List -->
      <div class="left-panel">
        <div class="panel-header">
          <h3>工作流定义</h3>
          <a-button type="primary" size="small" @click="showCreateModal = true">
            新建
          </a-button>
        </div>

        <div class="filter-bar">
          <a-select v-model:value="statusFilter" placeholder="状态筛选" allow-clear style="width: 100%" @change="loadDefinitions">
            <a-select-option value="DRAFT">草稿</a-select-option>
            <a-select-option value="PUBLISHED">已发布</a-select-option>
            <a-select-option value="ARCHIVED">已归档</a-select-option>
          </a-select>
        </div>

        <div class="definition-list">
          <div
            v-for="def in definitions"
            :key="def.id"
            :class="['definition-item', { active: selectedDefinition?.id === def.id }]"
            @click="selectDefinition(def)"
          >
            <div class="def-name">{{ def.name }}</div>
            <div class="def-meta">
              <StatusBadge :status="def.status" :status-map="statusMap" />
              <span class="def-version">v{{ def.version }}</span>
            </div>
            <div class="def-desc" v-if="def.description">{{ def.description }}</div>
          </div>
          <a-empty v-if="definitions.length === 0 && !loading" description="暂无工作流定义" />
        </div>
      </div>

      <!-- Center Panel: Definition Detail / Editor -->
      <div class="center-panel">
        <template v-if="selectedDefinition">
          <div class="detail-header">
            <div class="detail-title">
              <h2>{{ selectedDefinition.name }}</h2>
              <StatusBadge :status="selectedDefinition.status" :status-map="statusMap" />
            </div>
            <div class="detail-actions">
              <a-button v-if="selectedDefinition.status === 'DRAFT'" type="primary" @click="showEditModal = true">
                编辑
              </a-button>
              <a-button
                v-if="selectedDefinition.status === 'DRAFT'"
                type="primary"
                @click="handlePublish"
              >
                发布
              </a-button>
              <a-button
                v-if="selectedDefinition.status === 'PUBLISHED'"
                @click="handleStartWorkflow"
              >
                启动实例
              </a-button>
              <a-popconfirm
                v-if="selectedDefinition.status !== 'PUBLISHED'"
                title="确定删除此工作流定义？"
                @confirm="handleDelete"
              >
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </div>
          </div>

          <!-- Nodes Visualization -->
          <a-card title="工作流节点" size="small" style="margin-bottom: 16px">
            <div class="nodes-grid" v-if="parsedNodes.length > 0">
              <div
                v-for="(node, index) in parsedNodes"
                :key="node.id"
                :class="['node-card', `node-type-${node.type.toLowerCase()}`]"
              >
                <div class="node-header">
                  <span class="node-index">#{{ index + 1 }}</span>
                  <span class="node-type-tag">{{ getNodeTypeName(node.type) }}</span>
                </div>
                <div class="node-name">{{ node.name || node.id }}</div>
                <div class="node-config" v-if="node.config">
                  <div v-for="(val, key) in node.config" :key="key" class="config-item">
                    <span class="config-key">{{ key }}:</span>
                    <span class="config-val">{{ typeof val === 'object' ? JSON.stringify(val) : val }}</span>
                  </div>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无节点定义" />
          </a-card>

          <!-- Edges -->
          <a-card title="连接关系" size="small" style="margin-bottom: 16px">
            <div class="edges-list" v-if="parsedEdges.length > 0">
              <div v-for="edge in parsedEdges" :key="edge.id" class="edge-item">
                <span class="edge-source">{{ edge.source }}</span>
                <span class="edge-arrow">--&gt;</span>
                <span class="edge-target">{{ edge.target }}</span>
                <a-tag v-if="edge.label" color="blue" size="small">{{ edge.label }}</a-tag>
              </div>
            </div>
            <a-empty v-else description="暂无连接关系" />
          </a-card>

          <!-- Instances of this definition -->
          <a-card title="最近实例" size="small">
            <a-table
              :columns="instanceColumns"
              :data-source="definitionInstances"
              :loading="loadingInstances"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusBadge :status="record.status" :status-map="instanceStatusMap" />
                </template>
                <template v-else-if="column.key === 'startedAt'">
                  {{ record.startedAt ? formatDate(record.startedAt) : '-' }}
                </template>
                <template v-else-if="column.key === 'completedAt'">
                  {{ record.completedAt ? formatDate(record.completedAt) : '-' }}
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-space>
                    <a-button type="link" size="small" @click="viewInstance(record)">详情</a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </template>

        <a-empty v-else description="请从左侧选择一个工作流定义" style="margin-top: 100px" />
      </div>
    </div>

    <!-- Create Definition Modal -->
    <a-modal
      v-model:open="showCreateModal"
      title="新建工作流定义"
      @ok="handleCreate"
      :confirm-loading="creating"
      width="640px"
    >
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="createForm.name" placeholder="请输入工作流名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="createForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
        <a-form-item label="节点配置 (JSON)">
          <a-textarea
            v-model:value="createForm.nodesJson"
            placeholder='{"nodes": [{"id": "start", "type": "START", "name": "开始"}]}'
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="连接关系 (JSON)">
          <a-textarea
            v-model:value="createForm.edgesJson"
            placeholder='{"edges": [{"id": "e1", "source": "start", "target": "end"}]}'
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="触发器配置 (JSON)">
          <a-textarea
            v-model:value="createForm.triggersJson"
            placeholder='{"type": "manual"}'
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Edit Definition Modal -->
    <a-modal
      v-model:open="showEditModal"
      title="编辑工作流定义"
      @ok="handleUpdate"
      :confirm-loading="updating"
      width="640px"
    >
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.name" placeholder="请输入工作流名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
        <a-form-item label="节点配置 (JSON)">
          <a-textarea
            v-model:value="editForm.nodesJson"
            placeholder='{"nodes": [...]}'
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="连接关系 (JSON)">
          <a-textarea
            v-model:value="editForm.edgesJson"
            placeholder='{"edges": [...]}'
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="触发器配置 (JSON)">
          <a-textarea
            v-model:value="editForm.triggersJson"
            placeholder='{"type": "manual"}'
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Start Workflow Modal -->
    <a-modal
      v-model:open="showStartModal"
      title="启动工作流实例"
      @ok="handleStart"
      :confirm-loading="starting"
      width="500px"
    >
      <a-form layout="vertical">
        <a-form-item :label="`工作流: ${selectedDefinition?.name}`" />
        <a-form-item label="输入变量 (JSON)">
          <a-textarea
            v-model:value="startVariables"
            placeholder='{"key": "value"}'
            :rows="6"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PageHeader, StatusBadge } from '@/components'
import { workflowApi, type WorkflowDefinition, type WorkflowInstance, type WorkflowNode, type WorkflowEdge } from '@/api/workflow'

const loading = ref(false)
const creating = ref(false)
const updating = ref(false)
const starting = ref(false)
const loadingInstances = ref(false)
const definitions = ref<WorkflowDefinition[]>([])
const selectedDefinition = ref<WorkflowDefinition | null>(null)
const definitionInstances = ref<WorkflowInstance[]>([])
const statusFilter = ref<string | undefined>(undefined)

// Modals
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showStartModal = ref(false)

// Forms
const createForm = ref({
  name: '',
  description: '',
  nodesJson: '{"nodes": [{"id": "start", "type": "START", "name": "开始"}, {"id": "end", "type": "END", "name": "结束"}]}',
  edgesJson: '{"edges": [{"id": "e1", "source": "start", "target": "end"}]}',
  triggersJson: '{"type": "manual"}'
})

const editForm = ref({
  name: '',
  description: '',
  nodesJson: '',
  edgesJson: '',
  triggersJson: ''
})

const startVariables = ref('{}')

const statusMap: Record<string, { text: string; color: string }> = {
  DRAFT: { text: '草稿', color: 'default' },
  PUBLISHED: { text: '已发布', color: 'green' },
  ARCHIVED: { text: '已归档', color: 'orange' }
}

const instanceStatusMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待执行', color: 'default' },
  RUNNING: { text: '运行中', color: 'blue' },
  COMPLETED: { text: '已完成', color: 'green' },
  FAILED: { text: '失败', color: 'red' },
  CANCELLED: { text: '已取消', color: 'orange' },
  SUSPENDED: { text: '已挂起', color: 'purple' }
}

const instanceColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '当前节点', dataIndex: 'currentNodeId', key: 'currentNodeId', ellipsis: true },
  { title: '启动时间', key: 'startedAt', width: 170 },
  { title: '完成时间', key: 'completedAt', width: 170 },
  { title: '操作', key: 'actions', width: 80 }
]

const parsedNodes = computed<WorkflowNode[]>(() => {
  if (!selectedDefinition.value?.nodes) return []
  const nodes = (selectedDefinition.value.nodes as any).nodes
  return Array.isArray(nodes) ? nodes : []
})

const parsedEdges = computed<WorkflowEdge[]>(() => {
  if (!selectedDefinition.value?.edges) return []
  const edges = (selectedDefinition.value.edges as any).edges
  return Array.isArray(edges) ? edges : []
})

function getNodeTypeName(type: string) {
  const map: Record<string, string> = {
    START: '开始',
    END: '结束',
    AGENT: 'AI Agent',
    APPROVAL: '审批',
    CONDITION: '条件',
    NOTIFY: '通知',
    HTTP: 'HTTP请求',
    DELAY: '延迟',
    PARALLEL: '并行'
  }
  return map[type] || type
}

function formatDate(date: string) {
  return new Date(date).toLocaleString('zh-CN')
}

async function loadDefinitions() {
  loading.value = true
  try {
    const res = await workflowApi.getDefinitions(0, 100, statusFilter.value)
    definitions.value = res.data?.data?.records || []
  } catch (error) {
    message.error('加载工作流定义失败')
  } finally {
    loading.value = false
  }
}

function selectDefinition(def: WorkflowDefinition) {
  selectedDefinition.value = def
  loadDefinitionInstances(def.id)
}

async function loadDefinitionInstances(definitionId: number) {
  loadingInstances.value = true
  try {
    const res = await workflowApi.getInstances(0, 10, { definitionId })
    definitionInstances.value = res.data?.data?.records || []
  } catch (error) {
    message.error('加载实例列表失败')
  } finally {
    loadingInstances.value = false
  }
}

function safeJsonParse(str: string) {
  try {
    return JSON.parse(str)
  } catch {
    return undefined
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    message.warning('请输入工作流名称')
    return
  }
  creating.value = true
  try {
    await workflowApi.createDefinition({
      name: createForm.value.name,
      description: createForm.value.description || undefined,
      nodes: safeJsonParse(createForm.value.nodesJson),
      edges: safeJsonParse(createForm.value.edgesJson),
      triggers: safeJsonParse(createForm.value.triggersJson)
    })
    message.success('创建成功')
    showCreateModal.value = false
    createForm.value = {
      name: '',
      description: '',
      nodesJson: '{"nodes": [{"id": "start", "type": "START", "name": "开始"}, {"id": "end", "type": "END", "name": "结束"}]}',
      edgesJson: '{"edges": [{"id": "e1", "source": "start", "target": "end"}]}',
      triggersJson: '{"type": "manual"}'
    }
    loadDefinitions()
  } catch (error) {
    message.error('创建失败')
  } finally {
    creating.value = false
  }
}

// openEditModal 已移除，使用 editDefinition 代替

async function handleUpdate() {
  if (!selectedDefinition.value || !editForm.value.name.trim()) {
    message.warning('请输入工作流名称')
    return
  }
  updating.value = true
  try {
    await workflowApi.updateDefinition(selectedDefinition.value.id, {
      name: editForm.value.name,
      description: editForm.value.description || undefined,
      nodes: safeJsonParse(editForm.value.nodesJson),
      edges: safeJsonParse(editForm.value.edgesJson),
      triggers: safeJsonParse(editForm.value.triggersJson)
    })
    message.success('更新成功')
    showEditModal.value = false
    loadDefinitions()
    // Refresh selected definition
    const res = await workflowApi.getDefinition(selectedDefinition.value.id)
    selectedDefinition.value = res.data?.data
  } catch (error) {
    message.error('更新失败')
  } finally {
    updating.value = false
  }
}

async function handlePublish() {
  if (!selectedDefinition.value) return
  try {
    await workflowApi.publishDefinition(selectedDefinition.value.id)
    message.success('发布成功')
    loadDefinitions()
    const res = await workflowApi.getDefinition(selectedDefinition.value.id)
    selectedDefinition.value = res.data?.data
  } catch (error) {
    message.error('发布失败')
  }
}

async function handleDelete() {
  if (!selectedDefinition.value) return
  try {
    await workflowApi.deleteDefinition(selectedDefinition.value.id)
    message.success('删除成功')
    selectedDefinition.value = null
    loadDefinitions()
  } catch (error) {
    message.error('删除失败')
  }
}

function handleStartWorkflow() {
  startVariables.value = '{}'
  showStartModal.value = true
}

async function handleStart() {
  if (!selectedDefinition.value) return
  starting.value = true
  try {
    const variables = safeJsonParse(startVariables.value)
    await workflowApi.startWorkflow(selectedDefinition.value.id, variables)
    message.success('工作流已启动')
    showStartModal.value = false
    loadDefinitionInstances(selectedDefinition.value.id)
  } catch (error) {
    message.error('启动失败')
  } finally {
    starting.value = false
  }
}

function viewInstance(instance: WorkflowInstance) {
  // Navigate to instance page
  window.location.hash = `/workflow/instances?id=${instance.id}`
}

onMounted(() => {
  loadDefinitions()
})
</script>

<style scoped>
.workflow-designer-page {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.designer-layout {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

.left-panel {
  width: 300px;
  min-width: 300px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.filter-bar {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.definition-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.definition-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.definition-item:hover {
  background: #f5f5f5;
}

.definition-item.active {
  background: #e6f7ff;
  border-color: #1890ff;
}

.def-name {
  font-weight: 500;
  margin-bottom: 6px;
  color: #333;
}

.def-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.def-version {
  font-size: 12px;
  color: #999;
}

.def-desc {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.center-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 20px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-title h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.nodes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.node-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  transition: box-shadow 0.2s;
}

.node-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.node-index {
  font-size: 12px;
  color: #999;
}

.node-type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
}

.node-type-start .node-type-tag { background: #e6f7e6; color: #52c41a; }
.node-type-end .node-type-tag { background: #fff1f0; color: #f5222d; }
.node-type-agent .node-type-tag { background: #e6f7ff; color: #1890ff; }
.node-type-approval .node-type-tag { background: #fff7e6; color: #fa8c16; }
.node-type-condition .node-type-tag { background: #f9f0ff; color: #722ed1; }
.node-type-notify .node-type-tag { background: #e6fffb; color: #13c2c2; }
.node-type-http .node-type-tag { background: #fcffe6; color: #a0d911; }
.node-type-delay .node-type-tag { background: #fff0f6; color: #eb2f96; }
.node-type-parallel .node-type-tag { background: #e6f7ff; color: #2f54eb; }

.node-name {
  font-weight: 500;
  margin-bottom: 6px;
  color: #333;
}

.node-config {
  font-size: 12px;
  color: #666;
}

.config-item {
  margin-bottom: 2px;
}

.config-key {
  color: #999;
}

.edges-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edge-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
}

.edge-source,
.edge-target {
  font-weight: 500;
  color: #333;
}

.edge-arrow {
  color: #999;
}
</style>
