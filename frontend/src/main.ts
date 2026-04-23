import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { createPinia } from 'pinia'
import i18n from './locales'
import { setupErrorHandler } from './utils/errorHandler'
import './style.css'
import './styles/common.css'

const app = createApp(App)

// 创建 Pinia 实例
const pinia = createPinia()

// 注册全局错误处理
setupErrorHandler(app)

// 使用插件
app.use(pinia)
app.use(router)
app.use(Antd)
app.use(i18n)

// 路由切换时取消未完成的请求
router.afterEach(() => {
  // 动态导入避免循环依赖
  import('./utils/requestManager').then(({ cancelAllPendingRequests }) => {
    cancelAllPendingRequests()
  })
})

// 挂载应用
app.mount('#app')
