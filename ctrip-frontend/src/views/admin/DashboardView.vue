<template>
  <div class="admin-dashboard">
    <el-alert
      title="P1.1 后台基础已就绪"
      description="用户管理与角色守卫已启用；内容审核、订单/退款与运营数据将在后续阶段开放。"
      type="success"
      show-icon
      :closable="false"
      class="stage-alert"
    />

    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">用户管理</div>
          <div class="stat-value">已启用</div>
          <div class="stat-note">列表 / 筛选 / 封禁 / 解封</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">内容审核</div>
          <div class="stat-value muted">P1.2</div>
          <div class="stat-note">攻略审核 / 评论管理</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">订单与退款</div>
          <div class="stat-value muted">P1.3</div>
          <div class="stat-note">模板规则 / 退款 / 异常重试</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">运营管理</div>
          <div class="stat-value muted">P1.4</div>
          <div class="stat-note">轮播图 / 推荐位 / 数据概览</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="welcome-card">
      <h3>欢迎，{{ userStore.username }}</h3>
      <p>
        当前角色：{{ userStore.isAdmin ? '系统管理员' : '内容运营' }}。
        <template v-if="!userStore.isAdmin">
          内容运营功能将在 P1.2 开放。
        </template>
      </p>
      <p class="hint">左侧菜单会根据角色显示可访问的功能。</p>
    </el-card>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.stage-alert {
  margin-bottom: 20px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #67c23a;
}

.stat-value.muted {
  color: #c0c4cc;
}

.stat-note {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.welcome-card {
  border-radius: 10px;
}

.welcome-card h3 {
  margin: 0 0 12px;
}

.welcome-card p {
  margin: 0 0 8px;
  color: #606266;
}

.hint {
  color: #909399;
  font-size: 13px;
}
</style>
