<template>
  <div class="orders-page">
    <el-row :gutter="24">
      <el-col :span="6">
        <UserSidebar active-menu="/user/orders" />
      </el-col>

      <el-col :span="18">
        <el-card class="orders-card">
          <template #header>
            <div class="card-header">
              <h3>我的订单</h3>
              <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="PENDING">待支付</el-radio-button>
                <el-radio-button value="PAID">待开盒</el-radio-button>
                <el-radio-button value="OPENED">已开盒</el-radio-button>
                <el-radio-button value="CANCELLED">已取消</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div v-loading="loading">
            <div v-if="orders.length > 0" class="order-list">
              <div v-for="order in orders" :key="order.orderNo" class="order-item">
                <div class="order-header">
                  <span class="order-no">订单号: {{ order.orderNo }}</span>
                  <el-tag :type="getStatusType(order.status)" size="small">
                    {{ getStatusName(order.status) }}
                  </el-tag>
                </div>
                <div class="order-body">
                  <div class="order-info">
                    <div class="info-row">
                      <span class="label">盲盒名称</span>
                      <span class="value">{{ order.templateName || '-' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="label">金额</span>
                      <span class="value price">¥{{ order.payAmount || order.price || '0.00' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="label">创建时间</span>
                      <span class="value">{{ formatDateTime(order.createdAt) }}</span>
                    </div>
                  </div>
                </div>
                <div class="order-actions">
                  <el-button
                    v-if="order.status === 'PENDING'"
                    type="primary"
                    size="small"
                    @click="goToPay(order.orderNo)"
                  >
                    去支付
                  </el-button>
                  <el-button
                    v-if="order.status === 'PAID'"
                    type="primary"
                    size="small"
                    @click="goToOpen(order.orderNo)"
                  >
                    去开盒
                  </el-button>
                  <el-button
                    v-if="order.status === 'OPENED'"
                    type="primary"
                    size="small"
                    plain
                    @click="goToResult(order.orderNo)"
                  >
                    查看结果
                  </el-button>
                  <el-button
                    v-if="order.status === 'PENDING'"
                    size="small"
                    plain
                    @click="handleCancel(order.orderNo)"
                  >
                    取消订单
                  </el-button>
                </div>
              </div>
            </div>

            <el-empty v-else-if="!loading" description="暂无订单" />

            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadOrders"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserSidebar from '@/components/common/UserSidebar.vue'
import { getBlindBoxOrders, cancelBlindBoxOrder } from '@/api/blindbox'

const router = useRouter()

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

const statusMap = {
  PENDING: { name: '待支付', type: 'warning' },
  PAID: { name: '待开盒', type: 'primary' },
  OPENED: { name: '已开盒', type: 'success' },
  CANCELLED: { name: '已取消', type: 'info' },
  REFUNDED: { name: '已退款', type: 'danger' }
}

function getStatusName(status) {
  return statusMap[status]?.name || status
}

function getStatusType(status) {
  return statusMap[status]?.type || 'info'
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  if (Array.isArray(dateStr)) {
    const [y, m, d, h, min] = dateStr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h || 0).padStart(2, '0')}:${String(min || 0).padStart(2, '0')}`
  }
  return String(dateStr).replace('T', ' ').substring(0, 16)
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await getBlindBoxOrders({
      page: page.value,
      size: pageSize.value,
      status: statusFilter.value || undefined
    })
    orders.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载订单失败:', e)
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadOrders()
}

function goToPay(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/pay`)
}

function goToOpen(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/open`)
}

function goToResult(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/result`)
}

async function handleCancel(orderNo) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await cancelBlindBoxOrder(orderNo)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  padding: 10px 0;
}

.orders-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.order-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f2f5;
}

.order-no {
  font-size: 13px;
  color: #666;
  font-family: monospace;
}

.order-body {
  padding: 16px;
}

.order-info {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row .label {
  font-size: 12px;
  color: #999;
}

.info-row .value {
  font-size: 14px;
  color: #333;
}

.info-row .price {
  font-weight: 600;
  color: #ff6b6b;
}

.order-actions {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #f0f2f5;
  background: #fafafa;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
