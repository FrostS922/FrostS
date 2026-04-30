import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import ProjectList from './pages/ProjectList'
import RequirementList from './pages/RequirementList'
import TestCaseList from './pages/TestCaseList'
import TestPlanList from './pages/TestPlanList'
import DefectList from './pages/DefectList'
import SystemManagement from './pages/SystemManagement'
import SystemSettings from './pages/SystemSettings'
import { useUserStore } from './store/userStore'

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useUserStore((state) => state.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />
}

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
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
        <Route path="system" element={<SystemManagement />} />
        <Route path="system/settings" element={<SystemSettings />} />
      </Route>
    </Routes>
  )
}

export default App
