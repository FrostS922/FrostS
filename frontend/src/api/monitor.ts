import request from '@/utils/request'

export interface MetricSnapshot {
  metricName: string
  avgValue: number
  count: number
  poorCount: number
}

export interface RecentError {
  errorMessage: string
  category: string
  createdAt: string
}

export interface RealtimeSummary {
  performance: {
    totalReports: number
    todayReports: number
    metrics: MetricSnapshot[]
  }
  errors: {
    totalErrors: number
    todayErrors: number
    recentErrors: RecentError[]
  }
  security: {
    todayLoginSuccesses: number
    todayLoginFailures: number
    todayAnomalousIps: number
    lockedAccounts: number
    bannedIps: number
  }
}

export interface RecentAlert {
  id: number
  title: string
  content: string
  priority: string
  createdAt: string
}

export const getRealtimeSummary = () => {
  return request.get('/monitor/realtime-summary') as unknown as Promise<{
    code: number
    data: RealtimeSummary
  }>
}

export const getRecentAlerts = () => {
  return request.get('/monitor/alerts/recent') as unknown as Promise<{
    code: number
    data: RecentAlert[]
  }>
}
