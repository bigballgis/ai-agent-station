import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import MainLayout from '@/layouts/MainLayout.vue'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { requiresAuth: false }
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
        meta: { title: 'Dashboard' }
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/pages/AgentList.vue'),
        meta: { title: 'Agent管理' }
      },
      {
        path: 'agents/:id/edit',
        name: 'AgentEdit',
        component: () => import('@/pages/AgentEdit.vue'),
        meta: { title: '编辑Agent' }
      },
      {
        path: 'agents/:id/versions',
        name: 'AgentVersions',
        component: () => import('@/pages/AgentVersions.vue'),
        meta: { title: '版本管理' }
      },
      {
        path: 'agent/design',
        name: 'AgentDesign',
        component: () => import('@/pages/Placeholder.vue'),
        meta: { title: 'Agent Design' }
      },
      {
        path: 'mcp/tools',
        name: 'McpToolMarket',
        component: () => import('@/pages/McpToolMarket.vue'),
        meta: { title: 'MCP 工具市场' }
      },
      {
        path: 'agent/approval',
        name: 'Approval',
        component: () => import('@/pages/ApprovalManagement.vue'),
        meta: { title: '审批管理' }
      },
      {
        path: 'agent/templates',
        name: 'AgentTemplateMarket',
        component: () => import('@/pages/AgentTemplateMarket.vue'),
        meta: { title: 'Agent 模板市场' }
      },
      {
        path: 'agent/debugger',
        name: 'AgentDebugger',
        component: () => import('@/pages/AgentDebugger.vue'),
        meta: { title: '在线调试' }
      },
      {
        path: 'agent/memory',
        name: 'MemoryManagement',
        component: () => import('@/pages/MemoryManagement.vue'),
        meta: { title: '记忆管理' }
      },
      {
        path: 'agent/deployment',
        name: 'Deployment',
        component: () => import('@/pages/DeploymentManagement.vue'),
        meta: { title: '发布管理' }
      },
      {
        path: 'api/manage',
        name: 'ApiManagement',
        component: () => import('@/pages/ApiManagement.vue'),
        meta: { title: 'API管理' }
      },
      {
        path: 'api/docs',
        name: 'ApiDocumentation',
        component: () => import('@/pages/ApiDocumentation.vue'),
        meta: { title: 'API文档' }
      },
      {
        path: 'tenant',
        name: 'TenantManagement',
        component: () => import('@/pages/TenantManagement.vue'),
        meta: { title: 'Tenant Management' }
      },
      {
        path: 'system/permission',
        name: 'Permission',
        component: () => import('@/pages/PermissionManagement.vue'),
        meta: { title: 'Permission Management' }
      },
      {
        path: 'system/i18n',
        name: 'I18n',
        component: () => import('@/pages/Placeholder.vue'),
        meta: { title: 'I18n Settings' }
      },
      {
        path: 'system/log',
        name: 'Log',
        component: () => import('@/pages/LogCenter.vue'),
        meta: { title: 'Log Monitoring' }
      },
      {
        path: 'system/alerts',
        name: 'AlertNotification',
        component: () => import('@/pages/AlertNotification.vue'),
        meta: { title: '告警中心' }
      },
      {
        path: 'system/quota',
        name: 'QuotaManagement',
        component: () => import('@/pages/QuotaManagement.vue'),
        meta: { title: '配额管理' }
      },
      {
        path: 'system/files',
        name: 'FileManagement',
        component: () => import('@/pages/FileManagement.vue'),
        meta: { title: '文件管理' }
      },
      // 测试相关路由
      {
        path: 'test-cases',
        name: 'TestCaseList',
        component: () => import('@/pages/TestCaseList.vue'),
        meta: { title: '测试用例管理' }
      },
      {
        path: 'test-cases/edit/:id?',
        name: 'TestCaseEdit',
        component: () => import('@/pages/TestCaseEdit.vue'),
        meta: { title: '编辑测试用例' }
      },
      {
        path: 'test-cases/versions/:id',
        name: 'TestCaseVersions',
        component: () => import('@/pages/TestCaseVersions.vue'),
        meta: { title: '测试用例版本管理' }
      },
      {
        path: 'test-executions',
        name: 'TestExecutionList',
        component: () => import('@/pages/TestExecutionList.vue'),
        meta: { title: '测试执行管理' }
      },
      {
        path: 'test-results',
        name: 'TestResultList',
        component: () => import('@/pages/TestResultList.vue'),
        meta: { title: '测试结果管理' }
      },
      {
        path: 'test-results/detail/:id',
        name: 'TestResultDetail',
        component: () => import('@/pages/TestResultDetail.vue'),
        meta: { title: '测试结果详情' }
      },
      {
        path: 'test-results/:executionId',
        name: 'TestResultsByExecution',
        component: () => import('@/pages/TestResultList.vue'),
        meta: { title: '测试执行结果' }
      },
      {
        path: 'workflow/designer',
        name: 'WorkflowDesigner',
        component: () => import('@/pages/WorkflowDesigner.vue'),
        meta: { title: '工作流设计器' }
      },
      {
        path: 'workflow/instances',
        name: 'WorkflowInstance',
        component: () => import('@/pages/WorkflowInstance.vue'),
        meta: { title: '工作流实例' }
      },
      {
        path: 'suggestions',
        name: 'SuggestionList',
        component: () => import('@/pages/SuggestionList.vue'),
        meta: { title: '优化建议管理' }
      },
      {
        path: 'evolution',
        name: 'Evolution',
        component: () => import('@/pages/Evolution.vue'),
        meta: { title: 'Agent自进化' }
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
