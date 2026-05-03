import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { App as AntApp } from 'antd'
import Layout from './components/Layout'
import Login from './pages/Login'
import ChangePassword from './pages/ChangePassword'
import Dashboard from './pages/Dashboard'
import ProjectList from './pages/ProjectList'
import RequirementList from './pages/RequirementList'
import TestCaseList from './pages/TestCaseList'
import TestPlanList from './pages/TestPlanList'
import DefectList from './pages/DefectList'
import ProjectReport from './pages/ProjectReport'
import SystemManagement from './pages/SystemManagement'
import SystemSettings from './pages/SystemSettings'
import DictionaryManagement from './pages/DictionaryManagement'
import NotificationCenter from './pages/NotificationCenter'
import NotificationSettings from './pages/NotificationSettings'
import Profile from './pages/Profile'
import AuditLogManagement from './pages/AuditLogManagement'
import IpBanManagement from './pages/IpBanManagement'
import ErrorLogManagement from './pages/ErrorLogManagement'
import MfaSetup from './pages/MfaSetup'
import MfaVerify from './pages/MfaVerify'
import MonitorDashboard from './pages/MonitorDashboard'
import ApmTracing from './pages/ApmTracing'
import { useUserStore } from './store/userStore'

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useUserStore((state) => state.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />
}

const MustChangePasswordRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const mustChangePassword = useUserStore((state) => state.mustChangePassword)
  if (mustChangePassword) {
    return <Navigate to="/change-password" replace />
  }
  return <>{children}</>
}

const App: React.FC = () => {
  return (
    <AntApp>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/mfa-verify" element={<MfaVerify />} />
        <Route
          path="/monitor/dashboard"
          element={
            <PrivateRoute>
              <MonitorDashboard />
            </PrivateRoute>
          }
        />
        <Route
          path="/change-password"
          element={
            <PrivateRoute>
              <ChangePassword />
            </PrivateRoute>
          }
        />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <MustChangePasswordRoute>
                <Layout />
              </MustChangePasswordRoute>
            </PrivateRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="projects" element={<ProjectList />} />
          <Route path="projects/:id/requirements" element={<RequirementList />} />
          <Route path="projects/:id/testcases" element={<TestCaseList />} />
          <Route path="projects/:id/testplans" element={<TestPlanList />} />
          <Route path="projects/:id/defects" element={<DefectList />} />
          <Route path="projects/:id/report" element={<ProjectReport />} />
          <Route path="system" element={<SystemManagement />} />
          <Route path="system/settings" element={<SystemSettings />} />
          <Route path="system/dictionary" element={<DictionaryManagement />} />
          <Route path="notifications" element={<NotificationCenter />} />
          <Route path="notifications/settings" element={<NotificationSettings />} />
          <Route path="profile" element={<Profile />} />
          <Route path="audit-logs" element={<AuditLogManagement />} />
          <Route path="ip-bans" element={<IpBanManagement />} />
          <Route path="error-logs" element={<ErrorLogManagement />} />
          <Route path="mfa-setup" element={<MfaSetup />} />
          <Route path="monitor/tracing" element={<ApmTracing />} />
        </Route>
      </Routes>
    </AntApp>
  )
}

export default App
