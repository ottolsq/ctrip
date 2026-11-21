<template>
  <div class="draw-record-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" placeholder="用户 ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="模板ID">
          <el-input v-model="filters.templateId" placeholder="模板 ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="档位">
          <el-select v-model="filters.tier" placeholder="全部" clearable style="width: 120px">
            <el-option label="普通" value="COMMON" />
            <el-option label="稀有" value="RARE" />
            <el-option label="传说" value="LEGENDARY" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽取日期">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="records" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            {{ row.userName || '-' }}
            <span class="muted">(ID: {{ row.userId }})</span>
          </template>
        </el-table-column>
        <el-table-column label="模板" min-width="150">
          <template #default="{ row }">
            {{ row.templateName || '-' }}
            <span class="muted">(ID: {{ row.templateId }})</span>
          </template>
        </el-table-column>
        <el-table-column label="档位" width="100">
          <template #default="{ row }">
            <el-tag :type="tierTagType(row.tier)" size="small">{{ tierLabel(row.tier) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目的地" min-width="110">
          <template #default="{ row }">{{ row.destination || '-' }}</template>
        </el-table-column>
        <el-table-column label="保底" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.guaranteed" type="warning" size="small">是</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="probabilityVersion" label="概率版本" width="100" />
        <el-table-column label="抽取时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
          @current-change="loadRecords"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAdminDrawRecords } from '@/api/admin/drawRecord'

const loading = ref(false)
const records = ref([])

const filters = reactive({
  userId: '',
  templateId: '',
  tier: '',
  dateRange: []
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const tierLabels = {
  COMMON: '普通',
  RARE: '稀有',
  LEGENDARY: '传说'
}

function tierLabel(tier) {
  return tierLabels[tier] || tier || '-'
}

function tierTagType(tier) {
  if (tier === 'LEGENDARY') return 'danger'
  if (tier === 'RARE') return 'warning'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0] = value
    const pad = n => String(n).padStart(2, '0')
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}`
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function buildParams() {
  const params = { page: pagination.page, size: pagination.size }
  if (filters.userId) params.userId = filters.userId
  if (filters.templateId) params.templateId = filters.templateId
  if (filters.tier) params.tier = filters.tier
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getAdminDrawRecords(buildParams())
    records.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadRecords()
}

function handleReset() {
  filters.userId = ''
  filters.templateId = ''
  filters.tier = ''
  filters.dateRange = []
  pagination.page = 1
  loadRecords()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadRecords()
}

onMounted(loadRecords)
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
</style>
