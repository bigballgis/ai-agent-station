<template>
  <div class="page-header">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3 min-w-0">
        <!-- 返回按钮 -->
        <a-button
          v-if="showBack"
          type="text"
          class="back-btn"
          @click="handleBack"
        >
          <template #icon>
            <ArrowLeftOutlined />
          </template>
        </a-button>

        <div class="min-w-0">
          <!-- 面包屑导航 -->
          <div v-if="breadcrumbs && breadcrumbs.length > 0" class="mb-1">
            <a-breadcrumb>
              <a-breadcrumb-item v-for="(crumb, index) in breadcrumbs" :key="index">
                <router-link
                  v-if="crumb.path && index < breadcrumbs.length - 1"
                  :to="crumb.path"
                  class="breadcrumb-link"
                >
                  {{ crumb.title }}
                </router-link>
                <span v-else class="breadcrumb-current">{{ crumb.title }}</span>
              </a-breadcrumb-item>
            </a-breadcrumb>
          </div>

          <!-- 标题 -->
          <h1 class="page-title">
            {{ title }}
          </h1>

          <!-- 副标题 -->
          <p v-if="subtitle" class="page-subtitle">
            {{ subtitle }}
          </p>
        </div>
      </div>

      <!-- 右侧操作区 -->
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'

/**
 * PageHeader 组件
 * 统一的页面头部，包含标题、副标题、面包屑和操作按钮
 * 替代各页面重复的头部区域
 */

export interface BreadcrumbItem {
  /** 面包屑标题 */
  title: string
  /** 面包屑路径（最后一项不需要） */
  path?: string
}

interface Props {
  /** 页面标题 */
  title: string
  /** 副标题/描述 */
  subtitle?: string
  /** 面包屑导航项 */
  breadcrumbs?: BreadcrumbItem[]
  /** 是否显示返回按钮 */
  showBack?: boolean
}

defineProps<Props>()

const router = useRouter()

/** 返回上一页 */
function handleBack() {
  router.back()
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-out;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #171717;
  letter-spacing: -0.025em;
  line-height: 1.2;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #737373;
  margin-top: 4px;
  line-height: 1.5;
}

.header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  flex-shrink: 0;
}

.breadcrumb-link {
  color: #a3a3a3;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #3b82f6;
}

.breadcrumb-current {
  color: #737373;
}

/* 暗色模式 */
:global(.dark) .page-title {
  color: #fafafa;
}

:global(.dark) .page-subtitle {
  color: #a3a3a3;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
