<template>
  <div class="content-stats" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">攻略总数</div>
          <div class="stat-value">{{ stats.totalGuides || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待审核</div>
          <div class="stat-value warning">{{ stats.pendingCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已发布</div>
          <div class="stat-value success">{{ stats.publishedCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总浏览</div>
          <div class="stat-value">{{ stats.totalViews || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总点赞</div>
          <div class="stat-value">{{ stats.totalLikes || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">评论总数</div>
          <div class="stat-value">{{ stats.totalComments || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="list-card">
          <template #header><h3>浏览量 Top 5</h3></template>
          <el-table :data="stats.topGuides || []" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column prop="likeCount" label="点赞" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="list-card">
          <template #header><h3>最近 7 天新增攻略</h3></template>
          <el-table :data="stats.last7Days || []" style="width: 100%">
            <el-table-column prop="date" label="日期" min-width="140" />
            <el-table-column prop="count" label="新增数量" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getContentStats } from '@/api/admin/content'

const loading = ref(false)
const stats = ref({})

async function loadStats() {
  loading.value = true
  try {
    const res = await getContentStats()
    stats.value = res.data || {}
  } catch {
    stats.value = {}
    ElMessage.error('加载内容数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.stat-row {
  margin-bottom: 8px;
}

.stat-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.success {
  color: #67c23a;
}

.list-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.list-card h3 {
  margin: 0;
  font-size: 16px;
}
</style>
