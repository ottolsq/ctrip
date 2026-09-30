<template>
  <div class="home">
    <!-- 轮播 Banner -->
    <el-carousel class="banner-carousel" :interval="4000" arrow="always">
      <el-carousel-item v-for="banner in banners" :key="banner.id">
        <div class="banner-item" :style="{ background: banner.bgColor }">
          <div class="banner-content">
            <h2>{{ banner.title }}</h2>
            <p>{{ banner.subtitle }}</p>
            <el-button type="warning" size="large" @click="handleBannerClick(banner)">
              {{ banner.btnText }}
            </el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 盲盒快捷入口 -->
    <div class="blind-box-entry-section">
      <div class="section-card blind-box-card" @click="router.push('/blind-box')">
        <div class="bb-card-inner">
          <el-icon :size="48" color="#fff"><Present /></el-icon>
          <h3>旅行盲盒</h3>
          <p>未知的目的地，惊喜的旅程</p>
          <el-button type="warning" round @click.stop="router.push('/blindbox')">立即开启</el-button>
        </div>
      </div>
      <div class="section-card guide-card" @click="router.push('/guides')">
        <div class="bb-card-inner">
          <el-icon :size="48" color="#fff"><Reading /></el-icon>
          <h3>旅行攻略</h3>
          <p>精选旅行攻略，灵感源泉</p>
          <div class="card-actions">
            <el-button type="primary" round @click.stop="router.push('/guides')">浏览攻略</el-button>
            <el-button type="success" round @click.stop="router.push('/guides/create')">发布攻略</el-button>
          </div>
        </div>
      </div>
      <div class="section-card itinerary-card" @click="router.push('/itineraries')">
        <div class="bb-card-inner">
          <el-icon :size="48" color="#fff"><MapLocation /></el-icon>
          <h3>行程规划</h3>
          <p>一键规划你的完美旅程</p>
          <el-button type="success" round>开始规划</el-button>
        </div>
      </div>
    </div>

    <!-- 热门目的地 -->
    <section class="section">
      <div class="section-header">
        <h2>热门目的地</h2>
        <router-link to="/destinations" class="section-more">查看全部 &gt;</router-link>
      </div>
      <el-row :gutter="20">
        <el-col v-for="item in hotDestinations" :key="item.id" :span="6">
          <div class="destination-card" @click="router.push(`/destinations/${item.id}`)">
            <div class="dest-img-wrapper">
              <el-image :src="item.coverUrl || '/placeholder-dest.jpg'" fit="cover" class="dest-img">
                <template #error>
                  <div class="img-placeholder">
                    <el-icon :size="32"><Location /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="dest-info">
              <h3>{{ item.name }}</h3>
              <p>{{ item.description?.slice(0, 30) || '探索未知的目的地' }}...</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 攻略精选 -->
    <section class="section">
      <div class="section-header">
        <h2>攻略精选</h2>
        <router-link to="/guides" class="section-more">查看全部 &gt;</router-link>
      </div>
      <el-row :gutter="20">
        <el-col v-for="item in featuredGuides" :key="item.id" :span="8">
          <div class="guide-card-h" @click="router.push(`/guides/${item.id}`)">
            <div class="guide-img-wrapper">
              <el-image :src="item.coverUrl || '/placeholder-guide.jpg'" fit="cover" class="guide-img">
                <template #error>
                  <div class="img-placeholder">
                    <el-icon :size="28"><Document /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="guide-info">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary?.slice(0, 60) || '暂无描述' }}...</p>
              <div class="guide-meta">
                <span><el-icon :size="12"><User /></el-icon> {{ item.authorName || '匿名' }}</span>
                <span><el-icon :size="12"><View /></el-icon> {{ item.viewCount || 0 }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDestinations } from '@/api/destination'
import { getGuides } from '@/api/guide'

const router = useRouter()

const banners = [
  {
    id: 1,
    title: '探索世界的美好',
    subtitle: '发现最精彩的旅行体验，开启属于你的冒险之旅',
    btnText: '探索目的地',
    action: '/destinations',
    bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    id: 2,
    title: '旅行盲盒',
    subtitle: '未知的目的地，惊喜的旅程，开启属于你的盲盒旅行',
    btnText: '开启盲盒',
    action: '/blind-box',
    bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    id: 3,
    title: '分享你的故事',
    subtitle: '记录旅途中的精彩瞬间，与千万旅行者分享',
    btnText: '发布攻略',
    action: '/guides/create',
    bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  }
]

