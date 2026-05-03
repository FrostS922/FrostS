import React, { useEffect, useState } from 'react'
import { Button, Form, Input, Result } from 'antd'
import { LockOutlined, KeyOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { changePassword, getPasswordPolicy } from '../api/auth'
import { useUserStore } from '../store/userStore'
import useMessage from '../hooks/useMessage'

const ChangePasswordPage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [passwordMinLength, setPasswordMinLength] = useState(6)
  const [passwordComplexity, setPasswordComplexity] = useState('LOW')
  const navigate = useNavigate()
  const { clearMustChangePassword, logout } = useUserStore()
  const { message } = useMessage()

  useEffect(() => {
    getPasswordPolicy()
      .then((response) => {
        if (response.code === 200 && response.data) {
          setPasswordMinLength(response.data.minLength)
          setPasswordComplexity(response.data.complexity || 'LOW')
        }
      })
      .catch(() => {})
  }, [])

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

  const onFinish = async (values: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }
    setLoading(true)
    try {
      await changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
      clearMustChangePassword()
      message.success('密码修改成功')
      navigate('/dashboard', { replace: true })
    } catch (error: any) {
      if (error.response?.data?.message) {
        message.error(error.response.data.message)
      } else {
        message.error('密码修改失败，请重试')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'var(--bg-primary, #f0f2f5)',
      padding: 24,
    }}>
      <div style={{
        width: 420,
        background: 'var(--bg-elevated, #fff)',
        borderRadius: 12,
        padding: '40px 32px',
        boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
      }}>
        <Result
          status="warning"
          title="请修改密码"
          subTitle="为了您的账号安全，需要修改当前密码后继续使用"
          style={{ padding: '12px 0 24px' }}
        />

        <Form
          onFinish={onFinish}
          layout="vertical"
          requiredMark={false}
          size="large"
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
            extra={<span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{getPasswordHint()}</span>}
          >
            <Input.Password prefix={<KeyOutlined />} placeholder={`请输入新密码（${getPasswordHint()}）`} />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="确认密码"
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

          <Form.Item style={{ marginBottom: 12 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
            >
              {loading ? '提交中...' : '确认修改'}
            </Button>
          </Form.Item>

          <Button type="link" block onClick={handleLogout} style={{ color: 'var(--text-muted)' }}>
            稍后修改，先退出
          </Button>
        </Form>
      </div>
    </div>
  )
}

export default ChangePasswordPage
