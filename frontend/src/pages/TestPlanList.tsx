import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { getTestPlans, createTestPlan, updateTestPlan, deleteTestPlan } from '../api/testPlan'
import useMessage from '../hooks/useMessage'

const TestPlanList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingPlan, setEditingPlan] = useState<any>(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const message = useMessage()

  const fetchData = async () => {
    setLoading(true)
    try {
      const response: any = await getTestPlans(Number(projectId), { page: pagination.current - 1, size: pagination.pageSize })
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0 }))
      }
    } catch (error) {
      message.error('获取测试计划失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [pagination.current])

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
    } catch (error) {
      message.error('操作失败')
    }
  }

  const columns = [
    { title: '计划名称', dataIndex: 'name', key: 'name' },
    { title: '负责人', dataIndex: 'owner', key: 'owner' },
    { title: '开始日期', dataIndex: 'startDate', key: 'startDate' },
    { title: '结束日期', dataIndex: 'endDate', key: 'endDate' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => { const colors: any = { DRAFT: 'default', ACTIVE: 'processing', COMPLETED: 'success', BLOCKED: 'error' }; return <Tag color={colors[s]}>{s}</Tag> } },
    { title: '操作', key: 'action', render: (_: any, record: any) => (<Space><Button icon={<EditOutlined />} size="small" onClick={() => { setEditingPlan(record); form.setFieldsValue(record); setModalVisible(true) }}>编辑</Button><Button icon={<PlayCircleOutlined />} size="small" type="primary">执行</Button><Popconfirm title="确定删除？" onConfirm={() => deleteTestPlan(Number(projectId), record.id).then(() => { message.success('已删除'); fetchData() })}><Button icon={<DeleteOutlined />} size="small" danger>删除</Button></Popconfirm></Space>) },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>测试计划管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingPlan(null); form.resetFields(); setModalVisible(true) }}>新建计划</Button>
      </div>
      <Table dataSource={data} columns={columns} loading={loading} rowKey="id" pagination={pagination} />
      <Modal title={editingPlan ? '编辑计划' : '新建计划'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={600} forceRender>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="计划名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="owner" label="负责人"><Input /></Form.Item>
          <Form.Item name="status" label="状态"><Select><Select.Option value="DRAFT">草稿</Select.Option><Select.Option value="ACTIVE">进行中</Select.Option><Select.Option value="COMPLETED">已完成</Select.Option><Select.Option value="BLOCKED">已阻塞</Select.Option></Select></Form.Item>
          <Form.Item name="scope" label="测试范围"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="testStrategy" label="测试策略"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TestPlanList
