import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { messages } from '@/locales'
import MainLayout from '@/layouts/MainLayout.vue'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
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
        component: () => import('@/pages/Dashboard.vue'),
        meta: { title: 'dashboard' }
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/pages/AgentList.vue'),
        meta: { title: 'agentList' }
      },
      {
        path: 'agents/:id/edit',
        name: 'AgentEdit',
        component: () => import('@/pages/AgentEdit.vue'),
        meta: { title: 'agentEdit' }
      },
      {
        path: 'agents/:id/versions',
        name: 'AgentVersions',
        component: () => import('@/pages/AgentVersions.vue'),
        meta: { title: 'agentVersions' }
      },
      {
        path: 'agents/design/:id?',
        name: 'AgentDesign',
        component: () => import('@/pages/AgentDesigner.vue'),
        meta: { title: 'agentDesign' }
      },
      {
        path: 'mcp/tools',
        name: 'McpToolMarket',
        component: () => import('@/pages/McpToolMarket.vue'),
        meta: { title: 'mcpToolMarket' }
      },
      {
        path: 'agents/approval',
        name: 'Approval',
        component: () => import('@/pages/ApprovalManagement.vue'),
        meta: { title: 'approval' }
      },
      {
        path: 'agents/templates',
        name: 'AgentTemplateMarket',
        component: () => import('@/pages/AgentTemplateMarket.vue'),
        meta: { title: 'agentTemplateMarket' }
      },
      {
        path: 'agents/debugger',
        name: 'AgentDebugger',
        component: () => import('@/pages/AgentDebugger.vue'),
        meta: { title: 'agentDebugger' }
      },
      {
        path: 'agents/memory',
        name: 'MemoryManagement',
        component: () => import('@/pages/MemoryManagement.vue'),
        meta: { title: 'memoryManagement' }
      },
      {
        path: 'agents/deployment',
        name: 'Deployment',
        component: () => import('@/pages/DeploymentManagement.vue'),
        meta: { title: 'deployment' }
      },
      {
        path: 'api/manage',
        name: 'ApiManagement',
        component: () => import('@/pages/ApiManagement.vue'),
        meta: { title: 'apiManagement' }
      },
      {
        path: 'api/docs',
        name: 'ApiDocumentation',
        component: () => import('@/pages/ApiDocumentation.vue'),
        meta: { title: 'apiDocs' }
      },
      {
        path: 'tenant',
        name: 'TenantManagement',
        component: () => import('@/pages/TenantManagement.vue'),
        meta: { title: 'tenantManagement', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/permission',
        name: 'Permission',
        component: () => import('@/pages/PermissionManagement.vue'),
        meta: { title: 'permission', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/i18n',
        name: 'I18n',
        component: () => import('@/pages/I18nSettings.vue'),
        meta: { title: 'i18nSettings' }
      },
      {
        path: 'system/log',
        name: 'Log',
        component: () => import('@/pages/LogCenter.vue'),
        meta: { title: 'logCenter', roles: ['ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'system/alerts',
        name: 'AlertNotification',
        component: () => import('@/pages/AlertNotification.vue'),
        meta: { title: 'alertCenter' }
      },
      {
        path: 'system/quota',
        name: 'QuotaManagement',
        component: () => import('@/pages/QuotaManagement.vue'),
        meta: { title: 'quotaManagement' }
      },
      {
        path: 'system/files',
        name: 'FileManagement',
        component: () => import('@/pages/FileManagement.vue'),
        meta: { title: 'fileManagement' }
      },
      // 测试相关路由
      {
        path: 'test-cases',
        name: 'TestCaseList',
        component: () => import('@/pages/TestCaseList.vue'),
        meta: { title: 'testCaseList' }
      },
      {
        path: 'test-cases/edit/:id?',
        name: 'TestCaseEdit',
        component: () => import('@/pages/TestCaseEdit.vue'),
        meta: { title: 'testCaseEdit' }
      },
      {
        path: 'test-cases/versions/:id',
        name: 'TestCaseVersions',
        component: () => import('@/pages/TestCaseVersions.vue'),
        meta: { title: 'testCaseVersions' }
      },
      {
        path: 'test-executions',
        name: 'TestExecutionList',
        component: () => import('@/pages/TestExecutionList.vue'),
        meta: { title: 'testExecutionList' }
      },
      {
        path: 'test-results',
        name: 'TestResultList',
        component: () => import('@/pages/TestResultList.vue'),
        meta: { title: 'testResultList' }
      },
      {
        path: 'test-results/detail/:id',
        name: 'TestResultDetail',
        component: () => import('@/pages/TestResultDetail.vue'),
        meta: { title: 'testResultDetail' }
      },
      {
        path: 'test-results/:executionId',
        name: 'TestResultsByExecution',
        component: () => import('@/pages/TestResultList.vue'),
        meta: { title: 'testResultsByExecution' }
      },
      {
        path: 'workflow/designer',
        name: 'WorkflowDesigner',
        component: () => import('@/pages/WorkflowDesigner.vue'),
        meta: { title: 'workflowDesigner' }
      },
      {
        path: 'workflow/instances',
        name: 'WorkflowInstance',
        component: () => import('@/pages/WorkflowInstance.vue'),
        meta: { title: 'workflowInstance' }
      },
      {
        path: 'suggestions',
        name: 'SuggestionList',
        component: () => import('@/pages/SuggestionList.vue'),
        meta: { title: 'suggestionList' }
      },
      {
        path: 'evolution',
        name: 'Evolution',
        component: () => import('@/pages/Evolution.vue'),
        meta: { title: 'evolution' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFound.vue'),
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

// 路由守卫
router.beforeEach(async (to, _from, next) => {
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
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 检查token是否过期
    const token = userStore.token
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        if (payload.exp && payload.exp * 1000 < Date.now()) {
          userStore.logout()
          return next({ path: '/login', query: { redirect: to.fullPath } })
        }
      } catch {
        // token格式无效
        userStore.logout()
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    // 检查路由角色权限
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
      const userRoles = userStore.userInfo?.roles || []
      const hasRole = userRoles.some((role: string) => requiredRoles.includes(role))
      if (!hasRole) {
        // 使用 query 参数传递提示信息，避免直接操作 UI
        return next({ path: '/dashboard', query: { noPermission: '1' } })
      }
    }
  } else {
    // 不需要认证的页面
    if (to.path === '/login' && userStore.isLoggedIn) {
      // 已登录但访问登录页，重定向到首页
      next('/dashboard')
      return
    }
  }
  
  next()
})

export default router
