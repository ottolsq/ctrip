import request from '@/utils/request'

/**
 * 管理端轮播图接口。
 */

export function getAdminBanners(params) {
  return request.get('/v1/admin/banners', { params })
}

export function createBanner(data) {
  return request.post('/v1/admin/banners', data)
}

export function updateBanner(id, data) {
  return request.put(`/v1/admin/banners/${id}`, data)
}

export function deleteBanner(id) {
  return request.delete(`/v1/admin/banners/${id}`)
}
