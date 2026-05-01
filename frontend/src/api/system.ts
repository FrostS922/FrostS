import request from '@/utils/request'

export interface ApiResponse<T> {
  code: number
  message: string
  data?: T
  total?: number
}

export interface Permission {
  id: number
  code: string
  name: string
  description?: string
  resource?: string
  action?: string
  createdAt?: string
  updatedAt?: string
}

export interface RoleSummary {
  id: number
  code: string
  name: string
}

export interface Role {
  id: number
  code: string
  name: string
  description?: string
  sortOrder: number
  permissions: Permission[]
  createdAt?: string
  updatedAt?: string
}

export interface SystemUser {
  id: number
  username: string
  realName?: string
  email?: string
  phone?: string
  enabled: boolean
  roles: RoleSummary[]
  createdAt?: string
  updatedAt?: string
}

export interface SystemOverview {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  totalRoles: number
  totalPermissions: number
  totalOrganizations: number
  totalSettings: number
}

export type OrganizationUnitType = 'COMPANY' | 'DEPARTMENT' | 'TEAM' | 'GROUP'

export interface OrganizationUnit {
  id: number
  parentId?: number
  code: string
  name: string
  type: OrganizationUnitType | string
  leader?: string
  contactEmail?: string
  contactPhone?: string
  sortOrder: number
  enabled: boolean
  description?: string
  children: OrganizationUnit[]
  createdAt?: string
  updatedAt?: string
}

export type SystemSettingValueType = 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT'

export interface SystemSetting {
  id: number
  settingKey: string
  settingValue: string
  defaultValue: string
  name: string
  category: string
  valueType: SystemSettingValueType | string
  options: string[]
  description?: string
  sortOrder: number
  editable: boolean
  updatedAt?: string
}

interface PageParams {
  page: number
  size: number
  search?: string
}

export const getSystemOverview = () => {
  return request.get('/system/overview') as unknown as Promise<ApiResponse<SystemOverview>>
}

export const getSystemUsers = (params: PageParams) => {
  return request.get('/system/users', { params }) as unknown as Promise<ApiResponse<SystemUser[]>>
}

export const createSystemUser = (data: {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  enabled?: boolean
  roleIds?: number[]
}) => {
  return request.post('/system/users', data) as unknown as Promise<ApiResponse<SystemUser>>
}

export const updateSystemUser = (
  id: number,
  data: {
    username?: string
    realName?: string
    email?: string
    phone?: string
    enabled?: boolean
    roleIds?: number[]
  },
) => {
  return request.put(`/system/users/${id}`, data) as unknown as Promise<ApiResponse<SystemUser>>
}

export const deleteSystemUser = (id: number) => {
  return request.delete(`/system/users/${id}`) as unknown as Promise<ApiResponse<null>>
}

export const resetSystemUserPassword = (id: number, data: { password: string }) => {
  return request.post(`/system/users/${id}/reset-password`, data) as unknown as Promise<ApiResponse<null>>
}

export const getSystemRoles = (params: PageParams) => {
  return request.get('/system/roles', { params }) as unknown as Promise<ApiResponse<Role[]>>
}

export const createSystemRole = (data: {
  code: string
  name: string
  description?: string
  permissionIds?: number[]
}) => {
  return request.post('/system/roles', data) as unknown as Promise<ApiResponse<Role>>
}

export const updateSystemRole = (
  id: number,
  data: {
    code: string
    name: string
    description?: string
    permissionIds?: number[]
  },
) => {
  return request.put(`/system/roles/${id}`, data) as unknown as Promise<ApiResponse<Role>>
}

export const deleteSystemRole = (id: number) => {
  return request.delete(`/system/roles/${id}`) as unknown as Promise<ApiResponse<null>>
}

export const updateRoleSort = (roleIds: number[]) => {
  return request.put('/system/roles/sort', { roleIds }) as unknown as Promise<ApiResponse<null>>
}

export const getSystemPermissions = () => {
  return request.get('/system/permissions') as unknown as Promise<ApiResponse<Permission[]>>
}

export const getOrganizationTree = () => {
  return request.get('/system/organizations/tree') as unknown as Promise<ApiResponse<OrganizationUnit[]>>
}

export const createOrganizationUnit = (data: {
  parentId?: number
  name: string
  code: string
  type: string
  leader?: string
  contactEmail?: string
  contactPhone?: string
  sortOrder?: number
  enabled?: boolean
  description?: string
}) => {
  return request.post('/system/organizations', data) as unknown as Promise<ApiResponse<OrganizationUnit>>
}

export const updateOrganizationUnit = (
  id: number,
  data: {
    parentId?: number
    name: string
    code: string
    type: string
    leader?: string
    contactEmail?: string
    contactPhone?: string
    sortOrder?: number
    enabled?: boolean
    description?: string
  },
) => {
  return request.put(`/system/organizations/${id}`, data) as unknown as Promise<ApiResponse<OrganizationUnit>>
}

export const deleteOrganizationUnit = (id: number) => {
  return request.delete(`/system/organizations/${id}`) as unknown as Promise<ApiResponse<null>>
}

export const getSystemSettings = (category?: string) => {
  return request.get('/system/settings', { params: { category } }) as unknown as Promise<ApiResponse<SystemSetting[]>>
}

export const updateSystemSettings = (settings: Array<{ settingKey: string; settingValue: string }>) => {
  return request.put('/system/settings', { settings }) as unknown as Promise<ApiResponse<SystemSetting[]>>
}

export const resetSystemSettings = (category?: string) => {
  return request.post('/system/settings/reset', null, { params: { category } }) as unknown as Promise<ApiResponse<SystemSetting[]>>
}
