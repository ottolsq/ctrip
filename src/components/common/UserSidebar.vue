<template>
  <div class="user-sidebar">
    <div class="user-avatar-section">
      <el-upload
        action=""
        :show-file-list="false"
        :before-upload="handleAvatarUpload"
        accept="image/*"
      >
        <el-avatar :size="80" :src="userStore.avatar">
          <el-icon :size="36"><User /></el-icon>
        </el-avatar>
        <div class="avatar-tip">点击更换头像</div>
      </el-upload>
      <h3 class="user-name">{{ userStore.username }}</h3>
    </div>
    <el-menu :default-active="activeMenu" router class="user-menu">
      <el-menu-item index="/user/profile">
        <el-icon><User /></el-icon>
        <span>个人资料</span>
      </el-menu-item>
      <el-menu-item index="/user/password">
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </el-menu-item>
      <el-menu-item index="/user/orders">
        <el-icon><List /></el-icon>
        <span>我的订单</span>
      </el-menu-item>
      <el-menu-item index="/user/guides">
        <el-icon><Document /></el-icon>
        <span>我的攻略</span>
      </el-menu-item>
      <el-menu-item index="/user/collections">
        <el-icon><Star /></el-icon>
        <span>我的收藏</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { updateAvatar } from '@/api/user'
import { ElMessage } from 'element-plus'

defineProps({
  activeMenu: { type: String, default: '/user/profile' }
})

const userStore = useUserStore()

const handleAvatarUpload = async (file) => {
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  const formData = new FormData()
  formData.append('file', file)
  try {
    await updateAvatar(formData)
    await userStore.fetchUserProfile()
    ElMessage.success('头像已更新')
  } catch (error) {
    // 错误已处理
  }
  return false
}
</script>

<style scoped>
.user-sidebar {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.user-avatar-section {
  text-align: center;
  padding: 32px 20px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.user-avatar-section .el-avatar {
  cursor: pointer;
  transition: opacity 0.2s;
}

.user-avatar-section .el-avatar:hover {
  opacity: 0.8;
}

.avatar-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  margin-top: 12px;
  color: #333;
}

.user-menu {
  border-right: none;
}
</style>
