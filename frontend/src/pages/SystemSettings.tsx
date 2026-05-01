import React, { useCallback, useEffect, useState } from 'react'
import { Button, Card, Empty, Input, InputNumber, Popconfirm, Select, Space, Switch, Tag, Tabs } from 'antd'
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import {
  getSystemSettings,
  resetSystemSettings,
  updateSystemSettings,
  type SystemSetting,
} from '../api/system'
import useMessage from '../hooks/useMessage'

const settingCategoryLabels: Record<string, string> = {
  BASIC: '基础设置',
  SECURITY: '安全设置',
  NOTIFICATION: '通知设置',
  QUALITY: '质量规则',
}

const SystemSettings: React.FC = () => {
  const message = useMessage()
  const [settings, setSettings] = useState<SystemSetting[]>([])
  const [settingValues, setSettingValues] = useState<Record<string, string>>({})
  const [activeCategory, setActiveCategory] = useState<string>('')

  const loadSettings = useCallback(async () => {
    const response = await getSystemSettings()
    if (response.code === 200) {
      const nextSettings = response.data || []
      setSettings(nextSettings)
      setSettingValues(
        Object.fromEntries(
          nextSettings.map((setting) => [setting.settingKey, setting.settingValue || '']),
        ),
      )
      if (nextSettings.length > 0) {
        setActiveCategory((current) => current || nextSettings[0].category)
      }
    }
  }, [])

  useEffect(() => {
    loadSettings()
  }, [loadSettings])

  const updateSettingValue = (settingKey: string, value: string) => {
    setSettingValues((current) => ({
      ...current,
      [settingKey]: value,
    }))
  }

  const submitSettingCategory = async (category: string) => {
    const categorySettings = settings.filter(
      (setting) => setting.category === category && setting.editable,
    )
    try {
      await updateSystemSettings(
        categorySettings.map((setting) => ({
          settingKey: setting.settingKey,
          settingValue: settingValues[setting.settingKey] ?? '',
        })),
      )
      message.success('系统设置保存成功')
      await loadSettings()
    } catch (error) {
      message.error('系统设置保存失败，请检查输入值')
    }
  }

  const resetSettingCategory = async (category: string) => {
    try {
      await resetSystemSettings(category)
      message.success('已恢复默认设置')
      await loadSettings()
    } catch (error) {
      message.error('恢复默认设置失败')
    }
  }

  const renderSettingControl = (setting: SystemSetting) => {
    const value = settingValues[setting.settingKey] ?? ''
    const disabled = !setting.editable

    if (setting.valueType === 'BOOLEAN') {
      return (
        <Switch
          checked={value === 'true'}
          disabled={disabled}
          checkedChildren="开启"
          unCheckedChildren="关闭"
          onChange={(checked) =>
            updateSettingValue(setting.settingKey, checked ? 'true' : 'false')
          }
        />
      )
    }

    if (setting.valueType === 'NUMBER') {
      return (
        <InputNumber
          value={value === '' ? undefined : Number(value)}
          disabled={disabled}
          min={0}
          style={{ width: '100%' }}
          onChange={(nextValue) =>
            updateSettingValue(
              setting.settingKey,
              nextValue === null ? '' : String(nextValue),
            )
          }
        />
      )
    }

    if (setting.valueType === 'SELECT') {
      return (
        <Select
          value={value}
          disabled={disabled}
          options={setting.options.map((option) => ({ label: option, value: option }))}
          onChange={(nextValue) => updateSettingValue(setting.settingKey, nextValue)}
        />
      )
    }

    if (setting.settingKey.includes('description')) {
      return (
        <Input.TextArea
          rows={3}
          value={value}
          disabled={disabled}
          onChange={(event) =>
            updateSettingValue(setting.settingKey, event.target.value)
          }
        />
      )
    }

    return (
      <Input
        value={value}
        disabled={disabled}
        onChange={(event) =>
          updateSettingValue(setting.settingKey, event.target.value)
        }
      />
    )
  }

  const settingsByCategory = settings.reduce<Record<string, SystemSetting[]>>(
    (groups, setting) => {
      groups[setting.category] = groups[setting.category] || []
      groups[setting.category].push(setting)
      return groups
    },
    {},
  )

  if (settings.length === 0) {
    return <Empty description="暂无系统设置项" style={{ marginTop: 60 }} />
  }

  return (
    <div>
      <Card
        title={settingCategoryLabels[activeCategory] || activeCategory || '系统设置'}
        extra={
          <Space>
            <Popconfirm
              title="恢复默认设置？"
              description="当前分类下的可编辑设置将恢复为默认值。"
              onConfirm={() => resetSettingCategory(activeCategory)}
            >
              <Button icon={<ReloadOutlined />}>恢复默认</Button>
            </Popconfirm>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={() => submitSettingCategory(activeCategory)}
            >
              保存
            </Button>
          </Space>
        }
      >
        <Tabs
          tabPosition="left"
          activeKey={activeCategory}
          onChange={setActiveCategory}
          items={Object.entries(settingsByCategory).map(([category, categorySettings]) => ({
            label: settingCategoryLabels[category] || category,
            key: category,
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%', maxWidth: 800, paddingLeft: 24 }}>
                {categorySettings.map((setting) => (
                  <div key={setting.settingKey}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        gap: 12,
                        marginBottom: 6,
                      }}
                    >
                      <span style={{ fontWeight: 600 }}>{setting.name}</span>
                      <Tag>{setting.settingKey}</Tag>
                    </div>
                    {renderSettingControl(setting)}
                    {setting.description && (
                      <div
                        style={{
                          color: 'var(--text-muted)',
                          fontSize: 12,
                          marginTop: 6,
                        }}
                      >
                        {setting.description}
                      </div>
                    )}
                  </div>
                ))}
              </Space>
            ),
          }))}
        />
      </Card>
    </div>
  )
}

export default SystemSettings
