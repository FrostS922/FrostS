import request from '@/utils/request'

export const getProjectStatistics = (projectId: number) => {
  return request.get(`/projects/${projectId}/statistics`)
}

export const getProjectOverview = (projectId: number) => {
  return request.get(`/projects/${projectId}/statistics/overview`)
}
