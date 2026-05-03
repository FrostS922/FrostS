import React, { useEffect, useState } from 'react'
import { Table, Button, Tag, Popconfirm, message } from 'antd'
import { DeleteOutlined, LogoutOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { getSessions, terminateSession, terminateAllOtherSessions, SessionInfo } from '@/api/security'
import showError from '@/utils/showError'
import dayjs from 'dayjs'

const SessionManagement: React.FC = () => {
  const [sessions, setSessions] = useState<SessionInfo[]>([])
  const [loading, setLoading] = useState(false)

  const fetchSessions = async () => {
    setLoading(true)
    try {
      const res = await getSessions()
      if (res.code === 200) {
        setSessions(res.data)
      }
    } catch (err: any) {
      showError(err, '获取会话列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSessions()
  }, [])

  const handleTerminate = async (id: number) => {
    try {
      const res = await terminateSession(id)
      if (res.code === 200) {
        message.success('已强制下线')
        fetchSessions()
      }
    } catch (err: any) {
      showError(err, '操作失败')
    }
  }

  const handleTerminateAll = async () => {
    try {
      const res = await terminateAllOtherSessions()
      if (res.code === 200) {
        message.success('已强制下线所有其他设备')
        fetchSessions()
      }
    } catch {
      message.error('操作失败')
    }
  }

  const columns: ColumnsType<SessionInfo> = [
    {
      title: '设备信息',
      dataIndex: 'deviceInfo',
      key: 'deviceInfo',
    },
    {
      title: 'IP 地址',
      dataIndex: 'clientIp',
      key: 'clientIp',
    },
    {
      title: '最后活跃',
      dataIndex: 'lastRefreshedAt',
      key: 'lastRefreshedAt',
      render: (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '登录时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '状态',
      key: 'current',
      render: (_, record) => record.current ? <Tag color="green">当前会话</Tag> : <Tag>其他设备</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => !record.current && (
        <Popconfirm title="确定要强制下线此设备吗？" onConfirm={() => handleTerminate(record.id)}>
          <Button type="link" danger icon={<DeleteOutlined />}>强制下线</Button>
        </Popconfirm>
      ),
    },
  ]

  const otherSessionCount = sessions.filter(s => !s.current).length

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Popconfirm
          title={`确定要强制下线其他 ${otherSessionCount} 个设备吗？`}
          onConfirm={handleTerminateAll}
          disabled={otherSessionCount === 0}
        >
          <Button
            danger
            icon={<LogoutOutlined />}
            disabled={otherSessionCount === 0}
          >
            全部下线（其他设备）
          </Button>
        </Popconfirm>
      </div>
      <Table
        columns={columns}
        dataSource={sessions}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
      />
    </div>
  )
}

export default SessionManagement
