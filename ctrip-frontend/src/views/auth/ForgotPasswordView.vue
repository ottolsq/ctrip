<template>
  <div class="forgot-page">
    <div class="forgot-card">
      <div class="forgot-header">
        <h2>找回密码</h2>
        <p>通过手机号验证重置密码</p>
      </div>

      <!-- 步骤条 -->
      <el-steps :active="step" align-center class="steps">
        <el-step title="验证手机" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        size="large"
        @submit.prevent
      >
        <!-- 步骤1: 验证手机 -->
        <template v-if="step === 0">
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入注册时的手机号"
              :prefix-icon="Phone"
              clearable
            />
          </el-form-item>

          <el-form-item prop="verifyCode">
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

          <el-form-item>
            <el-button
              type="primary"
              @click="handleVerify"
              :loading="verifyLoading"
              class="action-btn"
            >
              下一步
            </el-button>
          </el-form-item>
        </template>

        <!-- 步骤2: 重置密码 -->
        <template v-if="step === 1">
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请设置新密码（至少6位）"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleReset"
              :loading="resetLoading"
              class="action-btn"
            >
              重置密码
            </el-button>
          </el-form-item>
        </template>

        <!-- 步骤3: 完成 -->
        <template v-if="step === 2">
          <div class="success-state">
            <el-icon :size="64" color="#67c23a"><CircleCheck /></el-icon>
            <h3>密码重置成功</h3>
            <p>请使用新密码登录</p>
            <el-button type="primary" @click="router.push('/login')">
              去登录
            </el-button>
          </div>
        </template>
      </el-form>

      <div class="forgot-footer">
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { sendSmsCode, forgotPassword, resetPassword } from '@/api/auth'
import { Phone, Lock, Key, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const step = ref(0)
const smsLoading = ref(false)
const smsCountdown = ref(0)
const verifyLoading = ref(false)
const resetLoading = ref(false)
let countdownTimer = null

const form = reactive({
  phone: '',
  verifyCode: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules = computed(() => {
  if (step.value === 0) {
    return {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
      ],
      verifyCode: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
      ]
    }
  }
  return {
    password: [
      { required: true, message: '请设置新密码', trigger: 'blur' },
      { min: 6, message: '密码至少6个字符', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }
})

// 发送验证码
const handleSendCode = async () => {
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }

  smsLoading.value = true
  try {
    await sendSmsCode(form.phone)
    ElMessage.success('验证码已发送')
    smsCountdown.value = 60
    countdownTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch (error) {
    // 错误已处理
  } finally {
    smsLoading.value = false
  }
}

// 验证手机
const handleVerify = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  verifyLoading.value = true
  try {
    await forgotPassword({
      phone: form.phone,
      verifyCode: form.verifyCode
    })
    ElMessage.success('验证通过')
    step.value = 1
  } catch (error) {
    // 错误已处理
  } finally {
    verifyLoading.value = false
  }
}

// 重置密码
const handleReset = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  resetLoading.value = true
  try {
    await resetPassword({
      phone: form.phone,
      verifyCode: form.verifyCode,
      newPassword: form.password
    })
    ElMessage.success('密码重置成功')
    step.value = 2
  } catch (error) {
    // 错误已处理
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 20px;
}

.forgot-card {
  width: 420px;
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.forgot-header {
  text-align: center;
  margin-bottom: 24px;
}

.forgot-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.forgot-header p {
  font-size: 14px;
  color: #999;
}

.steps {
  margin-bottom: 32px;
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

.action-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.success-state {
  text-align: center;
  padding: 20px 0;
}

.success-state h3 {
  font-size: 18px;
  margin: 16px 0 8px;
  color: #333;
}

.success-state p {
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

.forgot-footer {
  text-align: center;
  margin-top: 16px;
}

.forgot-footer a {
  font-size: 14px;
  color: #409eff;
}
</style>
