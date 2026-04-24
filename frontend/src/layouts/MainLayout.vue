<template>
  <div class="h-screen flex flex-col overflow-hidden bg-neutral-50 dark:bg-neutral-950">
    <!-- ========== 顶部导航栏 (h-14, 56px) ========== -->
    <header
      class="h-14 flex-shrink-0 flex items-center justify-between px-4 border-b border-neutral-200/60 dark:border-neutral-800/60 backdrop-blur-xl bg-white/80 dark:bg-neutral-900/80 z-50"
    >
      <!-- 左侧: Logo -->
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center flex-shrink-0 shadow-glow">
          <RobotOutlined class="text-white text-base" />
        </div>
        <span class="text-base font-semibold text-neutral-800 dark:text-neutral-100 tracking-tight whitespace-nowrap">
          AI Agent Station
        </span>
      </div>

      <!-- 中间: 全局搜索框 -->
      <div class="hidden md:flex items-center flex-1 max-w-md mx-8">
        <div class="relative w-full group">
          <SearchOutlined class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 text-sm" />
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('common.search') + '...'"
            class="w-full h-9 pl-9 pr-4 rounded-xl bg-neutral-100/80 dark:bg-neutral-800/60 border border-transparent focus:border-primary-400 dark:focus:border-primary-500 text-sm text-neutral-700 dark:text-neutral-200 placeholder-neutral-400 dark:placeholder-neutral-500 outline-none transition-all duration-300 focus:shadow-glow focus:bg-white dark:focus:bg-neutral-800"
            @keydown.enter="handleSearch"
          />
          <kbd class="absolute right-3 top-1/2 -translate-y-1/2 hidden lg:inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[10px] font-medium text-neutral-400 dark:text-neutral-500 bg-neutral-200/60 dark:bg-neutral-700/40 border border-neutral-200/80 dark:border-neutral-700/60">
            <span class="text-xs">&#8984;</span>K
          </kbd>
        </div>
      </div>

      <!-- 右侧: 操作区 -->
      <div class="flex items-center gap-1">
        <!-- 通知铃铛 -->
        <a-badge :count="notificationStore.unreadCount" :offset="[-4, 4]" size="small">
          <button
            @click="router.push('/system/alerts')"
            :aria-label="t('header.notifications')"
            class="w-9 h-9 rounded-xl flex items-center justify-center text-neutral-500 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors duration-200"
          >
            <BellOutlined class="text-base" />
          </button>
        </a-badge>

        <!-- 暗色模式切换 -->
        <button
          @click="toggleTheme"
          class="w-9 h-9 rounded-xl flex items-center justify-center text-neutral-500 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors duration-200"
        >
          <BulbOutlined v-if="isDark" class="text-base" />
          <BulbFilled v-else class="text-base" />
        </button>

        <!-- 语言下拉 -->
        <a-dropdown>
          <button
            class="h-9 px-2.5 rounded-xl flex items-center gap-1.5 text-sm text-neutral-500 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors duration-200"
          >
            <GlobalOutlined class="text-sm" />
            <span class="hidden sm:inline">{{ currentLocale === 'zh-CN' ? t('i18n.chinese') : 'EN' }}</span>
          </button>
          <template #overlay>
            <a-menu @click="changeLocale">
              <a-menu-item key="zh-CN">{{ t('i18n.chinese') }}</a-menu-item>
              <a-menu-item key="en-US">English</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>

        <!-- 用户头像+名称下拉 -->
        <a-dropdown>
          <button class="h-9 pl-1 pr-3 rounded-xl flex items-center gap-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors duration-200">
            <a-avatar :size="30" class="bg-primary-500 flex items-center justify-center">
              <UserOutlined class="text-white text-sm" />
            </a-avatar>
            <span class="hidden sm:inline text-sm font-medium text-neutral-700 dark:text-neutral-200 max-w-[100px] truncate">
              {{ userInfo.nickname || userInfo.username || 'Admin' }}
            </span>
          </button>
          <template #overlay>
            <a-menu @click="handleUserMenu">
              <a-menu-item key="changePassword">
                <LockOutlined class="mr-2" />
                {{ t('password.changePassword') }}
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout">
                <LogoutOutlined class="mr-2" />
                {{ t('header.logout') }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </header>

    <!-- ========== 主体区域 ========== -->
    <div class="flex flex-1 overflow-hidden">
      <!-- 移动端遮罩 -->
      <div
        v-if="isMobile && !collapsed"
        class="fixed inset-0 bg-black/30 backdrop-blur-sm z-30 transition-opacity duration-300"
        @click="collapsed = true"
      />

      <!-- 左侧菜单 -->
      <aside
        :class="[
          'flex-shrink-0 flex flex-col border-r border-neutral-200/60 dark:border-neutral-800/60 backdrop-blur-xl bg-white/80 dark:bg-neutral-900/80 transition-all duration-300 ease-bounce-in z-40 overflow-hidden',
          collapsed ? 'w-16' : 'w-60',
          isMobile && !collapsed ? 'fixed left-0 top-14 bottom-8' : 'relative',
          isMobile && collapsed ? 'hidden' : '',
        ]"
      >
        <!-- 菜单内容 -->
        <nav class="flex-1 overflow-y-auto py-3 px-2 scrollbar-thin">
          <template v-for="group in menuGroups" :key="group.label">
            <!-- 分组标题 -->
            <div
              v-if="!collapsed"
              class="px-3 pt-4 pb-1.5 first:pt-1"
            >
              <span class="text-[11px] font-semibold uppercase tracking-wider text-neutral-400 dark:text-neutral-500">
                {{ group.label }}
              </span>
            </div>
            <div v-else class="pt-3" />

            <!-- 菜单项 -->
            <template v-for="item in group.items" :key="item.key">
              <!-- 无子菜单 -->
              <button
                v-if="!item.children"
                @click="navigateTo(item)"
                :class="[
                  'w-full flex items-center gap-3 rounded-xl transition-all duration-200 group relative',
                  collapsed ? 'justify-center px-0 py-2.5' : 'px-3 py-2',
                  isActive(item.key)
                    ? 'bg-primary-50 dark:bg-primary-950/40 text-primary-600 dark:text-primary-400'
                    : 'text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800/60 hover:text-neutral-800 dark:hover:text-neutral-200',
                ]"
              >
                <!-- 选中指示条 -->
                <div
                  v-if="isActive(item.key)"
                  class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 rounded-r-full bg-primary-500"
                />
                <component
                  :is="item.icon"
                  :class="[
                    'text-base flex-shrink-0 transition-colors duration-200',
                    isActive(item.key)
                      ? 'text-primary-500 dark:text-primary-400'
                      : 'text-neutral-400 dark:text-neutral-500 group-hover:text-neutral-600 dark:group-hover:text-neutral-300',
                  ]"
                />
                <span v-if="!collapsed" class="text-sm font-medium truncate">{{ t(item.label) }}</span>
              </button>

              <!-- 有子菜单 -->
              <div v-else>
                <button
                  @click="toggleSubmenu(item.key)"
                  :class="[
                    'w-full flex items-center gap-3 rounded-xl transition-all duration-200 group relative',
                    collapsed ? 'justify-center px-0 py-2.5' : 'px-3 py-2',
                    isGroupActive(item.key)
                      ? 'bg-primary-50 dark:bg-primary-950/40 text-primary-600 dark:text-primary-400'
                      : 'text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800/60 hover:text-neutral-800 dark:hover:text-neutral-200',
                  ]"
                >
                  <div
                    v-if="isGroupActive(item.key)"
                    class="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 rounded-r-full bg-primary-500"
                  />
                  <component
                    :is="item.icon"
                    :class="[
                      'text-base flex-shrink-0 transition-colors duration-200',
                      isGroupActive(item.key)
                        ? 'text-primary-500 dark:text-primary-400'
                        : 'text-neutral-400 dark:text-neutral-500 group-hover:text-neutral-600 dark:group-hover:text-neutral-300',
                    ]"
                  />
                  <span v-if="!collapsed" class="text-sm font-medium truncate flex-1 text-left">{{ t(item.label) }}</span>
                  <RightOutlined
                    v-if="!collapsed"
                    :class="[
                      'text-xs transition-transform duration-200',
                      openKeys.includes(item.key) ? 'rotate-90' : '',
                    ]"
                  />
                </button>

                <!-- 子菜单展开 -->
                <div
                  v-if="!collapsed && openKeys.includes(item.key)"
                  class="ml-4 mt-0.5 space-y-0.5 animate-fade-in"
                >
                  <button
                    v-for="child in item.children"
                    :key="child.key"
                    @click="navigateTo(child)"
                    :class="[
                      'w-full flex items-center gap-2.5 rounded-lg transition-all duration-200 group relative',
                      'px-3 py-1.5',
                      isActive(child.key)
                        ? 'bg-primary-50 dark:bg-primary-950/30 text-primary-600 dark:text-primary-400'
                        : 'text-neutral-500 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800/40 hover:text-neutral-700 dark:hover:text-neutral-200',
                    ]"
                  >
                    <div
                      v-if="isActive(child.key)"
                      class="absolute left-0 top-1/2 -translate-y-1/2 w-[2px] h-4 rounded-r-full bg-primary-400"
                    />
                    <component
                      :is="child.icon"
                      :class="[
                        'text-sm flex-shrink-0',
                        isActive(child.key)
                          ? 'text-primary-500'
                          : 'text-neutral-400 dark:text-neutral-500 group-hover:text-neutral-500 dark:group-hover:text-neutral-300',
                      ]"
                    />
                    <span class="text-[13px] truncate">{{ t(child.label) }}</span>
                  </button>
                </div>
              </div>
            </template>
          </template>
        </nav>

        <!-- 侧边栏底部: 展开/收起按钮 -->
        <div class="flex-shrink-0 p-2 border-t border-neutral-200/60 dark:border-neutral-800/60">
          <button
            @click="collapsed = !collapsed"
            class="w-full h-9 rounded-xl flex items-center justify-center gap-2 text-neutral-400 dark:text-neutral-500 hover:bg-neutral-100 dark:hover:bg-neutral-800 hover:text-neutral-600 dark:hover:text-neutral-300 transition-all duration-200"
          >
            <MenuFoldOutlined v-if="!collapsed" class="text-sm" />
            <MenuUnfoldOutlined v-else class="text-sm" />
            <span v-if="!collapsed" class="text-xs font-medium">{{ collapsed ? '' : t('menu.collapseMenu') }}</span>
          </button>
        </div>
      </aside>

      <!-- ========== 主内容区 ========== -->
      <main class="flex-1 flex flex-col overflow-hidden">
        <!-- 面包屑导航 -->
        <div class="flex-shrink-0 h-10 flex items-center px-6 bg-white/50 dark:bg-neutral-900/30 border-b border-neutral-100 dark:border-neutral-800/40">
          <a-breadcrumb class="text-sm">
            <a-breadcrumb-item>
              <router-link to="/dashboard" class="text-neutral-400 dark:text-neutral-500 hover:text-primary-500 transition-colors">
                <HomeOutlined class="mr-1" />
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item
              v-for="crumb in breadcrumbs"
              :key="crumb.path"
            >
              <router-link
                v-if="crumb.path"
                :to="crumb.path"
                class="text-neutral-500 dark:text-neutral-400 hover:text-primary-500 transition-colors"
              >
                {{ crumb.title }}
              </router-link>
              <span v-else class="text-neutral-700 dark:text-neutral-200 font-medium">{{ crumb.title }}</span>
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <!-- 内容区域 -->
        <div class="flex-1 overflow-auto p-3 sm:p-4 md:p-6">
          <router-view v-slot="{ Component, route: viewRoute }">
            <transition name="page-fade" mode="out-in">
              <component :is="Component" :key="viewRoute.path" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>

    <!-- ========== 底部状态栏 (h-8, 32px) ========== -->
    <footer
      class="h-8 flex-shrink-0 flex items-center justify-between px-4 border-t border-neutral-200/60 dark:border-neutral-800/60 backdrop-blur-xl bg-white/80 dark:bg-neutral-900/80 z-50"
    >
      <div class="flex items-center gap-2">
        <span class="relative flex h-2 w-2">
          <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-success-500 opacity-75" />
          <span class="relative inline-flex rounded-full h-2 w-2 bg-success-500" />
        </span>
        <span class="text-[11px] text-neutral-400 dark:text-neutral-500">{{ t('common.systemRunning') }}</span>
      </div>
      <span class="text-[11px] text-neutral-400 dark:text-neutral-500">v1.0.0</span>
    </footer>

    <!-- 修改密码弹窗 -->
    <ChangePasswordModal
      v-model:visible="showChangePassword"
      @success="handlePasswordChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, type Component, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  GlobalOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  LockOutlined,
  DashboardOutlined,
  FolderOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
  ApiOutlined,
  BookOutlined,
  TeamOutlined,
  SettingFilled,
  BulbOutlined,
  BulbFilled,
  SearchOutlined,
  RightOutlined,
  HomeOutlined,
  FileSearchOutlined,
  PlayCircleOutlined,
  BarChartOutlined,
  SafetyOutlined,
  TranslationOutlined,
  MonitorOutlined,
  AppstoreOutlined,
  BugOutlined,
  DatabaseOutlined,
  AlertOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { useNotificationStore } from '@/store/modules/notification'
