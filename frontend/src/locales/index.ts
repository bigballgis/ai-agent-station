import zhCN from './zh-CN'
import enUS from './en-US'
import { createI18n } from 'vue-i18n'

export const messages = {
  'zh-CN': zhCN,
  'en-US': enUS
}

export type LocaleType = keyof typeof messages

export const defaultLocale: LocaleType = 'zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('locale') || defaultLocale,
  fallbackLocale: defaultLocale,
  messages
})

export default i18n
