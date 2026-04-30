import request from '@/utils/request'

export const login = (data: { username: string; password: string }) => {
  return request.post('/auth/login', data)
}

export const getCurrentUser = () => {
  return request.get('/auth/me')
}