import { useTheme } from '@/composables/useTheme'
// ChangePasswordModal 仅在用户点击修改密码时显示，使用异步加载减少首屏包体积
const ChangePasswordModal = defineAsyncComponent(() => import('@/components/ChangePasswordModal.vue'))
import type { LocaleType } from '@/locales'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const userStore = useUserStore()
const appStore = useAppStore()
const notificationStore = useNotificationStore()
const { toggleTheme, isDark } = useTheme()

const collapsed = ref(false)
const openKeys = ref<string[]>([])
const searchQuery = ref('')
const windowWidth = ref(window.innerWidth)
const showChangePassword = ref(false)

// 全局搜索：按关键词跳转到Agent列表并带上搜索参数
const handleSearch = () => {
  const query = searchQuery.value.trim()
  if (query) {
    router.push({ path: '/agents', query: { search: query } })
    searchQuery.value = ''
  }
}

const isMobile = computed(() => windowWidth.value < 768)

const userInfo = computed(() => userStore.userInfo)
const currentLocale = computed(() => appStore.locale)


// 菜单项类型定义
type MenuItem = {
  key: string
  path?: string
  icon: Component
  label: string
  roles?: string[]
  children?: MenuItem[]
}

// 菜单分组定义（原始数据，包含 roles 属性）
const allMenuGroups: { label: string; items: MenuItem[] }[] = [
  {
    label: 'menu.workbench',
    items: [
      {
        key: '/dashboard',
        path: '/dashboard',
        icon: DashboardOutlined,
        label: 'menu.dashboard',
      },
    ],
  },
  {
    label: 'menu.agentManagement',
    items: [
      {
        key: '/agents',
        path: '/agents',
        icon: RobotOutlined,
        label: 'menu.agentManagement',
      },
      {
        key: '/agents/design',
        path: '/agents/design',
        icon: RobotOutlined,
        label: 'menu.agentDesign',
      },
      {
        key: '/mcp/tools',
        path: '/mcp/tools',
        icon: AppstoreOutlined,
        label: 'menu.mcpTools',
      },
      {
        key: '/agents/templates',
        path: '/agents/templates',
        icon: AppstoreOutlined,
        label: 'menu.templateMarket',
      },
      {
        key: '/agents/debugger',
        path: '/agents/debugger',
        icon: BugOutlined,
        label: 'menu.onlineDebug',
      },
      {
        key: '/agents/memory',
        path: '/agents/memory',
        icon: DatabaseOutlined,
        label: 'menu.memoryManagement',
      },
    ],
  },
  {
    label: 'menu.testCenter',
    items: [
      {
        key: '/test-cases',
        path: '/test-cases',
        icon: FileSearchOutlined,
        label: 'menu.testCaseManagement',
      },
      {
        key: '/test-executions',
        path: '/test-executions',
        icon: PlayCircleOutlined,
        label: 'menu.testExecutionManagement',
      },
      {
        key: '/test-results',
        path: '/test-results',
        icon: BarChartOutlined,
        label: 'menu.testResultManagement',
      },
    ],
  },
  {
    label: 'menu.opsManagement',
    items: [
      {
        key: 'ops-approval',
        icon: CheckCircleOutlined,
        label: 'menu.approval',
        children: [
          {
            key: '/agents/approval',
            path: '/agents/approval',
            icon: CheckCircleOutlined,
            label: 'menu.approval',
          },
        ],
      },
      {
        key: 'ops-publish',
        icon: ThunderboltOutlined,
        label: 'menu.testPublish',
        children: [
          {
            key: '/agents/deployment',
            path: '/agents/deployment',
            icon: ThunderboltOutlined,
            label: 'menu.testPublish',
          },
        ],
      },
      {
        key: 'ops-api',
        icon: ApiOutlined,
        label: 'menu.apiManagement',
        children: [
          {
            key: '/api/manage',
            path: '/api/manage',
            icon: ApiOutlined,
            label: 'menu.apiManagement',
          },
          {
            key: '/api/docs',
            path: '/api/docs',
            icon: BookOutlined,
            label: 'menu.apiDocumentation',
          },
        ],
      },
    ],
  },
  {
    label: 'menu.systemSettings',
    items: [
      {
        key: '/tenant',
        path: '/tenant',
        icon: TeamOutlined,
        label: 'menu.tenantManagement',
        roles: ['ADMIN', 'TENANT_ADMIN'],
      },
      {
        key: '/suggestions',
        path: '/suggestions',
        icon: BulbOutlined,
        label: 'menu.optimizationSuggestions',
      },
      {
        key: '/evolution',
        path: '/evolution',
        icon: BulbOutlined,
        label: 'menu.agentEvolution',
      },
      {
        key: 'system',
        icon: SettingFilled,
        label: 'menu.systemSettings',
        children: [
          {
            key: '/system/permission',
            path: '/system/permission',
            icon: SafetyOutlined,
            label: 'menu.permission',
            roles: ['ADMIN', 'TENANT_ADMIN'],
          },
          {
            key: '/system/i18n',
            path: '/system/i18n',
            icon: TranslationOutlined,
            label: 'menu.i18n',
          },
          {
            key: '/system/log',
            path: '/system/log',
            icon: MonitorOutlined,
            label: 'menu.log',
            roles: ['ADMIN', 'TENANT_ADMIN'],
          },
          {
            key: '/system/alerts',
            path: '/system/alerts',
            icon: AlertOutlined,
            label: 'menu.alertCenter',
          },
          {
            key: '/system/quota',
            path: '/system/quota',
            icon: DashboardOutlined,
            label: 'menu.quotaManagement',
          },
          {
            key: '/system/files',
            path: '/system/files',
            icon: FolderOutlined,
            label: 'menu.fileManagement',
          },
        ],
      },
    ],
  },
]

