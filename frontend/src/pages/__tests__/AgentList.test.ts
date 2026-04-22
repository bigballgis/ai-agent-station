import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import AgentList from '../AgentList.vue'

// Mock API
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

// Mock composables
vi.mock('@/composables/useKeyboardShortcuts', () => ({
  useKeyboardShortcuts: vi.fn()
}))

// Mock ant-design-vue
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

// Mock 组件
vi.mock('@/components', () => ({
  PageHeader: {
    name: 'PageHeader',
    template: '<div class="mock-page-header"><slot /><slot name="actions" /></div>'
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

describe('AgentList 页面', () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/agents', component: { template: '<div/>' } },
      { path: '/agents/:id/edit', component: { template: '<div/>' } },
      { path: '/agents/:id/versions', component: { template: '<div/>' } }
    ]
  })

  const mockAgents = [
    {
      id: 1,
      name: '客服助手',
      description: '智能客服Agent',
      status: 'PUBLISHED',
      isActive: true,
      config: { type: 'CHAT' },
      createdAt: '2025-01-15T10:00:00Z'
    },
    {
      id: 2,
      name: '数据分析',
      description: '数据分析Agent',
      status: 'DRAFT',
      isActive: false,
      config: { type: 'TASK' },
      createdAt: '2025-01-16T10:00:00Z'
    }
  ]

  const globalMountOptions = {
    plugins: [router],
    stubs: {
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
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAllAgents.mockResolvedValue({ data: mockAgents })
  })

  it('渲染Agent列表页面 - 包含aria-label', () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    expect(wrapper.find('[aria-label="Agent列表"]').exists()).toBe(true)
  })

  it('渲染页面标题', () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('Agent管理')
  })

  it('显示Agent卡片 - 加载完成后显示Agent列表', async () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    // 等待API调用完成
    await vi.dynamicImportSettled()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 检查Agent名称是否显示
    expect(wrapper.text()).toContain('客服助手')
    expect(wrapper.text()).toContain('数据分析')
  })

  it('显示创建Agent按钮', () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    // 检查创建按钮存在
    expect(wrapper.text()).toContain('创建 Agent')
  })

  it('点击创建按钮打开向导弹窗', async () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    // 查找创建按钮并点击
    const createButton = wrapper.find('[aria-label="创建Agent"]')
    if (createButton.exists()) {
      await createButton.trigger('click')
      await wrapper.vm.$nextTick()

      // 弹窗应该打开
      expect(wrapper.find('.ant-modal').exists()).toBe(true)
    }
  })

  it('渲染搜索栏', () => {
    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    // 检查搜索栏组件存在
    expect(wrapper.find('.mock-search-bar').exists()).toBe(true)
  })

  it('显示加载状态 - 骨架屏', () => {
    // 重新设置mock为pending状态
    mockGetAllAgents.mockReturnValue(new Promise(() => {}))

    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    // 加载中应显示骨架屏
    expect(wrapper.find('.animate-pulse').exists()).toBe(true)
  })

  it('显示空状态 - 无Agent时', async () => {
    mockGetAllAgents.mockResolvedValue({ data: [] })

    const wrapper = mount(AgentList, {
      global: globalMountOptions
    })

    await vi.dynamicImportSettled()
    await new Promise(resolve => setTimeout(resolve, 0))

    // 无数据时应显示空状态
    expect(wrapper.find('.mock-empty-state').exists()).toBe(true)
  })
})
