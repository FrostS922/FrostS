import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag,
  Descriptions, Row, Col, Tooltip, Progress, Tabs,
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  PlayCircleOutlined, CheckCircleOutlined,
  FileTextOutlined,
  EnvironmentOutlined, FlagOutlined,
  UserAddOutlined, MinusCircleOutlined,
  SearchOutlined, ThunderboltOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  getTestPlans, createTestPlan, updateTestPlan, deleteTestPlan,
  getTestPlanCases, batchAddTestCases, batchRemoveTestCases,
  batchExecuteTestCases, batchAssignTestCases,
} from '../api/testPlan'
import { getTestCases } from '../api/testCase'
import useMessage from '../hooks/useMessage'

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  ACTIVE: { label: '进行中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'success' },
  BLOCKED: { label: '已阻塞', color: 'error' },
  ARCHIVED: { label: '已归档', color: 'warning' },
}

const CASE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  NOT_RUN: { label: '未执行', color: 'default' },
  PASSED: { label: '通过', color: 'success' },
  FAILED: { label: '失败', color: 'error' },
  BLOCKED: { label: '阻塞', color: 'warning' },
  SKIPPED: { label: '跳过', color: 'default' },
}

const PRIORITY_MAP: Record<string, { label: string; color: string }> = {
  HIGH: { label: '高', color: 'red' },
  MEDIUM: { label: '中', color: 'orange' },
  LOW: { label: '低', color: 'green' },
}

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  FUNCTIONAL: { label: '功能测试', color: 'blue' },
  INTEGRATION: { label: '集成测试', color: 'purple' },
  PERFORMANCE: { label: '性能测试', color: 'orange' },
  SECURITY: { label: '安全测试', color: 'red' },
  USABILITY: { label: '易用性测试', color: 'cyan' },
  COMPATIBILITY: { label: '兼容性测试', color: 'geekblue' },
  REGRESSION: { label: '回归测试', color: 'magenta' },
}

