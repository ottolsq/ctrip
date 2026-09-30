import request from '@/utils/request'

/**
 * 上传相关接口
 */

/**
 * 上传单张图片
 */
export function uploadImage(formData) {
  return request.post('/v1/uploads/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量上传图片
 */
export function uploadImages(formData) {
  return request.post('/v1/uploads/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除图片
 */
export function deleteImage(filename) {
  return request.delete(`/v1/uploads/${filename}`)
}