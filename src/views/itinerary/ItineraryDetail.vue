<template>
  <div class="itinerary-detail">
    <div v-if="loading" class="loading-state">
      <el-spin />
    </div>

    <div v-else-if="!itinerary" class="empty-state">
      <el-empty description="行程不存在" />
      <el-button type="primary" @click="router.push('/itineraries')">返回列表</el-button>
    </div>

    <div v-else>
      <div class="detail-header">
        <div class="header-info">
          <h1>{{ itinerary.title }}</h1>
          <div class="meta-info">
            <span class="meta-item">
              <el-icon><MapLocation /></el-icon>
              {{ getDestinationName(itinerary) }}
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(itinerary.startDate) }} ~ {{ formatDate(itinerary.endDate) }}
            </span>
            <span class="meta-item">
              <el-icon><List /></el-icon>
              {{ itinerary.days?.length || 0 }} 天
            </span>
          </div>
        </div>
        <div class="header-actions">
          <el-button icon="Share" @click="handleShare">分享</el-button>
          <el-button icon="Edit" type="primary" @click="router.push(`/itineraries/${itinerary.id}/edit`)">编辑</el-button>
          <el-button icon="ArrowLeft" @click="router.push('/itineraries')">返回列表</el-button>
        </div>
      </div>

      <div class="stats-bar">
        <div class="stat-item">
          <el-icon><View /></el-icon>
          <span>{{ itinerary.viewCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <el-icon><Star /></el-icon>
          <span>{{ itinerary.likeCount || 0 }}</span>
        </div>
      </div>

      <div class="timeline-wrapper">
        <div class="timeline-title">
          <el-icon><Clock /></el-icon>
          <span>行程日程</span>
        </div>

        <div class="timeline-container">
          <div class="timeline-line"></div>

          <div
            v-for="day in itinerary.days"
            :key="day.id"
            class="timeline-item"
          >
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <el-card class="day-card">
                <div class="day-header">
                  <div class="day-number">第 {{ day.dayNumber }} 天</div>
                  <div class="day-title">{{ day.title || '自由活动' }}</div>
                </div>

                <div v-if="day.items?.length > 0" class="items-list">
                  <div
                    v-for="(item, index) in day.items"
                    :key="item.id"
                    class="item-card"
                  >
                    <div class="item-time">{{ item.timeSlot || `时段${index + 1}` }}</div>
                    <div class="item-content">
                      <div class="item-name">{{ item.name }}</div>
                      <div v-if="item.location" class="item-location">
                        <el-icon><Location /></el-icon>
                        {{ item.location }}
                      </div>
                      <div v-if="item.description" class="item-desc">
                        {{ item.description }}
                      </div>
                    </div>
                    <div class="item-type">
                      <el-tag size="small" :type="getItemTypeTag(item.type)">
                        {{ getItemTypeName(item.type) }}
                      </el-tag>
                    </div>
                  </div>
                </div>

                <div v-else class="empty-items">
                  <el-empty description="当天暂无安排" :image-size="60" />
                </div>
              </el-card>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="shareDialogVisible" title="分享行程" width="400px">
      <div class="share-content">
        <el-alert
          title="行程分享链接已生成"
          type="success"
          :closable="false"
          show-icon
        />
        <div class="share-link">
          <el-input :value="shareLink" readonly />
          <el-button type="primary" @click="copyShareLink">复制链接</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MapLocation, Calendar, List, View, Star, Clock, Location } from '@element-plus/icons-vue'
import { getItinerary, shareItinerary } from '@/api/itinerary'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const itinerary = ref(null)

const shareDialogVisible = ref(false)
const shareLink = ref('')

function formatDate(date) {
  if (!date) return '-'
  // 如果是数组格式 [year, month, day]
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}年${month}月${day}日`
  }
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function getItemTypeTag(type) {
  const map = {
    ATTRACTION: 'primary',
    FOOD: 'success',
    HOTEL: 'warning',
    TRANSPORT: 'info',
    ACTIVITY: 'danger'
  }
  return map[type] || 'info'
}

function getItemTypeName(type) {
  const map = {
    ATTRACTION: '景点',
    FOOD: '美食',
    HOTEL: '住宿',
    TRANSPORT: '交通',
    ACTIVITY: '活动'
  }
  return map[type] || '其他'
}

function getDestinationName(item) {
  // 优先使用 destinationName 字段
  if (item && item.destinationName) {
    return item.destinationName
  }
  // 检查嵌套的 destination 对象
  if (item && item.destination && item.destination.name) {
    return item.destination.name
  }
  return '未知目的地'
}

async function loadData() {
  const id = route.params.id
  loading.value = true
  try {
    const res = await getItinerary(id)
    itinerary.value = res.data
  } catch {
    itinerary.value = null
  } finally {
    loading.value = false
  }
}

async function handleShare() {
  try {
    const res = await shareItinerary(itinerary.value.id)
    shareLink.value = `${window.location.origin}/itineraries/share/${res.data.shareCode}`
    shareDialogVisible.value = true
  } catch (error) {
    ElMessage.error('生成分享链接失败')
  }
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.itinerary-detail {
  .loading-state, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
  }

  .empty-state {
    .el-button {
      margin-top: 16px;
    }
  }

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .header-info h1 {
    margin: 0 0 12px 0;
    font-size: 24px;
    font-weight: 600;
  }

  .meta-info {
    display: flex;
    gap: 20px;
  }

  .meta-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #606266;

    .el-icon {
      margin-right: 6px;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }

  .stats-bar {
    display: flex;
    gap: 32px;
    margin-bottom: 24px;
  }

  .stat-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #606266;

    .el-icon {
      margin-right: 6px;
      font-size: 16px;
    }
  }

  .timeline-wrapper {
    margin-top: 24px;
  }

  .timeline-title {
    display: flex;
    align-items: center;
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 24px;

    .el-icon {
      margin-right: 8px;
      font-size: 20px;
    }
  }

  .timeline-container {
    position: relative;
    padding-left: 30px;
  }

  .timeline-line {
    position: absolute;
    left: 8px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: #e4e7ed;
  }

  .timeline-item {
    position: relative;
    margin-bottom: 24px;
  }

  .timeline-item:last-child {
    margin-bottom: 0;
  }

  .timeline-dot {
    position: absolute;
    left: -26px;
    top: 20px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #409eff;
    border: 3px solid #fff;
    box-shadow: 0 0 0 2px #dbeafe;
  }

  .day-card {
    margin-bottom: 0;
  }

  .day-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .day-number {
    background: #409eff;
    color: #fff;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 600;
  }

  .day-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .items-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .item-card {
    display: flex;
    gap: 12px;
    padding: 12px;
    background: #fafafa;
    border-radius: 8px;
    transition: all 0.2s;
  }

  .item-card:hover {
    background: #f0f5ff;
  }

  .item-time {
    width: 80px;
    font-size: 14px;
    font-weight: 600;
    color: #409eff;
    flex-shrink: 0;
  }

  .item-content {
    flex: 1;
  }

  .item-name {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .item-location {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;

    .el-icon {
      font-size: 12px;
      margin-right: 4px;
    }
  }

  .item-desc {
    font-size: 13px;
    color: #606266;
    line-height: 1.5;
  }

  .item-type {
    flex-shrink: 0;
  }

  .empty-items {
    padding: 20px 0;
  }

  .share-content {
    .share-link {
      display: flex;
      gap: 12px;
      margin-top: 16px;

      .el-input {
        flex: 1;
      }
    }
  }
}
</style>