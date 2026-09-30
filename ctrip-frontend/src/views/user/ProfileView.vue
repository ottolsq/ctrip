<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左侧菜单 -->
      <el-col :span="6">
        <UserSidebar active-menu="/user/profile" />
      </el-col>

      <!-- 右侧内容 -->
      <el-col :span="18">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <h3>个人资料</h3>
              <el-button
                v-if="!isEditing"
                type="primary"
                plain
                size="small"
                @click="startEdit"
              >
                编辑
              </el-button>
              <div v-else>
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button type="primary" size="small" @click="handleSave" :loading="saveLoading">
                  保存
                </el-button>
              </div>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="formRules"
            label-width="100px"
            :disabled="!isEditing"
            size="large"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input :model-value="userStore.userInfo?.phone || '未绑定'" disabled />
            </el-form-item>

            <el-form-item label="邮箱">
              <el-input :model-value="userStore.userInfo?.email || '未绑定'" disabled />
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio value="MALE">男</el-radio>
                <el-radio value="FEMALE">女</el-radio>
                <el-radio value="UNSPECIFIED">保密</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="生日" prop="birthday">
              <el-date-picker
                v-model="form.birthday"
                type="date"
                placeholder="选择生日"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <!-- <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="form.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己吧"
                maxlength="200"
                show-word-limit
              />
            </el-form-item> -->
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserProfile, updateAvatar } from '@/api/user'
import UserSidebar from '@/components/common/UserSidebar.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const formRef = ref(null)
const isEditing = ref(false)
const saveLoading = ref(false)

const form = reactive({
  username: '',
  gender: 'UNSPECIFIED',
  birthday: '',
  // bio: ''
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ]
}

// 加载用户资料
const loadProfile = () => {
  const info = userStore.userInfo
  if (info) {
    form.username = info.username || ''
    form.gender = info.gender || 'UNSPECIFIED'
    form.birthday = info.birthday || ''
    // form.bio = info.bio || ''
  }
}

// 开始编辑
const startEdit = () => {
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  loadProfile()
}

// 保存资料
const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saveLoading.value = true
  try {
    await updateUserProfile({
      username: form.username,
      gender: form.gender,
      birthday: form.birthday,
      // bio: form.bio
    })

    // 更新 store
    await userStore.fetchUserProfile()
    ElMessage.success('资料已保存')
    isEditing.value = false
  } catch (error) {
    // 错误已处理
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  padding: 10px 0;
}

.profile-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
</style>
