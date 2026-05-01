import React, { useRef, useState } from 'react'
import { Button, Modal, Form, Input, Select, Space, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { ProTable, type ProColumns, type ActionType } from '@ant-design/pro-components'
import { getProjects, createProject, updateProject, deleteProject } from '../api/project'
import useMessage from '../hooks/useMessage'

const ProjectList: React.FC = () => {
  const [modalVisible, setModalVisible] = useState(false)
  const [editingProject, setEditingProject] = useState<any>(null)
  const [form] = Form.useForm()
  const actionRef = useRef<ActionType>()
  const message = useMessage()

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
      title: '负责人',
      dataIndex: 'manager',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: {
        PLANNING: { text: '规划中' },
        IN_PROGRESS: { text: '进行中' },
        COMPLETED: { text: '已完成' },
        ON_HOLD: { text: '已暂停' },
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
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
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="项目名称" rules={[{ required: true, message: '请输入项目名称' }]}>
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          <Form.Item name="code" label="项目编码" rules={[{ required: true, message: '请输入项目编码' }]}>
            <Input placeholder="请输入项目编码" />
          </Form.Item>
          <Form.Item name="description" label="项目描述">
            <Input.TextArea rows={3} placeholder="请输入项目描述" />
          </Form.Item>
          <Form.Item name="manager" label="负责人">
            <Input placeholder="请输入负责人" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select>
              <Select.Option value="PLANNING">规划中</Select.Option>
              <Select.Option value="IN_PROGRESS">进行中</Select.Option>
              <Select.Option value="COMPLETED">已完成</Select.Option>
              <Select.Option value="ON_HOLD">已暂停</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ProjectList
