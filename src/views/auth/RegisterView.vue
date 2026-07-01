<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h2>创建账号</h2>
        <p>加入捷程旅行网，探索精彩世界</p>
      </div>

      <!-- 注册方式切换 -->
      <el-tabs v-model="registerType" class="register-tabs">
        <el-tab-pane label="手机号注册" name="phone" />
        <el-tab-pane label="邮箱注册" name="email" />
      </el-tabs>

      <el-form
        ref="formRef"
        :model="form"
        :rules="currentRules"
        size="large"
        :key="registerType"
        @submit.prevent="handleRegister"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item v-if="registerType === 'phone'" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item v-if="registerType === 'email'" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item v-if="registerType === 'phone'" prop="verifyCode">
          <div class="code-input">
            <el-input
              v-model="form.verifyCode"
              placeholder="6位验证码"
              :prefix-icon="Key"
              clearable
            />
            <el-button
              type="primary"
              plain
              :disabled="smsCountdown > 0"
              :loading="smsLoading"
              @click="handleSendCode"
              class="sms-btn"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请设置密码（至少6位，包含字母和数字）"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <!-- 用户协议 -->
        <el-form-item>
          <el-checkbox v-model="agreeTerms">
            <span>我已阅读并同意</span>
            <a href="#" @click.prevent>《用户协议》</a>
            <span>和</span>
            <a href="#" @click.prevent>《隐私政策》</a>
          </el-checkbox>
        </el-form-item>

        <!-- 注册按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="!agreeTerms"
            class="register-btn"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { registerByPhone, registerByEmail, sendSmsCode } from '@/api/auth'
import { User, Phone, Message, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const registerType = ref('phone')
const loading = ref(false)
const agreeTerms = ref(false)
const smsLoading = ref(false)
const smsCountdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  phone: '',
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: ''
})

// 确认密码校验
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const currentRules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ],
  phone: registerType.value === 'phone' ? [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ] : [],
  email: registerType.value === 'email' ? [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ] : [],
  verifyCode: registerType.value === 'phone' ? [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ] : [],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,}$/, message: '密码需包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}))

// 发送验证码
const handleSendCode = async () => {
  if (!form.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }

  smsLoading.value = true
  try {
    await sendSmsCode(form.phone)
    ElMessage.success('验证码已发送')

    // 开始倒计时
    smsCountdown.value = 60
    countdownTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (error) {
    // 错误已由 request.js 处理
  } finally {
    smsLoading.value = false
  }
}

// 注册
const handleRegister = async () => {
  console.log('[Register] handleRegister called, registerType:', registerType.value)
  console.log('[Register] form:', JSON.stringify(form))
  
  try {
    const valid = await formRef.value.validate()
    console.log('[Register] validate result:', valid)
  } catch (err) {
    console.error('[Register] validate error:', err)
    return
  }

  loading.value = true
  console.log('[Register] sending request...')
  try {
    let res
    if (registerType.value === 'phone') {
      res = await registerByPhone({
        username: form.username,
        phone: form.phone,
        password: form.password,
        smsCode: form.verifyCode
      })
    } else {
      res = await registerByEmail({
        username: form.username,
        email: form.email,
        password: form.password
      })
    }
    console.log('[Register] response:', res)

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('[Register] request error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 20px;
}

.register-card {
  width: 420px;
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.register-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.register-header p {
  font-size: 14px;
  color: #999;
}

.register-tabs {
  margin-bottom: 8px;
}

.code-input {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input .el-input {
  flex: 1;
}

.sms-btn {
  width: 120px;
  flex-shrink: 0;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #999;
}

.register-footer a {
  color: #409eff;
  font-weight: 500;
}
</style>
