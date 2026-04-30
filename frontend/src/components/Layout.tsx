import React, { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Avatar, Dropdown, Space, Tooltip } from 'antd'
import {
  DashboardOutlined,
  ProjectOutlined,
  FileTextOutlined,
  CheckSquareOutlined,
  CalendarOutlined,
  BugOutlined,
  LogoutOutlined,
  UserOutlined,
  SettingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  SunOutlined,
} from '@ant-design/icons'
import { useUserStore } from '../store/userStore'
import { useThemeStore } from '../store/themeStore'

const { Header, Sider, Content } = Layout

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useUserStore()
  const { mode, toggleMode } = useThemeStore()

  const logoSectionStyle: React.CSSProperties = {
    height: 64,
    display: 'flex',
    alignItems: 'center',
    justifyContent: collapsed ? 'center' : 'flex-start',
    padding: collapsed ? '0 8px' : '0 16px',
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/projects',
      icon: <ProjectOutlined />,
      label: '项目管理',
    },
    {
      key: '/requirements',
      icon: <FileTextOutlined />,
      label: '需求管理',
      children: [
        { key: '/projects/1/requirements', label: '需求列表' },
      ],
    },
    {
      key: '/testcases',
      icon: <CheckSquareOutlined />,
      label: '用例管理',
      children: [
        { key: '/projects/1/testcases', label: '用例列表' },
      ],
    },
    {
      key: '/testplans',
      icon: <CalendarOutlined />,
      label: '测试计划',
      children: [
        { key: '/projects/1/testplans', label: '计划列表' },
      ],
    },
    {
      key: '/defects',
      icon: <BugOutlined />,
      label: '缺陷管理',
      children: [
        { key: '/projects/1/defects', label: '缺陷列表' },
      ],
    },
    {
      key: '/system',
      icon: <SettingOutlined />,
      label: '系统管理',
    },
    {
      key: '/system/settings',
      icon: <SettingOutlined />,
      label: '系统设置',
    },
  ]

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  const handleMenuClick = ({ key }: { key: string }) => {
    if (!key.includes('/projects/')) {
      navigate(key)
    }
  }

  return (
    <Layout className="app-layout">
      <Sider className="app-sider" trigger={null} collapsible collapsed={collapsed}>
        <div className="app-logo" style={logoSectionStyle}>
          <img src="/logo.svg" alt="Logo" className="app-logo-mark" />
          {!collapsed && (
            <span className="app-logo-title" style={{ opacity: collapsed ? 0 : 1 }}>
              FrostS
            </span>
          )}
        </div>
        <Menu
          theme={mode === 'dark' ? 'dark' : 'light'}
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <Button
            className="app-icon-button"
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <div className="app-header-actions">
            <Tooltip title={mode === 'dark' ? '切换为亮色主题' : '切换为暗色主题'}>
              <Button
                className="app-icon-button"
                type="text"
                shape="circle"
                aria-label="切换主题"
                icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
                onClick={toggleMode}
              />
            </Tooltip>
            <Dropdown menu={{ items: userMenuItems }}>
              <Space className="app-user-trigger">
                <Avatar icon={<UserOutlined />} />
                <span>{user?.realName || user?.username}</span>
              </Space>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
