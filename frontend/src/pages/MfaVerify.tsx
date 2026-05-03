import React, { useState } from 'react'
import { Card, Input, Button, Typography, message, Space } from 'antd'
import { SafetyCertificateOutlined } from '@ant-design/icons'
import { verifyMfaLogin } from '@/api/auth'
import { useNavigate, useSearchParams } from 'react-router-dom'
import showError from '@/utils/showError'
import { useUserStore } from '@/store/userStore'

const { Title, Text } = Typography

const MfaVerify: React.FC = () => {
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [useBackup, setUseBackup] = useState(false)
  const [backupCode, setBackupCode] = useState('')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const mfaToken = searchParams.get('token') || ''
  const { login: loginUser } = useUserStore()

  const handleVerify = async () => {
    const verifyCode = useBackup ? backupCode : code
    if (!verifyCode) {
      message.warning(useBackup ? '请输入备份码' : '请输入验证码')
      return
    }
    if (!mfaToken) {
      message.error('MFA令牌无效，请重新登录')
      navigate('/login')
      return
    }
    setLoading(true)
    try {
      const res: any = await verifyMfaLogin(verifyCode, mfaToken)
      if (res?.token) {
        const { token, refreshToken, username, realName, email, roles } = res
        loginUser({ token, refreshToken, username, realName, email, roles })
        message.success('登录成功')
        navigate('/')
      } else {
        message.error('验证响应异常，缺少必要信息')
      }
    } catch (err: any) {
      showError(err, '验证码不正确，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card style={{ maxWidth: 400, margin: '0 auto', marginTop: 80 }}>
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <SafetyCertificateOutlined style={{ fontSize: 48, color: '#1890ff' }} />
        <Title level={4} style={{ marginTop: 12 }}>两步验证</Title>
        <Text type="secondary">请输入验证器应用中的验证码</Text>
      </div>

      {!useBackup ? (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Input.OTP length={6} value={code} onChange={setCode} />
          </div>
          <Button type="primary" block onClick={handleVerify} loading={loading}>
            验证
          </Button>
          <Button type="link" block onClick={() => setUseBackup(true)}>
            使用备份码
          </Button>
        </Space>
      ) : (
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Input value={backupCode} onChange={e => setBackupCode(e.target.value.toUpperCase())}
                 placeholder="请输入备份码" maxLength={8} />
          <Button type="primary" block onClick={handleVerify} loading={loading}>
            验证
          </Button>
          <Button type="link" block onClick={() => setUseBackup(false)}>
            使用验证器
          </Button>
        </Space>
      )}
    </Card>
  )
}

export default MfaVerify
