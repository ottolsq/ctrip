import request from '@/utils/request'

/**
 * 用户相关接口
 */

/**
 * 获取个人资料
 */
export function getUserProfile() {
  return request.get('/v1/users/me')
}

/**
 * 更新个人资料
 */
export function updateUserProfile(data) {
  return request.put('/v1/users/me', data)
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request.put('/v1/users/me/password', data)
}

/**
 * 更新头像
 * @param {FormData} formData - 包含 avatar 文件的 FormData
 */
export function updateAvatar(formData) {
  return request.put('/v1/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
