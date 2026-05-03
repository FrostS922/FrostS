import React, { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Card,
  Drawer,
  Empty,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Slider,
  Space,
  Switch,
  Table,
  Tag,
  Tabs,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
} from '@ant-design/icons'
import {
  getSystemSettings,
  resetSystemSettings,
  updateSystemSettings,
  type SystemSetting,
} from '../api/system'
import {
  getAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  toggleAlertRule,
  previewAlertRule,
  type AlertRuleItem,
  type AlertRulePreview,
  type CreateAlertRuleParams,
} from '../api/alertRule'
import useMessage from '../hooks/useMessage'

const settingCategoryLabels: Record<string, string> = {
  BASIC: '基础设置',
  SECURITY: '安全设置',
  NOTIFICATION: '通知设置',
  QUALITY: '质量规则',
  ALERT_RULE: '告警规则',
}

const METRIC_OPTIONS: Record<string, { label: string; value: string }[]> = {
  PERFORMANCE: [
    { label: 'LCP', value: 'LCP' },
    { label: 'CLS', value: 'CLS' },
    { label: 'FID', value: 'FID' },
    { label: 'TTFB', value: 'TTFB' },
    { label: 'INP', value: 'INP' },
    { label: 'FCP', value: 'FCP' },
  ],
  ERROR: [
    { label: 'ERROR_RATE', value: 'ERROR_RATE' },
    { label: 'ERROR_COUNT', value: 'ERROR_COUNT' },
  ],
  SECURITY: [
    { label: 'ANOMALOUS_IP_COUNT', value: 'ANOMALOUS_IP_COUNT' },
    { label: 'LOGIN_FAILURE_RATE', value: 'LOGIN_FAILURE_RATE' },
  ],
}

const THRESHOLD_CONFIG: Record<
  string,
  { min: number; max: number; step: number; good: number; poor: number; unit: string }
> = {
  LCP: { min: 0, max: 6000, step: 100, good: 2500, poor: 4000, unit: 'ms' },
  CLS: { min: 0, max: 0.5, step: 0.01, good: 0.1, poor: 0.25, unit: '' },
  FID: { min: 0, max: 500, step: 10, good: 100, poor: 300, unit: 'ms' },
  TTFB: { min: 0, max: 3000, step: 100, good: 800, poor: 1800, unit: 'ms' },
  INP: { min: 0, max: 1000, step: 10, good: 200, poor: 500, unit: 'ms' },
  FCP: { min: 0, max: 5000, step: 100, good: 1800, poor: 3000, unit: 'ms' },
}

const PRIORITY_COLOR_MAP: Record<string, string> = {
  LOW: 'default',
  MEDIUM: 'blue',
  HIGH: 'orange',
  CRITICAL: 'red',
}

const RULE_TYPE_COLOR_MAP: Record<string, string> = {
  PERFORMANCE: 'green',
  ERROR: 'red',
  SECURITY: 'orange',
}

