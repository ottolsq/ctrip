import request from '@/utils/request'

/**
 * 攻略相关接口
 */

/**
 * 获取攻略列表
 */
export function getGuides(params) {
  return request.get('/v1/guides', { params })
}

/**
 * 获取攻略详情
 */
export function getGuide(id) {
  return request.get(`/v1/guides/${id}`)
}

/**
 * 发布攻略
 */
export function createGuide(data) {
  return request.post('/v1/guides', data)
}

/**
 * 编辑攻略
 */
export function updateGuide(id, data) {
  return request.put(`/v1/guides/${id}`, data)
}

/**
 * 删除攻略
 */
export function deleteGuide(id) {
  return request.delete(`/v1/guides/${id}`)
}

/**
 * 点赞攻略
 */
export function likeGuide(id) {
  return request.post(`/v1/guides/${id}/like`)
}

/**
 * 取消点赞
 */
export function unlikeGuide(id) {
  return request.delete(`/v1/guides/${id}/like`)
}