// 检查用户是否拥有指定角色
function hasAnyRole(roles?: string[]): boolean {
  if (!roles || roles.length === 0) return true
  const userRoles = userStore.userInfo?.roles || []
  return userRoles.some((role: string) => roles.includes(role))
}

// 过滤菜单项（递归处理子菜单）
function filterMenuItems(items: MenuItem[]): MenuItem[] {
  return items
    .map(item => {
      if (!item.children) return item
      const filteredChildren = filterMenuItems(item.children)
      return { ...item, children: filteredChildren }
    })
    .filter(item => {
      if (!hasAnyRole(item.roles)) return false
      if (item.children && item.children.length === 0) return false
      return true
    })
}

// 根据用户角色过滤后的菜单分组
const menuGroups = computed(() => {
  return allMenuGroups
    .map(group => ({
      ...group,
      items: filterMenuItems(group.items) as MenuItem[],
    }))
    .filter(group => group.items.length > 0)
})

// 面包屑计算
const breadcrumbs = computed(() => {
  const crumbs: { title: string; path?: string }[] = []
  const matched = route.matched.filter(r => r.meta?.title)
  matched.forEach((r, i) => {
    const isLast = i === matched.length - 1
    const titleKey = (r.meta.title as string) || ''
    crumbs.push({
      title: t(`routes.${titleKey}`) || titleKey,
      path: isLast ? undefined : r.path,
    })
  })
  return crumbs
})

