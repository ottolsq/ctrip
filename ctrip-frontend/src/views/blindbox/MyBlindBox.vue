<template>
  <div class="my-blind-box">
    <div class="page-header">
      <h1>我的盲盒</h1>
    </div>

    <el-tabs v-model="activeTab" class="order-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="待支付" name="PENDING" />
      <el-tab-pane label="待开盒" name="PAID" />
      <el-tab-pane label="已开盒" name="OPENED" />
    </el-tabs>

    <div v-if="loading" class="loading-state">
      <el-spin />
    </div>

    <div v-else-if="list.length === 0" class="empty-state">
      <el-empty description="暂无订单" />
    </div>

    <el-row :gutter="20" v-else>
      <el-col v-for="item in list" :key="item.orderNo" :xs="24" :sm="12" :md="8">
        <el-card class="order-card" shadow="hover">
          <div class="card-header">
            <h3 class="blind-box-name">{{ item.templateName || '未知盲盒' }}</h3>
            <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
          </div>
          <div class="card-info">
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value order-no">{{ item.orderNo }}</span>
            </div>
            <div class="info-item">
              <span class="label">价格：</span>
              <span class="value price">¥{{ item.payAmount?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="card-actions">
            <template v-if="item.status === 'PENDING'">
              <el-button size="small" type="danger" @click="handleCancel(item.orderNo)">取消订单</el-button>
              <el-button size="small" type="primary" @click="goPay(item.orderNo)">去支付</el-button>
            </template>
            <template v-else-if="item.status === 'PAID'">
              <el-button size="small" type="primary" @click="goOpen(item.orderNo)">去开盒</el-button>
            </template>
            <template v-else-if="item.status === 'OPENED'">
              <el-button size="small" type="primary" @click="goResult(item.orderNo)">查看结果</el-button>
            </template>
            <template v-else-if="item.status === 'CANCELLED'">
              <el-button size="small" disabled>已取消</el-button>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBlindBoxOrders, cancelBlindBoxOrder } from '@/api/blindbox'

const router = useRouter()

const activeTab = ref('ALL')
const loading = ref(false)
const list = ref([])
const total = ref(0)

const pagination = ref({
  current: 1,
  pageSize: 12
})

const statusMap = {
  PENDING: '待支付',
  PAID: '待开盒',
  OPENED: '已开盒',
  REFUNDED: '已退款',
  CANCELLED: '已取消'
}

const statusTypeMap = {
  PENDING: 'warning',
  PAID: 'primary',
  OPENED: 'success',
  REFUNDED: 'info',
  CANCELLED: 'info'
}

function getStatusText(status) {
  return statusMap[status] || '未知状态'
}

function getStatusType(status) {
  return statusTypeMap[status] || 'info'
}

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function handleTabChange() {
  pagination.value.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.current,
      limit: pagination.value.pageSize
    }
    if (activeTab.value !== 'ALL') {
      params.status = activeTab.value
    }
    const res = await getBlindBoxOrders(params)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goPay(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/pay`)
}

function goOpen(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/open`)
}

function goResult(orderNo) {
  router.push(`/blind-box/orders/${orderNo}/result`)
}

async function handleCancel(orderNo) {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelBlindBoxOrder(orderNo)
    ElMessage.success('取消成功')
    loadData()
  } catch {
    // 用户取消或操作失败
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-blind-box {
  padding: 0 10px;

  .page-header {
    margin-bottom: 16px;

    h1 {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #303133;
    }
  }

  .order-tabs {
    margin-bottom: 20px;
  }

  .loading-state, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 0;
  }

  .order-card {
    transition: all 0.3s;
    height: 100%;
    display: flex;
    flex-direction: column;
    margin-bottom: 20px;
    border-radius: 12px;
    overflow: hidden;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 12px;

    .blind-box-name {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      margin-right: 8px;
    }
  }

  .card-info {
    margin-bottom: 12px;
    flex: 1;
  }

  .info-item {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;

    .label {
      color: #909399;
      margin-right: 4px;
    }

    .value {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .order-no {
      font-family: monospace;
      font-size: 12px;
    }

    .price {
      color: #f56c6c;
      font-weight: 600;
    }
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 32px;
    padding-bottom: 24px;
  }
}
</style>
