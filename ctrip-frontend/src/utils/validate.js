/**
 * 表单验证工具
 */

/**
 * 验证手机号
 */
export function isValidPhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 验证邮箱
 */
export function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

/**
 * 验证密码强度（至少6位，包含字母和数字）
 */
export function isValidPassword(password) {
  if (!password || password.length < 6) return false
  return /^(?=.*[A-Za-z])(?=.*\d).{6,}$/.test(password)
}

/**
 * 验证用户名（2-20个字符）
 */
export function isValidUsername(username) {
  if (!username) return false
  return username.length >= 2 && username.length <= 20
}

/**
 * 验证验证码（6位数字）
 */
export function isValidVerifyCode(code) {
  return /^\d{6}$/.test(code)
}

/**
 * Element Plus 表单验证规则
 */
export const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ]
}
