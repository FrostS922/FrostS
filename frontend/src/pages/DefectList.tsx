import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag,
  Drawer, Descriptions, Row, Col, Tooltip, Badge,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  CheckOutlined, CloseOutlined, RollbackOutlined,
  SafetyCertificateOutlined, FileTextOutlined, BugOutlined,
  EnvironmentOutlined, BranchesOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getDefects, createDefect, updateDefect, deleteDefect,
  resolveDefect, closeDefect,
} from '../api/defect'
import useMessage from '../hooks/useMessage'

// ── Enum maps ──────────────────────────────────────────

const SEVERITY_MAP: Record<string, { label: string; color: string }> = {
  CRITICAL: { label: '致命', color: 'magenta' },
  MAJOR: { label: '严重', color: 'red' },
  MINOR: { label: '一般', color: 'orange' },
  TRIVIAL: { label: '轻微', color: 'blue' },
}

const PRIORITY_MAP: Record<string, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  MEDIUM: { label: '中', color: 'orange' },
  LOW: { label: '低', color: 'green' },
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  NEW: { label: '新建', color: 'default' },
  OPEN: { label: '打开', color: 'processing' },
  IN_PROGRESS: { label: '处理中', color: 'blue' },
  RESOLVED: { label: '已解决', color: 'success' },
  VERIFIED: { label: '已验证', color: 'cyan' },
  CLOSED: { label: '已关闭', color: 'default' },
  REOPENED: { label: '重开', color: 'error' },
  REJECTED: { label: '已拒绝', color: 'warning' },
}

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  FUNCTIONAL: { label: '功能缺陷', color: 'blue' },
  UI: { label: 'UI缺陷', color: 'purple' },
  PERFORMANCE: { label: '性能缺陷', color: 'orange' },
  COMPATIBILITY: { label: '兼容性', color: 'cyan' },
  SECURITY: { label: '安全缺陷', color: 'red' },
  DATA: { label: '数据缺陷', color: 'geekblue' },
}

const REPRODUCIBILITY_MAP: Record<string, string> = {
  ALWAYS: '必现',
  OFTEN: '经常',
  SOMETIMES: '偶尔',
  RARELY: '很少',
  UNABLE: '无法复现',
}

const SOURCE_MAP: Record<string, string> = {
  TESTING: '测试发现',
  CUSTOMER: '客户反馈',
  INTERNAL: '内部反馈',
  AUTOMATION: '自动化测试',
  CODE_REVIEW: '代码审查',
  OTHER: '其他',
}

// ── Component ──────────────────────────────────────────

