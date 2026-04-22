import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '@/components/StatusBadge.vue'

/**
 * StatusBadge 组件测试
 * 测试状态标签的颜色映射和标签显示
 */
describe('StatusBadge 组件', () => {
  // Agent 状态测试
  it('渲染 DRAFT 状态 - 显示草稿标签和 neutral 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'DRAFT', type: 'agent' }
    })
    expect(wrapper.text()).toContain('草稿')
    expect(wrapper.classes()).toContain('badge-neutral')
  })

  it('渲染 PENDING_APPROVAL 状态 - 显示待审批标签和 warning 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'PENDING_APPROVAL', type: 'agent' }
    })
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.classes()).toContain('badge-warning')
  })

  it('渲染 APPROVED 状态 - 显示已审批标签和 success 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'APPROVED', type: 'agent' }
    })
    expect(wrapper.text()).toContain('已审批')
    expect(wrapper.classes()).toContain('badge-success')
  })

  it('渲染 PUBLISHED 状态 - 显示已发布标签和 primary 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'PUBLISHED', type: 'agent' }
    })
    expect(wrapper.text()).toContain('已发布')
    expect(wrapper.classes()).toContain('badge-primary')
  })

  it('渲染 ERROR 状态 - 显示异常标签和 danger 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'ERROR', type: 'agent' }
    })
    expect(wrapper.text()).toContain('异常')
    expect(wrapper.classes()).toContain('badge-danger')
  })

  // 测试状态测试
  it('渲染 PASSED 测试状态 - 显示通过标签和 success 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'PASSED', type: 'test' }
    })
    expect(wrapper.text()).toContain('通过')
    expect(wrapper.classes()).toContain('badge-success')
  })

  it('渲染 FAILED 测试状态 - 显示失败标签和 danger 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'FAILED', type: 'test' }
    })
    expect(wrapper.text()).toContain('失败')
    expect(wrapper.classes()).toContain('badge-danger')
  })

  // 审批状态测试
  it('渲染 PENDING 审批状态 - 显示待审批标签和 warning 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'PENDING', type: 'approval' }
    })
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.classes()).toContain('badge-warning')
  })

  it('渲染 REJECTED 审批状态 - 显示已拒绝标签和 danger 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'REJECTED', type: 'approval' }
    })
    expect(wrapper.text()).toContain('已拒绝')
    expect(wrapper.classes()).toContain('badge-danger')
  })

  // 用户状态测试
  it('渲染 ACTIVE 用户状态 - 显示活跃标签和 success 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'ACTIVE', type: 'user' }
    })
    expect(wrapper.text()).toContain('活跃')
    expect(wrapper.classes()).toContain('badge-success')
  })

  it('渲染 DISABLED 用户状态 - 显示已禁用标签和 danger 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'DISABLED', type: 'user' }
    })
    expect(wrapper.text()).toContain('已禁用')
    expect(wrapper.classes()).toContain('badge-danger')
  })

  // 自定义标签测试
  it('使用自定义 label 覆盖默认标签', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'DRAFT', type: 'agent', label: '自定义标签' }
    })
    expect(wrapper.text()).toContain('自定义标签')
    expect(wrapper.text()).not.toContain('草稿')
  })

  // 未知状态测试
  it('渲染未知状态 - 显示原始值和 neutral 颜色', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'UNKNOWN_STATUS', type: 'agent' }
    })
    expect(wrapper.text()).toContain('UNKNOWN_STATUS')
    expect(wrapper.classes()).toContain('badge-neutral')
  })

  // 状态点测试
  it('显示状态点 - dot 属性为 true 时渲染状态点', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'RUNNING', type: 'agent', dot: true }
    })
    const dot = wrapper.find('.status-dot')
    expect(dot.exists()).toBe(true)
    expect(dot.classes()).toContain('dot-success')
  })

  it('不显示状态点 - dot 属性为 false 时不渲染状态点', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'RUNNING', type: 'agent', dot: false }
    })
    const dot = wrapper.find('.status-dot')
    expect(dot.exists()).toBe(false)
  })
})
