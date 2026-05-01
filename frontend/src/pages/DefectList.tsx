import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Table, Button, Modal, Form, Input, Select, Space, Popconfirm, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { getDefects, createDefect, updateDefect, deleteDefect, resolveDefect, closeDefect } from '../api/defect'
import useMessage from '../hooks/useMessage'

const DefectList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingDefect, setEditingDefect] = useState<any>(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const message = useMessage()

  const fetchData = async () => {
    setLoading(true)
    try {
      const response: any = await getDefects(Number(projectId), { page: pagination.current - 1, size: pagination.pageSize })
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0 }))
      }
    } catch (error) {
      message.error('获取缺陷列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [pagination.current])

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
    } catch (error) {
      message.error('操作失败')
    }
  }

  const columns = [
    { title: '缺陷编号', dataIndex: 'defectNumber', key: 'defectNumber', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title' },
    { title: '严重性', dataIndex: 'severity', key: 'severity', render: (s: string) => { const colors: any = { CRITICAL: 'magenta', MAJOR: 'red', MINOR: 'orange', TRIVIAL: 'blue' }; return <Tag color={colors[s]}>{s}</Tag> } },
    { title: '优先级', dataIndex: 'priority', key: 'priority', render: (p: string) => <Tag color={p === 'HIGH' ? 'red' : p === 'MEDIUM' ? 'orange' : 'green'}>{p}</Tag> },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => { const colors: any = { NEW: 'default', OPEN: 'processing', RESOLVED: 'success', CLOSED: 'default' }; return <Tag color={colors[s]}>{s}</Tag> } },
    { title: '指派给', dataIndex: 'assignedTo', key: 'assignedTo' },
    { title: '操作', key: 'action', width: 280, render: (_: any, record: any) => (<Space><Button icon={<EditOutlined />} size="small" onClick={() => { setEditingDefect(record); form.setFieldsValue(record); setModalVisible(true) }}>编辑</Button>{record.status === 'OPEN' && <Button icon={<CheckOutlined />} size="small" onClick={() => resolveDefect(Number(projectId), record.id, { resolution: '已修复', resolvedBy: 'admin' }).then(() => { message.success('已解决'); fetchData() })}>解决</Button>}{record.status === 'RESOLVED' && <Button icon={<CloseOutlined />} size="small" onClick={() => closeDefect(Number(projectId), record.id, { closedBy: 'admin' }).then(() => { message.success('已关闭'); fetchData() })}>关闭</Button>}<Popconfirm title="确定删除？" onConfirm={() => deleteDefect(Number(projectId), record.id).then(() => { message.success('已删除'); fetchData() })}><Button icon={<DeleteOutlined />} size="small" danger>删除</Button></Popconfirm></Space>) },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>缺陷管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingDefect(null); form.resetFields(); setModalVisible(true) }}>提交缺陷</Button>
      </div>
      <Table dataSource={data} columns={columns} loading={loading} rowKey="id" pagination={pagination} scroll={{ x: 1000 }} />
      <Modal title={editingDefect ? '编辑缺陷' : '提交缺陷'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={700} forceRender>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="stepsToReproduce" label="重现步骤"><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="expectedBehavior" label="预期行为"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="actualBehavior" label="实际行为"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="severity" label="严重性"><Select><Select.Option value="CRITICAL">致命</Select.Option><Select.Option value="MAJOR">严重</Select.Option><Select.Option value="MINOR">一般</Select.Option><Select.Option value="TRIVIAL">轻微</Select.Option></Select></Form.Item>
          <Form.Item name="priority" label="优先级"><Select><Select.Option value="HIGH">高</Select.Option><Select.Option value="MEDIUM">中</Select.Option><Select.Option value="LOW">低</Select.Option></Select></Form.Item>
          <Form.Item name="assignedTo" label="指派给"><Input /></Form.Item>
          <Form.Item name="environment" label="环境"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DefectList
