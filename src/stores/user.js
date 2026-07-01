import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage } from '@/utils/storage'
import { getUserProfile } from '@/api/user'
import { logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref(storage.getUserInfo())
  const token = ref(storage.getToken())
  const refreshToken = ref(storage.getRefreshToken())

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatarUrl || '')
  const roles = computed(() => userInfo.value?.roles || [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  // 设置 Token
  function setToken(newToken) {
    token.value = newToken
    storage.setToken(newToken)
  }

  // 设置刷新 Token
  function setRefreshToken(newToken) {
    refreshToken.value = newToken
    storage.setRefreshToken(newToken)
  }

  // 设置用户信息
  function setUserInfo(info) {
    userInfo.value = info
    storage.setUserInfo(info)
  }

  // 登录成功后的处理
  function loginSuccess(data) {
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    if (data.user) {
      setUserInfo(data.user)
    }
  }

  // 获取用户资料
  async function fetchUserProfile() {
    try {
      const res = await getUserProfile()
      setUserInfo(res.data)
      return res.data
    } catch (error) {
      console.error('获取用户资料失败:', error)
      throw error
    }
  }

  // 登出
  async function logout() {
    try {
      await logoutApi(refreshToken.value)
    } catch (error) {
      console.error('登出接口调用失败:', error)
    } finally {
      resetState()
    }
  }

  // 初始化用户信息（应用启动时调用）
  async function init() {
    if (token.value && !userInfo.value) {
      try {
        await fetchUserProfile()
      } catch (error) {
        console.error('初始化用户信息失败:', error)
        // 如果获取失败，可能是 token 已过期，清除登录状态
        resetState()
      }
    }
  }

  // 重置状态
  function resetState() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    storage.clearTokens()
    storage.clearUserInfo()
  }

  return {
    userInfo,
    token,
    refreshToken,
    isLoggedIn,
    userId,
    username,
    avatar,
    roles,
    isAdmin,
    setToken,
    setRefreshToken,
    setUserInfo,
    loginSuccess,
    fetchUserProfile,
    init,
    logout,
    resetState
  }
})
