type ErrorReportPayload = {
  message: string
  stack?: string
  url: string
  timestamp: number
  userAgent: string
  extra?: Record<string, unknown>
}

type PerformanceReportPayload = {
  name: string
  value: number
  rating?: string
  url: string
  userAgent: string
  extra?: Record<string, unknown>
}

const ERROR_ENDPOINT = '/api/error-report'
const PERFORMANCE_ENDPOINT = '/api/performance-report'
const DEDUP_INTERVAL = 5000
const recentReports = new Map<string, number>()

function isProduction(): boolean {
  return import.meta.env.PROD
}

function isReportEnabled(): boolean {
  const envFlag = import.meta.env.VITE_ERROR_REPORT_ENABLED
  if (envFlag === 'false') return false
  if (envFlag === 'true') return true
  return isProduction()
}

function getSamplingRate(): number {
  const rate = import.meta.env.VITE_PERF_SAMPLING_RATE
  if (rate != null) {
    const n = parseInt(rate, 10)
    if (!isNaN(n) && n >= 1 && n <= 100) return n
  }
  return 100
}

function shouldSample(): boolean {
  const rate = getSamplingRate()
  if (rate >= 100) return true
  return Math.random() * 100 < rate
}

function buildPayload(err: any, fallback: string, extra?: Record<string, unknown>): ErrorReportPayload {
  const message = err?.response?.data?.message || err?.message || fallback
  const traceId = window.__FROSTS_TRACE_ID__
  return {
    message,
    stack: err?.stack,
    url: window.location.href,
    timestamp: Date.now(),
    userAgent: navigator.userAgent,
    extra: {
      status: err?.response?.status,
      fallback,
      ...(traceId ? { traceId } : {}),
      ...extra,
    },
  }
}

function dedupKey(payload: ErrorReportPayload): string {
  return `${payload.message}::${payload.url}`
}

function sendReport(payload: ErrorReportPayload): void {
  const key = dedupKey(payload)
  const now = Date.now()
  const lastTime = recentReports.get(key)
  if (lastTime && now - lastTime < DEDUP_INTERVAL) {
    return
  }
  recentReports.set(key, now)

  const body = JSON.stringify(payload)
  if (navigator.sendBeacon) {
    navigator.sendBeacon(ERROR_ENDPOINT, body)
  } else {
    fetch(ERROR_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      keepalive: true,
    }).catch(() => {})
  }
}

function sendPerformanceReport(payload: PerformanceReportPayload): void {
  const body = JSON.stringify(payload)
  if (navigator.sendBeacon) {
    navigator.sendBeacon(PERFORMANCE_ENDPOINT, body)
  } else {
    fetch(PERFORMANCE_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      keepalive: true,
    }).catch(() => {})
  }
}

function report(err: any, fallback: string, extra?: Record<string, unknown>): void {
  const payload = buildPayload(err, fallback, extra)
  console.error('[FrostS Error]', payload.message, err)

  if (isReportEnabled()) {
    try {
      sendReport(payload)
    } catch {
    }
  }
}

function reportGlobalError(message: string, stack?: string, extra?: Record<string, unknown>): void {
  report({ message, stack }, '全局未捕获错误', extra)
}

function installGlobalHandlers(): void {
  const originalOnError = window.onerror
  window.onerror = (msg, source, lineno, colno, error) => {
    reportGlobalError(
      typeof msg === 'string' ? msg : String(msg),
      error?.stack || (source ? `at ${source}:${lineno}:${colno}` : undefined),
      { type: 'window.onerror', source, lineno, colno }
    )
    if (originalOnError) {
      originalOnError(msg, source, lineno, colno, error)
    }
  }

  const originalOnUnhandledRejection = window.onunhandledrejection
  window.onunhandledrejection = (event: PromiseRejectionEvent) => {
    const reason = event.reason
    reportGlobalError(
      reason?.message || String(reason),
      reason?.stack,
      { type: 'unhandledrejection' }
    )
    if (originalOnUnhandledRejection) {
      originalOnUnhandledRejection.call(window, event)
    }
  }
}

function reportWebVital(metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }): void {
  if (!isReportEnabled()) return
  if (!shouldSample()) return

  const traceId = window.__FROSTS_TRACE_ID__
  const payload: PerformanceReportPayload = {
    name: metric.name,
    value: Math.round(metric.value * 100) / 100,
    rating: metric.rating,
    url: window.location.href,
    userAgent: navigator.userAgent,
    extra: {
      delta: metric.delta,
      navigationType: metric.navigationType,
      ...(traceId ? { traceId } : {}),
    },
  }

  try {
    sendPerformanceReport(payload)
  } catch {
  }
}

function reportCustomMetric(name: string, value: number, extra?: Record<string, unknown>): void {
  if (!isReportEnabled()) return

  const payload: PerformanceReportPayload = {
    name,
    value: Math.round(value * 100) / 100,
    url: window.location.href,
    userAgent: navigator.userAgent,
    extra,
  }

  try {
    sendPerformanceReport(payload)
  } catch {
  }
}

const errorReporter = { report, reportGlobalError, installGlobalHandlers, reportWebVital, reportCustomMetric }
export default errorReporter
