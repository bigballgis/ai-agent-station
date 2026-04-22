<template>
  <div class="breadcrumb-nav">
    <a-breadcrumb class="breadcrumb-list">
      <!-- 首页 -->
      <a-breadcrumb-item>
        <router-link to="/dashboard" class="breadcrumb-link">
          <HomeOutlined class="breadcrumb-icon" />
        </router-link>
      </a-breadcrumb-item>

      <!-- 自动生成的面包屑 -->
      <a-breadcrumb-item v-for="(crumb, index) in breadcrumbItems" :key="index">
        <router-link
          v-if="crumb.path && index < breadcrumbItems.length - 1"
          :to="crumb.path"
          class="breadcrumb-link"
        >
          <component v-if="crumb.icon" :is="crumb.icon" class="breadcrumb-icon" />
          {{ crumb.title }}
        </router-link>
        <span v-else class="breadcrumb-current">
          <component v-if="crumb.icon" :is="crumb.icon" class="breadcrumb-icon" />
          {{ crumb.title }}
        </span>
      </a-breadcrumb-item>
    </a-breadcrumb>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeOutlined } from '@ant-design/icons-vue'
import type { Component } from 'vue'

/**
 * BreadcrumbNav 组件
 * 自动面包屑导航组件
 * 根据当前路由自动生成面包屑
 * 支持自定义面包屑项和图标
 */

// 面包屑项类型
export interface BreadcrumbItem {
  /** 标题 */
  title: string
  /** 路径（最后一项不需要） */
  path?: string
  /** 图标组件 */
  icon?: Component
}

interface Props {
  /** 自定义面包屑项（优先于自动生成） */
  items?: BreadcrumbItem[]
}

const props = defineProps<Props>()

const route = useRoute()

// 路由 meta 标题映射（补充中文标题）
const titleMap: Record<string, string> = {
  'Dashboard': '仪表盘',
  'AgentList': 'Agent 管理',
  'AgentEdit': '编辑 Agent',
  'AgentVersions': '版本管理',
  'AgentDesign': 'Agent 设计',
  'McpToolMarket': 'MCP 工具市场',
  'Approval': '审批管理',
  'AgentTemplateMarket': 'Agent 模板市场',
  'AgentDebugger': '在线调试',
  'MemoryManagement': '记忆管理',
  'Deployment': '发布管理',
  'ApiManagement': 'API 管理',
  'ApiDocumentation': 'API 文档',
  'TenantManagement': '租户管理',
  'Permission': '权限管理',
  'I18n': '国际化设置',
  'Log': '日志中心',
  'AlertNotification': '告警中心',
  'QuotaManagement': '配额管理',
  'FileManagement': '文件管理',
  'TestCaseList': '测试用例管理',
  'TestCaseEdit': '编辑测试用例',
  'TestCaseVersions': '用例版本管理',
  'TestExecutionList': '测试执行管理',
  'TestResultList': '测试结果管理',
  'TestResultDetail': '测试结果详情',
  'SuggestionList': '优化建议管理',
  'Evolution': 'Agent 自进化',
}

// 自动生成面包屑
const autoBreadcrumbs = computed<BreadcrumbItem[]>(() => {
  const crumbs: BreadcrumbItem[] = []
  const matched = route.matched.filter(r => r.meta?.title)

  matched.forEach((r, i) => {
    const isLast = i === matched.length - 1
    const title = titleMap[r.meta.title as string] || (r.meta.title as string) || ''
    crumbs.push({
      title,
      path: isLast ? undefined : r.path,
    })
  })

  return crumbs
})

// 最终面包屑列表（优先使用自定义项）
const breadcrumbItems = computed(() => {
  if (props.items && props.items.length > 0) {
    return props.items
  }
  return autoBreadcrumbs.value
})
</script>

<style scoped>
.breadcrumb-nav {
  display: flex;
  align-items: center;
}

.breadcrumb-list {
  line-height: 1;
}

.breadcrumb-link {
  color: #a3a3a3;
  transition: color 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.breadcrumb-link:hover {
  color: #3b82f6;
}

.breadcrumb-current {
  color: #525252;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.breadcrumb-icon {
  font-size: 12px;
}

/* 覆盖 Ant Design 分隔符样式 */
:deep(.ant-breadcrumb-separator) {
  color: #d4d4d4 !important;
  margin: 0 6px !important;
  font-size: 10px !important;
}

/* 暗色模式 */
:global(.dark) .breadcrumb-link {
  color: #525252;
}

:global(.dark) .breadcrumb-link:hover {
  color: #60a5fa;
}

:global(.dark) .breadcrumb-current {
  color: #d4d4d4;
}

:global(.dark) :deep(.ant-breadcrumb-separator) {
  color: #404040 !important;
}
</style>
