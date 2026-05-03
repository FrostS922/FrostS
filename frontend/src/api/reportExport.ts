import request from '@/utils/request'

export interface ReportExportData {
  overview: {
    totalReports: number
    todayReports: number
    metrics: {
      metricName: string
      avgValue: number
      minValue: number
      maxValue: number
      count: number
      poorCount: number
    }[]
  }
  trend: {
    dates: string[]
    metrics: {
      metricName: string
      values: number[]
    }[]
  }
  percentiles: {
    metrics: {
      metricName: string
      p50: number
      p75: number
      p90: number
      p95: number
      p99: number
      sampleCount: number
    }[]
  }
  diagnosis: {
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
  errorStats: {
    totalErrors: number
    todayErrors: number
    topCategories: {
      category: string
      count: number
    }[]
  }
}

export const getReportExportData = (params: {
  startTime: string
  endTime: string
}) => {
  return request.get('/report/export-data', { params }) as unknown as Promise<{
    code: number
    data: ReportExportData
  }>
}