// 判断菜单项是否激活
function isActive(key: string): boolean {
  return route.path === key || route.path.startsWith(key + '/')
}

// 判断分组是否激活（任一子项激活）
function isGroupActive(groupKey: string): boolean {
  const group = findGroupItem(groupKey)
  if (!group?.children) return false
  return group.children.some(child => isActive(child.key))
}

// 查找分组项
function findGroupItem(key: string) {
  for (const group of menuGroups.value) {
    const found = group.items.find(item => item.key === key)
    if (found) return found
  }
  return null
}

// 切换子菜单展开
function toggleSubmenu(key: string) {
  const idx = openKeys.value.indexOf(key)
  if (idx > -1) {
    openKeys.value.splice(idx, 1)
  } else {
    openKeys.value.push(key)
  }
}

// 导航
function navigateTo(item: { path?: string; key?: string }) {
  if (item.path) {
    router.push(item.path)
    // 移动端自动收起
    if (isMobile.value) {
      collapsed.value = true
    }
  }
}

// 切换语言
function changeLocale(e: { key: string }) {
  appStore.setLocale(e.key as LocaleType)
}

// 用户菜单操作
async function handleUserMenu(e: { key: string }) {
  if (e.key === 'changePassword') {
    showChangePassword.value = true
  } else if (e.key === 'logout') {
    Modal.confirm({
      title: t('header.logout'),
      content: t('header.logoutConfirm'),
      onOk: async () => {
        await userStore.logout()
        message.success(t('header.logoutSuccess'))
        router.push('/login')
      },
    })
  }
}

