import request from '@/utils/request'

/**
 * 景点相关接口
 */

/**
 * 获取景点列表
 */
export function getAttractions(params) {
  return request.get('/v1/attractions', { params })
}

/**
 * 获取景点详情
 */
export function getAttraction(id) {
  return request.get(`/v1/attractions/${id}`)
}