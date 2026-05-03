import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge, Popover, List, Button, Space, Tag, Empty, Typography } from 'antd'
import {
  BellOutlined,
  NotificationOutlined,
  BulbOutlined,
  AlertOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { useNotificationStore } from '../store/notificationStore'
import { useNotificationWebSocket } from '../hooks/useNotificationWebSocket'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Text } = Typography

const typeConfig: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  SYSTEM: { icon: <NotificationOutlined />, color: 'blue', label: '系统' },
  BUSINESS: { icon: <BulbOutlined />, color: 'green', label: '业务' },
  REMINDER: { icon: <AlertOutlined />, color: 'orange', label: '提醒' },
  TODO: { icon: <ClockCircleOutlined />, color: 'purple', label: '待办' },
}

const NotificationBell: React.FC = () => {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const { unreadCount, fetchUnreadCount, notifications, fetchNotifications, markAsRead } =
    useNotificationStore()

  useNotificationWebSocket()

  useEffect(() => {
    fetchUnreadCount()
    const interval = setInterval(() => {
      fetchUnreadCount()
    }, 30000)
    return () => clearInterval(interval)
  }, [fetchUnreadCount])

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen)
    if (newOpen) {
      fetchNotifications(0, 5)
    }
  }

  const handleNotificationClick = (notification: any) => {
    if (!notification.isRead) {
      markAsRead(notification.id)
    }
    if (notification.targetUrl) {
      navigate(notification.targetUrl)
      setOpen(false)
    }
  }

  const content = (
    <div style={{ width: 360 }}>
      {notifications.length === 0 ? (
        <Empty description="暂无通知" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          dataSource={notifications.slice(0, 5)}
          renderItem={(item: any) => {
            const config = typeConfig[item.type] || typeConfig.SYSTEM
            return (
              <List.Item
                style={{
                  cursor: 'pointer',
                  padding: '8px 12px',
                  backgroundColor: item.isRead ? 'transparent' : 'rgba(22,119,255,0.04)',
                  borderRadius: 6,
                }}
                onClick={() => handleNotificationClick(item)}
              >
                <List.Item.Meta
                  avatar={
                    <span style={{ fontSize: 18, color: config.color }}>{config.icon}</span>
                  }
                  title={
                    <Space size={4}>
                      {!item.isRead && (
                        <span
                          style={{
                            display: 'inline-block',
                            width: 6,
                            height: 6,
                            borderRadius: '50%',
                            backgroundColor: '#1677ff',
                          }}
                        />
                      )}
                      <Text strong={!item.isRead} style={{ fontSize: 13 }}>
                        {item.title}
                      </Text>
                    </Space>
                  }
                  description={
                    <Space size={4}>
                      <Tag color={config.color} style={{ fontSize: 11, lineHeight: '16px', padding: '0 4px' }}>
                        {config.label}
                      </Tag>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {dayjs(item.createdAt).fromNow()}
                      </Text>
                    </Space>
                  }
                />
              </List.Item>
            )
          }}
        />
      )}
      <div style={{ textAlign: 'center', borderTop: '1px solid #f0f0f0', paddingTop: 8, marginTop: 4 }}>
        <Button type="link" onClick={() => { navigate('/notifications'); setOpen(false) }}>
          查看全部通知
        </Button>
      </div>
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={handleOpenChange}
      placement="bottomRight"
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>通知</span>
          {unreadCount.total > 0 && (
            <Button
              type="link"
              size="small"
              onClick={() => {
                useNotificationStore.getState().markAllAsRead()
              }}
            >
              全部已读
            </Button>
          )}
        </div>
      }
    >
      <Badge count={unreadCount.total} size="small" offset={[-2, 2]}>
        <Button type="text" shape="circle" icon={<BellOutlined style={{ fontSize: 16 }} />} />
      </Badge>
    </Popover>
  )
}

export default NotificationBell
