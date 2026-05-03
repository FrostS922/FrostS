import request from '@/utils/request'

export interface AlertRuleItem {
  id: number
  name: string
  ruleType: string
  enabled: boolean
  metricName: string | null
  conditionType: string | null
  threshold: number | null
  comparator: string | null
  windowMinutes: number | null
  minSampleCount: number | null
  notifyType: string | null
  priority: string | null
  cooldownMinutes: number | null
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface AlertRulePreview {
  currentValue: number | null
  threshold: number
  wouldTrigger: boolean
  sampleCount: number
  message: string
}

export interface CreateAlertRuleParams {
  name: string
  ruleType: string
  metricName?: string
  conditionType?: string
  threshold?: number
  comparator?: string
  windowMinutes?: number
  minSampleCount?: number
  notifyType?: string
  priority?: string
  cooldownMinutes?: number
  description?: string
}

export const getAlertRules = () => {
  return request.get('/alert-rules') as unknown as Promise<{
    code: number
    data: AlertRuleItem[]
  }>
}

export const createAlertRule = (data: CreateAlertRuleParams) => {
  return request.post('/alert-rules', data) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const updateAlertRule = (id: number, data: Partial<CreateAlertRuleParams>) => {
  return request.put(`/alert-rules/${id}`, data) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const deleteAlertRule = (id: number) => {
  return request.delete(`/alert-rules/${id}`) as unknown as Promise<{
    code: number
  }>
}

export const toggleAlertRule = (id: number) => {
  return request.patch(`/alert-rules/${id}/toggle`) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const previewAlertRule = (data: CreateAlertRuleParams) => {
  return request.post('/alert-rules/preview', data) as unknown as Promise<{
    code: number
    data: AlertRulePreview
  }>
}
