import request from '@/utils/request'

/**
 * 管理端抽取记录接口。
 */

export function getAdminDrawRecords(params) {
  return request.get('/v1/admin/blind-box/draw-records', { params })
}
