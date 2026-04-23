<template>
  <div class="login-page min-h-screen flex">
    <!-- 左侧品牌区域 - 大屏显示 -->
    <div class="brand-panel hidden lg:flex lg:w-1/2 relative overflow-hidden flex-col items-center justify-center bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-700">
      <!-- 装饰性几何图形 -->
      <div class="geometric-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
        <div class="shape shape-6"></div>
        <div class="shape shape-7"></div>
        <div class="shape shape-8"></div>
      </div>

      <!-- 浮动粒子 -->
      <div class="particles">
        <div class="particle" v-for="n in 20" :key="n" :style="particleStyle(n)"></div>
      </div>

      <!-- 品牌内容 -->
      <div class="brand-content relative z-10 text-center px-12">
        <!-- AI 图标 -->
        <div class="brand-icon mb-8">
          <div class="icon-ring">
            <div class="icon-inner">
              <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
                <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
                <path d="M12 2v1"/>
                <circle cx="12" cy="6" r="1" fill="white" stroke="none"/>
                <path d="M8 6H4"/>
                <path d="M16 6h4"/>
                <path d="M12 10v2"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- 产品名称 -->
        <h1 class="brand-title text-5xl font-bold text-white mb-4 tracking-tight">
          AI Agent Station
        </h1>

        <!-- 产品描述 -->
        <p class="brand-description text-lg text-indigo-100 leading-relaxed max-w-md mx-auto">
          {{ t('login.brandDescription') || '智能体管理与编排平台，赋能团队高效构建、部署和管理 AI 智能体应用' }}
        </p>

        <!-- 特性标签 -->
        <div class="brand-features mt-10 flex flex-wrap justify-center gap-3">
          <span class="feature-tag">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
            {{ t('login.feature1') }}
          </span>
          <span class="feature-tag">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            {{ t('login.feature2') }}
          </span>
          <span class="feature-tag">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            {{ t('login.feature3') }}
          </span>
        </div>
      </div>

      <!-- 底部版权 -->
      <div class="absolute bottom-8 left-0 right-0 text-center text-indigo-200 text-sm opacity-60">
        &copy; 2025 AI Agent Station. All rights reserved.
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="form-panel w-full lg:w-1/2 flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4 py-8 lg:px-8">
      <div class="login-card w-full max-w-md animate-slide-up">
        <!-- 移动端 Logo -->
        <div class="lg:hidden text-center mb-8">
          <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg mb-4">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2a4 4 0 0 1 4 4v1a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/>
              <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
              <circle cx="12" cy="6" r="1" fill="white" stroke="none"/>
            </svg>
          </div>
          <h2 class="text-2xl font-bold text-gray-900 dark:text-white">AI Agent Station</h2>
        </div>

        <!-- 表单卡片 -->
        <div class="bg-white dark:bg-gray-800 rounded-3xl shadow-xl dark:shadow-gray-900/50 p-8 lg:p-10">
          <!-- Tab 切换 -->
          <div class="auth-tabs mb-8">
            <button
              :class="['auth-tab', activeTab === 'login' ? 'auth-tab-active' : 'auth-tab-inactive']"
              @click="switchTab('login')"
            >
              {{ t('login.login') }}
            </button>
            <button
              :class="['auth-tab', activeTab === 'register' ? 'auth-tab-active' : 'auth-tab-inactive']"
              @click="switchTab('register')"
            >
              {{ t('login.register') }}
            </button>
          </div>

          <!-- 标题 -->
          <div class="mb-8">
            <h2 v-if="activeTab === 'login'" class="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              {{ t('login.welcomeBack') || '欢迎回来' }}
            </h2>
            <h2 v-else class="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              {{ t('login.createAccount') || '创建账号' }}
            </h2>
            <p class="text-gray-500 dark:text-gray-400 text-base">
              {{ activeTab === 'login'
                ? (t('login.loginToStation') || '登录到 AI Agent Station')
                : (t('login.registerToStation') || '注册 AI Agent Station 账号') }}
            </p>
          </div>

          <!-- 登录表单 -->
          <a-form
            v-if="activeTab === 'login'"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            layout="vertical"
            @finish="handleLogin"
          >
            <!-- 用户名 -->
            <a-form-item name="username" class="form-item-custom">
              <label for="login-username" class="form-label">{{ t('login.username') }}</label>
              <a-input
                id="login-username"
                v-model:value="loginForm.username"
                size="large"
                :placeholder="t('login.usernamePlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>

            <!-- 密码 -->
            <a-form-item name="password" class="form-item-custom">
              <label for="login-password" class="form-label">{{ t('login.password') }}</label>
              <a-input-password
                id="login-password"
                v-model:value="loginForm.password"
                size="large"
                :placeholder="t('login.passwordPlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 验证码 -->
            <a-form-item name="captchaAnswer" class="form-item-custom">
              <label class="form-label">{{ t('login.captcha') }}</label>
              <div class="captcha-row">
                <a-input
                  v-model:value="loginForm.captchaAnswer"
                  size="large"
                  :placeholder="t('login.captchaPlaceholder')"
                  class="input-custom captcha-input"
                />
                <div class="captcha-display" @click="fetchCaptcha" :title="t('login.captchaRefresh')">
                  <span v-if="captchaLoading" class="captcha-loading">...</span>
                  <span v-else-if="captchaQuestion" class="captcha-text">{{ captchaQuestion }}</span>
                  <span v-else class="captcha-text" @click="fetchCaptcha">{{ t('login.captchaRefresh') }}</span>
                </div>
              </div>
            </a-form-item>

            <!-- 记住我 -->
            <div class="flex items-center justify-between mb-6">
              <a-checkbox v-model:checked="rememberMe" class="checkbox-custom">
                {{ t('login.remember') }}
              </a-checkbox>
              <a class="forgot-password-link" @click="showResetPassword = true">
                {{ t('login.forgotPassword') }}
              </a>
            </div>

            <!-- 登录按钮 -->
            <a-form-item class="mb-0">
              <a-button
                type="primary"
                size="large"
                html-type="submit"
                block
                :loading="loading"
                class="login-button"
              >
                {{ t('login.login') }}
              </a-button>
            </a-form-item>
          </a-form>

          <!-- 注册表单 -->
          <a-form
            v-else
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            layout="vertical"
            @finish="handleRegister"
          >
            <!-- 用户名 -->
            <a-form-item name="username" class="form-item-custom">
              <label class="form-label">{{ t('login.username') }}</label>
              <a-input
                v-model:value="registerForm.username"
                size="large"
                :placeholder="t('login.usernamePlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>

            <!-- 邮箱（可选） -->
            <a-form-item name="email" class="form-item-custom">
              <label class="form-label">{{ t('login.email') }} <span class="text-gray-400 text-xs">({{ t('login.optional') }})</span></label>
              <a-input
                v-model:value="registerForm.email"
                size="large"
                :placeholder="t('login.emailPlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <MailOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>

            <!-- 密码 -->
            <a-form-item name="password" class="form-item-custom">
              <label class="form-label">{{ t('login.password') }}</label>
              <a-input-password
                v-model:value="registerForm.password"
                size="large"
                :placeholder="t('login.passwordPlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 确认密码 -->
            <a-form-item name="confirmPassword" class="form-item-custom">
              <label class="form-label">{{ t('login.confirmPassword') }}</label>
              <a-input-password
                v-model:value="registerForm.confirmPassword"
                size="large"
                :placeholder="t('login.confirmPasswordPlaceholder')"
                class="input-custom"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 注册按钮 -->
            <a-form-item class="mb-0">
              <a-button
                type="primary"
                size="large"
                html-type="submit"
                block
                :loading="loading"
                class="login-button"
              >
                {{ t('login.register') }}
              </a-button>
            </a-form-item>
          </a-form>
        </div>

        <!-- 语言切换 -->
        <div class="text-center mt-6">
          <div class="inline-flex items-center gap-1 text-sm">
            <a
              @click="changeLocale('zh-CN')"
              :class="[
                'locale-link',
                appStore.locale === 'zh-CN' ? 'locale-active' : 'locale-inactive'
              ]"
            >
              中文
            </a>
            <span class="text-gray-300 dark:text-gray-600 mx-1">|</span>
            <a
              @click="changeLocale('en-US')"
              :class="[
                'locale-link',
                appStore.locale === 'en-US' ? 'locale-active' : 'locale-inactive'
              ]"
            >
              English
            </a>
          </div>
        </div>
      </div>

      <!-- 忘记密码/重置密码弹窗 -->
      <a-modal
        :open="showResetPassword"
        :title="t('password.resetPassword')"
        :confirm-loading="resetLoading"
        :ok-text="t('common.confirm')"
        :cancel-text="t('common.cancel')"
        @ok="handleResetPassword"
        @cancel="showResetPassword = false"
        :width="420"
        destroyOnClose
      >
        <div class="mt-4 mb-2">
          <a-alert
            :message="t('password.resetPasswordHint')"
            type="info"
            show-icon
            class="mb-4"
          />
          <a-form
            ref="resetFormRef"
            :model="resetForm"
            :rules="resetRules"
            layout="vertical"
          >
            <a-form-item :label="t('login.username')" name="username">
              <a-input
                v-model:value="resetForm.username"
                :placeholder="t('login.usernamePlaceholder')"
                size="large"
              >
                <template #prefix>
                  <UserOutlined class="text-neutral-400" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item :label="t('password.newPassword')" name="newPassword">
              <a-input-password
                v-model:value="resetForm.newPassword"
                :placeholder="t('password.newPasswordPlaceholder')"
                size="large"
              >
                <template #prefix>
                  <LockOutlined class="text-neutral-400" />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item :label="t('password.confirmPassword')" name="confirmPassword">
              <a-input-password
                v-model:value="resetForm.confirmPassword"
                :placeholder="t('password.confirmPasswordPlaceholder')"
                size="large"
              >
                <template #prefix>
                  <LockOutlined class="text-neutral-400" />
                </template>
              </a-input-password>
            </a-form-item>
          </a-form>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { register, getCaptcha } from '@/api/user'
