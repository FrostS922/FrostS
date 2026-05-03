import React, { useEffect, useState } from 'react'
import { Card, Tabs, Form, Input, Button, Avatar, Descriptions, Tag, Space, Spin, Row, Col, Divider, Typography, Upload, Table, Alert, Badge } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined, SafetyOutlined, SaveOutlined, KeyOutlined, CameraOutlined, CheckCircleOutlined, CloseCircleOutlined, HistoryOutlined, LogoutOutlined } from '@ant-design/icons'
import ImgCrop from 'antd-img-crop'
import { getProfile, updateProfile, changePassword, getPasswordPolicy, uploadAvatar, getSecurityInfo } from '../api/profile'
import type { ProfileData, SecurityInfo, LoginHistoryItem } from '../api/profile'
import { useUserStore } from '../store/userStore'
import useMessage from '../hooks/useMessage'
import SessionManagement from '../components/SessionManagement'

const { Title, Text } = Typography

const ProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<ProfileData | null>(null)
  const [securityInfo, setSecurityInfo] = useState<SecurityInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordMinLength, setPasswordMinLength] = useState(6)
  const [passwordComplexity, setPasswordComplexity] = useState('LOW')
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()
  const { updateProfileFromData } = useUserStore()
  const { message, showError } = useMessage()

  const fetchProfile = async () => {
    try {
      setLoading(true)
      const response = await getProfile()
      if (response.code === 200 && response.data) {
        setProfile(response.data)
        updateProfileFromData(response.data)
        profileForm.setFieldsValue({
          realName: response.data.realName || '',
          email: response.data.email || '',
          phone: response.data.phone || '',
          department: response.data.department || '',
          position: response.data.position || '',
        })
      }
    } catch (err: any) {
      showError(err, '获取用户信息失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchSecurityInfo = async () => {
    try {
      const response = await getSecurityInfo()
      if (response.code === 200 && response.data) {
        setSecurityInfo(response.data)
      }
    } catch {
      // ignore
    }
  }

  const fetchPasswordPolicy = async () => {
    try {
      const response = await getPasswordPolicy()
      if (response.code === 200 && response.data) {
        setPasswordMinLength(response.data.minLength)
        setPasswordComplexity(response.data.complexity || 'LOW')
      }
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    fetchProfile()
    fetchPasswordPolicy()
    fetchSecurityInfo()
  }, [])

  const handleAvatarUpload = async (file: File) => {
    setAvatarUploading(true)
    try {
      const uploadRes = await uploadAvatar(file)
      if (uploadRes.code === 200 && uploadRes.data) {
        const avatarUrl = uploadRes.data.url
        const updateRes = await updateProfile({ avatar: avatarUrl })
        if (updateRes.code === 200 && updateRes.data) {
          setProfile(updateRes.data)
          updateProfileFromData(updateRes.data)
          message.success('头像更新成功')
        }
      }
    } catch (error: any) {
      if (error.response?.data?.message) {
        message.error(error.response.data.message)
      } else {
        message.error('头像上传失败')
      }
    } finally {
      setAvatarUploading(false)
    }
    return false
  }

  const handleProfileSubmit = async (values: {
    realName: string
    email: string
    phone: string
    department: string
    position: string
  }) => {
    setSaving(true)
    try {
      const response = await updateProfile({
        realName: values.realName || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
        department: values.department || undefined,
        position: values.position || undefined,
      })
      if (response.code === 200 && response.data) {
        setProfile(response.data)
        updateProfileFromData(response.data)
        message.success('资料更新成功')
      }
    } catch (error: any) {
      if (error.response?.data?.message) {
        message.error(error.response.data.message)
      } else {
        message.error('资料更新失败')
      }
    } finally {
      setSaving(false)
    }
  }

  const getPasswordHint = () => {
    const hints = [`至少 ${passwordMinLength} 位`]
    if (passwordComplexity === 'MEDIUM') {
      hints.push('需含字母和数字')
    } else if (passwordComplexity === 'HIGH') {
      hints.push('需含大小写字母、数字和特殊字符')
    }
    return hints.join('，')
  }

  const validatePasswordComplexity = (_: any, value: string) => {
    if (!value) return Promise.reject(new Error('请输入新密码'))
    if (value.length < passwordMinLength) {
      return Promise.reject(new Error(`密码至少 ${passwordMinLength} 位`))
    }
    if (passwordComplexity === 'MEDIUM') {
      if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) {
        return Promise.reject(new Error('密码需包含字母和数字'))
      }
    } else if (passwordComplexity === 'HIGH') {
      if (!/[a-z]/.test(value)) {
        return Promise.reject(new Error('密码需包含小写字母'))
      }
      if (!/[A-Z]/.test(value)) {
        return Promise.reject(new Error('密码需包含大写字母'))
      }
      if (!/\d/.test(value)) {
        return Promise.reject(new Error('密码需包含数字'))
      }
      if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(value)) {
        return Promise.reject(new Error('密码需包含特殊字符'))
      }
    }
    return Promise.resolve()
  }

  const handlePasswordSubmit = async (values: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }
    setPasswordSaving(true)
    try {
      const response = await changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      })
      if (response.code === 200) {
        message.success('密码修改成功')
        passwordForm.resetFields()
        fetchSecurityInfo()
      }
    } catch (error: any) {
      if (error.response?.data?.message) {
        message.error(error.response.data.message)
      } else {
        message.error('密码修改失败')
      }
    } finally {
      setPasswordSaving(false)
    }
  }

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '-'
    try {
      return new Date(dateStr).toLocaleString('zh-CN')
    } catch {
      return dateStr
    }
  }

  const parseUserAgent = (ua: string) => {
    if (!ua) return '-'
    if (ua.includes('Chrome') && !ua.includes('Edg')) return 'Chrome'
    if (ua.includes('Edg')) return 'Edge'
    if (ua.includes('Firefox')) return 'Firefox'
    if (ua.includes('Safari') && !ua.includes('Chrome')) return 'Safari'
    return '浏览器'
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <Spin size="large" />
      </div>
    )
  }

  const avatarUrl = profile?.avatar ? `/api${profile.avatar}` : undefined

  const profileTab = (
    <Row gutter={24}>
      <Col xs={24} lg={8}>
        <Card style={{ textAlign: 'center', marginBottom: 24 }}>
          <ImgCrop
            rotationSlider
            aspectSlider
            showReset
            aspect={1}
            quality={0.9}
            modalTitle="裁剪头像"
            modalOk="确认"
            modalCancel="取消"
          >
            <Upload
              accept="image/*"
              showUploadList={false}
              beforeUpload={(file) => {
                handleAvatarUpload(file)
                return false
              }}
            >
              <div style={{ position: 'relative', display: 'inline-block', cursor: 'pointer' }}>
                <Avatar
                  size={96}
                  icon={<UserOutlined />}
                  src={avatarUrl}
                  style={{ marginBottom: 16, backgroundColor: '#1677ff' }}
                />
                <div style={{
                  position: 'absolute',
                  bottom: 16,
                  right: -4,
                  background: '#1677ff',
                  borderRadius: '50%',
                  width: 28,
                  height: 28,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid #fff',
                }}>
                  <CameraOutlined style={{ color: '#fff', fontSize: 12 }} />
                </div>
                {avatarUploading && (
                  <div style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: 96,
                    height: 96,
                    borderRadius: '50%',
                    background: 'rgba(0,0,0,0.4)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    <Spin size="small" />
                  </div>
                )}
              </div>
            </Upload>
          </ImgCrop>
          <Title level={4} style={{ marginBottom: 4 }}>{profile?.realName || profile?.username}</Title>
          <Text type="secondary">@{profile?.username}</Text>
          <div style={{ marginTop: 12 }}>
            {profile?.roles?.map((role) => {
              const roleName = role.replace('ROLE_', '')
              const colorMap: Record<string, string> = {
                ADMIN: 'red',
                TEST_MANAGER: 'blue',
                TEST_ENGINEER: 'green',
              }
              return (
                <Tag key={role} color={colorMap[roleName] || 'default'} style={{ marginBottom: 4 }}>
                  {roleName}
                </Tag>
              )
            })}
          </div>
          <Divider style={{ margin: '16px 0' }} />
          <Descriptions column={1} size="small">
            <Descriptions.Item label="部门">{profile?.department || '-'}</Descriptions.Item>
            <Descriptions.Item label="职位">{profile?.position || '-'}</Descriptions.Item>
            <Descriptions.Item label="登录次数">{profile?.loginCount ?? 0}</Descriptions.Item>
            <Descriptions.Item label="最后登录">{formatDateTime(profile?.lastLoginAt)}</Descriptions.Item>
            <Descriptions.Item label="注册时间">{formatDateTime(profile?.createdAt)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </Col>
      <Col xs={24} lg={16}>
        <Card title="编辑资料">
          <Form
            form={profileForm}
            onFinish={handleProfileSubmit}
            layout="vertical"
            requiredMark={false}
          >
            <Form.Item name="realName" label="真实姓名">
              <Input prefix={<UserOutlined />} placeholder="请输入真实姓名" maxLength={50} />
            </Form.Item>
            <Form.Item
              name="email"
              label="邮箱"
              rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
            >
              <Input prefix={<MailOutlined />} placeholder="请输入邮箱" maxLength={100} />
            </Form.Item>
            <Form.Item name="phone" label="手机号">
              <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" maxLength={20} />
            </Form.Item>
            <Form.Item name="department" label="部门">
              <Input placeholder="请输入部门" maxLength={100} />
            </Form.Item>
            <Form.Item name="position" label="职位">
              <Input placeholder="请输入职位" maxLength={100} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={saving} icon={<SaveOutlined />}>
                保存修改
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </Col>
    </Row>
  )

  const passwordTab = (
    <Card title="修改密码" style={{ maxWidth: 500 }}>
      <Form
        form={passwordForm}
        onFinish={handlePasswordSubmit}
        layout="vertical"
        requiredMark={false}
      >
        <Form.Item
          name="oldPassword"
          label="当前密码"
          rules={[{ required: true, message: '请输入当前密码' }]}
        >
          <Input.Password prefix={<LockOutlined />} placeholder="请输入当前密码" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[{ validator: validatePasswordComplexity }]}
          extra={<Text type="secondary" style={{ fontSize: 12 }}>{getPasswordHint()}</Text>}
        >
          <Input.Password prefix={<KeyOutlined />} placeholder={`请输入新密码（${getPasswordHint()}）`} />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认新密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请确认新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('两次输入的密码不一致'))
              },
            }),
          ]}
        >
          <Input.Password prefix={<LockOutlined />} placeholder="请再次输入新密码" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={passwordSaving} icon={<SafetyOutlined />}>
            修改密码
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )

  const securityTab = (
    <div>
      <Row gutter={24} style={{ marginBottom: 24 }}>
        <Col xs={24} md={8}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <SafetyOutlined style={{ fontSize: 32, color: securityInfo?.accountNonLocked !== false ? '#52c41a' : '#ff4d4f' }} />
              <Title level={5} style={{ marginTop: 12, marginBottom: 4 }}>账号状态</Title>
              {securityInfo?.accountNonLocked !== false ? (
                <Badge status="success" text={<Text type="success">正常</Text>} />
              ) : (
                <Badge status="error" text={<Text type="danger">已锁定</Text>} />
              )}
              {securityInfo?.lockReason && (
                <div style={{ marginTop: 8 }}>
                  <Alert type="error" message={securityInfo.lockReason} showIcon style={{ textAlign: 'left' }} />
                </div>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <KeyOutlined style={{ fontSize: 32, color: '#1677ff' }} />
              <Title level={5} style={{ marginTop: 12, marginBottom: 4 }}>密码安全</Title>
              <Text type="secondary">
                上次修改: {formatDateTime(securityInfo?.passwordChangedAt)}
              </Text>
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  登录失败次数: {securityInfo?.loginFailCount ?? 0}
                </Text>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <HistoryOutlined style={{ fontSize: 32, color: '#722ed1' }} />
              <Title level={5} style={{ marginTop: 12, marginBottom: 4 }}>登录统计</Title>
              <Text type="secondary">
                累计登录: {securityInfo?.loginCount ?? 0} 次
              </Text>
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  最近IP: {securityInfo?.lastLoginIp || '-'}
                </Text>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Card title="最近登录记录">
        <Table<LoginHistoryItem>
          dataSource={securityInfo?.recentLogins || []}
          rowKey="id"
          pagination={{ pageSize: 10 }}
          size="small"
          columns={[
            {
              title: '时间',
              dataIndex: 'loginAt',
              key: 'loginAt',
              width: 180,
              render: (v: string) => formatDateTime(v),
            },
            {
              title: 'IP 地址',
              dataIndex: 'loginIp',
              key: 'loginIp',
              width: 140,
            },
            {
              title: '浏览器',
              dataIndex: 'userAgent',
              key: 'userAgent',
              render: (v: string) => parseUserAgent(v),
            },
            {
              title: '状态',
              dataIndex: 'success',
              key: 'success',
              width: 100,
              render: (v: boolean) => v
                ? <Tag icon={<CheckCircleOutlined />} color="success">成功</Tag>
                : <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>,
            },
            {
              title: '失败原因',
              dataIndex: 'failReason',
              key: 'failReason',
              ellipsis: true,
              render: (v: string) => v || '-',
            },
          ]}
        />
      </Card>
    </div>
  )

  return (
    <div style={{ padding: '0 4px' }}>
      <Title level={4} style={{ marginBottom: 24 }}>个人中心</Title>
      <Tabs
        defaultActiveKey="profile"
        items={[
          {
            key: 'profile',
            label: (
              <Space>
                <UserOutlined />
                个人资料
              </Space>
            ),
            children: profileTab,
          },
          {
            key: 'password',
            label: (
              <Space>
                <LockOutlined />
                修改密码
              </Space>
            ),
            children: passwordTab,
          },
          {
            key: 'security',
            label: (
              <Space>
                <SafetyOutlined />
                安全设置
              </Space>
            ),
            children: securityTab,
          },
          {
            key: 'sessions',
            label: (
              <Space>
                <LogoutOutlined />
                在线会话
              </Space>
            ),
            children: <SessionManagement />,
          },
        ]}
      />
    </div>
  )
}

export default ProfilePage
