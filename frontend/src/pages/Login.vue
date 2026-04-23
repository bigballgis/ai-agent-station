<template>
  <div class="login-page min-h-screen flex">
    <!-- 左侧品牌区域 - 大屏显示 -->
    <BrandPanel />

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
              <label for="register-username" class="form-label">{{ t('login.username') }}</label>
              <a-input
                id="register-username"
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
              <label for="register-email" class="form-label">{{ t('login.email') }} <span class="text-gray-400 text-xs">({{ t('login.optional') }})</span></label>
              <a-input
                id="register-email"
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
              <label for="register-password" class="form-label">{{ t('login.password') }}</label>
              <a-input-password
                id="register-password"
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
              <label for="register-confirm-password" class="form-label">{{ t('login.confirmPassword') }}</label>
              <a-input-password
                id="register-confirm-password"
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
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { register, getCaptcha } from '@/api/user'
import type { LocaleType } from '@/locales'
import BrandPanel from '@/components/login/BrandPanel.vue'

const router = useRouter()
const route = useRoute()
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
    message.warning(t('login.captchaLoadFailed'))
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
      const redirect = route.query.redirect as string
      router.push(redirect || '/dashboard')
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
        const redirect = route.query.redirect as string
        router.push(redirect || '/dashboard')
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
