import request from '@/utils/request'

export const getRequirements = (projectId: number, params: { page: number; size: number }) => {
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

export const deleteRequirement = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/requirements/${id}`)
}