import type { LocaleType } from '@/locales'

const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const appStore = useAppStore()

const activeTab = ref<'login' | 'register'>('login')
const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

// Captcha state
const captchaId = ref('')
const captchaQuestion = ref('')
const captchaLoading = ref(false)

async function fetchCaptcha() {
  try {
    captchaLoading.value = true
    const res = await getCaptcha()
    if (res.code === 200 && res.data) {
      captchaId.value = res.data.captchaId
      captchaQuestion.value = res.data.question
    }
  } catch {
    // silently fail
  } finally {
    captchaLoading.value = false
  }
}

const loginForm = reactive({
  username: '',
  password: '',
  captchaAnswer: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const loginRules = {
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('login.passwordMinLength'), trigger: 'blur' }
  ],
  captchaAnswer: [
    { required: true, message: t('login.captchaRequired'), trigger: 'blur' }
  ]
}

const registerRules = {
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 50, message: t('login.usernameLength'), trigger: 'blur' }
  ],
  email: [
    { type: 'email' as const, message: t('login.emailInvalid'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('login.passwordMinLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('login.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string) => {
        if (value && value !== registerForm.password) {
          return Promise.reject(t('login.passwordMismatch'))
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

function switchTab(tab: 'login' | 'register') {
  activeTab.value = tab
  if (tab === 'login' && !captchaQuestion.value) {
    fetchCaptcha()
  }
}

onMounted(() => {
  fetchCaptcha()
})

async function handleLogin() {
  try {
    await loginFormRef.value.validate()
    loading.value = true

    const success = await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      remember: rememberMe.value,
      captchaId: captchaId.value,
      captchaAnswer: loginForm.captchaAnswer
    })

    if (success) {
      message.success(t('login.loginSuccess'))
      router.push('/dashboard')
    } else {
      message.error(t('login.loginFailed'))
      fetchCaptcha()
    }
  } catch (error: unknown) {
    console.error('Login error:', error)
    const errMessage = error instanceof Error ? error.message : undefined
    const axiosError = error as { response?: { data?: { message?: string } } }
    message.error(axiosError?.response?.data?.message || errMessage || t('login.loginFailed'))
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value.validate()
    loading.value = true

    const res = await register({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      email: registerForm.email || undefined
    })

    if (res.code === 200 || res.data) {
      message.success(t('login.registerSuccess'))
      // 注册成功后自动登录（后端已返回token），存储登录信息
      if (res.data?.token) {
        userStore.setToken(res.data.token)
        router.push('/dashboard')
      } else {
        // 如果后端没有返回token，切换到登录tab
        activeTab.value = 'login'
        loginForm.username = registerForm.username
        loginForm.password = ''
      }
    } else {
      message.error(res.message || t('login.registerFailed'))
    }
  } catch (error: unknown) {
    console.error('Register error:', error)
    const errMessage = error instanceof Error ? error.message : undefined
    const axiosError = error as { response?: { data?: { message?: string } } }
    message.error(axiosError?.response?.data?.message || errMessage || t('login.registerFailed'))
  } finally {
    loading.value = false
  }
}

function changeLocale(newLocale: LocaleType) {
  appStore.setLocale(newLocale)
  locale.value = newLocale
}

function particleStyle(_n: number) {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 8
  const duration = Math.random() * 6 + 8
  const opacity = Math.random() * 0.3 + 0.1
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    opacity: opacity,
  }
}
</script>

<style scoped>
/* ===========================================
   Tab 切换
   =========================================== */

.auth-tabs {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 12px;
  padding: 4px;
}

:deep(.dark) .auth-tabs {
  background: #374151;
}

.auth-tab {
  flex: 1;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  text-align: center;
}

.auth-tab-active {
  background: white;
  color: #4f46e5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

:deep(.dark) .auth-tab-active {
  background: #1f2937;
  color: #818cf8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.auth-tab-inactive {
  background: transparent;
  color: #6b7280;
}

.auth-tab-inactive:hover {
  color: #4f46e5;
}

:deep(.dark) .auth-tab-inactive {
  color: #9ca3af;
}

:deep(.dark) .auth-tab-inactive:hover {
  color: #818cf8;
}

/* ===========================================
   入场动画
   =========================================== */

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

/* ===========================================
   左侧品牌面板
   =========================================== */

.brand-panel {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 30%, #7c3aed 70%, #7e22ce 100%);
  position: relative;
}

/* 装饰性几何图形 */
.geometric-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: white;
}

.shape-1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -60px;
  animation: floatShape 12s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -40px;
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
  animation: floatShape 15s ease-in-out infinite reverse;
}

.shape-3 {
  width: 120px;
  height: 120px;
  top: 30%;
  right: 15%;
  border-radius: 50%;
  opacity: 0.05;
  animation: floatShape 10s ease-in-out infinite;
}

.shape-4 {
  width: 80px;
  height: 80px;
  top: 60%;
  left: 20%;
  border-radius: 20%;
  transform: rotate(45deg);
  opacity: 0.06;
  animation: floatShape 8s ease-in-out infinite;
}

.shape-5 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  right: 20%;
  border-radius: 50%;
  opacity: 0.04;
  animation: floatShape 14s ease-in-out infinite;
}

