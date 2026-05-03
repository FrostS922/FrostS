import React from 'react'
import dayjs from 'dayjs'
import type { ReportExportData } from '../api/reportExport'

const metricNameConfig: Record<string, { label: string; unit: string }> = {
  LCP: { label: '最大内容绘制', unit: 'ms' },
  FID: { label: '首次输入延迟', unit: 'ms' },
  CLS: { label: '累积布局偏移', unit: '' },
  TTFB: { label: '首字节时间', unit: 'ms' },
  INP: { label: '交互延迟', unit: 'ms' },
  FCP: { label: '首次内容绘制', unit: 'ms' },
}

const categoryLabelMap: Record<string, string> = {
  NETWORK: '网络',
  RESOURCE: '资源加载',
  CODE: '代码逻辑',
  AUTH: '认证授权',
  NOT_FOUND: '404',
  SERVER: '服务端',
  OTHER: '其他',
}

interface ReportPreviewProps {
  data: ReportExportData
  title: string
  dateRange: [dayjs.Dayjs, dayjs.Dayjs]
  modules: {
    performanceOverview: boolean
    trendChart: boolean
    percentileAnalysis: boolean
    diagnosis: boolean
    errorStats: boolean
    apmOverview: boolean
  }
}

const ReportPreview: React.FC<ReportPreviewProps> = ({ data, title, dateRange, modules }) => {
  const dateRangeStr = `${dateRange[0].format('YYYY-MM-DD HH:mm')} ~ ${dateRange[1].format('YYYY-MM-DD HH:mm')}`
  const generatedAt = dayjs().format('YYYY-MM-DD HH:mm:ss')

  const formatMetricValue = (name: string, value: number) => {
    const config = metricNameConfig[name]
    if (!config) return String(Math.round(value))
    if (name === 'CLS') return value.toFixed(3)
    return `${Math.round(value)} ms`
  }

  const ratingColor = (metricName: string, value: number) => {
    const thresholds: Record<string, [number, number]> = {
      LCP: [2500, 4000],
      FID: [100, 300],
      CLS: [0.1, 0.25],
      TTFB: [800, 1800],
      INP: [200, 500],
      FCP: [1800, 3000],
    }
    const t = thresholds[metricName]
    if (!t) return '#333'
    if (value <= t[0]) return '#52c41a'
    if (value <= t[1]) return '#faad14'
    return '#ff4d4f'
  }

  const thStyle: React.CSSProperties = { border: '1px solid #e8e8e8', padding: '10px 12px' }
  const tdStyle: React.CSSProperties = { border: '1px solid #e8e8e8', padding: '10px 12px' }
  const tdCenter: React.CSSProperties = { ...tdStyle, textAlign: 'center' as const }
  const thCenter: React.CSSProperties = { ...thStyle, textAlign: 'center' as const }
  const thLeft: React.CSSProperties = { ...thStyle, textAlign: 'left' as const }

  return (
    <div
      id="report-preview"
      style={{
        width: 794,
        background: '#fff',
        color: '#333',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        fontSize: 14,
        lineHeight: 1.6,
      }}
    >
      <div style={{ padding: '80px 60px', textAlign: 'center', pageBreakAfter: 'always' }}>
        <div style={{ fontSize: 32, fontWeight: 700, color: '#1677ff', marginBottom: 24 }}>{title}</div>
        <div style={{ fontSize: 16, color: '#666', marginBottom: 12 }}>报告时段：{dateRangeStr}</div>
        <div style={{ fontSize: 14, color: '#999', marginBottom: 60 }}>生成时间：{generatedAt}</div>
        <div style={{ borderTop: '3px solid #1677ff', width: 80, margin: '0 auto' }} />
        <div style={{ marginTop: 40, fontSize: 13, color: '#999' }}>FrostS 性能监控平台</div>
      </div>

      {modules.performanceOverview && data.overview && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>性能概览</h2>
          <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
            <div style={{ flex: 1, background: '#f6f8fa', borderRadius: 8, padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: '#1677ff' }}>{data.overview.totalReports}</div>
              <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>总报告数</div>
            </div>
            <div style={{ flex: 1, background: '#f6f8fa', borderRadius: 8, padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: '#52c41a' }}>{data.overview.todayReports}</div>
              <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>今日报告数</div>
            </div>
          </div>
          {data.overview.metrics.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#fafafa' }}>
                  <th style={thLeft}>指标</th>
                  <th style={thCenter}>平均值</th>
                  <th style={thCenter}>最小值</th>
                  <th style={thCenter}>最大值</th>
                  <th style={thCenter}>采样数</th>
                  <th style={thCenter}>差评数</th>
                </tr>
              </thead>
              <tbody>
                {data.overview.metrics.map((m) => (
                  <tr key={m.metricName}>
                    <td style={tdStyle}>{m.metricName} - {metricNameConfig[m.metricName]?.label || m.metricName}</td>
                    <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.avgValue), fontWeight: 600 }}>{formatMetricValue(m.metricName, m.avgValue)}</td>
                    <td style={tdCenter}>{formatMetricValue(m.metricName, m.minValue)}</td>
                    <td style={tdCenter}>{formatMetricValue(m.metricName, m.maxValue)}</td>
                    <td style={tdCenter}>{m.count}</td>
                    <td style={{ ...tdCenter, color: m.poorCount > 0 ? '#ff4d4f' : '#333' }}>{m.poorCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {modules.trendChart && data.trend && data.trend.dates.length > 0 && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>趋势分析</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
            <thead>
              <tr style={{ background: '#fafafa' }}>
                <th style={thLeft}>日期</th>
                {data.trend.metrics.map((m) => (
                  <th key={m.metricName} style={thCenter}>{m.metricName}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.trend.dates.map((date, di) => (
                <tr key={date}>
                  <td style={tdStyle}>{date}</td>
                  {data.trend.metrics.map((m) => {
                    const val = m.values[di]
                    return (
                      <td key={m.metricName} style={{ ...tdCenter, color: val != null ? ratingColor(m.metricName, val) : '#999' }}>
                        {val != null ? formatMetricValue(m.metricName, val) : '-'}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modules.percentileAnalysis && data.percentiles && data.percentiles.metrics.length > 0 && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>百分位分析</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#fafafa' }}>
                <th style={thLeft}>指标</th>
                <th style={thCenter}>P50</th>
                <th style={thCenter}>P75</th>
                <th style={thCenter}>P90</th>
                <th style={thCenter}>P95</th>
                <th style={thCenter}>P99</th>
                <th style={thCenter}>采样数</th>
              </tr>
            </thead>
            <tbody>
              {data.percentiles.metrics.map((m) => (
                <tr key={m.metricName}>
                  <td style={tdStyle}>{m.metricName}</td>
                  <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.p50) }}>{formatMetricValue(m.metricName, m.p50)}</td>
                  <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.p75) }}>{formatMetricValue(m.metricName, m.p75)}</td>
                  <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.p90) }}>{formatMetricValue(m.metricName, m.p90)}</td>
                  <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.p95) }}>{formatMetricValue(m.metricName, m.p95)}</td>
                  <td style={{ ...tdCenter, color: ratingColor(m.metricName, m.p99) }}>{formatMetricValue(m.metricName, m.p99)}</td>
                  <td style={tdCenter}>{m.sampleCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modules.diagnosis && data.diagnosis && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>智能诊断</h2>
          <div style={{ background: '#f6f8fa', borderRadius: 8, padding: 20, marginBottom: 20 }}>
            <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>诊断结论</div>
            <div style={{ fontSize: 14, color: '#333' }}>{data.diagnosis.conclusion}</div>
          </div>
          {data.diagnosis.correlations.length > 0 && (
            <div style={{ marginBottom: 20 }}>
              <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>关联分析</div>
              {data.diagnosis.correlations.map((c, i) => (
                <div key={i} style={{ background: '#fff7e6', borderLeft: '3px solid #faad14', padding: '10px 14px', marginBottom: 8, fontSize: 13 }}>{c}</div>
              ))}
            </div>
          )}
          <div style={{ display: 'flex', gap: 16 }}>
            <div style={{ flex: 1, background: '#fff2f0', borderRadius: 8, padding: 16 }}>
              <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 8, color: '#ff4d4f' }}>错误概览</div>
              <div style={{ fontSize: 13 }}>错误总数：<strong>{data.diagnosis.errorSummary.totalErrors}</strong></div>
              {data.diagnosis.errorSummary.topCategories.length > 0 && (
                <div style={{ fontSize: 13, marginTop: 4 }}>主要分类：{data.diagnosis.errorSummary.topCategories.join('、')}</div>
              )}
            </div>
            <div style={{ flex: 1, background: '#f0f5ff', borderRadius: 8, padding: 16 }}>
              <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 8, color: '#1677ff' }}>性能概览</div>
              <div style={{ fontSize: 13 }}>劣化指标总数：<strong>{data.diagnosis.performanceSummary.totalPoorMetrics}</strong></div>
              {data.diagnosis.performanceSummary.poorMetrics.length > 0 && (
                <div style={{ fontSize: 13, marginTop: 4 }}>
                  {data.diagnosis.performanceSummary.poorMetrics.map((m) => `${m.metricName}(${m.poorCount}/${m.totalCount})`).join('、')}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {modules.errorStats && data.errorStats && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>错误统计</h2>
          <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
            <div style={{ flex: 1, background: '#fff2f0', borderRadius: 8, padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: '#ff4d4f' }}>{data.errorStats.totalErrors}</div>
              <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>错误总数</div>
            </div>
            <div style={{ flex: 1, background: '#fff7e6', borderRadius: 8, padding: 20, textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: '#faad14' }}>{data.errorStats.todayErrors}</div>
              <div style={{ fontSize: 13, color: '#666', marginTop: 4 }}>今日错误数</div>
            </div>
          </div>
          {data.errorStats.topCategories.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ background: '#fafafa' }}>
                  <th style={thLeft}>错误分类</th>
                  <th style={thCenter}>数量</th>
                </tr>
              </thead>
              <tbody>
                {data.errorStats.topCategories.map((c) => (
                  <tr key={c.category}>
                    <td style={tdStyle}>{categoryLabelMap[c.category] || c.category}</td>
                    <td style={{ ...tdCenter, color: c.count > 0 ? '#ff4d4f' : '#333', fontWeight: 600 }}>{c.count}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {modules.apmOverview && (
        <div style={{ padding: '40px 60px', pageBreakAfter: 'always' }}>
          <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1677ff', borderBottom: '2px solid #1677ff', paddingBottom: 8, marginBottom: 24 }}>APM 概览</h2>
          <div style={{ background: '#f6f8fa', borderRadius: 8, padding: 24, textAlign: 'center' }}>
            <div style={{ fontSize: 14, color: '#666' }}>APM 监控数据将在后续版本中提供</div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ReportPreview
