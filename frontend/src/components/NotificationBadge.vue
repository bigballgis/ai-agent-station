<template>
  <a-popover
    v-model:open="popoverVisible"
    trigger="click"
    placement="bottomRight"
    :overlay-class-name="'notification-popover'"
  >
    <template #content>
      <div class="notification-dropdown">
        <!-- 头部 -->
        <div class="notification-header">
          <span class="notification-title">{{ t('component.notification') }}</span>
          <a-button type="link" size="small" @click="handleMarkAllRead">
            {{ t('component.markAllRead') }}
          </a-button>
        </div>

        <!-- 通知列表 -->
        <div class="notification-list">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ 'notification-item-unread': !notification.read }"
            @click="handleClickNotification(notification)"
          >
            <div class="notification-dot" :class="`dot-${notification.type || 'info'}`" />
            <div class="notification-content">
              <p class="notification-message">{{ notification.message }}</p>
              <span class="notification-time">{{ notification.time }}</span>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="notifications.length === 0" class="notification-empty">
            {{ t('component.noNotifications') }}
          </div>
        </div>

        <!-- 底部 -->
        <div v-if="notifications.length > 0" class="notification-footer">
          <a-button type="link" size="small" block @click="handleViewAll">
            {{ t('component.viewAllNotifications') }}
          </a-button>
        </div>
      </div>
    </template>

    <!-- 触发按钮 -->
    <a-badge :count="count" :offset="[-2, 4]" size="small">
      <button class="notification-trigger" :aria-label="t('header.notifications')">
        <BellOutlined v-if="!icon" class="notification-bell" />
        <component v-else :is="icon" class="notification-bell" />
      </button>
    </a-badge>
  </a-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { BellOutlined } from '@ant-design/icons-vue'
import type { Component } from 'vue'

const { t } = useI18n()

/**
 * NotificationBadge 组件
 * 通知徽标下拉组件
 * 显示通知数量，点击展开通知列表
 * 支持标记全部已读、查看全部
 * 用于 MainLayout 头部
 */

// 通知项类型
export interface NotificationItem {
  /** 通知 ID */
  id: string | number
  /** 通知消息 */
  message: string
  /** 通知时间 */
  time: string
  /** 是否已读 */
  read?: boolean
  /** 通知类型 */
  type?: 'info' | 'success' | 'warning' | 'error'
}

interface Props {
  /** 通知数量 */
  count?: number
  /** 自定义图标 */
  icon?: Component
  /** 通知列表 */
  notifications?: NotificationItem[]
}

withDefaults(defineProps<Props>(), {
  count: 0,
  notifications: () => [],
})

const emit = defineEmits<{
  (e: 'markAllRead'): void
  (e: 'viewAll'): void
  (e: 'click', notification: NotificationItem): void
}>()

const popoverVisible = ref(false)

/** 标记全部已读 */
function handleMarkAllRead() {
  emit('markAllRead')
}

/** 查看全部 */
function handleViewAll() {
  popoverVisible.value = false
  emit('viewAll')
}

/** 点击通知项 */
function handleClickNotification(notification: NotificationItem) {
  emit('click', notification)
  popoverVisible.value = false
}
</script>

<style scoped>
.notification-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: none;
  color: #737373;
  cursor: pointer;
  transition: all 0.2s;
}

.notification-trigger:hover {
  background: #f5f5f5;
  color: #404040;
}

.notification-bell {
  font-size: 16px;
}

/* 下拉面板 */
.notification-dropdown {
  width: 320px;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: #171717;
}

/* 通知列表 */
.notification-list {
  max-height: 320px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #fafafa;
}

.notification-item:hover {
  background: #fafafa;
}

.notification-item-unread {
  background: rgba(59, 130, 246, 0.02);
}

.notification-item-unread:hover {
  background: rgba(59, 130, 246, 0.05);
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.dot-info { background-color: #3b82f6; }
.dot-success { background-color: #22c55e; }
.dot-warning { background-color: #f59e0b; }
.dot-error { background-color: #ef4444; }

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-message {
  font-size: 13px;
  color: #404040;
  line-height: 1.5;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  font-size: 12px;
  color: #a3a3a3;
}

.notification-empty {
  text-align: center;
  padding: 32px 16px;
  color: #a3a3a3;
  font-size: 14px;
}

/* 底部 */
.notification-footer {
  border-top: 1px solid #f0f0f0;
  padding: 4px 0;
}

/* 暗色模式 */
:global(.dark) .notification-trigger:hover {
  background: #262626;
  color: #d4d4d4;
}

:global(.dark) .notification-header {
  border-bottom-color: #262626;
}

:global(.dark) .notification-title {
  color: #fafafa;
}

:global(.dark) .notification-item {
  border-bottom-color: #1a1a1a;
}

:global(.dark) .notification-item:hover {
  background: #1a1a1a;
}

:global(.dark) .notification-item-unread {
  background: rgba(96, 165, 250, 0.03);
}

:global(.dark) .notification-message {
  color: #d4d4d4;
}

:global(.dark) .notification-footer {
  border-top-color: #262626;
}

:global(.dark) .notification-empty {
  color: #525252;
}
</style>
