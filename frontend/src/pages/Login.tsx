import React, { useState } from 'react'
import { Button, Form, Input, Tooltip, message } from 'antd'
import {
  BarChartOutlined,
  ExperimentOutlined,
  LockOutlined,
  MoonOutlined,
  SafetyCertificateOutlined,
  SunOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useThemeStore } from '../store/themeStore'
import { useUserStore } from '../store/userStore'

const fadeStyle = (delayMs: number): React.CSSProperties => ({
  animationDelay: `${delayMs}ms`,
})

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login: setUser } = useUserStore()
  const { mode, toggleMode } = useThemeStore()

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const response: any = await login(values)
      if (response?.token) {
        const { token, username, realName, email, roles } = response
        setUser({ token, username, realName, email, roles })
        message.success('登录成功，正在跳转...')
        navigate('/dashboard')
      } else {
        message.error('登录响应异常，缺少必要信息')
      }
    } catch (error: any) {
      if (error.response) {
        const { status, data } = error.response
        if (status === 401) {
          message.error('用户名或密码错误')
        } else if (status === 403) {
          message.error('账号已被禁用或无访问权限')
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
          <div className="login-form-title theme-fade-up" style={fadeStyle(160)}>
            登录工作台
          </div>
          <div className="login-form-subtitle theme-fade-up" style={fadeStyle(250)}>
            使用你的 FrostS 账号继续
          </div>

          <Form
            onFinish={onFinish}
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

          <div className="login-hint theme-fade-up" style={fadeStyle(640)}>
            默认账号
            <div className="login-hint-code">
              <span>admin</span>
              <span>/</span>
              <span>admin123</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

export default LoginPage
