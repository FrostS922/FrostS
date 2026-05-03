import request from '@/utils/request'

export interface PerformanceLogItem {
  id: number
  metricName: string
  metricValue: number
  rating: string | null
  pageUrl: string | null
  userAgent: string | null
  extraInfo: string | null
  createdAt: string
}

export interface PerformanceMetricStat {
  metricName: string
  avgValue: number
  minValue: number
  maxValue: number
  count: number
  poorCount: number
}

export interface PerformanceOverviewData {
  totalReports: number
  todayReports: number
  metrics: PerformanceMetricStat[]
}

export interface PerformanceTrendPoint {
  metricName: string
  values: number[]
}

export interface PerformanceTrendData {
  dates: string[]
  metrics: PerformanceTrendPoint[]
}

export interface MetricPercentile {
  metricName: string
  p50: number
  p75: number
  p90: number
  p95: number
  p99: number
  sampleCount: number
}

export interface PerformancePercentileData {
  metrics: MetricPercentile[]
}

export interface PerformanceCompareData {
  labels: string[]
  metrics: {
    metricName: string
    avgValues: (number | null)[]
    sampleCounts: number[]
  }[]
}

export const getPerformanceLogs = (params: {
  metricName?: string
  page?: number
  size?: number
  startTime?: string
  endTime?: string
}) => {
  return request.get('/performance-report', { params }) as unknown as Promise<{
    code: number
    data: PerformanceLogItem[]
    total: number
  }>
}

export const getPerformanceOverview = () => {
  return request.get('/performance-report/overview') as unknown as Promise<{
    code: number
    data: PerformanceOverviewData
  }>
}

export const getPerformanceTrend = (days?: number) => {
  return request.get('/performance-report/trend', { params: { days } }) as unknown as Promise<{
    code: number
    data: PerformanceTrendData
  }>
}

export const deletePerformanceLog = (id: number) => {
  return request.delete(`/performance-report/${id}`) as unknown as Promise<{
    code: number
  }>
}

export const getPerformancePercentiles = (days?: number) => {
  return request.get('/performance-report/percentiles', { params: { days } }) as unknown as Promise<{
    code: number
    data: PerformancePercentileData
  }>
}

export interface DiagnosisData {
  startTime: string
  endTime: string
  errorSummary: {
    totalErrors: number
    topCategories: string[]
    topMessages: string[]
  }
  performanceSummary: {
    totalPoorMetrics: number
    poorMetrics: {
      metricName: string
      avgValue: number
      poorCount: number
      totalCount: number
    }[]
  }
  correlations: string[]
  conclusion: string
}

export const comparePerformance = (params: {
  baseStart: string
  baseEnd: string
  compareStart: string
  compareEnd: string
}) => {
  return request.get('/performance-report/compare', { params }) as unknown as Promise<{
    code: number
    data: PerformanceCompareData
  }>
}

export const diagnosePerformance = (params: {
  startTime: string
  endTime: string
}) => {
  return request.get('/performance-report/diagnose', { params }) as unknown as Promise<{
    code: number
    data: DiagnosisData
  }>
}
