import request from '@/utils/request'

export function getAdminComments(params) {
  return request.get('/v1/admin/comments', { params })
}

export function deleteAdminComment(id) {
  return request.delete(`/v1/admin/comments/${id}`)
}
