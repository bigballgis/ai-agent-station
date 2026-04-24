import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/components/EmptyState.vue'

/**
 * EmptyState 组件测试
 * 测试空状态组件的类型渲染、描述文本和操作按钮
 */

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'common.empty.noData': '暂无数据',
        'common.empty.noSearch': '未找到搜索结果',
        'common.empty.noPermission': '暂无权限',
        'common.empty.error': '出错了',
        'common.empty.network': '网络异常'
      }
      return map[key] || key
    }
  })
}))

// Mock ant-design-vue Button
vi.mock('ant-design-vue', () => ({
  Button: {
    template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>'
  }
}))

describe('EmptyState 组件', () => {
  it('默认渲染 noData 类型', () => {
    const wrapper = mount(EmptyState)

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.empty-state-noData').exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无数据')
  })

  it('noSearch 类型 - 显示搜索结果为空', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noSearch' }
    })

    expect(wrapper.find('.empty-state-noSearch').exists()).toBe(true)
    expect(wrapper.text()).toContain('未找到搜索结果')
  })

  it('noPermission 类型 - 显示无权限', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noPermission' }
    })

    expect(wrapper.find('.empty-state-noPermission').exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无权限')
  })

  it('error 类型 - 显示错误信息', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'error' }
    })

    expect(wrapper.find('.empty-state-error').exists()).toBe(true)
    expect(wrapper.text()).toContain('出错了')
  })

  it('network 类型 - 显示网络异常', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'network' }
    })

    expect(wrapper.find('.empty-state-network').exists()).toBe(true)
    expect(wrapper.text()).toContain('网络异常')
  })

  it('显示自定义描述', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'noData',
        description: '请先创建一个Agent'
      }
    })

    expect(wrapper.text()).toContain('请先创建一个Agent')
    expect(wrapper.find('.empty-description').exists()).toBe(true)
  })

  it('无描述时不渲染描述元素', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noData' }
    })

    expect(wrapper.find('.empty-description').exists()).toBe(false)
  })

  it('有 actionText 时渲染操作按钮', () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'noData',
        actionText: '创建Agent'
      }
    })

    expect(wrapper.find('.empty-action').exists()).toBe(true)
    expect(wrapper.text()).toContain('创建Agent')
  })

  it('点击操作按钮 - 触发 action 事件', async () => {
    const wrapper = mount(EmptyState, {
      props: {
        type: 'noData',
        actionText: '创建'
      }
    })

    await wrapper.find('.empty-action').trigger('click')

    expect(wrapper.emitted('action')).toBeTruthy()
    expect(wrapper.emitted('action')!.length).toBe(1)
  })

  it('无 actionText 时不渲染操作按钮', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noData' }
    })

    expect(wrapper.find('.empty-action').exists()).toBe(false)
  })

  it('渲染插画区域', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noData' }
    })

    expect(wrapper.find('.empty-illustration').exists()).toBe(true)
    expect(wrapper.find('svg').exists()).toBe(true)
  })

  it('不同类型渲染不同的 SVG 插画', () => {
    const types = ['noData', 'noSearch', 'noPermission', 'error', 'network'] as const

    types.forEach(type => {
      const wrapper = mount(EmptyState, {
        props: { type }
      })
      expect(wrapper.find('svg').exists()).toBe(true)
      wrapper.unmount()
    })
  })

  it('渲染标题元素', () => {
    const wrapper = mount(EmptyState, {
      props: { type: 'noData' }
    })

    expect(wrapper.find('.empty-title').exists()).toBe(true)
  })
})
