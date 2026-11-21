<template>
  <div class="forbidden-page">
    <el-result
      icon="warning"
      title="403"
      sub-title="你没有访问该页面的权限"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
        <el-button v-if="!userStore.isLoggedIn" @click="router.push('/login')">去登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<style scoped>
.forbidden-page {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}
</style>
