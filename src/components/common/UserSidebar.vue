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
        <!-- <div class="avatar-tip">点击更换头像</div> -->
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
      <!-- <el-menu-item index="/user/collections">
        <el-icon><Star /></el-icon>
        <span>我的收藏</span>
      </el-menu-item> -->
    </el-menu>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { updateAvatar } from '@/api/user'
import { uploadImage } from '@/api/upload'
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

  try {
    const formData = new FormData()
    formData.append('file', file)

    const uploadRes = await uploadImage(formData)
    const avatarUrl = uploadRes.data?.url || uploadRes.data

    await updateAvatar(avatarUrl)
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
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.user-avatar-section {
  text-align: center;
  padding: 40px 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.05) 0%, transparent 100%);
}

.user-avatar-section .el-avatar {
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.user-avatar-section .el-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.25);
}

.avatar-tip {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 10px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-top: 14px;
  color: var(--text-primary);
}

.user-menu {
  border-right: none;
}

.user-menu .el-menu-item {
  color: var(--text-secondary);
  font-size: 14px;
  transition: all 0.2s;
  margin: 4px 8px;
  border-radius: 8px;
}

.user-menu .el-menu-item:hover {
  background: rgba(64, 158, 255, 0.08);
  color: var(--primary-color);
}

.user-menu .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.12);
  color: var(--primary-color);
}
</style>
