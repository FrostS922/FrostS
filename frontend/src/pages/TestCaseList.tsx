import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag,
  Drawer, Descriptions, Row, Col, Tooltip, Progress,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  CheckCircleOutlined, CloseCircleOutlined, PlayCircleOutlined,
  BugOutlined, FileTextOutlined, LinkOutlined,
  RobotOutlined, UserOutlined, TagOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getTestCases, createTestCase, updateTestCase, deleteTestCase,
} from '../api/testCase'
import { getRootRequirements } from '../api/requirement'
import useMessage from '../hooks/useMessage'

// ── Enum maps ──────────────────────────────────────────

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  FUNCTIONAL: { label: '功能测试', color: 'blue' },
  INTEGRATION: { label: '集成测试', color: 'purple' },
  PERFORMANCE: { label: '性能测试', color: 'orange' },
  SECURITY: { label: '安全测试', color: 'red' },
  USABILITY: { label: '易用性测试', color: 'cyan' },
  COMPATIBILITY: { label: '兼容性测试', color: 'geekblue' },
  REGRESSION: { label: '回归测试', color: 'magenta' },
}

const PRIORITY_MAP: Record<string, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  MEDIUM: { label: '中', color: 'orange' },
  LOW: { label: '低', color: 'green' },
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  ACTIVE: { label: '激活', color: 'success' },
  OBSOLETE: { label: '废弃', color: 'error' },
  UNDER_REVIEW: { label: '评审中', color: 'processing' },
}

const REVIEW_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待评审', color: 'default' },
  PASSED: { label: '通过', color: 'success' },
  FAILED: { label: '未通过', color: 'error' },
}

// ── Component ──────────────────────────────────────────

