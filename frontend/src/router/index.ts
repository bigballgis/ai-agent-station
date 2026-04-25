import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { messages } from '@/locales'
import { setupRouteChangeCancellation } from '@/utils/request'
import { globalPerformanceTracker } from '@/composables/usePerformanceMark'
import {
  isTokenExpired,
  checkRoutePermission,
  logRouteChange,
  handleTokenExpired,
  handleNoPermission,
} from '@/router/guards'
// MainLayout 作为所有认证路由的布局包装器，保持静态导入
// 避免嵌套懒加载导致的布局闪烁问题
import MainLayout from '@/layouts/MainLayout.vue'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import(/* webpackChunkName: "login" */ '@/pages/Login.vue'),
    meta: { requiresAuth: false, title: 'login' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import(/* webpackChunkName: "dashboard" */ /* webpackPrefetch: true */ '@/pages/Dashboard.vue'),
        meta: { title: 'dashboard' }
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import(/* webpackChunkName: "agent-list" */ /* webpackPrefetch: true */ '@/pages/AgentList.vue'),
        meta: { title: 'agentList' }
      },
      {
        path: 'agents/:id/edit',
        name: 'AgentEdit',
        component: () => import(/* webpackChunkName: "agent-edit" */ '@/pages/AgentEdit.vue'),
        meta: { title: 'agentEdit' }
      },
      {
        path: 'agents/:id/versions',
        name: 'AgentVersions',
        component: () => import(/* webpackChunkName: "agent-versions" */ '@/pages/AgentVersions.vue'),
        meta: { title: 'agentVersions' }
      },
      {
        path: 'agents/design/:id?',
        name: 'AgentDesign',
        component: () => import(/* webpackChunkName: "agent-designer" */ '@/pages/AgentDesigner.vue'),
        meta: { title: 'agentDesign' }
      },
      {
        path: 'mcp/tools',
        name: 'McpToolMarket',
        component: () => import(/* webpackChunkName: "mcp-tools" */ '@/pages/McpToolMarket.vue'),
        meta: { title: 'mcpToolMarket' }
      },
      {
        path: 'agents/approval',
        name: 'Approval',
        component: () => import(/* webpackChunkName: "approval" */ '@/pages/ApprovalManagement.vue'),
        meta: { title: 'approval' }
      },
      {
        path: 'agents/templates',
        name: 'AgentTemplateMarket',
        component: () => import(/* webpackChunkName: "agent-templates" */ '@/pages/AgentTemplateMarket.vue'),
        meta: { title: 'agentTemplateMarket' }
      },
      {
        path: 'agents/debugger',
        name: 'AgentDebugger',
        component: () => import(/* webpackChunkName: "agent-debugger" */ '@/pages/AgentDebugger.vue'),
        meta: { title: 'agentDebugger' }
      },
      {
        path: 'agents/memory',
        name: 'MemoryManagement',
        component: () => import(/* webpackChunkName: "memory" */ '@/pages/MemoryManagement.vue'),
        meta: { title: 'memoryManagement' }
      },
      {
        path: 'agents/deployment',
        name: 'Deployment',
        component: () => import(/* webpackChunkName: "deployment" */ '@/pages/DeploymentManagement.vue'),
        meta: { title: 'deployment' }
      },
      {
        path: 'api/manage',
        name: 'ApiManagement',
        component: () => import(/* webpackChunkName: "api-manage" */ '@/pages/ApiManagement.vue'),
        meta: { title: 'apiManagement' }
      },
      {
        path: 'api/docs',
        name: 'ApiDocumentation',
        component: () => import(/* webpackChunkName: "api-docs" */ '@/pages/ApiDocumentation.vue'),
        meta: { title: 'apiDocs' }
      },
      {
        path: 'tenant',
        name: 'TenantManagement',
        component: () => import(/* webpackChunkName: "tenant" */ '@/pages/TenantManagement.vue'),
        meta: { title: 'tenantManagement', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/permission',
        name: 'Permission',
        component: () => import(/* webpackChunkName: "permission" */ '@/pages/PermissionManagement.vue'),
        meta: { title: 'permission', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/i18n',
        name: 'I18n',
        component: () => import(/* webpackChunkName: "i18n" */ '@/pages/I18nSettings.vue'),
        meta: { title: 'i18nSettings' }
      },
      {
        path: 'system/log',
        name: 'Log',
        component: () => import(/* webpackChunkName: "log" */ '@/pages/LogCenter.vue'),
        meta: { title: 'logCenter', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/alerts',
        name: 'AlertNotification',
        component: () => import(/* webpackChunkName: "alerts" */ '@/pages/AlertNotification.vue'),
        meta: { title: 'alertCenter' }
      },
      {
        path: 'system/quota',
        name: 'QuotaManagement',
        component: () => import(/* webpackChunkName: "quota" */ '@/pages/QuotaManagement.vue'),
        meta: { title: 'quotaManagement' }
      },
      {
        path: 'system/files',
        name: 'FileManagement',
        component: () => import(/* webpackChunkName: "files" */ '@/pages/FileManagement.vue'),
        meta: { title: 'fileManagement' }
      },
      // 测试相关路由
      {
        path: 'test-cases',
        name: 'TestCaseList',
        component: () => import(/* webpackChunkName: "test-cases" */ '@/pages/TestCaseList.vue'),
        meta: { title: 'testCaseList' }
      },
      {
        path: 'test-cases/edit/:id?',
        name: 'TestCaseEdit',
        component: () => import(/* webpackChunkName: "test-case-edit" */ '@/pages/TestCaseEdit.vue'),
        meta: { title: 'testCaseEdit' }
      },
      {
        path: 'test-cases/versions/:id',
        name: 'TestCaseVersions',
        component: () => import(/* webpackChunkName: "test-case-versions" */ '@/pages/TestCaseVersions.vue'),
        meta: { title: 'testCaseVersions' }
      },
      {
        path: 'test-executions',
        name: 'TestExecutionList',
        component: () => import(/* webpackChunkName: "test-executions" */ '@/pages/TestExecutionList.vue'),
        meta: { title: 'testExecutionList' }
      },
      {
        path: 'test-results',
        name: 'TestResultList',
        component: () => import(/* webpackChunkName: "test-results" */ '@/pages/TestResultList.vue'),
        meta: { title: 'testResultList' }
      },
      {
        path: 'test-results/detail/:id',
        name: 'TestResultDetail',
        component: () => import(/* webpackChunkName: "test-result-detail" */ '@/pages/TestResultDetail.vue'),
        meta: { title: 'testResultDetail' }
      },
      {
        path: 'test-results/:executionId',
        name: 'TestResultsByExecution',
        component: () => import(/* webpackChunkName: "test-results" */ '@/pages/TestResultList.vue'),
        meta: { title: 'testResultsByExecution' }
      },
      {
        path: 'workflow/designer',
        name: 'WorkflowDesigner',
        component: () => import(/* webpackChunkName: "workflow-designer" */ /* webpackPrefetch: true */ '@/pages/WorkflowDesigner.vue'),
        meta: { title: 'workflowDesigner' }
      },
      {
        path: 'workflow/instances',
        name: 'WorkflowInstance',
        component: () => import(/* webpackChunkName: "workflow-instance" */ '@/pages/WorkflowInstance.vue'),
        meta: { title: 'workflowInstance' }
      },
      {
        path: 'suggestions',
        name: 'SuggestionList',
        component: () => import(/* webpackChunkName: "suggestions" */ '@/pages/SuggestionList.vue'),
        meta: { title: 'suggestionList' }
      },
      {
        path: 'evolution',
        name: 'Evolution',
        component: () => import(/* webpackChunkName: "evolution" */ '@/pages/Evolution.vue'),
        meta: { title: 'evolution' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import(/* webpackChunkName: "not-found" */ '@/pages/NotFound.vue'),
    meta: { requiresAuth: false }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由切换时取消未完成的请求（防止数据覆盖）
setupRouteChangeCancellation(router)

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 性能追踪：记录路由切换开始时间
  globalPerformanceTracker.startMark(`route:${to.fullPath}`)

  // 路由变化日志（仅开发模式）
  logRouteChange(to, from)

  // 设置页面标题（使用 i18n 翻译）
  const titleKey = to.meta.title as string | undefined
  if (titleKey) {
    const currentLocale = localStorage.getItem('locale') || 'zh-CN'
    const localeMessages = messages[currentLocale as keyof typeof messages]
    const translatedTitle = (localeMessages as unknown as Record<string, Record<string, string>>)?.routes?.[titleKey] || titleKey
    document.title = `${translatedTitle} - AI Agent Station`
  }

  const userStore = useUserStore()

  // 检查路由是否需要认证
  if (to.meta.requiresAuth !== false) {
    if (!userStore.isLoggedIn) {
      // 未登录，跳转到登录页
      logRouteChange(to, from, 'redirect')
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 检查 token 是否过期（使用增强的守卫工具函数）
    const token = userStore.token
    if (token && isTokenExpired(token)) {
      logRouteChange(to, from, 'redirect')
      return next(handleTokenExpired(to))
    }

    // 检查路由权限（统一权限检查：roles + requiresPermission + requiresAllPermissions + requiresAllRoles）
    if (!checkRoutePermission(to)) {
      logRouteChange(to, from, 'redirect')
      return next(handleNoPermission(to))
    }
  } else {
    // 不需要认证的页面
    if (to.path === '/login' && userStore.isLoggedIn) {
      // 已登录但访问登录页，重定向到首页
      logRouteChange(to, from, 'redirect')
      next('/dashboard')
      return
    }
  }

  next()
})

// 预取策略：在空闲时预加载用户可能访问的下一路由
// 当用户停留在当前页面 2 秒后，预取相邻路由以提升导航体验
const prefetchMap: Record<string, string[]> = {
  '/dashboard': ['/agents'],
  '/agents': ['/dashboard', '/agents/design'],
  '/agents/design': ['/agents', '/agents/debugger'],
  '/agents/debugger': ['/agents', '/agents/design'],
}

let prefetchTimer: ReturnType<typeof setTimeout> | null = null

router.afterEach((to, from) => {
  // 性能追踪：记录路由切换结束时间
  globalPerformanceTracker.endMark(`route:${to.fullPath}`)

  // 路由变化日志（仅开发模式）
  logRouteChange(to, from)

  // 清除之前的预取定时器
  if (prefetchTimer) {
    clearTimeout(prefetchTimer)
    prefetchTimer = null
  }

  // 延迟 2 秒后在空闲时预取
  prefetchTimer = setTimeout(() => {
    const prefetchTargets = prefetchMap[to.path]
    if (prefetchTargets) {
      prefetchTargets.forEach((path) => {
        const matched = router.resolve(path)
        if (matched && matched.matched.length > 0) {
          // 使用 requestIdleCallback 在浏览器空闲时预取
          const prefetch = () => {
            matched.matched.forEach((record) => {
              if (record.components) {
                Object.values(record.components).forEach((component) => {
                  if (typeof component === 'function') {
                    (component as () => unknown)()
                  }
                })
              }
            })
          }
          if ('requestIdleCallback' in window) {
            requestIdleCallback(prefetch)
          }
        }
      })
    }
  }, 2000)
})

export default router
