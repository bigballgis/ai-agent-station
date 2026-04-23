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
  messages,
  // 缺失 key 时使用 fallbackLocale 的翻译，而不是显示 key 本身
  silentFallbackWarn: true,
  silentTranslationWarn: true,
  missing(locale, key) {
    // 生产环境不打印 missing key 警告
    if (import.meta.env.PROD) return
    console.warn(`[i18n] Missing translation: locale="${locale}", key="${key}"`)
  }
})

export default i18n
