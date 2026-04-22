import { defineStore } from 'pinia'
import { ref } from 'vue'
import { defaultLocale } from '@/locales'
import type { LocaleType } from '@/locales'

export const useAppStore = defineStore('app', () => {
  const locale = ref<LocaleType>((localStorage.getItem('locale') as LocaleType) || defaultLocale)
  const sidebarCollapsed = ref(false)

  function setLocale(newLocale: LocaleType) {
    locale.value = newLocale
    localStorage.setItem('locale', newLocale)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    locale,
    sidebarCollapsed,
    setLocale,
    toggleSidebar
  }
})