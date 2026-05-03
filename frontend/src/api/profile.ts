import request from '@/utils/request'

export interface ProfileData {
  id: number
  username: string
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  department?: string
  position?: string
  enabled: boolean
  accountNonLocked: boolean
  lastLoginAt?: string
  lastLoginIp?: string
  loginCount: number
  passwordChangedAt?: string
  roles: string[]
  createdAt?: string
  updatedAt?: string
}

export interface UpdateProfileData {
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  department?: string
  position?: string
}

export interface ChangePasswordData {
  oldPassword: string
  newPassword: string
}

export interface LoginHistoryItem {
  id: number
  loginAt: string
  loginIp: string
  userAgent: string
  success: boolean
  failReason?: string
}

export interface SecurityInfo {
  passwordChangedAt?: string
  accountNonLocked: boolean
  lockReason?: string
  loginFailCount: number
  lastLoginAt?: string
  lastLoginIp?: string
  loginCount: number
  recentLogins: LoginHistoryItem[]
}

export const getProfile = () => {
  return request.get('/user/profile') as unknown as Promise<{ code: number; data: ProfileData }>
}

export const updateProfile = (data: UpdateProfileData) => {
  return request.put('/user/profile', data) as unknown as Promise<{ code: number; data: ProfileData }>
}

export const getCurrentUser = () => {
  return request.get('/auth/me') as unknown as Promise<{ code: number; data: ProfileData }>
}

export const changePassword = (data: ChangePasswordData) => {
  return request.post('/auth/change-password', data) as unknown as Promise<{ code: number; data: null }>
}

export const getPasswordPolicy = () => {
  return request.get('/auth/password-policy') as unknown as Promise<{ code: number; data: { minLength: number; complexity: string } }>
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/avatars', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<{ code: number; data: { url: string } }>
}

export const getSecurityInfo = () => {
  return request.get('/user/security-info') as unknown as Promise<{ code: number; data: SecurityInfo }>
}
