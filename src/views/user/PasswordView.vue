<template>
  <div class="password-page">
    <el-row :gutter="24">
      <!-- 左侧菜单 -->
      <el-col :span="6">
        <UserSidebar active-menu="/user/password" />
      </el-col>

      <!-- 右侧内容 -->
      <el-col :span="18">
        <el-card class="password-card">
          <template #header>
            <h3>修改密码</h3>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="formRules"
            label-width="120px"
            size="large"
            style="max-width: 500px"
            @submit.prevent="handleChange"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="form.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                placeholder="请设置新密码（至少6位，包含字母和数字）"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
              >
                确认修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/user'
import UserSidebar from '@/components/common/UserSidebar.vue'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,}$/, message: '密码需包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleChange = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 检查新密码不能与旧密码相同
  if (form.oldPassword === form.newPassword) {
    ElMessage.warning('新密码不能与当前密码相同')
    return
  }

  loading.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })

    ElMessage.success('密码修改成功')
    resetForm()
  } catch (error) {
    // 错误已处理
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.password-page {
  padding: 10px 0;
}

.password-card {
  min-height: 400px;
}

.password-card h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
</style>
