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
