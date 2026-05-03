import request from '@/utils/request'

export interface ErrorLogItem {
  id: number
  errorMessage: string
  stackTrace: string | null
  pageUrl: string | null
  userAgent: string | null
  httpStatus: number | null
  fallbackMessage: string | null
  extraInfo: string | null
  category: string | null
  createdAt: string
}

export interface ErrorTrendData {
  dates: string[]
  counts: number[]
  total: number
}

export interface ErrorAggregationItem {
  errorMessage: string
  count: number
  lastSeen: string
  firstSeen: string
}

export interface ErrorOverviewData {
  totalErrors: number
  todayErrors: number
  recentErrors: {
    id: number
    errorMessage: string
    httpStatus: number | null
    pageUrl: string | null
    createdAt: string
  }[]
}

export const getErrorsByMessage = (params: {
  errorMessage: string
  page?: number
  size?: number
}) => {
  return request.get('/error-report/by-message', { params }) as unknown as Promise<{
    code: number
    data: ErrorLogItem[]
    total: number
  }>
}

export const getErrorOverview = () => {
  return request.get('/error-report/overview') as unknown as Promise<{
    code: number
    data: ErrorOverviewData
  }>
}

export const getErrorLogs = (params: {
  page?: number
  size?: number
  keyword?: string
  startTime?: string
  endTime?: string
}) => {
  return request.get('/error-report', { params }) as unknown as Promise<{
    code: number
    data: ErrorLogItem[]
    total: number
  }>
}

export const getAggregatedErrors = (params: {
  page?: number
  size?: number
  keyword?: string
  startTime?: string
  endTime?: string
}) => {
  return request.get('/error-report/aggregated', { params }) as unknown as Promise<{
    code: number
    data: ErrorAggregationItem[]
    total: number
  }>
}

export const getErrorTrend = (days?: number) => {
  return request.get('/error-report/trend', { params: { days } }) as unknown as Promise<{
    code: number
    data: ErrorTrendData
  }>
}

export const exportErrorLogs = (params: {
  keyword?: string
  startTime?: string
  endTime?: string
  format?: string
}) => {
  return request.get('/error-report/export', {
    params,
    responseType: 'blob',
  }) as unknown as Promise<Blob>
}

export const deleteErrorLog = (id: number) => {
  return request.delete(`/error-report/${id}`) as unknown as Promise<{
    code: number
  }>
}

export const batchDeleteErrorLogs = (ids: number[]) => {
  return request.delete('/error-report/batch', { data: { ids } }) as unknown as Promise<{
    code: number
  }>
}
