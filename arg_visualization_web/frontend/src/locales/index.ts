import { createI18n } from 'vue-i18n'
import { ref, computed } from 'vue'
import zh from './zh'
import en from './en'

// Element Plus 语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import enUs from 'element-plus/dist/locale/en.mjs'

// Element Plus 语言映射
export const elementPlusLocales = {
  zh: zhCn,
  en: enUs
}

// 读取本地存储的语言设置
const getLocale = (): string => {
  const savedLocale = localStorage.getItem('locale')
  if (savedLocale && ['zh', 'en'].includes(savedLocale)) {
    return savedLocale
  }
  // 默认中文
  return 'zh'
}

// 当前语言（响应式）
export const currentLocale = ref(getLocale())

// Element Plus 当前语言配置
export const elementPlusLocale = computed(() => {
  return elementPlusLocales[currentLocale.value as keyof typeof elementPlusLocales]
})

// 切换语言函数
export const setLocale = (locale: string) => {
  if (['zh', 'en'].includes(locale)) {
    currentLocale.value = locale
    localStorage.setItem('locale', locale)
    // 更新 i18n 实例的语言
    i18n.global.locale.value = locale
  }
}

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getLocale(),
  fallbackLocale: 'zh',
  messages: {
    zh,
    en
  }
})

export default i18n
