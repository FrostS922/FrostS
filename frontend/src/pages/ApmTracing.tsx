import React, { useState, useEffect, useCallback } from 'react'
import { Tabs, Table, Tag, Select, Input, InputNumber, DatePicker, Button, Space, Typography, Spin, Empty, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { NetworkGraph } from '@ant-design/charts'
import type { Dayjs } from 'dayjs'
import {
  getApmServices,
  queryTraces,
  getTraceDetail,
  getServiceTopology,
  type ServiceMetric,
  type TraceItem,
  type TraceSpan,
  type TopologyNode,
  type TopologyEdge,
} from '@/api/apm'

const { RangePicker } = DatePicker
const { Text } = Typography

const durationOptions = [
  { value: 'DAY', label: '最近一天' },
  { value: 'WEEK', label: '最近一周' },
  { value: 'MONTH', label: '最近一月' },
]

const healthTagProps = (status: string) => {
  switch (status) {
    case 'HEALTHY': return { color: 'green' as const, text: '健康' }
    case 'UNHEALTHY': return { color: 'red' as const, text: '不健康' }
    default: return { color: 'default' as const, text: '未知' }
  }
}

const WaterfallChart: React.FC<{ spans: TraceSpan[] }> = ({ spans }) => {
  if (spans.length === 0) return <Empty description="暂无Span数据" />

  const sortedSpans = [...spans].sort((a, b) => a.startTime - b.startTime)
  const minStart = sortedSpans[0].startTime
  const maxEnd = Math.max(...sortedSpans.map(s => s.startTime + s.duration))
  const totalDuration = maxEnd - minStart || 1

  const spanMap = new Map(sortedSpans.map(s => [s.spanId, s]))
  const depthMap = new Map<string, number>()

  const getDepth = (span: TraceSpan): number => {
    if (depthMap.has(span.spanId)) return depthMap.get(span.spanId)!
    if (!span.parentSpanId || !spanMap.has(span.parentSpanId)) {
      depthMap.set(span.spanId, 0)
      return 0
    }
    const depth = getDepth(spanMap.get(span.parentSpanId)!) + 1
    depthMap.set(span.spanId, depth)
    return depth
  }

  sortedSpans.forEach(s => getDepth(s))

  return (
    <div style={{ overflowX: 'auto' }}>
      {sortedSpans.map(span => {
        const depth = depthMap.get(span.spanId) || 0
        const left = ((span.startTime - minStart) / totalDuration) * 100
        const width = Math.max((span.duration / totalDuration) * 100, 0.5)
        const isError = span.status === 'ERROR'

        return (
          <div key={span.spanId} style={{ display: 'flex', alignItems: 'center', marginBottom: 4, height: 28 }}>
            <div style={{ width: 300, paddingLeft: depth * 20, flexShrink: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              <Text style={{ fontSize: 12 }}>[{span.serviceCode}] {span.operationName}</Text>
            </div>
            <div style={{ flex: 1, position: 'relative', height: 20 }}>
              <div style={{
                position: 'absolute',
                left: `${left}%`,
                width: `${width}%`,
                height: '100%',
                backgroundColor: isError ? '#ff4d4f' : '#1677ff',
                borderRadius: 3,
                minWidth: 2,
              }} />
            </div>
            <div style={{ width: 80, textAlign: 'right', flexShrink: 0 }}>
              <Text style={{ fontSize: 12 }}>{span.duration}ms</Text>
            </div>
          </div>
        )
      })}
    </div>
  )
}

const TopologyGraph: React.FC<{ nodes: TopologyNode[]; edges: TopologyEdge[] }> = ({ nodes, edges }) => {
  if (nodes.length === 0) return <Empty description="暂无拓扑数据" />

  const graphData = {
    nodes: nodes.map(n => ({
      id: n.name,
      data: { type: n.type, healthStatus: n.healthStatus },
    })),
    edges: edges.map(e => ({
      source: e.source,
      target: e.target,
      data: { callCount: e.callCount, errorRate: e.errorRate },
    })),
  }

  const config = {
    data: graphData,
    node: {
      style: (d: any) => ({
        fill: d.data?.healthStatus === 'HEALTHY' ? '#52c41a' : d.data?.healthStatus === 'UNHEALTHY' ? '#ff4d4f' : '#d9d9d9',
        stroke: '#fff',
        lineWidth: 1,
      }),
    },
    edge: {
      style: (d: any) => {
        const errorRate = d.data?.errorRate || 0
        const r = Math.round(255 * Math.min(errorRate, 1))
        const g = Math.round(255 * (1 - Math.min(errorRate, 1)))
        return {
          stroke: `rgb(${r}, ${g}, 0)`,
          lineWidth: Math.max(1, Math.min((d.data?.callCount || 0) / 100, 8)),
        }
      },
    },
    layout: {
      type: 'd3-force',
      link: { distance: 128 },
      collide: { radius: 32 },
      manyBody: { strength: -400 },
    },
  }

  return <NetworkGraph {...config} />
}

const ApmTracing: React.FC = () => {
  const [activeTab, setActiveTab] = useState('services')
  const [duration, setDuration] = useState('DAY')
  const [services, setServices] = useState<ServiceMetric[]>([])
  const [servicesLoading, setServicesLoading] = useState(false)
  const [traceServiceName, setTraceServiceName] = useState('')
  const [traceIdInput, setTraceIdInput] = useState('')
  const [minDuration, setMinDuration] = useState<number | null>(null)
  const [timeRange, setTimeRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [traces, setTraces] = useState<TraceItem[]>([])
  const [tracesLoading, setTracesLoading] = useState(false)
  const [detailTraceId, setDetailTraceId] = useState('')
  const [spans, setSpans] = useState<TraceSpan[]>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [topoDuration, setTopoDuration] = useState('DAY')
  const [topoNodes, setTopoNodes] = useState<TopologyNode[]>([])
  const [topoEdges, setTopoEdges] = useState<TopologyEdge[]>([])
  const [topoLoading, setTopoLoading] = useState(false)

  const loadServices = useCallback(async () => {
    setServicesLoading(true)
    try {
      const res = await getApmServices(duration)
      if (res.code === 200 && res.data) {
        setServices(res.data)
      }
    } catch {
      message.error('加载服务列表失败')
    } finally {
      setServicesLoading(false)
    }
  }, [duration])

  useEffect(() => {
    loadServices()
  }, [loadServices])

  const handleSearchTraces = async () => {
    setTracesLoading(true)
    try {
      const params: Record<string, any> = {}
      if (traceServiceName) params.serviceName = traceServiceName
      if (traceIdInput) params.traceId = traceIdInput
      if (minDuration != null) params.minDuration = minDuration
      if (timeRange) {
        params.startTime = timeRange[0].toISOString()
        params.endTime = timeRange[1].toISOString()
      }
      const res = await queryTraces(params)
      if (res.code === 200 && res.data) {
        setTraces(res.data.traces)
      }
    } catch {
      message.error('查询链路失败')
    } finally {
      setTracesLoading(false)
    }
  }

  const loadTraceDetail = useCallback(async (tid: string) => {
    setDetailLoading(true)
    try {
      const res = await getTraceDetail(tid)
      if (res.code === 200 && res.data) {
        setSpans(res.data.spans)
      }
    } catch {
      message.error('加载链路详情失败')
    } finally {
      setDetailLoading(false)
    }
  }, [])

  useEffect(() => {
    if (detailTraceId) {
      loadTraceDetail(detailTraceId)
    }
  }, [detailTraceId, loadTraceDetail])

  const loadTopology = useCallback(async () => {
    setTopoLoading(true)
    try {
      const res = await getServiceTopology(topoDuration)
      if (res.code === 200 && res.data) {
        setTopoNodes(res.data.nodes)
        setTopoEdges(res.data.edges)
      }
    } catch {
      message.error('加载服务拓扑失败')
    } finally {
      setTopoLoading(false)
    }
  }, [topoDuration])

  useEffect(() => {
    loadTopology()
  }, [loadTopology])

  const handleTraceClick = (tid: string) => {
    setDetailTraceId(tid)
    setActiveTab('detail')
  }

  const serviceColumns = [
    { title: '服务名', dataIndex: 'serviceName', key: 'serviceName' },
    { title: '平均响应时间(ms)', dataIndex: 'avgResponseTime', key: 'avgResponseTime' },
    { title: 'P99(ms)', dataIndex: 'p99', key: 'p99' },
    { title: '吞吐量(cpm)', dataIndex: 'throughput', key: 'throughput' },
    { title: '错误率(%)', dataIndex: 'errorRate', key: 'errorRate', render: (v: number) => `${v.toFixed(2)}` },
    {
      title: '健康状态',
      dataIndex: 'healthStatus',
      key: 'healthStatus',
      render: (status: string) => {
        const props = healthTagProps(status)
        return <Tag color={props.color}>{props.text}</Tag>
      },
    },
  ]

  const traceColumns = [
    {
      title: 'TraceID',
      dataIndex: 'traceId',
      key: 'traceId',
      render: (text: string) => (
        <a onClick={() => handleTraceClick(text)}>{text}</a>
      ),
    },
    { title: '服务', dataIndex: 'serviceName', key: 'serviceName' },
    { title: '端点', dataIndex: 'endpoint', key: 'endpoint' },
    { title: '持续时间(ms)', dataIndex: 'duration', key: 'duration' },
    { title: 'Span数', dataIndex: 'spanCount', key: 'spanCount' },
    {
      title: '状态',
      dataIndex: 'isError',
      key: 'isError',
      render: (isError: boolean) => isError
        ? <Tag color="red">错误</Tag>
        : <Tag color="green">正常</Tag>,
    },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  ]

  const tabItems = [
    {
      key: 'services',
      label: '服务列表',
      children: (
        <div>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <span>时间范围:</span>
              <Select value={duration} onChange={setDuration} options={durationOptions} style={{ width: 120 }} />
            </Space>
          </div>
          <Table
            dataSource={services}
            columns={serviceColumns}
            loading={servicesLoading}
            rowKey="serviceName"
            pagination={false}
          />
        </div>
      ),
    },
    {
      key: 'traces',
      label: '链路查询',
      children: (
        <div>
          <div style={{ marginBottom: 16 }}>
            <Space wrap>
              <Input
                placeholder="服务名"
                value={traceServiceName}
                onChange={e => setTraceServiceName(e.target.value)}
                style={{ width: 160 }}
              />
              <Input
                placeholder="TraceID"
                value={traceIdInput}
                onChange={e => setTraceIdInput(e.target.value)}
                style={{ width: 200 }}
              />
              <InputNumber
                placeholder="最小持续时间(ms)"
                value={minDuration}
                onChange={v => setMinDuration(v)}
                style={{ width: 160 }}
              />
              <RangePicker
                value={timeRange}
                onChange={v => setTimeRange(v as [Dayjs, Dayjs] | null)}
                showTime
              />
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearchTraces} loading={tracesLoading}>
                查询
              </Button>
            </Space>
          </div>
          <Table
            dataSource={traces}
            columns={traceColumns}
            loading={tracesLoading}
            rowKey="traceId"
          />
        </div>
      ),
    },
    {
      key: 'detail',
      label: '链路详情',
      children: (
        <div>
          {detailTraceId ? (
            <Spin spinning={detailLoading}>
              <div style={{ marginBottom: 16 }}>
                <Text strong>TraceID: </Text>
                <Text code>{detailTraceId}</Text>
              </div>
              <WaterfallChart spans={spans} />
            </Spin>
          ) : (
            <Empty description="请从链路查询中选择一条链路查看详情" />
          )}
        </div>
      ),
    },
    {
      key: 'topology',
      label: '服务拓扑',
      children: (
        <div>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <span>时间范围:</span>
              <Select value={topoDuration} onChange={setTopoDuration} options={durationOptions} style={{ width: 120 }} />
            </Space>
          </div>
          <Spin spinning={topoLoading}>
            <TopologyGraph nodes={topoNodes} edges={topoEdges} />
          </Spin>
        </div>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
    </div>
  )
}

export default ApmTracing
