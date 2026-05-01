import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag,
  Drawer, Descriptions, Row, Col, Tooltip,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  SendOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SearchOutlined, ReloadOutlined,
  FileTextOutlined, ClockCircleOutlined, CheckSquareOutlined, StopOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getRequirements, createRequirement, updateRequirement,
  deleteRequirement, updateRequirementStatus, getRootRequirements,
  type RequirementQueryParams,
} from '../api/requirement'
import useMessage from '../hooks/useMessage'

// ── Enum maps ──────────────────────────────────────────

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  FUNCTIONAL: { label: '功能需求', color: 'blue' },
  NON_FUNCTIONAL: { label: '非功能需求', color: 'purple' },
  INTERFACE: { label: '接口需求', color: 'cyan' },
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
}

// ── Component ──────────────────────────────────────────

const RequirementList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const message = useMessage()
  const [form] = Form.useForm()

  // Data
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

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
    } catch {
      message.error('获取需求列表失败')
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
    } catch {
      message.error(editingRequirement ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteRequirement(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleStatusChange = async (id: number, newStatus: string, label: string) => {
    try {
      await updateRequirementStatus(Number(projectId), id, newStatus)
      message.success(`已${label}`)
      fetchData()
    } catch {
      message.error('操作失败')
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
            <Input.TextArea rows={4} placeholder="请输入需求详细描述..." />
          </Form.Item>
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
              </Descriptions>
            </div>

            <div className="req-detail-section">
              <div className="req-detail-section-title">描述</div>
              <div className="req-detail-desc">
                {viewingRequirement.description || <span className="req-detail-empty">暂无描述</span>}
              </div>
            </div>

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
