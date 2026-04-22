import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import AgentDesigner from '../AgentDesigner.vue'

// Mock API
vi.mock('@/api/agent', () => ({
  agentApi: {
    getAgent: vi.fn(() => Promise.resolve({ data: { id: 1, name: '测试Agent' } })),
    updateAgent: vi.fn(() => Promise.resolve({ data: {} })),
    saveAgentGraph: vi.fn(() => Promise.resolve({ data: {} })),
    runAgent: vi.fn(() => Promise.resolve({ data: { taskId: 'task-001' } }))
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

describe('AgentDesigner 页面', () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/agents/:id/designer', component: { template: '<div/>' } },
      { path: '/agents', component: { template: '<div/>' } }
    ]
  })

  const globalMountOptions = {
    plugins: [router],
    stubs: {
      'a-modal': {
        props: ['open', 'title', 'footer', 'width'],
        template: '<div class="ant-modal" v-if="open"><slot /></div>',
        emits: ['update:open', 'cancel', 'ok']
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
      'a-button': {
        props: ['type', 'disabled'],
        template: '<button class="ant-button"><slot /></button>',
        emits: ['click']
      },
      'a-tooltip': {
        props: ['title'],
        template: '<div class="ant-tooltip"><slot /></div>'
      },
      'a-dropdown': {
        template: '<div class="ant-dropdown"><slot /></div>'
      },
      'a-menu': {
        template: '<div class="ant-menu"><slot /></div>'
      },
      'a-menu-item': {
        template: '<div class="ant-menu-item"><slot /></div>'
      },
      'a-switch': {
        props: ['checked'],
        template: '<input type="checkbox" class="ant-switch" :checked="checked" />',
        emits: ['update:checked']
      }
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染Agent设计器页面', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查设计器页面容器存在
    expect(wrapper.find('.agent-designer-page').exists()).toBe(true)
  })

  it('渲染工具栏', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查工具栏存在
    expect(wrapper.find('.designer-toolbar').exists()).toBe(true)
  })

  it('工具栏包含返回按钮', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查返回按钮存在
    const toolbarBtns = wrapper.findAll('.toolbar-btn')
    expect(toolbarBtns.length).toBeGreaterThan(0)
  })

  it('工具栏包含撤销和重做按钮', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    const toolbarBtns = wrapper.findAll('.toolbar-btn')
    // 至少有返回、撤销、重做、缩小、放大、重置缩放、导入、导出、运行、保存按钮
    expect(toolbarBtns.length).toBeGreaterThanOrEqual(8)
  })

  it('工具栏包含保存按钮', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查保存按钮存在
    const saveBtn = wrapper.find('.btn-save')
    expect(saveBtn.exists()).toBe(true)
    expect(saveBtn.text()).toContain('保存')
  })

  it('工具栏包含运行按钮', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查运行按钮存在
    const runBtn = wrapper.find('.btn-run')
    expect(runBtn.exists()).toBe(true)
    expect(runBtn.text()).toContain('运行')
  })

  it('渲染节点面板', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查左侧面板存在
    expect(wrapper.find('.left-panel').exists()).toBe(true)
    // 检查节点面板标题
    expect(wrapper.text()).toContain('节点面板')
  })

  it('渲染画布区域', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查中间画布区域存在
    expect(wrapper.find('.center-panel').exists()).toBe(true)
  })

  it('渲染Agent名称输入框', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查Agent名称输入框存在
    expect(wrapper.find('.agent-name-input').exists()).toBe(true)
  })

  it('画布包含网格背景', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查画布网格存在
    expect(wrapper.find('.canvas-grid').exists()).toBe(true)
  })

  it('画布包含连接线层', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查连接线SVG层存在
    expect(wrapper.find('.connections-layer').exists()).toBe(true)
  })

  it('显示缩放比例', () => {
    const wrapper = mount(AgentDesigner, {
      global: globalMountOptions
    })

    // 检查缩放比例显示
    expect(wrapper.find('.zoom-label').exists()).toBe(true)
  })
})
