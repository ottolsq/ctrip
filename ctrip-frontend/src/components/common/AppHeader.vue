<template>
  <header class="app-header">
    <div class="header-container">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <el-icon :size="24"><Compass /></el-icon>
        <span class="logo-text">趣行旅行平台</span>
      </router-link>

      <!-- 导航菜单 -->
      <nav class="nav-menu" aria-label="主导航">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/destinations" class="nav-item">目的地</router-link>
        <router-link to="/guides" class="nav-item">攻略</router-link>
        <router-link to="/itineraries" class="nav-item">行程</router-link>
        <router-link to="/blind-box" class="nav-item blind-box-entry">
          <el-icon><Present /></el-icon>
          盲盒
        </router-link>
      </nav>

      <!-- 右侧操作区 -->
      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatar">
                <el-icon :size="18"><User /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><List /></el-icon>我的订单
                </el-dropdown-item>
                <el-dropdown-item command="guides">
                  <el-icon><Document /></el-icon>我的攻略
                </el-dropdown-item>
                <!-- <el-dropdown-item command="collections">
                  <el-icon><Star /></el-icon>我的收藏
                </el-dropdown-item> -->
                <el-dropdown-item v-if="userStore.canAccessAdmin" divided command="admin">
                  <el-icon><Setting /></el-icon>后台管理
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="router.push('/login')">
            登录
          </el-button>
          <el-button size="small" @click="router.push('/register')">
            注册
          </el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'orders':
      router.push('/user/orders')
      break
    case 'guides':
      router.push('/user/guides')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'collections':
      router.push('/user/collections')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        router.push('/')
      } catch {
        // 用户取消
      }
      break
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #174a5a;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-item {
  text-decoration: none;
  color: #333;
  font-size: 14px;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.nav-item:hover,
.nav-item.router-link-exact-active {
  color: #174a5a;
  border-bottom-color: #174a5a;
}

.blind-box-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #83501a;
}

.blind-box-entry:hover,
.blind-box-entry.router-link-exact-active {
  color: #83501a;
  border-bottom-color: #83501a;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.app-header :is(a, button, [tabindex]):focus-visible { outline: 2px solid #206681; outline-offset: 2px; }

@media (max-width: 840px) {
  .header-container { display: grid; grid-template-columns: minmax(0, 1fr) auto; height: auto; padding: 8px 16px 0; }
  .header-actions { justify-self: end; gap: 4px; }
  .nav-menu { grid-row: 2; grid-column: 1 / -1; justify-content: space-between; gap: 4px; }
  .nav-item { display: flex; align-items: center; justify-content: center; flex: 1; min-height: 44px; white-space: nowrap; }
  .logo-text, .username { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .username { max-width: 20vw; }
}
</style>
