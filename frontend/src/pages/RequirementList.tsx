import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Table, Button, Modal, Form, Input, Select, Space, message, Popconfirm, Tag } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getRequirements, createRequirement, updateRequirement, deleteRequirement } from '../api/requirement'

const RequirementList: React.FC = () => {
  const { id: projectId } = useParams<{ id: string }>()
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRequirement, setEditingRequirement] = useState<any>(null)
  const [form] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  const fetchData = async () => {
    setLoading(true)
    try {
      const response: any = await getRequirements(Number(projectId), {
        page: pagination.current - 1,
        size: pagination.pageSize,
      })
      if (response.code === 200) {
        setData(response.data || [])
        setPagination((prev) => ({ ...prev, total: response.total || 0 }))
      }
    } catch (error) {
      message.error('获取需求列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [pagination.current])

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      if (editingRequirement) {
        await updateRequirement(Number(projectId), editingRequirement.id, values)
        message.success('更新成功')
      } else {
        await createRequirement(Number(projectId), values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (error) {
      message.error(editingRequirement ? '更新失败' : '创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteRequirement(Number(projectId), id)
      message.success('删除成功')
      fetchData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const columns = [
    { title: '需求编号', dataIndex: 'requirementNumber', key: 'requirementNumber' },
    { title: '标题', dataIndex: 'title', key: 'title' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: string) => {
        const colors: any = { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }
        return <Tag color={colors[priority]}>{priority}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colors: any = { DRAFT: 'default', REVIEWING: 'processing', APPROVED: 'success', REJECTED: 'error' }
        return <Tag color={colors[status]}>{status}</Tag>
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <Button icon={<EditOutlined />} size="small" onClick={() => { setEditingRequirement(record); form.setFieldsValue(record); setModalVisible(true) }}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button icon={<DeleteOutlined />} size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>需求管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingRequirement(null); form.resetFields(); setModalVisible(true) }}>
          新建需求
        </Button>
      </div>
      <Table dataSource={data} columns={columns} loading={loading} rowKey="id" pagination={pagination} />
      <Modal title={editingRequirement ? '编辑需求' : '新建需求'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="type" label="类型"><Select><Select.Option value="FUNCTIONAL">功能需求</Select.Option><Select.Option value="NON_FUNCTIONAL">非功能需求</Select.Option><Select.Option value="INTERFACE">接口需求</Select.Option></Select></Form.Item>
          <Form.Item name="priority" label="优先级"><Select><Select.Option value="HIGH">高</Select.Option><Select.Option value="MEDIUM">中</Select.Option><Select.Option value="LOW">低</Select.Option></Select></Form.Item>
          <Form.Item name="status" label="状态"><Select><Select.Option value="DRAFT">草稿</Select.Option><Select.Option value="REVIEWING">评审中</Select.Option><Select.Option value="APPROVED">已批准</Select.Option><Select.Option value="REJECTED">已拒绝</Select.Option></Select></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default RequirementList
