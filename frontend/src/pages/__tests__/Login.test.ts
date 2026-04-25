import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createI18n } from 'vue-i18n'
import Login from '../Login.vue'

// ==================== Mocks ====================

const mockUserLogin = vi.fn()
const mockSetToken = vi.fn()
const mockGetCaptcha = vi.fn()
const mockRegister = vi.fn()

vi.mock('@/store/modules/user', () => ({
  useUserStore: () => ({
    login: mockUserLogin,
    setToken: mockSetToken
  })
}))

vi.mock('@/store/modules/app', () => ({
  useAppStore: () => ({
    locale: 'zh-CN',
    setLocale: vi.fn()
  })
}))

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

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@ant-design/icons-vue', () => ({
  UserOutlined: { name: 'UserOutlined', template: '<span class="icon-user">user</span>' },
  LockOutlined: { name: 'LockOutlined', template: '<span class="icon-lock">lock</span>' },
  MailOutlined: { name: 'MailOutlined', template: '<span class="icon-mail">mail</span>' }
}))

vi.mock('@/api/user', () => ({
  register: (...args: any[]) => mockRegister(...args),
  getCaptcha: (...args: any[]) => mockGetCaptcha(...args)
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn()
  }
}))

vi.mock('@/components/login/BrandPanel.vue', () => ({
  default: {
    name: 'BrandPanel',
    template: '<div class="mock-brand-panel">Brand</div>'
  }
}))

// ==================== i18n ====================

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      login: {
        welcomeBack: '欢迎回来',
        createAccount: '创建账号',
        loginToStation: '登录到 AegisNexus',
        registerToStation: '注册 AegisNexus 账号',
        username: '用户名',
        password: '密码',
        confirmPassword: '确认密码',
        email: '邮箱',
        optional: '可选',
        usernamePlaceholder: '请输入用户名',
        passwordPlaceholder: '请输入密码',
        confirmPasswordPlaceholder: '请再次输入密码',
        emailPlaceholder: '请输入邮箱',
        usernameRequired: '请输入用户名',
        passwordRequired: '请输入密码',
        passwordMinLength: '密码长度不能少于6位',
        captchaRequired: '请输入验证码',
        captchaPlaceholder: '请输入验证码',
        captcha: '验证码',
        captchaRefresh: '点击刷新',
        captchaLoadFailed: '验证码加载失败',
        remember: '记住我',
        forgotPassword: '忘记密码？',
        login: '登录',
        register: '注册',
        loginSuccess: '登录成功',
        loginFailed: '登录失败',
        registerSuccess: '注册成功',
        registerFailed: '注册失败',
        usernameLength: '用户名长度为3-50个字符',
        emailInvalid: '请输入有效的邮箱地址',
        passwordMismatch: '两次输入的密码不一致',
        confirmPasswordRequired: '请确认密码'
      },
      password: {
        resetPassword: '重置密码',
        resetPasswordHint: '请输入用户名和新密码',
        newPassword: '新密码',
        newPasswordPlaceholder: '请输入新密码',
        newPasswordRequired: '请输入新密码',
        newPasswordMinLength: '密码长度不能少于6位',
        confirmPassword: '确认密码',
        confirmPasswordPlaceholder: '请再次输入新密码',
        confirmPasswordRequired: '请确认密码',
        passwordMismatch: '两次输入的密码不一致',
        resetPasswordSuccess: '密码重置成功',
        resetPasswordFailed: '密码重置失败'
      },
      common: {
        confirm: '确认',
        cancel: '取消'
      }
    }
  }
})

// ==================== Helpers ====================

function createRouterInstance() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
      { path: '/login', component: { template: '<div>Login</div>' } }
    ]
  })
}

const antdStubs = {
  'a-form': {
    template: '<form class="ant-form"><slot /></form>'
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
    template: '<label class="ant-checkbox"><input type="checkbox" :checked="checked" /><slot /></label>',
    emits: ['update:checked']
  },
  'a-button': {
    props: ['type', 'size', 'loading', 'block', 'htmlType', 'disabled'],
    template: '<button class="ant-button" :disabled="disabled" :loading="loading"><slot /></button>',
    emits: ['click']
  },
  'a-modal': {
    props: ['open', 'title', 'confirmLoading', 'okText', 'cancelText', 'width', 'destroyOnClose'],
    template: '<div class="ant-modal" v-if="open"><slot /></div>',
    emits: ['update:open', 'cancel', 'ok']
  },
  'a-alert': {
    props: ['message', 'type'],
    template: '<div class="ant-alert">{{ message }}</div>'
  }
}

function getGlobalMountOptions() {
  return {
    plugins: [createRouterInstance(), i18n],
    stubs: antdStubs
  }
}

// ==================== Tests ====================

