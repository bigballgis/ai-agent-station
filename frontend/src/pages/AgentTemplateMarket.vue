<template>
  <div class="template-market-page">
    <!-- 页面头部 -->
    <div class="mb-8 animate-fade-in">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-neutral-900 dark:text-neutral-50 tracking-tight">
            {{ t('tplMarket.title') }}
          </h1>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            {{ t('tplMarket.subtitle') }}
          </p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
          @click="showSaveModal = true"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
          </svg>
          {{ t('tplMarket.saveAsTemplateBtn') }}
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
          :placeholder="t('tplMarket.searchPlaceholder')"
          class="w-full pl-10 pr-4 py-2.5 rounded-xl text-sm bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200"
          @input="handleSearch"
        />
      </div>
      <div class="flex items-center gap-1.5 bg-neutral-100 dark:bg-neutral-800/60 rounded-xl p-1">
        <button
          v-for="cat in categories"
          :key="cat.value"
          class="px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer"
          :class="activeCategory === cat.value
            ? 'bg-white dark:bg-neutral-700 text-neutral-900 dark:text-neutral-100 shadow-sm'
            : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300'"
          @click="activeCategory = cat.value; handleSearch()"
        >
          {{ cat.label }}
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div
      v-if="filteredTemplates.length === 0"
      class="flex flex-col items-center justify-center py-20 animate-fade-in"
    >
      <div class="w-20 h-20 rounded-2xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center mb-5">
        <svg class="w-10 h-10 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
        </svg>
      </div>
      <h3 class="text-base font-semibold text-neutral-700 dark:text-neutral-300 mb-1">{{ t('tplMarket.noMatch') }}</h3>
      <p class="text-sm text-neutral-400 dark:text-neutral-500">{{ t('tplMarket.adjustSearch') }}</p>
    </div>

    <!-- 模板卡片网格 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
      <div
        v-for="(tpl, index) in filteredTemplates"
        :key="tpl.id"
        class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card overflow-hidden hover:-translate-y-1 hover:shadow-float transition-all duration-200 cursor-pointer group animate-slide-up"
        :style="{ animationDelay: `${index * 60}ms` }"
      >
        <!-- 模板预览缩略图 -->
        <div
          class="h-36 flex items-center justify-center relative"
          :class="tpl.gradient"
        >
          <div class="w-16 h-16 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
            <component :is="tpl.icon" class="text-3xl text-white/90" />
          </div>
          <span
            class="absolute top-3 right-3 inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-white/20 text-white backdrop-blur-sm"
          >
            {{ tpl.category }}
          </span>
        </div>

        <!-- 卡片内容 -->
        <div class="p-5">
          <h3 class="text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-1 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors duration-200">
            {{ tpl.name }}
          </h3>
          <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-4 line-clamp-2 leading-relaxed">
            {{ tpl.description }}
          </p>

          <!-- 创建者 + 使用次数 + 评分 -->
          <div class="flex items-center gap-4 mb-4">
            <div class="flex items-center gap-1.5">
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center">
                <span class="text-[10px] text-white font-medium">{{ tpl.creator.charAt(0) }}</span>
              </div>
              <span class="text-xs text-neutral-500 dark:text-neutral-400">{{ tpl.creator }}</span>
            </div>
            <div class="flex items-center gap-1 text-xs text-neutral-400 dark:text-neutral-500">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              {{ tpl.usageCount }}
            </div>
            <div class="flex items-center gap-0.5">
              <svg
                v-for="star in 5"
                :key="star"
                class="w-3.5 h-3.5"
                :class="star <= tpl.rating ? 'text-amber-400' : 'text-neutral-200 dark:text-neutral-700'"
                fill="currentColor" viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
              <span class="text-xs text-neutral-400 dark:text-neutral-500 ml-1">{{ tpl.rating }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="border-t border-neutral-100 dark:border-neutral-800 pt-4 flex items-center gap-2">
            <button
              class="flex-1 inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium text-white bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
              @click.stop="useTemplate(tpl)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ t('tplMarket.useTemplateBtn') }}
            </button>
            <button
              class="p-2 rounded-xl text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
              :title="t('tplMarket.previewTitle')"
              @click.stop="previewTemplate(tpl)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
            <button
              class="p-2 rounded-xl transition-all duration-200 cursor-pointer"
              :class="tpl.favorited
                ? 'text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30'
                : 'text-neutral-400 dark:text-neutral-500 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30'"
              :title="t('tplMarket.favoriteTitle')"
              @click.stop="toggleFavorite(tpl)"
            >
              <svg
                class="w-4 h-4"
                :fill="tpl.favorited ? 'currentColor' : 'none'"
                stroke="currentColor" viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 模板预览弹窗 -->
    <a-modal
      v-model:open="showPreviewModal"
      :title="previewTemplateData?.name || t('tplMarket.preview')"
      :footer="null"
      width="640px"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
    >
      <div v-if="previewTemplateData" class="mt-4">
        <!-- 预览头部 -->
        <div
          class="h-40 rounded-xl flex items-center justify-center mb-6"
          :class="previewTemplateData.gradient"
        >
          <div class="w-20 h-20 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
            <component :is="previewTemplateData.icon" class="text-4xl text-white/90" />
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="space-y-4 mb-6">
          <div>
            <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('tplMarket.templateName') }}</h3>
            <p class="text-base font-semibold text-neutral-900 dark:text-neutral-50">{{ previewTemplateData.name }}</p>
          </div>
          <div>
            <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('common.description') }}</h3>
            <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed">{{ previewTemplateData.description }}</p>
          </div>
          <div class="flex items-center gap-6">
            <div>
              <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('common.category') }}</h3>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-medium bg-primary-50 dark:bg-primary-950/30 text-primary-600 dark:text-primary-400">
                {{ previewTemplateData.category }}
              </span>
            </div>
            <div>
              <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('tplMarket.creator') }}</h3>
              <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ previewTemplateData.creator }}</p>
            </div>
            <div>
              <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-1">{{ t('tplMarket.usageCount') }}</h3>
              <p class="text-sm text-neutral-700 dark:text-neutral-300">{{ previewTemplateData.usageCount }}</p>
            </div>
          </div>
        </div>

        <!-- Agent 图结构概览 -->
        <div class="mb-6">
          <h3 class="text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-3">{{ t('tplMarket.graphOverview') }}</h3>
          <div class="bg-neutral-50 dark:bg-neutral-800/60 rounded-xl p-4">
            <div class="flex items-center gap-3 mb-3">
              <div class="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-900/40 flex items-center justify-center">
                <svg class="w-4 h-4 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <span class="text-sm font-medium text-neutral-700 dark:text-neutral-300">{{ t('tplMarket.mainAgentNode') }}</span>
            </div>
            <div class="ml-4 border-l-2 border-neutral-200 dark:border-neutral-700 pl-4 space-y-2">
              <div
                v-for="(node, idx) in previewTemplateData.nodes"
                :key="idx"
                class="flex items-center gap-2"
              >
                <div class="w-2 h-2 rounded-full" :class="node.color" />
                <span class="text-sm text-neutral-600 dark:text-neutral-400">{{ node.label }}</span>
                <span class="text-xs text-neutral-400 dark:text-neutral-500">({{ node.type }})</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 使用按钮 -->
        <div class="flex justify-end">
          <button
            class="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
            @click="useTemplate(previewTemplateData!); showPreviewModal = false"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ t('tplMarket.useThisTemplate') }}
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 保存为模板弹窗 -->
    <a-modal
      v-model:open="showSaveModal"
      :title="t('tplMarket.saveAsTemplate')"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
      @ok="handleSaveTemplate"
      @cancel="showSaveModal = false"
    >
      <a-form layout="vertical" class="mt-4">
        <a-form-item :label="t('tplMarket.templateName')">
          <a-input v-model:value="newTemplate.name" :placeholder="t('tplMarket.templateNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('common.description')">
          <a-textarea
            v-model:value="newTemplate.description"
            :placeholder="t('tplMarket.templateDescPlaceholder')"
            :rows="3"
          />
        </a-form-item>
        <a-form-item :label="t('common.category')">
          <a-select v-model:value="newTemplate.category" :placeholder="t('tplMarket.selectCategory')">
            <a-select-option value="customer_service">{{ t('tplMarket.customerService') }}</a-select-option>
            <a-select-option value="data_analysis">{{ t('tplMarket.dataAnalysis') }}</a-select-option>
            <a-select-option value="code_generation">{{ t('tplMarket.codeGeneration') }}</a-select-option>
            <a-select-option value="document_processing">{{ t('tplMarket.documentProcessing') }}</a-select-option>
            <a-select-option value="automation">{{ t('tplMarket.automation') }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  RobotOutlined,
} from '@ant-design/icons-vue'
import { agentApi } from '@/api/agent'
import { logger } from '@/utils/logger'

