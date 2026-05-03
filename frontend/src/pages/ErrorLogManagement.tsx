import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Card, Table, Input, Space, Button, Tag, Typography, Row, Col, Modal, DatePicker, Popconfirm, Statistic, Segmented, Dropdown, Badge, Select, Tabs } from 'antd'
import { SearchOutlined, ReloadOutlined, BugOutlined, DeleteOutlined, EyeOutlined, GlobalOutlined, WarningOutlined, LineChartOutlined, DownloadOutlined, ThunderboltOutlined, SwapOutlined, MedicineBoxOutlined, FilePdfOutlined } from '@ant-design/icons'
import { Line } from '@ant-design/charts'
import { Client, IMessage } from '@stomp/stompjs'
import { getErrorLogs, deleteErrorLog, batchDeleteErrorLogs, getErrorTrend, getAggregatedErrors, exportErrorLogs, getErrorsByMessage } from '../api/errorReport'
import type { ErrorLogItem, ErrorTrendData, ErrorAggregationItem } from '../api/errorReport'
import { getPerformanceLogs, getPerformanceTrend, deletePerformanceLog, getPerformancePercentiles, comparePerformance, diagnosePerformance } from '../api/performanceReport'
import type { PerformanceLogItem, PerformanceTrendData, MetricPercentile, PerformanceCompareData, DiagnosisData } from '../api/performanceReport'
import useMessage from '../hooks/useMessage'
import ReportExportModal from '../components/ReportExportModal'

const { Title, Text, Paragraph } = Typography
const { RangePicker } = DatePicker

const httpStatusColor = (status: number | null) => {
  if (!status) return 'default'
  if (status >= 500) return 'red'
  if (status >= 400) return 'orange'
  if (status >= 300) return 'blue'
  return 'green'
}

const countColor = (count: number) => {
  if (count >= 20) return 'red'
  if (count >= 10) return 'orange'
  if (count >= 5) return 'gold'
  return 'blue'
}

const categoryConfig: Record<string, { label: string; color: string }> = {
  NETWORK: { label: '网络', color: 'geekblue' },
  RESOURCE: { label: '资源加载', color: 'purple' },
  CODE: { label: '代码逻辑', color: 'volcano' },
  AUTH: { label: '认证授权', color: 'magenta' },
  NOT_FOUND: { label: '404', color: 'orange' },
  SERVER: { label: '服务端', color: 'red' },
  OTHER: { label: '其他', color: 'default' },
}

const metricNameConfig: Record<string, { label: string; unit: string; color: string; goodThreshold: number; poorThreshold: number }> = {
  LCP: { label: '最大内容绘制', unit: 'ms', color: '#1677ff', goodThreshold: 2500, poorThreshold: 4000 },
  FID: { label: '首次输入延迟', unit: 'ms', color: '#52c41a', goodThreshold: 100, poorThreshold: 300 },
  CLS: { label: '累积布局偏移', unit: '', color: '#faad14', goodThreshold: 0.1, poorThreshold: 0.25 },
  TTFB: { label: '首字节时间', unit: 'ms', color: '#722ed1', goodThreshold: 800, poorThreshold: 1800 },
  INP: { label: '交互延迟', unit: 'ms', color: '#13c2c2', goodThreshold: 200, poorThreshold: 500 },
  FCP: { label: '首次内容绘制', unit: 'ms', color: '#2f54eb', goodThreshold: 1800, poorThreshold: 3000 },
}

const ratingColor = (rating: string | null) => {
  if (rating === 'good') return 'green'
  if (rating === 'needs-improvement') return 'orange'
  if (rating === 'poor') return 'red'
  return 'default'
}

const ratingLabel = (rating: string | null) => {
  if (rating === 'good') return '良好'
  if (rating === 'needs-improvement') return '需改进'
  if (rating === 'poor') return '较差'
  return '-'
}

const formatMetricValue = (name: string, value: number) => {
  const config = metricNameConfig[name]
  if (!config) return String(Math.round(value))
  if (name === 'CLS') return value.toFixed(3)
  return `${Math.round(value)} ms`
}

