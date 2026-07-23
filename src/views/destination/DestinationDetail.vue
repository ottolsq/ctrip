<template>
  <div class="destination-detail" v-loading="loading">
    <div v-if="destination" class="detail-content">
      <!-- 头部信息 -->
      <div class="hero-section" :style="{ background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)` }">
        <div class="hero-text">
          <h2>{{ destination.name }}</h2>
          <p>{{ destination.description }}</p>
        </div>
      </div>

      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <h3>目的地信息</h3>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ destination.name }}</el-descriptions-item>
          <el-descriptions-item label="国家">{{ destination.country || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="最佳季节">{{ formatSeason(destination.bestSeason) }}</el-descriptions-item>
          <el-descriptions-item label="省份">{{ destination.province || '未知' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 景点列表 -->
      <el-card class="section-card" shadow="never" v-if="attractions.length > 0">
        <template #header>
          <h3>热门景点 ({{ attractions.length }})</h3>
        </template>
        <el-row :gutter="16">
          <el-col v-for="item in attractions" :key="item.id" :span="8">
            <div class="attraction-item" @click="showAttractionDetail(item)">
              <div class="attr-img-wrapper">
                <el-image :src="getAttractionFirstImage(item)" fit="cover" class="attr-img">
                  <template #error>
                    <div class="img-placeholder"><el-icon><Camera /></el-icon></div>
                  </template>
                </el-image>
              </div>
              <div class="attr-info">
                <h4>{{ item.name }}</h4>
                <p>{{ item.description?.slice(0, 30) || '暂无描述' }}</p>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 相关攻略 -->
      <el-card class="section-card" shadow="never" v-if="relatedGuides.length > 0">
        <template #header>
          <h3>相关攻略 ({{ relatedGuides.length }})</h3>
        </template>
        <el-row :gutter="16">
          <el-col v-for="item in relatedGuides" :key="item.id" :span="8">
            <div class="guide-item" @click="router.push(`/guides/${item.id}`)">
              <div class="guide-item-img">
                <el-image :src="item.coverUrl" fit="cover" class="guide-sm-img">
                  <template #error>
                    <div class="img-placeholder"><el-icon><Document /></el-icon></div>
                  </template>
                </el-image>
              </div>
              <div class="guide-item-info">
                <h4>{{ item.title }}</h4>
                <span class="guide-author">{{ item.authorName || '匿名' }}</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <div v-if="attractions.length === 0 && relatedGuides.length === 0" class="empty-state">
        <el-empty description="暂无更多信息" />
      </div>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty description="目的地不存在" />
      <el-button type="primary" @click="router.push('/destinations')">返回列表</el-button>
    </div>

    <!-- 景点详情弹窗 -->
    <el-dialog v-model="attractionDialogVisible" :title="selectedAttraction?.name" width="700px" center>
      <div v-if="selectedAttraction" class="attraction-detail-modal">
        <div class="modal-carousel" v-if="getAttractionImages(selectedAttraction).length > 0">
          <el-carousel :interval="4000" type="card" height="300px" indicator-position="outside">
            <el-carousel-item v-for="(img, index) in getAttractionImages(selectedAttraction)" :key="index">
              <el-image :src="img" fit="contain" class="modal-carousel-img">
                <template #error>
                  <div class="img-placeholder-lg"><el-icon><Camera /></el-icon></div>
                </template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="modal-info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="简介">{{ selectedAttraction.description || '暂无简介' }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ selectedAttraction.location || '暂无地址信息' }}</el-descriptions-item>
            <el-descriptions-item label="门票价格">
              <span v-if="selectedAttraction.ticketPrice">¥{{ selectedAttraction.ticketPrice }}</span>
              <span v-else>免费或未设置</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="attractionDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDestination } from '@/api/destination'
import { getAttractions } from '@/api/attraction'
import { getGuides } from '@/api/guide'
import { Camera } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const seasonMap = {
  SPRING: '春季',
  SUMMER: '夏季',
  AUTUMN: '秋季',
  WINTER: '冬季',
  YEAR_ROUND: '全年'
}

function formatSeason(season) {
  if (!season) return '全年'
  return seasonMap[season] || '全年'
}

function getAttractionImages(item) {
  let images = []
  if (item.imageUrls) {
    let parsedUrls = []
    if (typeof item.imageUrls === 'string') {
      try {
        parsedUrls = JSON.parse(item.imageUrls)
      } catch (e) {
        console.warn('Failed to parse imageUrls:', e)
      }
    } else if (Array.isArray(item.imageUrls)) {
      parsedUrls = item.imageUrls
    }
    if (Array.isArray(parsedUrls)) {
      images = parsedUrls.filter(url => url && typeof url === 'string' && url.trim())
    }
  }
  if (item.coverUrl && item.coverUrl.trim()) {
    const cover = item.coverUrl.trim()
    if (!images.includes(cover)) {
      images.unshift(cover)
    }
  }
  return images
}

function getAttractionFirstImage(item) {
  const images = getAttractionImages(item)
  if (images.length === 0) return ''
  return images[0]
}

const loading = ref(true)
const destination = ref(null)
const attractions = ref([])
const relatedGuides = ref([])

// 景点弹窗相关
const attractionDialogVisible = ref(false)
const selectedAttraction = ref(null)

function showAttractionDetail(attraction) {
  selectedAttraction.value = attraction
  attractionDialogVisible.value = true
}

onMounted(async () => {
  const id = route.params.id
  try {
    const destRes = await getDestination(id)
    destination.value = destRes.data

    // 并行加载景点和相关攻略
    const [attrRes, guideRes] = await Promise.allSettled([
      getAttractions({ destinationId: id, current: 1, size: 6 }),
      getGuides({ destinationId: id, current: 1, size: 6 })
    ])

    if (attrRes.status === 'fulfilled') {
      attractions.value = attrRes.value.data?.records || []
    }
    if (guideRes.status === 'fulfilled') {
      relatedGuides.value = guideRes.value.data?.records || []
    }
  } catch {
    destination.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.destination-detail {
  padding: 10px 0 40px;
}

.hero-section {
  border-radius: 20px;
  padding: 70px 40px;
  margin-bottom: 32px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, transparent 50%);
}

.hero-text {
  position: relative;
  z-index: 1;
}

.hero-text h2 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
}

.hero-text p {
  font-size: 17px;
  opacity: 0.95;
  line-height: 1.7;
}

.info-card {
  margin-bottom: 32px;
  border-radius: 16px;
}

.section-card {
  margin-bottom: 32px;
  border-radius: 16px;
}

.section-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.attraction-item {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.attraction-item:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}

.attr-img-wrapper {
  height: 150px;
  overflow: hidden;
  position: relative;
}

.attr-img-wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: linear-gradient(to top, rgba(255,255,255,0.9) 0%, transparent 100%);
}

.attr-img {
  width: 100%;
  height: 100%;
}

.attr-info {
  padding: 14px;
}

.attr-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.attr-info p {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-item {
  display: flex;
  gap: 14px;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 14px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.guide-item:hover {
  background: var(--bg-light);
  transform: translateY(-2px);
}

.guide-item-img {
  width: 90px;
  height: 65px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.guide-sm-img {
  width: 100%;
  height: 100%;
}

.guide-item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.guide-item-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-author {
  font-size: 13px;
  color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  color: var(--primary-color);
}

/* 景点详情弹窗样式 */
.attraction-detail-modal {
  .modal-carousel {
    width: 100%;
    border-radius: 12px;
    overflow: hidden;
    margin-bottom: 20px;
  }

  .modal-carousel-img {
    width: 100%;
    height: 100%;
  }

  .img-placeholder-lg {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #c0c4cc;
    font-size: 48px;
  }

  .modal-info {
    margin-top: 16px;
  }
}
</style>