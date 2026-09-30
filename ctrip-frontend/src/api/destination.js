import request from '@/utils/request'

/**
 * 目的地相关接口
 */

/**
 * 获取目的地列表（分页、筛选）
 */
export function getDestinations(params) {
  return request.get('/v1/destinations', { params })
}

/**
 * 获取目的地详情
 */
export function getDestination(id) {
  return request.get(`/v1/destinations/${id}`)
}