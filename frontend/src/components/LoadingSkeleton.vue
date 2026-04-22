<template>
  <div class="loading-skeleton" :class="`skeleton-${type}`">
    <!-- 表格骨架屏 -->
    <template v-if="type === 'table'">
      <div class="skeleton-table-header">
        <div v-for="col in rows" :key="`th-${col}`" class="skeleton-block skeleton-th" />
      </div>
      <div v-for="row in rows" :key="`tr-${row}`" class="skeleton-table-row">
        <div v-for="col in 5" :key="`td-${row}-${col}`" class="skeleton-block skeleton-td" />
      </div>
    </template>

    <!-- 卡片骨架屏 -->
    <template v-else-if="type === 'card'">
      <div v-for="row in rows" :key="`card-${row}`" class="skeleton-card">
        <div class="skeleton-card-header">
          <div class="skeleton-block skeleton-avatar" />
          <div class="skeleton-block skeleton-badge" />
        </div>
        <div class="skeleton-block skeleton-title" />
        <div class="skeleton-block skeleton-text" />
        <div class="skeleton-block skeleton-text skeleton-text-short" />
        <div class="skeleton-card-footer">
          <div class="skeleton-block skeleton-meta" />
          <div class="skeleton-card-actions">
            <div class="skeleton-block skeleton-btn" />
            <div class="skeleton-block skeleton-btn" />
          </div>
        </div>
      </div>
    </template>

    <!-- 表单骨架屏 -->
    <template v-else-if="type === 'form'">
      <div v-for="row in rows" :key="`form-${row}`" class="skeleton-form-item">
        <div class="skeleton-block skeleton-label" />
        <div class="skeleton-block skeleton-input" />
      </div>
    </template>

    <!-- 图表骨架屏 -->
    <template v-else-if="type === 'chart'">
      <div class="skeleton-chart">
        <div class="skeleton-chart-header">
          <div class="skeleton-block skeleton-chart-title" />
          <div class="skeleton-block skeleton-chart-subtitle" />
        </div>
        <div class="skeleton-chart-body">
          <div
            v-for="i in 7"
            :key="`bar-${i}`"
            class="skeleton-bar"
            :style="{ height: `${40 + Math.random() * 50}%` }"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * LoadingSkeleton 组件
 * 加载骨架屏组件，提供多种预设类型
 * 在数据加载时展示动画占位符，提升用户体验
 */

interface Props {
  /** 骨架屏类型 */
  type?: 'table' | 'card' | 'form' | 'chart'
  /** 行数/数量 */
  rows?: number
}

withDefaults(defineProps<Props>(), {
  type: 'table',
  rows: 3,
})
</script>

<style scoped>
.loading-skeleton {
  width: 100%;
}

/* 骨架块基础样式 + 动画 */
.skeleton-block {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
  border-radius: 6px;
}

@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 表格骨架屏 */
.skeleton-table-header {
  display: flex;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.skeleton-th {
  height: 14px;
  flex: 1;
}

.skeleton-table-row {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid #fafafa;
}

.skeleton-td {
  height: 16px;
  flex: 1;
}

/* 卡片骨架屏 */
.skeleton-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.skeleton-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
}

.skeleton-badge {
  width: 60px;
  height: 24px;
  border-radius: 9999px;
}

.skeleton-title {
  height: 20px;
  width: 70%;
  margin-bottom: 10px;
}

.skeleton-text {
  height: 14px;
  width: 100%;
  margin-bottom: 8px;
}

.skeleton-text-short {
  width: 60%;
}

.skeleton-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f5f5f5;
}

.skeleton-meta {
  height: 12px;
  width: 80px;
}

.skeleton-card-actions {
  display: flex;
  gap: 8px;
}

.skeleton-btn {
  width: 56px;
  height: 32px;
  border-radius: 8px;
}

/* 卡片网格布局 */
.skeleton-card ~ .skeleton-card {
  margin-top: 16px;
}

/* 表单骨架屏 */
.skeleton-form-item {
  margin-bottom: 20px;
}

.skeleton-label {
  height: 14px;
  width: 80px;
  margin-bottom: 8px;
}

.skeleton-input {
  height: 40px;
  width: 100%;
  border-radius: 10px;
}

/* 图表骨架屏 */
.skeleton-chart {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #f0f0f0;
}

.skeleton-chart-header {
  margin-bottom: 24px;
}

.skeleton-chart-title {
  height: 18px;
  width: 120px;
  margin-bottom: 6px;
}

.skeleton-chart-subtitle {
  height: 12px;
  width: 180px;
}

.skeleton-chart-body {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 200px;
}

.skeleton-bar {
  flex: 1;
  border-radius: 6px 6px 0 0;
  min-height: 20px;
}

/* 暗色模式 */
:global(.dark) .skeleton-block {
  background: linear-gradient(90deg, #262626 25%, #2a2a2a 50%, #262626 75%);
  background-size: 200% 100%;
}

:global(.dark) .skeleton-card,
:global(.dark) .skeleton-chart {
  background: #171717;
  border-color: #262626;
}

:global(.dark) .skeleton-table-header {
  border-bottom-color: #262626;
}

:global(.dark) .skeleton-table-row {
  border-bottom-color: #1a1a1a;
}

:global(.dark) .skeleton-card-footer {
  border-top-color: #262626;
}
</style>
