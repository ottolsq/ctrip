import request from '@/utils/request'

/**
 * 公开推荐位接口。
 */

export function getPublicRecommendations(slot = 'home_featured') {
  return request.get('/v1/recommendations', { params: { slot } })
}