const { t } = useI18n()
const router = useRouter()

// 分类
const categories = [
  { label: t('tplMarket.all'), value: '' },
  { label: t('tplMarket.customerService'), value: 'customer_service' },
  { label: t('tplMarket.dataAnalysis'), value: 'data_analysis' },
  { label: t('tplMarket.codeGeneration'), value: 'code_generation' },
  { label: t('tplMarket.documentProcessing'), value: 'document_processing' },
  { label: t('tplMarket.automation'), value: 'automation' },
]

const activeCategory = ref('')
const searchQuery = ref('')

// 模板数据（从 API 获取）
const templates = ref<any[]>([])
const loading = ref(false)

async function fetchTemplates() {
  loading.value = true
  try {
    const res = await agentApi.getAllAgents()
    const agents = res.data || res || []
interface AgentTemplate {
  id: number
  name: string
  isTemplate?: boolean
  type?: string
  category?: string
  creator?: string
  usageCount?: number
  rating?: number
  [key: string]: unknown
}

    templates.value = agents
      .filter((a: AgentTemplate) => a.isTemplate || a.type === 'template')
      .map((a: AgentTemplate) => ({
        ...a,
        category: a.category || t('tplMarket.defaultCategory'),
        creator: a.creator || t('tplMarket.defaultCreator'),
        usageCount: a.usageCount || 0,
        rating: a.rating || 4.0,
        favorited: a.favorited || false,
        gradient: a.gradient || 'bg-gradient-to-br from-blue-500 to-cyan-400',
        icon: a.icon || RobotOutlined,
        nodes: a.nodes || [],
      }))
  } catch (e) {
    logger.error(t('tplMarket.fetchTemplatesFailed'), e)
    message.error(t('tplMarket.fetchFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchTemplates()
})

// 筛选
const filteredTemplates = computed(() => {
  let result = templates.value
  if (activeCategory.value) {
    result = result.filter(t => t.category === activeCategory.value)
  }
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(t =>
      t.name.toLowerCase().includes(q) || t.description.toLowerCase().includes(q)
    )
  }
  return result
})

function handleSearch() {
  // 筛选由 computed 自动处理
}

// 预览
const showPreviewModal = ref(false)
const previewTemplateData = ref<typeof templates.value[0] | null>(null)

function previewTemplate(tpl: typeof templates.value[0]) {
  previewTemplateData.value = tpl
  showPreviewModal.value = true
}

// 使用模板
async function useTemplate(tpl: typeof templates.value[0]) {
  try {
    await agentApi.createAgent({
      name: tpl.name,
      description: tpl.description,
      config: tpl.config || tpl.graphDefinition || {},
    })
    message.success(`${t('tplMarket.useSuccess')} "${tpl.name}"`)
    router.push('/agents')
  } catch (e) {
    logger.error(t('tplMarket.useTemplateFailed'), e)
    message.error(t('tplMarket.useFailed'))
  }
}

// 收藏
function toggleFavorite(tpl: typeof templates.value[0]) {
  tpl.favorited = !tpl.favorited
  message.success(tpl.favorited ? t('tplMarket.favorited') : t('tplMarket.unfavorited'))
}

// 保存为模板
const showSaveModal = ref(false)
const newTemplate = ref({ name: '', description: '', category: undefined as string | undefined })

async function handleSaveTemplate() {
  if (!newTemplate.value.name) {
    message.error(t('tplMarket.nameRequired'))
    return
  }
  try {
    await agentApi.createAgent({
      ...newTemplate.value,
      isTemplate: true,
      config: {},
    })
    message.success(t('tplMarket.saveSuccess'))
    showSaveModal.value = false
    newTemplate.value = { name: '', description: '', category: undefined }
    await fetchTemplates()
  } catch (e) {
    logger.error(t('tplMarket.saveTemplateFailed'), e)
    message.error(t('tplMarket.saveFailed'))
  }
}
</script>

<style scoped>
.template-market-page {
  padding: 0;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
