import React, { useEffect, useState } from 'react'
import { Card, Input, Button, Typography, message, Space, Alert, List, Divider } from 'antd'
import { SafetyCertificateOutlined, DownloadOutlined, CopyOutlined } from '@ant-design/icons'
import { setupMfa, verifyMfaSetup, MfaSetupData } from '@/api/auth'
import showError from '@/utils/showError'

const { Title, Text, Paragraph } = Typography

const MfaSetup: React.FC = () => {
  const [setupData, setSetupData] = useState<MfaSetupData | null>(null)
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [verifying, setVerifying] = useState(false)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    fetchSetup()
  }, [])

  const fetchSetup = async () => {
    setLoading(true)
    try {
      const res = await setupMfa()
      if (res.code === 200) {
        setSetupData(res.data)
      }
    } catch (err: any) {
      showError(err, '获取MFA设置信息失败')
    } finally {
      setLoading(false)
    }
  }

  const handleVerify = async () => {
    if (!code || code.length !== 6) {
      message.warning('请输入6位验证码')
      return
    }
    setVerifying(true)
    try {
      const res = await verifyMfaSetup(code)
      if (res.code === 200) {
        message.success('MFA绑定成功！')
        setSuccess(true)
      }
    } catch (err: any) {
      showError(err, '验证码不正确，请重试')
    } finally {
      setVerifying(false)
    }
  }

  const handleDownloadBackupCodes = () => {
    if (!setupData) return
    const text = setupData.backupCodes.join('\n')
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'frosts-mfa-backup-codes.txt'
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleCopySecret = () => {
    if (setupData) {
      navigator.clipboard.writeText(setupData.secret)
      message.success('已复制密钥')
    }
  }

  if (success) {
    return (
      <Card style={{ maxWidth: 500, margin: '0 auto', marginTop: 48 }}>
        <div style={{ textAlign: 'center' }}>
          <SafetyCertificateOutlined style={{ fontSize: 64, color: '#52c41a' }} />
          <Title level={3} style={{ marginTop: 16 }}>MFA 绑定成功</Title>
          <Paragraph>您的账号已成功启用多因素认证。请妥善保管备份码。</Paragraph>
          <Button type="primary" onClick={() => window.location.href = '/'}>
            返回首页
          </Button>
        </div>
      </Card>
    )
  }

  return (
    <Card title="设置多因素认证 (MFA)" style={{ maxWidth: 600, margin: '0 auto', marginTop: 48 }}
          loading={loading}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Alert message="请使用验证器应用（如 Google Authenticator、Microsoft Authenticator）扫描下方二维码"
               type="info" showIcon />

        {setupData && (
          <>
            <div style={{ textAlign: 'center' }}>
              <img src={`data:image/png;base64,${setupData.qrCodeBase64}`}
                   alt="MFA QR Code" style={{ width: 200, height: 200 }} />
            </div>

            <div>
              <Text strong>手动输入密钥：</Text>
              <Input.Group compact style={{ marginTop: 8 }}>
                <Input value={setupData.secret} readOnly style={{ width: 'calc(100% - 80px)', fontFamily: 'monospace' }} />
                <Button icon={<CopyOutlined />} onClick={handleCopySecret}>复制</Button>
              </Input.Group>
            </div>

            <Divider />

            <div>
              <Title level={5}>输入验证码确认绑定</Title>
              <Space>
                <Input.OTP length={6} value={code} onChange={setCode} />
                <Button type="primary" onClick={handleVerify} loading={verifying}>
                  确认绑定
                </Button>
              </Space>
            </div>

            <Divider />

            <div>
              <Title level={5}>备份码</Title>
              <Paragraph type="secondary">
                请妥善保管以下备份码。当您无法使用验证器时，可使用备份码登录。每个备份码只能使用一次。
              </Paragraph>
              <List
                grid={{ column: 2 }}
                dataSource={setupData.backupCodes}
                renderItem={(item) => (
                  <List.Item>
                    <Text code style={{ fontSize: 16 }}>{item}</Text>
                  </List.Item>
                )}
              />
              <Button icon={<DownloadOutlined />} onClick={handleDownloadBackupCodes}
                      style={{ marginTop: 8 }}>
                下载备份码
              </Button>
            </div>
          </>
        )}
      </Space>
    </Card>
  )
}

export default MfaSetup
