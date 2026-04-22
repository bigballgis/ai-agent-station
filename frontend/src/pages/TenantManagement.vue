<template>
  <div class="tenant-page">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            租户管理
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            管理多租户配置、资源配额和租户状态
          </p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="openCreateModal"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新建租户
        </button>
      </div>
    </div>

    <!-- 搜索/筛选栏 -->
    <div class="mb-6 flex flex-wrap items-center gap-3 animate-slide-up">
      <div class="relative flex-1 min-w-[240px] max-w-md">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400 dark:text-neutral-500"
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索租户名称..."
          class="w-full pl-10 pr-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
        />
      </div>
      <select
        v-model="statusFilter"
        class="px-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer min-w-[140px]"
      >
        <option value="">全部状态</option>
        <option value="active">活跃</option>
        <option value="inactive">停用</option>
      </select>
    </div>

    <!-- 租户表格 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 overflow-hidden animate-slide-up">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50/50 dark:bg-neutral-800/30">
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">租户名称</th>
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">Schema 名称</th>
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">状态</th>
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">Agent 数量</th>
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">API 调用量</th>
              <th class="text-left py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">创建时间</th>
              <th class="text-right py-3.5 px-5 text-xs font-semibold text-neutral-500 dark:text-neutral-400">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="tenant in paginatedTenants"
              :key="tenant.id"
              class="border-b border-neutral-50 dark:border-neutral-800/50 hover:bg-neutral-50 dark:hover:bg-neutral-800/30 transition-colors"
            >
              <td class="py-4 px-5">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl flex items-center justify-center text-white text-sm font-bold" :style="{ background: tenant.color }">
                    {{ tenant.name.charAt(0) }}
                  </div>
                  <div>
                    <p class="text-neutral-800 dark:text-neutral-200 font-semibold">{{ tenant.name }}</p>
                    <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ tenant.contact }}</p>
                  </div>
                </div>
              </td>
              <td class="py-4 px-5">
                <code class="text-xs px-2 py-1 rounded-md bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400 font-mono">{{ tenant.schema }}</code>
              </td>
              <td class="py-4 px-5">
                <span
                  :class="[
                    'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium',
                    tenant.status === 'active'
                      ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'
                      : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400',
                  ]"
                >
                  <span
                    :class="[
                      'w-1.5 h-1.5 rounded-full',
                      tenant.status === 'active' ? 'bg-green-500' : 'bg-neutral-400',
                    ]"
                  />
                  {{ tenant.status === 'active' ? '活跃' : '停用' }}
                </span>
              </td>
              <td class="py-4 px-5">
                <div class="flex items-center gap-1.5">
                  <span class="text-neutral-800 dark:text-neutral-200 font-medium">{{ tenant.agentCount }}</span>
                  <span class="text-xs text-neutral-400 dark:text-neutral-500">/ {{ tenant.quota.agentLimit }}</span>
                </div>
                <div class="w-20 h-1.5 rounded-full bg-neutral-100 dark:bg-neutral-800 mt-1.5">
                  <div
                    class="h-full rounded-full transition-all duration-300"
                    :class="tenant.agentCount / tenant.quota.agentLimit > 0.8 ? 'bg-red-500' : 'bg-blue-500'"
                    :style="{ width: `${Math.min(100, (tenant.agentCount / tenant.quota.agentLimit) * 100)}%` }"
                  />
                </div>
              </td>
              <td class="py-4 px-5 text-neutral-600 dark:text-neutral-300">
                {{ formatNumber(tenant.apiCalls) }}
              </td>
              <td class="py-4 px-5 text-neutral-500 dark:text-neutral-400 text-xs whitespace-nowrap">{{ tenant.createdAt }}</td>
              <td class="py-4 px-5 text-right">
                <div class="flex items-center justify-end gap-1">
                  <button
                    class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-blue-500 dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/30 transition-colors cursor-pointer"
                    title="编辑"
                    @click="openEditModal(tenant)"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-amber-500 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 transition-colors cursor-pointer"
                    :title="tenant.status === 'active' ? '停用' : '启用'"
                    @click="toggleTenantStatus(tenant)"
                  >
                    <svg v-if="tenant.status === 'active'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                    </svg>
                    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </button>
                  <button
                    class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-500 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-colors cursor-pointer"
                    title="查看详情"
                    @click="showDetail(tenant)"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                  <button
                    class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-purple-500 dark:hover:text-purple-400 hover:bg-purple-50 dark:hover:bg-purple-950/30 transition-colors cursor-pointer"
                    title="资源配额"
                    @click="openQuotaModal(tenant)"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between px-5 py-4 border-t border-neutral-100 dark:border-neutral-800">
        <span class="text-xs text-neutral-400 dark:text-neutral-500">共 {{ filteredTenants.length }} 个租户</span>
        <div class="flex items-center gap-1">
          <button
            :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', currentPage <= 1 ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
            :disabled="currentPage <= 1"
            @click="currentPage--"
          >
            上一页
          </button>
          <span class="px-3 py-1.5 text-xs text-neutral-500 dark:text-neutral-400">{{ currentPage }} / {{ totalPages }}</span>
          <button
            :class="['px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer', currentPage >= totalPages ? 'text-neutral-300 dark:text-neutral-600 cursor-not-allowed' : 'text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700']"
            :disabled="currentPage >= totalPages"
            @click="currentPage++"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 新建/编辑租户弹窗 -->
    <a-modal
      v-model:open="showModal"
      :title="editingTenant ? '编辑租户' : '新建租户'"
      :footer="null"
      :width="520"
      centered
    >
      <div class="space-y-4 pt-2">
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">租户名称 <span class="text-red-500">*</span></label>
          <input
            v-model="form.name"
            type="text"
            placeholder="请输入租户名称"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">描述</label>
          <textarea
            v-model="form.description"
            rows="2"
            placeholder="请输入租户描述"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 resize-none"
          />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">联系人 <span class="text-red-500">*</span></label>
            <input
              v-model="form.contact"
              type="text"
              placeholder="联系人姓名"
              class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">邮箱 <span class="text-red-500">*</span></label>
            <input
              v-model="form.email"
              type="email"
              placeholder="联系邮箱"
              class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>
        </div>
        <!-- 配额设置 -->
        <div class="pt-2 border-t border-neutral-100 dark:border-neutral-800">
          <p class="text-xs font-semibold text-neutral-500 dark:text-neutral-400 mb-3">配额设置</p>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">Agent 数量上限</label>
              <input
                v-model.number="form.quota.agentLimit"
                type="number"
                min="1"
                class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">API 调用上限/月</label>
              <input
                v-model.number="form.quota.apiLimit"
                type="number"
                min="1000"
                class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">Token 额度</label>
              <input
                v-model.number="form.quota.tokenLimit"
                type="number"
                min="10000"
                class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
              />
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="showModal = false"
          >
            取消
          </button>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
            @click="saveTenant"
          >
            {{ editingTenant ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 资源配额弹窗 -->
    <a-modal
      v-model:open="showQuotaModal"
      title="资源配额配置"
      :footer="null"
      :width="480"
      centered
    >
      <div v-if="quotaTenant" class="space-y-5 pt-2">
        <div class="p-4 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white text-sm font-bold" :style="{ background: quotaTenant.color }">
              {{ quotaTenant.name.charAt(0) }}
            </div>
            <div>
              <p class="text-sm font-semibold text-neutral-800 dark:text-neutral-200">{{ quotaTenant.name }}</p>
              <p class="text-xs text-neutral-400 dark:text-neutral-500">{{ quotaTenant.schema }}</p>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <!-- Agent 配额 -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium text-neutral-700 dark:text-neutral-300">Agent 数量</span>
              <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ quotaTenant.agentCount }} / {{ quotaForm.agentLimit }}</span>
            </div>
            <div class="w-full h-2 rounded-full bg-neutral-100 dark:bg-neutral-800">
              <div
                class="h-full rounded-full bg-blue-500 transition-all duration-300"
                :style="{ width: `${Math.min(100, (quotaTenant.agentCount / quotaForm.agentLimit) * 100)}%` }"
              />
            </div>
            <input
              v-model.number="quotaForm.agentLimit"
              type="number"
              min="1"
              class="w-full mt-2 px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>

          <!-- API 调用配额 -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium text-neutral-700 dark:text-neutral-300">API 调用量/月</span>
              <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ formatNumber(quotaTenant.apiCalls) }} / {{ formatNumber(quotaForm.apiLimit) }}</span>
            </div>
            <div class="w-full h-2 rounded-full bg-neutral-100 dark:bg-neutral-800">
              <div
                class="h-full rounded-full transition-all duration-300"
                :class="(quotaTenant.apiCalls / quotaForm.apiLimit) > 0.8 ? 'bg-red-500' : 'bg-green-500'"
                :style="{ width: `${Math.min(100, (quotaTenant.apiCalls / quotaForm.apiLimit) * 100)}%` }"
              />
            </div>
            <input
              v-model.number="quotaForm.apiLimit"
              type="number"
              min="1000"
              class="w-full mt-2 px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>

          <!-- Token 配额 -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-medium text-neutral-700 dark:text-neutral-300">Token 额度</span>
              <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ formatNumber(quotaTenant.tokenUsed) }} / {{ formatNumber(quotaForm.tokenLimit) }}</span>
            </div>
            <div class="w-full h-2 rounded-full bg-neutral-100 dark:bg-neutral-800">
              <div
                class="h-full rounded-full transition-all duration-300"
                :class="(quotaTenant.tokenUsed / quotaForm.tokenLimit) > 0.8 ? 'bg-red-500' : 'bg-purple-500'"
                :style="{ width: `${Math.min(100, (quotaTenant.tokenUsed / quotaForm.tokenLimit) * 100)}%` }"
              />
            </div>
            <input
              v-model.number="quotaForm.tokenLimit"
              type="number"
              min="10000"
              class="w-full mt-2 px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="showQuotaModal = false"
          >
            取消
          </button>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
            @click="saveQuota"
          >
            保存配额
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 租户详情弹窗 -->
    <a-modal
      v-model:open="showDetailModal"
      title="租户详情"
      :footer="null"
      :width="560"
      centered
    >
      <div v-if="detailTenant" class="space-y-4 pt-2">
        <div class="flex items-center gap-4 p-4 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-white text-xl font-bold" :style="{ background: detailTenant.color }">
            {{ detailTenant.name.charAt(0) }}
          </div>
          <div>
            <h3 class="text-lg font-semibold text-neutral-800 dark:text-neutral-100">{{ detailTenant.name }}</h3>
            <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-0.5">{{ detailTenant.description }}</p>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">Schema</p>
            <code class="text-sm text-neutral-700 dark:text-neutral-300 font-mono">{{ detailTenant.schema }}</code>
          </div>
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">状态</p>
            <span
              :class="[
                'inline-flex items-center gap-1.5 text-sm font-medium',
                detailTenant.status === 'active' ? 'text-green-600 dark:text-green-400' : 'text-neutral-500 dark:text-neutral-400',
              ]"
            >
              <span :class="['w-2 h-2 rounded-full', detailTenant.status === 'active' ? 'bg-green-500' : 'bg-neutral-400']" />
              {{ detailTenant.status === 'active' ? '活跃' : '停用' }}
            </span>
          </div>
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">联系人</p>
            <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ detailTenant.contact }}</p>
          </div>
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">邮箱</p>
            <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ detailTenant.email }}</p>
          </div>
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">创建时间</p>
            <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ detailTenant.createdAt }}</p>
          </div>
          <div class="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mb-1">API 调用量</p>
            <p class="text-sm text-neutral-700 dark:text-neutral-300 font-medium">{{ formatNumber(detailTenant.apiCalls) }}</p>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  getTenants,
  createTenant as createTenantApi,
  updateTenant as updateTenantApi,
} from '@/api/tenant'

