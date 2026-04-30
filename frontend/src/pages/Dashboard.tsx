import React from 'react'
import { Card, Row, Col, Statistic } from 'antd'
import { ProjectOutlined, CheckCircleOutlined, BugOutlined, FileTextOutlined } from '@ant-design/icons'

const Dashboard: React.FC = () => {
  const stats = [
    { title: '项目总数', value: 12, icon: <ProjectOutlined />, color: '#1890ff' },
    { title: '测试用例', value: 1234, icon: <CheckCircleOutlined />, color: '#52c41a' },
    { title: '活跃缺陷', value: 56, icon: <BugOutlined />, color: '#ff4d4f' },
    { title: '需求总数', value: 345, icon: <FileTextOutlined />, color: '#faad14' },
  ]

  return (
    <div>
      <h2 className="page-title">仪表盘</h2>
      <Row gutter={[16, 16]}>
        {stats.map((stat) => (
          <Col xs={24} sm={12} lg={6} key={stat.title}>
            <Card className="metric-card">
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={React.cloneElement(stat.icon, { style: { color: stat.color, fontSize: 24 } })}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}

export default Dashboard
