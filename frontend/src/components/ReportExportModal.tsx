import React, { useRef, useState } from 'react'
import { Modal, Input, DatePicker, Checkbox, Button, Progress, Space, message } from 'antd'
import { FilePdfOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { getReportExportData, type ReportExportData } from '../api/reportExport'
import ReportPreview from './ReportPreview'

interface ReportExportModalProps {
  open: boolean
  onClose: () => void
  defaultStartTime?: string
  defaultEndTime?: string
}

const moduleOptions = [
  { label: '性能概览', value: 'performanceOverview' as const },
  { label: '趋势图表', value: 'trendChart' as const },
  { label: '百分位分析', value: 'percentileAnalysis' as const },
  { label: '智能诊断', value: 'diagnosis' as const },
  { label: '错误统计', value: 'errorStats' as const },
  { label: 'APM 概览', value: 'apmOverview' as const },
]

type ModuleKey = typeof moduleOptions[number]['value']

const defaultModules: Record<ModuleKey, boolean> = {
  performanceOverview: true,
  trendChart: true,
  percentileAnalysis: true,
  diagnosis: true,
  errorStats: true,
  apmOverview: false,
}

const ReportExportModal: React.FC<ReportExportModalProps> = ({ open, onClose, defaultStartTime, defaultEndTime }) => {
  const [title, setTitle] = useState('FrostS 性能监控报告')
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>(() => {
    const start = defaultStartTime ? dayjs(defaultStartTime) : dayjs().subtract(7, 'day')
    const end = defaultEndTime ? dayjs(defaultEndTime) : dayjs()
    return [start, end]
  })
  const [modules, setModules] = useState<Record<ModuleKey, boolean>>({ ...defaultModules })
  const [generating, setGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const [reportData, setReportData] = useState<ReportExportData | null>(null)
  const previewRef = useRef<HTMLDivElement>(null)

  const handleModuleChange = (key: ModuleKey) => {
    setModules((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleGenerate = async () => {
    if (!dateRange[0] || !dateRange[1]) {
      message.warning('请选择日期范围')
      return
    }

    const hasModule = Object.values(modules).some(Boolean)
    if (!hasModule) {
      message.warning('请至少选择一个报告模块')
      return
    }

    setGenerating(true)
    setProgress(0)

    try {
      setProgress(5)
      const response = await getReportExportData({
        startTime: dateRange[0].format('YYYY-MM-DDTHH:mm:ss'),
        endTime: dateRange[1].format('YYYY-MM-DDTHH:mm:ss'),
      })
      if (response.code !== 200) {
        message.error('获取报告数据失败')
        return
      }
      setReportData(response.data)
      setProgress(20)

      await new Promise((r) => setTimeout(r, 100))

      setProgress(30)
      await new Promise((r) => setTimeout(r, 200))
      setProgress(50)

      if (!previewRef.current) {
        message.error('报告渲染失败')
        return
      }

      const sections = previewRef.current.querySelectorAll('[style*="page-break-after"]')
      const totalSections = sections.length + 1
      const canvasImages: HTMLCanvasElement[] = []

      for (let i = 0; i < totalSections; i++) {
        const section = i < sections.length ? sections[i] : previewRef.current
        const canvas = await html2canvas(section as HTMLElement, {
          scale: 2,
          useCORS: true,
          backgroundColor: '#ffffff',
          logging: false,
        })
        canvasImages.push(canvas)
        setProgress(50 + Math.round((30 * (i + 1)) / totalSections))
      }

      setProgress(85)
      const pdf = new jsPDF('p', 'mm', 'a4')
      const pdfWidth = pdf.internal.pageSize.getWidth()
      const pdfHeight = pdf.internal.pageSize.getHeight()

      canvasImages.forEach((canvas, index) => {
        if (index > 0) pdf.addPage()
        const imgData = canvas.toDataURL('image/png')
        const imgWidth = pdfWidth
        const imgHeight = (canvas.height * pdfWidth) / canvas.width
        const yOffset = imgHeight > pdfHeight ? 0 : (pdfHeight - imgHeight) / 2
        pdf.addImage(imgData, 'PNG', 0, yOffset, imgWidth, Math.min(imgHeight, pdfHeight))
      })

      setProgress(95)
      pdf.save(`${title}_${dayjs().format('YYYYMMDDHHmmss')}.pdf`)
      setProgress(100)
      message.success('PDF 报告已生成并下载')
    } catch (err: any) {
      message.error(err?.message || '生成 PDF 报告失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleClose = () => {
    if (!generating) {
      setReportData(null)
      setProgress(0)
      onClose()
    }
  }

  const progressText = () => {
    if (progress <= 20) return '加载数据'
    if (progress <= 50) return '渲染报告'
    if (progress <= 80) return '生成截图'
    return '合成PDF'
  }

  return (
    <Modal
      title={
        <Space>
          <FilePdfOutlined style={{ color: '#ff4d4f' }} />
          导出 PDF 报告
        </Space>
      }
      open={open}
      onCancel={handleClose}
      width={520}
      footer={[
        <Button key="cancel" onClick={handleClose} disabled={generating}>
          取消
        </Button>,
        <Button
          key="generate"
          type="primary"
          icon={<FilePdfOutlined />}
          loading={generating}
          onClick={handleGenerate}
        >
          {generating ? progressText() : '生成 PDF'}
        </Button>,
      ]}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div>
          <div style={{ marginBottom: 6, fontWeight: 500 }}>报告标题</div>
          <Input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="输入报告标题"
          />
        </div>

        <div>
          <div style={{ marginBottom: 6, fontWeight: 500 }}>日期范围</div>
          <DatePicker.RangePicker
            style={{ width: '100%' }}
            showTime
            value={dateRange}
            onChange={(values) => {
              if (values && values[0] && values[1]) {
                setDateRange([values[0], values[1]])
              }
            }}
          />
        </div>

        <div>
          <div style={{ marginBottom: 6, fontWeight: 500 }}>报告模块</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {moduleOptions.map((opt) => (
              <Checkbox
                key={opt.value}
                checked={modules[opt.value]}
                onChange={() => handleModuleChange(opt.value)}
              >
                {opt.label}
              </Checkbox>
            ))}
          </div>
        </div>

        {generating && (
          <div>
            <Progress percent={progress} status="active" />
            <div style={{ textAlign: 'center', fontSize: 12, color: '#999', marginTop: 4 }}>
              {progressText()}... {progress}%
            </div>
          </div>
        )}
      </div>

      <div style={{ position: 'absolute', left: '-9999px', top: 0 }}>
        {reportData && (
          <div ref={previewRef}>
            <ReportPreview
              data={reportData}
              title={title}
              dateRange={dateRange}
              modules={modules}
            />
          </div>
        )}
      </div>
    </Modal>
  )
}

export default ReportExportModal
