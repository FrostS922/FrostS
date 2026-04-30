import request from '@/utils/request'

export const getTestPlans = (projectId: number, params: { page: number; size: number }) => {
  return request.get(`/projects/${projectId}/testplans`, { params })
}

export const getTestPlan = (projectId: number, id: number) => {
  return request.get(`/projects/${projectId}/testplans/${id}`)
}

export const getTestPlanCases = (projectId: number, planId: number) => {
  return request.get(`/projects/${projectId}/testplans/${planId}/cases`)
}

export const createTestPlan = (projectId: number, data: any) => {
  return request.post(`/projects/${projectId}/testplans`, data)
}

export const updateTestPlan = (projectId: number, id: number, data: any) => {
  return request.put(`/projects/${projectId}/testplans/${id}`, data)
}

export const deleteTestPlan = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/testplans/${id}`)
}

export const addTestCaseToPlan = (projectId: number, planId: number, data: any) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases`, data)
}

export const executeTestCase = (projectId: number, caseId: number, params: { status: string; actualResult: string; executedBy: string }) => {
  return request.post(`/projects/${projectId}/testplans/cases/${caseId}/execute`, null, { params })
}
