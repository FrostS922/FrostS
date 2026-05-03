import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import errorReporter from './errorReporter'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

const API_PERF_THRESHOLD = 3000

let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function onTokenRefreshed(newToken: string) {
  refreshSubscribers.forEach((cb) => cb(newToken))
  refreshSubscribers = []
}

function addRefreshSubscriber(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

function redirectToLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    ;(config as any)._startTime = Date.now()
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const traceId = response.headers['x-trace-id'] || response.headers['sw-traceid']
    if (traceId) {
      window.__FROSTS_TRACE_ID__ = Array.isArray(traceId) ? traceId[0] : traceId
    }
    reportApiPerf(response.config, response.status)
    return response.data
  },
  async (error: AxiosError) => {
    if (error.config) {
      reportApiPerf(error.config, error.response?.status)
    }
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      const storedRefreshToken = localStorage.getItem('refreshToken')

      if (!storedRefreshToken) {
        redirectToLogin()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          addRefreshSubscriber((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await axios.post('/api/auth/refresh', {
          refreshToken: storedRefreshToken,
        })

        const data = response.data
        const newToken = data.token
        const newRefreshToken = data.refreshToken

        localStorage.setItem('token', newToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        onTokenRefreshed(newToken)

        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return request(originalRequest)
      } catch (refreshError) {
        redirectToLogin()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status === 401 && originalRequest._retry) {
      redirectToLogin()
    }

    return Promise.reject(error)
  }
)

export default request

function reportApiPerf(config: InternalAxiosRequestConfig, httpStatus?: number) {
  const startTime = (config as any)?._startTime
  if (!startTime) return

  const duration = Date.now() - startTime
  if (duration < API_PERF_THRESHOLD) return

  const url = config.url || ''
  const skipPaths = ['/auth/refresh', '/error-report', '/performance-report']
  if (skipPaths.some((p) => url.includes(p))) return

  errorReporter.reportCustomMetric('API_RESPONSE_TIME', duration, {
    method: config.method?.toUpperCase(),
    url: url.substring(0, 200),
    httpStatus: httpStatus || 0,
  })
}
