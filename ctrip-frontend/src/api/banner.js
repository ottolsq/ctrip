import request from '@/utils/request'

/**
 * 公开轮播图接口。
 */

export function getPublicBanners() {
  return request.get('/v1/banners')
}