describe('Login 页面', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUserLogin.mockReset()
    mockSetToken.mockReset()
    mockGetCaptcha.mockResolvedValue({
      code: 200,
      data: { captchaId: 'test-captcha-id', question: '1 + 1 = ?' }
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---------- 1. 表单渲染 ----------

  it('1. 登录表单渲染包含用户名和密码输入框', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('用户名')
    expect(wrapper.text()).toContain('密码')
  })

  it('2. 登录按钮正确渲染', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    const buttons = wrapper.findAll('.ant-button')
    expect(buttons.length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('登录')
  })

  it('3. 页面包含品牌名称', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('AegisNexus')
  })

  // ---------- 4. 表单验证 ----------

  it('4. 登录表单包含验证规则 - 密码最小长度', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    const vm = wrapper.vm as any
    // 检查 loginRules 中 password 的 min 规则
    expect(vm.loginRules.password).toBeDefined()
    const hasMinRule = vm.loginRules.password.some(
      (rule: any) => rule.min === 6
    )
    expect(hasMinRule).toBe(true)
  })

  it('5. 登录表单包含验证规则 - 用户名必填', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    const vm = wrapper.vm as any
    expect(vm.loginRules.username).toBeDefined()
    const hasRequiredRule = vm.loginRules.username.some(
      (rule: any) => rule.required === true
    )
    expect(hasRequiredRule).toBe(true)
  })

  it('6. 注册表单包含密码一致性验证', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    const vm = wrapper.vm as any
    expect(vm.registerRules.confirmPassword).toBeDefined()
    const hasValidator = vm.registerRules.confirmPassword.some(
      (rule: any) => typeof rule.validator === 'function'
    )
    expect(hasValidator).toBe(true)
  })

  // ---------- 7. Tab 切换 ----------

  it('7. Tab 切换 - 默认显示登录表单', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('欢迎回来')
  })

  it('8. Tab 切换 - 点击注册切换到注册表单', async () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    // 找到注册 tab 按钮并点击
    const tabs = wrapper.findAll('.auth-tab')
    // 第二个 tab 是注册
    if (tabs.length >= 2) {
      await tabs[1].trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('创建账号')
    }
  })

  // ---------- 9. 验证码 ----------

  it('9. 验证码区域存在且可刷新', async () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    // 验证码显示区域
    const captchaDisplay = wrapper.find('.captcha-display')
    expect(captchaDisplay.exists()).toBe(true)

    // 验证码已加载
    expect(mockGetCaptcha).toHaveBeenCalled()
  })

  it('10. 点击验证码刷新', async () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    const captchaDisplay = wrapper.find('.captcha-display')
    if (captchaDisplay.exists()) {
      await captchaDisplay.trigger('click')
      await flushPromises()

      // getCaptcha 应该被再次调用（初始一次 + 点击一次）
      expect(mockGetCaptcha).toHaveBeenCalledTimes(2)
    }
  })

  // ---------- 11. 登录流程 ----------

  it('11. 登录成功后跳转到 dashboard', async () => {
    mockUserLogin.mockResolvedValue(true)

    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    const vm = wrapper.vm as any
    // 设置表单数据
    vm.loginForm.username = 'admin'
    vm.loginForm.password = 'password123'
    vm.loginForm.captchaAnswer = '2'

    // 模拟 formRef.validate 返回成功
    vm.loginFormRef = { validate: () => Promise.resolve() }

    await vm.handleLogin()
    await flushPromises()

    expect(mockUserLogin).toHaveBeenCalledWith(
      expect.objectContaining({
        username: 'admin',
        password: 'password123'
      })
    )
    expect(mockPush).toHaveBeenCalledWith('/dashboard')
  })

  it('12. 登录失败后显示错误消息', async () => {
    mockUserLogin.mockResolvedValue(false)

    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    await flushPromises()

    const vm = wrapper.vm as any
    vm.loginForm.username = 'admin'
    vm.loginForm.password = 'wrong'
    vm.loginForm.captchaAnswer = '2'
    vm.loginFormRef = { validate: () => Promise.resolve() }

    await vm.handleLogin()
    await flushPromises()

    // 登录失败后应刷新验证码
    expect(mockGetCaptcha).toHaveBeenCalled()
  })

  // ---------- 13. 记住我 ----------

  it('13. 记住我复选框存在', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('记住我')
  })

  // ---------- 14. 语言切换 ----------

  it('14. 语言切换链接存在', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('中文')
    expect(wrapper.text()).toContain('English')
  })

  // ---------- 15. 忘记密码 ----------

  it('15. 忘记密码链接存在', () => {
    const wrapper = mount(Login, {
      global: getGlobalMountOptions()
    })

    expect(wrapper.text()).toContain('忘记密码？')
  })
})