// 密码修改成功后跳转到登录页
function handlePasswordChanged() {
  message.info(t('password.passwordChangedRedirect'))
  userStore.logout()
  router.push('/login')
}

// 监听路由变化，自动展开对应子菜单
watch(
  () => route.path,
  (path) => {
    for (const group of menuGroups.value) {
      for (const item of group.items) {
        if (item.children) {
          const match = item.children.some(child => path === child.key || path.startsWith(child.key + '/'))
          if (match && !openKeys.value.includes(item.key)) {
            openKeys.value.push(item.key)
          }
        }
      }
    }
  },
  { immediate: true }
)

// 同步 locale
watch(
  () => appStore.locale,
  (newLocale) => {
    locale.value = newLocale
  },
  { immediate: true }
)

// 响应式窗口监听
function handleResize() {
  windowWidth.value = window.innerWidth
  if (windowWidth.value < 768) {
    collapsed.value = true
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 页面切换过渡动画 */
.page-fade-enter-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.page-fade-leave-active {
  transition: all 0.15s ease-in;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 自定义滚动条 */
.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 4px;
}
.dark .scrollbar-thin::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
}
.scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.15);
}
.dark .scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.15);
}

/* 覆盖 Ant Design Breadcrumb 样式 */
:deep(.ant-breadcrumb) {
  line-height: 1;
}
:deep(.ant-breadcrumb-separator) {
  color: #d4d4d4 !important;
  margin: 0 6px !important;
  font-size: 10px !important;
}
.dark :deep(.ant-breadcrumb-separator) {
  color: #525252 !important;
}

/* 覆盖 Ant Design Badge 样式 */
:deep(.ant-badge-count) {
  font-size: 10px !important;
  height: 16px !important;
  line-height: 16px !important;
  padding: 0 4px !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.8) !important;
}

/* 覆盖 Ant Design Dropdown 样式 */
:deep(.ant-dropdown-menu) {
  border-radius: 12px !important;
  padding: 4px !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08) !important;
}
:deep(.ant-dropdown-menu-item) {
  border-radius: 8px !important;
  padding: 6px 12px !important;
  margin: 2px 0 !important;
}
</style>