.shape-6 {
  width: 60px;
  height: 60px;
  top: 15%;
  left: 30%;
  border-radius: 50%;
  opacity: 0.1;
  animation: floatShape 9s ease-in-out infinite reverse;
}

.shape-7 {
  width: 240px;
  height: 240px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  opacity: 0.03;
  animation: pulseShape 6s ease-in-out infinite;
}

.shape-8 {
  width: 40px;
  height: 40px;
  bottom: 30%;
  right: 30%;
  border-radius: 50%;
  opacity: 0.12;
  animation: floatShape 7s ease-in-out infinite;
}

@keyframes floatShape {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  33% {
    transform: translateY(-20px) rotate(5deg);
  }
  66% {
    transform: translateY(10px) rotate(-3deg);
  }
}

@keyframes pulseShape {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.03;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 0.06;
  }
}

/* 浮动粒子 */
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.particle {
  position: absolute;
  bottom: -10px;
  background: white;
  border-radius: 50%;
  animation: particleRise linear infinite;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) translateX(30px);
    opacity: 0;
  }
}

/* 品牌图标 */
.brand-icon {
  display: flex;
  justify-content: center;
}

.icon-ring {
  width: 100px;
  height: 100px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: iconFloat 4s ease-in-out infinite;
}

.icon-inner {
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes iconFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

/* 品牌标题 */
.brand-title {
  letter-spacing: -0.02em;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.15);
}

