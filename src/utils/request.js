import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 是否正在刷新 Token
let isRefreshing = false
// 刷新 Token 期间的请求队列
let requestQueue = []

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
request.interceptors.response.use(
  response => {
    const res = response.data
    // 后端统一返回格式: { success, data, error }
    if (res.success !== undefined) {
      if (res.success) {
        return res
      } else {
        // 业务错误
        ElMessage.error(res.error || '操作失败')
        return Promise.reject(new Error(res.error || '操作失败'))
      }
    }
    // 非标准格式，直接返回
    return res
  },
  async error => {
    const { response, config } = error

    if (!response) {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    const { status } = response

    // 401: Token 过期或未登录
    if (status === 401 && !config._isRetry) {
      const refreshToken = localStorage.getItem('refreshToken')

      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          // 尝试刷新 Token
          const res = await axios.post('/api/v1/auth/refresh', {
            refreshToken
          })
          const { accessToken, refreshToken: newRefreshToken } = res.data.data

          localStorage.setItem('token', accessToken)
          localStorage.setItem('refreshToken', newRefreshToken)

          // 重试队列中的请求
          config._isRetry = true
          config.headers.Authorization = `Bearer ${accessToken}`
          requestQueue.forEach(cb => cb(accessToken))
          requestQueue = []

          return request(config)
        } catch (e) {
          // 刷新失败，清除 Token 并跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          requestQueue = []
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
          return Promise.reject(e)
        } finally {
          isRefreshing = false
        }
      } else if (isRefreshing) {
        // 正在刷新，加入队列等待
        return new Promise(resolve => {
          requestQueue.push(token => {
            config.headers.Authorization = `Bearer ${token}`
            config._isRetry = true
            resolve(request(config))
          })
        })
      } else {
        // 没有 refreshToken，直接跳转登录
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        ElMessage.error('请先登录')
        router.push('/login')
      }
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (status === 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      const msg = response.data?.error || response.data?.message || '请求失败'
      ElMessage.error(msg)
    }

    return Promise.reject(error)
  }
)

export default request
