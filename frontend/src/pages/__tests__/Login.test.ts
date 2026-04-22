import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import Login from '../Login.vue'

// Mock store
const mockLogin = vi.fn()
vi.mock('@/store/modules/user', () => ({
  useUserStore: () => ({
    login: mockLogin
  })
}))

vi.mock('@/store/modules/app', () => ({
  useAppStore: () => ({
    locale: 'zh-CN',
    setLocale: vi.fn()
  })
}))

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: mockPush
    })
  }
})

// Mock ant-design-vue
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

// Mock icons
vi.mock('@ant-design/icons-vue', () => ({
  UserOutlined: { name: 'UserOutlined', template: '<span>user-icon</span>' },
  LockOutlined: { name: 'LockOutlined', template: '<span>lock-icon</span>' }
}))

describe('Login 页面', () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/dashboard', component: { template: '<div>Dashboard</div>' } }
    ]
  })

  const i18n = createI18n({
    legacy: false,
    locale: 'zh-CN',
    messages: {
      'zh-CN': {
        login: {
          welcomeBack: '欢迎回来',
          loginToStation: '登录到 AI Agent Station',
          username: '用户名',
          password: '密码',
          usernamePlaceholder: '请输入用户名',
          passwordPlaceholder: '请输入密码',
          usernameRequired: '请输入用户名',
          passwordRequired: '请输入密码',
          remember: '记住我',
          login: '登录',
          loginSuccess: '登录成功',
          loginFailed: '登录失败',
          brandDescription: '智能体管理与编排平台'
        }
      }
    }
  })

  const globalMountOptions = {
    plugins: [router, i18n],
    stubs: {
      'a-form': {
        template: '<form><slot /></form>',
        methods: {
          validate: () => Promise.resolve()
        }
      },
      'a-form-item': {
        template: '<div class="ant-form-item"><slot /></div>'
      },
      'a-input': {
        props: ['value', 'placeholder', 'size'],
        template: '<input class="ant-input" :value="value" :placeholder="placeholder" />',
        emits: ['update:value']
      },
      'a-input-password': {
        props: ['value', 'placeholder', 'size'],
        template: '<input type="password" class="ant-input-password" :value="value" :placeholder="placeholder" />',
        emits: ['update:value']
      },
      'a-checkbox': {
        props: ['checked'],
        template: '<input type="checkbox" class="ant-checkbox" :checked="checked" />',
        emits: ['update:checked']
      },
      'a-button': {
        props: ['type', 'size', 'loading', 'block', 'htmlType', 'disabled'],
        template: '<button class="ant-button" :disabled="disabled" :loading="loading"><slot /></button>',
        emits: ['click']
      }
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockLogin.mockReset()
  })

  it('渲染登录页面 - 包含欢迎标题', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('欢迎回来')
  })

  it('渲染登录页面 - 包含品牌名称', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('AI Agent Station')
  })

  it('渲染登录表单 - 包含用户名和密码输入框', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    // 检查用户名和密码标签
    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
  })

  it('渲染登录按钮', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    const buttons = wrapper.findAll('.ant-button')
    expect(buttons.length).toBeGreaterThan(0)
  })

  it('包含记住我选项', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('记住我')
  })

  it('包含语言切换链接', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('中文')
    expect(wrapper.text()).toContain('English')
  })

  it('渲染品牌描述', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    expect(wrapper.text()).toContain('智能体管理与编排平台')
  })

  it('登录表单存在form-item', () => {
    const wrapper = mount(Login, {
      global: globalMountOptions
    })

    const formItems = wrapper.findAll('.ant-form-item')
    // 至少包含用户名、密码和登录按钮的form-item
    expect(formItems.length).toBeGreaterThanOrEqual(2)
  })
})
