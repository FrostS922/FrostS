import React, { useEffect, useState } from 'react'
import { Card, Table, Input, Space, Button, Tag, Typography, Row, Col, Dropdown } from 'antd'
import { SearchOutlined, ReloadOutlined, AuditOutlined, UserOutlined, SafetyOutlined, DownloadOutlined, FileExcelOutlined, FileTextOutlined } from '@ant-design/icons'
import { getAuditLogs } from '../api/auditLog'
import type { AuditLogItem, AuditLogPage } from '../api/auditLog'
import useMessage from '../hooks/useMessage'

const { Title } = Typography

const API_BASE = '/api'

const actionColorMap: Record<string, string> = {
  UPDATE_PROFILE: 'blue',
  CHANGE_PASSWORD: 'orange',
  LOGIN: 'green',
  CREATE: 'cyan',
  UPDATE: 'blue',
  DELETE: 'red',
}

const actionLabelMap: Record<string, string> = {
  UPDATE_PROFILE: '更新资料',
  CHANGE_PASSWORD: '修改密码',
  LOGIN: '登录',
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
}

const AuditLogPage: React.FC = () => {
  const [data, setData] = useState<AuditLogPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [actionFilter, setActionFilter] = useState('')
  const [operatorFilter, setOperatorFilter] = useState('')
  const { message, showError } = useMessage()

  const fetchData = async (p = page, act = actionFilter, op = operatorFilter) => {
    setLoading(true)
    try {
      const params: any = { page: p, size }
      if (act) params.action = act
      if (op) params.operator = op
      const response = await getAuditLogs(params)
      if (response.code === 200 && response.data) {
        setData(response.data)
      }
    } catch (err: any) {
      showError(err, '获取审计日志失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData(0)
  }, [])

  const handleSearch = () => {
    setPage(0)
    fetchData(0, actionFilter, operatorFilter)
  }

  const handleReset = () => {
    setActionFilter('')
    setOperatorFilter('')
    setPage(0)
    fetchData(0, '', '')
  }

  const handlePageChange = (newPage: number) => {
    const p = newPage - 1
    setPage(p)
    fetchData(p)
  }

  const buildExportUrl = (format: 'excel' | 'csv') => {
    const params = new URLSearchParams()
    if (actionFilter) params.set('action', actionFilter)
    if (operatorFilter) params.set('operator', operatorFilter)
    const token = localStorage.getItem('token')
    if (token) params.set('token', token)
    const qs = params.toString()
    return `${API_BASE}/audit-logs/export/${format}${qs ? '?' + qs : ''}`
  }

  const handleExport = async (format: 'excel' | 'csv') => {
    setExporting(true)
    try {
      const url = buildExportUrl(format)
      const token = localStorage.getItem('token')
      const response = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })
      if (!response.ok) throw new Error('导出失败')
      const blob = await response.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = downloadUrl
      a.download = `审计日志_${new Date().toISOString().slice(0, 10)}.${format === 'excel' ? 'xlsx' : 'csv'}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(downloadUrl)
      message.success('导出成功')
    } catch (err: any) {
      showError(err, '导出失败')
    } finally {
      setExporting(false)
    }
  }

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '-'
    try {
      return new Date(dateStr).toLocaleString('zh-CN')
    } catch {
      return dateStr
    }
  }

  const columns = [
    {
      title: '时间',
      dataIndex: 'operatedAt',
      key: 'operatedAt',
      width: 180,
      render: (v: string) => formatDateTime(v),
    },
    {
      title: '操作类型',
      dataIndex: 'action',
      key: 'action',
      width: 130,
      render: (v: string) => (
        <Tag color={actionColorMap[v] || 'default'}>
          {actionLabelMap[v] || v}
        </Tag>
      ),
    },
    {
      title: '目标',
      dataIndex: 'target',
      key: 'target',
      width: 80,
    },
    {
      title: '操作人',
      dataIndex: 'operator',
      key: 'operator',
      width: 120,
      render: (v: string) => (
        <Space>
          <UserOutlined />
          {v}
        </Space>
      ),
    },
    {
      title: 'IP 地址',
      dataIndex: 'operatorIp',
      key: 'operatorIp',
      width: 130,
      render: (v: string) => v || '-',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (v: string) => v || '-',
    },
  ]

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        <AuditOutlined style={{ marginRight: 8 }} />
        审计日志
      </Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col xs={24} sm={8} md={6}>
            <Input
              placeholder="按操作类型筛选"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              onPressEnter={handleSearch}
              prefix={<SafetyOutlined />}
              allowClear
            />
          </Col>
          <Col xs={24} sm={8} md={6}>
            <Input
              placeholder="按操作人筛选"
              value={operatorFilter}
              onChange={(e) => setOperatorFilter(e.target.value)}
              onPressEnter={handleSearch}
              prefix={<UserOutlined />}
              allowClear
            />
          </Col>
          <Col xs={24} sm={8} md={6}>
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
                    {
                      key: 'excel',
                      icon: <FileExcelOutlined />,
                      label: '导出 Excel',
                      onClick: () => handleExport('excel'),
                    },
                    {
                      key: 'csv',
                      icon: <FileTextOutlined />,
                      label: '导出 CSV',
                      onClick: () => handleExport('csv'),
                    },
                  ],
                }}
              >
                <Button icon={<DownloadOutlined />} loading={exporting}>
                  导出
                </Button>
              </Dropdown>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table<AuditLogItem>
          dataSource={data?.content || []}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize: size,
            total: data?.totalElements || 0,
            showTotal: (total) => `共 ${total} 条记录`,
            showSizeChanger: false,
            onChange: handlePageChange,
          }}
          size="small"
        />
      </Card>
    </div>
  )
}

export default AuditLogPage
