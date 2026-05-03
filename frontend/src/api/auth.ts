import request from '@/utils/request'

export const login = (data: { username: string; password: string }) => {
  return request.post('/auth/login', data)
}

export const register = (data: {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  captchaKey: string
  captchaCode: string
}) => {
  return request.post('/auth/register', data)
}

export const refreshToken = (refreshToken: string) => {
  return request.post('/auth/refresh', { refreshToken })
}

export const getRegistrationStatus = () => {
  return request.get('/auth/registration-status') as unknown as Promise<{ code: number; data: boolean }>
}

export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.post('/auth/change-password', data) as unknown as Promise<{ code: number; data: null }>
}

export const getPasswordPolicy = () => {
  return request.get('/auth/password-policy') as unknown as Promise<{ code: number; data: { minLength: number; complexity: string } }>
}

export const getCaptcha = () => {
  return request.get('/auth/captcha') as unknown as Promise<{ code: number; data: { captchaKey: string; captchaImage: string } }>
}

export const getCurrentUser = () => {
  return request.get('/auth/me') as unknown as Promise<{ code: number; data: import('./profile').ProfileData }>
}

export interface MfaSetupData {
  secret: string
  otpAuthUrl: string
  qrCodeBase64: string
  backupCodes: string[]
}

export const setupMfa = () => {
  return request.post('/auth/mfa/setup') as unknown as Promise<{ code: number; data: MfaSetupData }>
}

export const verifyMfaSetup = (code: string) => {
  return request.post('/auth/mfa/verify-setup', { code }) as unknown as Promise<{ code: number; data: null }>
}

export const verifyMfaLogin = (code: string, mfaToken: string) => {
  return request.post('/auth/mfa/verify', { code, mfaToken })
}

export const disableMfa = (password: string) => {
  return request.delete('/auth/mfa', { data: { password } }) as unknown as Promise<{ code: number; data: null }>
}

export const getMfaStatus = () => {
  return request.get('/auth/mfa/status') as unknown as Promise<{ code: number; data: { enabled: boolean } }>
}
