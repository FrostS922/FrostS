import React, { useEffect } from 'react'
import { Card, Typography, Switch, Space, Divider, TimePicker, message } from 'antd'
import {
  SettingOutlined, NotificationOutlined, BulbOutlined,
  AlertOutlined, ClockCircleOutlined, MailOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNotificationStore } from '../store/notificationStore'

const { Title, Text } = Typography

const typeLabels: Record<string, { icon: React.ReactNode; label: string }> = {
  SYSTEM: { icon: <NotificationOutlined />, label: '系统公告' },
  BUSINESS: { icon: <BulbOutlined />, label: '业务通知' },
  REMINDER: { icon: <AlertOutlined />, label: '用户提醒' },
  TODO: { icon: <ClockCircleOutlined />, label: '待办催办' },
}

const categoryLabels: Record<string, string> = {
  DEFECT_ASSIGNED: '缺陷分配',
  DEFECT_STATUS_CHANGED: '缺陷状态变更',
  PLAN_EXPIRED: '计划到期',
  PLAN_ASSIGNED: '计划分配',
  REQUIREMENT_REVIEW: '需求审核',
  PERMISSION_CHANGED: '权限变更',
  PASSWORD_CHANGED: '密码修改',
  LOGIN_ANOMALY: '登录异常',
  TASK_OVERDUE: '任务逾期',
  TEST_REMINDER: '测试提醒',
}

const NotificationSettings: React.FC = () => {
  const { preferences, fetchPreferences, updatePreferences } = useNotificationStore()
  const [messageApi, contextHolder] = message.useMessage()

  useEffect(() => {
    fetchPreferences()
  }, [])

  const handleTypeToggle = (type: string, checked: boolean) => {
    if (!preferences) return
    updatePreferences({
      typeSettings: { ...preferences.typeSettings, [type]: checked },
    })
    messageApi.success(checked ? `已开启${typeLabels[type]?.label}通知` : `已关闭${typeLabels[type]?.label}通知`)
  }

  const handleCategoryToggle = (category: string, checked: boolean) => {
    if (!preferences) return
    updatePreferences({
      categorySettings: { ...preferences.categorySettings, [category]: checked },
    })
  }

  const handleChannelToggle = (channel: string, checked: boolean) => {
    if (!preferences) return
    updatePreferences({
      receiveChannels: { ...preferences.receiveChannels, [channel]: checked },
    })
  }

  const handleQuietHoursChange = (field: 'quietHoursStart' | 'quietHoursEnd', time: dayjs.Dayjs | null) => {
    if (!preferences || !time) return
    updatePreferences({
      [field]: time.format('HH:mm'),
    })
  }

  if (!preferences) return null

  return (
    <div style={{ padding: 24 }}>
      {contextHolder}
      <Card>
        <Title level={4} style={{ marginBottom: 24 }}>
          <SettingOutlined /> 通知设置
        </Title>

        <Title level={5}>按类型开关</Title>
        <Text type="secondary" style={{ marginBottom: 16, display: 'block' }}>
          控制各类通知的接收开关
        </Text>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {Object.entries(typeLabels).map(([key, config]) => (
            <div
              key={key}
              style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '12px 16px', backgroundColor: 'rgba(0,0,0,0.02)', borderRadius: 8,
              }}
            >
              <Space>
                <span style={{ fontSize: 18 }}>{config.icon}</span>
                <Text strong>{config.label}</Text>
              </Space>
              <Switch
                checked={preferences.typeSettings?.[key] !== false}
                onChange={(checked) => handleTypeToggle(key, checked)}
              />
            </div>
          ))}
        </Space>

        <Divider />

        <Title level={5}>按分类细控</Title>
        <Text type="secondary" style={{ marginBottom: 16, display: 'block' }}>
          精细控制各业务分类的通知接收
        </Text>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
          {Object.entries(categoryLabels).map(([key, label]) => (
            <div
              key={key}
              style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '8px 12px', backgroundColor: 'rgba(0,0,0,0.02)', borderRadius: 6,
              }}
            >
              <Text>{label}</Text>
              <Switch
                size="small"
                checked={preferences.categorySettings?.[key] !== false}
                onChange={(checked) => handleCategoryToggle(key, checked)}
              />
            </div>
          ))}
        </div>

        <Divider />

        <Title level={5}>接收渠道</Title>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div
            style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              padding: '12px 16px', backgroundColor: 'rgba(0,0,0,0.02)', borderRadius: 8,
            }}
          >
            <Space>
              <NotificationOutlined />
              <Text strong>站内通知</Text>
            </Space>
            <Switch
              checked={preferences.receiveChannels?.in_app !== false}
              onChange={(checked) => handleChannelToggle('in_app', checked)}
            />
          </div>
          <div
            style={{
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              padding: '12px 16px', backgroundColor: 'rgba(0,0,0,0.02)', borderRadius: 8,
            }}
          >
            <Space>
              <MailOutlined />
              <Text strong>邮件通知</Text>
            </Space>
            <Switch
              checked={preferences.receiveChannels?.email === true}
              onChange={(checked) => handleChannelToggle('email', checked)}
            />
          </div>
        </Space>

        <Divider />

        <Title level={5}>免打扰时段</Title>
        <Text type="secondary" style={{ marginBottom: 16, display: 'block' }}>
          设置免打扰时段，该时段内不会收到通知提醒
        </Text>
        <Space size={16}>
          <div>
            <Text style={{ marginBottom: 4, display: 'block' }}>开始时间</Text>
            <TimePicker
              format="HH:mm"
              value={preferences.quietHoursStart ? dayjs(preferences.quietHoursStart, 'HH:mm') : null}
              onChange={(time) => handleQuietHoursChange('quietHoursStart', time)}
              placeholder="选择时间"
            />
          </div>
          <div>
            <Text style={{ marginBottom: 4, display: 'block' }}>结束时间</Text>
            <TimePicker
              format="HH:mm"
              value={preferences.quietHoursEnd ? dayjs(preferences.quietHoursEnd, 'HH:mm') : null}
              onChange={(time) => handleQuietHoursChange('quietHoursEnd', time)}
              placeholder="选择时间"
            />
          </div>
        </Space>
      </Card>
    </div>
  )
}

export default NotificationSettings
