import request from '@/utils/request'

/**
 * 盲盒相关接口
 */

/**
 * 盲盒模板列表
 */
export function getBlindBoxTemplates(params) {
  return request.get('/v1/blind-box', { params })
}

/**
 * 模板详情
 */
export function getBlindBoxTemplate(id) {
  return request.get(`/v1/blind-box/${id}`)
}

/**
 * 我的盲盒列表
 */
export function getMyBlindBoxes() {
  return request.get('/v1/blind-box/my')
}

/**
 * 创建订单
 */
export function createBlindBoxOrder(data) {
  return request.post('/v1/blind-box/orders', data)
}

/**
 * 我的订单列表
 */
export function getBlindBoxOrders(params) {
  return request.get('/v1/blind-box/orders', { params })
}

/**
 * 订单详情
 */
export function getBlindBoxOrder(orderNo) {
  return request.get(`/v1/blind-box/orders/${orderNo}`)
}

/**
 * 取消订单
 */
export function cancelBlindBoxOrder(orderNo) {
  return request.post(`/v1/blind-box/orders/${orderNo}/cancel`)
}

/**
 * 开盒
 */
export function openBlindBox(orderNo) {
  return request.post(`/v1/blind-box/orders/${orderNo}/open`)
}

/**
 * 结果详情
 */
export function getBlindBoxResult(orderNo) {
  return request.get(`/v1/blind-box/orders/${orderNo}/result`)
}

/**
 * 生成分享链接
 */
export function shareBlindBoxResult(orderNo) {
  return request.post(`/v1/blind-box/orders/${orderNo}/share-result`)
}

/**
 * 查看分享结果（公开）
 */
export function getSharedBlindBoxResult(code) {
  return request.get(`/v1/blind-box/share/${code}`)
}