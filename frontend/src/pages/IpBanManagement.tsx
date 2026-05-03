import React, { useEffect, useState } from 'react'
import { Card, Table, Button, Tag, Typography, Popconfirm, Empty } from 'antd'
import { StopOutlined, UnlockOutlined, ReloadOutlined } from '@ant-design/icons'
import { getBannedIps, unbanIp } from '../api/security'
import type { BannedIp } from '../api/security'
import useMessage from '../hooks/useMessage'

const { Title } = Typography

const IpBanManagement: React.FC = () => {
  const [data, setData] = useState<BannedIp[]>([])
  const [loading, setLoading] = useState(true)
  const { message, showError } = useMessage()

  const fetchData = async () => {
    setLoading(true)
    try {
      const response = await getBannedIps()
      if (response.code === 200 && response.data) {
        setData(response.data)
      }
    } catch (err: any) {
      showError(err, '获取封禁IP列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleUnban = async (ip: string) => {
    try {
      const response = await unbanIp(ip)
      if (response.code === 200) {
        message.success(`IP ${ip} 已解除封禁`)
        fetchData()
      }
    } catch (err: any) {
      showError(err, '解封失败')
    }
  }

  const formatRemaining = (seconds: string) => {
    const secs = parseInt(seconds, 10)
    if (secs <= 0) return '即将到期'
    if (secs < 60) return `${secs}秒`
    if (secs < 3600) return `${Math.floor(secs / 60)}分${secs % 60}秒`
    return `${Math.floor(secs / 3600)}时${Math.floor((secs % 3600) / 60)}分`
  }

  const formatBannedAt = (dateStr: string) => {
    if (!dateStr) return '-'
    try {
      return new Date(dateStr).toLocaleString('zh-CN')
    } catch {
      return dateStr
    }
  }

  const columns = [
    {
      title: 'IP 地址',
      dataIndex: 'ip',
      key: 'ip',
      width: 180,
      render: (v: string) => <Tag color="red" style={{ fontSize: 14 }}>{v}</Tag>,
    },
    {
      title: '封禁时间',
      dataIndex: 'bannedAt',
      key: 'bannedAt',
      width: 200,
      render: (v: string) => formatBannedAt(v),
    },
    {
      title: '剩余时间',
      dataIndex: 'remainingSeconds',
      key: 'remainingSeconds',
      width: 150,
      render: (v: string) => (
        <Tag color={parseInt(v, 10) < 300 ? 'orange' : 'blue'}>
          {formatRemaining(v)}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: BannedIp) => (
        <Popconfirm
          title={`确定要解封 IP ${record.ip} 吗？`}
          onConfirm={() => handleUnban(record.ip)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="link" danger icon={<UnlockOutlined />}>
            解封
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        <StopOutlined style={{ marginRight: 8 }} />
        IP 封禁管理
      </Title>

      <Card
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchData} loading={loading}>
            刷新
          </Button>
        }
      >
        {data.length === 0 && !loading ? (
          <Empty description="当前没有被封禁的IP" />
        ) : (
          <Table<BannedIp>
            dataSource={data}
            columns={columns}
            rowKey="ip"
            loading={loading}
            pagination={false}
            size="middle"
          />
        )}
      </Card>
    </div>
  )
}

export default IpBanManagement
