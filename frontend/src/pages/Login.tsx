import React, { useEffect, useState } from 'react'
import { Button, Form, Input, Tooltip } from 'antd'
import {
  BarChartOutlined,
  ExperimentOutlined,
  LockOutlined,
  MoonOutlined,
  SafetyCertificateOutlined,
  SunOutlined,
  UserOutlined,
  MailOutlined,
  PhoneOutlined,
  ArrowLeftOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login, register, getRegistrationStatus, getPasswordPolicy, getCaptcha } from '../api/auth'
import { useThemeStore } from '../store/themeStore'
import { useUserStore } from '../store/userStore'
import useMessage from '../hooks/useMessage'

const fadeStyle = (delayMs: number): React.CSSProperties => ({
  animationDelay: `${delayMs}ms`,
})

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [registrationOpen, setRegistrationOpen] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [passwordMinLength, setPasswordMinLength] = useState(6)
  const [passwordComplexity, setPasswordComplexity] = useState('LOW')
  const [captchaKey, setCaptchaKey] = useState('')
  const [captchaImage, setCaptchaImage] = useState('')
  const navigate = useNavigate()
  const { login: setUser } = useUserStore()
  const { mode, toggleMode } = useThemeStore()
  const { message } = useMessage()

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
    if (!value) return Promise.reject(new Error('请输入密码'))
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

  useEffect(() => {
    getRegistrationStatus()
      .then((response) => {
        if (response.code === 200) {
          setRegistrationOpen(response.data === true)
        }
      })
      .catch(() => {
        setRegistrationOpen(false)
      })
    getPasswordPolicy()
      .then((response) => {
        if (response.code === 200 && response.data) {
          setPasswordMinLength(response.data.minLength)
          setPasswordComplexity(response.data.complexity || 'LOW')
        }
      })
      .catch(() => {})
  }, [])

  const refreshCaptcha = async () => {
    try {
      const response = await getCaptcha()
      if (response.code === 200 && response.data) {
        setCaptchaKey(response.data.captchaKey)
        setCaptchaImage(response.data.captchaImage)
      }
    } catch (_) {}
  }

  useEffect(() => {
    if (showRegister) {
      refreshCaptcha()
    }
  }, [showRegister])

  const onLogin = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const response: any = await login(values)
      if (response?.requireMfa) {
        navigate(`/mfa-verify?token=${response.mfaToken}`)
        return
      }
      if (response?.token) {
        const { token, refreshToken, username, realName, email, roles, mustChangePassword } = response
        setUser({ token, refreshToken, username, realName, email, roles, mustChangePassword })
        if (mustChangePassword) {
          message.warning('请先修改初始密码')
          navigate('/change-password', { replace: true })
        } else {
          message.success('登录成功，正在跳转...')
          setTimeout(() => navigate('/dashboard'), 800)
        }
      } else {
        message.error('登录响应异常，缺少必要信息')
      }
    } catch (error: any) {
      if (error.response) {
        const { status, data } = error.response
        if (status === 401) {
          message.error('用户名或密码错误')
        } else if (status === 403) {
          message.error(data?.message || '账号已被禁用或锁定')
        } else if (status === 500) {
          message.error(data?.message || '服务器内部错误，请稍后重试')
        } else {
          message.error(`登录失败 (错误码: ${status})`)
        }
      } else if (error.request) {
        message.error('网络连接失败，请检查网络后重试')
      } else {
        message.error('登录请求处理异常，请重试')
      }
    } finally {
      setLoading(false)
    }
  }

  const onRegister = async (values: { username: string; password: string; realName?: string; email?: string; phone?: string; captchaCode: string }) => {
    setLoading(true)
    try {
      const response: any = await register({
        ...values,
        captchaKey,
      })
      if (response?.token) {
        const { token, refreshToken, username, realName, email, roles } = response
        setUser({ token, refreshToken, username, realName, email, roles })
        message.success('注册成功，正在跳转...')
        setTimeout(() => navigate('/dashboard'), 800)
      } else {
        message.error('注册响应异常，缺少必要信息')
      }
    } catch (error: any) {
      refreshCaptcha()
      if (error.response) {
        const { status, data } = error.response
        if (status === 400) {
          message.error(data?.message || '注册信息不正确')
        } else if (status === 403) {
          message.error(data?.message || '系统未开放注册')
        } else if (status === 500) {
          message.error(data?.message || '服务器内部错误，请稍后重试')
        } else {
          message.error(`注册失败 (错误码: ${status})`)
        }
      } else if (error.request) {
        message.error('网络连接失败，请检查网络后重试')
      } else {
        message.error('注册请求处理异常，请重试')
      }
    } finally {
      setLoading(false)
    }
  }

  const features = [
    {
      icon: <ExperimentOutlined />,
      title: '测试资产统一管理',
      description: '需求、用例、计划和缺陷在同一工作台中流转',
    },
    {
      icon: <SafetyCertificateOutlined />,
      title: '角色权限控制',
      description: '管理员、测试经理、工程师和开发角色清晰分权',
    },
    {
      icon: <BarChartOutlined />,
      title: '执行趋势可视化',
      description: '用轻量指标快速识别质量风险和交付状态',
    },
  ]

  return (
    <div className="login-page">
      <Tooltip title={mode === 'dark' ? '切换为亮色主题' : '切换为暗色主题'}>
        <Button
          className="login-theme-toggle app-icon-button"
          shape="circle"
          type="text"
          aria-label="切换主题"
          icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          onClick={toggleMode}
        />
      </Tooltip>

      <section className="login-brand-panel">
        <div className="login-brand-content">
          <div className="login-badge theme-fade-up" style={fadeStyle(0)}>
            <span className="login-badge-dot" />
            FrostS Enterprise
          </div>
          <h1 className="login-brand-title theme-fade-up" style={fadeStyle(110)}>
            测试管理平台，<span>清晰掌控</span>每一次交付
          </h1>
          <p className="login-brand-subtitle theme-fade-up" style={fadeStyle(220)}>
            为企业级测试团队打造的统一质量工作台，支持项目、需求、用例、测试计划、缺陷和系统权限管理。
          </p>
        </div>

        <div className="login-feature-stack">
          {features.map((feature, index) => (
            <div
              className="login-feature theme-fade-up"
              key={feature.title}
              style={fadeStyle(340 + index * 90)}
            >
              <span className="login-feature-icon">{feature.icon}</span>
              <span>
                <strong>{feature.title}</strong>
                <br />
                {feature.description}
              </span>
            </div>
          ))}
        </div>

        <div className="login-version theme-fade-up" style={fadeStyle(640)}>
          THEME READY · DARK / LIGHT · 2026
        </div>
      </section>

      <section className="login-form-panel">
        <div className="login-card">
          {!showRegister ? (
            <>
              <div className="login-form-title theme-fade-up" style={fadeStyle(160)}>
                登录工作台
              </div>
              <div className="login-form-subtitle theme-fade-up" style={fadeStyle(250)}>
                使用你的 FrostS 账号继续
              </div>

              <Form
                onFinish={onLogin}
                size="large"
                layout="vertical"
                requiredMark={false}
              >
                <div className="theme-fade-up" style={fadeStyle(360)}>
                  <Form.Item
                    name="username"
                    label="用户名"
                    rules={[{ required: true, message: '请输入用户名' }]}
                  >
                    <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
                  </Form.Item>
                </div>

                <div className="theme-fade-up" style={fadeStyle(450)}>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[{ required: true, message: '请输入密码' }]}
                  >
                    <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
                  </Form.Item>
                </div>

                <div className="theme-fade-up" style={fadeStyle(540)}>
                  <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
                    <Button
                      className="login-submit"
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                    >
                      {loading ? '登录中...' : '登录'}
                    </Button>
                  </Form.Item>
                </div>
              </Form>

              {registrationOpen && (
                <div className="login-hint theme-fade-up" style={fadeStyle(640)}>
                  还没有账号？
                  <Button
                    type="link"
                    style={{ padding: 0, marginLeft: 4, fontWeight: 600 }}
                    onClick={() => setShowRegister(true)}
                  >
                    立即注册
                  </Button>
                </div>
              )}


            </>
          ) : (
            <>
              <div className="login-form-header">
                <div className="login-form-title theme-fade-up" style={fadeStyle(160)}>
                  注册新账号
                </div>
                <div className="login-form-subtitle theme-fade-up" style={fadeStyle(250)}>
                  创建你的 FrostS 账号
                </div>
              </div>

              <Form
                onFinish={onRegister}
                size="large"
                layout="vertical"
                requiredMark={false}
              >
                <div className="login-form-scroll">
                  <div className="theme-fade-up" style={fadeStyle(360)}>
                    <Form.Item
                      name="username"
                      label="用户名"
                      rules={[
                        { required: true, message: '请输入用户名' },
                        { max: 50, message: '用户名最长 50 个字符' },
                      ]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
                    </Form.Item>
                  </div>

                  <div className="theme-fade-up" style={fadeStyle(400)}>
                    <Form.Item
                      name="password"
                      label="密码"
                      rules={[{ validator: validatePasswordComplexity }]}
                      extra={<span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{getPasswordHint()}</span>}
                    >
                      <Input.Password prefix={<LockOutlined />} placeholder={`请输入密码（${getPasswordHint()}）`} />
                    </Form.Item>
                  </div>

                  <div className="theme-fade-up" style={fadeStyle(440)}>
                    <Form.Item name="realName" label="姓名">
                      <Input placeholder="请输入姓名（选填）" />
                    </Form.Item>
                  </div>

                  <div className="theme-fade-up" style={fadeStyle(480)}>
                    <Form.Item
                      name="email"
                      label="邮箱"
                      rules={[{ type: 'email', message: '邮箱格式不正确' }]}
                    >
                      <Input prefix={<MailOutlined />} placeholder="请输入邮箱（选填）" />
                    </Form.Item>
                  </div>

                  <div className="theme-fade-up" style={fadeStyle(520)}>
                    <Form.Item name="phone" label="手机号">
                      <Input prefix={<PhoneOutlined />} placeholder="请输入手机号（选填）" />
                    </Form.Item>
                  </div>

                  <div className="theme-fade-up" style={fadeStyle(550)}>
                    <Form.Item
                      name="captchaCode"
                      label="验证码"
                      rules={[{ required: true, message: '请输入验证码' }]}
                    >
                      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                        <Input placeholder="请输入验证码" style={{ flex: 1 }} />
                        {captchaImage && (
                          <div
                            className="captcha-wrapper"
                            style={{
                              position: 'relative',
                              height: 40,
                              borderRadius: 4,
                              overflow: 'hidden',
                              cursor: 'pointer',
                              flexShrink: 0,
                            }}
                            onClick={refreshCaptcha}
                            title="点击刷新验证码"
                          >
                            <img
                              src={captchaImage}
                              alt="验证码"
                              style={{ height: 40, display: 'block', borderRadius: 4 }}
                            />
                            <div
                              style={{
                                position: 'absolute',
                                inset: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                background: 'rgba(0, 0, 0, 0.45)',
                                opacity: 0,
                                transition: 'opacity 0.2s ease',
                                borderRadius: 4,
                                color: '#fff',
                                fontSize: 12,
                                gap: 4,
                              }}
                              className="captcha-overlay"
                            >
                              <ReloadOutlined style={{ fontSize: 16 }} />
                              <span>换一张</span>
                            </div>
                          </div>
                        )}
                      </div>
                    </Form.Item>
                  </div>
                </div>

                <div className="login-form-footer">
                  <div className="theme-fade-up" style={fadeStyle(580)}>
                    <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
                      <Button
                        className="login-submit"
                        type="primary"
                        htmlType="submit"
                        loading={loading}
                      >
                        {loading ? '注册中...' : '注册'}
                      </Button>
                    </Form.Item>
                  </div>

                  <div className="login-hint theme-fade-up" style={fadeStyle(640)}>
                    已有账号？
                    <Button
                      type="link"
                      style={{ padding: 0, marginLeft: 4, fontWeight: 600 }}
                      icon={<ArrowLeftOutlined />}
                      onClick={() => setShowRegister(false)}
                    >
                      返回登录
                    </Button>
                  </div>
                </div>
              </Form>
            </>
          )}
        </div>
      </section>
    </div>
  )
}

export default LoginPage
