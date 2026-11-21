import request from '@/utils/request'

export function getAdminGuides(params) {
  return request.get('/v1/admin/guides', { params })
}

export function getAdminGuide(id) {
  return request.get(`/v1/admin/guides/${id}`)
}

export function approveGuide(id) {
  return request.post(`/v1/admin/guides/${id}/approve`)
}

export function rejectGuide(id, reason) {
  return request.post(`/v1/admin/guides/${id}/reject`, { reason })
}
