import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag,
  Drawer, Descriptions, Row, Col, Tooltip, Progress,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  SendOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SearchOutlined, ReloadOutlined,
  FileTextOutlined, ClockCircleOutlined, CheckSquareOutlined,
  LinkOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getRequirements, createRequirement, updateRequirement,
  deleteRequirement, updateRequirementStatus, getRootRequirements,
  getRequirementCoverage,
  type RequirementQueryParams,
} from '../api/requirement'
import useMessage from '../hooks/useMessage'

// ── Enum maps ──────────────────────────────────────────

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  FUNCTIONAL: { label: '功能需求', color: 'blue' },
  NON_FUNCTIONAL: { label: '非功能需求', color: 'purple' },
  INTERFACE: { label: '接口需求', color: 'cyan' },
  DATA: { label: '数据需求', color: 'geekblue' },
  SECURITY: { label: '安全需求', color: 'magenta' },
}

const PRIORITY_MAP: Record<string, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  MEDIUM: { label: '中', color: 'orange' },
  LOW: { label: '低', color: 'green' },
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  REVIEWING: { label: '评审中', color: 'processing' },
  APPROVED: { label: '已批准', color: 'success' },
  REJECTED: { label: '已拒绝', color: 'error' },
  IMPLEMENTING: { label: '实现中', color: 'blue' },
  TESTING: { label: '测试中', color: 'purple' },
  COMPLETED: { label: '已完成', color: 'success' },
}

const SOURCE_MAP: Record<string, string> = {
  CUSTOMER: '客户反馈',
  INTERNAL: '内部规划',
  MARKET: '市场调研',
  COMPETITOR: '竞品分析',
  REGULATION: '法规要求',
  OTHER: '其他',
}

// ── Component ──────────────────────────────────────────

