<template>
  <div class="alert-notification-page">
    <!-- 页面头部 -->
    <PageHeader title="告警中心" subtitle="统一管理和监控所有告警规则与告警记录，及时发现和处理系统异常" />

    <!-- 统计概览卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
      <StatCard
        v-for="(stat, index) in alertStatCards"
        :key="stat.title"
        :title="stat.title"
        :value="stat.value"
        :icon="stat.icon"
        :trend="stat.trend"
        :trend-value="stat.trendValue"
        :color="stat.color"
        class="animate-slide-up"
        :style="{ animationDelay: `${index * 80}ms` }"
      />
    </div>

    <!-- Tab 切换 -->
    <div class="bg-white dark:bg-neutral-900 rounded-2xl shadow-card overflow-hidden animate-slide-up" style="animation-delay: 200ms;">
      <div class="border-b border-neutral-100 dark:border-neutral-800 px-6">
        <div class="flex items-center gap-1">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-4 py-3.5 text-sm font-medium transition-all duration-200 cursor-pointer relative"
            :class="activeTab === tab.key
              ? 'text-primary-600 dark:text-primary-400'
              : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300'"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span
              v-if="tab.count !== undefined"
              class="ml-1.5 inline-flex items-center justify-center w-5 h-5 rounded-full text-xs font-medium"
              :class="activeTab === tab.key
                ? 'bg-primary-100 dark:bg-primary-900/40 text-primary-600 dark:text-primary-400'
                : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400'"
            >
              {{ tab.count }}
            </span>
            <div
              v-if="activeTab === tab.key"
              class="absolute bottom-0 left-2 right-2 h-0.5 rounded-full bg-primary-500"
            />
          </button>
        </div>
      </div>

      <!-- 告警规则 Tab -->
      <div v-if="activeTab === 'rules'" class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-base font-semibold text-neutral-800 dark:text-neutral-200">告警规则列表</h3>
          <button
            class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-white text-sm font-medium bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer"
            @click="showCreateRuleModal = true"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            新建规则
          </button>
        </div>

        <div class="bg-white dark:bg-neutral-900 rounded-xl border border-neutral-100 dark:border-neutral-800 overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/60">
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">规则名称</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">类型</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">阈值</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">严重级别</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">状态</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="rule in alertRules"
                :key="rule.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 last:border-0 hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="px-4 py-3 font-medium text-neutral-800 dark:text-neutral-200">{{ rule.name }}</td>
                <td class="px-4 py-3">
                  <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400">
                    {{ rule.type }}
                  </span>
                </td>
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 font-mono text-xs">{{ rule.threshold }}</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="getSeverityBadgeClass(rule.severity)"
                  >
                    {{ rule.severity }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium cursor-pointer transition-colors"
                    :class="rule.enabled
                      ? 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'
                      : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400'"
                    @click="toggleRule(rule)"
                  >
                    <span class="w-1.5 h-1.5 rounded-full" :class="rule.enabled ? 'bg-green-500' : 'bg-neutral-400'" />
                    {{ rule.enabled ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
                      title="编辑"
                      @click="editRule(rule)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
                      title="删除"
                      @click="deleteRule(rule)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 告警记录 Tab -->
      <div v-if="activeTab === 'records'" class="p-6">
        <!-- 筛选栏 -->
        <div class="flex flex-wrap items-center gap-3 mb-4">
          <select
            v-model="recordFilter.timeRange"
            class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="1h">最近 1 小时</option>
            <option value="24h">最近 24 小时</option>
            <option value="7d">最近 7 天</option>
            <option value="30d">最近 30 天</option>
          </select>
          <select
            v-model="recordFilter.severity"
            class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="">全部级别</option>
            <option value="critical">严重</option>
            <option value="warning">警告</option>
            <option value="info">信息</option>
          </select>
          <select
            v-model="recordFilter.status"
            class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
          >
            <option value="">全部状态</option>
            <option value="firing">触发中</option>
            <option value="resolved">已解决</option>
          </select>
        </div>

        <div class="bg-white dark:bg-neutral-900 rounded-xl border border-neutral-100 dark:border-neutral-800 overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/60">
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">触发时间</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">规则名称</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">严重级别</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">指标值</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">状态</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="record in filteredRecords"
                :key="record.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 last:border-0 hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 text-xs whitespace-nowrap">{{ record.triggeredAt }}</td>
                <td class="px-4 py-3 font-medium text-neutral-800 dark:text-neutral-200">{{ record.ruleName }}</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="getSeverityBadgeClass(record.severity)"
                  >
                    {{ getSeverityLabel(record.severity) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 font-mono text-xs">{{ record.metricValue }}</td>
                <td class="px-4 py-3">
                  <StatusBadge :status="record.status" type="alert" :dot="true" />
                </td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      v-if="record.status === 'firing'"
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-950/30 transition-all duration-200 cursor-pointer"
                      title="确认解决"
                      @click="resolveRecord(record)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
                      title="查看详情"
                      @click="viewRecordDetail(record)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 新建规则弹窗 -->
    <a-modal
      v-model:open="showCreateRuleModal"
      title="新建告警规则"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
      @ok="handleCreateRule"
      @cancel="showCreateRuleModal = false"
    >
      <a-form layout="vertical" class="mt-4">
        <a-form-item label="规则名称">
          <a-input v-model:value="newRule.name" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="告警类型">
          <a-select v-model:value="newRule.type" placeholder="请选择类型">
            <a-select-option value="API 错误率">API 错误率</a-select-option>
            <a-select-option value="响应时间">响应时间</a-select-option>
            <a-select-option value="Token 使用量">Token 使用量</a-select-option>
            <a-select-option value="CPU 使用率">CPU 使用率</a-select-option>
            <a-select-option value="内存使用率">内存使用率</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="阈值">
          <a-input v-model:value="newRule.threshold" placeholder="例如: > 95%" />
        </a-form-item>
        <a-form-item label="严重级别">
          <a-select v-model:value="newRule.severity" placeholder="请选择级别">
            <a-select-option value="critical">严重</a-select-option>
            <a-select-option value="warning">警告</a-select-option>
            <a-select-option value="info">信息</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 告警详情弹窗 -->
    <a-modal
      v-model:open="showDetailModal"
      :title="'告警详情 - ' + detailRecord?.ruleName"
      :footer="null"
      width="560px"
    >
      <div v-if="detailRecord" class="mt-4 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">触发时间</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailRecord.triggeredAt }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">规则名称</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailRecord.ruleName }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">严重级别</h4>
            <span
              class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
              :class="getSeverityBadgeClass(detailRecord.severity)"
            >
              {{ getSeverityLabel(detailRecord.severity) }}
            </span>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">指标值</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200 font-mono">{{ detailRecord.metricValue }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">状态</h4>
            <span
              class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium"
              :class="detailRecord.status === 'firing'
                ? 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400'
                : 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400'"
            >
              {{ detailRecord.status === 'firing' ? '触发中' : '已解决' }}
            </span>
          </div>
        </div>
        <div>
          <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">告警描述</h4>
          <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed">{{ detailRecord.description }}</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  AlertOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { getAlertRules, createAlertRule, updateAlertRule, deleteAlertRule, getAlertRecords, getAlertStats } from '@/api/alert'
import { PageHeader, StatCard, StatusBadge, ConfirmModal } from '@/components'

// Tab
const tabs = ref([
  { key: 'rules', label: '告警规则', count: 0 },
  { key: 'records', label: '告警记录', count: 0 },
])
const activeTab = ref('rules')

// 统计卡片
const stats = ref([
  {
    label: '活跃告警数',
    value: 0,
    trend: '-',
    iconBg: 'bg-red-100 dark:bg-red-900/40',
    iconColor: 'text-red-600 dark:text-red-400',
    trendBg: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
    icon: AlertOutlined,
  },
  {
    label: '24h 告警总数',
    value: 0,
    trend: '-',
    iconBg: 'bg-orange-100 dark:bg-orange-900/40',
    iconColor: 'text-orange-600 dark:text-orange-400',
    trendBg: 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400',
    icon: WarningOutlined,
  },
  {
    label: '已解决告警数',
    value: 0,
    trend: '-',
    iconBg: 'bg-green-100 dark:bg-green-900/40',
    iconColor: 'text-green-600 dark:text-green-400',
    trendBg: 'bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400',
    icon: CheckCircleOutlined,
  },
  {
    label: '告警规则数',
    value: 0,
    trend: '-',
    iconBg: 'bg-blue-100 dark:bg-blue-900/40',
    iconColor: 'text-blue-600 dark:text-blue-400',
    trendBg: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    icon: SettingOutlined,
  },
])

// StatCard 配置（响应式）
const alertStatCards = computed(() => [
  {
    title: stats.value[0].label,
    value: stats.value[0].value,
    icon: stats.value[0].icon,
    trend: 'none' as const,
    trendValue: stats.value[0].trend,
    color: 'red' as const,
  },
  {
    title: stats.value[1].label,
    value: stats.value[1].value,
    icon: stats.value[1].icon,
    trend: 'none' as const,
    trendValue: stats.value[1].trend,
    color: 'orange' as const,
  },
  {
    title: stats.value[2].label,
    value: stats.value[2].value,
    icon: stats.value[2].icon,
    trend: 'none' as const,
    trendValue: stats.value[2].trend,
    color: 'green' as const,
  },
  {
    title: stats.value[3].label,
    value: stats.value[3].value,
    icon: stats.value[3].icon,
    trend: 'none' as const,
    trendValue: stats.value[3].trend,
    color: 'blue' as const,
  },
])

// 告警规则
const alertRules = ref<any[]>([])

// 告警记录
const alertRecords = ref<any[]>([])

async function fetchAlertData() {
  try {
    const [rulesRes, recordsRes, statsRes] = await Promise.all([
      getAlertRules(),
      getAlertRecords({ page: 0, size: 50 }),
      getAlertStats(),
    ])

    alertRules.value = rulesRes.data || rulesRes || []
    alertRecords.value = recordsRes.data?.content || recordsRes.data || recordsRes || []

    // 更新统计卡片
    if (statsRes.data) {
      const s = statsRes.data
      stats.value[0].value = s.activeCount ?? s.active ?? 0
      stats.value[0].trend = s.activeTrend ?? '-'
      stats.value[1].value = s.total24h ?? s.total24Hour ?? 0
      stats.value[1].trend = s.total24hTrend ?? '-'
      stats.value[2].value = s.resolvedCount ?? s.resolved ?? 0
      stats.value[2].trend = s.resolvedRate ?? '-'
      stats.value[3].value = s.ruleCount ?? s.rules ?? 0
      stats.value[3].trend = s.enabledRuleCount != null ? `启用 ${s.enabledRuleCount} 条` : '-'
    }

    // 更新 Tab 计数
    tabs.value[0].count = alertRules.value.length
    tabs.value[1].count = alertRecords.value.length
  } catch (e) {
    console.error('获取告警数据失败:', e)
  }
}

onMounted(async () => {
  await fetchAlertData()
})

// 记录筛选
const recordFilter = ref({
  timeRange: '24h',
  severity: '',
  status: '',
})

const filteredRecords = computed(() => {
  let result = alertRecords.value
  if (recordFilter.value.severity) {
    result = result.filter(r => r.severity === recordFilter.value.severity)
  }
  if (recordFilter.value.status) {
    result = result.filter(r => r.status === recordFilter.value.status)
  }
  return result
})

// 辅助函数
function getSeverityBadgeClass(severity: string): string {
  const map: Record<string, string> = {
    critical: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
    warning: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    info: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
  }
  return map[severity] || map.info
}

function getSeverityLabel(severity: string): string {
  const map: Record<string, string> = {
    critical: '严重',
    warning: '警告',
    info: '信息',
  }
  return map[severity] || severity
}

// 规则操作
async function toggleRule(rule: typeof alertRules.value[0]) {
  try {
    await updateAlertRule(rule.id, { enabled: !rule.enabled })
    message.success(rule.enabled ? '规则已禁用' : '规则已启用')
    await fetchAlertData()
  } catch (e) {
    console.error('切换规则状态失败:', e)
    message.error('操作失败')
  }
}

function editRule(rule: typeof alertRules.value[0]) {
  message.info(`编辑规则: ${rule.name}`)
}

function deleteRule(rule: typeof alertRules.value[0]) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除规则 "${rule.name}" 吗？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteAlertRule(rule.id)
        message.success('删除成功')
        await fetchAlertData()
      } catch (e) {
        console.error('删除规则失败:', e)
        message.error('删除失败')
      }
    },
  })
}

// 记录操作
function resolveRecord(record: typeof alertRecords.value[0]) {
  record.status = 'resolved'
  message.success('告警已标记为已解决')
}

const showDetailModal = ref(false)
const detailRecord = ref<typeof alertRecords.value[0] | null>(null)

function viewRecordDetail(record: typeof alertRecords.value[0]) {
  detailRecord.value = record
  showDetailModal.value = true
}

// 新建规则
const showCreateRuleModal = ref(false)
const newRule = ref({ name: '', type: undefined as string | undefined, threshold: '', severity: undefined as string | undefined })

async function handleCreateRule() {
  if (!newRule.value.name) {
    message.error('请输入规则名称')
    return
  }
  try {
    await createAlertRule(newRule.value)
    message.success('规则创建成功')
    showCreateRuleModal.value = false
    newRule.value = { name: '', type: undefined, threshold: '', severity: undefined }
    await fetchAlertData()
  } catch (e) {
    console.error('创建规则失败:', e)
    message.error('规则创建失败')
  }
}
</script>

<style scoped>
.alert-notification-page {
  padding: 0;
}
</style>
