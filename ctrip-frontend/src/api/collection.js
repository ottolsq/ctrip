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

/**
 * 查询收藏状态
 */
export function getCollectionStatus(targetType, targetId) {
  return request.get('/v1/collections/status', { params: { targetType, targetId } })
}

/**
 * 取消收藏任意目标（攻略/行程）
 */
export function uncollectTarget(targetType, targetId) {
  return request.delete(`/v1/collections/${targetType}/${targetId}`)
}