import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Tabs,
  Tag,
  Tree,
  Descriptions,
  Empty,
  Typography,
  message as antMessage,
} from 'antd'
import {
  ApartmentOutlined,
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  LockOutlined,
  UnlockOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  TeamOutlined,
  IdcardOutlined,
} from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { ProTable, ProList, DragSortTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import {
  createOrganizationUnit,
  createSystemRole,
  createSystemUser,
  deleteOrganizationUnit,
  deleteSystemRole,
  deleteSystemUser,
  getOrganizationTree,
  getSystemOverview,
  getSystemPermissions,
  getSystemRoles,
  getSystemUsers,
  updateOrganizationUnit,
  resetSystemUserPassword,
  unlockSystemUser,
  updateSystemRole,
  updateSystemUser,
  updateRoleSort,
  type OrganizationUnit,
  type Permission,
  type Role,
  type SystemOverview,
  type SystemUser,
} from '../api/system'
import useMessage from '../hooks/useMessage'

interface UserFormValues {
  username: string
  realName?: string
  email?: string
  phone?: string
  department?: string
  position?: string
  enabled?: boolean
  roleIds?: number[]
}

interface RoleFormValues {
  code: string
  name: string
  description?: string
  permissionIds?: number[]
}

interface OrganizationFormValues {
  parentId?: number
  name: string
  code: string
  type: string
  leader?: string
  contactEmail?: string
  contactPhone?: string
  sortOrder?: number
  enabled?: boolean
  description?: string
}

const flattenOrganizations = (items: OrganizationUnit[]): OrganizationUnit[] =>
  items.flatMap((item) => [item, ...flattenOrganizations(item.children || [])])

const findOrganization = (items: OrganizationUnit[], id?: number): OrganizationUnit | undefined => {
  if (!id) {
    return undefined
  }
  for (const item of items) {
    if (item.id === id) {
      return item
    }
    const child = findOrganization(item.children || [], id)
    if (child) {
      return child
    }
  }
  return undefined
}

const collectOrganizationIds = (item?: OrganizationUnit): number[] => {
  if (!item) {
    return []
  }
  return [item.id, ...(item.children || []).flatMap(collectOrganizationIds)]
}

const toOrganizationTreeData = (items: OrganizationUnit[]): DataNode[] =>
  items.map((item) => ({
    key: item.id,
    title: `${item.name} (${item.code})`,
    children: toOrganizationTreeData(item.children || []),
  }))

const SystemManagement: React.FC = () => {
  const [searchParams] = useSearchParams()
  const tabFromUrl = searchParams.get('tab')
  const [activeTab, setActiveTab] = useState(tabFromUrl || 'users')
  const { message } = useMessage()
  const [overview, setOverview] = useState<SystemOverview>({
    totalUsers: 0,
    enabledUsers: 0,
    disabledUsers: 0,
    totalRoles: 0,
    totalPermissions: 0,
    totalOrganizations: 0,
    totalSettings: 0,
  })
  const [roles, setRoles] = useState<Role[]>([])
  const [permissions, setPermissions] = useState<Permission[]>([])
  const [organizations, setOrganizations] = useState<OrganizationUnit[]>([])
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number>()
  const [userModalVisible, setUserModalVisible] = useState(false)
  const [roleModalVisible, setRoleModalVisible] = useState(false)
  const [organizationModalVisible, setOrganizationModalVisible] = useState(false)
  const [resetModalVisible, setResetModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState<SystemUser | null>(null)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const [editingOrganization, setEditingOrganization] = useState<OrganizationUnit | null>(null)
  const [resettingUser, setResettingUser] = useState<SystemUser | null>(null)
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null)
  const [userForm] = Form.useForm<UserFormValues>()
  const [roleForm] = Form.useForm<RoleFormValues>()
  const [organizationForm] = Form.useForm<OrganizationFormValues>()
  const userActionRef = useRef<ActionType>()
  const roleActionRef = useRef<ActionType>()

  const loadReferenceData = useCallback(async () => {
    const [overviewResponse, rolesResponse, permissionsResponse, organizationsResponse] = await Promise.all([
      getSystemOverview(),
      getSystemRoles({ page: 0, size: 200 }),
      getSystemPermissions(),
      getOrganizationTree(),
    ])

    if (overviewResponse.code === 200 && overviewResponse.data) {
      setOverview(overviewResponse.data)
    }
    if (rolesResponse.code === 200) {
      setRoles(rolesResponse.data || [])
    }
    if (permissionsResponse.code === 200) {
      setPermissions(permissionsResponse.data || [])
    }
    if (organizationsResponse.code === 200) {
      const tree = organizationsResponse.data || []
      setOrganizations(tree)
      setSelectedOrganizationId((current) => current || tree[0]?.id)
    }
  }, [])

  useEffect(() => {
    loadReferenceData()
  }, [loadReferenceData])

  const openCreateUser = () => {
    setEditingUser(null)
    userForm.resetFields()
    userForm.setFieldsValue({ enabled: true, roleIds: [] })
    setUserModalVisible(true)
  }

  const openEditUser = (record: SystemUser) => {
    setEditingUser(record)
    userForm.setFieldsValue({
      username: record.username,
      realName: record.realName,
      email: record.email,
      phone: record.phone,
      department: record.department,
      position: record.position,
      enabled: record.enabled,
      roleIds: record.roles.map((role) => role.id),
    })
    setUserModalVisible(true)
  }

  const submitUser = async () => {
    const values = await userForm.validateFields()
    const payload = {
      ...values,
      enabled: values.enabled ?? true,
      roleIds: values.roleIds || [],
    }

    try {
      if (editingUser) {
        await updateSystemUser(editingUser.id, {
          username: payload.username,
          realName: payload.realName,
          email: payload.email,
          phone: payload.phone,
          enabled: payload.enabled,
          roleIds: payload.roleIds,
        })
        message.success('用户更新成功')
        setUserModalVisible(false)
      } else {
        const response = await createSystemUser(payload)
        if (response.data?.generatedPassword) {
          setGeneratedPassword(response.data.generatedPassword)
        }
        message.success('用户创建成功')
        setUserModalVisible(false)
      }
      userActionRef.current?.reload()
      await loadReferenceData()
    } catch (error) {
      message.error(editingUser ? '用户更新失败' : '用户创建失败')
    }
  }

  const removeUser = async (id: number) => {
    try {
      await deleteSystemUser(id)
      message.success('用户删除成功')
      userActionRef.current?.reload()
      await loadReferenceData()
    } catch (error) {
      message.error('用户删除失败')
    }
  }

  const handleUnlockUser = async (id: number) => {
    try {
      await unlockSystemUser(id)
      message.success('用户解锁成功')
      userActionRef.current?.reload()
    } catch (error) {
      message.error('用户解锁失败')
    }
  }

  const openResetPassword = (record: SystemUser) => {
    setResettingUser(record)
    setResetModalVisible(true)
  }

  const submitResetPassword = async () => {
    if (!resettingUser) {
      return
    }

    try {
      const response = await resetSystemUserPassword(resettingUser.id)
      if (response.data?.generatedPassword) {
        setGeneratedPassword(response.data.generatedPassword)
      }
      setResetModalVisible(false)
      message.success('密码重置成功')
    } catch (error) {
      message.error('密码重置失败')
    }
  }

  const openCreateRole = () => {
    setEditingRole(null)
    roleForm.resetFields()
    roleForm.setFieldsValue({ permissionIds: [] })
    setRoleModalVisible(true)
  }

  const openEditRole = (record: Role) => {
    setEditingRole(record)
    roleForm.setFieldsValue({
      code: record.code,
      name: record.name,
      description: record.description,
      permissionIds: record.permissions.map((permission) => permission.id),
    })
    setRoleModalVisible(true)
  }

  const submitRole = async () => {
    const values = await roleForm.validateFields()
    const payload = {
      ...values,
      code: values.code.toUpperCase(),
      permissionIds: values.permissionIds || [],
    }

    try {
      if (editingRole) {
        await updateSystemRole(editingRole.id, payload)
        message.success('角色更新成功')
      } else {
        await createSystemRole(payload)
        message.success('角色创建成功')
      }
      setRoleModalVisible(false)
      roleActionRef.current?.reload()
      await loadReferenceData()
    } catch (error) {
      message.error(editingRole ? '角色更新失败' : '角色创建失败')
    }
  }

  const removeRole = async (id: number) => {
    try {
      await deleteSystemRole(id)
      message.success('角色删除成功')
      roleActionRef.current?.reload()
      await loadReferenceData()
    } catch (error) {
      message.error('角色删除失败')
    }
  }

  const openCreateOrganization = (parent?: OrganizationUnit) => {
    setEditingOrganization(null)
    organizationForm.resetFields()
    organizationForm.setFieldsValue({
      parentId: parent?.id,
      type: parent ? 'DEPARTMENT' : 'COMPANY',
      enabled: true,
      sortOrder: 0,
    })
    setOrganizationModalVisible(true)
  }

  const openEditOrganization = (record: OrganizationUnit) => {
    setEditingOrganization(record)
    organizationForm.setFieldsValue({
      parentId: record.parentId,
      name: record.name,
      code: record.code,
      type: record.type,
      leader: record.leader,
      contactEmail: record.contactEmail,
      contactPhone: record.contactPhone,
      sortOrder: record.sortOrder,
      enabled: record.enabled,
      description: record.description,
    })
    setOrganizationModalVisible(true)
  }

  const submitOrganization = async () => {
    const values = await organizationForm.validateFields()
    const payload = {
      ...values,
      code: values.code.toUpperCase(),
      enabled: values.enabled ?? true,
      sortOrder: values.sortOrder ?? 0,
    }

    try {
      if (editingOrganization) {
        const response = await updateOrganizationUnit(editingOrganization.id, payload)
        setSelectedOrganizationId(response.data?.id || editingOrganization.id)
        message.success('组织更新成功')
      } else {
        const response = await createOrganizationUnit(payload)
        setSelectedOrganizationId(response.data?.id)
        message.success('组织创建成功')
      }
      setOrganizationModalVisible(false)
      await loadReferenceData()
    } catch (error) {
      message.error(editingOrganization ? '组织更新失败' : '组织创建失败')
    }
  }

  const removeOrganization = async (id: number) => {
    try {
      await deleteOrganizationUnit(id)
      message.success('组织删除成功')
      setSelectedOrganizationId(undefined)
      await loadReferenceData()
    } catch (error) {
      message.error('组织删除失败，请先确认没有下级组织')
    }
  }

  const roleOptions = roles.map((role) => ({
    label: `${role.name} (${role.code})`,
    value: role.id,
  }))

  const permissionOptions = permissions.map((permission) => ({
    label: `${permission.name} (${permission.code})`,
    value: permission.id,
  }))

  const organizationTreeData = toOrganizationTreeData(organizations)
  const allOrganizations = flattenOrganizations(organizations)
  const selectedOrganization = findOrganization(organizations, selectedOrganizationId)
  const excludedParentIds = collectOrganizationIds(editingOrganization || undefined)
  const parentOptions = allOrganizations
    .filter((organization) => !excludedParentIds.includes(organization.id))
    .map((organization) => ({
      label: `${organization.name} (${organization.code})`,
      value: organization.id,
    }))

  const userColumns: ProColumns<SystemUser>[] = [
    {
      title: '关键词',
      dataIndex: 'search',
      hideInTable: true,
      fieldProps: {
        placeholder: '用户名 / 姓名 / 邮箱',
      },
    },
    {
      title: '用户名',
      dataIndex: 'username',
      search: false,
      width: 120,
      ellipsis: true,
    },
    {
      title: '姓名',
      dataIndex: 'realName',
      search: false,
      width: 100,
      ellipsis: true,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      search: false,
      width: 180,
      ellipsis: true,
    },
    {
      title: '部门',
      dataIndex: 'department',
      search: false,
      width: 120,
      ellipsis: true,
      render: (_: any, record: SystemUser) => record.department || '—',
    },
    {
      title: '职位',
      dataIndex: 'position',
      search: false,
      width: 100,
      ellipsis: true,
      render: (_: any, record: SystemUser) => record.position || '—',
    },
    {
      title: '角色',
      dataIndex: 'roles',
      search: false,
      width: 180,
      render: (_, record) => (
        <Space size={[0, 4]} wrap>
          {record.roles.map((role) => (
            <Tag color="blue" key={role.id}>
              {role.name}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      search: false,
      width: 100,
      render: (_, record) => (
        <Space size={4}>
          <Tag color={record.enabled ? 'green' : 'default'}>
            {record.enabled ? '启用' : '停用'}
          </Tag>
          {record.accountNonLocked === false && (
            <Tag color="red">锁定</Tag>
          )}
        </Space>
      ),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size={16}>
          <Button type="link" style={{ padding: 0 }} icon={<EditOutlined />} onClick={() => openEditUser(record)}>
            编辑
          </Button>
          {record.accountNonLocked === false && (
            <Popconfirm title="确定解锁此用户吗？" onConfirm={() => handleUnlockUser(record.id)}>
              <Button type="link" style={{ padding: 0 }} icon={<UnlockOutlined />}>
                解锁
              </Button>
            </Popconfirm>
          )}
          <Button type="link" style={{ padding: 0 }} icon={<LockOutlined />} onClick={() => openResetPassword(record)}>
            重置密码
          </Button>
          <Popconfirm title="确定删除此用户吗？" onConfirm={() => removeUser(record.id)}>
            <Button type="link" style={{ padding: 0 }} icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const roleColumns: ProColumns<Role>[] = [
    {
      title: '排序',
      dataIndex: 'sort',
      width: 60,
      className: 'drag-visible',
    },
    {
      title: '关键词',
      dataIndex: 'search',
      hideInTable: true,
      fieldProps: {
        placeholder: '角色编码 / 角色名称',
      },
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      search: false,
      ellipsis: true,
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      search: false,
      ellipsis: true,
    },
    {
      title: '描述',
      dataIndex: 'description',
      search: false,
      ellipsis: true,
    },
    {
      title: '权限数量',
      dataIndex: 'permissions',
      search: false,
      width: 110,
      render: (_, record) => <Tag color="purple">{record.permissions.length}</Tag>,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 180,
      render: (_, record) => (
        <Space size={16}>
          <Button type="link" style={{ padding: 0 }} icon={<EditOutlined />} onClick={() => openEditRole(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除此角色吗？" onConfirm={() => removeRole(record.id)}>
            <Button type="link" style={{ padding: 0 }} icon={<DeleteOutlined />} danger disabled={record.code === 'ADMIN'}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]



  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="metric-card" size="small">
            <Statistic title="用户总数" value={overview.totalUsers} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="metric-card" size="small">
            <Statistic title="角色数量" value={overview.totalRoles} prefix={<SafetyCertificateOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="metric-card" size="small">
            <Statistic title="权限数量" value={overview.totalPermissions} prefix={<LockOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="metric-card" size="small">
            <Statistic title="组织节点" value={overview.totalOrganizations} prefix={<ApartmentOutlined />} />
          </Card>
        </Col>
      </Row>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => setActiveTab(key)}
        items={[
          {
            key: 'users',
            label: '用户管理',
            children: (
              <ProTable<SystemUser>
                columns={userColumns}
                actionRef={userActionRef}
                rowKey="id"
                rowSelection={{ fixed: true }}
                tableAlertRender={false}
                scroll={{ x: 1100 }}
                headerTitle="用户管理"
                toolBarRender={() => [
                  <Button key="create" type="primary" icon={<PlusOutlined />} onClick={openCreateUser}>
                    新建用户
                  </Button>,
                ]}
                request={async (params) => {
                  const response = await getSystemUsers({
                    page: (params.current || 1) - 1,
                    size: params.pageSize || 10,
                    search: params.search as string | undefined,
                  })

                  return {
                    data: response.data || [],
                    success: response.code === 200,
                    total: response.total || 0,
                  }
                }}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showQuickJumper: true,
                }}
              />
            ),
          },
          {
            key: 'roles',
            label: '角色管理',
            children: (
              <DragSortTable<Role>
                columns={roleColumns}
                rowKey="id"
                headerTitle="角色管理"
                search={false}
                dragSortKey="sort"
                dataSource={roles}
                onDragSortEnd={async (_beforeIndex, _afterIndex, newDataSource) => {
                  try {
                    const ids = newDataSource.map(role => role.id)
                    await updateRoleSort(ids)
                    setRoles(newDataSource)
                    message.success('排序操作成功')
                  } catch (e: any) {
                    console.error('排序保存失败:', e)
                    message.error('排序保存失败')
                  }
                }}
                toolBarRender={() => [
                  <Button key="create" type="primary" icon={<PlusOutlined />} onClick={openCreateRole}>
                    新建角色
                  </Button>,
                ]}
                pagination={false}
              />
            ),
          },
          {
            key: 'organizations',
            label: '组织架构',
            children: (
              <Row gutter={[16, 16]}>
                <Col xs={24} lg={9}>
                  <Card
                    title="组织树"
                    extra={
                      <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreateOrganization()}>
                        新建根组织
                      </Button>
                    }
                  >
                    {organizationTreeData.length > 0 ? (
                      <Tree
                        blockNode
                        defaultExpandAll
                        selectedKeys={selectedOrganizationId ? [selectedOrganizationId] : []}
                        treeData={organizationTreeData}
                        onSelect={(keys) => {
                          if (keys[0]) {
                            setSelectedOrganizationId(Number(keys[0]))
                          }
                        }}
                      />
                    ) : (
                      <Empty description="暂无组织节点" />
                    )}
                  </Card>
                </Col>
                <Col xs={24} lg={15}>
                  <Card
                    title="组织详情"
                    extra={
                      <Space>
                        <Button
                          icon={<PlusOutlined />}
                          disabled={!selectedOrganization}
                          onClick={() => selectedOrganization && openCreateOrganization(selectedOrganization)}
                        >
                          新建下级
                        </Button>
                        <Button
                          icon={<EditOutlined />}
                          disabled={!selectedOrganization}
                          onClick={() => selectedOrganization && openEditOrganization(selectedOrganization)}
                        >
                          编辑
                        </Button>
                        <Popconfirm
                          title="确定删除此组织吗？"
                          description="存在下级组织时无法删除。"
                          onConfirm={() => selectedOrganization && removeOrganization(selectedOrganization.id)}
                        >
                          <Button icon={<DeleteOutlined />} danger disabled={!selectedOrganization}>
                            删除
                          </Button>
                        </Popconfirm>
                      </Space>
                    }
                  >
                    {selectedOrganization ? (
                      <Descriptions bordered column={2} size="small">
                        <Descriptions.Item label="组织名称">{selectedOrganization.name}</Descriptions.Item>
                        <Descriptions.Item label="组织编码">{selectedOrganization.code}</Descriptions.Item>
                        <Descriptions.Item label="组织类型">
                          <Tag color="blue">{selectedOrganization.type}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="状态">
                          <Tag color={selectedOrganization.enabled ? 'green' : 'default'}>
                            {selectedOrganization.enabled ? '启用' : '停用'}
                          </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="负责人">
                          {selectedOrganization.leader || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="排序">
                          {selectedOrganization.sortOrder}
                        </Descriptions.Item>
                        <Descriptions.Item label="联系邮箱">
                          {selectedOrganization.contactEmail || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="联系电话">
                          {selectedOrganization.contactPhone || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="描述" span={2}>
                          {selectedOrganization.description || '-'}
                        </Descriptions.Item>
                      </Descriptions>
                    ) : (
                      <Empty description="请选择组织节点" />
                    )}
                  </Card>
                </Col>
              </Row>
            ),
          },
          {
            key: 'permissions',
            label: '权限查看',
            children: (
              <ProList<Permission>
                dataSource={permissions}
                rowKey="id"
                headerTitle="权限查看"
                itemLayout="horizontal"
                // variant="borderless"
                split={true}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                }}
                metas={{
                  title: {
                    dataIndex: 'name',
                  },
                  description: {
                    dataIndex: 'code',
                  },
                  subTitle: {
                    render: (_, record) => (
                      <Space size={[0, 8]} wrap>
                        <Tag color="blue">{record.resource}</Tag>
                        <Tag>{record.action}</Tag>
                      </Space>
                    ),
                  },
                  content: {
                    dataIndex: 'description',
                  },
                }}
              />
            ),
          },
        ]}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={userModalVisible}
        onOk={submitUser}
        onCancel={() => setUserModalVisible(false)}
        forceRender
      >
        <Form form={userForm} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="请输入用户名" disabled={Boolean(editingUser)} />
          </Form.Item>
          {!editingUser && (
            <div style={{ marginBottom: 24, padding: '12px 16px', background: 'var(--panel-bg-elevated)', borderRadius: 8 }}>
              <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 4 }}>
                初始密码将由系统自动生成，创建成功后请及时告知用户
              </div>
            </div>
          )}
          <Form.Item name="realName" label="姓名">
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="department" label="部门">
                <Input placeholder="请输入部门" prefix={<TeamOutlined />} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="position" label="职位">
                <Input placeholder="请输入职位" prefix={<IdcardOutlined />} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="roleIds" label="角色">
            <Select mode="multiple" placeholder="请选择角色" options={roleOptions} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingRole ? '编辑角色' : '新建角色'}
        open={roleModalVisible}
        onOk={submitRole}
        onCancel={() => setRoleModalVisible(false)}
        forceRender
      >
        <Form form={roleForm} layout="vertical">
          <Form.Item
            name="code"
            label="角色编码"
            rules={[
              { required: true, message: '请输入角色编码' },
              { pattern: /^[A-Z][A-Z0-9_]{1,49}$/, message: '需使用大写字母、数字或下划线' },
            ]}
          >
            <Input placeholder="例如 TEST_MANAGER" disabled={editingRole?.code === 'ADMIN'} />
          </Form.Item>
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="请输入角色名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="permissionIds" label="权限">
            <Select mode="multiple" placeholder="请选择权限" options={permissionOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingOrganization ? '编辑组织' : '新建组织'}
        open={organizationModalVisible}
        onOk={submitOrganization}
        onCancel={() => setOrganizationModalVisible(false)}
        forceRender
      >
        <Form form={organizationForm} layout="vertical">
          <Form.Item name="parentId" label="上级组织">
            <Select allowClear placeholder="不选择则作为根组织" options={parentOptions} />
          </Form.Item>
          <Form.Item name="name" label="组织名称" rules={[{ required: true, message: '请输入组织名称' }]}>
            <Input placeholder="请输入组织名称" />
          </Form.Item>
          <Form.Item
            name="code"
            label="组织编码"
            rules={[
              { required: true, message: '请输入组织编码' },
              { pattern: /^[A-Z][A-Z0-9_]{1,49}$/, message: '需使用大写字母、数字或下划线' },
            ]}
          >
            <Input placeholder="例如 QA_CENTER" />
          </Form.Item>
          <Form.Item name="type" label="组织类型" rules={[{ required: true, message: '请选择组织类型' }]}>
            <Select
              options={[
                { label: '公司', value: 'COMPANY' },
                { label: '部门', value: 'DEPARTMENT' },
                { label: '团队', value: 'TEAM' },
                { label: '小组', value: 'GROUP' },
              ]}
            />
          </Form.Item>
          <Form.Item name="leader" label="负责人">
            <Input placeholder="请输入负责人" />
          </Form.Item>
          <Form.Item name="contactEmail" label="联系邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input placeholder="请输入联系邮箱" />
          </Form.Item>
          <Form.Item name="contactPhone" label="联系电话">
            <Input placeholder="请输入联系电话" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`重置密码${resettingUser ? ` - ${resettingUser.username}` : ''}`}
        open={resetModalVisible}
        onOk={submitResetPassword}
        onCancel={() => setResetModalVisible(false)}
        okText="确认重置"
      >
        <div style={{ padding: '12px 0' }}>
          <p>确定要为用户 <strong>{resettingUser?.username}</strong> 重置密码吗？</p>
          <p style={{ color: 'var(--text-muted, #999)', fontSize: 13 }}>系统将自动生成新的随机密码，重置后请及时告知用户</p>
        </div>
      </Modal>

      <Modal
        title="密码信息"
        open={generatedPassword !== null}
        onCancel={() => setGeneratedPassword(null)}
        footer={<Button type="primary" onClick={() => setGeneratedPassword(null)}>知道了</Button>}
      >
        <div style={{ padding: '12px 0' }}>
          <p>系统已自动生成以下密码，请妥善保管并告知用户：</p>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '12px 16px',
            background: 'var(--panel-bg-elevated)',
            borderRadius: 8,
            marginTop: 12,
          }}>
            <Typography.Text strong style={{ fontSize: 18, fontFamily: 'monospace', letterSpacing: 1 }}>
              {generatedPassword}
            </Typography.Text>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => {
                navigator.clipboard.writeText(generatedPassword || '')
                antMessage.success('密码已复制到剪贴板')
              }}
            >
              复制
            </Button>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: 12, marginTop: 12 }}>
            关闭此窗口后将无法再次查看此密码，请务必复制保存
          </p>
        </div>
      </Modal>
    </div>
  )
}

export default SystemManagement
