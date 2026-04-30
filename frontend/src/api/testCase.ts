import request from '@/utils/request'

export const getTestCases = (projectId: number, params: { page: number; size: number }) => {
  return request.get(`/projects/${projectId}/testcases`, { params })
}

export const getTestCase = (projectId: number, id: number) => {
  return request.get(`/projects/${projectId}/testcases/${id}`)
}

export const createTestCase = (projectId: number, data: any) => {
  return request.post(`/projects/${projectId}/testcases`, data)
}

export const updateTestCase = (projectId: number, id: number, data: any) => {
  return request.put(`/projects/${projectId}/testcases/${id}`, data)
}

export const deleteTestCase = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/testcases/${id}`)
}
