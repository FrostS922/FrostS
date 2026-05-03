import React, { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Spin, List, Typography, Tag, Space } from 'antd'
import { ProjectOutlined, CheckCircleOutlined, BugOutlined, FileTextOutlined, SafetyOutlined, LockOutlined, StopOutlined, WarningOutlined, LoginOutlined, LineChartOutlined, GlobalOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Line } from '@ant-design/charts'
import { getSecurityOverview, getLoginTrend } from '../api/security'
import type { SecurityOverview, LoginTrendPoint } from '../api/security'
import { getErrorOverview } from '../api/errorReport'
import type { ErrorOverviewData } from '../api/errorReport'
import { getPerformanceOverview } from '../api/performanceReport'
import type { PerformanceOverviewData } from '../api/performanceReport'
import { useUserStore } from '../store/userStore'

const { Text } = Typography

const metricColor = (name: string) => {
  const map: Record<string, string> = {
    LCP: 'blue', FID: 'green', CLS: 'orange', TTFB: 'purple', INP: 'cyan', FCP: 'geekblue',
  }
  return map[name] || 'default'
}

const formatMetricValue = (name: string, value: number) => {
  if (name === 'CLS') return value.toFixed(3)
  return `${Math.round(value)} ms`
}

const Dashboard: React.FC = () => {
  const [securityData, setSecurityData] = useState<SecurityOverview | null>(null)
  const [trendData, setTrendData] = useState<LoginTrendPoint[]>([])
  const [errorOverview, setErrorOverview] = useState<ErrorOverviewData | null>(null)
  const [perfOverview, setPerfOverview] = useState<PerformanceOverviewData | null>(null)
  const [securityLoading, setSecurityLoading] = useState(false)
  const { user } = useUserStore()

  const isAdmin = user?.roles?.some(r => r.includes('ADMIN'))

  const stats = [
    { title: '项目总数', value: 12, icon: <ProjectOutlined />, color: '#1890ff' },
    { title: '测试用例', value: 1234, icon: <CheckCircleOutlined />, color: '#52c41a' },
    { title: '活跃缺陷', value: 56, icon: <BugOutlined />, color: '#ff4d4f' },
    { title: '需求总数', value: 345, icon: <FileTextOutlined />, color: '#faad14' },
  ]

  useEffect(() => {
    if (isAdmin) {
      setSecurityLoading(true)
      Promise.all([getSecurityOverview(), getLoginTrend(), getErrorOverview(), getPerformanceOverview()])
        .then(([overviewRes, trendRes, errorRes, perfRes]) => {
          if (overviewRes.code === 200 && overviewRes.data) {
            setSecurityData(overviewRes.data)
          }
          if (trendRes.code === 200 && trendRes.data) {
            setTrendData(trendRes.data)
          }
          if (errorRes.code === 200 && errorRes.data) {
            setErrorOverview(errorRes.data)
          }
          if (perfRes.code === 200 && perfRes.data) {
            setPerfOverview(perfRes.data)
          }
        })
        .catch(() => {})
        .finally(() => setSecurityLoading(false))
    }
  }, [isAdmin])

  const securityStats = securityData ? [
    {
      title: '今日登录成功',
      value: securityData.todayLoginSuccesses,
      icon: <LoginOutlined />,
      color: '#52c41a',
    },
    {
      title: '今日登录失败',
      value: securityData.todayLoginFailures,
      icon: <WarningOutlined />,
      color: securityData.todayLoginFailures > 10 ? '#ff4d4f' : '#faad14',
    },
    {
      title: '异常IP数',
      value: securityData.todayAnomalousIps,
      icon: <StopOutlined />,
      color: securityData.todayAnomalousIps > 0 ? '#ff4d4f' : '#52c41a',
    },
    {
      title: '锁定账号',
      value: securityData.lockedAccounts,
      icon: <LockOutlined />,
      color: securityData.lockedAccounts > 0 ? '#faad14' : '#52c41a',
    },
    {
      title: '封禁IP',
      value: securityData.bannedIps,
      icon: <StopOutlined />,
      color: securityData.bannedIps > 0 ? '#ff4d4f' : '#52c41a',
    },
  ] : []

  const chartData = trendData.flatMap(item => [
    { date: item.date, value: item.successes, type: '登录成功' },
    { date: item.date, value: item.failures, type: '登录失败' },
  ])

  const chartConfig = {
    data: chartData,
    xField: 'date',
    yField: 'value',
    colorField: 'type',
    smooth: true,
    point: { shapeField: 'circle', sizeField: 3 },
    interaction: { tooltip: { marker: true } },
    style: { lineWidth: 2 },
    color: ['#52c41a', '#ff4d4f'],
    axis: {
      x: { title: '日期' },
      y: { title: '次数' },
    },
  }

  return (
    <div>
      <h2 className="page-title">仪表盘</h2>
      <Row gutter={[16, 16]}>
        {stats.map((stat) => (
          <Col xs={24} sm={12} lg={6} key={stat.title}>
            <Card className="metric-card">
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={React.cloneElement(stat.icon, { style: { color: stat.color, fontSize: 24 } })}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {isAdmin && (
        <>
          <h3 style={{ marginTop: 24, marginBottom: 16 }}>
            <SafetyOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            安全概览
          </h3>
          {securityLoading ? (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <Spin />
            </div>
          ) : (
            <>
              <Row gutter={[16, 16]}>
                {securityStats.map((stat) => (
                  <Col xs={24} sm={12} lg={Math.floor(24 / securityStats.length)} key={stat.title}>
                    <Card className="metric-card">
                      <Statistic
                        title={stat.title}
                        value={stat.value}
                        prefix={React.cloneElement(stat.icon, { style: { color: stat.color, fontSize: 24 } })}
                      />
                    </Card>
                  </Col>
                ))}
              </Row>

              <Card
                title={
                  <span>
                    <LineChartOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                    近7天登录趋势
                  </span>
                }
                style={{ marginTop: 16 }}
              >
                {chartData.length > 0 ? (
                  <Line {...chartConfig} height={300} />
                ) : (
                  <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>
                    暂无趋势数据
                  </div>
                )}
              </Card>
            </>
          )}

          {errorOverview && (
            <>
              <h3 style={{ marginTop: 24, marginBottom: 16 }}>
                <BugOutlined style={{ marginRight: 8, color: '#ff4d4f' }} />
                错误监控
              </h3>
              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} lg={6}>
                  <Card className="metric-card">
                    <Statistic
                      title="错误总数"
                      value={errorOverview.totalErrors}
                      prefix={<WarningOutlined style={{ color: errorOverview.totalErrors > 0 ? '#ff4d4f' : '#52c41a', fontSize: 24 }} />}
                      valueStyle={{ color: errorOverview.totalErrors > 0 ? '#ff4d4f' : '#52c41a' }}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                  <Card className="metric-card">
                    <Statistic
                      title="今日新增"
                      value={errorOverview.todayErrors}
                      prefix={<BugOutlined style={{ color: errorOverview.todayErrors > 0 ? '#faad14' : '#52c41a', fontSize: 24 }} />}
                      valueStyle={{ color: errorOverview.todayErrors > 0 ? '#faad14' : '#52c41a' }}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={24} lg={12}>
                  <Card
                    title="最近错误"
                    size="small"
                    bodyStyle={{ padding: '0 12px 12px' }}
                  >
                    <List
                      size="small"
                      dataSource={errorOverview.recentErrors}
                      locale={{ emptyText: '暂无错误记录' }}
                      renderItem={(item) => (
                        <List.Item style={{ padding: '6px 0' }}>
                          <List.Item.Meta
                            title={
                              <Text type="danger" ellipsis style={{ maxWidth: 280, fontSize: 13 }}>
                                {item.errorMessage}
                              </Text>
                            }
                            description={
                              <Space size={8} style={{ fontSize: 12, color: '#999' }}>
                                {item.httpStatus && <Tag color={item.httpStatus >= 500 ? 'red' : 'orange'} style={{ fontSize: 11 }}>{item.httpStatus}</Tag>}
                                {item.pageUrl && (
                                  <Text style={{ fontSize: 12, color: '#999' }}>
                                    <GlobalOutlined /> {item.pageUrl.replace(/^https?:\/\/[^/]+/, '')}
                                  </Text>
                                )}
                                <span>{item.createdAt}</span>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          )}

          {perfOverview && perfOverview.metrics.length > 0 && (
            <>
              <h3 style={{ marginTop: 24, marginBottom: 16 }}>
                <ThunderboltOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                性能监控
              </h3>
              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} lg={6}>
                  <Card className="metric-card">
                    <Statistic
                      title="性能上报总数"
                      value={perfOverview.totalReports}
                      prefix={<ThunderboltOutlined style={{ color: '#1677ff', fontSize: 24 }} />}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12} lg={6}>
                  <Card className="metric-card">
                    <Statistic
                      title="今日上报"
                      value={perfOverview.todayReports}
                      prefix={<LineChartOutlined style={{ color: '#52c41a', fontSize: 24 }} />}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={24} lg={12}>
                  <Card title="核心指标 (近7天均值)" size="small" bodyStyle={{ padding: '0 12px 12px' }}>
                    <List
                      size="small"
                      dataSource={perfOverview.metrics}
                      renderItem={(item) => (
                        <List.Item style={{ padding: '6px 0' }}>
                          <List.Item.Meta
                            title={
                              <Space>
                                <Tag color={metricColor(item.metricName)}>{item.metricName}</Tag>
                                <Text strong>{formatMetricValue(item.metricName, item.avgValue)}</Text>
                                <Text type="secondary" style={{ fontSize: 12 }}>avg</Text>
                              </Space>
                            }
                            description={
                              <Space size={8} style={{ fontSize: 12, color: '#999' }}>
                                <span>min: {formatMetricValue(item.metricName, item.minValue)}</span>
                                <span>max: {formatMetricValue(item.metricName, item.maxValue)}</span>
                                <span>{item.count} 次采样</span>
                                {item.poorCount > 0 && <Tag color="red" style={{ fontSize: 11 }}>{item.poorCount} poor</Tag>}
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          )}
        </>
      )}
    </div>
  )
}

export default Dashboard
