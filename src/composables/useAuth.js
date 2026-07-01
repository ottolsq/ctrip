import { useUserStore } from '@/stores/user'
import { login as loginApi, registerByPhone, registerByEmail, sendSmsCode } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 认证相关组合式函数
 */
export function useAuth() {
  const userStore = useUserStore()

  /**
   * 登录
   */
  async function handleLogin(form) {
    const res = await loginApi(form)
    userStore.loginSuccess(res.data)
    ElMessage.success('登录成功')
    return res
  }

  /**
   * 手机号注册
   */
  async function handleRegisterByPhone(data) {
    await registerByPhone(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  }

  /**
   * 邮箱注册
   */
  async function handleRegisterByEmail(data) {
    await registerByEmail(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  }

  /**
   * 发送验证码
   */
  async function handleSendSmsCode(phone) {
    await sendSmsCode(phone)
    ElMessage.success('验证码已发送')
  }

  /**
   * 登出
   */
  async function handleLogout() {
    await userStore.logout()
    router.push('/')
  }

  return {
    handleLogin,
    handleRegisterByPhone,
    handleRegisterByEmail,
    handleSendSmsCode,
    handleLogout
  }
}