// ============ 数据 ============

interface TenantQuota {
  agentLimit: number
  apiLimit: number
  tokenLimit: number
}

interface Tenant {
  id: string
  name: string
  schema: string
  description: string
  contact: string
  email: string
  status: 'active' | 'inactive'
  color: string
  agentCount: number
  apiCalls: number
  tokenUsed: number
  quota: TenantQuota
  createdAt: string
}

const tenants = ref<Tenant[]>([])
const loading = ref(false)

async function fetchTenants() {
  loading.value = true
  try {
    const res = await getTenants()
    tenants.value = res.data || res || []
  } catch (e) {
    console.error('获取租户列表失败:', e)
    message.error('获取租户列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchTenants()
})

// ============ 状态 ============

const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = 5
const showModal = ref(false)
const editingTenant = ref<Tenant | null>(null)
const showQuotaModal = ref(false)
const quotaTenant = ref<Tenant | null>(null)
const showDetailModal = ref(false)
const detailTenant = ref<Tenant | null>(null)

const form = ref({
  name: '',
  description: '',
  contact: '',
  email: '',
  quota: { agentLimit: 10, apiLimit: 1000000, tokenLimit: 5000000 },
})

const quotaForm = ref({ agentLimit: 10, apiLimit: 1000000, tokenLimit: 5000000 })

// ============ 计算属性 ============

const filteredTenants = computed(() => {
  let result = tenants.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(t => t.name.toLowerCase().includes(q) || t.schema.toLowerCase().includes(q))
  }
  if (statusFilter.value) {
    result = result.filter(t => t.status === statusFilter.value)
  }
  return result
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredTenants.value.length / pageSize)))

