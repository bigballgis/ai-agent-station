import { defineStore } from 'pinia'
import { ref } from 'vue'
import { defaultLocale } from '@/locales'
import type { LocaleType } from '@/locales'
import { requireStoreReady } from '../utils'

export const useAppStore = defineStore('app', () => {
  requireStoreReady('app')

  // State
  const locale = ref<LocaleType>((localStorage.getItem('locale') as LocaleType) || defaultLocale)
  const sidebarCollapsed = ref(false)

  // Actions
  function setLocale(newLocale: LocaleType): void {
    locale.value = newLocale
    localStorage.setItem('locale', newLocale)
  }

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function $reset(): void {
    locale.value = defaultLocale
    sidebarCollapsed.value = false
    localStorage.removeItem('locale')
  }

  return {
    // State
    locale,
    sidebarCollapsed,
    // Actions
    setLocale,
    toggleSidebar,
    $reset,
  }
})
