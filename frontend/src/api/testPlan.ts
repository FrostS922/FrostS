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

export const addTestCaseToPlan = (projectId: number, planId: number, data: { testCaseId: number; priority?: string; assignedTo?: string }) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases`, data)
}

export const executeTestCase = (projectId: number, caseId: number, data: { status: string; actualResult?: string; executedBy: string; defectId?: string; defectLink?: string; evidence?: string; blockReason?: string }) => {
  return request.post(`/projects/${projectId}/testplans/cases/${caseId}/execute`, data)
}

export const batchAddTestCases = (projectId: number, planId: number, data: { testCaseIds: number[] }) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases/batch`, data)
}

export const batchRemoveTestCases = (projectId: number, planId: number, data: { planCaseIds: number[] }) => {
  return request.delete(`/projects/${projectId}/testplans/${planId}/cases/batch`, { data })
}

export const batchExecuteTestCases = (projectId: number, data: { planCaseIds: number[]; status: string; executedBy: string }) => {
  return request.post(`/projects/${projectId}/testplans/cases/batch-execute`, data)
}

export const assignTestCase = (projectId: number, caseId: number, data: { assignedTo: string }) => {
  return request.put(`/projects/${projectId}/testplans/cases/${caseId}/assign`, data)
}

export const batchAssignTestCases = (projectId: number, planId: number, data: { planCaseIds: number[]; assignedTo: string }) => {
  return request.post(`/projects/${projectId}/testplans/${planId}/cases/batch-assign`, data)
}