const paginatedTenants = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredTenants.value.slice(start, start + pageSize)
})

// ============ 方法 ============

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return String(num)
}

function openCreateModal() {
  editingTenant.value = null
  form.value = { name: '', description: '', contact: '', email: '', quota: { agentLimit: 10, apiLimit: 1000000, tokenLimit: 5000000 } }
  showModal.value = true
}

function openEditModal(tenant: Tenant) {
  editingTenant.value = tenant
  form.value = {
    name: tenant.name,
    description: tenant.description,
    contact: tenant.contact,
    email: tenant.email,
    quota: { ...tenant.quota },
  }
  showModal.value = true
}

function saveTenant() {
  if (!form.value.name || !form.value.contact || !form.value.email) {
    message.warning('请填写必填字段')
    return
  }
  if (editingTenant.value) {
    // 编辑
    updateTenantApi(Number(editingTenant.value.id), form.value)
      .then(() => {
        message.success('租户信息已更新')
        showModal.value = false
        fetchTenants()
      })
      .catch((e: any) => {
        console.error('更新租户失败:', e)
        message.error('更新租户失败')
      })
  } else {
    // 新建
    createTenantApi(form.value)
      .then(() => {
        message.success('租户创建成功')
        showModal.value = false
        fetchTenants()
      })
      .catch((e: any) => {
        console.error('创建租户失败:', e)
        message.error('创建租户失败')
      })
  }
}

