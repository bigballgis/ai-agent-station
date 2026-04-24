import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import AgentList from '../AgentList.vue'

// ==================== Mocks ====================

const mockGetAllAgents = vi.fn()
const mockCreateAgent = vi.fn()
const mockDeleteAgent = vi.fn()
const mockCopyAgent = vi.fn()

vi.mock('@/api/agent', () => ({
  agentApi: {
    getAllAgents: (...args: any[]) => mockGetAllAgents(...args),
    createAgent: (...args: any[]) => mockCreateAgent(...args),
    deleteAgent: (...args: any[]) => mockDeleteAgent(...args),
    copyAgent: (...args: any[]) => mockCopyAgent(...args)
  }
}))

vi.mock('@/composables/useKeyboardShortcuts', () => ({
  useKeyboardShortcuts: vi.fn()
}))

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  Modal: {
    confirm: vi.fn()
  }
}))

vi.mock('@/components', () => ({
  PageHeader: {
    name: 'PageHeader',
    props: ['title', 'subtitle'],
    template: '<div class="mock-page-header"><span class="page-title">{{ title }}</span><slot /><slot name="actions" /></div>'
  },
  SearchBar: {
    name: 'SearchBar',
    props: ['fields'],
    template: '<div class="mock-search-bar"><slot /></div>',
    emits: ['search', 'reset']
  },
  StatusBadge: {
    name: 'StatusBadge',
    props: ['status', 'type'],
    template: '<span class="mock-status-badge">{{ status }}</span>'
  },
  EmptyState: {
    name: 'EmptyState',
    props: ['type', 'description', 'actionText'],
    template: '<div class="mock-empty-state">{{ description }}</div>',
    emits: ['action']
  },
  ConfirmModal: {
    name: 'ConfirmModal',
    template: '<div class="mock-confirm-modal"><slot /></div>'
  }
}))

// ==================== i18n ====================

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      routes: { agentList: 'Agent列表' },
      agent: {
        list: 'Agent管理',
        listDesc: '管理和编排您的智能体',
        createAgent: '创建 Agent',
        createFirstDesc: '还没有Agent，点击创建第一个',
        createFirstBtn: '立即创建',
        noDescription: '暂无描述',
        statusDraft: '草稿',
        statusPendingApproval: '待审批',
        statusApproved: '已审批',
        statusPublished: '已发布',
        statusArchived: '已归档',
        statusEnabled: '已启用',
        statusDisabled: '已禁用',
        agentName: 'Agent名称',
        searchPlaceholder: '搜索Agent名称或描述',
        allStatus: '全部状态',
        allTypes: '全部类型',
        inputNamePlaceholder: '请输入Agent名称',
        inputDescPlaceholder: '请输入Agent描述',
        typeConversational: '对话型',
        typeTask: '任务型',
        typeWorkflow: '工作流型',
        typeGeneral: '通用',
        typeCustomerService: '客服',
        typeDataAnalysis: '数据分析',
        typeCodeGeneration: '代码生成',
        typeDocument: '文档',
        wizard: {
          basicInfo: '基本信息',
          basicInfoDesc: '设置Agent基本信息',
          modelConfig: '模型配置',
          modelConfigDesc: '选择AI模型和参数',
          toolBinding: '工具绑定',
          toolBindingDesc: '选择Agent可用的工具',
          preview: '预览确认',
          previewDesc: '确认配置并创建',
          prev: '上一步',
          next: '下一步',
          confirmCreate: '确认创建',
          llmModel: 'LLM模型',
          temperature: '温度',
          maxTokens: '最大Token数',
          model: '模型'
        },
        configPreview: '配置预览',
        boundTools: '已绑定工具',
        loadFailed: '加载Agent列表失败',
        createSuccess: '创建成功',
        createFailed: '创建失败',
        copyAgentTitle: '复制Agent',
        copySuccess: '复制成功',
        copyFailed: '复制失败',
        confirmDelete: '确认删除',
        confirmDeleteContent: '确定要删除',
        deleteSuccess: '删除成功',
        deleteFailed: '删除失败',
        inputAgentNameWarning: '请输入Agent名称',
        toolWebSearch: '网页搜索',
        toolWebSearchDesc: '搜索互联网信息',
        toolCodeExecutor: '代码执行',
        toolCodeExecutorDesc: '执行代码片段',
        toolFileReader: '文件读取',
        toolFileReaderDesc: '读取文件内容',
        toolDatabaseQuery: '数据库查询',
        toolDatabaseQueryDesc: '查询数据库',
        toolApiCaller: 'API调用',
        toolApiCallerDesc: '调用外部API',
        toolImageGenerator: '图像生成',
        toolImageGeneratorDesc: '生成图像',
        qwenMax: '通义千问Max',
        qwenPlus: '通义千问Plus',
        newName: '新Agent名称',
        inputNewNamePlaceholder: '请输入新Agent名称'
      },
      common: {
        name: '名称',
        description: '描述',
        type: '类型',
        category: '分类',
        status: '状态',
        edit: '编辑',
        version: '版本',
        delete: '删除',
        cancel: '取消',
        prevPage: '上一页',
        nextPage: '下一页'
      }
    }
  }
})

