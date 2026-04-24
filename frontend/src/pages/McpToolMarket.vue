<template>
  <div class="mcp-tool-page">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            {{ t('mcpTool.title') }}
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            {{ t('mcpTool.subtitle') }}
          </p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="openConnectModal"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ t('mcpTool.connectToolBtn') }}
        </button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6 animate-slide-up">
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-950/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <div>
            <p class="text-xs text-neutral-400 dark:text-neutral-500">{{ t('mcpTool.totalTools') }}</p>
            <p class="text-xl font-bold text-neutral-800 dark:text-neutral-100">{{ tools.length }}</p>
          </div>
        </div>
      </div>
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-50 dark:bg-green-950/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p class="text-xs text-neutral-400 dark:text-neutral-500">{{ t('mcpTool.enabled') }}</p>
            <p class="text-xl font-bold text-neutral-800 dark:text-neutral-100">{{ enabledCount }}</p>
          </div>
        </div>
      </div>
      <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-purple-50 dark:bg-purple-950/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          </div>
          <div>
            <p class="text-xs text-neutral-400 dark:text-neutral-500">{{ t('mcpTool.avgSuccessRate') }}</p>
            <p class="text-xl font-bold text-neutral-800 dark:text-neutral-100">{{ avgSuccessRate }}%</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 工具卡片网格 -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5 animate-slide-up">
      <div
        v-for="tool in tools"
        :key="tool.id"
        class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card border border-neutral-100 dark:border-neutral-800 p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300 group"
      >
        <!-- 头部: 图标 + 名称 + 状态 -->
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-3">
            <div
              class="w-11 h-11 rounded-xl flex items-center justify-center text-lg flex-shrink-0"
              :style="{ background: tool.iconBg }"
            >
              {{ tool.icon }}
            </div>
            <div>
              <h3 class="text-sm font-semibold text-neutral-800 dark:text-neutral-100">{{ tool.name }}</h3>
              <span
                :class="[
                  'inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-medium mt-0.5',
                  tool.type === 'HTTP' ? 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400' : 'bg-purple-50 dark:bg-purple-950/30 text-purple-600 dark:text-purple-400',
                ]"
              >
                {{ tool.type }}
              </span>
            </div>
          </div>
          <span
            :class="[
              'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium',
              tool.enabled
                ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'
                : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400',
            ]"
          >
            <span :class="['w-1.5 h-1.5 rounded-full', tool.enabled ? 'bg-green-500' : 'bg-neutral-400']" />
            {{ tool.enabled ? t('mcpTool.enabled') : t('mcpTool.disabled') }}
          </span>
        </div>

        <!-- 描述 -->
        <p class="text-xs text-neutral-500 dark:text-neutral-400 line-clamp-2 mb-4 leading-relaxed">
          {{ tool.description }}
        </p>

        <!-- 统计指标 -->
        <div class="flex items-center gap-4 mb-4 py-3 border-t border-neutral-100 dark:border-neutral-800">
          <div class="flex items-center gap-1.5">
            <svg class="w-3.5 h-3.5 text-neutral-400 dark:text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
            <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ formatNumber(tool.callCount) }} {{ t('mcpTool.calls') }}</span>
          </div>
          <div class="flex items-center gap-1.5">
            <svg class="w-3.5 h-3.5 text-neutral-400 dark:text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <span
              :class="[
                'text-xs font-medium',
                tool.successRate >= 99 ? 'text-green-600 dark:text-green-400' :
                tool.successRate >= 95 ? 'text-amber-600 dark:text-amber-400' :
                'text-red-600 dark:text-red-400',
              ]"
            >
              {{ tool.successRate }}%
            </span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex items-center gap-2">
          <button
            class="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="openConfigModal(tool)"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {{ t('mcpTool.configBtn') }}
          </button>
          <button
            :class="[
              'flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-colors duration-200 cursor-pointer',
              tool.enabled
                ? 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/30 hover:bg-amber-100 dark:hover:bg-amber-950/50'
                : 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-950/30 hover:bg-green-100 dark:hover:bg-green-950/50',
            ]"
            @click="toggleTool(tool)"
          >
            <svg v-if="tool.enabled" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
            </svg>
            <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ tool.enabled ? t('mcpTool.disable') : t('mcpTool.enable') }}
          </button>
          <button
            class="inline-flex items-center justify-center p-2 rounded-xl text-neutral-400 dark:text-neutral-500 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors duration-200 cursor-pointer"
            title="t('common.delete')"
            @click="handleDeleteTool(tool)"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 接入工具弹窗 -->
    <a-modal
      v-model:open="showConnectModal"
      :title="t('mcpTool.connectTool')"
      :footer="null"
      :width="520"
      centered
    >
      <div class="space-y-4 pt-2">
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.toolName') }} <span class="text-red-500">*</span></label>
          <input
            v-model="connectForm.name"
            type="text"
            :placeholder="t('mcpTool.toolNamePlaceholder')"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('common.type') }} <span class="text-red-500">*</span></label>
          <select
            v-model="connectForm.type"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="HTTP">HTTP</option>
            <option value="custom">{{ t('common.custom') }}</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.serviceUrl') }} <span class="text-red-500">*</span></label>
          <input
            v-model="connectForm.url"
            type="text"
            placeholder="https://api.example.com/mcp"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 font-mono"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.authType') }}</label>
          <select
            v-model="connectForm.authType"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="none">{{ t('mcpTool.noAuth') }}</option>
            <option value="api_key">API Key</option>
            <option value="bearer">Bearer Token</option>
          </select>
        </div>
        <div v-if="connectForm.authType !== 'none'">
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">
            {{ connectForm.authType === 'api_key' ? 'API Key' : 'Bearer Token' }}
          </label>
          <input
            v-model="connectForm.authValue"
            :type="connectForm.authType === 'api_key' ? 'text' : 'password'"
            :placeholder="connectForm.authType === 'api_key' ? t('mcpTool.inputApiKey') : t('mcpTool.inputBearerToken')"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 font-mono"
          />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.timeoutSetting') }}</label>
            <input
              v-model.number="connectForm.timeout"
              type="number"
              min="1000"
              step="1000"
              class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
            />
          </div>
          <div />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('common.description') }}</label>
          <textarea
            v-model="connectForm.description"
            rows="2"
            :placeholder="t('mcpTool.toolDescPlaceholder')"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 resize-none"
          />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="showConnectModal = false"
          >
            {{ t('mcpTool.cancel') }}
          </button>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
            @click="connectTool"
          >
            {{ t('mcpTool.connectBtn') }}
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 配置弹窗 -->
    <a-modal
      v-model:open="showConfigModal"
      :title="t('mcpTool.toolConfig')"
      :footer="null"
      :width="480"
      centered
    >
      <div v-if="configTool" class="space-y-4 pt-2">
        <div class="flex items-center gap-3 p-4 rounded-xl bg-neutral-50 dark:bg-neutral-800 border border-neutral-100 dark:border-neutral-700">
          <div
            class="w-11 h-11 rounded-xl flex items-center justify-center text-lg flex-shrink-0"
            :style="{ background: configTool.iconBg }"
          >
            {{ configTool.icon }}
          </div>
          <div>
            <p class="text-sm font-semibold text-neutral-800 dark:text-neutral-100">{{ configTool.name }}</p>
            <p class="text-xs text-neutral-400 dark:text-neutral-500 mt-0.5">{{ configTool.description }}</p>
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.serviceAddress') }}</label>
          <input
            v-model="configForm.url"
            type="text"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 font-mono"
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.authType') }}</label>
          <select
            v-model="configForm.authType"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="none">None (无需鉴权)</option>
            <option value="api_key">API Key</option>
            <option value="bearer">Bearer Token</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1.5">{{ t('mcpTool.timeoutSetting') }}</label>
          <input
            v-model.number="configForm.timeout"
            type="number"
            min="1000"
            step="1000"
            class="w-full px-3.5 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          />
        </div>

        <div class="flex justify-end gap-3 pt-2">
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-neutral-600 dark:text-neutral-300 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors duration-200 cursor-pointer"
            @click="showConfigModal = false"
          >
            {{ t('mcpTool.cancel') }}
          </button>
          <button
            class="px-4 py-2 rounded-xl text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 transition-colors duration-200 cursor-pointer"
            @click="saveConfig"
          >
            {{ t('mcpTool.saveBtn') }}
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getTools, refreshTools } from '@/api/tool'
import { logger } from '@/utils/logger'

