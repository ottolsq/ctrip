<template>
  <div class="itinerary-list">
    <div class="page-header">
      <h1>我的行程</h1>
      <el-button type="primary" @click="router.push('/itineraries/create')">
        <el-icon><Plus /></el-icon>
        创建行程
      </el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-spin />
    </div>

    <div v-else-if="list.length === 0" class="empty-state">
      <el-empty description="暂无行程" />
      <el-button type="primary" @click="router.push('/itineraries/create')">创建第一个行程</el-button>
    </div>

    <el-row :gutter="20" v-else>
      <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8">
        <el-card class="itinerary-card" shadow="hover" @click="goDetail(item.id)">
          <div class="card-header">
            <h3>{{ item.title }}</h3>
            <el-tag v-if="item.shareCode" type="success" size="small">已分享</el-tag>
          </div>
          <div class="card-info">
            <div class="info-item">
              <el-icon><MapLocation /></el-icon>
              <span>{{ getDestinationName(item) }}</span>
            </div>
            <div class="info-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDate(item.startDate) }} ~ {{ formatDate(item.endDate) }}</span>
            </div>
            <div class="info-item">
              <el-icon><List /></el-icon>
              <span>{{ getDayCount(item) }} 天行程</span>
            </div>
          </div>
          <div class="card-footer">
            <div class="card-stats">
              <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }}</span>
              <span><el-icon><Star /></el-icon> {{ item.likeCount || 0 }}</span>
            </div>
            <div class="card-actions">
              <el-button size="small" type="primary" @click.stop="goEdit(item.id)">编辑</el-button>
              <el-button size="small" type="danger" @click.stop="handleDelete(item.id)">删除</el-button>
            </div>
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
import { Plus, MapLocation, Calendar, List, View, Star } from '@element-plus/icons-vue'
import { getItineraries, deleteItinerary } from '@/api/itinerary'
import { getDestinations } from '@/api/destination'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const destinations = ref([])

const pagination = ref({
  current: 1,
  pageSize: 12
})

function formatDate(date) {
  if (!date) return '-'
  // 如果是数组格式 [year, month, day]
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getDayCount(item) {
  // 如果有 days 数组，返回其长度
  if (item.days && Array.isArray(item.days)) {
    return item.days.length
  }
  // 如果有 dayCount 字段，直接返回
  if (item.dayCount !== undefined) {
    return item.dayCount
  }
  // 根据 startDate 和 endDate 计算
  if (item.startDate && item.endDate) {
    const start = new Date(item.startDate)
    const end = new Date(item.endDate)
    const diffTime = Math.abs(end - start)
    const days = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
    return days > 0 ? days : 0
  }
  return 0
}

function getDestinationName(item) {
  // 优先使用 destinationName 字段
  if (item.destinationName) {
    return item.destinationName
  }
  // 检查嵌套的 destination 对象
  if (item.destination && item.destination.name) {
    return item.destination.name
  }
  // 根据 destinationId 从目的地列表中查找
  if (item.destinationId && destinations.value.length > 0) {
    const dest = destinations.value.find(d => d.id === item.destinationId)
    if (dest) {
      return dest.name
    }
  }
  return '未知目的地'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getItineraries({
      page: pagination.value.current,
      limit: pagination.value.pageSize
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/itineraries/${id}`)
}

function goEdit(id) {
  router.push(`/itineraries/${id}/edit`)
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个行程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteItinerary(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消或删除失败
  }
}

async function loadDestinations() {
  try {
    const res = await getDestinations({ current: 1, size: 100 })
    destinations.value = res.data.records || []
  } catch {
    destinations.value = []
  }
}

onMounted(async () => {
  await loadDestinations()
  loadData()
})
</script>

<style scoped>
.itinerary-list {
  padding: 0 10px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h1 {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #303133;
    }
  }

  .loading-state, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 0;
  }

  .empty-state {
    .el-button {
      margin-top: 16px;
    }
  }

  .itinerary-card {
    cursor: pointer;
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

    h3 {
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

    .el-icon {
      margin-right: 8px;
      font-size: 14px;
      color: #409eff;
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
    margin-top: auto;
  }

  .card-stats {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #909399;

    .el-icon {
      margin-right: 4px;
    }
  }

  .card-actions {
    display: flex;
    gap: 8px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 32px;
    padding-bottom: 24px;
  }
}
</style>