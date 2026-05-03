import request from '@/utils/request'

export interface NotificationResponse {
  id: number
  title: string
  content: string
  type: string
  category: string
  priority: string
  targetType: string
  targetId: number
  targetUrl: string
  isGlobal: boolean
  isRead: boolean
  isStarred: boolean
  readAt: string
  createdAt: string
  senderName: string
}

export interface NotificationDetailResponse {
  id: number
  title: string
  content: string
  type: string
  category: string
  priority: string
  targetType: string
  targetId: number
  targetUrl: string
  isGlobal: boolean
  isRead: boolean
  isStarred: boolean
  readAt: string
  createdAt: string
  expiresAt: string
  senderName: string
  senderId: number
}

export interface UnreadCountResponse {
  total: number
  systemCount: number
  businessCount: number
  reminderCount: number
  todoCount: number
}

export interface NotificationPreferenceResponse {
  id: number
  userId: number
  typeSettings: Record<string, boolean>
  categorySettings: Record<string, boolean>
  receiveChannels: Record<string, boolean>
  quietHoursStart: string
  quietHoursEnd: string
}

export interface CreateNotificationRequest {
  title: string
  content: string
  type: string
  category?: string
  priority?: string
  senderId?: number
  recipientIds?: number[]
  targetType?: string
  targetId?: number
  targetUrl?: string
  isGlobal?: boolean
  expiresAt?: string
}

export interface UpdateNotificationPreferenceRequest {
  typeSettings?: Record<string, boolean>
  categorySettings?: Record<string, boolean>
  receiveChannels?: Record<string, boolean>
  quietHoursStart?: string
  quietHoursEnd?: string
}

export const getNotifications = (params: {
  type?: string
  isRead?: boolean
  page: number
  size: number
}) => {
  return request.get('/notifications', { params })
}

export const getUnreadCount = () => {
  return request.get('/notifications/unread-count')
}

export const getNotificationDetail = (id: number) => {
  return request.get(`/notifications/${id}`)
}

export const markAsRead = (id: number) => {
  return request.put(`/notifications/${id}/read`)
}

export const markAllAsRead = () => {
  return request.put('/notifications/read-all')
}

export const toggleStar = (id: number) => {
  return request.put(`/notifications/${id}/star`)
}

export const deleteNotification = (id: number) => {
  return request.delete(`/notifications/${id}`)
}

export const createNotification = (data: CreateNotificationRequest) => {
  return request.post('/notifications', data)
}

export const getNotificationPreferences = () => {
  return request.get('/notifications/preferences')
}

export const updateNotificationPreferences = (data: UpdateNotificationPreferenceRequest) => {
  return request.put('/notifications/preferences', data)
}
