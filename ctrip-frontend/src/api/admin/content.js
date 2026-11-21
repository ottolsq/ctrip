import request from '@/utils/request'

export function getContentStats() {
  return request.get('/v1/admin/content/stats')
}
