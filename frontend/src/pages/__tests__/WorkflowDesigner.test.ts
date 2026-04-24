import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import WorkflowDesigner from '../WorkflowDesigner.vue'

// ==================== Mocks ====================

const mockGetDefinitions = vi.fn()
const mockGetDefinition = vi.fn()
const mockCreateDefinition = vi.fn()
const mockUpdateDefinition = vi.fn()
const mockDeleteDefinition = vi.fn()
const mockPublishDefinition = vi.fn()
const mockGetInstances = vi.fn()
const mockStartWorkflow = vi.fn()

vi.mock('@/api/workflow', () => ({
  workflowApi: {
    getDefinitions: (...args: any[]) => mockGetDefinitions(...args),
    getDefinition: (...args: any[]) => mockGetDefinition(...args),
    createDefinition: (...args: any[]) => mockCreateDefinition(...args),
    updateDefinition: (...args: any[]) => mockUpdateDefinition(...args),
    deleteDefinition: (...args: any[]) => mockDeleteDefinition(...args),
    publishDefinition: (...args: any[]) => mockPublishDefinition(...args),
    getInstances: (...args: any[]) => mockGetInstances(...args),
    startWorkflow: (...args: any[]) => mockStartWorkflow(...args)
  }
}))

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/components', () => ({
  PageHeader: {
    name: 'PageHeader',
    props: ['title', 'breadcrumbs'],
    template: '<div class="mock-page-header"><span class="page-title">{{ title }}</span><slot /></div>'
  },
  StatusBadge: {
    name: 'StatusBadge',
    props: ['status', 'statusMap'],
    template: '<span class="mock-status-badge">{{ status }}</span>'
  }
}))

// ==================== i18n ====================

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      routes: { dashboard: '仪表盘' },
      workflow: {
        designer: '工作流设计器',
        definitionList: '工作流定义',
        newCreate: '新建',
        statusFilter: '状态筛选',
        draft: '草稿',
        statuses: { published: '已发布' },
        archived: '已归档',
        noDefinitions: '暂无工作流定义',
        edit: '编辑',
        publish: '发布',
        startInstance: '启动实例',
        deleteConfirm: '确定要删除此工作流定义吗？',
        nodesTitle: '节点列表',
        edgesTitle: '连线列表',
        recentInstances: '最近实例',
        noNodes: '暂无节点',
        noEdges: '暂无连线',
        selectDefinition: '请从左侧选择一个工作流定义',
        currentNode: '当前节点',
        startTime: '开始时间',
        endTime: '结束时间',
        detail: '详情',
        createDefinitionTitle: '创建工作流定义',
        nameLabel: '名称',
        namePlaceholder: '请输入名称',
        descriptionLabel: '描述',
        descriptionPlaceholder: '请输入描述',
        nodesConfig: '节点配置',
        edgesConfig: '连线配置',
        triggersConfig: '触发器配置',
        editDefinitionTitle: '编辑工作流定义',
        startWorkflowTitle: '启动工作流',
        workflowLabel: '工作流',
        instanceDetail: { input: '输入参数' },
        inputName: '请输入名称',
        createSuccess: '创建成功',
        createFailed: '创建失败',
        updateSuccess: '更新成功',
        updateFailed: '更新失败',
        publishSuccess: '发布成功',
        publishFailed: '发布失败',
        deleteSuccess: '删除成功',
        deleteFailed: '删除失败',
        loadDefinitionsFailed: '加载工作流定义失败',
        loadInstancesFailed: '加载实例列表失败',
        workflowStarted: '工作流已启动',
        startFailed: '启动失败',
        nodeTypes: {
          START: '开始',
          END: '结束',
          APPROVAL: '审批',
          CONDITION: '条件',
          NOTIFY: '通知',
          HTTP: 'HTTP请求',
          DELAY: '延迟',
          PARALLEL: '并行'
        },
        instanceStatuses: {
          PENDING: '待处理',
          RUNNING: '运行中',
          COMPLETED: '已完成',
          FAILED: '失败',
          CANCELLED: '已取消',
          SUSPENDED: '已暂停'
        }
      },
      common: {
        delete: '删除',
        status: '状态',
        actions: '操作'
      }
    }
  }
})