const { t } = useI18n()

// ============ 数据 ============

interface McpTool {
  id: string
  name: string
  type: 'HTTP' | '自定义'
  icon: string
  iconBg: string
  description: string
  enabled: boolean
  callCount: number
  successRate: number
  url: string
  authType: string
  timeout: number
}

const tools = ref<McpTool[]>([])
const loading = ref(false)

async function fetchTools() {
  loading.value = true
  try {
    const res = await getTools()
    tools.value = res.data || res || []
  } catch (e) {
    logger.error('Failed to fetch tools:', e)
    message.error(t('mcpTool.fetchFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchTools()
})

// ============ 状态 ============

const showConnectModal = ref(false)
const showConfigModal = ref(false)
const configTool = ref<McpTool | null>(null)

const connectForm = ref({
  name: '',
  type: 'HTTP' as 'HTTP' | '自定义',
  url: '',
  authType: 'none',
  authValue: '',
  timeout: 5000,
  description: '',
})

const configForm = ref({
  url: '',
  authType: 'none',
  timeout: 5000,
})

// ============ 计算属性 ============

const enabledCount = computed(() => tools.value.filter(t => t.enabled).length)

const avgSuccessRate = computed(() => {
  if (tools.value.length === 0) return 0
  const sum = tools.value.reduce((acc, t) => acc + t.successRate, 0)
  return (sum / tools.value.length).toFixed(1)
})

// ============ 方法 ============

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return String(num)
}

function openConnectModal() {
  connectForm.value = { name: '', type: 'HTTP', url: '', authType: 'none', authValue: '', timeout: 5000, description: '' }
  showConnectModal.value = true
}

function connectTool() {
  if (!connectForm.value.name || !connectForm.value.url) {
    message.warning(t('mcpTool.nameAndUrlRequired'))
    return
  }
  showConnectModal.value = false
  message.success(t('mcpTool.connectSubmitted'))
  refreshTools().then(() => {
    fetchTools()
  }).catch((e: Error) => {
    logger.error('刷新工具失败:', e)
    message.error(t('mcpTool.connectFailed'))
  })
}

function openConfigModal(tool: McpTool) {
  configTool.value = tool
  configForm.value = {
    url: tool.url,
    authType: tool.authType,
    timeout: tool.timeout,
  }
  showConfigModal.value = true
}

function saveConfig() {
  if (!configTool.value) return
  showConfigModal.value = false
  message.success(t('mcpTool.configSaved'))
  refreshTools().then(() => {
    fetchTools()
  }).catch((e: Error) => {
    logger.error('刷新工具失败:', e)
    message.error(t('mcpTool.configSaveFailed'))
  })
}

function toggleTool(tool: McpTool) {
  message.success(tool.enabled ? `正在禁用「${tool.name}」...` : `正在启用「${tool.name}」...`)
  refreshTools().then(() => {
    fetchTools()
  }).catch((e: Error) => {
    logger.error('切换工具状态失败:', e)
    message.error(t('mcpTool.toggleToolFailed'))
  })
}

function handleDeleteTool(_tool: McpTool) {
  message.info(t('mcpTool.deleteViaBackend'))
}


</script>
