import request from '@/utils/request'

export const getDefects = (projectId: number, params: { page: number; size: number }) => {
  return request.get(`/projects/${projectId}/defects`, { params })
}

export const getDefect = (projectId: number, id: number) => {
  return request.get(`/projects/${projectId}/defects/${id}`)
}

export const createDefect = (projectId: number, data: any) => {
  return request.post(`/projects/${projectId}/defects`, data)
}

export const updateDefect = (projectId: number, id: number, data: any) => {
  return request.put(`/projects/${projectId}/defects/${id}`, data)
}

export const resolveDefect = (projectId: number, id: number, params: { resolution: string; resolvedBy: string }) => {
  return request.post(`/projects/${projectId}/defects/${id}/resolve`, null, { params })
}

export const closeDefect = (projectId: number, id: number, params: { closedBy: string }) => {
  return request.post(`/projects/${projectId}/defects/${id}/close`, null, { params })
}

export const deleteDefect = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/defects/${id}`)
}

export const getDefectStatistics = (projectId: number) => {
  return request.get(`/projects/${projectId}/defects/statistics`)
}
