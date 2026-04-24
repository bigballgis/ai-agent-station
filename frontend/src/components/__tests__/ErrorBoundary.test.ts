import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

/**
 * ErrorBoundary 组件测试
 * 测试错误边界捕获、错误 UI 渲染、重试功能及自定义错误消息
 *
 * 策略：由于 onErrorCaptured 在 jsdom 测试环境中对 slot 内容抛出的错误
 * 捕获行为不稳定，我们采用两种测试方式：
 * 1. 正常状态下的渲染测试（直接 mount）
 * 2. 通过模拟内部 error 状态来测试错误 UI（使用 setData 或直接操作 vm）
 */

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        'common.renderError': '渲染错误',
        'common.retry': '重试',
      }
      return map[key] || key
    },
  }),
}))

// 正常渲染的子组件
const NormalChild = defineComponent({
  name: 'NormalChild',
  render() {
    return h('div', '正常内容')
  },
})

describe('ErrorBoundary 组件', () => {
  it('正常渲染 slot 内容 - 无错误时显示子组件', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    expect(wrapper.text()).toContain('正常内容')
    expect(wrapper.find('.error-boundary').exists()).toBe(false)
  })

  it('无错误时不显示错误 UI', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    expect(wrapper.find('.error-boundary').exists()).toBe(false)
    expect(wrapper.find('h3').exists()).toBe(false)
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('无错误时 slot 内容可见', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h('div', { class: 'child-content' }, '子组件内容'),
      },
    })

    expect(wrapper.find('.child-content').exists()).toBe(true)
    expect(wrapper.find('.child-content').text()).toBe('子组件内容')
  })

  it('模拟错误状态 - 显示错误 UI', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    // 通过 vm 直接设置内部 error 状态来模拟错误捕获
    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('模拟错误消息')
    await nextTick()

    // 错误 UI 应该显示
    expect(wrapper.find('.error-boundary').exists()).toBe(true)
    expect(wrapper.text()).toContain('渲染错误')
    expect(wrapper.text()).toContain('模拟错误消息')
    // slot 内容应该被隐藏
    expect(wrapper.text()).not.toContain('正常内容')
  })

  it('模拟错误状态 - 显示重试按钮', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('test')
    await nextTick()

    const button = wrapper.find('button')
    expect(button.exists()).toBe(true)
    expect(button.text()).toContain('重试')
  })

  it('点击重试按钮重置错误状态', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('test')
    await nextTick()

    // 确认错误状态
    expect(wrapper.find('.error-boundary').exists()).toBe(true)

    // 点击重试按钮
    await wrapper.find('button').trigger('click')
    await nextTick()

    // 重试后 error 应该被重置为 null
    expect(vm.error).toBe(null)
    // slot 内容应该重新显示
    expect(wrapper.text()).toContain('正常内容')
    // 错误 UI 应该消失
    expect(wrapper.find('.error-boundary').exists()).toBe(false)
  })

  it('错误 UI 包含正确的 CSS 类', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('test')
    await nextTick()

    const errorDiv = wrapper.find('.error-boundary')
    expect(errorDiv.exists()).toBe(true)
    expect(errorDiv.classes()).toContain('p-6')
    expect(errorDiv.classes()).toContain('text-center')
  })

  it('错误 UI 中的标题使用红色样式', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('test')
    await nextTick()

    const heading = wrapper.find('h3')
    expect(heading.exists()).toBe(true)
    expect(heading.classes()).toContain('text-red-500')
    expect(heading.classes()).toContain('font-semibold')
  })

  it('重试按钮包含正确的样式类', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('test')
    await nextTick()

    const button = wrapper.find('button')
    expect(button.classes()).toContain('px-4')
    expect(button.classes()).toContain('py-2')
    expect(button.classes()).toContain('bg-blue-500')
    expect(button.classes()).toContain('text-white')
    expect(button.classes()).toContain('rounded')
  })

  it('错误消息文本正确显示', async () => {
    const wrapper = mount(ErrorBoundary, {
      slots: {
        default: () => h(NormalChild),
      },
    })

    const vm = wrapper.vm as { error: Error | null }
    vm.error = new Error('自定义错误消息 12345')
    await nextTick()

    expect(wrapper.text()).toContain('自定义错误消息 12345')
  })
})
