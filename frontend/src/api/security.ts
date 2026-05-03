import request from '@/utils/request'

export interface SecurityOverview {
  todayLoginSuccesses: number
  todayLoginFailures: number
  todayAnomalousIps: number
  lockedAccounts: number
  bannedIps: number
}

export interface BannedIp {
  ip: string
  bannedAt: string
  remainingSeconds: string
}

export interface LoginTrendPoint {
  date: string
  successes: number
  failures: number
}

export const getSecurityOverview = () => {
  return request.get('/security/overview') as unknown as Promise<{ code: number; data: SecurityOverview }>
}

export const getBannedIps = () => {
  return request.get('/security/banned-ips') as unknown as Promise<{ code: number; data: BannedIp[] }>
}

export const unbanIp = (ip: string) => {
  return request.delete(`/security/banned-ips/${ip}`) as unknown as Promise<{ code: number; data: null }>
}

export const getLoginTrend = () => {
  return request.get('/security/login-trend') as unknown as Promise<{ code: number; data: LoginTrendPoint[] }>
}

export interface SessionInfo {
  id: number
  deviceInfo: string
  clientIp: string
  createdAt: string
  lastRefreshedAt: string
  current: boolean
}

export const getSessions = () => {
  return request.get('/security/sessions') as unknown as Promise<{ code: number; data: SessionInfo[] }>
}

export const terminateSession = (id: number) => {
  return request.delete(`/security/sessions/${id}`) as unknown as Promise<{ code: number; data: null }>
}

export const terminateAllOtherSessions = () => {
  return request.delete('/security/sessions') as unknown as Promise<{ code: number; data: null }>
}

export const sendWeeklyReport = () => {
  return request.post('/security/weekly-report/send') as unknown as Promise<{ code: number; data: null }>
}

export const previewWeeklyReport = () => {
  return request.get('/security/weekly-report/preview') as unknown as Promise<{ code: number; data: string }>
}
