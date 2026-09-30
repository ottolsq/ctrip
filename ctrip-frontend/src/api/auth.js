import request from '@/utils/request'

/**
 * 认证相关接口
 */

/**
 * 手机号注册
 */
export function registerByPhone(data) {
  return request.post('/v1/auth/register/phone', data)
}

/**
 * 邮箱注册
 */
export function registerByEmail(data) {
  return request.post('/v1/auth/register/email', data)
}

/**
 * 登录
 * @param {Object} data - { phone/email, password }
 */
export function login(data) {
  return request.post('/v1/auth/login', data)
}

/**
 * 刷新令牌
 */
export function refreshToken(refreshToken) {
  return request.post('/v1/auth/refresh', { refreshToken })
}

/**
 * 登出
 */
export function logout(refreshToken) {
  return request.post('/v1/auth/logout', { refreshToken })
}

/**
 * 发送短信验证码
 */
export function sendSmsCode(phone) {
  return request.post('/v1/auth/sms/send', { phone })
}

/**
 * 忘记密码
 */
export function forgotPassword(data) {
  return request.post('/v1/auth/password/forgot', data)
}

/**
 * 重置密码
 */
export function resetPassword(data) {
  return request.post('/v1/auth/password/reset', data)
}
