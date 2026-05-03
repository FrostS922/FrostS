import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Row, Col, Statistic, Spin, Empty } from 'antd'
import {
  CheckCircleOutlined, BugOutlined,
  FileTextOutlined, CalendarOutlined, RobotOutlined,
  SafetyCertificateOutlined,
  BarChartOutlined,
} from '@ant-design/icons'
import { Column, Pie, Line } from '@ant-design/charts'
import { getProjectStatistics } from '../api/statistics'
import useMessage from '../hooks/useMessage'

// ── Color palette ──────────────────────────────────────

const COLORS = ['#2dd4bf', '#7aa2ff', '#f472b6', '#fbbf24', '#a78bfa', '#34d399', '#fb923c', '#60a5fa']

const STATUS_COLORS: Record<string, string> = {
  NEW: '#94a3b8',
  OPEN: '#60a5fa',
  IN_PROGRESS: '#7aa2ff',
  RESOLVED: '#34d399',
  VERIFIED: '#2dd4bf',
  CLOSED: '#94a3b8',
  REOPENED: '#f472b6',
  REJECTED: '#fb923c',
  DRAFT: '#94a3b8',
  ACTIVE: '#34d399',
  COMPLETED: '#2dd4bf',
  ON_HOLD: '#fbbf24',
  PLANNING: '#a78bfa',
  HIGH: '#f472b6',
  MEDIUM: '#fbbf24',
  LOW: '#34d399',
  CRITICAL: '#ef4444',
  MAJOR: '#f97316',
  MINOR: '#fbbf24',
  TRIVIAL: '#60a5fa',
}

// ── Helper ─────────────────────────────────────────────

const mapToChartData = (map: Record<string, number> | undefined) => {
  if (!map) return []
  return Object.entries(map).map(([key, value]) => ({
    type: key,
    value: Number(value),
  }))
}

const getColor = (key: string) => STATUS_COLORS[key] || COLORS[Math.abs(key.split('').reduce((a, b) => a + b.charCodeAt(0), 0)) % COLORS.length]

// ── Component ──────────────────────────────────────────

