<template>
  <div class="order-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input
            v-model="filters.orderNo"
            placeholder="订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input
            v-model="filters.userId"
            placeholder="用户 ID"
            clearable
            style="width: 120px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="(label, value) in statusLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建日期">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            {{ row.userName || '-' }}
            <span class="muted">(ID: {{ row.userId }})</span>
          </template>
        </el-table-column>
        <el-table-column label="模板" min-width="150">
          <template #default="{ row }">{{ row.templateName || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ Number(row.payAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canRefund(row)"
              link
              type="danger"
              @click="openRefund(row)"
            >
              退款
            </el-button>
            <el-button
              v-if="row.status === 'PROCESSING'"
              link
              type="warning"
              @click="handleRetry(row)"
            >
              重试开盒
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadOrders"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.order.status)" size="small">
              {{ statusLabel(detail.order.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ detail.order.userName || '-' }} (ID: {{ detail.order.userId }})
          </el-descriptions-item>
          <el-descriptions-item label="模板">
            {{ detail.order.templateName || '-' }} (ID: {{ detail.order.templateId }})
          </el-descriptions-item>
          <el-descriptions-item label="金额">
            ¥{{ Number(detail.order.payAmount).toFixed(2) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detail.order.payMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDateTime(detail.order.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付截止">{{ formatDateTime(detail.order.expireAt) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.order.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="退款时间">{{ formatDateTime(detail.order.refundedAt) }}</el-descriptions-item>
          <el-descriptions-item label="退款原因" :span="2">
            {{ detail.order.refundReason || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="detail.result">
          <el-divider content-position="left">开盒结果</el-divider>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="目的地">{{ detail.result.destination }}</el-descriptions-item>
            <el-descriptions-item label="主题">{{ detail.result.theme }}</el-descriptions-item>
            <el-descriptions-item label="分享码">{{ detail.result.shareCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="开盒时间">{{ formatDateTime(detail.result.openedAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-collapse class="result-collapse">
            <el-collapse-item title="完整方案 JSON" name="result">
              <pre class="result-json">{{ prettyResultText }}</pre>
            </el-collapse-item>
          </el-collapse>
        </template>
        <el-empty v-else description="订单尚未开盒" :image-size="80" />
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="订单退款" width="480px">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="90px">
        <el-form-item label="订单号">
          <span>{{ refundTarget?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写退款原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refundSubmitting" @click="handleRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminOrders,
  getAdminOrder,
  refundOrder,
  retryOpenOrder
} from '@/api/admin/blindbox'

const loading = ref(false)
const orders = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const refundVisible = ref(false)
const refundSubmitting = ref(false)
const refundTarget = ref(null)
const refundFormRef = ref()
const refundForm = reactive({ reason: '' })

const filters = reactive({
  orderNo: '',
  userId: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statusLabels = {
  PENDING: '待支付',
  PAID: '已支付',
  PROCESSING: '生成中',
  OPENED: '已开盒',
  REFUNDED: '已退款',
  CANCELLED: '已取消'
}

const refundRules = {
  reason: [{ required: true, message: '请填写退款原因', trigger: 'blur' }]
}

const prettyResultText = computed(() => {
  const raw = detail.value?.result?.resultText
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
})

function statusLabel(status) {
  return statusLabels[status] || status || '-'
}

function statusTagType(status) {
  if (status === 'OPENED') return 'success'
  if (status === 'PAID') return 'primary'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'REFUNDED') return 'danger'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function canRefund(row) {
  return row.status === 'PAID' || row.status === 'OPENED'
}

function buildParams() {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (filters.orderNo) params.orderNo = filters.orderNo
  if (filters.userId) params.userId = filters.userId
  if (filters.status) params.status = filters.status
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await getAdminOrders(buildParams())
    orders.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    orders.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadOrders()
}

function handleReset() {
  filters.orderNo = ''
  filters.userId = ''
  filters.status = ''
  filters.dateRange = []
  pagination.page = 1
  loadOrders()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadOrders()
}

async function openDetail(row) {
  try {
    const res = await getAdminOrder(row.orderNo)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    // 错误已由请求拦截器提示
  }
}

function openRefund(row) {
  refundTarget.value = row
  refundForm.reason = ''
  refundVisible.value = true
}

async function handleRefund() {
  try {
    await refundFormRef.value.validate()
  } catch {
    return
  }
  refundSubmitting.value = true
  try {
    await refundOrder(refundTarget.value.orderNo, refundForm.reason)
    ElMessage.success('退款成功')
    refundVisible.value = false
    await loadOrders()
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    refundSubmitting.value = false
  }
}

async function handleRetry(row) {
  try {
    await ElMessageBox.confirm(
      `确定重新触发订单「${row.orderNo}」的开盒吗？`,
      '重试开盒',
      { confirmButtonText: '确定重试', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await retryOpenOrder(row.orderNo)
    ElMessage.success('已重新提交开盒请求')
    await loadOrders()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.table-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.result-collapse {
  margin-top: 12px;
}

.result-json {
  margin: 0;
  padding: 12px;
  max-height: 320px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
