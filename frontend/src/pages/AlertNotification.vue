<template>
  <div class="alert-notification-page" aria-label="告警通知">
    <!-- 页面头部 -->
    <PageHeader title="告警中心" subtitle="统一管理和监控所有告警规则与告警记录，及时发现和处理系统异常" />

    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex items-center justify-center py-20">
      <a-spin size="large" />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="flex flex-col items-center justify-center py-20">
      <svg class="w-12 h-12 mb-3 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
      </svg>
      <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-3">{{ loadError }}</p>
      <button
        class="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium bg-primary-50 dark:bg-primary-950/30 text-primary-600 dark:text-primary-400 hover:bg-primary-100 dark:hover:bg-primary-950/50 transition-all duration-200 cursor-pointer"
        @click="fetchAlertData"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        重新加载
      </button>
    </div>

    <template v-else>

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
            @click="switchTab(tab.key)"
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
          <div class="flex items-center gap-3">
            <h3 class="text-base font-semibold text-neutral-800 dark:text-neutral-200">告警规则列表</h3>
            <!-- 搜索框 -->
            <div class="relative">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                v-model="ruleSearch"
                type="text"
                placeholder="搜索规则名称..."
                class="pl-9 pr-4 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 w-56"
              />
            </div>
            <!-- 严重级别筛选 -->
            <select
              v-model="ruleSeverityFilter"
              class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
            >
              <option value="">全部级别</option>
              <option value="CRITICAL">严重</option>
              <option value="WARNING">警告</option>
              <option value="INFO">信息</option>
            </select>
          </div>
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

        <!-- 规则加载状态 -->
        <div v-if="rulesLoading" class="flex items-center justify-center py-12">
          <a-spin />
        </div>

        <!-- 规则空状态 -->
        <div v-else-if="filteredRules.length === 0" class="flex flex-col items-center justify-center py-12">
          <svg class="w-10 h-10 mb-2 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <p class="text-sm text-neutral-400 dark:text-neutral-500">暂无告警规则</p>
        </div>

        <div v-else class="bg-white dark:bg-neutral-900 rounded-xl border border-neutral-100 dark:border-neutral-800 overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-neutral-100 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-800/60">
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">规则名称</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">严重级别</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">状态</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">创建时间</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="rule in filteredRules"
                :key="rule.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 last:border-0 hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="px-4 py-3 font-medium text-neutral-800 dark:text-neutral-200">{{ rule.name }}</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="getSeverityBadgeClass(rule.severity)"
                  >
                    {{ getSeverityLabel(rule.severity) }}
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
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 text-xs whitespace-nowrap">{{ formatDateTime(rule.createdAt) }}</td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
                      role="button"
                      tabindex="0"
                      title="编辑"
                      @click="editRule(rule)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-all duration-200 cursor-pointer"
                      role="button"
                      tabindex="0"
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
            v-model="recordFilter.severity"
            class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
            @change="handleRecordFilterChange"
          >
            <option value="">全部级别</option>
            <option value="CRITICAL">严重</option>
            <option value="WARNING">警告</option>
            <option value="INFO">信息</option>
          </select>
          <select
            v-model="recordFilter.status"
            class="px-3 py-2 rounded-xl text-sm bg-neutral-50 dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-400 dark:focus:border-primary-500 transition-all duration-200 cursor-pointer"
            @change="handleRecordFilterChange"
          >
            <option value="">全部状态</option>
            <option value="firing">触发中</option>
            <option value="resolved">已解决</option>
          </select>
        </div>

        <!-- 记录加载状态 -->
        <div v-if="recordsLoading" class="flex items-center justify-center py-12">
          <a-spin />
        </div>

        <!-- 记录空状态 -->
        <div v-else-if="alertRecords.length === 0" class="flex flex-col items-center justify-center py-12">
          <svg class="w-10 h-10 mb-2 text-neutral-300 dark:text-neutral-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-sm text-neutral-400 dark:text-neutral-500">暂无告警记录</p>
        </div>

        <template v-else>
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
                v-for="record in alertRecords"
                :key="record.id"
                class="border-b border-neutral-50 dark:border-neutral-800/50 last:border-0 hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30 transition-colors"
              >
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 text-xs whitespace-nowrap">{{ formatDateTime(record.firedAt) }}</td>
                <td class="px-4 py-3 font-medium text-neutral-800 dark:text-neutral-200">{{ record.ruleName }}</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="getSeverityBadgeClass(record.severity)"
                  >
                    {{ getSeverityLabel(record.severity) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-neutral-600 dark:text-neutral-400 font-mono text-xs">{{ formatMetricValue(record.metricValue, record.threshold) }}</td>
                <td class="px-4 py-3">
                  <StatusBadge :status="record.status" type="alert" :dot="true" />
                </td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      v-if="record.status === 'firing'"
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-950/30 transition-all duration-200 cursor-pointer"
                      role="button"
                      tabindex="0"
                      title="确认解决"
                      @click="resolveRecord(record)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button
                      class="p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-950/30 transition-all duration-200 cursor-pointer"
                      role="button"
                      tabindex="0"
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

        <!-- 分页 -->
        <div v-if="recordsPagination.total > recordsPagination.size" class="flex items-center justify-end mt-4">
          <a-pagination
            v-model:current="recordsPagination.page"
            :total="recordsPagination.total"
            :page-size="recordsPagination.size"
            :show-size-changer="false"
            size="small"
            @change="handleRecordsPageChange"
          />
        </div>
        </template>
      </div>
    </div>

    <!-- 新建规则弹窗 -->
    <a-modal
      v-model:open="showCreateRuleModal"
      :title="editingRuleId ? '编辑告警规则' : '新建告警规则'"
      :ok-button-props="{ class: '!rounded-xl' }"
      :cancel-button-props="{ class: '!rounded-xl' }"
      :confirm-loading="ruleSubmitting"
      @ok="handleCreateRule"
      @cancel="handleCancelRuleModal"
    >
      <a-form layout="vertical" class="mt-4">
        <a-form-item label="规则名称" required>
          <a-input v-model:value="newRule.name" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="newRule.description" placeholder="请输入规则描述" :rows="2" />
        </a-form-item>
        <a-form-item label="告警类型" required>
          <a-select v-model:value="newRule.alertType" placeholder="请选择类型">
            <a-select-option value="API_ERROR_RATE">API 错误率</a-select-option>
            <a-select-option value="API_RESPONSE_TIME">响应时间</a-select-option>
            <a-select-option value="JVM_CPU">CPU 使用率</a-select-option>
            <a-select-option value="JVM_MEMORY">内存使用率</a-select-option>
            <a-select-option value="DB_CONNECTION_POOL">数据库连接池</a-select-option>
            <a-select-option value="AGENT_EXECUTION_FAILURE">Agent 执行失败</a-select-option>
            <a-select-option value="QUOTA_EXCEEDED">配额超限</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="指标名称" required>
          <a-input v-model:value="newRule.metricName" placeholder="例如: api_error_rate" />
        </a-form-item>
        <div class="grid grid-cols-2 gap-4">
          <a-form-item label="阈值" required>
            <a-input-number v-model:value="newRule.threshold" placeholder="阈值" :style="{ width: '100%' }" />
          </a-form-item>
          <a-form-item label="比较运算符">
            <a-select v-model:value="newRule.comparisonOperator" placeholder="请选择">
              <a-select-option value="gt">大于 (>)</a-select-option>
              <a-select-option value="gte">大于等于 (>=)</a-select-option>
              <a-select-option value="lt">小于 (<)</a-select-option>
              <a-select-option value="lte">小于等于 (<=)</a-select-option>
              <a-select-option value="eq">等于 (=)</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <a-form-item label="严重级别">
            <a-select v-model:value="newRule.severity" placeholder="请选择级别">
              <a-select-option value="INFO">信息</a-select-option>
              <a-select-option value="WARNING">警告</a-select-option>
              <a-select-option value="CRITICAL">严重</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="持续时间(秒)">
            <a-input-number v-model:value="newRule.durationSeconds" placeholder="持续时间" :min="0" :style="{ width: '100%' }" />
          </a-form-item>
        </div>
        <a-form-item label="通知渠道">
          <a-select v-model:value="newRule.notifyChannels" placeholder="请选择通知渠道">
            <a-select-option value="email">邮件</a-select-option>
            <a-select-option value="webhook">Webhook</a-select-option>
            <a-select-option value="sms">短信</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="通知目标">
          <a-input v-model:value="newRule.notifyTargets" placeholder="请输入通知目标（邮箱、URL等）" />
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
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ formatDateTime(detailRecord.firedAt) }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">规则名称</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ detailRecord.ruleName }}</p>
          </div>
          <div>
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">告警类型</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ formatAlertType(detailRecord.alertType) }}</p>
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
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">阈值</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200 font-mono">{{ detailRecord.threshold }}</p>
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
          <div v-if="detailRecord.resolvedAt">
            <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">解决时间</h4>
            <p class="text-sm text-neutral-800 dark:text-neutral-200">{{ formatDateTime(detailRecord.resolvedAt) }}</p>
          </div>
        </div>
        <div v-if="detailRecord.message">
          <h4 class="text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">告警描述</h4>
          <p class="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed">{{ detailRecord.message }}</p>
        </div>
      </div>
    </a-modal>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  AlertOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import {
  getAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  getAlertRecords,
  getAlertStats,
  resolveAlertRecord,
  getActiveAlerts,
} from '@/api/alert'
import { PageHeader, StatCard, StatusBadge } from '@/components'

// ==================== 类型定义 ====================

interface AlertRuleVO {
  id: number
  name: string
  severity: string
  status: string
  enabled: boolean
  createdAt: string
}

interface AlertRecord {
  id: number
  ruleId: number
  ruleName: string
  alertType: string
  severity: string
  message: string
  metricValue: number
  threshold: number
  status: string
  firedAt: string
  resolvedAt: string
  tenantId: number
}

interface AlertRuleForm {
  name: string
  description: string
  alertType: string | undefined
  metricName: string
  threshold: number | null
  comparisonOperator: string
  durationSeconds: number | null
  severity: string | undefined
  notifyChannels: string
  notifyTargets: string
  isActive: boolean
}

// ==================== Tab ====================

const tabs = ref([
  { key: 'rules', label: '告警规则', count: 0 },
  { key: 'records', label: '告警记录', count: 0 },
])
const activeTab = ref('rules')

// ==================== 统计卡片 ====================

const stats = ref([
  {
    label: '24h 活跃告警',
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
    label: '已解决告警',
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

// ==================== 数据状态 ====================

const alertRules = ref<AlertRuleVO[]>([])
const alertRecords = ref<AlertRecord[]>([])
const activeAlertCount = ref(0)
const resolvedAlertCount = ref(0)

// Loading & Error 状态
const pageLoading = ref(true)
const loadError = ref('')
const rulesLoading = ref(false)
const recordsLoading = ref(false)

// ==================== 分页 ====================

const recordsPagination = ref({
  page: 1,   // ant-design-vue 分页从1开始
  size: 20,
  total: 0,
})

// ==================== 筛选 & 搜索 ====================

const ruleSearch = ref('')
const ruleSeverityFilter = ref('')

const recordFilter = ref({
  severity: '',
  status: '',
})

// ==================== Modal 状态 ====================

const showCreateRuleModal = ref(false)
const showDetailModal = ref(false)
const detailRecord = ref<AlertRecord | null>(null)
const editingRuleId = ref<number | string | null>(null)
const ruleSubmitting = ref(false)

const defaultRuleForm = (): AlertRuleForm => ({
  name: '',
  description: '',
  alertType: undefined,
  metricName: '',
  threshold: null,
  comparisonOperator: 'gt',
  durationSeconds: 300,
  severity: 'WARNING',
  notifyChannels: 'email',
  notifyTargets: '',
  isActive: true,
})

const newRule = ref<AlertRuleForm>(defaultRuleForm())

// ==================== 数据获取 ====================

async function fetchAlertData() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await Promise.all([
      fetchRules(),
      fetchRecords(),
      fetchStats(),
    ])
  } catch (e: any) {
    console.error('获取告警数据失败:', e)
    loadError.value = e?.message || '获取告警数据失败，请稍后重试'
  } finally {
    pageLoading.value = false
  }
}

async function fetchRules() {
  rulesLoading.value = true
  try {
    const res = await getAlertRules()
    const data = res.data
    alertRules.value = Array.isArray(data) ? data : (data?.records || [])
    tabs.value[0].count = alertRules.value.length
  } catch (e) {
    console.error('获取告警规则失败:', e)
    throw e
  } finally {
    rulesLoading.value = false
  }
}

async function fetchRecords() {
  recordsLoading.value = true
  try {
    const params: Record<string, unknown> = {
      page: recordsPagination.value.page - 1, // 后端从0开始
      size: recordsPagination.value.size,
    }
    const res = await getAlertRecords(params)
    const pageData = res.data
    // PageResult 结构: { total, records, page, size, totalPages }
    alertRecords.value = pageData?.records || []
    recordsPagination.value.total = pageData?.total || 0
    tabs.value[1].count = pageData?.total || 0
  } catch (e) {
    console.error('获取告警记录失败:', e)
    throw e
  } finally {
    recordsLoading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getAlertStats()
    const s = res.data || {}

    // 后端返回: { activeIn24h, totalRules }
    // 同时获取活跃告警列表以计算已解决数
    const activeRes = await getActiveAlerts()
    const activeList = activeRes.data || []
    activeAlertCount.value = s.activeIn24h ?? activeList.length ?? 0

    // 通过总记录数减去活跃数来估算已解决数
    const totalRecords = recordsPagination.value.total
    resolvedAlertCount.value = Math.max(0, totalRecords - activeAlertCount.value)

    stats.value[0].value = activeAlertCount.value
    stats.value[1].value = totalRecords
    stats.value[2].value = resolvedAlertCount.value
    stats.value[3].value = s.totalRules ?? alertRules.value.length ?? 0
  } catch (e) {
    console.error('获取告警统计失败:', e)
  }
}

// ==================== Tab 切换 ====================

function switchTab(key: string) {
  activeTab.value = key
  // 切换到记录 tab 时刷新数据
  if (key === 'records') {
    fetchRecords()
  }
}

// ==================== 规则搜索和筛选 ====================

const filteredRules = computed(() => {
  let result = alertRules.value
  if (ruleSearch.value.trim()) {
    const keyword = ruleSearch.value.trim().toLowerCase()
    result = result.filter(r => r.name?.toLowerCase().includes(keyword))
  }
  if (ruleSeverityFilter.value) {
    result = result.filter(r => r.severity === ruleSeverityFilter.value)
  }
  return result
})

// ==================== 记录筛选 & 分页 ====================

function handleRecordFilterChange() {
  // 筛选变更时重置到第一页并重新获取
  recordsPagination.value.page = 1
  fetchRecords()
}

function handleRecordsPageChange(page: number) {
  recordsPagination.value.page = page
  fetchRecords()
}

// ==================== Modal 自动聚焦 ====================

watch(showCreateRuleModal, (val) => {
  if (val) {
    nextTick(() => {
      (document.querySelector('.ant-modal input') as HTMLElement | null)?.focus()
    })
  }
})

watch(showDetailModal, (val) => {
  if (val) {
    nextTick(() => {
      (document.querySelector('.ant-modal input') as HTMLElement | null)?.focus()
    })
  }
})

// ==================== 生命周期 ====================

onMounted(async () => {
  await fetchAlertData()
})

// ==================== 格式化工具函数 ====================

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch {
    return dateStr
  }
}

function formatMetricValue(metricValue: number | undefined, threshold: number | undefined): string {
  if (metricValue == null) return '-'
  const val = typeof metricValue === 'number' ? metricValue.toFixed(2) : metricValue
  if (threshold != null) {
    return `${val} / ${threshold}`
  }
  return String(val)
}

function formatAlertType(alertType: string | undefined): string {
  const map: Record<string, string> = {
    API_ERROR_RATE: 'API 错误率',
    API_RESPONSE_TIME: 'API 响应时间',
    JVM_CPU: 'JVM CPU',
    JVM_MEMORY: 'JVM 内存',
    DB_CONNECTION_POOL: '数据库连接池',
    AGENT_EXECUTION_FAILURE: 'Agent 执行失败',
    QUOTA_EXCEEDED: '配额超限',
  }
  return map[alertType || ''] || alertType || '-'
}

function getSeverityBadgeClass(severity: string): string {
  const map: Record<string, string> = {
    critical: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
    CRITICAL: 'bg-red-50 dark:bg-red-950/30 text-red-600 dark:text-red-400',
    warning: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    WARNING: 'bg-amber-50 dark:bg-amber-950/30 text-amber-600 dark:text-amber-400',
    info: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
    INFO: 'bg-blue-50 dark:bg-blue-950/30 text-blue-600 dark:text-blue-400',
  }
  return map[severity] || map.info
}

function getSeverityLabel(severity: string): string {
  const map: Record<string, string> = {
    critical: '严重',
    CRITICAL: '严重',
    warning: '警告',
    WARNING: '警告',
    info: '信息',
    INFO: '信息',
  }
  return map[severity] || severity
}

// ==================== 规则操作 ====================

async function toggleRule(rule: AlertRuleVO) {
  try {
    await updateAlertRule(rule.id, { isActive: !rule.enabled })
    message.success(rule.enabled ? '规则已禁用' : '规则已启用')
    await fetchRules()
  } catch (e) {
    console.error('切换规则状态失败:', e)
    message.error('操作失败')
  }
}

function editRule(rule: AlertRuleVO) {
  editingRuleId.value = rule.id
  // AlertRuleVO 只有部分字段，编辑时保留已有信息
  newRule.value = {
    ...defaultRuleForm(),
    name: rule.name,
    severity: rule.severity,
    isActive: rule.enabled,
  }
  showCreateRuleModal.value = true
}

function deleteRule(rule: AlertRuleVO) {
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
        await fetchRules()
        await fetchStats()
      } catch (e) {
        console.error('删除规则失败:', e)
        message.error('删除失败')
      }
    },
  })
}

