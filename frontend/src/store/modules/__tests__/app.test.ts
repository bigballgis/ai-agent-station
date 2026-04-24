import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/store/modules/app'

/**
 * App Store 单元测试
 * 测试: locale, sidebarCollapsed, setLocale, toggleSidebar
 */

// Mock @/locales
vi.mock('@/locales', () => ({
  defaultLocale: 'zh-CN',
}))

describe('App Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 初始状态 ----------

  describe('初始状态', () => {
    it('locale 默认为 zh-CN', () => {
      const store = useAppStore()
      expect(store.locale).toBe('zh-CN')
    })

    it('sidebarCollapsed 默认为 false', () => {
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)
    })

    it('从 localStorage 恢复 locale', () => {
      localStorage.setItem('locale', 'en-US')
      const store = useAppStore()
      expect(store.locale).toBe('en-US')
    })
  })

  // ---------- setLocale ----------

  describe('setLocale', () => {
    it('设置 locale 并持久化到 localStorage', () => {
      const store = useAppStore()
      store.setLocale('en-US')

      expect(store.locale).toBe('en-US')
      expect(localStorage.getItem('locale')).toBe('en-US')
    })

    it('切换回 zh-CN', () => {
      const store = useAppStore()
      store.setLocale('en-US')
      store.setLocale('zh-CN')

      expect(store.locale).toBe('zh-CN')
      expect(localStorage.getItem('locale')).toBe('zh-CN')
    })
  })

  // ---------- toggleSidebar ----------

  describe('toggleSidebar', () => {
    it('切换 sidebar 状态', () => {
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)

      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(true)

      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(false)
    })

    it('多次切换', () => {
      const store = useAppStore()
      store.toggleSidebar()
      store.toggleSidebar()
      store.toggleSidebar()

      expect(store.sidebarCollapsed).toBe(true)
    })
  })
})
