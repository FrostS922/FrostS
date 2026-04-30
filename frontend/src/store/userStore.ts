import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface User {
  username: string
  realName: string
  email: string
  roles: string[]
  token: string
}

interface UserState {
  user: User | null
  setUser: (user: User | null) => void
  isAuthenticated: boolean
  login: (user: User) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      setUser: (user) => set({ user }),
      login: (user) => {
        localStorage.setItem('token', user.token)
        set({ user, isAuthenticated: true })
      },
      logout: () => {
        localStorage.removeItem('token')
        set({ user: null, isAuthenticated: false })
      },
    }),
    {
      name: 'user-storage',
    }
  )
)
