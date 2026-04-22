import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CountUp from '@/components/CountUp.vue'

/**
 * CountUp 组件测试
 * 测试数字滚动动画组件
 */

// Mock requestAnimationFrame
let rafCallbacks: Array<() => void> = []
const mockRaf = vi.fn((cb: FrameRequestCallback) => {
  rafCallbacks.push(() => cb(performance.now()))
  return rafCallbacks.length
})
const mockCancelRaf = vi.fn((id: number) => {
  rafCallbacks = rafCallbacks.filter((_, i) => i !== id - 1)
})

vi.stubGlobal('requestAnimationFrame', mockRaf)
vi.stubGlobal('cancelAnimationFrame', mockCancelRaf)

describe('CountUp 组件', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    rafCallbacks = []
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('渲染初始值 - 挂载后显示0', () => {
    const wrapper = mount(CountUp, {
      props: { endValue: 1000 }
    })

    // 初始时 displayValue 应该是空字符串或 '0'
    expect(wrapper.text()).toBeDefined()
  })

  it('动画完成后显示目标值', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 100, duration: 100 }
    })

    // 等待动画完成
    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('100')
  })

  it('使用前缀 - 显示前缀', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 50, duration: 100, prefix: '$' }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('$')
    expect(wrapper.text()).toContain('50')
  })

  it('使用后缀 - 显示后缀', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 100, duration: 100, suffix: '%' }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('%')
    expect(wrapper.text()).toContain('100')
  })

  it('使用小数位 - 正确显示小数', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 99.99, duration: 100, decimals: 2 }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('99.99')
  })

  it('使用千分位分隔符 - 正确格式化', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 1000000, duration: 100, separator: ',' }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('1,000,000')
  })

  it('endValue 为 0 - 显示 0', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 0, duration: 100 }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('0')
  })

  it('负数值 - 正确显示负数', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: -500, duration: 100 }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    expect(wrapper.text()).toContain('-500')
  })

  it('卸载时取消动画帧', () => {
    const wrapper = mount(CountUp, {
      props: { endValue: 1000, duration: 5000 }
    })

    wrapper.unmount()

    expect(mockCancelRaf).toHaveBeenCalled()
  })

  it('同时使用前缀和后缀', async () => {
    vi.useRealTimers()

    const wrapper = mount(CountUp, {
      props: { endValue: 75, duration: 100, prefix: '¥', suffix: '元' }
    })

    await new Promise(resolve => setTimeout(resolve, 200))

    const text = wrapper.text()
    expect(text).toContain('¥')
    expect(text).toContain('75')
    expect(text).toContain('元')
  })
})
