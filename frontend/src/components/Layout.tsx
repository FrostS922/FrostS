import React, { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Avatar, Dropdown, Space, Tooltip, Breadcrumb } from 'antd'
import {
  DashboardOutlined,
  ProjectOutlined,
  FileTextOutlined,
  CheckSquareOutlined,
  CalendarOutlined,
  BugOutlined,
  BarChartOutlined,
  LogoutOutlined,
  UserOutlined,
  SettingOutlined,
  BookOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  SunOutlined,
  BellOutlined,
  AuditOutlined,
  StopOutlined,
  WarningOutlined,
  MonitorOutlined,
} from '@ant-design/icons'
import { useUserStore } from '../store/userStore'
import { useThemeStore } from '../store/themeStore'
import NotificationBell from './NotificationBell'

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
        { key: '/projects/1/requirements', icon: <FileTextOutlined />, label: '需求列表' },
      ],
    },
    {
      key: '/testcases',
      icon: <CheckSquareOutlined />,
      label: '用例管理',
      children: [
        { key: '/projects/1/testcases', icon: <CheckSquareOutlined />, label: '用例列表' },
      ],
    },
    {
      key: '/testplans',
      icon: <CalendarOutlined />,
      label: '测试计划',
      children: [
        { key: '/projects/1/testplans', icon: <CalendarOutlined />, label: '计划列表' },
      ],
    },
    {
      key: '/defects',
      icon: <BugOutlined />,
      label: '缺陷管理',
      children: [
        { key: '/projects/1/defects', icon: <BugOutlined />, label: '缺陷列表' },
      ],
    },
    {
      key: '/report',
      icon: <BarChartOutlined />,
      label: '报表统计',
      children: [
        { key: '/projects/1/report', icon: <BarChartOutlined />, label: '项目报表' },
      ],
    },
    {
      key: '/notifications',
      icon: <BellOutlined />,
      label: '通知中心',
    },
    {
      key: '/monitor',
      icon: <MonitorOutlined />,
      label: '监控中心',
      children: [
        { key: '/monitor/dashboard', icon: <DashboardOutlined />, label: '实时大屏' },
        { key: '/monitor/tracing', icon: <MonitorOutlined />, label: '链路追踪' },
      ],
    },
    {
      key: 'system-menu',
      icon: <SettingOutlined />,
      label: '系统管理',
      children: [
        { key: '/system', icon: <UserOutlined />, label: '用户与权限' },
        { key: '/system/dictionary', icon: <BookOutlined />, label: '数据字典' },
        { key: '/system/settings', icon: <SettingOutlined />, label: '系统设置' },
        { key: '/ip-bans', icon: <StopOutlined />, label: 'IP封禁' },
      ],
    },
    {
      key: '/logs',
      icon: <AuditOutlined />,
      label: '日志管理',
      children: [
        { key: '/audit-logs', icon: <AuditOutlined />, label: '审计日志' },
        { key: '/error-logs', icon: <WarningOutlined />, label: '错误日志' },
      ],
    },
  ]

  const defaultOpenKeys = menuItems
    .filter(item => item.children?.some(child => child.key === location.pathname))
    .map(item => item.key)
  const [openKeys, setOpenKeys] = useState<string[]>(defaultOpenKeys)

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const breadcrumbItems = (() => {
    const items = [{ title: '首页', key: 'home' }]
    for (const item of menuItems) {
      if (item.key === location.pathname) {
        items.push({ title: item.label, key: item.key })
        break
      }
      if (item.children) {
        const child = item.children.find(c => c.key === location.pathname)
        if (child) {
          items.push({ title: item.label, key: item.key })
          items.push({ title: child.label, key: child.key })
          break
        }
      }
    }
    return items
  })()

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
        <div className="app-sider-menu-wrapper">
          <Menu
            theme={mode === 'dark' ? 'dark' : 'light'}
            mode="inline"
            selectedKeys={[location.pathname]}
            openKeys={collapsed ? [] : openKeys}
            onOpenChange={(keys) => setOpenKeys(keys)}
            items={menuItems}
            onClick={handleMenuClick}
          />
        </div>
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <Button
              className="app-icon-button"
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
            />
            <Breadcrumb items={breadcrumbItems} />
          </div>
          <div className="app-header-actions">
            <NotificationBell />
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
                <Avatar icon={<UserOutlined />} src={user?.avatar ? `/api${user.avatar}` : undefined} />
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
