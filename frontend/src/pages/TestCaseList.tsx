import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getTestCases, createTestCase, updateTestCase, deleteTestCase } from '../api/testCase'
import useMessage from '../hooks/useMessage'

const TestCaseList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingCase, setEditingCase] = useState<any>(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const message = useMessage()

  const fetchData = async () => {
    setLoading(true)
    try {
      const response: any = await getTestCases(Number(projectId), { page: pagination.current - 1, size: pagination.pageSize })
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0 }))
      }
    } catch (error) {
      message.error('获取测试用例失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [pagination.current])

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      if (editingCase) {
        await updateTestCase(Number(projectId), editingCase.id, values)
        message.success('更新成功')
      } else {
        await createTestCase(Number(projectId), values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (error) {
      message.error('操作失败')
    }
  }

  const columns = [
    { title: '用例编号', dataIndex: 'caseNumber', key: 'caseNumber', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title' },
    { title: '类型', dataIndex: 'type', key: 'type', render: (t: string) => <Tag color="blue">{t}</Tag> },
    { title: '优先级', dataIndex: 'priority', key: 'priority', render: (p: string) => <Tag color={p === 'HIGH' ? 'red' : p === 'MEDIUM' ? 'orange' : 'green'}>{p}</Tag> },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s}</Tag> },
    { title: '操作', key: 'action', render: (_: any, record: any) => (<Space><Button icon={<EditOutlined />} size="small" onClick={() => { setEditingCase(record); form.setFieldsValue(record); setModalVisible(true) }}>编辑</Button><Popconfirm title="确定删除？" onConfirm={() => deleteTestCase(Number(projectId), record.id).then(() => { message.success('已删除'); fetchData() })}><Button icon={<DeleteOutlined />} size="small" danger>删除</Button></Popconfirm></Space>) },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>测试用例管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingCase(null); form.resetFields(); setModalVisible(true) }}>新建用例</Button>
      </div>
      <Table dataSource={data} columns={columns} loading={loading} rowKey="id" pagination={pagination} scroll={{ x: 800 }} />
      <Modal title={editingCase ? '编辑用例' : '新建用例'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={700} forceRender>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="preconditions" label="前置条件"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="steps" label="测试步骤"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="expectedResults" label="预期结果"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="type" label="类型"><Select><Select.Option value="FUNCTIONAL">功能测试</Select.Option><Select.Option value="INTEGRATION">集成测试</Select.Option><Select.Option value="PERFORMANCE">性能测试</Select.Option><Select.Option value="SECURITY">安全测试</Select.Option></Select></Form.Item>
          <Form.Item name="priority" label="优先级"><Select><Select.Option value="HIGH">高</Select.Option><Select.Option value="MEDIUM">中</Select.Option><Select.Option value="LOW">低</Select.Option></Select></Form.Item>
          <Form.Item name="status" label="状态"><Select><Select.Option value="DRAFT">草稿</Select.Option><Select.Option value="ACTIVE">激活</Select.Option><Select.Option value="OBSOLETE">废弃</Select.Option></Select></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TestCaseList
