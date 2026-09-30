<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h2>欢迎回来</h2>
        <p>登录捷程旅行网，开启你的旅程</p>
      </div>

      <!-- 登录方式切换 -->
      <el-tabs v-model="loginType" class="login-tabs">
        <el-tab-pane label="手机号登录" name="phone" />
        <el-tab-pane label="邮箱登录" name="email" />
      </el-tabs>

      <el-form
        ref="formRef"
        :model="form"
        :rules="currentRules"
        size="large"
        :key="loginType"
        @submit.prevent="handleLogin"
      >
        <!-- 手机号 -->
        <el-form-item v-if="loginType === 'phone'" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item v-if="loginType === 'email'" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <!-- 记住 & 忘记 -->
        <div class="login-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
        </div>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="login-btn"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { Phone, Message, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loginType = ref('phone')
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  phone: '',
  email: '',
  password: ''
})

const currentRules = computed(() => ({
  phone: loginType.value === 'phone' ? [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ] : [],
  email: loginType.value === 'email' ? [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ] : [],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}))

const handleLogin = async () => {
  console.log('[Login] handleLogin called, loginType:', loginType.value)
  console.log('[Login] form:', JSON.stringify(form))
  
  try {
    const valid = await formRef.value.validate()
    console.log('[Login] validate result:', valid)
  } catch (err) {
    console.error('[Login] validate error:', err)
    return
  }

  loading.value = true
  console.log('[Login] sending request...')
  try {
    const data = { password: form.password }
    if (loginType.value === 'phone') {
      data.credential = form.phone
    } else {
      data.credential = form.email
    }
    console.log('[Login] request data:', data)

    const res = await login(data)
    console.log('[Login] response:', res)
    
    const loginData = res.data || res
    console.log('[Login] loginData:', loginData)
    
    userStore.loginSuccess(loginData)

    // 获取用户信息
    try {
      await userStore.fetchUserProfile()
    } catch (error) {
      console.error('[Login] 获取用户信息失败:', error)
    }

    ElMessage.success('登录成功')

    // 跳转到来源页或首页
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    console.error('[Login] request error:', error)
    // 错误已由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 20px;
}

.login-card {
  width: 420px;
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #999;
}

.login-tabs {
  margin-bottom: 8px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forgot-link {
  font-size: 13px;
  color: #409eff;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #999;
}

.login-footer a {
  color: #409eff;
  font-weight: 500;
}
</style>
