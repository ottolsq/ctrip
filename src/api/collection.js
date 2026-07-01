import request from '@/utils/request'

/**
 * 收藏相关接口
 */

/**
 * 获取收藏列表
 */
export function getCollections(params) {
  return request.get('/v1/collections', { params })
}

/**
 * 收藏攻略
 */
export function collectGuide(guideId) {
  return request.post(`/v1/guides/${guideId}/collect`)
}

/**
 * 取消收藏攻略
 */
export function uncollectGuide(guideId) {
  return request.delete(`/v1/guides/${guideId}/collect`)
}