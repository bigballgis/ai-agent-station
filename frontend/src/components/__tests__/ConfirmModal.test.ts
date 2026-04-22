import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfirmModal from '@/components/ConfirmModal.vue'

/**
 * ConfirmModal 组件测试
 * 测试确认弹窗的 ok/cancel 事件触发
 */

// Mock ant-design-vue Modal
vi.mock('ant-design-vue', () => ({
  Modal: {
    template: `
      <div v-if="open" class="ant-modal" data-testid="modal">
        <div class="ant-modal-title">{{ title }}</div>
        <div class="ant-modal-content">
          <slot />
        </div>
        <button class="ant-modal-ok" @click="$emit('ok')">确认</button>
        <button class="ant-modal-cancel" @click="$emit('cancel')">取消</button>
      </div>
    `
  }
}))

// Mock ant-design/icons-vue
vi.mock('@ant-design/icons-vue', () => ({
  ExclamationCircleOutlined: { template: '<span class="icon-exclamation">!</span>' },
  CloseCircleOutlined: { template: '<span class="icon-close">X</span>' },
  InfoCircleOutlined: { template: '<span class="icon-info">i</span>' }
}))

describe('ConfirmModal 组件', () => {
  it('visible 为 true 时渲染弹窗', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        title: '确认操作',
        content: '确定要执行此操作吗？'
      }
    })

    expect(wrapper.find('.ant-modal').exists()).toBe(true)
  })

  it('visible 为 false 时不渲染弹窗', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: false,
        title: '确认操作',
        content: '确定要执行此操作吗？'
      }
    })

    expect(wrapper.find('.ant-modal').exists()).toBe(false)
  })

  it('显示内容文本', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        content: '确定要删除此Agent吗？'
      }
    })

    expect(wrapper.text()).toContain('确定要删除此Agent吗？')
  })

  it('点击确认按钮 - 触发 ok 事件', async () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        content: '确认删除？'
      }
    })

    await wrapper.find('.ant-modal-ok').trigger('click')

    expect(wrapper.emitted('ok')).toBeTruthy()
    expect(wrapper.emitted('ok')!.length).toBe(1)
  })

  it('点击取消按钮 - 触发 cancel 事件和 update:visible', async () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        content: '确认操作？'
      }
    })

    await wrapper.find('.ant-modal-cancel').trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0][0]).toBe(false)
  })

  it('delete 类型 - 显示删除标题', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'delete'
      }
    })

    expect(wrapper.text()).toContain('确认删除')
  })

  it('warning 类型 - 显示警告标题', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'warning'
      }
    })

    expect(wrapper.text()).toContain('警告')
  })

  it('info 类型 - 显示提示标题', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'info'
      }
    })

    expect(wrapper.text()).toContain('提示')
  })

  it('自定义标题 - 覆盖默认标题', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        title: '自定义标题',
        type: 'delete'
      }
    })

    expect(wrapper.text()).toContain('自定义标题')
  })

  it('delete 类型 - 显示危险图标', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'delete'
      }
    })

    expect(wrapper.find('.icon-danger').exists()).toBe(true)
  })

  it('warning 类型 - 显示警告图标', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'warning'
      }
    })

    expect(wrapper.find('.icon-warning').exists()).toBe(true)
  })

  it('info 类型 - 显示信息图标', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        type: 'info'
      }
    })

    expect(wrapper.find('.icon-info').exists()).toBe(true)
  })

  it('渲染额外内容插槽', () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        content: '确认操作？'
      },
      slots: {
        extra: '<div class="extra-content">额外信息</div>'
      }
    })

    expect(wrapper.find('.extra-content').exists()).toBe(true)
    expect(wrapper.text()).toContain('额外信息')
  })

  it('confirmLoading 为 true 时不影响 ok 事件触发', async () => {
    const wrapper = mount(ConfirmModal, {
      props: {
        visible: true,
        confirmLoading: true
      }
    })

    await wrapper.find('.ant-modal-ok').trigger('click')

    expect(wrapper.emitted('ok')).toBeTruthy()
  })
})