const hotDestinations = ref([])
const featuredGuides = ref([])

function handleBannerClick(banner) {
  router.push(banner.action)
}

onMounted(async () => {
  try {
    const destRes = await getDestinations({ current: 1, size: 4 })
    hotDestinations.value = destRes.data?.records || []
  } catch {
    // 接口暂未实现，使用空数据
    hotDestinations.value = []
  }

  try {
    const guideRes = await getGuides({ current: 1, size: 3 })
    featuredGuides.value = guideRes.data?.records || []
  } catch {
    // 接口暂未实现，使用空数据
    featuredGuides.value = []
  }
})
</script>

<style scoped>
.home {
  padding: 0 0 40px;
}

/* Banner */
.banner-carousel {
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 32px;
  height: 400px;
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.15);
  background: transparent;
}

.banner-carousel :deep(.el-carousel__container) {
  height: 100%;
  background: transparent;
}

.banner-carousel :deep(.el-carousel__indicators) {
  margin-bottom: 20px;
  background: transparent;
  display: flex;
  gap: 6px;
}

.banner-carousel :deep(.el-carousel__indicator) {
  margin: 0;
  padding: 0;
  border: none;
  width: 20px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.banner-carousel :deep(.el-carousel__indicator.is-active) {
  width: 32px;
  background: rgba(255, 255, 255, 0.95);
}

.banner-carousel :deep(.el-carousel__indicator::before) {
  display: none;
}

.banner-carousel :deep(.el-carousel__button) {
  display: none;
}

.banner-carousel :deep(.el-carousel__item) {
  background: transparent;
}

.banner-item {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  color: #fff;
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.banner-content h2 {
  font-size: 44px;
  font-weight: 700;
  margin-bottom: 16px;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: linear-gradient(135deg, #fff 0%, #e6f3ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.banner-content p {
  font-size: 18px;
  opacity: 0.95;
  margin-bottom: 28px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 快捷入口 */
.blind-box-entry-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 48px;
}

.section-card {
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  min-height: 220px;
  position: relative;
}

.section-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, transparent 50%);
}

.section-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(64, 158, 255, 0.2);
}

.blind-box-card {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.guide-card {
  background: linear-gradient(135deg, #72c1ff, #9fd6ff);
}

.itinerary-card {
  background: linear-gradient(135deg, #a5d8ff, #c7e4ff);
}

.bb-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #fff;
  text-align: center;
  position: relative;
  z-index: 1;
}

.bb-card-inner h3 {
  font-size: 22px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: #fff;
}

.bb-card-inner p {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 24px;
}

.card-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 通用分区 */
.section {
  margin-bottom: 48px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.section-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  position: relative;
  padding-left: 16px;
}

.section-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: linear-gradient(180deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: 2px;
}

.section-more {
  font-size: 14px;
  color: var(--primary-color);
  text-decoration: none;
  padding: 6px 16px;
  border-radius: 20px;
  transition: all var(--transition-fast);
}

.section-more:hover {
  background: rgba(64, 158, 255, 0.1);
  text-decoration: none;
}

/* 目的地卡片 */
.destination-card {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.destination-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}

.dest-img-wrapper {
  height: 170px;
  overflow: hidden;
  position: relative;
}

.dest-img-wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(to top, rgba(255,255,255,0.9) 0%, transparent 100%);
}

.dest-img {
  width: 100%;
  height: 100%;
}

.dest-info {
  padding: 16px;
}

.dest-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.dest-info p {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin: 0;
}

/* 攻略卡片 */
.guide-card-h {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.guide-card-h:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}

.guide-img-wrapper {
  height: 190px;
  overflow: hidden;
  position: relative;
}

.guide-img-wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(to top, rgba(255,255,255,0.95) 0%, transparent 100%);
}

.guide-img {
  width: 100%;
  height: 100%;
}

.guide-info {
  padding: 16px;
}

.guide-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-info p {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-light);
}

.guide-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  color: var(--primary-color);
}
</style>