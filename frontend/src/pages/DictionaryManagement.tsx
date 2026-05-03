import React, { useCallback, useEffect, useRef, useState } from 'react'
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
  Switch,
  Tag,
  Tree,
  Upload,
  Drawer,
  Empty,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
  HistoryOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import {
  getDictionaryTypeTree,
  createDictionaryType,
  updateDictionaryType,
  deleteDictionaryType,
  getDictionaryItems,
  createDictionaryItem,
  updateDictionaryItem,
  deleteDictionaryItem,
  updateDictionaryItemStatus,
  importDictionaryExcel,
  exportDictionaryExcel,
  getDictionaryLogs,
  getTypePermissions,
  setTypePermissions,
  type DictionaryType,
  type DictionaryItem,
  type DictionaryLog,
} from '../api/dictionary'
import { getSystemRoles, type Role } from '../api/system'
import useMessage from '../hooks/useMessage'

interface TypeFormValues {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

interface ItemFormValues {
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

const COLORS = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#13c2c2', '#eb2f96', '#fa541c', '#fa8c16', '#a0d911',
]

const toTreeData = (types: DictionaryType[]): DataNode[] =>
  types.map((type) => ({
    key: type.id,
    title: `${type.name} (${type.code})`,
    children: type.children ? toTreeData(type.children) : undefined,
  }))

const findTypeById = (types: DictionaryType[], id: number): DictionaryType | undefined => {
  for (const type of types) {
    if (type.id === id) return type
    if (type.children) {
      const found = findTypeById(type.children, id)
      if (found) return found
    }
  }
  return undefined
}

const flattenTypes = (types: DictionaryType[]): DictionaryType[] =>
  types.flatMap((type) => [type, ...(type.children ? flattenTypes(type.children) : [])])

const DictionaryManagement: React.FC = () => {
  const { message: msg } = useMessage()
  const [types, setTypes] = useState<DictionaryType[]>([])
  const [selectedTypeId, setSelectedTypeId] = useState<number>()
  const [roles, setRoles] = useState<Role[]>([])
  const [typeModalVisible, setTypeModalVisible] = useState(false)
  const [itemModalVisible, setItemModalVisible] = useState(false)
  const [permissionModalVisible, setPermissionModalVisible] = useState(false)
  const [logDrawerVisible, setLogDrawerVisible] = useState(false)
  const [editingType, setEditingType] = useState<DictionaryType | null>(null)
  const [editingItem, setEditingItem] = useState<DictionaryItem | null>(null)
  const [logs, setLogs] = useState<DictionaryLog[]>([])
  const [typeForm] = Form.useForm<TypeFormValues>()
  const [itemForm] = Form.useForm<ItemFormValues>()
  const [permissionForm] = Form.useForm<{ permissions: Array<{ roleId: number; permission: 'READ' | 'WRITE' | 'ADMIN' }> }>()
  const itemActionRef = useRef<ActionType>(null)

  const selectedType = selectedTypeId ? findTypeById(types, selectedTypeId) : undefined
  const allTypes = flattenTypes(types)

  const loadTypes = useCallback(async () => {
    const response = await getDictionaryTypeTree()
    if (response.code === 200) {
      setTypes(response.data || [])
    }
  }, [])

  const loadRoles = useCallback(async () => {
    const response = await getSystemRoles({ page: 0, size: 200 })
    if (response.code === 200) {
      setRoles(response.data || [])
    }
  }, [])

  useEffect(() => {
    loadTypes()
    loadRoles()
  }, [loadTypes, loadRoles])

  // 分类操作
  const openCreateType = (parent?: DictionaryType) => {
    setEditingType(null)
    typeForm.resetFields()
    typeForm.setFieldsValue({ parentId: parent?.id, enabled: true, sortOrder: 0 })
    setTypeModalVisible(true)
  }

  const openEditType = (type: DictionaryType) => {
    setEditingType(type)
    typeForm.setFieldsValue({
      parentId: type.parentId,
      code: type.code,
      name: type.name,
      description: type.description,
      sortOrder: type.sortOrder,
      enabled: type.enabled,
    })
    setTypeModalVisible(true)
  }

  const submitType = async () => {
    const values = await typeForm.validateFields()
    try {
      if (editingType) {
        await updateDictionaryType(editingType.id, values)
        msg.success('分类更新成功')
      } else {
        await createDictionaryType(values)
        msg.success('分类创建成功')
      }
      setTypeModalVisible(false)
      await loadTypes()
    } catch (error: any) {
      msg.error(error.response?.data?.message || (editingType ? '分类更新失败' : '分类创建失败'))
    }
  }

  const removeType = async (id: number) => {
    try {
      await deleteDictionaryType(id)
      msg.success('分类删除成功')
      setSelectedTypeId(undefined)
      await loadTypes()
    } catch (error: any) {
      msg.error(error.response?.data?.message || '分类删除失败')
    }
  }

  // 枚举值操作
  const openCreateItem = () => {
    setEditingItem(null)
    itemForm.resetFields()
    itemForm.setFieldsValue({ enabled: true, sortOrder: 0, isDefault: false })
    setItemModalVisible(true)
  }

  const openEditItem = (item: DictionaryItem) => {
    setEditingItem(item)
    itemForm.setFieldsValue({
      code: item.code,
      name: item.name,
      value: item.value,
      description: item.description,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
      isDefault: item.isDefault,
      color: item.color,
    })
    setItemModalVisible(true)
  }

  const submitItem = async () => {
    if (!selectedTypeId) return
    const values = await itemForm.validateFields()
    try {
      if (editingItem) {
        await updateDictionaryItem(editingItem.id, values)
        msg.success('枚举值更新成功')
      } else {
        await createDictionaryItem({ ...values, typeId: selectedTypeId })
        msg.success('枚举值创建成功')
      }
      setItemModalVisible(false)
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error(error.response?.data?.message || (editingItem ? '枚举值更新失败' : '枚举值创建失败'))
    }
  }

  const removeItem = async (id: number) => {
    try {
      await deleteDictionaryItem(id)
      msg.success('枚举值删除成功')
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error(error.response?.data?.message || '枚举值删除失败')
    }
  }

  const toggleItemStatus = async (item: DictionaryItem) => {
    try {
      await updateDictionaryItemStatus(item.id, !item.enabled)
      msg.success(item.enabled ? '枚举值已禁用' : '枚举值已启用')
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error('状态更新失败')
    }
  }

  // 权限设置
  const openPermissionModal = async () => {
    if (!selectedTypeId) return
    const response = await getTypePermissions(selectedTypeId)
    if (response.code === 200) {
      const typedPermissions = (response.data || []).map(p => ({
        roleId: p.roleId,
        permission: p.permission as 'READ' | 'WRITE' | 'ADMIN'
      }))
      permissionForm.setFieldsValue({ permissions: typedPermissions })
      setPermissionModalVisible(true)
    }
  }

  const submitPermissions = async () => {
    if (!selectedTypeId) return
    const values = await permissionForm.validateFields()
    try {
      await setTypePermissions(selectedTypeId, values.permissions || [])
      msg.success('权限设置成功')
      setPermissionModalVisible(false)
    } catch (error: any) {
      msg.error('权限设置失败')
    }
  }

  // 导入导出
  const handleImport = async (file: File) => {
    try {
      const response = await importDictionaryExcel(file)
      if (response.code === 200) {
        msg.success(`导入成功: ${response.data?.typeCount || 0} 个分类, ${response.data?.itemCount || 0} 个枚举值`)
        await loadTypes()
      } else {
        msg.error(response.message || '导入失败')
      }
    } catch (error: any) {
      msg.error('导入失败')
    }
    return false
  }

  const handleExport = async () => {
    try {
      const blob = await exportDictionaryExcel()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `dictionary_export_${new Date().toISOString().slice(0, 10)}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      msg.success('导出成功')
    } catch (error) {
      msg.error('导出失败')
    }
  }

  // 日志
  const openLogDrawer = async () => {
    const response = await getDictionaryLogs({ page: 0, size: 50 })
    if (response.code === 200) {
      setLogs(response.data || [])
      setLogDrawerVisible(true)
    }
  }

  const itemColumns: ProColumns<DictionaryItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '编码 / 名称' },
    },
    {
      title: '枚举编码',
      dataIndex: 'code',
      search: false,
      width: 120,
    },
    {
      title: '枚举名称',
      dataIndex: 'name',
      search: false,
      width: 120,
    },
    {
      title: '实际值',
      dataIndex: 'value',
      search: false,
      width: 120,
      render: (_: any, record: DictionaryItem) => record.value || '-',
    },
    {
      title: '描述',
      dataIndex: 'description',
      search: false,
      ellipsis: true,
      render: (_: any, record: DictionaryItem) => record.description || '-',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      search: false,
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      search: false,
      width: 100,
      render: (_, record) => (
        <Tag color={record.enabled ? 'green' : 'default'}>
          {record.enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '默认',
      dataIndex: 'isDefault',
      search: false,
      width: 80,
      render: (_, record) => record.isDefault ? <Tag color="blue">是</Tag> : <Tag>否</Tag>,
    },
    {
      title: '颜色',
      dataIndex: 'color',
      search: false,
      width: 80,
      render: (_, record) => record.color ? (
        <div style={{ width: 20, height: 20, backgroundColor: record.color, borderRadius: 4 }} />
      ) : '-',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={16}>
          <Button type="link" style={{ padding: 0 }} icon={<EditOutlined />} onClick={() => openEditItem(record)}>
            编辑
          </Button>
          <Button type="link" style={{ padding: 0 }} onClick={() => toggleItemStatus(record)}>
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Popconfirm title="确定删除此枚举值吗？" onConfirm={() => removeItem(record.id)}>
            <Button type="link" style={{ padding: 0 }} icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const logColumns = [
    { title: '操作类型', dataIndex: 'action', width: 100 },
    { title: '分类ID', dataIndex: 'typeId', width: 80 },
    { title: '项ID', dataIndex: 'itemId', width: 80 },
    { title: '操作人', dataIndex: 'operator', width: 100 },
    { title: '操作时间', dataIndex: 'operatedAt', width: 180 },
    { title: 'IP地址', dataIndex: 'ipAddress', width: 120 },
  ]

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Space>
            <Upload beforeUpload={handleImport} showUploadList={false}>
              <Button icon={<UploadOutlined />}>导入Excel</Button>
            </Upload>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>导出Excel</Button>
            <Button icon={<HistoryOutlined />} onClick={openLogDrawer}>操作日志</Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card
            title="字典分类"
            extra={
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreateType()}>
                新建分类
              </Button>
            }
          >
            {types.length > 0 ? (
              <Tree
                blockNode
                defaultExpandAll
                selectedKeys={selectedTypeId ? [selectedTypeId] : []}
                treeData={toTreeData(types)}
                onSelect={(keys) => {
                  if (keys[0]) {
                    setSelectedTypeId(Number(keys[0]))
                  }
                }}
                titleRender={(node) => (
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>{node.title as string}</span>
                    <Space size={4}>
                      <Button
                        type="link"
                        size="small"
                        icon={<PlusOutlined />}
                        onClick={(e) => {
                          e.stopPropagation()
                          const type = findTypeById(types, node.key as number)
                          if (type) openCreateType(type)
                        }}
                      />
                      <Button
                        type="link"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => {
                          e.stopPropagation()
                          const type = findTypeById(types, node.key as number)
                          if (type) openEditType(type)
                        }}
                      />
                      <Popconfirm
                        title="确定删除此分类吗？"
                        onConfirm={(e) => {
                          e?.stopPropagation()
                          removeType(node.key as number)
                        }}
                      >
                        <Button type="link" size="small" icon={<DeleteOutlined />} danger />
                      </Popconfirm>
                    </Space>
                  </div>
                )}
              />
            ) : (
              <Empty description="暂无字典分类" />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card
            title={selectedType ? `${selectedType.name} (${selectedType.code})` : '请选择字典分类'}
            extra={
              selectedType ? (
                <Space>
                  <Button icon={<SafetyCertificateOutlined />} onClick={openPermissionModal}>
                    权限设置
                  </Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateItem}>
                    新建枚举值
                  </Button>
                </Space>
              ) : null
            }
          >
            {selectedType ? (
              <ProTable<DictionaryItem>
                columns={itemColumns}
                actionRef={itemActionRef}
                rowKey="id"
                search={false}
                headerTitle={`${selectedType.name} - 枚举值列表`}
                request={async (params) => {
                  const response = await getDictionaryItems(selectedType.id, {
                    keyword: params.keyword as string | undefined,
                    page: (params.current || 1) - 1,
                    size: params.pageSize || 10,
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
                }}
              />
            ) : (
              <Empty description="请选择左侧的字典分类" />
            )}
          </Card>
        </Col>
      </Row>

      {/* 分类编辑弹窗 */}
      <Modal
        title={editingType ? '编辑分类' : '新建分类'}
        open={typeModalVisible}
        onOk={submitType}
        onCancel={() => setTypeModalVisible(false)}
      >
        <Form form={typeForm} layout="vertical">
          <Form.Item name="parentId" label="父分类">
            <Select
              allowClear
              placeholder="不选择则作为根分类"
              options={allTypes
                .filter((t) => !editingType || t.id !== editingType.id)
                .map((t) => ({ label: `${t.name} (${t.code})`, value: t.id }))}
            />
          </Form.Item>
          <Form.Item
            name="code"
            label="分类编码"
            rules={[{ required: true, message: '请输入分类编码' }]}
          >
            <Input placeholder="例如 DEFECT_STATUS" disabled={Boolean(editingType?.isSystem)} />
          </Form.Item>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input placeholder="请输入分类名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 枚举值编辑弹窗 */}
      <Modal
        title={editingItem ? '编辑枚举值' : '新建枚举值'}
        open={itemModalVisible}
        onOk={submitItem}
        onCancel={() => setItemModalVisible(false)}
      >
        <Form form={itemForm} layout="vertical">
          <Form.Item name="code" label="枚举编码" rules={[{ required: true, message: '请输入枚举编码' }]}>
            <Input placeholder="例如 HIGH" />
          </Form.Item>
          <Form.Item name="name" label="枚举名称" rules={[{ required: true, message: '请输入枚举名称' }]}>
            <Input placeholder="请输入枚举名称" />
          </Form.Item>
          <Form.Item name="value" label="实际值">
            <Input placeholder="请输入实际值（可选）" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="color" label="颜色">
            <Select placeholder="请选择颜色" allowClear>
              {COLORS.map((color) => (
                <Select.Option key={color} value={color}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ width: 16, height: 16, backgroundColor: color, borderRadius: 4 }} />
                    {color}
                  </div>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="enabled" label="启用状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="isDefault" label="是否默认" valuePropName="checked">
                <Switch checkedChildren="是" unCheckedChildren="否" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 权限设置弹窗 */}
      <Modal
        title="权限设置"
        open={permissionModalVisible}
        onOk={submitPermissions}
        onCancel={() => setPermissionModalVisible(false)}
      >
        <Form form={permissionForm} layout="vertical">
          <Form.List name="permissions">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Row key={field.key} gutter={8} align="middle">
                    <Col span={10}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'roleId']}
                        rules={[{ required: true, message: '请选择角色' }]}
                      >
                        <Select placeholder="选择角色" options={roles.map((r) => ({ label: r.name, value: r.id }))} />
                      </Form.Item>
                    </Col>
                    <Col span={10}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'permission']}
                        rules={[{ required: true, message: '请选择权限' }]}
                      >
                        <Select
                          placeholder="选择权限"
                          options={[
                            { label: '只读', value: 'READ' },
                            { label: '读写', value: 'WRITE' },
                            { label: '管理', value: 'ADMIN' },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={4}>
                      <Button type="link" danger onClick={() => remove(field.name)}>
                        删除
                      </Button>
                    </Col>
                  </Row>
                ))}
                <Button type="dashed" onClick={() => add()} block>
                  添加权限
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>

      {/* 操作日志抽屉 */}
      <Drawer
        title="操作日志"
        width={800}
        open={logDrawerVisible}
        onClose={() => setLogDrawerVisible(false)}
      >
        <ProTable<DictionaryLog>
          dataSource={logs}
          columns={logColumns}
          rowKey="id"
          search={false}
          pagination={{ pageSize: 20 }}
          size="small"
        />
      </Drawer>
    </div>
  )
}

export default DictionaryManagement
