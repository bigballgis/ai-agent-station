<template>
  <a-popover
    v-model:open="popoverVisible"
    trigger="click"
    placement="bottomRight"
    :overlay-class-name="'notification-center-popover'"
  >
    <template #content>
      <div class="notification-center">
        <!-- Header -->
        <div class="nc-header">
          <span class="nc-title">{{ t('component.notificationCenter') }}</span>
          <div class="nc-header-actions">
            <a-button type="link" size="small" :disabled="totalUnread === 0" @click="handleMarkAllRead">
              {{ t('component.markAllRead') }}
            </a-button>
            <a-button type="link" size="small" @click="handleOpenSettings">
              <SettingOutlined />
            </a-button>
          </div>
        </div>

        <!-- Category filter tabs -->
        <div class="nc-filter">
          <button
            v-for="filter in categoryFilters"
            :key="filter.value"
            class="nc-filter-btn"
            :class="{ 'nc-filter-active': activeFilter === filter.value }"
            @click="activeFilter = filter.value"
          >
            <span class="nc-filter-dot" :class="`dot-${filter.value}`" />
            <span>{{ t(filter.label) }}</span>
            <span v-if="getFilterCount(filter.value) > 0" class="nc-filter-count">
              {{ getFilterCount(filter.value) }}
            </span>
          </button>
        </div>

        <!-- Notification list -->
        <div class="nc-list">
          <div
            v-for="item in displayedNotifications"
            :key="item.id"
            class="nc-item"
            :class="{ 'nc-item-unread': !item.read }"
            @click="handleClickNotification(item)"
          >
            <div class="nc-item-dot" :class="`dot-${item.category}`" />
            <div class="nc-item-content">
              <div class="nc-item-header">
                <span class="nc-item-title">{{ item.title }}</span>
                <span
                  v-if="item.priority === 'urgent' || item.priority === 'high'"
                  class="nc-item-priority"
                  :class="`priority-${item.priority}`"
                >
                  {{ t(`component.priority.${item.priority}`) }}
                </span>
              </div>
              <p class="nc-item-message">{{ item.content }}</p>
              <span class="nc-item-time">{{ formatTime(item.timestamp) }}</span>
            </div>
            <button
              v-if="!item.read"
              class="nc-item-read-btn"
              :aria-label="t('component.markAsRead')"
              @click.stop="handleMarkAsRead(item.id)"
            >
              <CheckOutlined />
            </button>
          </div>

          <!-- Empty state -->
          <div v-if="displayedNotifications.length === 0" class="nc-empty">
            <BellOutlined class="nc-empty-icon" />
            <span>{{ t('component.noNotifications') }}</span>
          </div>
        </div>

        <!-- Footer -->
        <div v-if="realtimeNotifications.length > 0" class="nc-footer">
          <a-button type="link" size="small" block @click="handleViewAll">
            {{ t('component.viewAllNotifications') }}
          </a-button>
        </div>
      </div>
    </template>

    <!-- Trigger button -->
    <a-badge :count="totalUnread" :offset="[-2, 4]" size="small">
      <button class="nc-trigger" :aria-label="t('header.notifications')">
        <BellOutlined class="nc-trigger-icon" />
      </button>
    </a-badge>
  </a-popover>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { BellOutlined, CheckOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { useNotificationStore, type CategoryFilter, type RealtimeNotification } from '@/store/modules/notification'

const { t } = useI18n()
const notificationStore = useNotificationStore()

/**
 * NotificationCenter component
 * Real-time notification center with category filtering, read/unread tracking,
 * and priority indicators. Integrates with the notification store.
 */

const popoverVisible = ref(false)
const activeFilter = ref<CategoryFilter>('all')

const categoryFilters: { value: CategoryFilter; label: string }[] = [
  { value: 'all', label: 'component.filterAll' },
  { value: 'info', label: 'common.info' },
  { value: 'success', label: 'common.success' },
  { value: 'warning', label: 'common.warning' },
  { value: 'error', label: 'common.errorText' },
]

const realtimeNotifications = computed(() => notificationStore.filteredRealtimeNotifications)
const totalUnread = computed(() => notificationStore.totalUnreadCount)

/** Get unread count for a specific category filter */
function getFilterCount(filter: CategoryFilter): number {
  if (filter === 'all') {
    return notificationStore.realtimeUnreadCount
  }
  return notificationStore.realtimeNotifications.filter(
    (n) => n.category === filter && !n.read
  ).length
}

const displayedNotifications = computed(() => {
  // Sync filter with store
  notificationStore.setCategoryFilter(activeFilter.value)
  return realtimeNotifications.value.slice(0, 50) // Show max 50 in dropdown
})

/** Format timestamp to relative time */
function formatTime(timestamp: string): string {
  try {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSec = Math.floor(diffMs / 1000)
    const diffMin = Math.floor(diffSec / 60)
    const diffHour = Math.floor(diffMin / 60)
    const diffDay = Math.floor(diffHour / 24)

    if (diffSec < 60) return t('component.timeJustNow')
    if (diffMin < 60) return t('component.timeMinutesAgo', { count: diffMin })
    if (diffHour < 24) return t('component.timeHoursAgo', { count: diffHour })
    if (diffDay < 7) return t('component.timeDaysAgo', { count: diffDay })
    return date.toLocaleDateString()
  } catch {
    return timestamp
  }
}

/** Mark a single notification as read */
function handleMarkAsRead(id: string | number): void {
  notificationStore.markRealtimeAsRead(id)
}

/** Mark all notifications as read */
function handleMarkAllRead(): void {
  notificationStore.markAllRealtimeAsRead()
}

/** Click on a notification item */
function handleClickNotification(item: RealtimeNotification): void {
  notificationStore.markRealtimeAsRead(item.id)
  if (item.link) {
    popoverVisible.value = false
    // Navigate if a link is provided
    window.location.hash = item.link
  }
}

/** View all notifications */
function handleViewAll(): void {
  popoverVisible.value = false
}

/** Open notification settings */
function handleOpenSettings(): void {
  popoverVisible.value = false
}
</script>

<style scoped>
.nc-trigger {
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

.nc-trigger:hover {
  background: #f5f5f5;
  color: #404040;
}

.nc-trigger-icon {
  font-size: 16px;
}

/* Notification center dropdown */
.notification-center {
  width: 380px;
}

.nc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.nc-title {
  font-size: 15px;
  font-weight: 600;
  color: #171717;
}

.nc-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Category filter */
.nc-filter {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
}

.nc-filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: none;
  color: #737373;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.nc-filter-btn:hover {
  background: #f5f5f5;
}

.nc-filter-active {
  background: #f0f5ff;
  color: #3b82f6;
  border-color: #bfdbfe;
}

.nc-filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-all { background-color: #737373; }
.dot-info { background-color: #3b82f6; }
.dot-success { background-color: #22c55e; }
.dot-warning { background-color: #f59e0b; }
.dot-error { background-color: #ef4444; }

.nc-filter-count {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #3b82f6;
  color: white;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Notification list */
.nc-list {
  max-height: 400px;
  overflow-y: auto;
}

.nc-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #fafafa;
  position: relative;
}

.nc-item:hover {
  background: #fafafa;
}

.nc-item-unread {
  background: rgba(59, 130, 246, 0.02);
}

.nc-item-unread:hover {
  background: rgba(59, 130, 246, 0.05);
}

.nc-item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.nc-item-content {
  flex: 1;
  min-width: 0;
}

.nc-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.nc-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #171717;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nc-item-priority {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.priority-urgent {
  background: #fef2f2;
  color: #dc2626;
}

.priority-high {
  background: #fffbeb;
  color: #d97706;
}

.nc-item-message {
  font-size: 12px;
  color: #525252;
  line-height: 1.5;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.nc-item-time {
  font-size: 11px;
  color: #a3a3a3;
}

.nc-item-read-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: none;
  color: #a3a3a3;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.nc-item:hover .nc-item-read-btn {
  opacity: 1;
}

.nc-item-read-btn:hover {
  background: #f0f5ff;
  color: #3b82f6;
}

/* Empty state */
.nc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 16px;
  color: #a3a3a3;
  font-size: 14px;
}

.nc-empty-icon {
  font-size: 32px;
  color: #d4d4d4;
}

/* Footer */
.nc-footer {
  border-top: 1px solid #f0f0f0;
  padding: 4px 0;
}

/* Dark mode */
:global(.dark) .nc-trigger:hover {
  background: #262626;
  color: #d4d4d4;
}

:global(.dark) .nc-header {
  border-bottom-color: #262626;
}

:global(.dark) .nc-title {
  color: #fafafa;
}

:global(.dark) .nc-filter {
  border-bottom-color: #262626;
}

:global(.dark) .nc-filter-btn:hover {
  background: #262626;
}

:global(.dark) .nc-filter-active {
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
  border-color: rgba(59, 130, 246, 0.2);
}

:global(.dark) .nc-item {
  border-bottom-color: #1a1a1a;
}

:global(.dark) .nc-item:hover {
  background: #1a1a1a;
}

:global(.dark) .nc-item-unread {
  background: rgba(96, 165, 250, 0.03);
}

:global(.dark) .nc-item-title {
  color: #fafafa;
}

:global(.dark) .nc-item-message {
  color: #a3a3a3;
}

:global(.dark) .nc-item-read-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
}

:global(.dark) .nc-footer {
  border-top-color: #262626;
}

:global(.dark) .nc-empty {
  color: #525252;
}

:global(.dark) .nc-empty-icon {
  color: #404040;
}

:global(.dark) .priority-urgent {
  background: rgba(220, 38, 38, 0.1);
  color: #f87171;
}

:global(.dark) .priority-high {
  background: rgba(217, 119, 6, 0.1);
  color: #fbbf24;
}
</style>