const ProjectReport: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const { message, showError } = useMessage()
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState<any>(null)

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const response: any = await getProjectStatistics(Number(projectId))
        if (response.code === 200) {
          setData(response.data)
        }
      } catch (err: any) {
        showError(err, '获取统计数据失败')
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [projectId, message])

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!data) {
    return <Empty description="暂无统计数据" style={{ marginTop: 100 }} />
  }

  // ── Chart configs ──

  const pieConfig = (chartData: any[]) => ({
    data: chartData,
    angleField: 'value',
    colorField: 'type',
    radius: 0.8,
    label: {
      type: 'outer',
      content: (data: any) => `${data.type}\n${((data.value / chartData.reduce((sum: number, d: any) => sum + d.value, 0)) * 100).toFixed(1)}%`,
    },
    legend: { position: 'bottom' as const },
    color: chartData.map((d: any) => getColor(d.type)),
    interactions: [{ type: 'element-active' }],
  })

  const columnConfig = (chartData: any[]) => ({
    data: chartData,
    xField: 'type',
    yField: 'value',
    label: { position: 'top' as const },
    color: chartData.map((d: any) => getColor(d.type)),
    xAxis: { label: { autoRotate: true } },
    meta: { value: { alias: '数量' } },
  })

  const lineConfig = (chartData: any[], fields: string[]) => ({
    data: chartData.flatMap((d: any) =>
      fields.map((field) => ({
        date: d.date,
        type: field === 'created' ? '新建' : field === 'resolved' ? '解决' : '关闭',
        value: d[field] || 0,
      }))
    ),
    xField: 'date',
    yField: 'value',
    seriesField: 'type',
    smooth: true,
    point: { size: 3 },
    legend: { position: 'top' as const },
    color: ['#f472b6', '#34d399', '#60a5fa'],
  })

  // ── Data ──

  const overviewCards = [
    { title: '需求总数', value: data.totalRequirements || 0, icon: <FileTextOutlined />, color: '#2dd4bf' },
    { title: '测试用例', value: data.totalTestCases || 0, icon: <CheckCircleOutlined />, color: '#7aa2ff' },
    { title: '测试计划', value: data.totalTestPlans || 0, icon: <CalendarOutlined />, color: '#fbbf24' },
    { title: '缺陷总数', value: data.totalDefects || 0, icon: <BugOutlined />, color: '#f472b6' },
    { title: '自动化率', value: `${data.automationRate || 0}%`, icon: <RobotOutlined />, color: '#34d399' },
    { title: '平均通过率', value: `${data.averagePassRate || 0}%`, icon: <SafetyCertificateOutlined />, color: '#60a5fa' },
    { title: '需求覆盖率', value: `${data.requirementCoverageRate || 0}%`, icon: <SafetyCertificateOutlined />, color: '#a78bfa' },
    { title: '缺陷密度', value: data.defectDensity || 0, icon: <BarChartOutlined />, color: '#fb923c' },
  ]

  return (
    <div>
      <div className="report-page-header">
        <h2><BarChartOutlined /> 项目报表统计</h2>
      </div>

      {/* Overview Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {overviewCards.map((card) => (
          <Col xs={24} sm={12} lg={6} key={card.title}>
            <Card className="report-stat-card" variant="borderless">
              <Statistic
                title={card.title}
                value={card.value}
                prefix={React.cloneElement(card.icon as React.ReactElement, {
                  style: { color: card.color, fontSize: 24 },
                })}
                valueStyle={{ color: card.color, fontWeight: 600 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Defect Section */}
      <h3 className="report-section-title"><BugOutlined /> 缺陷分析</h3>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="缺陷状态分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.defectStatusDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="缺陷严重性分布" variant="borderless" className="report-chart-card">
            <Column {...columnConfig(mapToChartData(data.defectSeverityDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="缺陷优先级分布" variant="borderless" className="report-chart-card">
            <Column {...columnConfig(mapToChartData(data.defectPriorityDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="缺陷类型分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.defectTypeDistribution))} height={280} />
          </Card>
        </Col>
      </Row>

      {/* Defect Trend */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="缺陷趋势（最近30天）" variant="borderless" className="report-chart-card">
            {data.defectTrend && data.defectTrend.length > 0 ? (
              <Line {...lineConfig(data.defectTrend, ['created', 'resolved', 'closed'])} height={320} />
            ) : (
              <Empty description="暂无趋势数据" />
            )}
          </Card>
        </Col>
      </Row>

      {/* Test Case Section */}
      <h3 className="report-section-title"><CheckCircleOutlined /> 测试用例分析</h3>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={8}>
          <Card title="用例状态分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.testCaseStatusDistribution))} height={260} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="用例优先级分布" variant="borderless" className="report-chart-card">
            <Column {...columnConfig(mapToChartData(data.testCasePriorityDistribution))} height={260} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="用例类型分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.testCaseTypeDistribution))} height={260} />
          </Card>
        </Col>
      </Row>

      {/* Requirement Section */}
      <h3 className="report-section-title"><FileTextOutlined /> 需求分析</h3>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={8}>
          <Card title="需求状态分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.requirementStatusDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="需求优先级分布" variant="borderless" className="report-chart-card">
            <Column {...columnConfig(mapToChartData(data.requirementPriorityDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="需求覆盖分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.requirementCoverageDistribution))} height={280} />
          </Card>
        </Col>
      </Row>

      {/* Requirement Trend */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="需求完成趋势（最近30天）" variant="borderless" className="report-chart-card">
            {data.requirementCompletionTrend && data.requirementCompletionTrend.length > 0 ? (
              <Line {...lineConfig(data.requirementCompletionTrend, ['created', 'resolved'])} height={320} />
            ) : (
              <Empty description="暂无趋势数据" />
            )}
          </Card>
        </Col>
      </Row>

      {/* Test Plan Section */}
      <h3 className="report-section-title"><CalendarOutlined /> 测试计划分析</h3>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="计划状态分布" variant="borderless" className="report-chart-card">
            <Pie {...pieConfig(mapToChartData(data.testPlanStatusDistribution))} height={280} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="关键指标" variant="borderless" className="report-chart-card">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 24, padding: '20px 0' }}>
              <div>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>自动化覆盖率</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ flex: 1, height: 12, background: '#e2e8f0', borderRadius: 6, overflow: 'hidden' }}>
                    <div style={{
                      width: `${Math.min(data.automationRate || 0, 100)}%`,
                      height: '100%',
                      background: '#34d399',
                      borderRadius: 6,
                      transition: 'width 0.5s ease',
                    }} />
                  </div>
                  <span style={{ fontWeight: 600, color: '#34d399' }}>{data.automationRate || 0}%</span>
                </div>
              </div>
              <div>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>平均测试通过率</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ flex: 1, height: 12, background: '#e2e8f0', borderRadius: 6, overflow: 'hidden' }}>
                    <div style={{
                      width: `${Math.min(data.averagePassRate || 0, 100)}%`,
                      height: '100%',
                      background: '#60a5fa',
                      borderRadius: 6,
                      transition: 'width 0.5s ease',
                    }} />
                  </div>
                  <span style={{ fontWeight: 600, color: '#60a5fa' }}>{data.averagePassRate || 0}%</span>
                </div>
              </div>
              <div>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>缺陷密度（缺陷数/需求数）</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ flex: 1, height: 12, background: '#e2e8f0', borderRadius: 6, overflow: 'hidden' }}>
                    <div style={{
                      width: `${Math.min((data.defectDensity || 0) * 20, 100)}%`,
                      height: '100%',
                      background: '#f472b6',
                      borderRadius: 6,
                      transition: 'width 0.5s ease',
                    }} />
                  </div>
                  <span style={{ fontWeight: 600, color: '#f472b6' }}>{data.defectDensity || 0}</span>
                </div>
              </div>
              <div>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>需求覆盖率（有测试用例关联的需求占比）</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ flex: 1, height: 12, background: '#e2e8f0', borderRadius: 6, overflow: 'hidden' }}>
                    <div style={{
                      width: `${Math.min(data.requirementCoverageRate || 0, 100)}%`,
                      height: '100%',
                      background: '#a78bfa',
                      borderRadius: 6,
                      transition: 'width 0.5s ease',
                    }} />
                  </div>
                  <span style={{ fontWeight: 600, color: '#a78bfa' }}>{data.requirementCoverageRate || 0}%</span>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default ProjectReport
