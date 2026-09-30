import request from '@/utils/request'

/**
 * 行程相关接口
 */

/**
 * 创建行程
 */
export function createItinerary(data) {
  return request.post('/v1/itineraries', data)
}

/**
 * 获取我的行程列表
 */
export function getItineraries(params) {
  return request.get('/v1/itineraries', { params })
}

/**
 * 获取行程详情
 */
export function getItinerary(id) {
  return request.get(`/v1/itineraries/${id}`)
}

/**
 * 编辑行程
 */
export function updateItinerary(id, data) {
  return request.put(`/v1/itineraries/${id}`, data)
}

/**
 * 删除行程
 */
export function deleteItinerary(id) {
  return request.delete(`/v1/itineraries/${id}`)
}

/**
 * 添加日程
 */
export function addDay(itineraryId, data) {
  return request.post(`/v1/itineraries/${itineraryId}/days`, data)
}

/**
 * 编辑日程
 */
export function updateDay(dayId, data) {
  return request.put(`/v1/itineraries/days/${dayId}`, data)
}

/**
 * 删除日程
 */
export function deleteDay(dayId) {
  return request.delete(`/v1/itineraries/days/${dayId}`)
}

/**
 * 添加行程项
 */
export function addItem(itineraryId, dayId, data) {
  return request.post(`/v1/itineraries/${itineraryId}/days/${dayId}/items`, data)
}

/**
 * 编辑行程项
 */
export function updateItem(itemId, data) {
  return request.put(`/v1/itineraries/items/${itemId}`, data)
}

/**
 * 删除行程项
 */
export function deleteItem(itemId) {
  return request.delete(`/v1/itineraries/items/${itemId}`)
}

/**
 * 批量排序行程项
 */
export function reorderItems(dayId, data) {
  return request.post(`/v1/itineraries/days/${dayId}/items/reorder`, data)
}

/**
 * 收藏/取消收藏行程
 */
export function collectItinerary(id, add) {
  return request.post(`/v1/itineraries/${id}/collection?add=${add}`)
}

/**
 * 生成分享链接
 */
export function shareItinerary(id) {
  return request.post(`/v1/itineraries/${id}/share`)
}

/**
 * 查看分享行程（公开）
 */
export function getSharedItinerary(code) {
  return request.get(`/v1/share/${code}`)
}

/**
 * 取消分享
 */
export function unshareItinerary(id) {
  return request.delete(`/v1/itineraries/${id}/share`)
}