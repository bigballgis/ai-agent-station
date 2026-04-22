import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/store/modules/app'

/**
 * App Store 测试
 * 测试应用全局状态管理（语言、侧边栏等）
 */

describe('App Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始化 - 从 localStorage 恢复语言设置', () => {
    localStorage.setItem('locale', 'en-US')

    const store = useAppStore()

    expect(store.locale).toBe('en-US')
  })

  it('初始化 - 默认语言为 zh-CN', () => {
    const store = useAppStore()

    expect(store.locale).toBe('zh-CN')
  })

  it('初始化 - 侧边栏默认不折叠', () => {
    const store = useAppStore()

    expect(store.sidebarCollapsed).toBe(false)
  })

  it('setLocale - 切换语言为 en-US', () => {
    const store = useAppStore()

    store.setLocale('en-US')

    expect(store.locale).toBe('en-US')
    expect(localStorage.getItem('locale')).toBe('en-US')
  })

  it('setLocale - 切换语言为 zh-CN', () => {
    localStorage.setItem('locale', 'en-US')

    const store = useAppStore()
    store.setLocale('zh-CN')

    expect(store.locale).toBe('zh-CN')
    expect(localStorage.getItem('locale')).toBe('zh-CN')
  })

  it('toggleSidebar - 切换侧边栏折叠状态', () => {
    const store = useAppStore()

    expect(store.sidebarCollapsed).toBe(false)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('toggleSidebar - 多次切换', () => {
    const store = useAppStore()

    store.toggleSidebar() // true
    store.toggleSidebar() // false
    store.toggleSidebar() // true

    expect(store.sidebarCollapsed).toBe(true)
  })
})
