import request from '@/utils/request'

/**
 * 管理端推荐位接口。
 */

export function getAdminRecommendations(params) {
  return request.get('/v1/admin/recommendations', { params })
}

export function createRecommendation(data) {
  return request.post('/v1/admin/recommendations', data)
}

export function updateRecommendation(id, data) {
  return request.put(`/v1/admin/recommendations/${id}`, data)
}

export function deleteRecommendation(id) {
  return request.delete(`/v1/admin/recommendations/${id}`)
}