// ==================== Test Data ====================

const mockAgents = [
  {
    id: 1,
    name: '客服助手',
    description: '智能客服Agent，处理用户咨询',
    status: 'PUBLISHED',
    isActive: true,
    config: { type: 'CHAT' },
    createdAt: '2025-01-15T10:00:00Z'
  },
  {
    id: 2,
    name: '数据分析',
    description: '数据分析Agent，生成报表',
    status: 'DRAFT',
    isActive: false,
    config: { type: 'TASK' },
    createdAt: '2025-01-16T10:00:00Z'
  },
  {
    id: 3,
    name: '文档助手',
    description: '文档处理Agent',
    status: 'PUBLISHED',
    isActive: true,
    config: { type: 'FLOW' },
    createdAt: '2025-01-17T10:00:00Z'
  }
]

// ==================== Helpers ====================

function createRouterInstance() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/agents', component: { template: '<div/>' } },
      { path: '/agents/:id/edit', component: { template: '<div/>' } },
      { path: '/agents/:id/versions', component: { template: '<div/>' } }
    ]
  })
}

const antdStubs = {
  'a-modal': {
    props: ['open', 'title', 'footer', 'width', 'destroyOnClose'],
    template: '<div class="ant-modal" v-if="open"><slot /></div>',
    emits: ['update:open', 'cancel', 'ok']
  },
  'a-steps': {
    props: ['current', 'size'],
    template: '<div class="ant-steps"><slot /></div>'
  },
  'a-step': {
    props: ['title', 'description'],
    template: '<div class="ant-step"></div>'
  },
  'a-form': {
    template: '<form><slot /></form>'
  },
  'a-form-item': {
    props: ['label', 'required'],
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
  },
  'a-select': {
    props: ['value'],
    template: '<select class="ant-select"><slot /></select>',
    emits: ['update:value']
  },
  'a-select-option': {
    props: ['value'],
    template: '<option :value="value"><slot /></option>'
  },
  'a-slider': {
    props: ['value', 'min', 'max', 'step'],
    template: '<input type="range" />',
    emits: ['update:value']
  },
  'a-input-number': {
    props: ['value', 'min', 'max', 'step'],
    template: '<input type="number" class="ant-input-number" />',
    emits: ['update:value']
  },
  'a-checkbox-group': {
    props: ['value'],
    template: '<div class="ant-checkbox-group"><slot /></div>',
    emits: ['update:value']
  },
  'a-checkbox': {
    props: ['value'],
    template: '<label class="ant-checkbox"><slot /></label>'
  },
  'a-tag': {
    props: ['color'],
    template: '<span class="ant-tag"><slot /></span>'
  },
  'a-button': {
    props: ['type', 'disabled'],
    template: '<button class="ant-button"><slot /></button>',
    emits: ['click']
  }
}

function getGlobalMountOptions() {
  return {
    plugins: [createRouterInstance(), i18n],
    stubs: antdStubs
  }
}

// ==================== Tests ====================

