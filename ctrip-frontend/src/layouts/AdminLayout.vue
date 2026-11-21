<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo" @click="router.push('/')">
        <el-icon :size="22"><Compass /></el-icon>
        <span>趣行后台</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="admin-menu"
        background-color="#1f2d3d"
        text-color="#c0c4cc"
        active-text-color="#409eff"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/guides">
          <el-icon><Document /></el-icon>
          <span>攻略审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/content-stats">
          <el-icon><DataLine /></el-icon>
          <span>内容数据</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/blind-box/templates">
          <el-icon><Box /></el-icon>
          <span>盲盒模板</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/blind-box/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/banners">
          <el-icon><Picture /></el-icon>
          <span>轮播图</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-title">{{ route.meta.title || '后台管理' }}</div>
        <div class="header-actions">
          <span class="admin-user">{{ userStore.username }}</span>
          <el-tag size="small" :type="userStore.isAdmin ? 'danger' : 'warning'">
            {{ roleLabel }}
          </el-tag>
          <el-button link @click="router.push('/')">返回前台</el-button>
          <el-button link type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roleLabel = computed(() => (userStore.isAdmin ? '管理员' : '内容运营'))

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.admin-layout {
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
}

.admin-aside {
  background: #1f2d3d;
  min-height: 100vh;
}

.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-menu {
  border-right: none;
}

.admin-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-user {
  color: #606266;
  font-size: 14px;
}

.admin-main {
  padding: 24px;
}
</style>
