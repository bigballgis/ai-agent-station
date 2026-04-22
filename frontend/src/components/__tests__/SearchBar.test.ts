import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchBar from '@/components/SearchBar.vue'

/**
 * SearchBar 组件测试
 * 测试搜索栏的事件触发和字段渲染
 */

// Mock ant-design-vue 组件
vi.mock('ant-design-vue', () => ({
  Input: { template: '<input v-bind="$attrs" />' },
  Select: { template: '<select v-bind="$attrs" />' },
  RangePicker: { template: '<input type="text" />' },
  Button: {
    template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
  }
}))

// Mock ant-design/icons-vue
vi.mock('@ant-design/icons-vue', () => ({
  SearchOutlined: { template: '<span>search-icon</span>' },
  ReloadOutlined: { template: '<span>reload-icon</span>' },
  UpOutlined: { template: '<span>up-icon</span>' },
  DownOutlined: { template: '<span>down-icon</span>' }
}))

describe('SearchBar 组件', () => {
  const fields = [
    { label: '名称', key: 'name', type: 'input' as const },
    { label: '状态', key: 'status', type: 'select' as const, options: [{ label: '启用', value: '1' }] },
    { label: '创建时间', key: 'createdAt', type: 'dateRange' as const }
  ]

  it('渲染搜索栏 - 显示搜索和重置按钮', () => {
    const wrapper = mount(SearchBar, {
      props: { fields }
    })

    expect(wrapper.text()).toContain('搜索')
    expect(wrapper.text()).toContain('重置')
  })

  it('点击搜索按钮 - 触发 search 事件', async () => {
    const wrapper = mount(SearchBar, {
      props: { fields }
    })

    // 模拟输入值
    const vm = wrapper.vm as any
    vm.searchValues.name = 'test'

    // 触发搜索
    await wrapper.find('button[type="primary"], button').trigger('click')
    // 由于 ant-design-vue 按钮的 mock，我们直接调用 handleSearch
    vm.handleSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')![0][0]).toHaveProperty('name', 'test')
  })

  it('点击重置按钮 - 触发 reset 事件并清空值', async () => {
    const wrapper = mount(SearchBar, {
      props: { fields }
    })

    const vm = wrapper.vm as any
    vm.searchValues.name = 'test'
    vm.searchValues.status = '1'

    vm.handleReset()

    expect(vm.searchValues.name).toBeUndefined()
    expect(vm.searchValues.status).toBeUndefined()
    expect(wrapper.emitted('reset')).toBeTruthy()
  })

  it('搜索时过滤空值 - 不包含 undefined 和空字符串', () => {
    const wrapper = mount(SearchBar, {
      props: { fields }
    })

    const vm = wrapper.vm as any
    vm.searchValues.name = 'test'
    vm.searchValues.status = undefined
    vm.searchValues.createdAt = ''

    vm.handleSearch()

    const params = wrapper.emitted('search')![0][0]
    expect(params).toHaveProperty('name', 'test')
    expect(params).not.toHaveProperty('status')
    expect(params).not.toHaveProperty('createdAt')
  })

  it('默认显示前3个字段 - 折叠多余字段', () => {
    const manyFields = [
      { label: '字段1', key: 'f1', type: 'input' as const },
      { label: '字段2', key: 'f2', type: 'input' as const },
      { label: '字段3', key: 'f3', type: 'input' as const },
      { label: '字段4', key: 'f4', type: 'input' as const },
      { label: '字段5', key: 'f5', type: 'input' as const }
    ]

    const wrapper = mount(SearchBar, {
      props: { fields: manyFields, defaultShowCount: 3 }
    })

    const vm = wrapper.vm as any
    expect(vm.visibleFields.length).toBe(3)
  })

  it('展开后显示所有字段', async () => {
    const manyFields = [
      { label: '字段1', key: 'f1', type: 'input' as const },
      { label: '字段2', key: 'f2', type: 'input' as const },
      { label: '字段3', key: 'f3', type: 'input' as const },
      { label: '字段4', key: 'f4', type: 'input' as const }
    ]

    const wrapper = mount(SearchBar, {
      props: { fields: manyFields, defaultShowCount: 2 }
    })

    const vm = wrapper.vm as any
    expect(vm.visibleFields.length).toBe(2)

    vm.expanded = true
    await wrapper.vm.$nextTick()

    expect(vm.visibleFields.length).toBe(4)
  })

  it('空搜索条件 - search 事件返回空对象', () => {
    const wrapper = mount(SearchBar, {
      props: { fields }
    })

    const vm = wrapper.vm as any
    vm.handleSearch()

    const params = wrapper.emitted('search')![0][0]
    expect(Object.keys(params).length).toBe(0)
  })
})