/* 特性标签 */
.feature-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.feature-tag:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}

/* ===========================================
   右侧表单区域
   =========================================== */

.form-panel {
  transition: background-color 0.3s ease;
}

/* 表单卡片 */
.login-card {
  position: relative;
}

/* 表单标签 */
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

:deep(.dark) .form-label {
  color: #d1d5db;
}

/* 表单项间距 */
.form-item-custom {
  margin-bottom: 20px;
}

/* 输入框样式 */
.input-custom :deep(.ant-input-affix-wrapper) {
  border-radius: 12px;
  border: 1.5px solid #e5e7eb;
  padding: 10px 14px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  background: #f9fafb;
}

.input-custom :deep(.ant-input-affix-wrapper:hover) {
  border-color: #a5b4fc;
  background: #fff;
}

.input-custom :deep(.ant-input-affix-wrapper-focused) {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
  background: #fff;
}

.input-custom :deep(.ant-input) {
  background: transparent;
  font-size: 15px;
}

.input-custom :deep(.ant-input::placeholder) {
  color: #9ca3af;
}

/* 暗色模式输入框 */
:deep(.dark) .input-custom :deep(.ant-input-affix-wrapper) {
  background: #1f2937;
  border-color: #374151;
  color: #f3f4f6;
}

:deep(.dark) .input-custom :deep(.ant-input-affix-wrapper:hover) {
  border-color: #6366f1;
  background: #111827;
}

