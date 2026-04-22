import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PageHeader from '@/components/PageHeader.vue'

/**
 * PageHeader 组件测试
 * 测试页面头部标题、副标题、面包屑渲染
 */

// Mock vue-router
const mockBack = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    back: mockBack
  })
}))

// Mock ant-design-vue 组件
vi.mock('ant-design-vue', () => ({
  Button: { template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>' },
  Breadcrumb: { template: '<nav><slot /></nav>' },
  BreadcrumbItem: { template: '<span><slot /></span>' }
}))

// Mock ant-design/icons-vue
vi.mock('@ant-design/icons-vue', () => ({
  ArrowLeftOutlined: { template: '<span>back-icon</span>' }
}))

describe('PageHeader 组件', () => {
  it('渲染标题', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Agent管理' }
    })

    expect(wrapper.text()).toContain('Agent管理')
    expect(wrapper.find('h1').exists()).toBe(true)
  })

  it('渲染副标题', () => {
    const wrapper = mount(PageHeader, {
      props: {
        title: 'Agent管理',
        subtitle: '管理和配置AI Agent'
      }
    })

    expect(wrapper.text()).toContain('管理和配置AI Agent')
  })

  it('不显示副标题 - subtitle 为空', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Agent管理' }
    })

    // 不应该有副标题元素
    const subtitle = wrapper.find('.page-subtitle')
    expect(subtitle.exists()).toBe(false)
  })

  it('渲染面包屑导航', () => {
    const breadcrumbs = [
      { title: '首页', path: '/dashboard' },
      { title: 'Agent管理', path: '/agents' },
      { title: '编辑Agent' }
    ]

    const wrapper = mount(PageHeader, {
      props: { title: '编辑Agent', breadcrumbs }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('Agent管理')
    expect(wrapper.text()).toContain('编辑Agent')
  })

  it('面包屑最后一项不渲染链接', () => {
    const breadcrumbs = [
      { title: '首页', path: '/' },
      { title: '当前页' }
    ]

    const wrapper = mount(PageHeader, {
      props: { title: '当前页', breadcrumbs }
    })

    // 最后一项应该有 breadcrumb-current 类
    const currentCrumb = wrapper.find('.breadcrumb-current')
    expect(currentCrumb.exists()).toBe(true)
    expect(currentCrumb.text()).toBe('当前页')
  })

  it('空面包屑 - 不渲染面包屑区域', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '测试页面', breadcrumbs: [] }
    })

    const breadcrumb = wrapper.find('nav')
    expect(breadcrumb.exists()).toBe(false)
  })

  it('不传面包屑 - 不渲染面包屑区域', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '测试页面' }
    })

    const breadcrumb = wrapper.find('nav')
    expect(breadcrumb.exists()).toBe(false)
  })

  it('显示返回按钮 - showBack 为 true', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '编辑页面', showBack: true }
    })

    const backBtn = wrapper.find('.back-btn')
    expect(backBtn.exists()).toBe(true)
  })

  it('不显示返回按钮 - showBack 为 false', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '列表页面', showBack: false }
    })

    const backBtn = wrapper.find('.back-btn')
    expect(backBtn.exists()).toBe(false)
  })

  it('渲染操作区域插槽', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Agent管理' },
      slots: {
        actions: '<button class="test-action">创建Agent</button>'
      }
    })

    expect(wrapper.find('.test-action').exists()).toBe(true)
    expect(wrapper.text()).toContain('创建Agent')
  })

  it('不传操作插槽 - 不渲染操作区域', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Agent管理' }
    })

    const actions = wrapper.find('.header-actions')
    expect(actions.exists()).toBe(false)
  })

  it('点击返回按钮 - 调用 router.back()', async () => {
    const wrapper = mount(PageHeader, {
      props: { title: '编辑页面', showBack: true }
    })

    const backBtn = wrapper.find('.back-btn')
    await backBtn.trigger('click')

    expect(mockBack).toHaveBeenCalled()
  })
})