const DefectList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const { message, showError } = useMessage()
  const [form] = Form.useForm()

  // Data
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  // Modal / Drawer
  const [modalVisible, setModalVisible] = useState(false)
  const [editingDefect, setEditingDefect] = useState<any>(null)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [viewingDefect, setViewingDefect] = useState<any>(null)

  // ── Fetch ──

  const fetchData = useCallback(async (page?: number) => {
    setLoading(true)
    try {
      const response: any = await getDefects(Number(projectId), {
        page: (page ?? pagination.current) - 1,
        size: pagination.pageSize,
      })
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0, current: page ?? prev.current }))
      }
    } catch (err: any) {
      showError(err, '获取缺陷列表失败')
    } finally {
      setLoading(false)
    }
  }, [projectId, pagination.current, pagination.pageSize, message])

  useEffect(() => {
    fetchData()
  }, [pagination.current, pagination.pageSize]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Stats ──

  const stats = useMemo(() => {
    const all = data.length
    const newCount = data.filter((d) => d.status === 'NEW').length
    const open = data.filter((d) => d.status === 'OPEN' || d.status === 'IN_PROGRESS').length
    const resolved = data.filter((d) => d.status === 'RESOLVED' || d.status === 'VERIFIED').length
    const critical = data.filter((d) => d.severity === 'CRITICAL').length
    return { all, newCount, open, resolved, critical }
  }, [data])

  // ── Handlers ──

  const handleTableChange = (pag: any) => {
    setPagination((prev) => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))
  }

  const openCreate = () => {
    setEditingDefect(null)
    form.resetFields()
    setModalVisible(true)
  }

  const openEdit = (record: any) => {
    setEditingDefect(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const openDetail = (record: any) => {
    setViewingDefect(record)
    setDrawerVisible(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      if (editingDefect) {
        await updateDefect(Number(projectId), editingDefect.id, values)
        message.success('更新成功')
      } else {
        await createDefect(Number(projectId), values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (err: any) {
      showError(err, editingDefect ? '更新失败' : '创建失败')
    }
  }

  const handleResolve = async (record: any) => {
    try {
      await resolveDefect(Number(projectId), record.id, { resolution: '已修复', resolvedBy: 'admin' })
      message.success('已标记为已解决')
      fetchData()
    } catch (err: any) {
      showError(err, '解决缺陷失败')
    }
  }

  const handleClose = async (record: any) => {
    try {
      await closeDefect(Number(projectId), record.id, { closedBy: 'admin' })
      message.success('已关闭')
      fetchData()
    } catch (err: any) {
      showError(err, '关闭缺陷失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteDefect(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch (err: any) {
      showError(err, '删除失败')
    }
  }

  // ── Columns ──

  const columns = [
    {
      title: '缺陷编号',
      dataIndex: 'defectNumber',
      key: 'defectNumber',
      width: 150,
      render: (text: string, record: any) => (
        <span className="defect-number-link" onClick={() => openDetail(record)}>
          {text || '—'}
        </span>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: '严重性',
      dataIndex: 'severity',
      key: 'severity',
      width: 90,
      render: (severity: string) => {
        const info = SEVERITY_MAP[severity]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{severity}</Tag>
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority: string) => {
        const info = PRIORITY_MAP[priority]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{priority}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const info = STATUS_MAP[status]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{status}</Tag>
      },
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => {
        const info = TYPE_MAP[type]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{type}</Tag>
      },
    },
    {
      title: '指派给',
      dataIndex: 'assignedTo',
      key: 'assignedTo',
      width: 100,
      render: (text: string) => text || '—',
    },
    {
      title: '版本',
      dataIndex: 'foundInVersion',
      key: 'foundInVersion',
      width: 90,
      render: (text: string) => text || '—',
    },
    {
      title: '重开',
      dataIndex: 'reopenCount',
      key: 'reopenCount',
      width: 70,
      render: (count: number) => count > 0 ? <Badge count={count} style={{ backgroundColor: '#ff4d4f' }} /> : '—',
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Space size={4}>
          <Tooltip title="查看详情">
            <Button type="text" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)} />
          </Tooltip>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          </Tooltip>
          {(record.status === 'OPEN' || record.status === 'IN_PROGRESS' || record.status === 'REOPENED') && (
            <Tooltip title="标记已解决">
              <Button type="text" size="small" icon={<CheckOutlined />} onClick={() => handleResolve(record)} />
            </Tooltip>
          )}
          {record.status === 'RESOLVED' && (
            <Tooltip title="关闭缺陷">
              <Button type="text" size="small" icon={<CloseOutlined />} onClick={() => handleClose(record)} />
            </Tooltip>
          )}
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Tooltip title="删除">
              <Button type="text" size="small" icon={<DeleteOutlined />} danger />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // ── Render ──

  return (
    <div>
      {/* Header */}
      <div className="defect-page-header">
        <h2>缺陷管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          提交缺陷
        </Button>
      </div>

      {/* Stats */}
      <div className="defect-stats-row">
        <div className="defect-stat-card">
          <div className="defect-stat-icon" style={{ background: 'rgba(45,212,191,0.14)', color: 'var(--accent)' }}>
            <FileTextOutlined />
          </div>
          <div className="defect-stat-info">
            <span className="defect-stat-label">总缺陷</span>
            <span className="defect-stat-value">{stats.all}</span>
          </div>
        </div>
        <div className="defect-stat-card">
          <div className="defect-stat-icon" style={{ background: 'rgba(250,173,20,0.14)', color: '#faad14' }}>
            <BugOutlined />
          </div>
          <div className="defect-stat-info">
            <span className="defect-stat-label">新建</span>
            <span className="defect-stat-value">{stats.newCount}</span>
          </div>
        </div>
        <div className="defect-stat-card">
          <div className="defect-stat-icon" style={{ background: 'rgba(122,162,255,0.14)', color: 'var(--accent-secondary)' }}>
            <BranchesOutlined />
          </div>
          <div className="defect-stat-info">
            <span className="defect-stat-label">处理中</span>
            <span className="defect-stat-value">{stats.open}</span>
          </div>
        </div>
        <div className="defect-stat-card">
          <div className="defect-stat-icon" style={{ background: 'rgba(82,196,26,0.14)', color: '#52c41a' }}>
            <SafetyCertificateOutlined />
          </div>
          <div className="defect-stat-info">
            <span className="defect-stat-label">已解决</span>
            <span className="defect-stat-value">{stats.resolved}</span>
          </div>
        </div>
        <div className="defect-stat-card">
          <div className="defect-stat-icon" style={{ background: 'rgba(255,77,79,0.14)', color: '#ff4d4f' }}>
            <RollbackOutlined />
          </div>
          <div className="defect-stat-info">
            <span className="defect-stat-label">致命</span>
            <span className="defect-stat-value">{stats.critical}</span>
          </div>
        </div>
      </div>

      {/* Table */}
      <Table
        dataSource={data}
        columns={columns}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1300 }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          pageSizeOptions: ['10', '20', '50'],
        }}
        onChange={handleTableChange}
      />

      {/* Create / Edit Modal */}
      <Modal
        title={editingDefect ? '编辑缺陷' : '提交缺陷'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={720}
        forceRender
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入缺陷标题' }]}>
            <Input placeholder="请输入缺陷标题" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="severity" label="严重性" rules={[{ required: true, message: '请选择严重性' }]}>
                <Select placeholder="请选择严重性">
                  {Object.entries(SEVERITY_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请选择优先级' }]}>
                <Select placeholder="请选择优先级">
                  {Object.entries(PRIORITY_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="状态" initialValue="NEW">
                <Select placeholder="请选择状态">
                  {Object.entries(STATUS_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="type" label="缺陷类型">
                <Select placeholder="请选择类型">
                  {Object.entries(TYPE_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="assignedTo" label="指派给">
                <Input placeholder="请输入负责人" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="component" label="所属组件">
                <Input placeholder="例如: 登录模块" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="foundInVersion" label="发现版本">
                <Input placeholder="例如: v1.2.0" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="fixedInVersion" label="修复版本">
                <Input placeholder="例如: v1.2.1" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="environment" label="环境">
                <Input placeholder="测试环境" prefix={<EnvironmentOutlined />} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="reproducibility" label="可复现性">
                <Select placeholder="请选择可复现性">
                  {Object.entries(REPRODUCIBILITY_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="source" label="缺陷来源">
                <Select placeholder="请选择来源">
                  {Object.entries(SOURCE_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="请输入缺陷描述..." />
          </Form.Item>
          <Form.Item name="stepsToReproduce" label="重现步骤">
            <Input.TextArea rows={3} placeholder="请输入重现步骤..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="expectedBehavior" label="预期行为">
                <Input.TextArea rows={2} placeholder="预期行为..." />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="actualBehavior" label="实际行为">
                <Input.TextArea rows={2} placeholder="实际行为..." />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="impact" label="影响范围">
            <Input.TextArea rows={2} placeholder="请输入影响范围..." />
          </Form.Item>
          <Form.Item name="workaround" label="临时解决方案">
            <Input.TextArea rows={2} placeholder="请输入临时解决方案..." />
          </Form.Item>
          <Form.Item name="rootCause" label="根本原因">
            <Input.TextArea rows={2} placeholder="请输入根本原因分析..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={viewingDefect?.defectNumber || '缺陷详情'}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        width={600}
        className="defect-drawer"
      >
        {viewingDefect && (
          <>
            <div className="defect-detail-section">
              <div className="defect-detail-section-title">基本信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="标题" span={2}>
                  {viewingDefect.title}
                </Descriptions.Item>
                <Descriptions.Item label="编号">
                  {viewingDefect.defectNumber || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  {STATUS_MAP[viewingDefect.status] ? (
                    <Tag color={STATUS_MAP[viewingDefect.status].color}>
                      {STATUS_MAP[viewingDefect.status].label}
                    </Tag>
                  ) : viewingDefect.status || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="严重性">
                  {SEVERITY_MAP[viewingDefect.severity] ? (
                    <Tag color={SEVERITY_MAP[viewingDefect.severity].color}>
                      {SEVERITY_MAP[viewingDefect.severity].label}
                    </Tag>
                  ) : viewingDefect.severity || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="优先级">
                  {PRIORITY_MAP[viewingDefect.priority] ? (
                    <Tag color={PRIORITY_MAP[viewingDefect.priority].color}>
                      {PRIORITY_MAP[viewingDefect.priority].label}
                    </Tag>
                  ) : viewingDefect.priority || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="类型">
                  {TYPE_MAP[viewingDefect.type] ? (
                    <Tag color={TYPE_MAP[viewingDefect.type].color}>
                      {TYPE_MAP[viewingDefect.type].label}
                    </Tag>
                  ) : viewingDefect.type || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="指派给">
                  {viewingDefect.assignedTo || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="所属组件">
                  {viewingDefect.component || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="发现版本">
                  {viewingDefect.foundInVersion || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="修复版本">
                  {viewingDefect.fixedInVersion || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="环境">
                  {viewingDefect.environment || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="可复现性">
                  {REPRODUCIBILITY_MAP[viewingDefect.reproducibility] || viewingDefect.reproducibility || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="来源">
                  {SOURCE_MAP[viewingDefect.source] || viewingDefect.source || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="重开次数" span={2}>
                  {viewingDefect.reopenCount > 0 ? (
                    <Badge count={viewingDefect.reopenCount} style={{ backgroundColor: '#ff4d4f' }} />
                  ) : '0'}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <div className="defect-detail-section">
              <div className="defect-detail-section-title">缺陷详情</div>
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="描述">
                  {viewingDefect.description || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="重现步骤">
                  {viewingDefect.stepsToReproduce || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="预期行为">
                  {viewingDefect.expectedBehavior || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="实际行为">
                  {viewingDefect.actualBehavior || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <div className="defect-detail-section">
              <div className="defect-detail-section-title">分析与处理</div>
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="影响范围">
                  {viewingDefect.impact || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="临时解决方案">
                  {viewingDefect.workaround || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="根本原因">
                  {viewingDefect.rootCause || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="解决方案">
                  {viewingDefect.resolution || <span className="defect-detail-empty">无</span>}
                </Descriptions.Item>
              </Descriptions>
            </div>

            {(viewingDefect.resolvedAt || viewingDefect.closedAt || viewingDefect.verifiedAt) && (
              <div className="defect-detail-section">
                <div className="defect-detail-section-title">处理记录</div>
                <Descriptions column={2} size="small" bordered>
                  <Descriptions.Item label="解决人">
                    {viewingDefect.resolvedBy || '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="解决时间">
                    {viewingDefect.resolvedAt ? dayjs(viewingDefect.resolvedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="验证人">
                    {viewingDefect.verifiedBy || '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="验证时间">
                    {viewingDefect.verifiedAt ? dayjs(viewingDefect.verifiedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                  </Descriptions.Item>
                  <Descriptions.Item label="关闭时间" span={2}>
                    {viewingDefect.closedAt ? dayjs(viewingDefect.closedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                  </Descriptions.Item>
                </Descriptions>
              </div>
            )}

            <div className="defect-detail-section">
              <div className="defect-detail-section-title">审计信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="报告人">{viewingDefect.reportedBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {viewingDefect.createdAt ? dayjs(viewingDefect.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="更新人">{viewingDefect.updatedBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {viewingDefect.updatedAt ? dayjs(viewingDefect.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <Space style={{ marginTop: 8 }}>
              <Button icon={<EditOutlined />} onClick={() => { setDrawerVisible(false); openEdit(viewingDefect) }}>
                编辑
              </Button>
            </Space>
          </>
        )}
      </Drawer>
    </div>
  )
}

export default DefectList
