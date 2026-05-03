import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card, Tabs, Table, Tag, Button, Space, Badge, Tooltip, Typography, Select, Popconfirm,
} from 'antd'
import {
  BellOutlined, NotificationOutlined, BulbOutlined, AlertOutlined,
  ClockCircleOutlined, StarOutlined, StarFilled, DeleteOutlined,
  CheckOutlined, SettingOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { useNotificationStore } from '../store/notificationStore'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Text } = Typography

const typeConfig: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  SYSTEM: { icon: <NotificationOutlined />, color: 'blue', label: '系统公告' },
  BUSINESS: { icon: <BulbOutlined />, color: 'green', label: '业务通知' },
  REMINDER: { icon: <AlertOutlined />, color: 'orange', label: '用户提醒' },
  TODO: { icon: <ClockCircleOutlined />, color: 'purple', label: '待办催办' },
}

const priorityConfig: Record<string, { color: string; label: string }> = {
  LOW: { color: 'default', label: '低' },
  NORMAL: { color: 'blue', label: '普通' },
  HIGH: { color: 'orange', label: '高' },
  URGENT: { color: 'red', label: '紧急' },
}

const NotificationCenter: React.FC = () => {
  const navigate = useNavigate()
  const {
    notifications, total, loading, currentPage, pageSize, currentType, currentIsRead,
    fetchNotifications, markAsRead, markAllAsRead, toggleStar, deleteNotification,
    setCurrentPage, setPageSize, setCurrentType, setCurrentIsRead, unreadCount,
  } = useNotificationStore()

  useEffect(() => {
    fetchNotifications(0, pageSize)
  }, [currentType, currentIsRead])

  const handleTabChange = (key: string) => {
    setCurrentType(key === 'ALL' ? undefined : key)
    setCurrentPage(0)
  }

  const handleNotificationClick = (record: any) => {
    if (!record.isRead) {
      markAsRead(record.id)
    }
    if (record.targetUrl) {
      navigate(record.targetUrl)
    }
  }

  const columns = [
    {
      title: '状态',
      dataIndex: 'isRead',
      width: 50,
      render: (isRead: boolean) =>
        !isRead ? <Badge status="processing" /> : <Badge status="default" />,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 100,
      render: (type: string) => {
        const config = typeConfig[type] || typeConfig.SYSTEM
        return <Tag icon={config.icon} color={config.color}>{config.label}</Tag>
      },
    },
    {
      title: '标题',
      dataIndex: 'title',
      render: (title: string, record: any) => (
        <Space>
          <Text
            strong={!record.isRead}
            style={{ cursor: record.targetUrl ? 'pointer' : 'default' }}
            onClick={() => handleNotificationClick(record)}
          >
            {title}
          </Text>
          {record.priority && record.priority !== 'NORMAL' && (
            <Tag color={priorityConfig[record.priority]?.color}>
              {priorityConfig[record.priority]?.label}
            </Tag>
          )}
        </Space>
      ),
    },
    {
      title: '发送者',
      dataIndex: 'senderName',
      width: 100,
      render: (name: string) => <Text type="secondary">{name}</Text>,
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 150,
      render: (time: string) => (
        <Tooltip title={dayjs(time).format('YYYY-MM-DD HH:mm:ss')}>
          <Text type="secondary">{dayjs(time).fromNow()}</Text>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      width: 150,
      render: (_: any, record: any) => (
        <Space size={4}>
          {!record.isRead && (
            <Tooltip title="标记已读">
              <Button type="text" size="small" icon={<CheckOutlined />} onClick={() => markAsRead(record.id)} />
            </Tooltip>
          )}
          <Tooltip title={record.isStarred ? '取消标星' : '标星'}>
            <Button
              type="text"
              size="small"
              icon={record.isStarred ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
              onClick={() => toggleStar(record.id)}
            />
          </Tooltip>
          <Popconfirm title="确定删除此通知？" onConfirm={() => deleteNotification(record.id)}>
            <Tooltip title="删除">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const tabItems = [
    { key: 'ALL', label: <Space><BellOutlined />全部</Space> },
    { key: 'SYSTEM', label: <Space><NotificationOutlined />系统公告 {unreadCount.systemCount > 0 && <Badge count={unreadCount.systemCount} size="small" />}</Space> },
    { key: 'BUSINESS', label: <Space><BulbOutlined />业务通知 {unreadCount.businessCount > 0 && <Badge count={unreadCount.businessCount} size="small" />}</Space> },
    { key: 'REMINDER', label: <Space><AlertOutlined />用户提醒 {unreadCount.reminderCount > 0 && <Badge count={unreadCount.reminderCount} size="small" />}</Space> },
    { key: 'TODO', label: <Space><ClockCircleOutlined />待办催办 {unreadCount.todoCount > 0 && <Badge count={unreadCount.todoCount} size="small" />}</Space> },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            <BellOutlined /> 通知中心
          </Typography.Title>
          <Space>
            <Select
              placeholder="阅读状态"
              allowClear
              style={{ width: 120 }}
              value={currentIsRead}
              onChange={(val) => { setCurrentIsRead(val); setCurrentPage(0) }}
              options={[
                { label: '未读', value: false },
                { label: '已读', value: true },
              ]}
            />
            <Button onClick={() => markAllAsRead()} disabled={unreadCount.total === 0}>
              <CheckOutlined /> 全部已读
            </Button>
            <Button onClick={() => navigate('/notifications/settings')}>
              <SettingOutlined /> 通知设置
            </Button>
          </Space>
        </div>

        <Tabs items={tabItems} activeKey={currentType || 'ALL'} onChange={handleTabChange} />

        <Table
          columns={columns}
          dataSource={notifications}
          rowKey="id"
          loading={loading}
          pagination={{
            current: currentPage + 1,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, size) => {
              setCurrentPage(page - 1)
              setPageSize(size)
              fetchNotifications(page - 1, size)
            },
          }}
          onRow={(record) => ({
            style: { cursor: 'pointer', backgroundColor: record.isRead ? 'transparent' : 'rgba(22,119,255,0.02)' },
          })}
        />
      </Card>
    </div>
  )
}

export default NotificationCenter
