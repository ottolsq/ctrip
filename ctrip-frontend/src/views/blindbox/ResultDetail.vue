<template>
  <div class="result-detail" v-loading="loading">
    <div v-if="result" class="detail-content">
      <el-button @click="handleBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <el-card class="result-hero-card" shadow="never">
        <div class="hero-header">
          <div class="result-badge">
            <el-icon class="badge-icon"><Present /></el-icon>
            <span>盲盒结果</span>
          </div>
          <h1 class="result-title">恭喜你开出了「{{ result.destination }}」行程！</h1>
          <p class="result-subtitle">{{ result.theme || '神秘之旅' }} · {{ result.days || 3 }}天行程</p>
        </div>

        <div class="hero-image-wrapper" v-if="result.coverUrl">
          <el-image :src="result.coverUrl" fit="cover" class="hero-image">
            <template #error>
              <div class="img-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </div>

        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-label">目的地</div>
            <div class="stat-value">{{ result.destination }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">主题</div>
            <div class="stat-value">{{ result.theme || '不限' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">行程天数</div>
            <div class="stat-value">{{ result.days || 3 }} 天</div>
          </div>

        </div>

        <div class="hero-actions">
          <el-button v-if="!isShare" size="large" class="action-btn" @click="handleShare">
            <el-icon><Share /></el-icon>
            分享结果
          </el-button>
          <el-button type="primary" size="large" class="action-btn primary" :loading="creating" @click="handleCreateItinerary">
            <el-icon v-if="!creating"><MagicStick /></el-icon>
            {{ creating ? '正在创建行程...' : '生成我的行程' }}
          </el-button>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never" v-if="result.attractions && result.attractions.length > 0">
        <template #header>
          <div class="card-header">
            <el-icon><Location /></el-icon>
            <span>行程摘要</span>
          </div>
        </template>
        <div class="attractions-list">
          <div
            v-for="(item, index) in result.attractions"
            :key="index"
            class="attraction-item"
          >
            <el-tag type="primary" size="small" class="attraction-tag">景点{{ index + 1 }}</el-tag>
            <span class="attraction-name">{{ item.name || item }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never" v-if="result.dailyItinerary && result.dailyItinerary.length > 0">
        <template #header>
          <div class="card-header">
            <el-icon><Calendar /></el-icon>
            <span>行程详情</span>
          </div>
        </template>

        <div class="timeline-container">
          <div class="timeline-line"></div>

          <div
            v-for="(day, index) in result.dailyItinerary"
            :key="index"
            class="timeline-item"
          >
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="day-header">
                <div class="day-number">第 {{ day.dayNumber || index + 1 }} 天</div>
                <div class="day-title">{{ day.title || `第${index + 1}天行程` }}</div>
              </div>

              <div v-if="day.items && day.items.length > 0" class="items-list">
                <div
                  v-for="(item, itemIndex) in day.items"
                  :key="itemIndex"
                  class="item-card"
                >
                  <div class="item-time">{{ item.timeSlot || item.time || `时段${itemIndex + 1}` }}</div>
                  <div class="item-content">
                    <div class="item-name">{{ item.name }}</div>
                    <div v-if="item.location" class="item-location">
                      <el-icon class="small-icon"><Location /></el-icon>
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
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never" v-else-if="result.daysDetail && result.daysDetail.length > 0">
        <template #header>
          <div class="card-header">
            <el-icon><Calendar /></el-icon>
            <span>行程详情</span>
          </div>
        </template>

        <div class="timeline-container">
          <div class="timeline-line"></div>

          <div
            v-for="(day, index) in result.daysDetail"
            :key="index"
            class="timeline-item"
          >
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="day-header">
                <div class="day-number">第 {{ day.dayNumber || index + 1 }} 天</div>
                <div class="day-title">{{ day.title || `第${index + 1}天行程` }}</div>
              </div>

              <div v-if="day.items && day.items.length > 0" class="items-list">
                <div
                  v-for="(item, itemIndex) in day.items"
                  :key="itemIndex"
                  class="item-card"
                >
                  <div class="item-time">{{ item.timeSlot || item.time || `时段${itemIndex + 1}` }}</div>
                  <div class="item-content">
                    <div class="item-name">{{ item.name }}</div>
                    <div v-if="item.location" class="item-location">
                      <el-icon class="small-icon"><Location /></el-icon>
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
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty description="结果不存在或已失效" />
      <el-button type="primary" @click="handleBack">返回</el-button>
    </div>

    <el-dialog v-model="shareDialogVisible" title="分享盲盒结果" width="420px">
      <div class="share-content">
        <el-alert
          title="分享链接已生成"
          type="success"
          :closable="false"
          show-icon
        />
        <div class="share-desc">
          将链接分享给好友，TA 也能查看你的盲盒结果哦~
        </div>
        <div class="share-link">
          <el-input :value="shareLink" readonly>
            <template #append>
              <el-button @click="copyShareLink">复制</el-button>
            </template>
          </el-input>
        </div>
        <div class="share-tips">
          <el-icon><InfoFilled /></el-icon>
          <span>链接有效期 7 天，过期后无法访问</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Present,
  Picture,
  Location,
  Calendar,
  Share,
  MagicStick,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  getBlindBoxResult,
  getSharedBlindBoxResult,
  shareBlindBoxResult
} from '@/api/blindbox'
import {
  createItinerary,
  getItinerary,
  addItem
} from '@/api/itinerary'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const result = ref(null)
const shareDialogVisible = ref(false)
const shareLink = ref('')
const creating = ref(false)

const isShare = computed(() => {
  return route.meta?.isShare === true || route.name === 'BlindBoxShare'
})

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

async function loadData() {
  loading.value = true
  try {
    let res
    if (isShare.value) {
      const code = route.params.code
      res = await getSharedBlindBoxResult(code)
    } else {
      const orderNo = route.params.orderNo
      res = await getBlindBoxResult(orderNo)
    }
    const data = res.data
    if (data.resultText && typeof data.resultText === 'string') {
      try {
        const parsed = JSON.parse(data.resultText)
        Object.assign(data, parsed)
      } catch (e) {
        console.warn('Failed to parse resultText:', e)
      }
    }
    result.value = data
  } catch (err) {
    result.value = null
    ElMessage.error(isShare.value ? '获取分享结果失败' : '获取结果详情失败')
  } finally {
    loading.value = false
  }
}

function handleBack() {
  if (isShare.value) {
    router.push('/')
  } else if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/blind-box/my')
  }
}

async function handleShare() {
  if (!result.value || !result.value.orderNo) {
    ElMessage.error('无法生成分享链接')
    return
  }
  try {
    const res = await shareBlindBoxResult(result.value.orderNo)
    const code = res.data.shareCode || res.data.code
    shareLink.value = `${window.location.origin}/blind-box/share/${code}`
    shareDialogVisible.value = true
  } catch (err) {
    ElMessage.error('生成分享链接失败')
  }
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

async function handleCreateItinerary() {
  if (creating.value) return
  creating.value = true

  try {
    const data = result.value
    const days = data.days || 3

    const today = new Date()
    const startDate = formatDate(today)
    const endDate = new Date(today)
    endDate.setDate(today.getDate() + days - 1)
    const endDateStr = formatDate(endDate)

    const createRes = await createItinerary({
      title: `${data.destination || '盲盒'}·${data.theme || '旅行'}·${days}天`,
      destinationId: data.destinationId,
      startDate: startDate,
      endDate: endDateStr
    })

    const itineraryId = createRes.data.id

    const detailRes = await getItinerary(itineraryId)
    const backendDays = detailRes.data.days || []

    if (data.itinerary) {
      const keys = Object.keys(data.itinerary).sort()
      for (let i = 0; i < keys.length && i < backendDays.length; i++) {
        const dayData = data.itinerary[keys[i]]
        const dayId = backendDays[i].id

        if (dayData.items && Array.isArray(dayData.items)) {
          for (const itemText of dayData.items) {
            const item = parseItineraryItem(itemText)
            await addItem(itineraryId, dayId, item)
          }
        }
      }
    }

    if (data.hotel && data.hotel.name && backendDays.length > 0) {
      await addItem(itineraryId, backendDays[backendDays.length - 1].id, {
        type: 'HOTEL',
        name: data.hotel.name,
        location: data.hotel.address || '',
        timeSlot: '',
        description: data.hotel.rating ? `评分: ${data.hotel.rating}` : ''
      })
    }

    if (data.transport && data.transport.description && backendDays.length > 0) {
      await addItem(itineraryId, backendDays[0].id, {
        type: 'TRANSPORT',
        name: `${data.transport.type || '交通'}: ${data.transport.destination || ''}`,
        location: '',
        timeSlot: data.transport.departureTime || '',
        description: data.transport.description
      })
    }

    ElMessage.success('行程已自动创建，可在「我的行程」中查看和编辑')
    router.push(`/itineraries/${itineraryId}`)
  } catch (err) {
    console.error('创建行程失败:', err)
    ElMessage.error('创建行程失败：' + (err.message || '未知错误'))
  } finally {
    creating.value = false
  }
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function parseItineraryItem(text) {
  let type = 'ATTRACTION'
  let timeSlot = ''

  const timeMatch = text.match(/(上午|下午|傍晚|清晨|中午|凌晨|晚上)/)
  if (timeMatch) {
    timeSlot = timeMatch[1]
  }

  const typeKeywords = [
    { keyword: '用餐', type: 'FOOD' },
    { keyword: '午餐', type: 'FOOD' },
    { keyword: '晚餐', type: 'FOOD' },
    { keyword: '早餐', type: 'FOOD' },
    { keyword: '小吃', type: 'FOOD' },
    { keyword: '海鲜', type: 'FOOD' },
    { keyword: '餐厅', type: 'FOOD' },
    { keyword: '入住', type: 'HOTEL' },
    { keyword: '旅馆', type: 'HOTEL' },
    { keyword: '酒店', type: 'HOTEL' },
    { keyword: '高铁', type: 'TRANSPORT' },
    { keyword: '轮渡', type: 'TRANSPORT' },
    { keyword: '地铁', type: 'TRANSPORT' },
    { keyword: '公交', type: 'TRANSPORT' },
    { keyword: '骑行', type: 'TRANSPORT' },
    { keyword: '返程', type: 'TRANSPORT' },
    { keyword: '出发', type: 'TRANSPORT' },
    { keyword: '参观', type: 'ATTRACTION' },
    { keyword: '游览', type: 'ATTRACTION' },
    { keyword: '漫步', type: 'ATTRACTION' },
    { keyword: '探访', type: 'ATTRACTION' },
    { keyword: '打卡', type: 'ATTRACTION' },
    { keyword: '登', type: 'ATTRACTION' },
    { keyword: '逛', type: 'ATTRACTION' },
    { keyword: '夜游', type: 'ACTIVITY' },
    { keyword: '活动', type: 'ACTIVITY' }
  ]

  for (const item of typeKeywords) {
    if (text.includes(item.keyword)) {
      type = item.type
      break
    }
  }

  let name = text
  const nameMatch = text.match(/([\u4e00-\u9fa5]{2,30})(馆|楼|寺|塔|公园|广场|街|路|海滩|山|岛|码头|大学)/)
  if (nameMatch) {
    name = nameMatch[0]
  }

  return {
    type: type,
    name: name.length > 30 ? name.substring(0, 30) + '...' : name,
    location: '',
    timeSlot: timeSlot,
    description: text
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.result-detail {
  padding: 10px 0 100px;
}

.back-btn {
  margin-bottom: 16px;
}

.result-hero-card {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
}

.hero-header {
  text-align: center;
  padding: 20px 0 24px;
}

.result-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.badge-icon {
  font-size: 16px;
}

.result-title {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.result-subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.hero-image-wrapper {
  width: 100%;
  height: 280px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 24px;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 48px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.stat-item {
  text-align: center;
  padding: 12px 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-value.price {
  color: #f56c6c;
  font-size: 20px;
}

.section-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header .el-icon {
  color: #409eff;
}

.attractions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.attraction-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #f5f7fa;
  border-radius: 20px;
  transition: all 0.2s;
}

.attraction-item:hover {
  background: #ecf5ff;
}

.attraction-tag {
  flex-shrink: 0;
}

.attraction-name {
  font-size: 14px;
  color: #303133;
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

.timeline-content {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.day-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
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
  background: #fff;
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
}

.small-icon {
  font-size: 12px;
  margin-right: 4px;
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

.hero-actions {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f2f5;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
}

.action-btn.primary {
  flex: 1.5;
}

@media (max-width: 768px) {
  .hero-actions {
    flex-direction: column;
    gap: 12px;
  }

  .action-btn,
  .action-btn.primary {
    flex: none;
    width: 100%;
  }
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-state .el-button {
  margin-top: 16px;
}

.share-content {
  padding: 8px 0;
}

.share-desc {
  font-size: 14px;
  color: #606266;
  margin: 16px 0;
  text-align: center;
}

.share-link {
  margin-bottom: 16px;
}

.share-tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.share-tips .el-icon {
  color: #909399;
}
</style>
