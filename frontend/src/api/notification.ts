import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'

export interface Notification {
  id: number
  title: string
  content: string
  type: string
  read: boolean
  sender?: string
  link?: string
  createdAt: string
}

/**
 * 获取通知列表
 */
export function getNotifications(params?: { page?: number; size?: number }) {
  return request.get<ApiResponse<PageResult<Notification> | Notification[]>>('/v1/notifications', { params })
}

/**
 * 标记通知为已读
 */
export function markAsRead(id: number) {
  return request.put(`/v1/notifications/${id}/read`)
}

/**
 * 标记所有通知为已读
 */
export function markAllAsRead() {
  return request.put('/v1/notifications/read-all')
}

const notificationApi = {
  getNotifications,
  markAsRead,
  markAllAsRead,
}

export { notificationApi }
export default notificationApi