const SystemSettings: React.FC = () => {
  const { message } = useMessage()
  const [settings, setSettings] = useState<SystemSetting[]>([])
  const [settingValues, setSettingValues] = useState<Record<string, string>>({})
  const [activeCategory, setActiveCategory] = useState<string>('')

  const [alertRules, setAlertRules] = useState<AlertRuleItem[]>([])
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<AlertRuleItem | null>(null)
  const [formValues, setFormValues] = useState<CreateAlertRuleParams>({
    name: '',
    ruleType: 'PERFORMANCE',
    windowMinutes: 30,
    minSampleCount: 5,
    cooldownMinutes: 60,
  })
  const [preview, setPreview] = useState<AlertRulePreview | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)

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

  const loadAlertRules = useCallback(async () => {
    try {
      const response = await getAlertRules()
      if (response.code === 200) {
        setAlertRules(response.data || [])
      }
    } catch {
      message.error('加载告警规则失败')
    }
  }, [message])

  useEffect(() => {
    loadSettings()
    loadAlertRules()
  }, [loadSettings, loadAlertRules])

  useEffect(() => {
    if (activeCategory === 'ALERT_RULE') {
      loadAlertRules()
    }
  }, [activeCategory, loadAlertRules])

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

  const openAddDrawer = () => {
    setEditingRule(null)
    setFormValues({
      name: '',
      ruleType: 'PERFORMANCE',
      windowMinutes: 30,
      minSampleCount: 5,
      cooldownMinutes: 60,
    })
    setPreview(null)
    setDrawerOpen(true)
  }

  const openEditDrawer = (rule: AlertRuleItem) => {
    setEditingRule(rule)
    setFormValues({
      name: rule.name,
      ruleType: rule.ruleType,
      metricName: rule.metricName || undefined,
      conditionType: rule.conditionType || undefined,
      threshold: rule.threshold || undefined,
      comparator: rule.comparator || undefined,
      windowMinutes: rule.windowMinutes || undefined,
      minSampleCount: rule.minSampleCount || undefined,
      notifyType: rule.notifyType || undefined,
      priority: rule.priority || undefined,
      cooldownMinutes: rule.cooldownMinutes || undefined,
      description: rule.description || undefined,
    })
    setPreview(null)
    setDrawerOpen(true)
  }

  const handleDrawerClose = () => {
    setDrawerOpen(false)
    setEditingRule(null)
    setPreview(null)
  }

  const handleFormChange = (field: string, value: any) => {
    setFormValues((prev) => {
      const next = { ...prev, [field]: value }
      if (field === 'ruleType') {
        next.metricName = undefined
        next.threshold = undefined
      }
      if (field === 'metricName') {
        next.threshold = undefined
      }
      return next
    })
    setPreview(null)
  }

  const handleThresholdChange = async (value: number) => {
    setFormValues((prev) => ({ ...prev, threshold: value }))
    if (formValues.metricName && formValues.ruleType) {
      setPreviewLoading(true)
      try {
        const params = { ...formValues, threshold: value }
        const response = await previewAlertRule(params)
        if (response.code === 200) {
          setPreview(response.data)
        }
      } catch {
        setPreview(null)
      } finally {
        setPreviewLoading(false)
      }
    }
  }

  const handleSubmitRule = async () => {
    if (!formValues.name || !formValues.ruleType) {
      message.warning('请填写规则名称和类型')
      return
    }
    try {
      if (editingRule) {
        const response = await updateAlertRule(editingRule.id, formValues)
        if (response.code === 200) {
          message.success('告警规则更新成功')
        }
      } else {
        const response = await createAlertRule(formValues)
        if (response.code === 200) {
          message.success('告警规则创建成功')
        }
      }
      handleDrawerClose()
      loadAlertRules()
    } catch {
      message.error(editingRule ? '更新告警规则失败' : '创建告警规则失败')
    }
  }

  const handleDeleteRule = async (id: number) => {
    try {
      const response = await deleteAlertRule(id)
      if (response.code === 200) {
        message.success('告警规则已删除')
        loadAlertRules()
      }
    } catch {
      message.error('删除告警规则失败')
    }
  }

  const handleToggleRule = async (id: number) => {
    try {
      const response = await toggleAlertRule(id)
      if (response.code === 200) {
        message.success('告警规则状态已切换')
        loadAlertRules()
      }
    } catch {
      message.error('切换告警规则状态失败')
    }
  }

  const getSliderMarks = (metricName: string) => {
    const config = THRESHOLD_CONFIG[metricName]
    if (!config) return {}
    return {
      [config.good]: {
        label: `良好 ${config.good}${config.unit}`,
        style: { color: '#52c41a' },
      },
      [config.poor]: {
        label: `较差 ${config.poor}${config.unit}`,
        style: { color: '#ff4d4f' },
      },
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

  const renderAlertRuleTab = () => {
    const columns = [
      {
        title: '启用',
        dataIndex: 'enabled',
        width: 80,
        render: (enabled: boolean, record: AlertRuleItem) => (
          <Switch
            size="small"
            checked={enabled}
            onChange={() => handleToggleRule(record.id)}
          />
        ),
      },
      {
        title: '规则名称',
        dataIndex: 'name',
        ellipsis: true,
      },
      {
        title: '规则类型',
        dataIndex: 'ruleType',
        width: 100,
        render: (type: string) => <Tag color={RULE_TYPE_COLOR_MAP[type]}>{type}</Tag>,
      },
      {
        title: '指标名称',
        dataIndex: 'metricName',
        width: 140,
        render: (v: string | null) => v || '-',
      },
      {
        title: '阈值',
        dataIndex: 'threshold',
        width: 100,
        render: (v: number | null) => (v != null ? v : '-'),
      },
      {
        title: '优先级',
        dataIndex: 'priority',
        width: 90,
        render: (v: string | null) =>
          v ? <Tag color={PRIORITY_COLOR_MAP[v]}>{v}</Tag> : '-',
      },
      {
        title: '操作',
        width: 120,
        render: (_: unknown, record: AlertRuleItem) => (
          <Space size={4}>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => openEditDrawer(record)}
            />
            <Popconfirm
              title="确认删除此告警规则？"
              onConfirm={() => handleDeleteRule(record.id)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Space>
        ),
      },
    ]

    return (
      <div style={{ paddingLeft: 24 }}>
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={openAddDrawer}>
            新增规则
          </Button>
        </div>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={alertRules}
          pagination={false}
          size="small"
        />
      </div>
    )
  }

  const renderThresholdControl = () => {
    const { ruleType, metricName, threshold } = formValues
    if (ruleType === 'PERFORMANCE' && metricName && THRESHOLD_CONFIG[metricName]) {
      const config = THRESHOLD_CONFIG[metricName]
      return (
        <div>
          <Slider
            min={config.min}
            max={config.max}
            step={config.step}
            value={threshold ?? config.good}
            marks={getSliderMarks(metricName)}
            onChange={handleThresholdChange}
          />
          <div
            style={{
              textAlign: 'center',
              color: 'var(--text-muted)',
              fontSize: 12,
            }}
          >
            当前阈值: {threshold ?? config.good} {config.unit}
          </div>
        </div>
      )
    }
    return (
      <InputNumber
        style={{ width: '100%' }}
        value={threshold ?? undefined}
        min={0}
        placeholder="请输入阈值"
        onChange={(v) => handleFormChange('threshold', v ?? undefined)}
      />
    )
  }

  const renderPreview = () => {
    if (!preview) return null
    return (
      <div
        style={{
          padding: 12,
          background: 'var(--bg-secondary, #fafafa)',
          borderRadius: 6,
          marginTop: 8,
        }}
      >
        <div style={{ marginBottom: 4, fontWeight: 600 }}>规则预览</div>
        <div style={{ fontSize: 13 }}>
          <div>
            当前值:{' '}
            <span style={{ fontWeight: 600 }}>
              {preview.currentValue ?? '暂无数据'}
            </span>
            {' / 阈值: '}
            <span style={{ fontWeight: 600 }}>{preview.threshold}</span>
          </div>
          <div>
            样本数: <span style={{ fontWeight: 600 }}>{preview.sampleCount}</span>
          </div>
          <div>
            是否触发:{' '}
            <Tag color={preview.wouldTrigger ? 'red' : 'green'}>
              {preview.wouldTrigger ? '会触发' : '不会触发'}
            </Tag>
          </div>
          {preview.message && (
            <div style={{ color: 'var(--text-muted)', marginTop: 4 }}>
              {preview.message}
            </div>
          )}
        </div>
      </div>
    )
  }

  const renderAlertRuleDrawer = () => {
    const currentMetricOptions = METRIC_OPTIONS[formValues.ruleType] || []
    return (
      <Drawer
        title={editingRule ? '编辑告警规则' : '新增告警规则'}
        open={drawerOpen}
        onClose={handleDrawerClose}
        width={480}
        extra={
          <Button type="primary" onClick={handleSubmitRule}>
            保存
          </Button>
        }
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>规则名称</div>
            <Input
              value={formValues.name}
              placeholder="请输入规则名称"
              onChange={(e) => handleFormChange('name', e.target.value)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>规则类型</div>
            <Select
              style={{ width: '100%' }}
              value={formValues.ruleType}
              options={[
                { label: '性能', value: 'PERFORMANCE' },
                { label: '错误', value: 'ERROR' },
                { label: '安全', value: 'SECURITY' },
              ]}
              onChange={(v) => handleFormChange('ruleType', v)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>指标名称</div>
            <Select
              style={{ width: '100%' }}
              value={formValues.metricName}
              options={currentMetricOptions}
              placeholder="请选择指标"
              onChange={(v) => handleFormChange('metricName', v)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>条件类型</div>
            <Select
              style={{ width: '100%' }}
              value={formValues.conditionType}
              options={[
                { label: '比率', value: 'RATIO' },
                { label: '阈值', value: 'THRESHOLD' },
                { label: '计数', value: 'COUNT' },
              ]}
              placeholder="请选择条件类型"
              onChange={(v) => handleFormChange('conditionType', v)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>阈值</div>
            {renderThresholdControl()}
            {previewLoading && (
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                加载预览中...
              </div>
            )}
            {renderPreview()}
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>时间窗口（分钟）</div>
            <InputNumber
              style={{ width: '100%' }}
              value={formValues.windowMinutes ?? 30}
              min={1}
              onChange={(v) => handleFormChange('windowMinutes', v ?? undefined)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>最小样本数</div>
            <InputNumber
              style={{ width: '100%' }}
              value={formValues.minSampleCount ?? 5}
              min={1}
              onChange={(v) => handleFormChange('minSampleCount', v ?? undefined)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>通知方式</div>
            <Select
              style={{ width: '100%' }}
              value={formValues.notifyType}
              options={[
                { label: '站内通知', value: 'NOTIFICATION' },
                { label: '邮件', value: 'EMAIL' },
                { label: '两者都', value: 'BOTH' },
              ]}
              placeholder="请选择通知方式"
              onChange={(v) => handleFormChange('notifyType', v)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>优先级</div>
            <Select
              style={{ width: '100%' }}
              value={formValues.priority}
              options={[
                { label: '低', value: 'LOW' },
                { label: '中', value: 'MEDIUM' },
                { label: '高', value: 'HIGH' },
                { label: '严重', value: 'CRITICAL' },
              ]}
              placeholder="请选择优先级"
              onChange={(v) => handleFormChange('priority', v)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>冷却时间（分钟）</div>
            <InputNumber
              style={{ width: '100%' }}
              value={formValues.cooldownMinutes ?? 60}
              min={1}
              onChange={(v) => handleFormChange('cooldownMinutes', v ?? undefined)}
            />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontWeight: 600 }}>描述</div>
            <Input.TextArea
              rows={3}
              value={formValues.description}
              placeholder="请输入规则描述"
              onChange={(e) => handleFormChange('description', e.target.value)}
            />
          </div>
        </Space>
      </Drawer>
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

  const allTabKeys = [...Object.keys(settingsByCategory)]
  if (!allTabKeys.includes('ALERT_RULE')) {
    allTabKeys.push('ALERT_RULE')
  }

  if (settings.length === 0 && alertRules.length === 0) {
    return <Empty description="暂无系统设置项" style={{ marginTop: 60 }} />
  }

  return (
    <div className="system-settings-page">
      <Card
        title={settingCategoryLabels[activeCategory] || activeCategory || '系统设置'}
        extra={
          activeCategory === 'ALERT_RULE' ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={openAddDrawer}>
              新增规则
            </Button>
          ) : (
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
          )
        }
      >
        <Tabs
          tabPosition="left"
          activeKey={activeCategory}
          onChange={setActiveCategory}
          items={allTabKeys.map((category) => ({
            label: settingCategoryLabels[category] || category,
            key: category,
            children:
              category === 'ALERT_RULE' ? (
                renderAlertRuleTab()
              ) : (
                <Space
                  direction="vertical"
                  size={16}
                  style={{ width: '100%', maxWidth: 800, paddingLeft: 24 }}
                >
                  {(settingsByCategory[category] || []).map((setting) => (
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
        {renderAlertRuleDrawer()}
      </Card>
    </div>
  )
}

export default SystemSettings
