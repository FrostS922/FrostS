import request from '@/utils/request'

export interface ApiResponse<T> {
  code: number
  message: string
  data?: T
  total?: number
}

export interface DictionaryType {
  id: number
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder: number
  enabled: boolean
  isSystem?: boolean
  children?: DictionaryType[]
  createdAt?: string
  updatedAt?: string
}

export interface DictionaryItem {
  id: number
  typeId: number
  typeCode?: string
  typeName?: string
  code: string
  name: string
  value?: string
  description?: string
  sortOrder: number
  enabled: boolean
  isDefault?: boolean
  color?: string
  createdAt?: string
  updatedAt?: string
}

export interface DictionaryLog {
  id: number
  typeId?: number
  typeCode?: string
  itemId?: number
  itemCode?: string
  action: string
  oldValue?: string
  newValue?: string
  operator: string
  operatedAt: string
  ipAddress?: string
}

export interface CreateTypeRequest {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

export interface UpdateTypeRequest {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

export interface CreateItemRequest {
  typeId: number
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

export interface UpdateItemRequest {
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

export interface TypePermission {
  roleId: number
  permission: 'READ' | 'WRITE' | 'ADMIN'
}

// ==================== 分类管理 ====================

export const getDictionaryTypeTree = () => {
  return request.get('/dictionary/types') as unknown as Promise<ApiResponse<DictionaryType[]>>
}

export const getDictionaryType = (id: number) => {
  return request.get(`/dictionary/types/${id}`) as unknown as Promise<ApiResponse<DictionaryType>>
}

export const createDictionaryType = (data: CreateTypeRequest) => {
  return request.post('/dictionary/types', data) as unknown as Promise<ApiResponse<DictionaryType>>
}

export const updateDictionaryType = (id: number, data: UpdateTypeRequest) => {
  return request.put(`/dictionary/types/${id}`, data) as unknown as Promise<ApiResponse<DictionaryType>>
}

export const deleteDictionaryType = (id: number) => {
  return request.delete(`/dictionary/types/${id}`) as unknown as Promise<ApiResponse<null>>
}

// ==================== 枚举值管理 ====================

export const getDictionaryItems = (
  typeId: number,
  params?: { keyword?: string; page?: number; size?: number },
) => {
  return request.get(`/dictionary/types/${typeId}/items`, { params }) as unknown as Promise<
    ApiResponse<DictionaryItem[]>
  >
}

export const getDictionaryItemsByCode = (typeCode: string) => {
  return request.get(`/dictionary/types/code/${typeCode}/items`) as unknown as Promise<
    ApiResponse<DictionaryItem[]>
  >
}

export const createDictionaryItem = (data: CreateItemRequest) => {
  return request.post('/dictionary/items', data) as unknown as Promise<ApiResponse<DictionaryItem>>
}

export const updateDictionaryItem = (id: number, data: UpdateItemRequest) => {
  return request.put(`/dictionary/items/${id}`, data) as unknown as Promise<ApiResponse<DictionaryItem>>
}

export const deleteDictionaryItem = (id: number) => {
  return request.delete(`/dictionary/items/${id}`) as unknown as Promise<ApiResponse<null>>
}

export const updateDictionaryItemStatus = (id: number, enabled: boolean) => {
  return request.patch(`/dictionary/items/${id}/status?enabled=${enabled}`) as unknown as Promise<
    ApiResponse<DictionaryItem>
  >
}

// ==================== 权限管理 ====================

export const getTypePermissions = (id: number) => {
  return request.get(`/dictionary/types/${id}/permissions`) as unknown as Promise<
    ApiResponse<Array<{ roleId: number; permission: string }>>
  >
}

export const setTypePermissions = (id: number, permissions: TypePermission[]) => {
  return request.put(`/dictionary/types/${id}/permissions`, { permissions }) as unknown as Promise<
    ApiResponse<null>
  >
}

// ==================== 导入导出 ====================

export const importDictionaryExcel = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/dictionary/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<ApiResponse<{ typeCount: number; itemCount: number }>>
}

export const exportDictionaryExcel = () => {
  return request.get('/dictionary/export', { responseType: 'blob' }) as unknown as Promise<Blob>
}

// ==================== 操作日志 ====================

export const getDictionaryLogs = (params?: {
  typeId?: number
  operator?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) => {
  return request.get('/dictionary/logs', { params }) as unknown as Promise<ApiResponse<DictionaryLog[]>>
}