const TestCaseList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { message, showError } = useMessage()
  const [form] = Form.useForm()

  // Data
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  // Requirements list for form select
  const [requirements, setRequirements] = useState<any[]>([])

  // Filters (reserved for future filter implementation)
  const _searchText = useState('')
  void _searchText
  const _filterStatus = useState<string | undefined>()
  void _filterStatus
  const _filterPriority = useState<string | undefined>()
  void _filterPriority
  const _filterType = useState<string | undefined>()
  void _filterType

  // Modal / Drawer
  const [modalVisible, setModalVisible] = useState(false)
  const [editingCase, setEditingCase] = useState<any>(null)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [viewingCase, setViewingCase] = useState<any>(null)

  // ── Fetch ──

  const fetchData = useCallback(async (page?: number) => {
    setLoading(true)
    try {
      const params: any = {
        page: (page ?? pagination.current) - 1,
        size: pagination.pageSize,
      }
      const response: any = await getTestCases(Number(projectId), params)
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0, current: page ?? prev.current }))
      }
    } catch (err: any) {
      showError(err, '获取测试用例列表失败')
    } finally {
      setLoading(false)
    }
  }, [projectId, pagination.current, pagination.pageSize, message])

  useEffect(() => {
    fetchData()
  }, [pagination.current, pagination.pageSize]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (projectId) {
      getRootRequirements(Number(projectId))
        .then((res: any) => {
          if (res.code === 200) setRequirements(res.data || [])
        })
        .catch(() => { /* ignore */ })
    }
  }, [projectId])

  // ── Stats ──

  const stats = useMemo(() => {
    const all = data.length
    const draft = data.filter((d) => d.status === 'DRAFT').length
    const active = data.filter((d) => d.status === 'ACTIVE').length
    const automated = data.filter((d) => d.isAutomated).length
    const passRate = all > 0
      ? Math.round(data.reduce((sum, d) => sum + (d.passCount || 0), 0) /
          Math.max(data.reduce((sum, d) => sum + (d.totalExecutions || 0), 0), 1) * 100)
      : 0
    return { all, draft, active, automated, passRate }
  }, [data])

  // ── Handlers ──

  const handleTableChange = (pag: any) => {
    setPagination((prev) => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))
  }

  const openCreate = () => {
    setEditingCase(null)
    form.resetFields()
    setModalVisible(true)
  }

  const openEdit = (record: any) => {
    setEditingCase(record)
    form.setFieldsValue({
      ...record,
      isAutomated: record.isAutomated ?? false,
      requirementId: record.requirement?.id ?? undefined,
    })
    setModalVisible(true)
  }

  const openDetail = (record: any) => {
    setViewingCase(record)
    setDrawerVisible(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      const payload = { ...values }
      if (values.requirementId) {
        payload.requirement = { id: values.requirementId }
        delete payload.requirementId
      } else {
        payload.requirement = null
        delete payload.requirementId
      }
      if (editingCase) {
        await updateTestCase(Number(projectId), editingCase.id, payload)
        message.success('更新成功')
      } else {
        await createTestCase(Number(projectId), payload)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (err: any) {
      showError(err, editingCase ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteTestCase(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch (err: any) {
      showError(err, '删除失败')
    }
  }

  // ── Columns ──

  const columns = [
    {
      title: '用例编号',
      dataIndex: 'caseNumber',
      key: 'caseNumber',
      width: 160,
      render: (text: string, record: any) => (
        <span className="case-number-link" onClick={() => openDetail(record)}>
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
      title: '自动化',
      dataIndex: 'isAutomated',
      key: 'isAutomated',
      width: 90,
      render: (isAutomated: boolean) =>
        isAutomated ? <Tag color="blue"><RobotOutlined /> 是</Tag> : <Tag>否</Tag>,
    },
    {
      title: '关联需求',
      key: 'requirement',
      width: 160,
      render: (_: any, record: any) => {
        if (!record.requirement) return <span style={{ color: 'var(--text-muted)' }}>未关联</span>
        return (
          <Tooltip title={record.requirement.title}>
            <Tag
              color="blue"
              style={{ cursor: 'pointer' }}
              onClick={() => navigate(`/projects/${projectId}/requirements`)}
            >
              <LinkOutlined /> {record.requirement.requirementNumber || '查看'}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: '评审',
      dataIndex: 'reviewStatus',
      key: 'reviewStatus',
      width: 90,
      render: (reviewStatus: string) => {
        const info = REVIEW_STATUS_MAP[reviewStatus]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{reviewStatus}</Tag>
      },
    },
    {
      title: '执行统计',
      key: 'executionStats',
      width: 140,
      render: (_: any, record: any) => {
        const total = record.totalExecutions || 0
        const pass = record.passCount || 0
        const fail = record.failCount || 0
        if (total === 0) return <span style={{ color: 'var(--text-muted)' }}>未执行</span>
        return (
          <Space size={4}>
            <Tooltip title={`通过: ${pass}`}>
              <span style={{ color: '#52c41a' }}><CheckCircleOutlined /> {pass}</span>
            </Tooltip>
            <Tooltip title={`失败: ${fail}`}>
              <span style={{ color: '#ff4d4f' }}><CloseCircleOutlined /> {fail}</span>
            </Tooltip>
            <Tooltip title={`总计: ${total}`}>
              <span style={{ color: 'var(--text-muted)' }}><PlayCircleOutlined /> {total}</span>
            </Tooltip>
          </Space>
        )
      },
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 70,
      render: (text: string) => text || '1.0',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Space size={4}>
          <Tooltip title="查看详情">
            <Button type="text" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)} />
          </Tooltip>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          </Tooltip>
          <Popconfirm title="确定删除此用例？" onConfirm={() => handleDelete(record.id)}>
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
      <div className="tc-page-header">
        <h2>测试用例管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建用例
        </Button>
      </div>

      {/* Stats */}
      <div className="tc-stats-row">
        <div className="tc-stat-card">
          <div className="tc-stat-icon" style={{ background: 'rgba(45,212,191,0.14)', color: 'var(--accent)' }}>
            <FileTextOutlined />
          </div>
          <div className="tc-stat-info">
            <span className="tc-stat-label">总用例</span>
            <span className="tc-stat-value">{stats.all}</span>
          </div>
        </div>
        <div className="tc-stat-card">
          <div className="tc-stat-icon" style={{ background: 'rgba(82,196,26,0.14)', color: '#52c41a' }}>
            <CheckCircleOutlined />
          </div>
          <div className="tc-stat-info">
            <span className="tc-stat-label">激活</span>
            <span className="tc-stat-value">{stats.active}</span>
          </div>
        </div>
        <div className="tc-stat-card">
          <div className="tc-stat-icon" style={{ background: 'rgba(122,162,255,0.14)', color: 'var(--accent-secondary)' }}>
            <RobotOutlined />
          </div>
          <div className="tc-stat-info">
            <span className="tc-stat-label">自动化</span>
            <span className="tc-stat-value">{stats.automated}</span>
          </div>
        </div>
        <div className="tc-stat-card">
          <div className="tc-stat-icon" style={{ background: 'rgba(250,173,20,0.14)', color: '#faad14' }}>
            <BugOutlined />
          </div>
          <div className="tc-stat-info">
            <span className="tc-stat-label">通过率</span>
            <span className="tc-stat-value">{stats.passRate}%</span>
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
        title={editingCase ? '编辑用例' : '新建用例'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={720}
        forceRender
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入用例标题' }]}>
            <Input placeholder="请输入用例标题" />
          </Form.Item>
          <Form.Item name="requirementId" label="关联需求">
            <Select
              placeholder="选择关联需求（可选）"
              allowClear
              showSearch
              optionFilterProp="label"
              options={requirements.map((r) => ({
                label: `${r.requirementNumber || ''} - ${r.title}`,
                value: r.id,
              }))}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
                <Select placeholder="请选择类型">
                  {Object.entries(TYPE_MAP).map(([k, v]) => (
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
              <Form.Item name="status" label="状态" initialValue="DRAFT">
                <Select placeholder="请选择状态">
                  {Object.entries(STATUS_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="executionTime" label="预估执行时间 (分钟)">
                <Input type="number" min={0} placeholder="预估执行时间" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="version" label="版本">
                <Input placeholder="例如: 1.0" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="preconditions" label="前置条件">
            <Input.TextArea rows={2} placeholder="请输入前置条件..." />
          </Form.Item>
          <Form.Item name="steps" label="测试步骤">
            <Input.TextArea rows={4} placeholder="请输入测试步骤..." />
          </Form.Item>
          <Form.Item name="testData" label="测试数据">
            <Input.TextArea rows={2} placeholder="请输入测试数据..." />
          </Form.Item>
          <Form.Item name="expectedResults" label="预期结果">
            <Input.TextArea rows={2} placeholder="请输入预期结果..." />
          </Form.Item>
          <Form.Item name="postconditions" label="后置条件">
            <Input.TextArea rows={2} placeholder="请输入后置条件..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="isAutomated" label="是否自动化" valuePropName="checked">
                <Select placeholder="是否自动化">
                  <Select.Option value={true}>是</Select.Option>
                  <Select.Option value={false}>否</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="automationScript" label="自动化脚本路径">
                <Input placeholder="请输入脚本路径..." />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="reviewer" label="评审人">
                <Input placeholder="请输入评审人" prefix={<UserOutlined />} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reviewStatus" label="评审状态" initialValue="PENDING">
                <Select placeholder="请选择评审状态">
                  {Object.entries(REVIEW_STATUS_MAP).map(([k, v]) => (
                    <Select.Option key={k} value={k}>{v.label}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="reviewComments" label="评审意见">
            <Input.TextArea rows={2} placeholder="请输入评审意见..." />
          </Form.Item>
          <Form.Item name="tags" label="标签">
            <Input placeholder="多个标签用逗号分隔，例如: 登录, 权限, 核心功能" prefix={<TagOutlined />} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={viewingCase?.caseNumber || '用例详情'}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        width={600}
        className="tc-drawer"
      >
        {viewingCase && (
          <>
            <div className="tc-detail-section">
              <div className="tc-detail-section-title">基本信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="标题" span={2}>
                  {viewingCase.title}
                </Descriptions.Item>
                <Descriptions.Item label="编号">
                  {viewingCase.caseNumber || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="版本">
                  {viewingCase.version || '1.0'}
                </Descriptions.Item>
                <Descriptions.Item label="类型">
                  {TYPE_MAP[viewingCase.type] ? (
                    <Tag color={TYPE_MAP[viewingCase.type].color}>
                      {TYPE_MAP[viewingCase.type].label}
                    </Tag>
                  ) : viewingCase.type || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="优先级">
                  {PRIORITY_MAP[viewingCase.priority] ? (
                    <Tag color={PRIORITY_MAP[viewingCase.priority].color}>
                      {PRIORITY_MAP[viewingCase.priority].label}
                    </Tag>
                  ) : viewingCase.priority || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                  {STATUS_MAP[viewingCase.status] ? (
                    <Tag color={STATUS_MAP[viewingCase.status].color}>
                      {STATUS_MAP[viewingCase.status].label}
                    </Tag>
                  ) : viewingCase.status || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="自动化">
                  {viewingCase.isAutomated ? (
                    <Tag color="blue"><RobotOutlined /> 是</Tag>
                  ) : (
                    <Tag>否</Tag>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="评审状态">
                  {REVIEW_STATUS_MAP[viewingCase.reviewStatus] ? (
                    <Tag color={REVIEW_STATUS_MAP[viewingCase.reviewStatus].color}>
                      {REVIEW_STATUS_MAP[viewingCase.reviewStatus].label}
                    </Tag>
                  ) : viewingCase.reviewStatus || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="预估执行时间">
                  {viewingCase.executionTime ? `${viewingCase.executionTime} 分钟` : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="评审人" span={2}>
                  {viewingCase.reviewer || <span className="tc-detail-empty">未指定</span>}
                </Descriptions.Item>
                <Descriptions.Item label="标签" span={2}>
                  {viewingCase.tags ? (
                    viewingCase.tags.split(',').map((tag: string) => (
                      <Tag key={tag.trim()} style={{ marginBottom: 4 }}>{tag.trim()}</Tag>
                    ))
                  ) : (
                    <span className="tc-detail-empty">无标签</span>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="关联需求" span={2}>
                  {viewingCase.requirement ? (
                    <Space>
                      <Tag color="blue" style={{ cursor: 'pointer' }} onClick={() => navigate(`/projects/${projectId}/requirements`)}>
                        <LinkOutlined /> {viewingCase.requirement.requirementNumber}
                      </Tag>
                      <span>{viewingCase.requirement.title}</span>
                    </Space>
                  ) : (
                    <span className="tc-detail-empty">未关联需求</span>
                  )}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <div className="tc-detail-section">
              <div className="tc-detail-section-title">测试内容</div>
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="前置条件">
                  {viewingCase.preconditions || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="测试步骤">
                  {viewingCase.steps || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="测试数据">
                  {viewingCase.testData || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="预期结果">
                  {viewingCase.expectedResults || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="实际结果">
                  {viewingCase.actualResults || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
                <Descriptions.Item label="后置条件">
                  {viewingCase.postconditions || <span className="tc-detail-empty">无</span>}
                </Descriptions.Item>
              </Descriptions>
            </div>

            {viewingCase.automationScript && (
              <div className="tc-detail-section">
                <div className="tc-detail-section-title">自动化信息</div>
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="脚本路径">
                    <code>{viewingCase.automationScript}</code>
                  </Descriptions.Item>
                </Descriptions>
              </div>
            )}

            {(viewingCase.totalExecutions || 0) > 0 && (
              <div className="tc-detail-section">
                <div className="tc-detail-section-title">执行统计</div>
                <Descriptions column={2} size="small" bordered>
                  <Descriptions.Item label="总执行次数">{viewingCase.totalExecutions || 0}</Descriptions.Item>
                  <Descriptions.Item label="通过次数" style={{ color: '#52c41a' }}>
                    {viewingCase.passCount || 0}
                  </Descriptions.Item>
                  <Descriptions.Item label="失败次数" style={{ color: '#ff4d4f' }}>
                    {viewingCase.failCount || 0}
                  </Descriptions.Item>
                  <Descriptions.Item label="通过率">
                    {Math.round((viewingCase.passCount || 0) / Math.max(viewingCase.totalExecutions || 0, 1) * 100)}%
                  </Descriptions.Item>
                  <Descriptions.Item label="最后执行人">{viewingCase.lastExecutedBy || '—'}</Descriptions.Item>
                  <Descriptions.Item label="最后执行时间">
                    {viewingCase.lastExecutedAt ? dayjs(viewingCase.lastExecutedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                  </Descriptions.Item>
                </Descriptions>
                <Progress
                  percent={Math.round((viewingCase.passCount || 0) / Math.max(viewingCase.totalExecutions || 0, 1) * 100)}
                  size="small"
                  status="active"
                  style={{ marginTop: 8 }}
                />
              </div>
            )}

            {viewingCase.reviewComments && (
              <div className="tc-detail-section">
                <div className="tc-detail-section-title">评审意见</div>
                <div className="tc-detail-desc">
                  {viewingCase.reviewComments}
                </div>
              </div>
            )}

            <div className="tc-detail-section">
              <div className="tc-detail-section-title">审计信息</div>
              <Descriptions column={2} size="small" bordered>
                <Descriptions.Item label="创建人">{viewingCase.createdBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {viewingCase.createdAt ? dayjs(viewingCase.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="更新人">{viewingCase.updatedBy || '—'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {viewingCase.updatedAt ? dayjs(viewingCase.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
                </Descriptions.Item>
              </Descriptions>
            </div>

            <Space style={{ marginTop: 8 }}>
              <Button icon={<EditOutlined />} onClick={() => { setDrawerVisible(false); openEdit(viewingCase) }}>
                编辑
              </Button>
            </Space>
          </>
        )}
      </Drawer>
    </div>
  )
}

export default TestCaseList
