import request from '@/utils/request'

/**
 * 平台数据概览接口。
 */

export function getDashboard() {
  return request.get('/v1/admin/dashboard')
}