const TestPlanList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const { message, showError } = useMessage()
  const [form] = Form.useForm()
  const [executeForm] = Form.useForm()
  const [assignForm] = Form.useForm()

  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  const [modalVisible, setModalVisible] = useState(false)
  const [editingPlan, setEditingPlan] = useState<any>(null)

  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [viewingPlan, setViewingPlan] = useState<any>(null)
  const [activeTab, setActiveTab] = useState('basic')

  const [planCases, setPlanCases] = useState<any[]>([])
  const [planCasesLoading, setPlanCasesLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [filterStatus, setFilterStatus] = useState<string | undefined>()
  const [filterPriority, setFilterPriority] = useState<string | undefined>()
  const [caseSearchText, setCaseSearchText] = useState('')

  const [addCasesModalVisible, setAddCasesModalVisible] = useState(false)
  const [allTestCases, setAllTestCases] = useState<any[]>([])
  const [addCasesLoading, setAddCasesLoading] = useState(false)
  const [selectedCaseIds, setSelectedCaseIds] = useState<number[]>([])
  const [addCaseSearchText, setAddCaseSearchText] = useState('')
  const [addCaseFilterType, setAddCaseFilterType] = useState<string | undefined>()
  const [addCaseFilterPriority, setAddCaseFilterPriority] = useState<string | undefined>()

  const [executeModalVisible, setExecuteModalVisible] = useState(false)
  const [executingCase, setExecutingCase] = useState<any>(null)
  const [isBatchExecute, setIsBatchExecute] = useState(false)

  const [assignModalVisible, setAssignModalVisible] = useState(false)

  const fetchData = useCallback(async (page?: number) => {
    setLoading(true)
    try {
      const params: any = {
        page: (page ?? pagination.current) - 1,
        size: pagination.pageSize,
      }
      const response: any = await getTestPlans(Number(projectId), params)
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0, current: page ?? prev.current }))
      }
    } catch (err: any) {
      showError(err, '获取测试计划列表失败')
    } finally {
      setLoading(false)
    }
  }, [projectId, pagination.current, pagination.pageSize, message])

  useEffect(() => {
    fetchData()
  }, [pagination.current, pagination.pageSize]) // eslint-disable-line react-hooks/exhaustive-deps

  const stats = useMemo(() => {
    const all = data.length
    const active = data.filter((d) => d.status === 'ACTIVE').length
    const completed = data.filter((d) => d.status === 'COMPLETED').length
    const totalCases = data.reduce((sum, d) => sum + (d.totalCases || 0), 0)
    return { all, active, completed, totalCases }
  }, [data])

  const fetchPlanCases = useCallback(async () => {
    if (!viewingPlan) return
    setPlanCasesLoading(true)
    try {
      const response: any = await getTestPlanCases(Number(projectId), viewingPlan.id)
      if (response.code === 200) {
        setPlanCases(response.data || [])
      }
    } catch (err: any) {
      showError(err, '获取关联用例失败')
    } finally {
      setPlanCasesLoading(false)
    }
  }, [viewingPlan, projectId, message])

  useEffect(() => {
    if (detailModalVisible && viewingPlan && activeTab === 'cases') {
      fetchPlanCases()
    }
  }, [detailModalVisible, viewingPlan, activeTab]) // eslint-disable-line react-hooks/exhaustive-deps

  const filteredPlanCases = useMemo(() => {
    return planCases.filter((pc) => {
      if (filterStatus && pc.status !== filterStatus) return false
      if (filterPriority && pc.priority !== filterPriority) return false
      if (caseSearchText) {
        const tc = pc.testCase
        const text = caseSearchText.toLowerCase()
        if (!tc?.title?.toLowerCase().includes(text) && !tc?.caseNumber?.toLowerCase().includes(text)) return false
      }
      return true
    })
  }, [planCases, filterStatus, filterPriority, caseSearchText])

  const planCaseStats = useMemo(() => {
    const total = planCases.length
    const passed = planCases.filter((c) => c.status === 'PASSED').length
    const failed = planCases.filter((c) => c.status === 'FAILED').length
    const blocked = planCases.filter((c) => c.status === 'BLOCKED').length
    const notRun = planCases.filter((c) => !c.status || c.status === 'NOT_RUN').length
    return { total, passed, failed, blocked, notRun }
  }, [planCases])

  const handleTableChange = (pag: any) => {
    setPagination((prev) => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))
  }

  const openCreate = () => {
    setEditingPlan(null)
    form.resetFields()
    setModalVisible(true)
  }

  const openEdit = (record: any) => {
    setEditingPlan(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const openDetail = (record: any) => {
    setViewingPlan(record)
    setActiveTab('basic')
    setSelectedRowKeys([])
    setFilterStatus(undefined)
    setFilterPriority(undefined)
    setCaseSearchText('')
    setDetailModalVisible(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      if (editingPlan) {
        await updateTestPlan(Number(projectId), editingPlan.id, values)
        message.success('更新成功')
      } else {
        await createTestPlan(Number(projectId), values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (err: any) {
      showError(err, editingPlan ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteTestPlan(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch (err: any) {
      showError(err, '删除失败')
    }
  }

  const openAddCasesModal = async () => {
    setAddCasesModalVisible(true)
    setAddCasesLoading(true)
    setSelectedCaseIds([])
    setAddCaseSearchText('')
    setAddCaseFilterType(undefined)
    setAddCaseFilterPriority(undefined)
    try {
      const response: any = await getTestCases(Number(projectId), { page: 0, size: 500 })
      if (response.code === 200) {
        setAllTestCases(response.data || [])
      }
    } catch (err: any) {
      showError(err, '获取用例列表失败')
    } finally {
      setAddCasesLoading(false)
    }
  }

  const handleAddCases = async () => {
    if (selectedCaseIds.length === 0) {
      message.warning('请选择要添加的用例')
      return
    }
    try {
      await batchAddTestCases(Number(projectId), viewingPlan.id, { testCaseIds: selectedCaseIds })
      message.success(`成功添加 ${selectedCaseIds.length} 个用例`)
      setAddCasesModalVisible(false)
      fetchPlanCases()
      fetchData()
    } catch (err: any) {
      showError(err, '添加用例失败')
    }
  }

  const openExecuteModal = (planCase: any) => {
    setExecutingCase(planCase)
    setIsBatchExecute(false)
    executeForm.resetFields()
    executeForm.setFieldsValue({ status: 'PASSED', executedBy: '' })
    setExecuteModalVisible(true)
  }

  const openBatchExecuteModal = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要执行的用例')
      return
    }
    setExecutingCase(null)
    setIsBatchExecute(true)
    executeForm.resetFields()
    executeForm.setFieldsValue({ status: 'PASSED', executedBy: '' })
    setExecuteModalVisible(true)
  }

  const handleExecute = async () => {
    const values = await executeForm.validateFields()
    try {
      if (isBatchExecute) {
        await batchExecuteTestCases(Number(projectId), {
          planCaseIds: selectedRowKeys as number[],
          status: values.status,
          executedBy: values.executedBy,
        })
        message.success(`批量执行 ${selectedRowKeys.length} 个用例成功`)
      } else {
        await batchExecuteTestCases(Number(projectId), {
          planCaseIds: [executingCase.id],
          status: values.status,
          executedBy: values.executedBy,
        })
        message.success('执行成功')
      }
      setExecuteModalVisible(false)
      fetchPlanCases()
      fetchData()
      setSelectedRowKeys([])
    } catch (err: any) {
      showError(err, '执行失败')
    }
  }

  const openBatchAssignModal = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要分配的用例')
      return
    }
    assignForm.resetFields()
    setAssignModalVisible(true)
  }

  const handleBatchAssign = async () => {
    const values = await assignForm.validateFields()
    try {
      await batchAssignTestCases(Number(projectId), viewingPlan.id, {
        planCaseIds: selectedRowKeys as number[],
        assignedTo: values.assignedTo,
      })
      message.success(`成功分配 ${selectedRowKeys.length} 个用例`)
      setAssignModalVisible(false)
      fetchPlanCases()
      setSelectedRowKeys([])
    } catch (err: any) {
      showError(err, '分配失败')
    }
  }

  const handleBatchRemove = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要移除的用例')
      return
    }
    Modal.confirm({
      title: '确认移除',
      content: `确定要从计划中移除 ${selectedRowKeys.length} 个用例？`,
      onOk: async () => {
        try {
          await batchRemoveTestCases(Number(projectId), viewingPlan.id, {
            planCaseIds: selectedRowKeys as number[],
          })
          message.success('移除成功')
          fetchPlanCases()
          fetchData()
          setSelectedRowKeys([])
        } catch (err: any) {
          showError(err, '移除失败')
        }
      },
    })
  }

  const existingCaseIds = useMemo(() => {
    return new Set(planCases.map((pc) => pc.testCase?.id).filter(Boolean))
  }, [planCases])

  const filteredAllTestCases = useMemo(() => {
    return allTestCases.filter((tc) => {
      if (addCaseFilterType && tc.type !== addCaseFilterType) return false
      if (addCaseFilterPriority && tc.priority !== addCaseFilterPriority) return false
      if (addCaseSearchText) {
        const text = addCaseSearchText.toLowerCase()
        if (!tc.caseNumber?.toLowerCase().includes(text) && !tc.title?.toLowerCase().includes(text)) return false
      }
      return true
    })
  }, [allTestCases, addCaseSearchText, addCaseFilterType, addCaseFilterPriority])

  const columns = [
    {
      title: '计划编号',
      dataIndex: 'planNumber',
      key: 'planNumber',
      width: 160,
      render: (text: string, record: any) => (
        <span className="plan-number-link" onClick={() => openDetail(record)}>
          {text || '—'}
        </span>
      ),
    },
    {
      title: '计划名称',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true,
    },
    {
      title: '负责人',
      dataIndex: 'owner',
      key: 'owner',
      width: 100,
      render: (text: string) => text || '—',
    },
    {
      title: '环境',
      dataIndex: 'environment',
      key: 'environment',
      width: 120,
      render: (text: string) => text || '—',
    },
    {
      title: '进度',
      key: 'progress',
      width: 140,
      render: (_: any, record: any) => {
        const progress = record.progress ? Number(record.progress) : 0
        const total = record.totalCases || 0
        return (
          <Tooltip title={`${total} 个用例`}>
            <Progress percent={progress} size="small" />
          </Tooltip>
        )
      },
    },
    {
      title: '执行统计',
      key: 'executionStats',
      width: 180,
      render: (_: any, record: any) => {
        const total = record.totalCases || 0
        if (total === 0) return <span style={{ color: 'var(--text-muted)' }}>未添加用例</span>
        const passed = record.passedCases || 0
        const failed = record.failedCases || 0
        const blocked = record.blockedCases || 0
        const notRun = record.notRunCases || 0
        return (
          <Space size={4}>
            <Tooltip title={`通过: ${passed}`}>
              <Tag color="success">{passed}</Tag>
            </Tooltip>
            <Tooltip title={`失败: ${failed}`}>
              <Tag color="error">{failed}</Tag>
            </Tooltip>
            <Tooltip title={`阻塞: ${blocked}`}>
              <Tag color="warning">{blocked}</Tag>
            </Tooltip>
            <Tooltip title={`未执行: ${notRun}`}>
              <Tag>{notRun}</Tag>
            </Tooltip>
          </Space>
        )
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
      title: '起止日期',
      key: 'dates',
      width: 180,
      render: (_: any, record: any) => (
        <span style={{ fontSize: 12 }}>
          {record.startDate ? dayjs(record.startDate).format('MM-DD') : '—'}
          {' ~ '}
          {record.endDate ? dayjs(record.endDate).format('MM-DD') : '—'}
        </span>
      ),
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
          <Popconfirm title="确定删除此计划？" onConfirm={() => handleDelete(record.id)}>
            <Tooltip title="删除">
              <Button type="text" size="small" icon={<DeleteOutlined />} danger />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const planCaseColumns = [
    {
      title: '用例编号',
      key: 'caseNumber',
      width: 140,
      render: (_: any, record: any) => record.testCase?.caseNumber || '—',
    },
    {
      title: '标题',
      key: 'title',
      ellipsis: true,
      render: (_: any, record: any) => record.testCase?.title || '—',
    },
    {
      title: '类型',
      key: 'type',
      width: 100,
      render: (_: any, record: any) => {
        const t = record.testCase?.type
        const info = TYPE_MAP[t]
        return info ? <Tag color={info.color}>{info.label}</Tag> : t ? <Tag>{t}</Tag> : '—'
      },
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority: string, record: any) => {
        const info = PRIORITY_MAP[priority] || PRIORITY_MAP[record?.testCase?.priority]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{priority || '—'}</Tag>
      },
    },
    {
      title: '执行状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const info = CASE_STATUS_MAP[status]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{status || '未执行'}</Tag>
      },
    },
    {
      title: '执行人',
      dataIndex: 'assignedTo',
      key: 'assignedTo',
      width: 100,
      render: (text: string) => text || '—',
    },
    {
      title: '执行时间',
      dataIndex: 'executedAt',
      key: 'executedAt',
      width: 160,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '—',
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right' as const,
      render: (_: any, record: any) => (
        <Tooltip title="执行">
          <Button type="text" size="small" icon={<PlayCircleOutlined />} onClick={() => openExecuteModal(record)} />
        </Tooltip>
      ),
    },
  ]

  return (
    <div>
      <div className="tp-page-header">
        <h2>测试计划管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建计划
        </Button>
      </div>

      <div className="tp-stats-row">
        <div className="tp-stat-card">
          <div className="tp-stat-icon" style={{ background: 'rgba(45,212,191,0.14)', color: 'var(--accent)' }}>
            <FileTextOutlined />
          </div>
          <div className="tp-stat-info">
            <span className="tp-stat-label">总计划</span>
            <span className="tp-stat-value">{stats.all}</span>
          </div>
        </div>
        <div className="tp-stat-card">
          <div className="tp-stat-icon" style={{ background: 'rgba(122,162,255,0.14)', color: 'var(--accent-secondary)' }}>
            <PlayCircleOutlined />
          </div>
          <div className="tp-stat-info">
            <span className="tp-stat-label">进行中</span>
            <span className="tp-stat-value">{stats.active}</span>
          </div>
        </div>
        <div className="tp-stat-card">
          <div className="tp-stat-icon" style={{ background: 'rgba(82,196,26,0.14)', color: '#52c41a' }}>
            <CheckCircleOutlined />
          </div>
          <div className="tp-stat-info">
            <span className="tp-stat-label">已完成</span>
            <span className="tp-stat-value">{stats.completed}</span>
          </div>
        </div>
        <div className="tp-stat-card">
          <div className="tp-stat-icon" style={{ background: 'rgba(250,173,20,0.14)', color: '#faad14' }}>
            <FlagOutlined />
          </div>
          <div className="tp-stat-info">
            <span className="tp-stat-label">总用例</span>
            <span className="tp-stat-value">{stats.totalCases}</span>
          </div>
        </div>
      </div>

      <Table
        dataSource={data}
        columns={columns}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          pageSizeOptions: ['10', '20', '50'],
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={editingPlan ? '编辑计划' : '新建计划'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={720}
        forceRender
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="计划名称" rules={[{ required: true, message: '请输入计划名称' }]}>
            <Input placeholder="请输入计划名称" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="owner" label="负责人">
                <Input placeholder="请输入负责人" />
              </Form.Item>
            </Col>
            <Col span={12}>
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
              <Form.Item name="startDate" label="计划开始日期">
                <Input type="date" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="endDate" label="计划结束日期">
                <Input type="date" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="actualStartDate" label="实际开始日期">
                <Input type="date" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="actualEndDate" label="实际结束日期">
                <Input type="date" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="environment" label="测试环境">
                <Input placeholder="例如: 测试环境A, 预发布环境" prefix={<EnvironmentOutlined />} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="milestone" label="关联里程碑">
                <Input placeholder="请输入里程碑" prefix={<FlagOutlined />} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="请输入计划描述..." />
          </Form.Item>
          <Form.Item name="scope" label="测试范围">
            <Input.TextArea rows={2} placeholder="请输入测试范围..." />
          </Form.Item>
          <Form.Item name="testStrategy" label="测试策略">
            <Input.TextArea rows={2} placeholder="请输入测试策略..." />
          </Form.Item>
          <Form.Item name="entryCriteria" label="准入标准">
            <Input.TextArea rows={2} placeholder="请输入准入标准..." />
          </Form.Item>
          <Form.Item name="exitCriteria" label="准出标准">
            <Input.TextArea rows={2} placeholder="请输入准出标准..." />
          </Form.Item>
          <Form.Item name="risk" label="风险评估">
            <Input.TextArea rows={2} placeholder="请输入风险评估..." />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          <Space>
            <span>{viewingPlan?.planNumber || '计划详情'}</span>
            {viewingPlan && STATUS_MAP[viewingPlan.status] && (
              <Tag color={STATUS_MAP[viewingPlan.status].color}>{STATUS_MAP[viewingPlan.status].label}</Tag>
            )}
          </Space>
        }
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        width="100vw"
        style={{ top: 0, paddingBottom: 0, maxWidth: '100vw' }}
        styles={{ body: { height: 'calc(100vh - 110px)', overflow: 'auto', padding: '0 24px 24px' } }}
        footer={null}
        destroyOnHidden
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ marginTop: 8 }}
          items={[
            {
              key: 'basic',
              label: '基本信息',
              children: viewingPlan && (
                <>
                  <div className="tp-detail-section">
                    <div className="tp-detail-section-title">基本信息</div>
                    <Descriptions column={2} size="small" bordered>
                      <Descriptions.Item label="名称" span={2}>{viewingPlan.name}</Descriptions.Item>
                      <Descriptions.Item label="编号">{viewingPlan.planNumber || '—'}</Descriptions.Item>
                      <Descriptions.Item label="负责人">{viewingPlan.owner || '—'}</Descriptions.Item>
                      <Descriptions.Item label="状态">
                        {STATUS_MAP[viewingPlan.status] ? (
                          <Tag color={STATUS_MAP[viewingPlan.status].color}>{STATUS_MAP[viewingPlan.status].label}</Tag>
                        ) : viewingPlan.status || '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="环境">{viewingPlan.environment || '—'}</Descriptions.Item>
                      <Descriptions.Item label="里程碑">{viewingPlan.milestone || '—'}</Descriptions.Item>
                      <Descriptions.Item label="计划开始">
                        {viewingPlan.startDate ? dayjs(viewingPlan.startDate).format('YYYY-MM-DD') : '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="计划结束">
                        {viewingPlan.endDate ? dayjs(viewingPlan.endDate).format('YYYY-MM-DD') : '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="实际开始">
                        {viewingPlan.actualStartDate ? dayjs(viewingPlan.actualStartDate).format('YYYY-MM-DD') : '—'}
                      </Descriptions.Item>
                      <Descriptions.Item label="实际结束" span={2}>
                        {viewingPlan.actualEndDate ? dayjs(viewingPlan.actualEndDate).format('YYYY-MM-DD') : '—'}
                      </Descriptions.Item>
                    </Descriptions>
                  </div>

                  {(viewingPlan.totalCases || 0) > 0 && (
                    <div className="tp-detail-section">
                      <div className="tp-detail-section-title">执行概况</div>
                      <Progress
                        percent={viewingPlan.progress ? Number(viewingPlan.progress) : 0}
                        status="active"
                        format={(percent) => `${percent}% 完成`}
                      />
                      <Descriptions column={2} size="small" bordered style={{ marginTop: 12 }}>
                        <Descriptions.Item label="总用例">{viewingPlan.totalCases || 0}</Descriptions.Item>
                        <Descriptions.Item label="通过" style={{ color: '#52c41a' }}>{viewingPlan.passedCases || 0}</Descriptions.Item>
                        <Descriptions.Item label="失败" style={{ color: '#ff4d4f' }}>{viewingPlan.failedCases || 0}</Descriptions.Item>
                        <Descriptions.Item label="阻塞" style={{ color: '#faad14' }}>{viewingPlan.blockedCases || 0}</Descriptions.Item>
                        <Descriptions.Item label="未执行" span={2}>{viewingPlan.notRunCases || 0}</Descriptions.Item>
                      </Descriptions>
                    </div>
                  )}

                  <div className="tp-detail-section">
                    <div className="tp-detail-section-title">测试内容</div>
                    <Descriptions column={1} size="small" bordered>
                      <Descriptions.Item label="描述">{viewingPlan.description || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                      <Descriptions.Item label="测试范围">{viewingPlan.scope || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                      <Descriptions.Item label="测试策略">{viewingPlan.testStrategy || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                    </Descriptions>
                  </div>

                  <div className="tp-detail-section">
                    <div className="tp-detail-section-title">质量标准</div>
                    <Descriptions column={1} size="small" bordered>
                      <Descriptions.Item label="准入标准">{viewingPlan.entryCriteria || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                      <Descriptions.Item label="准出标准">{viewingPlan.exitCriteria || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                      <Descriptions.Item label="风险评估">{viewingPlan.risk || <span className="tp-detail-empty">无</span>}</Descriptions.Item>
                    </Descriptions>
                  </div>

                  <div className="tp-detail-section">
                    <div className="tp-detail-section-title">审计信息</div>
                    <Descriptions column={2} size="small" bordered>
                      <Descriptions.Item label="创建人">{viewingPlan.createdBy || '—'}</Descriptions.Item>
                      <Descriptions.Item label="创建时间">{viewingPlan.createdAt ? dayjs(viewingPlan.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}</Descriptions.Item>
                      <Descriptions.Item label="更新人">{viewingPlan.updatedBy || '—'}</Descriptions.Item>
                      <Descriptions.Item label="更新时间">{viewingPlan.updatedAt ? dayjs(viewingPlan.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '—'}</Descriptions.Item>
                    </Descriptions>
                  </div>

                  <Space style={{ marginTop: 8 }}>
                    <Button icon={<EditOutlined />} onClick={() => { setDetailModalVisible(false); openEdit(viewingPlan) }}>编辑</Button>
                  </Space>
                </>
              ),
            },
            {
              key: 'cases',
              label: '关联用例',
              children: (
                <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
                    <Space wrap>
                      <Button type="primary" icon={<PlusOutlined />} onClick={openAddCasesModal}>添加用例</Button>
                      <Button icon={<ThunderboltOutlined />} onClick={openBatchExecuteModal} disabled={selectedRowKeys.length === 0}>
                        批量执行
                      </Button>
                      <Button icon={<UserAddOutlined />} onClick={openBatchAssignModal} disabled={selectedRowKeys.length === 0}>
                        分配执行人
                      </Button>
                      <Button icon={<MinusCircleOutlined />} danger onClick={handleBatchRemove} disabled={selectedRowKeys.length === 0}>
                        批量移除
                      </Button>
                    </Space>
                    <Space wrap>
                      <Select
                        placeholder="执行状态"
                        allowClear
                        style={{ width: 120 }}
                        value={filterStatus}
                        onChange={setFilterStatus}
                      >
                        {Object.entries(CASE_STATUS_MAP).map(([k, v]) => (
                          <Select.Option key={k} value={k}>{v.label}</Select.Option>
                        ))}
                      </Select>
                      <Select
                        placeholder="优先级"
                        allowClear
                        style={{ width: 100 }}
                        value={filterPriority}
                        onChange={setFilterPriority}
                      >
                        {Object.entries(PRIORITY_MAP).map(([k, v]) => (
                          <Select.Option key={k} value={k}>{v.label}</Select.Option>
                        ))}
                      </Select>
                      <Input
                        placeholder="搜索用例"
                        prefix={<SearchOutlined />}
                        style={{ width: 180 }}
                        value={caseSearchText}
                        onChange={(e) => setCaseSearchText(e.target.value)}
                        allowClear
                      />
                    </Space>
                  </div>

                  <Table
                    dataSource={filteredPlanCases}
                    columns={planCaseColumns}
                    loading={planCasesLoading}
                    rowKey="id"
                    scroll={{ x: 900 }}
                    size="small"
                    rowSelection={{
                      selectedRowKeys,
                      onChange: setSelectedRowKeys,
                    }}
                    pagination={false}
                  />

                  <div style={{
                    marginTop: 12, padding: '8px 16px', background: 'var(--bg-secondary, #fafafa)',
                    borderRadius: 6, display: 'flex', gap: 24, fontSize: 13,
                  }}>
                    <span>总计 <strong>{planCaseStats.total}</strong></span>
                    <span style={{ color: '#52c41a' }}>通过 <strong>{planCaseStats.passed}</strong></span>
                    <span style={{ color: '#ff4d4f' }}>失败 <strong>{planCaseStats.failed}</strong></span>
                    <span style={{ color: '#faad14' }}>阻塞 <strong>{planCaseStats.blocked}</strong></span>
                    <span>未执行 <strong>{planCaseStats.notRun}</strong></span>
                  </div>
                </>
              ),
            },
          ]}
        />
      </Modal>

      <Modal
        title="添加用例"
        open={addCasesModalVisible}
        onOk={handleAddCases}
        onCancel={() => setAddCasesModalVisible(false)}
        width={800}
        destroyOnHidden
      >
        <div style={{ marginBottom: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <Input
            placeholder="搜索用例编号或标题"
            prefix={<SearchOutlined />}
            allowClear
            style={{ width: 240 }}
            value={addCaseSearchText}
            onChange={(e) => setAddCaseSearchText(e.target.value)}
          />
          <Select
            placeholder="类型"
            allowClear
            style={{ width: 130 }}
            value={addCaseFilterType}
            onChange={setAddCaseFilterType}
          >
            {Object.entries(TYPE_MAP).map(([k, v]) => (
              <Select.Option key={k} value={k}>{v.label}</Select.Option>
            ))}
          </Select>
          <Select
            placeholder="优先级"
            allowClear
            style={{ width: 110 }}
            value={addCaseFilterPriority}
            onChange={setAddCaseFilterPriority}
          >
            {Object.entries(PRIORITY_MAP).map(([k, v]) => (
              <Select.Option key={k} value={k}>{v.label}</Select.Option>
            ))}
          </Select>
        </div>
        <Table
          dataSource={filteredAllTestCases}
          loading={addCasesLoading}
          rowKey="id"
          size="small"
          scroll={{ y: 400 }}
          rowSelection={{
            selectedRowKeys: selectedCaseIds,
            onChange: (keys) => setSelectedCaseIds(keys as number[]),
            getCheckboxProps: (record: any) => ({
              disabled: existingCaseIds.has(record.id),
              name: record.id.toString(),
            }),
          }}
          columns={[
            { title: '编号', dataIndex: 'caseNumber', width: 140, render: (t: string) => t || '—' },
            { title: '标题', dataIndex: 'title', ellipsis: true },
            {
              title: '类型', dataIndex: 'type', width: 100,
              render: (t: string) => {
                const info = TYPE_MAP[t]
                return info ? <Tag color={info.color}>{info.label}</Tag> : t ? <Tag>{t}</Tag> : '—'
              },
            },
            {
              title: '优先级', dataIndex: 'priority', width: 80,
              render: (p: string) => {
                const info = PRIORITY_MAP[p]
                return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{p}</Tag>
              },
            },
            {
              title: '状态', width: 80,
              render: (_: any, record: any) =>
                existingCaseIds.has(record.id) ? <Tag color="blue">已添加</Tag> : null,
            },
          ]}
          pagination={false}
        />
        {selectedCaseIds.length > 0 && (
          <div style={{ marginTop: 8, color: 'var(--text-secondary)' }}>
            已选择 {selectedCaseIds.length} 个用例
          </div>
        )}
      </Modal>

      <Modal
        title={isBatchExecute ? '批量执行用例' : '执行用例'}
        open={executeModalVisible}
        onOk={handleExecute}
        onCancel={() => setExecuteModalVisible(false)}
        width={560}
        destroyOnHidden
      >
        <Form form={executeForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="status" label="执行状态" rules={[{ required: true, message: '请选择执行状态' }]}>
            <Select placeholder="请选择执行状态">
              {Object.entries(CASE_STATUS_MAP).map(([k, v]) => (
                <Select.Option key={k} value={k}>{v.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="executedBy" label="执行人" rules={[{ required: true, message: '请输入执行人' }]}>
            <Input placeholder="请输入执行人" />
          </Form.Item>
          {!isBatchExecute && (
            <Form.Item name="actualResult" label="实际结果">
              <Input.TextArea rows={2} placeholder="请输入实际结果..." />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="分配执行人"
        open={assignModalVisible}
        onOk={handleBatchAssign}
        onCancel={() => setAssignModalVisible(false)}
        width={400}
        destroyOnHidden
      >
        <Form form={assignForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="assignedTo" label="执行人" rules={[{ required: true, message: '请输入执行人' }]}>
            <Input placeholder="请输入执行人" prefix={<UserAddOutlined />} />
          </Form.Item>
        </Form>
        <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
          将为已选的 {selectedRowKeys.length} 个用例分配执行人
        </div>
      </Modal>
    </div>
  )
}

export default TestPlanList
