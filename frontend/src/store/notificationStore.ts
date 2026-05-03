import { create } from 'zustand'
import {
  getNotifications,
  getUnreadCount,
  markAsRead as markAsReadApi,
  markAllAsRead as markAllAsReadApi,
  toggleStar as toggleStarApi,
  deleteNotification as deleteNotificationApi,
  getNotificationPreferences,
  updateNotificationPreferences,
  type NotificationResponse,
  type UnreadCountResponse,
  type NotificationPreferenceResponse,
  type UpdateNotificationPreferenceRequest,
} from '@/api/notification'

interface NotificationState {
  notifications: NotificationResponse[]
  total: number
  unreadCount: UnreadCountResponse
  preferences: NotificationPreferenceResponse | null
  loading: boolean
  currentPage: number
  pageSize: number
  currentType: string | undefined
  currentIsRead: boolean | undefined

  fetchNotifications: (page?: number, size?: number) => Promise<void>
  fetchUnreadCount: () => Promise<void>
  markAsRead: (id: number) => Promise<void>
  markAllAsRead: () => Promise<void>
  toggleStar: (id: number) => Promise<void>
  deleteNotification: (id: number) => Promise<void>
  fetchPreferences: () => Promise<void>
  updatePreferences: (data: UpdateNotificationPreferenceRequest) => Promise<void>
  setCurrentPage: (page: number) => void
  setPageSize: (size: number) => void
  setCurrentType: (type: string | undefined) => void
  setCurrentIsRead: (isRead: boolean | undefined) => void
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  total: 0,
  unreadCount: { total: 0, systemCount: 0, businessCount: 0, reminderCount: 0, todoCount: 0 },
  preferences: null,
  loading: false,
  currentPage: 0,
  pageSize: 10,
  currentType: undefined,
  currentIsRead: undefined,

  fetchNotifications: async (page?: number, size?: number) => {
    set({ loading: true })
    try {
      const state = get()
      const res: any = await getNotifications({
        type: state.currentType,
        isRead: state.currentIsRead,
        page: page ?? state.currentPage,
        size: size ?? state.pageSize,
      })
      set({
        notifications: res.data?.content || res.data || [],
        total: res.data?.totalElements || res.total || 0,
        currentPage: page ?? state.currentPage,
        pageSize: size ?? state.pageSize,
      })
    } finally {
      set({ loading: false })
    }
  },

  fetchUnreadCount: async () => {
    try {
      const res: any = await getUnreadCount()
      set({ unreadCount: res.data || res })
    } catch {
      // silently fail for polling
    }
  },

  markAsRead: async (id: number) => {
    await markAsReadApi(id)
    const state = get()
    set({
      notifications: state.notifications.map(n =>
        n.id === id ? { ...n, isRead: true } : n
      ),
    })
    get().fetchUnreadCount()
  },

  markAllAsRead: async () => {
    await markAllAsReadApi()
    set({
      notifications: get().notifications.map(n => ({ ...n, isRead: true })),
    })
    get().fetchUnreadCount()
  },

  toggleStar: async (id: number) => {
    await toggleStarApi(id)
    set({
      notifications: get().notifications.map(n =>
        n.id === id ? { ...n, isStarred: !n.isStarred } : n
      ),
    })
  },

  deleteNotification: async (id: number) => {
    await deleteNotificationApi(id)
    set({
      notifications: get().notifications.filter(n => n.id !== id),
      total: get().total - 1,
    })
    get().fetchUnreadCount()
  },

  fetchPreferences: async () => {
    const res: any = await getNotificationPreferences()
    set({ preferences: res.data || res })
  },

  updatePreferences: async (data: UpdateNotificationPreferenceRequest) => {
    const res: any = await updateNotificationPreferences(data)
    set({ preferences: res.data || res })
  },

  setCurrentPage: (page: number) => set({ currentPage: page }),
  setPageSize: (size: number) => set({ pageSize: size }),
  setCurrentType: (type: string | undefined) => set({ currentType: type }),
  setCurrentIsRead: (isRead: boolean | undefined) => set({ currentIsRead: isRead }),
}))
