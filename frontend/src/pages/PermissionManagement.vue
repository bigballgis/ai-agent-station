<template>
  <div class="permission-page" :aria-label="t('routes.permissionManagement')">
    <!-- 页面头部 -->
    <PageHeader :title="t('permission.management')" :subtitle="t('permission.managementDesc')">
      <template #actions>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="handleCreateRole"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ t('permission.createRole') }}
        </button>
      </template>
    </PageHeader>

    <!-- 左右分栏布局 -->
    <div class="flex gap-6 animate-slide-up">
      <!-- 左侧: 角色列表 -->
      <div class="w-80 flex-shrink-0">
        <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden">
          <div class="p-4 border-b border-neutral-100 dark:border-neutral-800">
            <div class="relative">
              <svg
                class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 dark:text-neutral-500"
                fill="none" stroke="currentColor" viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                v-model="roleSearch"
                type="text"
                :placeholder="t('permission.searchRolePlaceholder')"
                class="w-full pl-10 pr-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
              />
            </div>
          </div>
          <div class="p-2 space-y-1 max-h-[calc(100vh-280px)] overflow-y-auto scrollbar-thin">
            <!-- 加载状态 -->
            <div v-if="loading" class="p-4">
              <LoadingSkeleton type="table" :rows="3" />
            </div>
            <template v-else>
            <button
              v-for="role in filteredRoles"
              :key="role.id"
              @click="selectRole(role)"
              @keydown.enter="selectRole(role)"
              role="button"
              tabindex="0"
              :class="[
                'w-full text-left p-3.5 rounded-xl transition-all duration-200 group cursor-pointer',
                selectedRole?.id === role.id
                  ? 'bg-primary-50 dark:bg-primary-950/40 border border-primary-200 dark:border-primary-800'
                  : 'hover:bg-neutral-50 dark:hover:bg-neutral-800/60 border border-transparent',
              ]"
            >
              <div class="flex items-center justify-between mb-1">
                <span
                  :class="[
                    'text-sm font-semibold',
                    selectedRole?.id === role.id
                      ? 'text-primary-700 dark:text-primary-300'
                      : 'text-neutral-800 dark:text-neutral-200',
                  ]"
                >
                  {{ role.name }}
                </span>
                <span
                  :class="[
                    'text-[10px] font-medium px-2 py-0.5 rounded-full',
                    role.isSystem
                      ? 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400'
                      : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400',
                  ]"
                >
                  {{ role.isSystem ? t('permission.typeSystem') : t('permission.typeCustom') }}
                </span>
              </div>
              <p class="text-xs text-neutral-500 dark:text-neutral-400 line-clamp-1 mb-2">
                {{ role.description }}
              </p>
              <div class="flex items-center gap-3 text-[11px] text-neutral-400 dark:text-neutral-500">
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
                  </svg>
                  {{ role.userCount }} {{ t('permission.userCount', { count: role.userCount }) }}
                </span>
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  {{ role.permissionCount }} {{ t('permission.permissionCount', { count: role.permissionCount }) }}
                </span>
              </div>
            </button>
            </template>
          </div>
        </div>
      </div>

      <!-- 右侧: 角色详情 -->
      <div class="flex-1 min-w-0">
        <!-- 空状态 -->
        <div
          v-if="!selectedRole"
          class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 flex flex-col items-center justify-center py-24"
        >
          <div class="w-16 h-16 rounded-2xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center mb-4">
            <svg class="w-8 h-8 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('permission.selectRoleHint') }}</p>
        </div>

        <!-- 角色详情内容 -->
        <div v-else class="space-y-6">
          <!-- 角色基本信息 -->
          <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-6">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('permission.roleInfo') }}</h2>
              <div class="flex items-center gap-2">
                <button
                  v-if="!editingRole"
                  class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
                  @click="editingRole = true"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  {{ t('common.edit') }}
                </button>
                <button
                  v-if="editingRole"
                  class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
                  @click="saveRoleInfo"
                >
                  {{ t('common.save') }}
                </button>
                <button
                  v-if="editingRole"
                  class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
                  @click="cancelEdit"
                >
                  {{ t('common.cancel') }}
                </button>
                <button
                  v-if="!selectedRole.isSystem"
                  class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium text-red-500 dark:text-red-400 bg-red-50 dark:bg-red-950/30 hover:bg-red-100 dark:hover:bg-red-950/50 transition-colors duration-200 cursor-pointer"
                  @click="handleDeleteRole"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  {{ t('common.delete') }}
                </button>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleName') }}</label>
                <input
                  v-if="editingRole"
                  v-model="editForm.name"
                  type="text"
                  class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
                />
                <p v-else class="text-sm text-neutral-800 dark:text-neutral-200">{{ selectedRole.name }}</p>
              </div>
              <div>
                <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleCode') }}</label>
                <p class="text-sm text-neutral-500 dark:text-neutral-400 font-mono">{{ selectedRole.code }}</p>
              </div>
              <div class="md:col-span-2">
                <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleDescription') }}</label>
                <textarea
                  v-if="editingRole"
                  v-model="editForm.description"
                  rows="2"
                  class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 resize-none"
                />
                <p v-else class="text-sm text-neutral-600 dark:text-neutral-300">{{ selectedRole.description }}</p>
              </div>
            </div>
          </div>

          <!-- 权限树 -->
          <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-6">
            <div class="flex items-center justify-between mb-5">
              <div>
                <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('permission.permissionConfig') }}</h2>
                <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ t('permission.selectPermissions') }}</p>
              </div>
              <div class="flex items-center gap-2">
                <button
                  class="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 transition-colors cursor-pointer"
                  @click="expandAll"
                >
                  {{ t('permission.expandAll') }}
                </button>
                <span class="text-neutral-300 dark:text-neutral-600">|</span>
                <button
                  class="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 transition-colors cursor-pointer"
                  @click="collapseAll"
                >
                  {{ t('permission.collapseAll') }}
                </button>
              </div>
            </div>
            <a-tree
              v-model:checkedKeys="checkedPermissions"
              v-model:expandedKeys="expandedKeys"
              :tree-data="permissionTree"
              checkable
              :selectable="false"
              class="permission-tree"
            >
              <template #title="{ title }">
                <span class="text-sm text-neutral-700 dark:text-neutral-300">{{ title }}</span>
              </template>
            </a-tree>
          </div>

          <!-- 角色下的用户列表 -->
          <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-6">
            <div class="flex items-center justify-between mb-5">
              <div>
                <h2 class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ t('permission.roleUsers') }}</h2>
                <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">
                  {{ t('permission.roleUserCount', { count: roleUsers.length }) }}
                </p>
              </div>
              <button
                class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-950/30 hover:bg-primary-100 dark:hover:bg-primary-950/50 transition-colors duration-200 cursor-pointer"
                @click="showAddUserModal = true"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                {{ t('permission.addUser') }}
              </button>
            </div>
            <div class="md:hidden text-center text-sm text-neutral-500 dark:text-neutral-400 py-3">
              {{ t('common.mobileTableTip') }}
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-neutral-100 dark:border-neutral-800">
                    <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('permission.userName') }}</th>
                    <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('permission.email') }}</th>
                    <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('permission.department') }}</th>
                    <th class="text-left py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('permission.joinTime') }}</th>
                    <th class="text-right py-3 px-4 text-xs font-medium text-neutral-500 dark:text-neutral-400">{{ t('permission.operation') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="user in roleUsers"
                    :key="user.id"
                    class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors"
                  >
                    <td class="py-3 px-4">
                      <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium">
                          {{ user.name.charAt(0) }}
                        </div>
                        <span class="text-neutral-800 dark:text-neutral-200 font-medium">{{ user.name }}</span>
                      </div>
                    </td>
                    <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400">{{ user.email }}</td>
                    <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400">{{ user.department }}</td>
                    <td class="py-3 px-4 text-neutral-500 dark:text-neutral-400">{{ user.joinedAt }}</td>
                    <td class="py-3 px-4 text-right">
                      <button
                        class="text-xs text-red-500 hover:text-red-600 dark:text-red-400 dark:hover:text-red-300 transition-colors cursor-pointer"
                        @click="removeUser(user.id)"
                      >
                        {{ t('permission.remove') }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除角色确认弹窗 -->
    <ConfirmModal
      v-model:visible="deleteVisible"
      :title="t('common.confirmDelete')"
      :content="t('permission.deleteRoleContent', { name: selectedRole?.name })"
      type="delete"
      :ok-text="t('common.delete')"
      @ok="confirmDeleteRole"
    />

    <!-- 新建角色弹窗 -->
    <a-modal
      v-model:open="showCreateModal"
      :title="t('permission.createRole')"
      :footer="null"
      :width="480"
      centered
    >
      <div class="space-y-4 pt-2">
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleName') }} <span class="text-red-500">*</span></label>
          <input
            v-model="createForm.name"
            type="text"
            :placeholder="t('permission.inputRoleNamePlaceholder')"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleCode') }} <span class="text-red-500">*</span></label>
          <input
            v-model="createForm.code"
            type="text"
            :placeholder="t('permission.inputRoleCodePlaceholder')"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 font-mono"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('permission.roleDescription') }}</label>
          <textarea
            v-model="createForm.description"
            :placeholder="t('permission.inputRoleDescPlaceholder')"
            rows="3"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 resize-none"
          />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="showCreateModal = false"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
            @click="handleCreateRoleSubmit"
          >
            {{ t('common.confirm') }}
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  getPermissions,
  getRoles,
  createRole,
  updateRole,
  deleteRole,
  removeRole,
  getRolePermissions,
} from '@/api/permission'
import { getUsers } from '@/api/user'
import { PageHeader, ConfirmModal, LoadingSkeleton } from '@/components'

const { t } = useI18n()

// ============ 数据 ============

interface Role {
  id: string
  name: string
  code: string
  description: string
  isSystem: boolean
  userCount: number
  permissionCount: number
}

interface RoleUser {
  id: string
  name: string
  email: string
  department: string
  joinedAt: string
}

const roles = ref<Role[]>([])
const roleUsers = ref<RoleUser[]>([])
const loading = ref(false)

// 权限树数据（从 API 获取）
interface PermissionTreeNode {
  key: string
  title: string
  children?: PermissionTreeNode[]
  [key: string]: unknown
}

const permissionTree = ref<PermissionTreeNode[]>([])

async function fetchPermissions() {
  try {
    const res = await getPermissions()
    const data = res.data || res || []
    permissionTree.value = Array.isArray(data) ? data : []
  } catch (e: unknown) {
    console.error('获取权限列表失败:', e)
    message.error(t('permission.fetchPermissionsFailed'))
  }
}

async function fetchRoles() {
  loading.value = true
  try {
    const res = await getRoles()
    roles.value = res.data || res || []
  } catch (e: unknown) {
    console.error('获取角色列表失败:', e)
    message.error(t('permission.fetchRolesFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchUsers() {
  try {
    const res = await getUsers()
    const users = res.data || res || []
    roleUsers.value = Array.isArray(users) ? users.map((u: Record<string, unknown>) => ({
      id: String(u.id),
      name: (u.name || u.username || '') as string,
      email: (u.email || '') as string,
      department: (u.department || '') as string,
      joinedAt: (u.createdAt || '') as string,
    })) : []
  } catch (e: unknown) {
    console.error('获取用户列表失败:', e)
    message.error(t('permission.fetchUsersFailed'))
  }
}

async function loadRoleUsers() {
  if (!selectedRole.value) return
  try {
    const res = await getUsers()
    const users = res.data || res || []
    roleUsers.value = Array.isArray(users) ? users.map((u: Record<string, unknown>) => ({
      id: String(u.id),
      name: (u.name || u.username || '') as string,
      email: (u.email || '') as string,
      department: (u.department || '') as string,
      joinedAt: (u.createdAt || '') as string,
    })) : []
  } catch (e) {
    console.error('获取角色用户失败:', e)
  }
}

onMounted(async () => {
  await Promise.all([fetchPermissions(), fetchRoles(), fetchUsers()])
})

// ============ 状态 ============

const roleSearch = ref('')
const selectedRole = ref<Role | null>(null)
const editingRole = ref(false)
const editForm = ref({ name: '', description: '' })
const showCreateModal = ref(false)
const showAddUserModal = ref(false)
const deleteVisible = ref(false)
const createForm = ref({ name: '', code: '', description: '' })
const checkedPermissions = ref<string[]>(['agent:list', 'agent:create', 'agent:edit', 'approval:list', 'approval:submit', 'deployment:list', 'api:list', 'api:docs', 'system:user', 'system:role', 'log:operation', 'log:api', 'log:error'])
const expandedKeys = ref<string[]>(['agent', 'approval', 'deployment', 'api', 'system', 'log'])

// ============ 计算属性 ============

const filteredRoles = computed(() => {
  if (!roleSearch.value) return roles.value
  const q = roleSearch.value.toLowerCase()
  return roles.value.filter(
    r => r.name.toLowerCase().includes(q) || r.code.toLowerCase().includes(q) || r.description.toLowerCase().includes(q)
  )
})

// ============ 方法 ============

async function selectRole(role: Role) {
  selectedRole.value = role
  editingRole.value = false
  editForm.value = { name: role.name, description: role.description }
  // 从 API 获取该角色的实际权限
  try {
    const res = await getRolePermissions(Number(role.id))
    const data = res?.data || res || []
    checkedPermissions.value = Array.isArray(data)
      ? data.map((p: Record<string, unknown>) => (p.code || p.key || p.name) as string)
      : []
  } catch (e) {
    console.error('获取角色权限失败:', e)
    checkedPermissions.value = []
  }
  loadRoleUsers()
}

function handleCreateRole() {
  createForm.value = { name: '', code: '', description: '' }
  showCreateModal.value = true
}

function handleCreateRoleSubmit() {
  if (!createForm.value.name) {
    message.error(t('permission.roleNameRequired'))
    return
  }
  if (!createForm.value.code) {
    message.error(t('permission.roleCodeRequired'))
    return
  }
  createRole(createForm.value)
    .then((res: { data?: { id?: number }; id?: number }) => {
      const newRole: Role = {
        id: String(res.data?.id || res?.id || Date.now()),
        name: createForm.value.name,
        code: createForm.value.code.toUpperCase(),
        description: createForm.value.description || t('permission.customRoleDefault'),
        isSystem: false,
        userCount: 0,
        permissionCount: 0,
      }
      roles.value.push(newRole)
      showCreateModal.value = false
      message.success(t('permission.roleCreated'))
      selectRole(newRole)
    })
    .catch((e: unknown) => {
      console.error('创建角色失败:', e)
      message.error(t('permission.createRoleFailed'))
    })
}

function saveRoleInfo() {
  if (!selectedRole.value) return
  if (!editForm.value.name) {
    message.error(t('permission.roleNameRequired'))
    return
  }
  updateRole(Number(selectedRole.value.id), {
    name: editForm.value.name,
    description: editForm.value.description,
  })
    .then(() => {
      if (selectedRole.value) {
        selectedRole.value.name = editForm.value.name
        selectedRole.value.description = editForm.value.description
      }
      editingRole.value = false
      message.success(t('permission.roleUpdated'))
      fetchRoles()
    })
    .catch((e: unknown) => {
      console.error('更新角色信息失败:', e)
      message.error(t('permission.updateRoleFailed'))
    })
}

function cancelEdit() {
  editingRole.value = false
  if (selectedRole.value) {
    editForm.value = { name: selectedRole.value.name, description: selectedRole.value.description }
  }
}

function handleDeleteRole() {
  if (!selectedRole.value) return
  deleteVisible.value = true
}

async function confirmDeleteRole() {
  if (!selectedRole.value) return
  try {
    await deleteRole(Number(selectedRole.value.id))
    roles.value = roles.value.filter(r => r.id !== selectedRole.value!.id)
    selectedRole.value = null
    message.success(t('permission.roleDeleted'))
  } catch (e: unknown) {
    console.error('删除角色失败:', e)
    message.error(t('permission.deleteRoleFailed'))
  } finally {
    deleteVisible.value = false
  }
}

function removeUser(userId: string) {
  if (!selectedRole.value) return
  const user = roleUsers.value.find(u => u.id === userId)
  Modal.confirm({
    title: t('permission.confirmRemove'),
    content: t('permission.confirmRemoveContent'),
    okText: t('permission.confirmRemove'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    async onOk() {
      try {
        await removeRole(Number(userId), Number(selectedRole.value!.id))
        roleUsers.value = roleUsers.value.filter(u => u.id !== userId)
        message.success(t('permission.userRemoved'))
      } catch (e: unknown) {
        console.error('移除用户失败:', e)
        message.error(t('permission.removeUserFailed'))
      }
    }
  })
}

function expandAll() {
  expandedKeys.value = permissionTree.value.map((m) => m.key)
}

function collapseAll() {
  expandedKeys.value = []
}


</script>

<style scoped>
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

/* 权限树样式覆盖 */
:deep(.permission-tree .ant-tree-node-content-wrapper) {
  padding: 4px 8px !important;
  border-radius: 8px !important;
  transition: background-color 0.2s;
}
:deep(.permission-tree .ant-tree-node-content-wrapper:hover) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}
.dark :deep(.permission-tree .ant-tree-node-content-wrapper:hover) {
  background-color: rgba(255, 255, 255, 0.03) !important;
}
:deep(.permission-tree .ant-tree-checkbox) {
  margin-right: 8px;
}
:deep(.permission-tree .ant-tree-treenode) {
  padding: 2px 0 !important;
}
</style>
