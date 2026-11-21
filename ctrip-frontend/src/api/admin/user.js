import request from '@/utils/request'

/**
 * 管理端用户接口。
 */

export function getUsers(params) {
  return request.get('/v1/admin/users', { params })
}

export function getUser(id) {
  return request.get(`/v1/admin/users/${id}`)
}

export function banUser(id) {
  return request.post(`/v1/admin/users/${id}/ban`)
}

export function unbanUser(id) {
  return request.post(`/v1/admin/users/${id}/unban`)
}