:deep(.dark) .input-custom :deep(.ant-input-affix-wrapper-focused) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
  background: #111827;
}

:deep(.dark) .input-custom :deep(.ant-input) {
  color: #f3f4f6;
}

:deep(.dark) .input-custom :deep(.ant-input::placeholder) {
  color: #6b7280;
}

/* 输入框图标 */
.input-icon {
  color: #9ca3af;
  font-size: 16px;
  transition: color 0.3s ease;
}

.input-custom :deep(.ant-input-affix-wrapper-focused) .input-icon {
  color: #4f46e5;
}

:deep(.dark) .input-custom :deep(.ant-input-affix-wrapper-focused) .input-icon {
  color: #818cf8;
}

/* 复选框 */
.checkbox-custom :deep(.ant-checkbox-inner) {
  border-radius: 6px;
  border-color: #d1d5db;
}

.checkbox-custom :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background-color: #4f46e5;
  border-color: #4f46e5;
}

.checkbox-custom :deep(.ant-checkbox-wrapper) {
  color: #6b7280;
  font-size: 14px;
}

:deep(.dark) .checkbox-custom :deep(.ant-checkbox-inner) {
  border-color: #4b5563;
  background: #1f2937;
}

:deep(.dark) .checkbox-custom :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background-color: #6366f1;
  border-color: #6366f1;
}

:deep(.dark) .checkbox-custom :deep(.ant-checkbox-wrapper) {
  color: #9ca3af;
}

/* 登录按钮 */
.login-button {
  height: 48px !important;
  border-radius: 12px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 50%, #7c3aed 100%) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35) !important;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
  letter-spacing: 0.02em;
}

.login-button:hover {
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.45) !important;
  filter: brightness(1.05);
}

.login-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 10px rgba(79, 70, 229, 0.3) !important;
}

/* 语言切换 */
.locale-link {
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  padding: 2px 4px;
  border-radius: 4px;
}

.locale-active {
  color: #4f46e5;
}

.locale-inactive {
  color: #9ca3af;
}

.locale-inactive:hover {
  color: #4f46e5;
}

:deep(.dark) .locale-active {
  color: #818cf8;
}

:deep(.dark) .locale-inactive {
  color: #6b7280;
}

:deep(.dark) .locale-inactive:hover {
  color: #818cf8;
}

/* ===========================================
   表单验证错误样式
   =========================================== */

:deep(.ant-form-item-explain-error) {
  font-size: 12px;
  margin-top: 4px;
  padding-left: 2px;
}

:deep(.ant-form-item-with-help .ant-form-item-explain) {
  min-height: auto;
}

/* ===========================================
   响应式优化
   =========================================== */

@media (max-width: 1023px) {
  .login-card {
    max-width: 420px;
  }
}

/* ===========================================
   验证码样式
   =========================================== */

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-display {
  flex-shrink: 0;
  width: 140px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border: 1.5px solid #c7d2fe;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

:deep(.dark) .captcha-display {
  background: linear-gradient(135deg, #312e81 0%, #3730a3 100%);
  border-color: #4338ca;
}

.captcha-display:hover {
  border-color: #818cf8;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.15);
}

.captcha-text {
  font-size: 16px;
  font-weight: 700;
  color: #4338ca;
  letter-spacing: 1px;
  font-family: 'Courier New', monospace;
}

:deep(.dark) .captcha-text {
  color: #a5b4fc;
}

.captcha-loading {
  font-size: 20px;
  color: #6366f1;
  animation: captchaPulse 1s ease-in-out infinite;
}

@keyframes captchaPulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
</style>