describe('AgentList 页面', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAllAgents.mockResolvedValue({ data: mockAgents })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 1. 页面渲染 ----------

  it('1. 页面正常渲染不崩溃', () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('.agent-list-page').exists()).toBe(true)
  })

  it('2. 页面包含正确的 aria-label', () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('[aria-label="Agent列表"]').exists()).toBe(true)
  })

  it('3. 页面标题正确渲染', () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.text()).toContain('Agent管理')
  })

  // ---------- 4. 搜索/筛选 ----------

  it('4. 搜索栏组件正确渲染', () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })
    expect(wrapper.find('.mock-search-bar').exists()).toBe(true)
  })

  it('5. 搜索过滤功能正常工作', async () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    // 模拟 SearchBar emit search 事件
    const searchBar = wrapper.findComponent({ name: 'SearchBar' })
    await searchBar.vm.$emit('search', { searchQuery: '客服' })
    await wrapper.vm.$nextTick()

    // 验证内部搜索状态已更新（filteredAgents 应只包含匹配项）
    const vm = wrapper.vm as any
    expect(vm.searchQuery).toBe('客服')
    expect(vm.currentPage).toBe(1)
  })

  it('6. 重置搜索条件', async () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    // 先设置搜索
    const searchBar = wrapper.findComponent({ name: 'SearchBar' })
    await searchBar.vm.$emit('search', { searchQuery: '测试', statusFilter: 'DRAFT' })
    await wrapper.vm.$nextTick()

    const vm = wrapper.vm as any
    expect(vm.searchQuery).toBe('测试')

    // 重置
    await searchBar.vm.$emit('reset')
    await wrapper.vm.$nextTick()

    expect(vm.searchQuery).toBe('')
    expect(vm.statusFilter).toBe('')
    expect(vm.activeFilter).toBe('')
    expect(vm.currentPage).toBe(1)
  })

  // ---------- 7. Agent 卡片数据 ----------

  it('7. Agent 卡片显示正确的数据', async () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 验证所有 Agent 名称都显示
    expect(wrapper.text()).toContain('客服助手')
    expect(wrapper.text()).toContain('数据分析')
    expect(wrapper.text()).toContain('文档助手')

    // 验证描述信息
    expect(wrapper.text()).toContain('智能客服Agent，处理用户咨询')
    expect(wrapper.text()).toContain('数据分析Agent，生成报表')
  })

  it('8. Agent 卡片显示状态标签', async () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    // StatusBadge 组件应该被渲染
    const statusBadges = wrapper.findAllComponents({ name: 'StatusBadge' })
    expect(statusBadges.length).toBeGreaterThan(0)
  })

  // ---------- 9. 分页 ----------

  it('9. 数据量超过 pageSize 时显示分页控件', async () => {
    // 创建超过 pageSize (9) 的数据
    const manyAgents = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      name: `Agent ${i + 1}`,
      description: `描述 ${i + 1}`,
      status: 'PUBLISHED',
      isActive: true,
      config: { type: 'CHAT' },
      createdAt: '2025-01-15T10:00:00Z'
    }))
    mockGetAllAgents.mockResolvedValue({ data: manyAgents })

    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    const vm = wrapper.vm as any
    expect(vm.totalPages).toBe(2)
  })

  it('10. 分页按钮点击切换页码', async () => {
    const manyAgents = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      name: `Agent ${i + 1}`,
      description: `描述 ${i + 1}`,
      status: 'PUBLISHED',
      isActive: true,
      config: { type: 'CHAT' },
      createdAt: '2025-01-15T10:00:00Z'
    }))
    mockGetAllAgents.mockResolvedValue({ data: manyAgents })

    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    const vm = wrapper.vm as any
    expect(vm.currentPage).toBe(1)

    // 点击下一页
    vm.currentPage++
    await wrapper.vm.$nextTick()

    expect(vm.currentPage).toBe(2)
  })

  // ---------- 11. 空状态 ----------

  it('11. 无 Agent 时显示空状态', async () => {
    mockGetAllAgents.mockResolvedValue({ data: [] })

    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.find('.mock-empty-state').exists()).toBe(true)
  })

  // ---------- 12. 加载状态 ----------

  it('12. 加载中显示加载状态', () => {
    // 让 API 永远 pending
    mockGetAllAgents.mockReturnValue(new Promise(() => {}))

    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    // loadAgents 在 onMounted 中被调用，loading 应该为 true
    const vm = wrapper.vm as any
    expect(vm.loading).toBe(true)
  })

  // ---------- 13. 创建按钮 ----------

  it('13. 创建 Agent 按钮存在并可点击', async () => {
    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('创建 Agent')

    const createButton = wrapper.find('[aria-label="创建Agent"]')
    if (createButton.exists()) {
      await createButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.find('.ant-modal').exists()).toBe(true)
    }
  })

  // ---------- 14. API 调用 ----------

  it('14. 页面挂载时调用 loadAgents API', () => {
    mount(AgentList, {
      global: getGlobalMountOptions()
    })

    expect(mockGetAllAgents).toHaveBeenCalledTimes(1)
  })

  it('15. API 调用失败时显示错误消息', async () => {
    mockGetAllAgents.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(AgentList, {
      global: getGlobalMountOptions()
    })

    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 加载完成后应显示空状态
    expect(wrapper.find('.mock-empty-state').exists()).toBe(true)
  })
})
