import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import i18n from './locales'
import { setupErrorHandler } from './utils/errorHandler'
import { useWebVitals } from './composables/useWebVitals'
import { setupDirectives } from './directives'
import './style.css'
import './assets/styles/theme.css'
import './styles/common.css'

const app = createApp(App)

// 创建 Pinia 实例
const pinia = createPinia()

// 注册全局错误处理
setupErrorHandler(app)

// 使用插件
app.use(pinia)
app.use(router)
app.use(i18n)

// 注册全局指令（v-permission, v-role）
setupDirectives(app)

// 初始化 Web Vitals 性能监控（非阻塞上报，不影响业务逻辑）
useWebVitals({
  reportToBackend: true,
})

// 路由切换时取消未完成的请求
router.afterEach(() => {
  // 动态导入避免循环依赖
  import('./utils/requestManager').then(({ cancelAllPendingRequests }) => {
    cancelAllPendingRequests()
  })
})

// 挂载应用
app.mount('#app')
