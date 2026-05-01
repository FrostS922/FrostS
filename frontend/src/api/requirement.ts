import request from '@/utils/request'

export interface RequirementQueryParams {
  page: number
  size: number
  search?: string
  status?: string
  priority?: string
  type?: string
}

export const getRequirements = (projectId: number, params: RequirementQueryParams) => {
  return request.get(`/projects/${projectId}/requirements`, { params })
}

export const getRootRequirements = (projectId: number) => {
  return request.get(`/projects/${projectId}/requirements/root`)
}

export const getRequirement = (projectId: number, id: number) => {
  return request.get(`/projects/${projectId}/requirements/${id}`)
}

export const createRequirement = (projectId: number, data: any) => {
  return request.post(`/projects/${projectId}/requirements`, data)
}

export const updateRequirement = (projectId: number, id: number, data: any) => {
  return request.put(`/projects/${projectId}/requirements/${id}`, data)
}

export const updateRequirementStatus = (projectId: number, id: number, status: string) => {
  return request.put(`/projects/${projectId}/requirements/${id}`, { status })
}

export const deleteRequirement = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/requirements/${id}`)
}
