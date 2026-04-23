<template>
  <div class="workflow-designer-page">
    <PageHeader :title="t('workflow.designer')" :breadcrumbs="[
      { title: t('routes.dashboard'), path: '/dashboard' },
      { title: t('workflow.designer') }
    ]" />

    <div class="designer-layout">
      <!-- Left Panel: Definition List -->
      <div class="left-panel">
        <div class="panel-header">
          <h3>{{ t('workflow.definitionList') }}</h3>
          <a-button type="primary" size="small" @click="showCreateModal = true">
            {{ t('workflow.newCreate') }}
          </a-button>
        </div>

        <div class="filter-bar">
          <a-select v-model:value="statusFilter" :placeholder="t('workflow.statusFilter')" allow-clear style="width: 100%" @change="loadDefinitions">
            <a-select-option value="DRAFT">{{ t('workflow.draft') }}</a-select-option>
            <a-select-option value="PUBLISHED">{{ t('workflow.statuses.published') }}</a-select-option>
            <a-select-option value="ARCHIVED">{{ t('workflow.archived') }}</a-select-option>
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
          <a-empty v-if="definitions.length === 0 && !loading" :description="t('workflow.noDefinitions')" />
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
                {{ t('workflow.edit') }}
              </a-button>
              <a-button
                v-if="selectedDefinition.status === 'DRAFT'"
                type="primary"
                @click="handlePublish"
              >
                {{ t('workflow.publish') }}
              </a-button>
              <a-button
                v-if="selectedDefinition.status === 'PUBLISHED'"
                @click="handleStartWorkflow"
              >
                {{ t('workflow.startInstance') }}
              </a-button>
              <a-popconfirm
                v-if="selectedDefinition.status !== 'PUBLISHED'"
                :title="t('workflow.deleteConfirm')"
                @confirm="handleDelete"
              >
                <a-button danger>{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </div>
          </div>

          <!-- Nodes Visualization -->
          <a-card :title="t('workflow.nodesTitle')" size="small" style="margin-bottom: 16px">
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
            <a-empty v-else :description="t('workflow.noNodes')" />
          </a-card>

          <!-- Edges -->
          <a-card :title="t('workflow.edgesTitle')" size="small" style="margin-bottom: 16px">
            <div class="edges-list" v-if="parsedEdges.length > 0">
              <div v-for="edge in parsedEdges" :key="edge.id" class="edge-item">
                <span class="edge-source">{{ edge.source }}</span>
                <span class="edge-arrow">--&gt;</span>
                <span class="edge-target">{{ edge.target }}</span>
                <a-tag v-if="edge.label" color="blue" size="small">{{ edge.label }}</a-tag>
              </div>
            </div>
            <a-empty v-else :description="t('workflow.noEdges')" />
          </a-card>

          <!-- Instances of this definition -->
          <a-card :title="t('workflow.recentInstances')" size="small">
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
                    <a-button type="link" size="small" @click="viewInstance(record)">{{ t('workflow.detail') }}</a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </template>

        <a-empty v-else :description="t('workflow.selectDefinition')" style="margin-top: 100px" />
      </div>
    </div>

    <!-- Create Definition Modal -->
    <a-modal
      v-model:open="showCreateModal"
      :title="t('workflow.createDefinitionTitle')"
      @ok="handleCreate"
      :confirm-loading="creating"
      width="640px"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('workflow.namePlaceholder').replace('请输入', '')" required>
          <a-input v-model:value="createForm.name" :placeholder="t('workflow.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('workflow.descriptionPlaceholder').replace('请输入', '')">
          <a-textarea v-model:value="createForm.description" :placeholder="t('workflow.descriptionPlaceholder')" :rows="3" />
        </a-form-item>
        <a-form-item :label="t('workflow.nodesConfig')">
          <a-textarea
            v-model:value="createForm.nodesJson"
            :rows="6"
            placeholder='{"nodes": [{"id": "start", "type": "START", "name": "Start"}]}'
          />
        </a-form-item>
        <a-form-item :label="t('workflow.edgesConfig')">
          <a-textarea
            v-model:value="createForm.edgesJson"
            placeholder='{"edges": [{"id": "e1", "source": "start", "target": "end"}]}'
            :rows="4"
          />
        </a-form-item>
        <a-form-item :label="t('workflow.triggersConfig')">
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
      :title="t('workflow.editDefinitionTitle')"
      @ok="handleUpdate"
      :confirm-loading="updating"
      width="640px"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('workflow.namePlaceholder').replace('请输入', '')" required>
          <a-input v-model:value="editForm.name" :placeholder="t('workflow.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('workflow.descriptionPlaceholder').replace('请输入', '')">
          <a-textarea v-model:value="editForm.description" :placeholder="t('workflow.descriptionPlaceholder')" :rows="3" />
        </a-form-item>
        <a-form-item :label="t('workflow.nodesConfig')">
          <a-textarea
            v-model:value="editForm.nodesJson"
            placeholder='{"nodes": [...]}'
            :rows="6"
          />
        </a-form-item>
        <a-form-item :label="t('workflow.edgesConfig')">
          <a-textarea
            v-model:value="editForm.edgesJson"
            placeholder='{"edges": [...]}'
            :rows="4"
          />
        </a-form-item>
        <a-form-item :label="t('workflow.triggersConfig')">
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
      :title="t('workflow.startWorkflowTitle')"
      @ok="handleStart"
      :confirm-loading="starting"
      width="500px"
    >
      <a-form layout="vertical">
        <a-form-item :label="`${t('workflow.workflowLabel')}: ${selectedDefinition?.name}`" />
        <a-form-item :label="t('workflow.instanceDetail.input')">
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
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PageHeader, StatusBadge } from '@/components'
import { workflowApi, type WorkflowDefinition, type WorkflowInstance, type WorkflowNode, type WorkflowEdge } from '@/api/workflow'