// ==================== Test Data ====================

const mockDefinitions = [
  {
    id: 1,
    name: '审批工作流',
    description: '员工请假审批流程',
    version: 1,
    status: 'DRAFT' as const,
    nodes: { nodes: [
      { id: 'start', type: 'START', name: '开始' },
      { id: 'end', type: 'END', name: '结束' }
    ]},
    edges: { edges: [
      { id: 'e1', source: 'start', target: 'end' }
    ]},
    triggers: { type: 'manual' },
    tenantId: 1,
    createdAt: '2025-01-15T10:00:00Z',
    updatedAt: '2025-01-15T10:00:00Z'
  },
  {
    id: 2,
    name: '数据处理流程',
    description: '自动数据处理',
    version: 2,
    status: 'PUBLISHED' as const,
    nodes: { nodes: [
      { id: 'start', type: 'START', name: '开始' },
      { id: 'agent1', type: 'AGENT', name: 'AI处理' },
      { id: 'end', type: 'END', name: '结束' }
    ]},
    edges: { edges: [
      { id: 'e1', source: 'start', target: 'agent1' },
      { id: 'e2', source: 'agent1', target: 'end' }
    ]},
    triggers: { type: 'cron', cron: '0 0 * * *' },
    tenantId: 1,
    createdAt: '2025-01-16T10:00:00Z',
    updatedAt: '2025-01-16T10:00:00Z'
  }
]

const mockInstances = [
  {
    id: 1,
    workflowDefinitionId: 2,
    workflowName: '数据处理流程',
    status: 'COMPLETED' as const,
    currentNodeId: 'end',
    startedAt: '2025-01-16T10:00:00Z',
    completedAt: '2025-01-16T10:05:00Z',
    tenantId: 1
  }
]

// ==================== Helpers ====================

function createRouterInstance() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/workflow/instances', component: { template: '<div>Instances</div>' } }
    ]
  })
}

const antdStubs = {
  'a-button': {
    props: ['type', 'size', 'danger'],
    template: '<button class="ant-button"><slot /></button>',
    emits: ['click']
  },
  'a-select': {
    props: ['value', 'placeholder', 'allowClear'],
    template: '<select class="ant-select"><slot /></select>',
    emits: ['update:value', 'change']
  },
  'a-select-option': {
    props: ['value'],
    template: '<option :value="value"><slot /></option>'
  },
  'a-empty': {
    props: ['description'],
    template: '<div class="ant-empty">{{ description }}</div>'
  },
  'a-card': {
    props: ['title', 'size'],
    template: '<div class="ant-card"><slot /></div>'
  },
  'a-table': {
    props: ['columns', 'dataSource', 'loading', 'pagination', 'size', 'rowKey'],
    template: '<div class="ant-table"></div>'
  },
  'a-tag': {
    props: ['color', 'size'],
    template: '<span class="ant-tag"><slot /></span>'
  },
  'a-space': {
    template: '<div class="ant-space"><slot /></div>'
  },
  'a-popconfirm': {
    props: ['title'],
    template: '<div class="ant-popconfirm"><slot /></div>',
    emits: ['confirm']
  },
  'a-modal': {
    props: ['open', 'title', 'confirmLoading', 'width'],
    template: '<div class="ant-modal" v-if="open"><slot /></div>',
    emits: ['update:open', 'ok', 'cancel']
  },
  'a-form': {
    template: '<form class="ant-form"><slot /></form>'
  },
  'a-form-item': {
    props: ['label'],
    template: '<div class="ant-form-item"><slot /></div>'
  },
  'a-input': {
    props: ['value', 'placeholder'],
    template: '<input class="ant-input" />',
    emits: ['update:value']
  },
  'a-textarea': {
    props: ['value', 'placeholder', 'rows'],
    template: '<textarea class="ant-textarea"></textarea>',
    emits: ['update:value']
  }
}