// ==================== 记录操作 ====================

async function resolveRecord(record: AlertRecord) {
  try {
    await resolveAlertRecord(record.id)
    record.status = 'resolved'
    message.success('告警已确认解决')
    // 刷新统计
    await fetchStats()
  } catch (e) {
    console.error('解决告警失败:', e)
    message.error('操作失败，请重试')
  }
}

function viewRecordDetail(record: AlertRecord) {
  detailRecord.value = record
  showDetailModal.value = true
}

// ==================== 创建/编辑规则 ====================

async function handleCreateRule() {
  if (!newRule.value.name) {
    message.error('请输入规则名称')
    return
  }
  if (!newRule.value.alertType) {
    message.error('请选择告警类型')
    return
  }
  if (!newRule.value.metricName) {
    message.error('请输入指标名称')
    return
  }
  if (newRule.value.threshold == null) {
    message.error('请输入阈值')
    return
  }

  ruleSubmitting.value = true
  try {
    if (editingRuleId.value) {
      await updateAlertRule(Number(editingRuleId.value), newRule.value)
      message.success('规则更新成功')
    } else {
      await createAlertRule(newRule.value)
      message.success('规则创建成功')
    }
    showCreateRuleModal.value = false
    editingRuleId.value = null
    newRule.value = defaultRuleForm()
    await fetchRules()
    await fetchStats()
  } catch (e) {
    console.error(editingRuleId.value ? '更新规则失败:' : '创建规则失败:', e)
    message.error(editingRuleId.value ? '规则更新失败' : '规则创建失败')
  } finally {
    ruleSubmitting.value = false
  }
}

function handleCancelRuleModal() {
  showCreateRuleModal.value = false
  editingRuleId.value = null
  newRule.value = defaultRuleForm()
}
</script>

<style scoped>
.alert-notification-page {
  padding: 0;
}
</style>
