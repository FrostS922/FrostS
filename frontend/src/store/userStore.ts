import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { ProfileData } from '../api/profile'

interface User {
  username: string
  realName: string
  email: string
  roles: string[]
  token: string
  refreshToken: string
  mustChangePassword?: boolean
  phone?: string
  avatar?: string
  department?: string
  position?: string
}

interface UserState {
  user: User | null
  profile: ProfileData | null
  setUser: (user: User | null) => void
  setProfile: (profile: ProfileData | null) => void
  isAuthenticated: boolean
  mustChangePassword: boolean
  login: (user: User) => void
  logout: () => void
  clearMustChangePassword: () => void
  updateTokens: (token: string, refreshToken: string) => void
  updateProfileFromData: (profile: ProfileData) => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      user: null,
      profile: null,
      isAuthenticated: false,
      mustChangePassword: false,
      setUser: (user) => set({ user }),
      setProfile: (profile) => set({ profile }),
      login: (user) => {
        localStorage.setItem('token', user.token)
        localStorage.setItem('refreshToken', user.refreshToken)
        set({ user, isAuthenticated: true, mustChangePassword: user.mustChangePassword === true })
      },
      logout: () => {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        set({ user: null, profile: null, isAuthenticated: false, mustChangePassword: false })
      },
      clearMustChangePassword: () => {
        set((state) => ({
          mustChangePassword: false,
          user: state.user ? { ...state.user, mustChangePassword: false } : null,
        }))
      },
      updateTokens: (token, refreshToken) => {
        localStorage.setItem('token', token)
        localStorage.setItem('refreshToken', refreshToken)
        set((state) => ({
          user: state.user ? { ...state.user, token, refreshToken } : null,
        }))
      },
      updateProfileFromData: (profile) => {
        set((state) => ({
          profile,
          user: state.user
            ? {
                ...state.user,
                realName: profile.realName || state.user.realName,
                email: profile.email || state.user.email,
                phone: profile.phone,
                avatar: profile.avatar,
                department: profile.department,
                position: profile.position,
              }
            : null,
        }))
      },
    }),
    {
      name: 'user-storage',
    }
  )
)
