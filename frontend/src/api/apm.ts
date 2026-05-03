import request from '@/utils/request'

export interface ServiceMetric {
  serviceName: string
  avgResponseTime: number
  p99: number
  throughput: number
  errorRate: number
  healthStatus: string
}

export interface TraceItem {
  traceId: string
  serviceName: string
  endpoint: string
  duration: number
  spanCount: number
  status: string
  startTime: string
  isError: boolean
}

export interface TraceSpan {
  spanId: string
  parentSpanId: string
  operationName: string
  startTime: number
  duration: number
  status: string
  serviceCode: string
  tags: Record<string, string>
}

export interface TopologyNode {
  name: string
  type: string
  healthStatus: string
}

export interface TopologyEdge {
  source: string
  target: string
  callCount: number
  errorRate: number
}

export const getApmServices = (duration?: string) => {
  return request.get('/apm/services', { params: { duration } }) as unknown as Promise<{
    code: number
    data: ServiceMetric[]
  }>
}

export const getServiceMetrics = (serviceName: string, duration?: string) => {
  return request.get(`/apm/service/${encodeURIComponent(serviceName)}/metrics`, { params: { duration } }) as unknown as Promise<{
    code: number
    data: ServiceMetric
  }>
}

export const queryTraces = (params: {
  serviceName?: string
  startTime?: string
  endTime?: string
  minDuration?: number
  traceId?: string
}) => {
  return request.get('/apm/traces', { params }) as unknown as Promise<{
    code: number
    data: { traces: TraceItem[] }
  }>
}

export const getTraceDetail = (traceId: string) => {
  return request.get(`/apm/traces/${encodeURIComponent(traceId)}`) as unknown as Promise<{
    code: number
    data: { traceId: string; spans: TraceSpan[] }
  }>
}

export const getServiceTopology = (duration?: string) => {
  return request.get('/apm/topology', { params: { duration } }) as unknown as Promise<{
    code: number
    data: { nodes: TopologyNode[]; edges: TopologyEdge[] }
  }>
}
