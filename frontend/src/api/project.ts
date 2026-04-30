import request from '@/utils/request'

export const getProjects = (params: { page: number; size: number; search?: string }) => {
  return request.get('/projects', { params })
}

export const getProjectList = () => {
  return request.get('/projects/list')
}

export const getProject = (id: number) => {
  return request.get(`/projects/${id}`)
}

export const createProject = (data: any) => {
  return request.post('/projects', data)
}

export const updateProject = (id: number, data: any) => {
  return request.put(`/projects/${id}`, data)
}

export const deleteProject = (id: number) => {
  return request.delete(`/projects/${id}`)
}
