import request from '@/utils/request'

export interface AuditLogItem {
  id: number
  action: string
  target: string
  targetId: string | null
  operator: string
  operatorIp: string | null
  oldValue: string | null
  newValue: string | null
  operatedAt: string
  description: string | null
}

export interface AuditLogPage {
  content: AuditLogItem[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const getAuditLogs = (params: { page?: number; size?: number; action?: string; operator?: string }) => {
  return request.get('/audit-logs', { params }) as unknown as Promise<{ code: number; data: AuditLogPage }>
}