function toggleTenantStatus(tenant: Tenant) {
  const action = tenant.status === 'active' ? '停用' : '启用'
  Modal.confirm({
    title: `确认${action}`,
    content: `确定要${action}租户「${tenant.name}」吗？`,
    okText: action,
    okType: tenant.status === 'active' ? 'danger' : 'primary',
    cancelText: '取消',
    onOk() {
      updateTenantApi(Number(tenant.id), { status: tenant.status === 'active' ? 'inactive' : 'active' })
        .then(() => {
          message.success(`租户已${action}`)
          fetchTenants()
        })
        .catch((e: any) => {
          console.error(`${action}租户失败:`, e)
          message.error(`${action}租户失败`)
        })
    },
  })
}

function showDetail(tenant: Tenant) {
  detailTenant.value = tenant
  showDetailModal.value = true
}

function openQuotaModal(tenant: Tenant) {
  quotaTenant.value = tenant
  quotaForm.value = { ...tenant.quota }
  showQuotaModal.value = true
}

function saveQuota() {
  if (!quotaTenant.value) return
  updateTenantApi(Number(quotaTenant.value.id), { quota: quotaForm.value })
    .then(() => {
      message.success('配额已更新')
      showQuotaModal.value = false
      fetchTenants()
    })
    .catch((e: any) => {
      console.error('更新配额失败:', e)
      message.error('更新配额失败')
    })
}


</script>