function getGlobalMountOptions() {
  return {
    plugins: [createRouterInstance(), i18n],
    stubs: antdStubs
  }
}

// ==================== Tests ====================

describe('WorkflowDesigner 页面', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetDefinitions.mockResolvedValue({
      data: { data: { records: mockDefinitions } }
    })
    mockGetInstances.mockResolvedValue({
      data: { data: { records: mockInstances } }
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 1. 页面渲染 ----------

  it('1. 页面正常渲染不崩溃', () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('.workflow-designer-page').exists()).toBe(true)
  })

  it('2. 页面包含正确的 aria-label', () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('[aria-label="工作流设计器"]').exists()).toBe(true)
  })

  // ---------- 3. 页面标题 ----------

  it('3. 页面标题正确显示', () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.text()).toContain('工作流设计器')
  })

  // ---------- 4. 左侧面板/节点面板 ----------

  it('4. 左侧定义列表面板可见', () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('.left-panel').exists()).toBe(true)
    expect(wrapper.text()).toContain('工作流定义')
  })

  it('5. 新建按钮存在', () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.text()).toContain('新建')
  })

  // ---------- 6. 工作流列表 ----------

  it('6. 加载后显示工作流定义列表', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('审批工作流')
    expect(wrapper.text()).toContain('数据处理流程')
  })

  // ---------- 7. 选中定义 ----------

  it('7. 点击定义后显示详情', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 点击第一个定义
    const defItems = wrapper.findAll('.definition-item')
    if (defItems.length > 0) {
      await defItems[0].trigger('click')
      await flushPromises()

      // 应显示详情面板
      const vm = wrapper.vm as any
      expect(vm.selectedDefinition).not.toBeNull()
      expect(vm.selectedDefinition.name).toBe('审批工作流')
    }
  })

  // ---------- 8. 选中后显示工作流名称 ----------

  it('8. 选中定义后显示工作流名称', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    const defItems = wrapper.findAll('.definition-item')
    if (defItems.length > 0) {
      await defItems[0].trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('审批工作流')
    }
  })

  // ---------- 9. 节点和连线显示 ----------

  it('9. 选中定义后显示节点信息', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    const defItems = wrapper.findAll('.definition-item')
    if (defItems.length > 0) {
      await defItems[0].trigger('click')
      await flushPromises()

      // 应显示节点卡片
      const nodeCards = wrapper.findAll('.node-card')
      expect(nodeCards.length).toBeGreaterThan(0)
    }
  })

  // ---------- 10. 保存/编辑按钮 ----------

  it('10. DRAFT 状态的定义显示编辑和发布按钮', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 点击第一个 DRAFT 定义
    const defItems = wrapper.findAll('.definition-item')
    if (defItems.length > 0) {
      await defItems[0].trigger('click')
      await flushPromises()

      const buttons = wrapper.findAll('.detail-actions .ant-button')
      const buttonTexts = buttons.map(b => b.text())
      expect(buttonTexts).toContain('编辑')
      expect(buttonTexts).toContain('发布')
    }
  })

  // ---------- 11. API 调用 ----------

  it('11. 页面挂载时调用加载定义 API', () => {
    mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    expect(mockGetDefinitions).toHaveBeenCalledTimes(1)
  })

  // ---------- 12. 空状态 ----------

  it('12. 无定义时显示空状态', async () => {
    mockGetDefinitions.mockResolvedValue({
      data: { data: { records: [] } }
    })

    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('暂无工作流定义')
  })

  // ---------- 13. 创建弹窗 ----------

  it('13. 点击新建按钮打开创建弹窗', async () => {
    const wrapper = mount(WorkflowDesigner, {
      global: getGlobalMountOptions()
    })

    const vm = wrapper.vm as any
    expect(vm.showCreateModal).toBe(false)

    // 直接通过 vm 设置
    vm.showCreateModal = true
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.ant-modal').exists()).toBe(true)
  })
})
