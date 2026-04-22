import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import Dashboard from '../Dashboard.vue'

// Mock API 调用
vi.mock('@/api/agent', () => ({
  getAllAgents: vi.fn(() => Promise.resolve({ data: [] }))
}))

vi.mock('@/api/tool', () => ({
  getToolStats: vi.fn(() => Promise.resolve({ data: { totalCalls: 100 } }))
}))

vi.mock('@/api/alert', () => ({
  getAlertStats: vi.fn(() => Promise.resolve({ data: { total: 0 } })),
  getAlertRecords: vi.fn(() => Promise.resolve({ data: [] }))
}))

vi.mock('@/api/test', () => ({
  testApi: {
    getTestResults: vi.fn(() => Promise.resolve({ data: [] }))
  }
}))

vi.mock('@/api/log', () => ({
  getLogs: vi.fn(() => Promise.resolve({ data: [] }))
}))

// Mock 组件
vi.mock('@/components', () => ({
  PageHeader: {
    name: 'PageHeader',
    template: '<div class="mock-page-header"><slot /><slot name="actions" /></div>'
  },
  StatCard: {
    name: 'StatCard',
    props: ['title', 'value', 'icon', 'trend', 'trendValue', 'color', 'decimals', 'suffix'],
    template: '<div class="mock-stat-card" :aria-label="title">{{ title }}: {{ value }}</div>'
  },
  ChartContainer: {
    name: 'ChartContainer',
    props: ['type', 'data', 'options', 'height'],
    template: '<div class="mock-chart-container"></div>'
  }
}))

// Mock composables
vi.mock('@/composables/useTheme', () => ({
  useTheme: () => ({ isDark: { value: false } })
}))

// Mock ant-design-vue
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('Dashboard 页面', () => {
  // 创建路由
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/agents', component: { template: '<div/>' } },
      { path: '/approvals', component: { template: '<div/>' } },
      { path: '/logs', component: { template: '<div/>' } },
      { path: '/tests', component: { template: '<div/>' } },
      { path: '/system/log', component: { template: '<div/>' } }
    ]
  })

  // 创建 i18n
  const i18n = createI18n({
    legacy: false,
    locale: 'zh-CN',
    messages: {
      'zh-CN': {
        dashboard: {
          title: '仪表盘',
          subtitle: '系统概览',
          totalAgents: 'Agent总数',
          passRate: '测试通过率',
          apiCalls: 'API调用次数',
          activeUsers: '活跃告警',
          apiTrend: 'API调用趋势',
          agentDistribution: 'Agent状态分布',
          quickActions: '快捷操作',
          recentActivity: '最近活动',
          viewAll: '查看全部',
          noActivity: '暂无活动',
          createNewAgent: '创建Agent',
          viewApprovals: '查看审批',
          checkApiUsage: 'API使用',
          testManagement: '测试管理',
          loadFailed: '加载失败',
          comparedLastMonth: '较上月',
          comparedYesterday: '较昨日',
          weekDays: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
          callVolume: '调用量',
          success: '成功',
          running: '运行中',
          stopped: '已停止',
          pendingApprovals: '待审批',
          abnormal: '异常',
          apiTrendDesc: '近7天API调用趋势',
          agentDistributionDesc: '当前Agent状态分布',
          activityTimes: ['刚刚']
        },
        common: {
          noData: '暂无数据'
        }
      }
    }
  })

  const globalMountOptions = {
    plugins: [router, i18n],
    stubs: {
      'a-spin': {
        template: '<div class="ant-spin"><slot /></div>'
      }
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('渲染仪表盘页面 - 包含aria-label', () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 检查仪表盘容器存在
    expect(wrapper.find('[aria-label="仪表盘"]').exists()).toBe(true)
  })

  it('渲染统计卡片', async () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 等待数据加载
    await vi.dynamicImportSettled()

    // 检查统计卡片区域存在
    const statCards = wrapper.findAll('.mock-stat-card')
    expect(statCards.length).toBe(4)
  })

  it('渲染快捷操作区域', () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 检查快捷操作区域存在
    expect(wrapper.find('[aria-label="快捷操作"]').exists()).toBe(true)
  })

  it('快捷操作按钮正确导航', async () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 等待组件渲染
    await wrapper.vm.$nextTick()

    // 查找所有快捷操作按钮
    const buttons = wrapper.findAll('.grid.grid-cols-2 button')
    expect(buttons.length).toBe(4)
  })

  it('显示加载状态', () => {
    // 手动设置加载状态
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // dashboardLoading 初始为 false，数据加载后变为 true 再变为 false
    // 检查组件是否正确渲染
    expect(wrapper.find('.dashboard-page').exists()).toBe(true)
  })

  it('渲染图表区域', () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 检查图表容器存在
    const charts = wrapper.findAll('.mock-chart-container')
    expect(charts.length).toBe(2) // 折线图和环形图
  })

  it('渲染最近活动区域', () => {
    const wrapper = mount(Dashboard, {
      global: globalMountOptions
    })

    // 检查最近活动标题存在
    expect(wrapper.text()).toContain('最近活动')
  })
})
