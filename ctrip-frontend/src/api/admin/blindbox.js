import request from '@/utils/request'

/**
 * 管理端盲盒模板接口。
 */

export function getAdminTemplates(params) {
  return request.get('/v1/admin/blind-box/templates', { params })
}

export function createTemplate(data) {
  return request.post('/v1/admin/blind-box/templates', data)
}

export function updateTemplate(id, data) {
  return request.put(`/v1/admin/blind-box/templates/${id}`, data)
}

export function deleteTemplate(id) {
  return request.delete(`/v1/admin/blind-box/templates/${id}`)
}

/**
 * 管理端订单接口。
 */

export function getAdminOrders(params) {
  return request.get('/v1/admin/blind-box/orders', { params })
}

export function getAdminOrder(orderNo) {
  return request.get(`/v1/admin/blind-box/orders/${encodeURIComponent(orderNo)}`)
}

export function refundOrder(orderNo, reason) {
  return request.post(`/v1/admin/blind-box/orders/${encodeURIComponent(orderNo)}/refund`, { reason })
}

export function retryOpenOrder(orderNo) {
  return request.post(`/v1/admin/blind-box/orders/${encodeURIComponent(orderNo)}/retry-open`)
}
