<template>
  <div class="admin-dashboard">
    <el-card shadow="never" class="welcome-card">
      <h3>欢迎，{{ userStore.username }}</h3>
      <p>平台运营数据实时汇总，数据来源于用户、攻略、订单与运营内容表。</p>
    </el-card>

    <div v-loading="loading">
      <template v-if="dashboard">
        <el-row :gutter="20" class="stat-row">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ formatNumber(dashboard.totalUsers) }}</div>
              <div class="stat-note">今日新增 {{ formatNumber(dashboard.todayNewUsers) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">攻略总数</div>
              <div class="stat-value">{{ formatNumber(dashboard.totalGuides) }}</div>
              <div class="stat-note">
                待审核 {{ formatNumber(dashboard.pendingGuides) }} ·
                已发布 {{ formatNumber(dashboard.publishedGuides) }}
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">订单总数</div>
              <div class="stat-value">{{ formatNumber(dashboard.totalOrders) }}</div>
              <div class="stat-note">总销售额 ¥{{ formatAmount(dashboard.totalSales) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">运营内容</div>
              <div class="stat-value">
                {{ formatNumber(dashboard.bannerCount + dashboard.recommendationCount) }}
              </div>
              <div class="stat-note">
                轮播图 {{ formatNumber(dashboard.bannerCount) }} ·
                推荐位 {{ formatNumber(dashboard.recommendationCount) }}
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" class="section-card">
          <template #header><span>订单状态</span></template>
          <div class="status-grid">
            <div v-for="item in orderStatusItems" :key="item.label" class="status-item">
              <el-tag :type="item.type" size="small">{{ item.label }}</el-tag>
              <span class="status-count">{{ formatNumber(item.value) }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="section-card">
          <template #header><span>最近 7 天新增</span></template>
          <el-table :data="dashboard.last7Days" style="width: 100%">
            <el-table-column prop="date" label="日期" min-width="140" />
            <el-table-column label="新增用户" min-width="140">
              <template #default="{ row }">{{ formatNumber(row.newUsers) }}</template>
            </el-table-column>
            <el-table-column label="新增订单" min-width="140">
              <template #default="{ row }">{{ formatNumber(row.newOrders) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>

      <el-empty v-else-if="!loading" description="数据加载失败" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboard } from '@/api/admin/dashboard'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const dashboard = ref(null)

const orderStatusItems = computed(() => {
  const data = dashboard.value
  if (!data) return []
  return [
    { label: '待支付', value: data.pendingOrders, type: 'info' },
    { label: '已支付', value: data.paidOrders, type: 'primary' },
    { label: '生成中', value: data.processingOrders, type: 'warning' },
    { label: '已开盒', value: data.openedOrders, type: 'success' },
    { label: '已退款', value: data.refundedOrders, type: 'danger' },
    { label: '已取消', value: data.cancelledOrders, type: 'info' }
  ]
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatAmount(value) {
  return Number(value || 0).toFixed(2)
}

async function loadDashboard() {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data
  } catch {
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.welcome-card {
  border-radius: 10px;
  margin-bottom: 20px;
}

.welcome-card h3 {
  margin: 0 0 8px;
}

.welcome-card p {
  margin: 0;
  color: #606266;
}

.stat-row {
  margin-bottom: 0;
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
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
}

.stat-note {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.section-card {
  border-radius: 10px;
  margin-top: 4px;
  margin-bottom: 16px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.status-count {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>