const ErrorLogManagement: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'error' | 'performance'>('error')
  const [viewMode, setViewMode] = useState<'detail' | 'aggregated'>('detail')
  const [data, setData] = useState<ErrorLogItem[]>([])
  const [aggData, setAggData] = useState<ErrorAggregationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [dateRange, setDateRange] = useState<[string, string] | null>(null)
  const [detailVisible, setDetailVisible] = useState(false)
  const [currentItem, setCurrentItem] = useState<ErrorLogItem | null>(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [trendData, setTrendData] = useState<ErrorTrendData | null>(null)
  const [newErrorCount, setNewErrorCount] = useState(0)
  const [messageDetailVisible, setMessageDetailVisible] = useState(false)
  const [selectedMessage, setSelectedMessage] = useState('')
  const [messageDetailData, setMessageDetailData] = useState<ErrorLogItem[]>([])
  const [messageDetailLoading, setMessageDetailLoading] = useState(false)
  const [messageDetailTotal, setMessageDetailTotal] = useState(0)
  const wsRef = useRef<Client | null>(null)
  const { showError } = useMessage()

  const [perfData, setPerfData] = useState<PerformanceLogItem[]>([])
  const [perfLoading, setPerfLoading] = useState(false)
  const [perfTotal, setPerfTotal] = useState(0)
  const [perfPage, setPerfPage] = useState(0)
  const [perfMetricFilter, setPerfMetricFilter] = useState<string | undefined>(undefined)
  const [perfTrendData, setPerfTrendData] = useState<PerformanceTrendData | null>(null)
  const [perfPercentiles, setPerfPercentiles] = useState<MetricPercentile[]>([])
  const [compareVisible, setCompareVisible] = useState(false)
  const [compareBaseRange, setCompareBaseRange] = useState<[string, string] | null>(null)
  const [compareTargetRange, setCompareTargetRange] = useState<[string, string] | null>(null)
  const [compareData, setCompareData] = useState<PerformanceCompareData | null>(null)
  const [compareLoading, setCompareLoading] = useState(false)
  const [diagnoseVisible, setDiagnoseVisible] = useState(false)
  const [diagnoseRange, setDiagnoseRange] = useState<[string, string] | null>(null)
  const [diagnoseData, setDiagnoseData] = useState<DiagnosisData | null>(null)
  const [diagnoseLoading, setDiagnoseLoading] = useState(false)
  const [reportExportVisible, setReportExportVisible] = useState(false)

  const fetchData = async (p = page, kw = keyword, dr = dateRange) => {
    setLoading(true)
    try {
      const params: any = { page: p, size }
      if (kw) params.keyword = kw
      if (dr) {
        params.startTime = dr[0]
        params.endTime = dr[1]
      }
      const response = await getErrorLogs(params)
      if (response.code === 200) {
        setData(response.data || [])
        setTotal(response.total || 0)
      }
    } catch (err: any) {
      showError(err, '获取错误日志失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchAggData = async (p = page, kw = keyword, dr = dateRange) => {
    setLoading(true)
    try {
      const params: any = { page: p, size }
      if (kw) params.keyword = kw
      if (dr) {
        params.startTime = dr[0]
        params.endTime = dr[1]
      }
      const response = await getAggregatedErrors(params)
      if (response.code === 200) {
        setAggData(response.data || [])
        setTotal(response.total || 0)
      }
    } catch (err: any) {
      showError(err, '获取聚合数据失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchTrend = async () => {
    try {
      const response = await getErrorTrend(7)
      if (response.code === 200) {
        setTrendData(response.data)
      }
    } catch {
    }
  }

  const fetchPerfData = async (p = perfPage, metric = perfMetricFilter) => {
    setPerfLoading(true)
    try {
      const params: any = { page: p, size }
      if (metric) params.metricName = metric
      const response = await getPerformanceLogs(params)
      if (response.code === 200) {
        setPerfData(response.data || [])
        setPerfTotal(response.total || 0)
      }
    } catch (err: any) {
      showError(err, '获取性能数据失败')
    } finally {
      setPerfLoading(false)
    }
  }

  const fetchPerfTrend = async () => {
    try {
      const response = await getPerformanceTrend(7)
      if (response.code === 200) {
        setPerfTrendData(response.data)
      }
    } catch {
    }
  }

  const fetchPerfPercentiles = async () => {
    try {
      const response = await getPerformancePercentiles(7)
      if (response.code === 200 && response.data) {
        setPerfPercentiles(response.data.metrics || [])
      }
    } catch {
    }
  }

  const handleCompare = async () => {
    if (!compareBaseRange || !compareTargetRange) return
    setCompareLoading(true)
    try {
      const response = await comparePerformance({
        baseStart: compareBaseRange[0],
        baseEnd: compareBaseRange[1],
        compareStart: compareTargetRange[0],
        compareEnd: compareTargetRange[1],
      })
      if (response.code === 200 && response.data) {
        setCompareData(response.data)
      }
    } catch (err: any) {
      showError(err, '对比查询失败')
    } finally {
      setCompareLoading(false)
    }
  }

  const handleDiagnose = async () => {
    if (!diagnoseRange) return
    setDiagnoseLoading(true)
    try {
      const response = await diagnosePerformance({
        startTime: diagnoseRange[0],
        endTime: diagnoseRange[1],
      })
      if (response.code === 200 && response.data) {
        setDiagnoseData(response.data)
      }
    } catch (err: any) {
      showError(err, '诊断查询失败')
    } finally {
      setDiagnoseLoading(false)
    }
  }

  useEffect(() => {
    fetchData(0)
    fetchTrend()
    fetchPerfTrend()
    fetchPerfPercentiles()
    connectWs()
    return () => { disconnectWs() }
  }, [])

  useEffect(() => {
    if (activeTab === 'performance') {
      fetchPerfData(0)
    }
  }, [activeTab])

  const connectWs = useCallback(() => {
    const token = localStorage.getItem('token')
    if (!token) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/api/ws/notifications/websocket`

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe('/topic/notifications', (message: IMessage) => {
          try {
            const data = JSON.parse(message.body)
            if (data.type === 'NEW_ERROR_LOG' && viewMode === 'detail') {
              const payload = data.payload
              const newItem: ErrorLogItem = {
                id: payload.id,
                errorMessage: payload.errorMessage,
                stackTrace: null,
                pageUrl: payload.pageUrl || null,
                userAgent: null,
                httpStatus: payload.httpStatus || null,
                fallbackMessage: null,
                extraInfo: null,
                category: payload.category || null,
                createdAt: payload.createdAt,
              }
              setData((prev) => [newItem, ...prev])
              setTotal((prev) => prev + 1)
              setNewErrorCount((prev) => prev + 1)
            }
          } catch {}
        })
      },
    })
    client.activate()
    wsRef.current = client
  }, [])

  const disconnectWs = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.deactivate()
      wsRef.current = null
    }
  }, [])

  const handleRefreshNewErrors = () => {
    setNewErrorCount(0)
    fetchData(0, keyword, dateRange)
    fetchTrend()
  }

  const handleExport = async (format: string) => {
    try {
      const params: any = { format }
      if (keyword) params.keyword = keyword
      if (dateRange) {
        params.startTime = dateRange[0]
        params.endTime = dateRange[1]
      }
      const blob = await exportErrorLogs(params)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `error-logs-${new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')}.${format === 'excel' ? 'xlsx' : 'csv'}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
    } catch (err: any) {
      showError(err, '导出失败')
    }
  }

  const showMessageDetail = async (errorMessage: string) => {
    setSelectedMessage(errorMessage)
    setMessageDetailVisible(true)
    setMessageDetailLoading(true)
    try {
      const response = await getErrorsByMessage({ errorMessage, page: 0, size: 50 })
      if (response.code === 200) {
        setMessageDetailData(response.data || [])
        setMessageDetailTotal(response.total || 0)
      }
    } catch (err: any) {
      showError(err, '获取错误详情失败')
    } finally {
      setMessageDetailLoading(false)
    }
  }

  const handleSearch = () => {
    setPage(0)
    setSelectedRowKeys([])
    if (viewMode === 'detail') fetchData(0, keyword, dateRange)
    else fetchAggData(0, keyword, dateRange)
  }

  const handleReset = () => {
    setKeyword('')
    setDateRange(null)
    setPage(0)
    setSelectedRowKeys([])
    if (viewMode === 'detail') fetchData(0, '', null)
    else fetchAggData(0, '', null)
  }

  const handlePageChange = (newPage: number) => {
    const p = newPage - 1
    setPage(p)
    if (viewMode === 'detail') fetchData(p)
    else fetchAggData(p)
  }

  const handleViewModeChange = (value: string) => {
    const mode = value as 'detail' | 'aggregated'
    setViewMode(mode)
    setPage(0)
    setSelectedRowKeys([])
    if (mode === 'detail') fetchData(0, keyword, dateRange)
    else fetchAggData(0, keyword, dateRange)
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteErrorLog(id)
      fetchData(page, keyword, dateRange)
    } catch (err: any) {
      showError(err, '删除失败')
    }
  }

  const handleBatchDelete = async () => {
    try {
      await batchDeleteErrorLogs(selectedRowKeys as number[])
      setSelectedRowKeys([])
      fetchData(page, keyword, dateRange)
      fetchTrend()
    } catch (err: any) {
      showError(err, '批量删除失败')
    }
  }

  const showDetail = (record: ErrorLogItem) => {
    setCurrentItem(record)
    setDetailVisible(true)
  }

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '-'
    try {
      return new Date(dateStr).toLocaleString('zh-CN')
    } catch {
      return dateStr
    }
  }

  const chartData = trendData
    ? trendData.dates.map((date, i) => ({ date, count: trendData.counts[i] }))
    : []

  const chartConfig = {
    data: chartData,
    xField: 'date',
    yField: 'count',
    smooth: true,
    style: { pointSize: 3 },
    color: '#ff4d4f',
    axis: { y: { min: 0 } },
    tooltip: { title: 'date' },
  }

  const perfChartData = perfTrendData
    ? perfTrendData.dates.flatMap((date, i) =>
        (perfTrendData.metrics || []).map((m) => ({
          date,
          value: m.values[i] ?? null,
          metric: m.metricName,
        }))
      ).filter((d) => d.value !== null)
    : []

  const perfChartConfig = {
    data: perfChartData,
    xField: 'date',
    yField: 'value',
    colorField: 'metric',
    smooth: true,
    style: { pointSize: 3 },
    axis: { y: { min: 0 } },
    tooltip: { title: 'date' },
    color: ['#1677ff', '#52c41a', '#faad14', '#722ed1', '#13c2c2', '#2f54eb'],
  }

  const detailColumns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (v: string) => (
        <Text type="danger" style={{ maxWidth: 300 }} ellipsis={{ tooltip: v }}>
          {v}
        </Text>
      ),
    },
    {
      title: 'HTTP状态',
      dataIndex: 'httpStatus',
      key: 'httpStatus',
      width: 100,
      render: (v: number | null) =>
        v ? <Tag color={httpStatusColor(v)}>{v}</Tag> : '-',
    },
    {
      title: '页面',
      dataIndex: 'pageUrl',
      key: 'pageUrl',
      width: 200,
      ellipsis: true,
      render: (v: string | null) =>
        v ? (
          <Text ellipsis={{ tooltip: v }} style={{ maxWidth: 180 }}>
            <GlobalOutlined style={{ marginRight: 4 }} />
            {v.replace(/^https?:\/\/[^/]+/, '')}
          </Text>
        ) : '-',
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 90,
      render: (v: string | null) => {
        const cat = categoryConfig[v || 'OTHER'] || categoryConfig.OTHER
        return <Tag color={cat.color}>{cat.label}</Tag>
      },
    },
    {
      title: '来源',
      key: 'source',
      width: 100,
      render: (_: any, record: ErrorLogItem) => {
        try {
          const extra = record.extraInfo ? JSON.parse(record.extraInfo) : {}
          const type = extra?.type
          if (type === 'window.onerror') return <Tag color="volcano">运行时</Tag>
          if (type === 'unhandledrejection') return <Tag color="purple">Promise</Tag>
          return <Tag color="blue">API</Tag>
        } catch {
          return <Tag color="blue">API</Tag>
        }
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: ErrorLogItem) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => showDetail(record)}
          >
            详情
          </Button>
          <Popconfirm
            title="确认删除此错误日志？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const aggColumns = [
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      ellipsis: true,
      render: (v: string) => (
        <Text type="danger" style={{ maxWidth: 400 }} ellipsis={{ tooltip: v }}>
          {v}
        </Text>
      ),
    },
    {
      title: '出现次数',
      dataIndex: 'count',
      key: 'count',
      width: 120,
      sorter: (a: ErrorAggregationItem, b: ErrorAggregationItem) => a.count - b.count,
      render: (v: number) => <Tag color={countColor(v)}>{v} 次</Tag>,
    },
    {
      title: '首次出现',
      dataIndex: 'firstSeen',
      key: 'firstSeen',
      width: 180,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '最近出现',
      dataIndex: 'lastSeen',
      key: 'lastSeen',
      width: 180,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: ErrorAggregationItem) => (
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => showMessageDetail(record.errorMessage)}>
          详情
        </Button>
      ),
    },
  ]

  const perfColumns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '指标',
      dataIndex: 'metricName',
      key: 'metricName',
      width: 120,
      render: (v: string) => {
        const config = metricNameConfig[v]
        return config ? <Tag color={config.color}>{v}</Tag> : <Tag>{v}</Tag>
      },
    },
    {
      title: '指标值',
      dataIndex: 'metricValue',
      key: 'metricValue',
      width: 120,
      render: (v: number, record: PerformanceLogItem) => (
        <Text strong>{formatMetricValue(record.metricName, v)}</Text>
      ),
    },
    {
      title: '评级',
      dataIndex: 'rating',
      key: 'rating',
      width: 100,
      render: (v: string | null) => <Tag color={ratingColor(v)}>{ratingLabel(v)}</Tag>,
    },
    {
      title: '页面',
      dataIndex: 'pageUrl',
      key: 'pageUrl',
      ellipsis: true,
      render: (v: string | null) =>
        v ? (
          <Text ellipsis={{ tooltip: v }} style={{ maxWidth: 250 }}>
            <GlobalOutlined style={{ marginRight: 4 }} />
            {v.replace(/^https?:\/\/[^/]+/, '')}
          </Text>
        ) : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: any, record: PerformanceLogItem) => (
        <Popconfirm
          title="确认删除此性能记录？"
          onConfirm={async () => {
            try {
              await deletePerformanceLog(record.id)
              fetchPerfData(perfPage, perfMetricFilter)
              fetchPerfTrend()
            } catch (err: any) {
              showError(err, '删除失败')
            }
          }}
          okText="删除"
          cancelText="取消"
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        <BugOutlined style={{ marginRight: 8 }} />
        监控中心
      </Title>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => setActiveTab(key as 'error' | 'performance')}
        items={[
          {
            key: 'error',
            label: (
              <Space>
                <BugOutlined />
                错误日志
              </Space>
            ),
            children: (
              <>
                {trendData && (
                  <Card style={{ marginBottom: 16 }}>
                    <Row gutter={16}>
                      <Col span={6}>
                        <Statistic
                          title="错误总数"
                          value={trendData.total}
                          prefix={<WarningOutlined />}
                          valueStyle={{ color: trendData.total > 0 ? '#ff4d4f' : '#52c41a' }}
                        />
                      </Col>
                      <Col span={18}>
                        <div style={{ display: 'flex', alignItems: 'center', height: '100%' }}>
                          <LineChartOutlined style={{ marginRight: 8, color: '#999' }} />
                          <span style={{ color: '#666', fontSize: 13 }}>近 7 天趋势</span>
                        </div>
                        <Line {...chartConfig} />
                      </Col>
                    </Row>
                  </Card>
                )}

                {newErrorCount > 0 && (
                  <Card style={{ marginBottom: 16, background: '#fff2f0', borderColor: '#ffccc7' }} styles={{ body: { padding: '8px 16px' } }}>
                    <Space>
                      <Badge count={newErrorCount} overflowCount={99} />
                      <Text type="danger">有 {newErrorCount} 条新错误上报</Text>
                      <Button type="link" size="small" onClick={handleRefreshNewErrors}>
                        点击刷新
                      </Button>
                    </Space>
                  </Card>
                )}

                <Card style={{ marginBottom: 16 }}>
                  <Row gutter={16} align="middle">
                    <Col xs={24} sm={8} md={6}>
                      <Input
                        placeholder="搜索错误信息"
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        onPressEnter={handleSearch}
                        prefix={<SearchOutlined />}
                        allowClear
                      />
                    </Col>
                    <Col xs={24} sm={10} md={8}>
                      <RangePicker
                        style={{ width: '100%' }}
                        showTime
                        onChange={(_, dateStrings) => {
                          if (dateStrings[0] && dateStrings[1]) {
                            setDateRange([dateStrings[0], dateStrings[1]])
                          } else {
                            setDateRange(null)
                          }
                        }}
                      />
                    </Col>
                    <Col xs={24} sm={6} md={4}>
                      <Space>
                        <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                          查询
                        </Button>
                        <Button icon={<ReloadOutlined />} onClick={handleReset}>
                          重置
                        </Button>
                        <Dropdown
                          menu={{
                            items: [
                              { key: 'csv', label: '导出 CSV' },
                              { key: 'excel', label: '导出 Excel' },
                            ],
                            onClick: ({ key }) => handleExport(key),
                          }}
                        >
                          <Button icon={<DownloadOutlined />}>导出</Button>
                        </Dropdown>
                        <Button
                          icon={<FilePdfOutlined />}
                          onClick={() => setReportExportVisible(true)}
                        >
                          导出PDF报告
                        </Button>
                      </Space>
                    </Col>
                    <Col xs={24} sm={4} md={6} style={{ textAlign: 'right' }}>
                      <Segmented
                        value={viewMode}
                        onChange={handleViewModeChange}
                        options={[
                          { label: '详细视图', value: 'detail' },
                          { label: '聚合视图', value: 'aggregated' },
                        ]}
                      />
                    </Col>
                  </Row>
                </Card>

                <Card>
                  {viewMode === 'detail' && selectedRowKeys.length > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <Space>
                        <Text>已选择 {selectedRowKeys.length} 项</Text>
                        <Popconfirm
                          title={`确认删除选中的 ${selectedRowKeys.length} 条错误日志？`}
                          onConfirm={handleBatchDelete}
                          okText="删除"
                          cancelText="取消"
                        >
                          <Button danger icon={<DeleteOutlined />}>
                            批量删除
                          </Button>
                        </Popconfirm>
                        <Button onClick={() => setSelectedRowKeys([])}>
                          取消选择
                        </Button>
                      </Space>
                    </div>
                  )}

                  {viewMode === 'detail' ? (
                    <Table<ErrorLogItem>
                      dataSource={data}
                      columns={detailColumns}
                      rowKey="id"
                      loading={loading}
                      rowSelection={{
                        selectedRowKeys,
                        onChange: setSelectedRowKeys,
                      }}
                      pagination={{
                        current: page + 1,
                        pageSize: size,
                        total,
                        showTotal: (t) => `共 ${t} 条记录`,
                        showSizeChanger: false,
                        onChange: handlePageChange,
                      }}
                      size="small"
                    />
                  ) : (
                    <Table<ErrorAggregationItem>
                      dataSource={aggData}
                      columns={aggColumns}
                      rowKey="errorMessage"
                      loading={loading}
                      pagination={{
                        current: page + 1,
                        pageSize: size,
                        total,
                        showTotal: (t) => `共 ${t} 种错误`,
                        showSizeChanger: false,
                        onChange: handlePageChange,
                      }}
                      size="small"
                    />
                  )}
                </Card>
              </>
            ),
          },
          {
            key: 'performance',
            label: (
              <Space>
                <ThunderboltOutlined />
                性能监控
              </Space>
            ),
            children: (
              <>
                {perfPercentiles.length > 0 && (
                  <Card style={{ marginBottom: 16 }} title="分位值统计 (近7天)" size="small">
                    <Row gutter={[16, 16]}>
                      {perfPercentiles.map((mp) => {
                        const cfg = metricNameConfig[mp.metricName]
                        return (
                          <Col xs={24} sm={12} lg={8} key={mp.metricName}>
                            <Card size="small" style={{ background: '#fafafa' }}>
                              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                <Space>
                                  <Tag color={cfg?.color || '#1677ff'}>{mp.metricName}</Tag>
                                  <Text type="secondary" style={{ fontSize: 12 }}>{cfg?.label || mp.metricName}</Text>
                                  <Text type="secondary" style={{ fontSize: 12 }}>({mp.sampleCount} 次)</Text>
                                </Space>
                                <Row gutter={8}>
                                  <Col span={8}><Statistic title="P50" value={formatMetricValue(mp.metricName, mp.p50)} valueStyle={{ fontSize: 14, color: mp.p50 <= (cfg?.goodThreshold || Infinity) ? '#52c41a' : '#ff4d4f' }} /></Col>
                                  <Col span={8}><Statistic title="P75" value={formatMetricValue(mp.metricName, mp.p75)} valueStyle={{ fontSize: 14 }} /></Col>
                                  <Col span={8}><Statistic title="P95" value={formatMetricValue(mp.metricName, mp.p95)} valueStyle={{ fontSize: 14, color: mp.p95 > (cfg?.poorThreshold || 0) ? '#ff4d4f' : undefined }} /></Col>
                                </Row>
                              </Space>
                            </Card>
                          </Col>
                        )
                      })}
                    </Row>
                  </Card>
                )}

                {perfTrendData && perfChartData.length > 0 && (
                  <Card style={{ marginBottom: 16 }}>
                    <div style={{ marginBottom: 12 }}>
                      <LineChartOutlined style={{ marginRight: 8, color: '#999' }} />
                      <span style={{ color: '#666', fontSize: 13 }}>近 7 天性能趋势</span>
                    </div>
                    <Line {...perfChartConfig} />
                  </Card>
                )}

                <Card style={{ marginBottom: 16 }}>
                  <Row gutter={16} align="middle">
                    <Col xs={24} sm={8} md={6}>
                      <Select
                        placeholder="筛选指标"
                        value={perfMetricFilter}
                        onChange={(v) => {
                          setPerfMetricFilter(v)
                          setPerfPage(0)
                          fetchPerfData(0, v)
                        }}
                        allowClear
                        style={{ width: '100%' }}
                        options={Object.entries(metricNameConfig).map(([key, cfg]) => ({
                          value: key,
                          label: `${key} - ${cfg.label}`,
                        }))}
                      />
                    </Col>
                    <Col xs={24} sm={6} md={4}>
                      <Space>
                        <Button
                          icon={<ReloadOutlined />}
                          onClick={() => {
                            setPerfPage(0)
                            fetchPerfData(0, perfMetricFilter)
                            fetchPerfTrend()
                          }}
                        >
                          刷新
                        </Button>
                        <Button
                          icon={<SwapOutlined />}
                          onClick={() => setCompareVisible(true)}
                        >
                          对比
                        </Button>
                        <Button
                          icon={<MedicineBoxOutlined />}
                          onClick={() => setDiagnoseVisible(true)}
                        >
                          诊断
                        </Button>
                      </Space>
                    </Col>
                  </Row>
                </Card>

                <Card>
                  <Table<PerformanceLogItem>
                    dataSource={perfData}
                    columns={perfColumns}
                    rowKey="id"
                    loading={perfLoading}
                    pagination={{
                      current: perfPage + 1,
                      pageSize: size,
                      total: perfTotal,
                      showTotal: (t) => `共 ${t} 条记录`,
                      showSizeChanger: false,
                      onChange: (p) => {
                        const newPage = p - 1
                        setPerfPage(newPage)
                        fetchPerfData(newPage, perfMetricFilter)
                      },
                    }}
                    size="small"
                  />
                </Card>
              </>
            ),
          },
        ]}
      />

      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#ff4d4f' }} />
            错误详情
          </Space>
        }
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={720}
      >
        {currentItem && (
          <div>
            <Paragraph>
              <Text strong>错误信息：</Text>
              <Text type="danger">{currentItem.errorMessage}</Text>
            </Paragraph>
            {currentItem.httpStatus && (
              <Paragraph>
                <Text strong>HTTP 状态码：</Text>
                <Tag color={httpStatusColor(currentItem.httpStatus)}>
                  {currentItem.httpStatus}
                </Tag>
              </Paragraph>
            )}
            {currentItem.fallbackMessage && (
              <Paragraph>
                <Text strong>兜底提示：</Text>
                {currentItem.fallbackMessage}
              </Paragraph>
            )}
            {currentItem.pageUrl && (
              <Paragraph>
                <Text strong>页面 URL：</Text>
                {currentItem.pageUrl}
              </Paragraph>
            )}
            {currentItem.userAgent && (
              <Paragraph>
                <Text strong>User-Agent：</Text>
                <Text style={{ fontSize: 12, wordBreak: 'break-all' }}>
                  {currentItem.userAgent}
                </Text>
              </Paragraph>
            )}
            {currentItem.stackTrace && (
              <div style={{ marginBottom: 16 }}>
                <Text strong>堆栈信息：</Text>
                <Paragraph>
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 6,
                      fontSize: 12,
                      maxHeight: 300,
                      overflow: 'auto',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-all',
                    }}
                  >
                    {currentItem.stackTrace}
                  </pre>
                </Paragraph>
              </div>
            )}
            {currentItem.extraInfo && (
              <div style={{ marginBottom: 16 }}>
                <Text strong>附加信息：</Text>
                <Paragraph>
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 6,
                      fontSize: 12,
                      maxHeight: 200,
                      overflow: 'auto',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-all',
                    }}
                  >
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(currentItem.extraInfo), null, 2)
                      } catch {
                        return currentItem.extraInfo
                      }
                    })()}
                  </pre>
                </Paragraph>
              </div>
            )}
            <Paragraph>
              <Text strong>发生时间：</Text>
              {formatDateTime(currentItem.createdAt)}
            </Paragraph>
          </div>
        )}
      </Modal>

      <Modal
        title={
          <Space>
            <BugOutlined style={{ color: '#ff4d4f' }} />
            <Text ellipsis style={{ maxWidth: 500 }}>{selectedMessage}</Text>
            <Tag>{messageDetailTotal} 条记录</Tag>
          </Space>
        }
        open={messageDetailVisible}
        onCancel={() => setMessageDetailVisible(false)}
        footer={null}
        width={900}
      >
        <Table<ErrorLogItem>
          dataSource={messageDetailData}
          columns={[
            { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, render: (v: string) => formatDateTime(v) },
            { title: 'HTTP状态', dataIndex: 'httpStatus', key: 'httpStatus', width: 100, render: (v: number | null) => v ? <Tag color={httpStatusColor(v)}>{v}</Tag> : '-' },
            { title: '分类', dataIndex: 'category', key: 'category', width: 90, render: (v: string | null) => { const cat = categoryConfig[v || 'OTHER'] || categoryConfig.OTHER; return <Tag color={cat.color}>{cat.label}</Tag> } },
            { title: '页面', dataIndex: 'pageUrl', key: 'pageUrl', ellipsis: true, render: (v: string | null) => v ? <Text ellipsis style={{ maxWidth: 250 }}>{v.replace(/^https?:\/\/[^/]+/, '')}</Text> : '-' },
          ]}
          rowKey="id"
          loading={messageDetailLoading}
          pagination={false}
          size="small"
        />
      </Modal>

      <Modal
        title={
          <Space>
            <SwapOutlined style={{ color: '#1677ff' }} />
            性能对比分析
          </Space>
        }
        open={compareVisible}
        onCancel={() => { setCompareVisible(false); setCompareData(null) }}
        footer={null}
        width={800}
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={10}>
            <Text strong>基准时段</Text>
            <RangePicker
              style={{ width: '100%', marginTop: 4 }}
              showTime
              onChange={(_, dateStrings) => {
                if (dateStrings[0] && dateStrings[1]) {
                  setCompareBaseRange([dateStrings[0], dateStrings[1]])
                } else {
                  setCompareBaseRange(null)
                }
              }}
            />
          </Col>
          <Col span={10}>
            <Text strong>对比时段</Text>
            <RangePicker
              style={{ width: '100%', marginTop: 4 }}
              showTime
              onChange={(_, dateStrings) => {
                if (dateStrings[0] && dateStrings[1]) {
                  setCompareTargetRange([dateStrings[0], dateStrings[1]])
                } else {
                  setCompareTargetRange(null)
                }
              }}
            />
          </Col>
          <Col span={4} style={{ display: 'flex', alignItems: 'flex-end' }}>
            <Button
              type="primary"
              icon={<SwapOutlined />}
              loading={compareLoading}
              disabled={!compareBaseRange || !compareTargetRange}
              onClick={handleCompare}
            >
              对比
            </Button>
          </Col>
        </Row>

        {compareData && (
          <Table
            dataSource={compareData.metrics}
            rowKey="metricName"
            pagination={false}
            size="small"
            columns={[
              {
                title: '指标',
                dataIndex: 'metricName',
                key: 'metricName',
                width: 120,
                render: (v: string) => {
                  const cfg = metricNameConfig[v]
                  return cfg ? <Tag color={cfg.color}>{v}</Tag> : <Tag>{v}</Tag>
                },
              },
              {
                title: compareData.labels[0] || '基准',
                key: 'base',
                width: 160,
                render: (_: any, record: any) => {
                  const val = record.avgValues[0]
                  return val != null ? <Text strong>{formatMetricValue(record.metricName, val)}</Text> : <Text type="secondary">无数据</Text>
                },
              },
              {
                title: compareData.labels[1] || '对比',
                key: 'compare',
                width: 160,
                render: (_: any, record: any) => {
                  const val = record.avgValues[1]
                  return val != null ? <Text strong>{formatMetricValue(record.metricName, val)}</Text> : <Text type="secondary">无数据</Text>
                },
              },
              {
                title: '变化',
                key: 'change',
                width: 120,
                render: (_: any, record: any) => {
                  const base = record.avgValues[0]
                  const target = record.avgValues[1]
                  if (base == null || target == null || base === 0) return <Text type="secondary">-</Text>
                  const change = ((target - base) / base * 100).toFixed(1)
                  const isWorse = target > base
                  return (
                    <Text style={{ color: isWorse ? '#ff4d4f' : '#52c41a' }}>
                      {isWorse ? '↑' : '↓'} {Math.abs(Number(change))}%
                    </Text>
                  )
                },
              },
              {
                title: '采样数',
                key: 'samples',
                render: (_: any, record: any) => (
                  <Text type="secondary">{record.sampleCounts[0]} / {record.sampleCounts[1]}</Text>
                ),
              },
            ]}
          />
        )}
      </Modal>

      <Modal
        title={
          <Space>
            <MedicineBoxOutlined style={{ color: '#faad14' }} />
            错误与性能关联诊断
          </Space>
        }
        open={diagnoseVisible}
        onCancel={() => { setDiagnoseVisible(false); setDiagnoseData(null) }}
        footer={null}
        width={800}
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={18}>
            <RangePicker
              style={{ width: '100%' }}
              showTime
              onChange={(_, dateStrings) => {
                if (dateStrings[0] && dateStrings[1]) {
                  setDiagnoseRange([dateStrings[0], dateStrings[1]])
                } else {
                  setDiagnoseRange(null)
                }
              }}
            />
          </Col>
          <Col span={6}>
            <Button
              type="primary"
              icon={<MedicineBoxOutlined />}
              loading={diagnoseLoading}
              disabled={!diagnoseRange}
              onClick={handleDiagnose}
              block
            >
              开始诊断
            </Button>
          </Col>
        </Row>

        {diagnoseData && (
          <div>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={12}>
                <Card size="small" title="错误概览" style={{ background: '#fff2f0' }}>
                  <Statistic title="错误总数" value={diagnoseData.errorSummary.totalErrors} valueStyle={{ color: '#ff4d4f' }} />
                  {diagnoseData.errorSummary.topCategories.length > 0 && (
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>主要分类：</Text>
                      <Space size={4} wrap>
                        {diagnoseData.errorSummary.topCategories.map((c, i) => <Tag key={i} color="volcano">{c}</Tag>)}
                      </Space>
                    </div>
                  )}
                  {diagnoseData.errorSummary.topMessages.length > 0 && (
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>高频错误：</Text>
                      <ul style={{ margin: '4px 0', paddingLeft: 16, fontSize: 12 }}>
                        {diagnoseData.errorSummary.topMessages.map((m, i) => <li key={i}><Text type="danger" ellipsis style={{ maxWidth: 280 }}>{m}</Text></li>)}
                      </ul>
                    </div>
                  )}
                </Card>
              </Col>
              <Col span={12}>
                <Card size="small" title="性能概览" style={{ background: '#fff7e6' }}>
                  <Statistic title="Poor 指标数" value={diagnoseData.performanceSummary.totalPoorMetrics} valueStyle={{ color: '#faad14' }} />
                  {diagnoseData.performanceSummary.poorMetrics.length > 0 && (
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>劣化指标：</Text>
                      <Space size={4} wrap>
                        {diagnoseData.performanceSummary.poorMetrics.map((m, i) => (
                          <Tag key={i} color="orange">{m.metricName} ({m.poorCount}/{m.totalCount} poor)</Tag>
                        ))}
                      </Space>
                    </div>
                  )}
                </Card>
              </Col>
            </Row>

            {diagnoseData.correlations.length > 0 && (
              <Card size="small" title="关联分析" style={{ marginBottom: 16, background: '#f6ffed', borderColor: '#b7eb8f' }}>
                <ul style={{ margin: 0, paddingLeft: 16 }}>
                  {diagnoseData.correlations.map((c, i) => (
                    <li key={i} style={{ marginBottom: 6 }}><Text style={{ color: '#389e0d' }}>{c}</Text></li>
                  ))}
                </ul>
              </Card>
            )}

            <Card size="small" title="诊断结论" style={{ background: '#e6f7ff', borderColor: '#91d5ff' }}>
              <Text strong style={{ fontSize: 14 }}>{diagnoseData.conclusion}</Text>
            </Card>
          </div>
        )}
      </Modal>

      <ReportExportModal
        open={reportExportVisible}
        onClose={() => setReportExportVisible(false)}
        defaultStartTime={dateRange?.[0]}
        defaultEndTime={dateRange?.[1]}
      />
    </div>
  )
}

export default ErrorLogManagement
