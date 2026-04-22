<template>
  <div class="avatar-group">
    <!-- 显示的头像列表 -->
    <a-tooltip v-for="(user, index) in visibleUsers" :key="user.name" :title="user.name">
      <div
        class="avatar-item"
        :style="{
          zIndex: visibleUsers.length - index,
          marginLeft: index > 0 ? '-8px' : '0',
        }"
      >
        <a-avatar
          v-if="user.avatar"
          :src="user.avatar"
          :size="avatarSize"
          class="avatar-inner"
        />
        <a-avatar
          v-else
          :size="avatarSize"
          :style="{ backgroundColor: getAvatarColor(index) }"
          class="avatar-inner"
        >
          {{ getInitial(user.name) }}
        </a-avatar>
      </div>
    </a-tooltip>

    <!-- 超出部分 +N 提示 -->
    <a-tooltip v-if="remainingCount > 0">
      <template #title>
        <div class="overflow-tooltip">
          <div v-for="user in overflowUsers" :key="user.name" class="overflow-user">
            {{ user.name }}
          </div>
        </div>
      </template>
      <div
        class="avatar-item avatar-overflow"
        :style="{
          zIndex: 0,
          marginLeft: visibleUsers.length > 0 ? '-8px' : '0',
        }"
      >
        <a-avatar :size="avatarSize" class="avatar-inner avatar-overflow-inner">
          +{{ remainingCount }}
        </a-avatar>
      </div>
    </a-tooltip>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AvatarGroup 组件
 * 用户头像组，显示重叠的头像列表
 * 超出最大数量时显示 +N 提示
 * 用于 PermissionManagement、AgentList 等页面
 */

// 用户数据类型
export interface AvatarUser {
  /** 用户名 */
  name: string
  /** 头像 URL */
  avatar?: string
}

interface Props {
  /** 用户列表 */
  users: AvatarUser[]
  /** 最大显示数量 */
  max?: number
  /** 头像尺寸 */
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  max: 5,
  size: 32,
})

const avatarSize = computed(() => props.size)

// 可见的用户列表
const visibleUsers = computed(() => {
  return props.users.slice(0, props.max)
})

// 溢出的用户列表
const overflowUsers = computed(() => {
  return props.users.slice(props.max)
})

// 溢出数量
const remainingCount = computed(() => {
  return Math.max(0, props.users.length - props.max)
})

// 头像颜色列表
const avatarColors = [
  '#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#a855f7',
  '#06b6d4', '#ec4899', '#14b8a6', '#f97316', '#6366f1',
]

/**
 * 根据索引获取头像背景色
 */
function getAvatarColor(index: number): string {
  return avatarColors[index % avatarColors.length]
}

/**
 * 获取名字首字母
 */
function getInitial(name: string): string {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}
</script>

<style scoped>
.avatar-group {
  display: inline-flex;
  align-items: center;
}

.avatar-item {
  position: relative;
  display: inline-flex;
  transition: transform 0.2s;
}

.avatar-item:hover {
  transform: translateY(-2px);
  z-index: 100 !important;
}

.avatar-inner {
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.avatar-item:hover .avatar-inner {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.avatar-overflow-inner {
  background-color: #f0f0f0 !important;
  color: #737373 !important;
  font-size: 12px !important;
  font-weight: 600 !important;
}

/* 暗色模式 */
:global(.dark) .avatar-inner {
  border-color: #262626;
}

:global(.dark) .avatar-overflow-inner {
  background-color: #333 !important;
  color: #a3a3a3 !important;
}

/* 溢出提示样式 */
.overflow-tooltip {
  max-width: 200px;
}

.overflow-user {
  font-size: 13px;
  padding: 2px 0;
  white-space: nowrap;
}
</style>
