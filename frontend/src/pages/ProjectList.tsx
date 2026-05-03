import React, { useRef, useState } from 'react'
import { Button, Modal, Form, Input, Select, Space, Popconfirm, Tag, Progress } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { ProTable, type ProColumns, type ActionType } from '@ant-design/pro-components'
import { getProjects, createProject, updateProject, deleteProject } from '../api/project'
import useMessage from '../hooks/useMessage'

const CATEGORY_MAP: Record<string, { label: string; color: string }> = {
  PRODUCT: { label: '产品研发', color: 'blue' },
  PROJECT: { label: '项目交付', color: 'purple' },
  OPERATION: { label: '运维支持', color: 'cyan' },
  RESEARCH: { label: '技术研究', color: 'orange' },
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PLANNING: { label: '规划中', color: 'default' },
  IN_PROGRESS: { label: '进行中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'success' },
  ON_HOLD: { label: '已暂停', color: 'warning' },
}

const HEALTH_MAP: Record<string, { label: string; color: string }> = {
  NORMAL: { label: '正常', color: 'success' },
  WARNING: { label: '警告', color: 'warning' },
  DANGER: { label: '危险', color: 'error' },
}

const ProjectList: React.FC = () => {
  const [modalVisible, setModalVisible] = useState(false)
  const [editingProject, setEditingProject] = useState<any>(null)
  const [form] = Form.useForm()
  const actionRef = useRef<ActionType>()
  const { message } = useMessage()

  const handleCreate = () => {
    setEditingProject(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: any) => {
    setEditingProject(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteProject(id)
      message.success('删除成功')
      actionRef.current?.reload()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    try {
      if (editingProject) {
        await updateProject(editingProject.id, values)
        message.success('更新成功')
      } else {
        await createProject(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      actionRef.current?.reload()
    } catch (error) {
      message.error(editingProject ? '更新失败' : '创建失败')
    }
  }

  const columns: ProColumns<any>[] = [
    {
      title: '项目名称',
      dataIndex: 'name',
      ellipsis: true,
      fieldProps: {
        placeholder: '请输入项目名称',
      },
    },
    {
      title: '项目编码',
      dataIndex: 'code',
      ellipsis: true,
    },
    {
      title: '分类',
      dataIndex: 'category',
      width: 110,
      render: (_: any, record: any) => {
        const info = CATEGORY_MAP[record.category]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{record.category}</Tag>
      },
    },
    {
      title: '负责人',
      dataIndex: 'manager',
      ellipsis: true,
      width: 100,
    },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 120,
      render: (_: any, record: any) => (
        <Progress percent={record.progress ? Number(record.progress) : 0} size="small" />
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (_: any, record: any) => {
        const info = STATUS_MAP[record.status]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{record.status}</Tag>
      },
    },
    {
      title: '健康度',
      dataIndex: 'health',
      width: 90,
      render: (_: any, record: any) => {
        const info = HEALTH_MAP[record.health]
        return info ? <Tag color={info.color}>{info.label}</Tag> : <Tag>{record.health}</Tag>
      },
    },
    {
      title: '预估工时',
      dataIndex: 'estimatedHours',
      width: 90,
      render: (_: any, record: any) => record.estimatedHours ?? '—',
    },
    {
      title: '实际工时',
      dataIndex: 'actualHours',
      width: 90,
      render: (_: any, record: any) => record.actualHours ?? '—',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除此项目吗？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <ProTable
        columns={columns}
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 'auto',
        }}
        headerTitle="项目管理"
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建项目
          </Button>,
        ]}
        request={async (params) => {
          const response: any = await getProjects({
            page: (params.current || 1) - 1,
            size: params.pageSize || 10,
            search: params.name,
          })
          if (response.code === 200) {
            return {
              data: response.data || [],
              success: true,
              total: response.total || 0,
            }
          }
          return {
            data: [],
            success: false,
            total: 0,
          }
        }}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
        }}
      />
      <Modal
        title={editingProject ? '编辑项目' : '新建项目'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        forceRender
        width={640}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="项目名称" rules={[{ required: true, message: '请输入项目名称' }]}>
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          <Form.Item name="code" label="项目编码" rules={[{ required: true, message: '请输入项目编码' }]}>
            <Input placeholder="请输入项目编码" />
          </Form.Item>
          <Form.Item name="description" label="项目描述">
            <Input.TextArea rows={3} placeholder="请输入项目描述" />
          </Form.Item>
          <Form.Item name="category" label="项目分类">
            <Select placeholder="请选择分类">
              {Object.entries(CATEGORY_MAP).map(([k, v]) => (
                <Select.Option key={k} value={k}>{v.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="manager" label="负责人">
            <Input placeholder="请输入负责人" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态">
              {Object.entries(STATUS_MAP).map(([k, v]) => (
                <Select.Option key={k} value={k}>{v.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="health" label="健康度">
            <Select placeholder="请选择健康度">
              {Object.entries(HEALTH_MAP).map(([k, v]) => (
                <Select.Option key={k} value={k}>{v.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="progress" label="进度 (%)">
            <Input type="number" min={0} max={100} placeholder="0-100" />
          </Form.Item>
          <Form.Item name="estimatedHours" label="预估工时 (小时)">
            <Input type="number" min={0} placeholder="请输入预估工时" />
          </Form.Item>
          <Form.Item name="actualHours" label="实际工时 (小时)">
            <Input type="number" min={0} placeholder="请输入实际工时" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ProjectList
