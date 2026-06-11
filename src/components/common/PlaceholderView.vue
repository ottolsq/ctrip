<template>
  <div class="page-container">
    <div class="page-card">
      <el-icon :size="48" color="#409eff"><Compass /></el-icon>
      <h2>{{ title }}</h2>
      <p class="placeholder-text">{{ description }}</p>
      <el-button type="primary" @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '页面' },
  description: { type: String, default: '即将开发' }
})
</script>

<style scoped>
.page-container { display: flex; justify-content: center; align-items: center; min-height: 60vh; }
.page-card { background: #fff; padding: 48px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); text-align: center; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.placeholder-text { color: #999; }
</style>