const { t } = useI18n()
const router = useRouter()

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
  DRAFT: { text: t('workflow.draft'), color: 'default' },
  PUBLISHED: { text: t('workflow.statuses.published'), color: 'green' },
  ARCHIVED: { text: t('workflow.archived'), color: 'orange' }
}

const instanceStatusMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: t('workflow.instanceStatuses.PENDING'), color: 'default' },
  RUNNING: { text: t('workflow.instanceStatuses.RUNNING'), color: 'blue' },
  COMPLETED: { text: t('workflow.instanceStatuses.COMPLETED'), color: 'green' },
  FAILED: { text: t('workflow.instanceStatuses.FAILED'), color: 'red' },
  CANCELLED: { text: t('workflow.instanceStatuses.CANCELLED'), color: 'orange' },
  SUSPENDED: { text: t('workflow.instanceStatuses.SUSPENDED'), color: 'purple' }
}

const instanceColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('workflow.currentNode'), dataIndex: 'currentNodeId', key: 'currentNodeId', ellipsis: true },
  { title: t('workflow.startTime'), key: 'startedAt', width: 170 },
  { title: t('workflow.endTime'), key: 'completedAt', width: 170 },
  { title: t('common.actions'), key: 'actions', width: 80 }
]

const parsedNodes = computed<WorkflowNode[]>(() => {
  if (!selectedDefinition.value?.nodes) return []
  const nodes = selectedDefinition.value.nodes?.nodes || []
  return Array.isArray(nodes) ? nodes : []
})

const parsedEdges = computed<WorkflowEdge[]>(() => {
  if (!selectedDefinition.value?.edges) return []
  const edges = selectedDefinition.value.edges?.edges || []
  return Array.isArray(edges) ? edges : []
})

function getNodeTypeName(type: string) {
  const map: Record<string, string> = {
    START: t('workflow.nodeTypes.START'),
    END: t('workflow.nodeTypes.END'),
    AGENT: 'AI Agent',
    APPROVAL: t('workflow.nodeTypes.APPROVAL'),
    CONDITION: t('workflow.nodeTypes.CONDITION'),
    NOTIFY: t('workflow.nodeTypes.NOTIFY'),
    HTTP: t('workflow.nodeTypes.HTTP'),
    DELAY: t('workflow.nodeTypes.DELAY'),
    PARALLEL: t('workflow.nodeTypes.PARALLEL')
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
    message.error(t('workflow.loadDefinitionsFailed'))
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
    message.error(t('workflow.loadInstancesFailed'))
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
    message.warning(t('workflow.inputName'))
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
    message.success(t('workflow.createSuccess'))
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
    message.error(t('workflow.createFailed'))
  } finally {
    creating.value = false
  }
}

// openEditModal 已移除，使用 editDefinition 代替

async function handleUpdate() {
  if (!selectedDefinition.value || !editForm.value.name.trim()) {
    message.warning(t('workflow.inputName'))
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
    message.success(t('workflow.updateSuccess'))
    showEditModal.value = false
    loadDefinitions()
    // Refresh selected definition
    const res = await workflowApi.getDefinition(selectedDefinition.value.id)
    selectedDefinition.value = res.data?.data
  } catch (error) {
    message.error(t('workflow.updateFailed'))
  } finally {
    updating.value = false
  }
}

async function handlePublish() {
  if (!selectedDefinition.value) return
  try {
    await workflowApi.publishDefinition(selectedDefinition.value.id)
    message.success(t('workflow.publishSuccess'))
    loadDefinitions()
    const res = await workflowApi.getDefinition(selectedDefinition.value.id)
    selectedDefinition.value = res.data?.data
  } catch (error) {
    message.error(t('workflow.publishFailed'))
  }
}

async function handleDelete() {
  if (!selectedDefinition.value) return
  try {
    await workflowApi.deleteDefinition(selectedDefinition.value.id)
    message.success(t('workflow.deleteSuccess'))
    selectedDefinition.value = null
    loadDefinitions()
  } catch (error) {
    message.error(t('workflow.deleteFailed'))
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
    message.success(t('workflow.workflowStarted'))
    showStartModal.value = false
    loadDefinitionInstances(selectedDefinition.value.id)
  } catch (error) {
    message.error(t('workflow.startFailed'))
  } finally {
    starting.value = false
  }
}

function viewInstance(instance: WorkflowInstance) {
  router.push({ path: '/workflow/instances', query: { id: String(instance.id) } })
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
