import React, { useEffect, useState, useRef, useCallback } from 'react'
import { Card, Row, Col, Statistic, Tag, Table, Badge, Space, Typography, Tooltip } from 'antd'
import {
  ThunderboltOutlined,
  SafetyOutlined,
  WarningOutlined,
  BugOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LinkOutlined,
  DisconnectOutlined,
  AlertOutlined,
  ArrowLeftOutlined,
  HomeOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { Gauge, Line } from '@ant-design/charts'
import { useMonitorWebSocket } from '@/hooks/useMonitorWebSocket'
import { getRecentAlerts, type RecentAlert } from '@/api/monitor'

const { Text } = Typography

const metricConfig: Record<string, { label: string; unit: string; color: string; good: number; poor: number; max: number }> = {
  LCP: { label: '最大内容绘制', unit: 'ms', color: '#1677ff', good: 2500, poor: 4000, max: 6000 },
  CLS: { label: '累积布局偏移', unit: '', color: '#faad14', good: 0.1, poor: 0.25, max: 0.5 },
  FID: { label: '首次输入延迟', unit: 'ms', color: '#52c41a', good: 100, poor: 300, max: 500 },
  TTFB: { label: '首字节时间', unit: 'ms', color: '#722ed1', good: 800, poor: 1800, max: 3000 },
  INP: { label: '交互延迟', unit: 'ms', color: '#13c2c2', good: 200, poor: 500, max: 800 },
  FCP: { label: '首次内容绘制', unit: 'ms', color: '#2f54eb', good: 1800, poor: 3000, max: 5000 },
}

const metricOrder = ['LCP', 'CLS', 'FID', 'TTFB', 'INP', 'FCP']

const getRating = (name: string, value: number): { label: string; color: string } => {
  const cfg = metricConfig[name]
  if (!cfg) return { label: '未知', color: '#999' }
  if (value <= cfg.good) return { label: '良好', color: '#52c41a' }
  if (value <= cfg.poor) return { label: '需优化', color: '#faad14' }
  return { label: '较差', color: '#ff4d4f' }
}

const formatValue = (name: string, value: number): string => {
  if (name === 'CLS') return value.toFixed(3)
  return `${Math.round(value)}`
}

const poorRatioColor = (ratio: number): string => {
  if (ratio > 50) return '#ff4d4f'
  if (ratio > 20) return '#faad14'
  return '#52c41a'
}

const priorityColor = (priority: string): string => {
  switch (priority) {
    case 'CRITICAL': return '#ff4d4f'
    case 'HIGH': return '#fa541c'
    case 'MEDIUM': return '#faad14'
    default: return '#1677ff'
  }
}

const MonitorDashboard: React.FC = () => {
  const navigate = useNavigate()
  const { connected, performanceData, lastUpdate } = useMonitorWebSocket(true)
  const [currentTime, setCurrentTime] = useState(new Date())
  const [alerts, setAlerts] = useState<RecentAlert[]>([])
  const alertsContainerRef = useRef<HTMLDivElement>(null)
  const scrollAnimRef = useRef<number | null>(null)

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000)
    return () => clearInterval(timer)
  }, [])

  const loadAlerts = useCallback(async () => {
    try {
      const res = await getRecentAlerts()
      if (res.code === 200 && res.data) {
        setAlerts(res.data)
      }
    } catch {}
  }, [])

  useEffect(() => {
    loadAlerts()
    const timer = setInterval(loadAlerts, 30000)
    return () => clearInterval(timer)
  }, [loadAlerts])

  useEffect(() => {
    const container = alertsContainerRef.current
    if (!container || alerts.length === 0) return

    let scrollPos = 0
    const scrollSpeed = 0.5
    const maxScroll = container.scrollWidth - container.clientWidth

    const animate = () => {
      scrollPos += scrollSpeed
      if (scrollPos >= maxScroll) {
        scrollPos = 0
      }
      container.scrollLeft = scrollPos
      scrollAnimRef.current = requestAnimationFrame(animate)
    }

    scrollAnimRef.current = requestAnimationFrame(animate)
    return () => {
      if (scrollAnimRef.current) cancelAnimationFrame(scrollAnimRef.current)
    }
  }, [alerts])

  const metrics = performanceData?.performance?.metrics || []
  const security = performanceData?.security
  const errors = performanceData?.errors

  const gaugeConfigs = metricOrder.map((name) => {
    const cfg = metricConfig[name]
    const metric = metrics.find((m) => m.metricName === name)
    const value = metric?.avgValue ?? 0
    const rating = getRating(name, value)
    const percent = cfg.max > 0 ? Math.min((value / cfg.max) * 100, 100) : 0

    return { name, cfg, value, rating, percent, metric }
  })

  const securityCards = security
    ? [
        { title: '今日登录成功', value: security.todayLoginSuccesses, icon: <CheckCircleOutlined />, color: '#52c41a' },
        { title: '今日登录失败', value: security.todayLoginFailures, icon: <CloseCircleOutlined />, color: security.todayLoginFailures > 10 ? '#ff4d4f' : '#faad14' },
        { title: '今日异常IP数', value: security.todayAnomalousIps, icon: <WarningOutlined />, color: security.todayAnomalousIps > 0 ? '#ff4d4f' : '#52c41a' },
        { title: '锁定账户', value: security.lockedAccounts, icon: <CloseCircleOutlined />, color: security.lockedAccounts > 0 ? '#ff4d4f' : '#52c41a' },
        { title: '封禁IP', value: security.bannedIps, icon: <CloseCircleOutlined />, color: security.bannedIps > 0 ? '#ff4d4f' : '#52c41a' },
      ]
    : []

  const tableData = metrics.map((m) => {
    const ratio = m.count > 0 ? (m.poorCount / m.count) * 100 : 0
    return {
      key: m.metricName,
      metricName: m.metricName,
      label: metricConfig[m.metricName]?.label || m.metricName,
      avgValue: m.avgValue,
      count: m.count,
      poorCount: m.poorCount,
      poorRatio: ratio,
    }
  })

  const tableColumns = [
    { title: '指标', dataIndex: 'label', key: 'label', render: (text: string, record: any) => <Space><Tag color={metricConfig[record.metricName]?.color}>{record.metricName}</Tag><Text style={{ color: '#e0e0e0' }}>{text}</Text></Space> },
    { title: '均值', dataIndex: 'avgValue', key: 'avgValue', render: (v: number, record: any) => <Text style={{ color: '#e0e0e0' }}>{formatValue(record.metricName, v)}{metricConfig[record.metricName]?.unit}</Text> },
    { title: '采样数', dataIndex: 'count', key: 'count', render: (v: number) => <Text style={{ color: '#e0e0e0' }}>{v}</Text> },
    { title: 'Poor数', dataIndex: 'poorCount', key: 'poorCount', render: (v: number) => <Text style={{ color: v > 0 ? '#ff4d4f' : '#52c41a' }}>{v}</Text> },
    { title: 'Poor占比', dataIndex: 'poorRatio', key: 'poorRatio', render: (v: number) => <Text style={{ color: poorRatioColor(v) }}>{v.toFixed(1)}%</Text> },
  ]

  const errorChartData = errors?.recentErrors?.map((e, i) => ({
    index: i + 1,
    message: e.errorMessage?.substring(0, 30) || 'Unknown',
    category: e.category,
  })) || []

  const errorChartConfig = {
    data: errorChartData.map((d) => ({ x: d.index, y: 1, ...d })),
    xField: 'x',
    yField: 'y',
    smooth: true,
    style: { lineWidth: 2 },
    color: '#ff4d4f',
    axis: {
      x: { title: '序号', label: { style: { fill: '#999' } } },
      y: { title: '错误', label: { style: { fill: '#999' } } },
    },
  }

  const darkCardStyle: React.CSSProperties = {
    background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: 8,
  }

  const headerStyle: React.CSSProperties = {
    background: 'rgba(255,255,255,0.04)',
    borderBottom: '1px solid rgba(255,255,255,0.1)',
    padding: '12px 24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  }

  return (
    <div style={{ background: '#0a1628', color: '#e0e0e0', height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={headerStyle}>
        <Space size={16}>
          <Tooltip title="返回仪表盘">
            <ArrowLeftOutlined
              style={{ fontSize: 18, color: '#999', cursor: 'pointer', transition: 'color 0.2s' }}
              onClick={() => navigate('/dashboard')}
              onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
              onMouseLeave={(e) => (e.currentTarget.style.color = '#999')}
            />
          </Tooltip>
          <ThunderboltOutlined style={{ fontSize: 24, color: '#1677ff' }} />
          <span style={{ fontSize: 20, fontWeight: 700, color: '#fff' }}>FrostS 监控大屏</span>
        </Space>
        <Space size={24}>
          {lastUpdate && (
            <Text style={{ color: '#999', fontSize: 12 }}>
              数据更新: {lastUpdate.toLocaleTimeString()}
            </Text>
          )}
          <Badge status={connected ? 'success' : 'error'} text={
            <Text style={{ color: connected ? '#52c41a' : '#ff4d4f', fontSize: 12 }}>
              {connected ? <><LinkOutlined /> 已连接</> : <><DisconnectOutlined /> 未连接</>}
            </Text>
          } />
          <Text style={{ color: '#fff', fontSize: 18, fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
            {currentTime.toLocaleTimeString()}
          </Text>
          <Tooltip title="返回首页">
            <HomeOutlined
              style={{ fontSize: 16, color: '#999', cursor: 'pointer', transition: 'color 0.2s' }}
              onClick={() => navigate('/dashboard')}
              onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
              onMouseLeave={(e) => (e.currentTarget.style.color = '#999')}
            />
          </Tooltip>
        </Space>
      </div>

      <div className="monitor-dashboard-content" style={{ flex: 1, padding: '16px 24px', display: 'flex', flexDirection: 'column', gap: 16, overflow: 'auto' }}>
        <Row gutter={[12, 12]} style={{ flex: '0 0 auto' }}>
          {gaugeConfigs.map(({ name, cfg, value, rating, percent }) => (
            <Col span={4} key={name}>
              <Card style={darkCardStyle} styles={{ body: { padding: '12px 16px' } }}>
                <div style={{ textAlign: 'center' }}>
                  <Text style={{ color: cfg.color, fontSize: 13, fontWeight: 600 }}>{cfg.label}</Text>
                  <div style={{ margin: '4px 0' }}>
                    <Text style={{ color: '#fff', fontSize: 28, fontWeight: 700 }}>{formatValue(name, value)}</Text>
                    <Text style={{ color: '#999', fontSize: 12, marginLeft: 4 }}>{cfg.unit}</Text>
                  </div>
                  <Tag color={rating.color} style={{ margin: 0 }}>{rating.label}</Tag>
                  <div style={{ marginTop: 8 }}>
                    <Gauge
                      height={60}
                      data={{
                        target: value,
                        total: cfg.max,
                        thresholds: [cfg.good, cfg.poor],
                      }}
                      scale={{
                        color: { range: ['#52c41a', '#faad14', '#ff4d4f'] },
                      }}
                      style={{
                        pointerStroke: '#fff',
                        pointerLineWidth: 2,
                        pinFill: '#fff',
                        pinR: 4,
                      }}
                    />
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>

        <Row gutter={[12, 12]} style={{ flex: '0 0 auto' }}>
          <Col span={14}>
            <Card
              title={<Space><BugOutlined style={{ color: '#ff4d4f' }} /><Text style={{ color: '#e0e0e0' }}>错误监控</Text></Space>}
              style={{ ...darkCardStyle, height: '100%' }}
              styles={{ body: { padding: 12, overflow: 'hidden' }, header: { color: '#e0e0e0', borderBottom: '1px solid rgba(255,255,255,0.1)' } }}
            >
              <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
                <Col span={12}>
                  <Statistic
                    title={<Text style={{ color: '#999' }}>错误总数</Text>}
                    value={errors?.totalErrors || 0}
                    prefix={<WarningOutlined style={{ color: (errors?.totalErrors || 0) > 0 ? '#ff4d4f' : '#52c41a' }} />}
                    valueStyle={{ color: (errors?.totalErrors || 0) > 0 ? '#ff4d4f' : '#52c41a' }}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title={<Text style={{ color: '#999' }}>今日新增</Text>}
                    value={errors?.todayErrors || 0}
                    prefix={<BugOutlined style={{ color: (errors?.todayErrors || 0) > 0 ? '#faad14' : '#52c41a' }} />}
                    valueStyle={{ color: (errors?.todayErrors || 0) > 0 ? '#faad14' : '#52c41a' }}
                  />
                </Col>
              </Row>
              {errorChartData.length > 0 ? (
                <Line {...errorChartConfig} height={120} />
              ) : (
                <div style={{ textAlign: 'center', padding: 20, color: '#666' }}>暂无错误数据</div>
              )}
            </Card>
          </Col>
          <Col span={10}>
            <Card
              title={<Space><SafetyOutlined style={{ color: '#1677ff' }} /><Text style={{ color: '#e0e0e0' }}>安全指标</Text></Space>}
              style={{ ...darkCardStyle, height: '100%' }}
              styles={{ body: { padding: 16 }, header: { color: '#e0e0e0', borderBottom: '1px solid rgba(255,255,255,0.1)' } }}
            >
              <Row gutter={[12, 12]}>
                {securityCards.map((card) => (
                  <Col span={12} key={card.title}>
                    <Card style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)' }} styles={{ body: { padding: '12px 16px' } }}>
                      <Statistic
                        title={<Text style={{ color: '#999', fontSize: 12 }}>{card.title}</Text>}
                        value={card.value}
                        prefix={React.cloneElement(card.icon, { style: { color: card.color, fontSize: 18 } })}
                        valueStyle={{ color: card.color, fontSize: 24 }}
                      />
                    </Card>
                  </Col>
                ))}
              </Row>
            </Card>
          </Col>
        </Row>

        <Card
          title={<Space><ThunderboltOutlined style={{ color: '#1677ff' }} /><Text style={{ color: '#e0e0e0' }}>性能指标详情</Text></Space>}
          style={{ ...darkCardStyle, flex: '0 0 auto' }}
          styles={{ body: { padding: '0 16px 8px' }, header: { color: '#e0e0e0', borderBottom: '1px solid rgba(255,255,255,0.1)', minHeight: 40, padding: '0 16px' } }}
        >
          <Table
            dataSource={tableData}
            columns={tableColumns}
            pagination={false}
            size="small"
            style={{ background: 'transparent' }}
            className="dark-table"
          />
        </Card>

        <Card
          title={<Space><AlertOutlined style={{ color: '#fa541c' }} /><Text style={{ color: '#e0e0e0' }}>实时告警</Text></Space>}
          style={{ ...darkCardStyle, flex: '0 0 auto' }}
          styles={{ body: { padding: '8px 16px' }, header: { color: '#e0e0e0', borderBottom: '1px solid rgba(255,255,255,0.1)', minHeight: 36, padding: '0 16px' } }}
        >
          <div
            ref={alertsContainerRef}
            style={{ overflow: 'hidden', whiteSpace: 'nowrap', display: 'flex', gap: 24 }}
          >
            {alerts.length > 0 ? (
              alerts.map((alert) => (
                <Space key={alert.id} size={8} style={{ flexShrink: 0 }}>
                  <Tag color={priorityColor(alert.priority)}>{alert.priority}</Tag>
                  <Text style={{ color: '#e0e0e0', fontSize: 13 }}>{alert.title}</Text>
                  <Text style={{ color: '#666', fontSize: 12 }}>{alert.createdAt}</Text>
                </Space>
              ))
            ) : (
              <Text style={{ color: '#666' }}>暂无告警</Text>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}

export default MonitorDashboard
