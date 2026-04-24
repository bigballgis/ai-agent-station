import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '@/components/StatusBadge.vue'

/**
 * StatusBadge 组件测试
 * 测试各类型状态文本渲染、颜色映射、未知状态处理及 i18n 集成
 */

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const map: Record<string, string> = {
        // Agent 状态
        'status.draft': '草稿',
        'status.pendingApproval': '待审批',
        'status.approved': '已审批',
        'status.published': '已发布',
        'status.archived': '已归档',
        'status.running': '运行中',
        'status.stopped': '已停止',
        'status.error': '错误',
        // 测试状态
        'status.passed': '通过',
        'status.failed': '失败',
        'status.pending': '待处理',
        'status.skipped': '已跳过',
        // 审批状态
        'status.rejected': '已拒绝',
        'status.cancelled': '已取消',
        // 发布状态
        'status.deploying': '部署中',
        'status.deployed': '已部署',
        'status.deployFailed': '部署失败',
        'status.rollback': '回滚中',
        // 告警状态
        'status.firing': '告警中',
        'status.resolved': '已解决',
        'status.critical': '严重',
        'status.warning': '警告',
        'status.info': '信息',
        // 用户状态
        'status.active': '活跃',
        'status.inactive': '未激活',
        'status.disabled': '已禁用',
        'status.enabled': '已启用',
        'status.locked': '已锁定',
      }
      return map[key] || key
    },
  }),
}))

describe('StatusBadge 组件', () => {
  describe('Agent 状态类型', () => {
    it('渲染 DRAFT 状态 - 草稿', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'DRAFT', type: 'agent' },
      })
      expect(wrapper.text()).toContain('草稿')
      expect(wrapper.find('.badge-neutral').exists()).toBe(true)
    })

    it('渲染 PENDING_APPROVAL 状态 - 待审批', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'PENDING_APPROVAL', type: 'agent' },
      })
      expect(wrapper.text()).toContain('待审批')
      expect(wrapper.find('.badge-warning').exists()).toBe(true)
    })

    it('渲染 APPROVED 状态 - 已审批', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'APPROVED', type: 'agent' },
      })
      expect(wrapper.text()).toContain('已审批')
      expect(wrapper.find('.badge-success').exists()).toBe(true)
    })

    it('渲染 PUBLISHED 状态 - 已发布', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'PUBLISHED', type: 'agent' },
      })
      expect(wrapper.text()).toContain('已发布')
      expect(wrapper.find('.badge-primary').exists()).toBe(true)
    })

    it('渲染 ERROR 状态 - 错误', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'ERROR', type: 'agent' },
      })
      expect(wrapper.text()).toContain('错误')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })
  })

  describe('测试状态类型', () => {
    it('渲染 PASSED 状态 - 通过', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'PASSED', type: 'test' },
      })
      expect(wrapper.text()).toContain('通过')
      expect(wrapper.find('.badge-success').exists()).toBe(true)
    })

    it('渲染 FAILED 状态 - 失败', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'FAILED', type: 'test' },
      })
      expect(wrapper.text()).toContain('失败')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })

    it('渲染小写 passed 状态', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'passed', type: 'test' },
      })
      expect(wrapper.text()).toContain('通过')
      expect(wrapper.find('.badge-success').exists()).toBe(true)
    })
  })

  describe('审批状态类型', () => {
    it('渲染 REJECTED 状态 - 已拒绝', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'REJECTED', type: 'approval' },
      })
      expect(wrapper.text()).toContain('已拒绝')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })

    it('渲染 CANCELLED 状态 - 已取消', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'CANCELLED', type: 'approval' },
      })
      expect(wrapper.text()).toContain('已取消')
      expect(wrapper.find('.badge-neutral').exists()).toBe(true)
    })
  })

  describe('发布状态类型', () => {
    it('渲染 DEPLOYING 状态 - 部署中', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'DEPLOYING', type: 'deployment' },
      })
      expect(wrapper.text()).toContain('部署中')
      expect(wrapper.find('.badge-primary').exists()).toBe(true)
    })

    it('渲染 FAILED 状态 - 部署失败', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'FAILED', type: 'deployment' },
      })
      expect(wrapper.text()).toContain('部署失败')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })

    it('渲染 ROLLBACK 状态 - 回滚中', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'ROLLBACK', type: 'deployment' },
      })
      expect(wrapper.text()).toContain('回滚中')
      expect(wrapper.find('.badge-warning').exists()).toBe(true)
    })
  })

  describe('告警状态类型', () => {
    it('渲染 firing 状态 - 告警中', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'firing', type: 'alert' },
      })
      expect(wrapper.text()).toContain('告警中')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })

    it('渲染 resolved 状态 - 已解决', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'resolved', type: 'alert' },
      })
      expect(wrapper.text()).toContain('已解决')
      expect(wrapper.find('.badge-success').exists()).toBe(true)
    })

    it('渲染 critical 状态 - 严重', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'critical', type: 'alert' },
      })
      expect(wrapper.text()).toContain('严重')
      expect(wrapper.find('.badge-danger').exists()).toBe(true)
    })

    it('渲染 warning 状态 - 警告', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'warning', type: 'alert' },
      })
      expect(wrapper.text()).toContain('警告')
      expect(wrapper.find('.badge-warning').exists()).toBe(true)
    })
  })

  describe('未知状态处理', () => {
    it('未知状态显示原始状态文本', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'UNKNOWN_STATUS', type: 'agent' },
      })
      expect(wrapper.text()).toContain('UNKNOWN_STATUS')
    })

    it('未知状态使用 neutral 颜色', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'UNKNOWN_STATUS', type: 'agent' },
      })
      expect(wrapper.find('.badge-neutral').exists()).toBe(true)
    })
  })

  describe('自定义标签', () => {
    it('label prop 覆盖默认标签', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'DRAFT', type: 'agent', label: '自定义标签' },
      })
      expect(wrapper.text()).toContain('自定义标签')
      expect(wrapper.text()).not.toContain('草稿')
    })
  })

  describe('状态点', () => {
    it('dot=false 时不渲染状态点', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'RUNNING', type: 'agent', dot: false },
      })
      expect(wrapper.find('.status-dot').exists()).toBe(false)
    })

    it('dot=true 时渲染状态点', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'RUNNING', type: 'agent', dot: true },
      })
      expect(wrapper.find('.status-dot').exists()).toBe(true)
      expect(wrapper.find('.dot-success').exists()).toBe(true)
    })
  })

  describe('基础样式', () => {
    it('包含 status-badge 基础类', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'DRAFT', type: 'agent' },
      })
      expect(wrapper.find('.status-badge').exists()).toBe(true)
    })

    it('默认类型为 agent', () => {
      const wrapper = mount(StatusBadge, {
        props: { status: 'DRAFT' },
      })
      expect(wrapper.text()).toContain('草稿')
      expect(wrapper.find('.badge-neutral').exists()).toBe(true)
    })
  })
})
