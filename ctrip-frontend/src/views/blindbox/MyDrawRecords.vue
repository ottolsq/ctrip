<template>
  <div class="draw-records">
    <div class="page-header">
      <div>
        <h1>我的抽取记录</h1>
        <p>每次开盒的档位、目的地与保底信息均可追溯。</p>
      </div>
      <el-button @click="router.push('/blind-box/my')">返回我的盲盒</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="records" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column label="盲盒" min-width="150">
          <template #default="{ row }">{{ row.templateName || '-' }}</template>
        </el-table-column>
        <el-table-column label="档位" width="100">
          <template #default="{ row }">
            <el-tag :type="tierTagType(row.tier)" size="small">{{ tierLabel(row.tier) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目的地" min-width="120">
          <template #default="{ row }">{{ row.destination || '-' }}</template>
        </el-table-column>
        <el-table-column label="保底" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.guaranteed" type="warning" size="small">保底</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="概率版本" width="100">
          <template #default="{ row }">{{ row.probabilityVersion || '-' }}</template>
        </el-table-column>
        <el-table-column label="抽取时间" min-width="170">
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
import { useRouter } from 'vue-router'
import { getMyDrawRecords } from '@/api/blindbox'

const router = useRouter()
const loading = ref(false)
const records = ref([])
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

async function loadRecords() {
  loading.value = true
  try {
    const res = await getMyDrawRecords({
      page: pagination.page,
      size: pagination.size
    })
    records.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadRecords()
}

onMounted(loadRecords)
</script>

<style scoped>
.draw-records {
  padding: 10px 0 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 22px;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
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
  color: #c0c4cc;
}
</style>