const RequirementList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message, showError } = useMessage()
  const [form] = Form.useForm()

  // Data
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  // Coverage data for drawer
  const [coverageData, setCoverageData] = useState<any>(null)
  const [coverageLoading, setCoverageLoading] = useState(false)

  // Filters
  const [searchText, setSearchText] = useState('')
  const [filterStatus, setFilterStatus] = useState<string | undefined>()
  const [filterPriority, setFilterPriority] = useState<string | undefined>()
  const [filterType, setFilterType] = useState<string | undefined>()

  // Modal / Drawer
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRequirement, setEditingRequirement] = useState<any>(null)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [viewingRequirement, setViewingRequirement] = useState<any>(null)

  // Parent requirements list (for form select)
  const [rootRequirements, setRootRequirements] = useState<any[]>([])

  // ── Fetch ──

  const fetchData = useCallback(async (page?: number) => {
    setLoading(true)
    try {
      const params: RequirementQueryParams = {
        page: (page ?? pagination.current) - 1,
        size: pagination.pageSize,
      }
      if (searchText) params.search = searchText
      if (filterStatus) params.status = filterStatus
      if (filterPriority) params.priority = filterPriority
      if (filterType) params.type = filterType

      const response: any = await getRequirements(Number(projectId), params)
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0, current: page ?? prev.current }))
      }
    } catch (err: any) {
      showError(err, '获取需求列表失败')
    } finally {
      setLoading(false)
    }
  }, [projectId, pagination.current, pagination.pageSize, searchText, filterStatus, filterPriority, filterType, message])

  useEffect(() => {
    fetchData()
  }, [pagination.current, pagination.pageSize]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    // Load root requirements for parent select
    if (projectId) {
      getRootRequirements(Number(projectId))
        .then((res: any) => {
          if (res.code === 200) setRootRequirements(res.data || [])
        })
        .catch(() => { /* ignore */ })
    }
  }, [projectId])

  // ── Stats ──

  const stats = useMemo(() => {
    const all = data.length
    const draft = data.filter((d) => d.status === 'DRAFT').length
    const approved = data.filter((d) => d.status === 'APPROVED').length
    const reviewing = data.filter((d) => d.status === 'REVIEWING').length
    return { all, draft, approved, reviewing }
  }, [data])

  // ── Handlers ──

  const handleSearch = () => {
    setPagination((prev) => ({ ...prev, current: 1 }))
    fetchData(1)
  }

  const handleReset = () => {
    setSearchText('')
    setFilterStatus(undefined)
    setFilterPriority(undefined)
    setFilterType(undefined)
    setPagination((prev) => ({ ...prev, current: 1 }))
    // Fetch after state updates via a micro-task
    setTimeout(() => fetchData(1), 0)
  }

  const handleTableChange = (pag: any) => {
    setPagination((prev) => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))
  }

  const openCreate = () => {
    setEditingRequirement(null)
    form.resetFields()
    setModalVisible(true)
  }

  const openEdit = (record: any) => {
    setEditingRequirement(record)
    form.setFieldsValue({
      ...record,
      parentId: record.parent?.id ?? undefined,
    })
    setModalVisible(true)
  }

  const openDetail = (record: any) => {
    setViewingRequirement(record)
    setDrawerVisible(true)
    setCoverageData(null)
    if (projectId && record.id) {
      setCoverageLoading(true)
      getRequirementCoverage(Number(projectId), record.id)
        .then((res: any) => {
          if (res.code === 200) setCoverageData(res.data)
        })
        .catch(() => { /* ignore */ })
        .finally(() => setCoverageLoading(false))
    }
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      // Build payload with parent relation
      const payload = { ...values }
      if (values.parentId) {
        payload.parent = { id: values.parentId }
        delete payload.parentId
      } else {
        payload.parent = null
        delete payload.parentId
      }

      if (editingRequirement) {
        await updateRequirement(Number(projectId), editingRequirement.id, payload)
        message.success('更新成功')
      } else {
        await createRequirement(Number(projectId), payload)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (err: any) {
      showError(err, editingRequirement ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteRequirement(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch (err: any) {
      showError(err, '删除失败')
    }
  }

  const handleStatusChange = async (id: number, newStatus: string, label: string) => {
    try {
      await updateRequirementStatus(Number(projectId), id, newStatus)
      message.success(`已${label}`)
      fetchData()
    } catch (err: any) {
      showError(err, '操作失败')
    }
  }

  // ── Columns ──

  const columns = [
    {
      title: '需求编号',
      dataIndex: 'requirementNumber',
      key: 'requirementNumber',
      width: 180,
      render: (text: string, record: any) => (
        <span className="req-number-link" onClick={() => openDetail(record)}>
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
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => {
        const info = TYPE_MAP[type]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{type}</Tag>
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 90,
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
      title: '指派给',
      dataIndex: 'assignedTo',
      key: 'assignedTo',
      width: 110,
      render: (text: string) => text || <span style={{ color: 'var(--text-muted)' }}>未指派</span>,
    },
    {
      title: '故事点',
      dataIndex: 'storyPoints',
      key: 'storyPoints',
      width: 80,
      render: (val: number) => val ?? '—',
    },
    {
      title: '预估工时',
      dataIndex: 'estimatedHours',
      key: 'estimatedHours',
      width: 90,
      render: (val: number) => val ?? '—',
    },
    {
      title: '实际工时',
      dataIndex: 'actualHours',
      key: 'actualHours',
      width: 90,
      render: (val: number) => val ?? '—',
    },
    {
      title: '截止日期',
      dataIndex: 'dueDate',
      key: 'dueDate',
      width: 110,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD') : '—',
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
      width: 100,
      render: (source: string) => SOURCE_MAP[source] || source || '—',
    },
    {
      title: '用例覆盖',
      key: 'coverage',
      width: 120,
      render: (_: any, record: any) => {
        const count = record.testCaseCount ?? 0
        if (count === 0) {
          return <Tag color="error">未覆盖</Tag>
        }
        return (
          <Tooltip title={`关联 ${count} 个测试用例`}>
            <Tag color="success" style={{ cursor: 'pointer' }} onClick={() => openDetail(record)}>
              <LinkOutlined /> {count} 个用例
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: '创建人',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 100,
      render: (text: string) => text || '—',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '—',
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
          {record.status === 'DRAFT' && (
            <Tooltip title="提交评审">
              <Button
                type="text"
                size="small"
                icon={<SendOutlined />}
                style={{ color: 'var(--accent)' }}
                onClick={() => handleStatusChange(record.id, 'REVIEWING', '提交评审')}
              />
            </Tooltip>
          )}
          {record.status === 'REVIEWING' && (
            <>
              <Tooltip title="批准">
                <Button
                  type="text"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  style={{ color: '#52c41a' }}
                  onClick={() => handleStatusChange(record.id, 'APPROVED', '批准')}
                />
              </Tooltip>
              <Tooltip title="拒绝">
                <Button
                  type="text"
                  size="small"
                  icon={<CloseCircleOutlined />}
                  style={{ color: '#ff4d4f' }}
                  onClick={() => handleStatusChange(record.id, 'REJECTED', '拒绝')}
                />
              </Tooltip>
            </>
          )}
          <Popconfirm title="确定删除此需求？" onConfirm={() => handleDelete(record.id)}>
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
      <div className="req-page-header">
        <h2>需求管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建需求
        </Button>
      </div>

      {/* Stats */}
      <div className="req-stats-row">
        <div className="req-stat-card">
          <div className="req-stat-icon" style={{ background: 'rgba(45,212,191,0.14)', color: 'var(--accent)' }}>
            <FileTextOutlined />
          </div>
          <div className="req-stat-info">
            <span className="req-stat-label">总需求</span>
            <span className="req-stat-value">{stats.all}</span>
          </div>
        </div>
        <div className="req-stat-card">
          <div className="req-stat-icon" style={{ background: 'rgba(122,162,255,0.14)', color: 'var(--accent-secondary)' }}>
            <ClockCircleOutlined />
          </div>
          <div className="req-stat-info">
            <span className="req-stat-label">草稿</span>
            <span className="req-stat-value">{stats.draft}</span>
          </div>
        </div>
        <div className="req-stat-card">
          <div className="req-stat-icon" style={{ background: 'rgba(250,173,20,0.14)', color: '#faad14' }}>
            <SendOutlined />
          </div>
          <div className="req-stat-info">
            <span className="req-stat-label">评审中</span>
            <span className="req-stat-value">{stats.reviewing}</span>
          </div>
        </div>
        <div className="req-stat-card">
          <div className="req-stat-icon" style={{ background: 'rgba(82,196,26,0.14)', color: '#52c41a' }}>
            <CheckSquareOutlined />
          </div>
          <div className="req-stat-info">
            <span className="req-stat-label">已批准</span>
            <span className="req-stat-value">{stats.approved}</span>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="req-filter-bar">
        <Input.Search
          placeholder="搜索需求标题..."
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          onSearch={handleSearch}
          enterButton={<SearchOutlined />}
        />
        <Select
          placeholder="状态"
          allowClear
          value={filterStatus}
          onChange={setFilterStatus}
          options={Object.entries(STATUS_MAP).map(([k, v]) => ({ label: v.label, value: k }))}
        />
        <Select
          placeholder="优先级"
          allowClear
          value={filterPriority}
          onChange={setFilterPriority}
          options={Object.entries(PRIORITY_MAP).map(([k, v]) => ({ label: v.label, value: k }))}
        />
        <Select
          placeholder="类型"
          allowClear
          value={filterType}
          onChange={setFilterType}
          options={Object.entries(TYPE_MAP).map(([k, v]) => ({ label: v.label, value: k }))}
        />
        <div className="req-filter-actions">
          <Button icon={<SearchOutlined />} type="primary" onClick={handleSearch}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
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
        title={editingRequirement ? '编辑需求' : '新建需求'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={640}
        forceRender
        destroyOnHidden={false}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入需求标题' }]}>
            <Input placeholder="请输入需求标题" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择需求类型' }]}>
                <Select placeholder="请选择类型">
                  {Object.entries(TYPE_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请选择优先级' }]}>
                <Select placeholder="请选择优先级">
                  {Object.entries(PRIORITY_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue="DRAFT">
                <Select placeholder="请选择状态">
                  {Object.entries(STATUS_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="assignedTo" label="指派给">
                <Input placeholder="请输入负责人" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="storyPoints" label="故事点">
                <Input type="number" min={0} placeholder="故事点" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="estimatedHours" label="预估工时 (小时)">
                <Input type="number" min={0} placeholder="预估工时" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="actualHours" label="实际工时 (小时)">
                <Input type="number" min={0} placeholder="实际工时" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="dueDate" label="截止日期">
                <Input type="date" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="source" label="需求来源">
                <Select placeholder="请选择来源">
                  {Object.entries(SOURCE_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="parentId" label="父需求">
            <Select
              placeholder="无（顶层需求）"
              allowClear
              showSearch
              optionFilterProp="label"
              options={rootRequirements
                .filter((r) => r.id !== editingRequirement?.id)
                .map((r) => ({
                  label: `${r.requirementNumber || ''} ${r.title}`,
                  value: r.id,
                }))}
            />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入需求详细描述..." />
          </Form.Item>
          <Form.Item name="acceptanceCriteria" label="验收标准">
            <Input.TextArea rows={3} placeholder="请输入验收标准..." />
          </Form.Item>
          {editingRequirement?.status === 'REJECTED' && (
            <Form.Item name="rejectedReason" label="拒绝原因">
              <Input.TextArea rows={2} placeholder="请输入拒绝原因..." />
            </Form.Item>
          )}
        </Form>
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={viewingRequirement?.requirementNumber || '需求详情'}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        width={560}
        className="req-drawer"
      >
        {viewingRequirement && (
          <>
            <div className="req-detail-section">
              <div className="req-detail-section-title">基本信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="标题" span={2}>
                  {viewingRequirement.title}
                </Descriptions.Item>
                <Descriptions.Item label="编号">
                  {viewingRequirement.requirementNumber || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="类型">
                  {TYPE_MAP[viewingRequirement.type] ? (
                    <Tag color={TYPE_MAP[viewingRequirement.type].color}>
                      {TYPE_MAP[viewingRequirement.type].label}
                    </Tag>
                  ) : viewingRequirement.type || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="优先级">
                  {PRIORITY_MAP[viewingRequirement.priority] ? (
                    <Tag color={PRIORITY_MAP[viewingRequirement.priority].color}>
                      {PRIORITY_MAP[viewingRequirement.priority].label}
                    </Tag>
                  ) : viewingRequirement.priority || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  {STATUS_MAP[viewingRequirement.status] ? (
                    <Tag color={STATUS_MAP[viewingRequirement.status].color}>
                      {STATUS_MAP[viewingRequirement.status].label}
                    </Tag>
                  ) : viewingRequirement.status || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="指派给">
                  {viewingRequirement.assignedTo || <span className="req-detail-empty">未指派</span>}
                </Descriptions.Item>
                <Descriptions.Item label="父需求">
                  {viewingRequirement.parent?.title || <span className="req-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="故事点">
                  {viewingRequirement.storyPoints ?? '—'}
                </Descriptions.Item>
                <Descriptions.Item label="预估工时">
                  {viewingRequirement.estimatedHours ? `${viewingRequirement.estimatedHours} 小时` : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="实际工时">
                  {viewingRequirement.actualHours ? `${viewingRequirement.actualHours} 小时` : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="截止日期">
                  {viewingRequirement.dueDate ? dayjs(viewingRequirement.dueDate).format('YYYY-MM-DD') : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="完成日期">
                  {viewingRequirement.completedDate ? dayjs(viewingRequirement.completedDate).format('YYYY-MM-DD') : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="需求来源">
                  {SOURCE_MAP[viewingRequirement.source] || viewingRequirement.source || '—'}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <div className="req-detail-section">
              <div className="req-detail-section-title">描述</div>
              <div className="req-detail-desc">
                {viewingRequirement.description || <span className="req-detail-empty">暂无描述</span>}
              </div>
            </div>

            <div className="req-detail-section">
              <div className="req-detail-section-title">验收标准</div>
              <div className="req-detail-desc">
                {viewingRequirement.acceptanceCriteria || <span className="req-detail-empty">暂无验收标准</span>}
              </div>
            </div>

            <div className="req-detail-section">
              <div className="req-detail-section-title">
                <LinkOutlined /> 关联测试用例
              </div>
              {coverageLoading ? (
                <div style={{ textAlign: 'center', padding: 16, color: 'var(--text-muted)' }}>加载中...</div>
              ) : coverageData && coverageData.totalTestCases > 0 ? (
                <>
                  <div style={{ marginBottom: 12 }}>
                    <Space>
                      <span>关联用例数: <strong>{coverageData.totalTestCases}</strong></span>
                      <span>激活用例数: <strong style={{ color: '#52c41a' }}>{coverageData.activeTestCases}</strong></span>
                      <span>覆盖率: <strong style={{ color: coverageData.coverageRate >= 80 ? '#52c41a' : coverageData.coverageRate >= 50 ? '#faad14' : '#ff4d4f' }}>{coverageData.coverageRate}%</strong></span>
                    </Space>
                    <Progress
                      percent={coverageData.coverageRate}
                      size="small"
                      status={coverageData.coverageRate >= 80 ? 'success' : coverageData.coverageRate >= 50 ? 'normal' : 'exception'}
                      style={{ marginTop: 8 }}
                    />
                  </div>
                  <Table
                    dataSource={coverageData.testCases || []}
                    rowKey="id"
                    size="small"
                    pagination={false}
                    columns={[
                      {
                        title: '编号',
                        dataIndex: 'caseNumber',
                        width: 140,
                        render: (text: string) => (
                          <Tag
                            color="blue"
                            style={{ cursor: 'pointer' }}
                            onClick={() => navigate(`/projects/${projectId}/testcases`)}
                          >
                            {text || '—'}
                          </Tag>
                        ),
                      },
                      { title: '标题', dataIndex: 'title', ellipsis: true },
                      {
                        title: '类型',
                        dataIndex: 'type',
                        width: 100,
                        render: (type: string) => {
                          const TC_TYPE_MAP: Record<string, { label: string; color: string }> = {
                            FUNCTIONAL: { label: '功能测试', color: 'blue' },
                            INTEGRATION: { label: '集成测试', color: 'purple' },
                            PERFORMANCE: { label: '性能测试', color: 'orange' },
                            SECURITY: { label: '安全测试', color: 'red' },
                          }
                          const info = TC_TYPE_MAP[type]
                          return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{type}</Tag>
                        },
                      },
                      {
                        title: '优先级',
                        dataIndex: 'priority',
                        width: 80,
                        render: (priority: string) => {
                          const PRIORITY_COLORS: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }
                          const PRIORITY_LABELS: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
                          return <Tag color={PRIORITY_COLORS[priority]}>{PRIORITY_LABELS[priority] || priority}</Tag>
                        },
                      },
                      {
                        title: '状态',
                        dataIndex: 'status',
                        width: 90,
                        render: (status: string) => {
                          const STATUS_COLORS: Record<string, string> = { DRAFT: 'default', ACTIVE: 'success', OBSOLETE: 'error' }
                          const STATUS_LABELS: Record<string, string> = { DRAFT: '草稿', ACTIVE: '激活', OBSOLETE: '废弃' }
                          return <Tag color={STATUS_COLORS[status]}>{STATUS_LABELS[status] || status}</Tag>
                        },
                      },
                    ]}
                  />
                </>
              ) : (
                <div style={{ textAlign: 'center', padding: 16, color: 'var(--text-muted)' }}>
                  暂无关联的测试用例
                </div>
              )}
            </div>

            {viewingRequirement.rejectedReason && (
              <div className="req-detail-section">
                <div className="req-detail-section-title">拒绝原因</div>
                <div className="req-detail-desc" style={{ color: '#ff4d4f' }}>
                  {viewingRequirement.rejectedReason}
                </div>
              </div>
            )}

            <div className="req-detail-section">
              <div className="req-detail-section-title">审计信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="创建人">{viewingRequirement.createdBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {viewingRequirement.createdAt ? dayjs(viewingRequirement.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="更新人">{viewingRequirement.updatedBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {viewingRequirement.updatedAt ? dayjs(viewingRequirement.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <Space style={{ marginTop: 8 }}>
              <Button icon={<EditOutlined />} onClick={() => { setDrawerVisible(false); openEdit(viewingRequirement) }}>
                编辑
              </Button>
              {viewingRequirement.status === 'DRAFT' && (
                <Button
                  icon={<SendOutlined />}
                  style={{ color: 'var(--accent)', borderColor: 'var(--accent)' }}
                  onClick={() => { handleStatusChange(viewingRequirement.id, 'REVIEWING', '提交评审'); setDrawerVisible(false) }}
                >
                  提交评审
                </Button>
              )}
              {viewingRequirement.status === 'REVIEWING' && (
                <>
                  <Button
                    icon={<CheckCircleOutlined />}
                    style={{ color: '#52c41a', borderColor: '#52c41a' }}
                    onClick={() => { handleStatusChange(viewingRequirement.id, 'APPROVED', '批准'); setDrawerVisible(false) }}
                  >
                    批准
                  </Button>
                  <Button
                    icon={<CloseCircleOutlined />}
                    danger
                    onClick={() => { handleStatusChange(viewingRequirement.id, 'REJECTED', '拒绝'); setDrawerVisible(false) }}
                  >
                    拒绝
                  </Button>
                </>
              )}
            </Space>
          </>
        )}
      </Drawer>
    </div>
  )
}

export default RequirementList
