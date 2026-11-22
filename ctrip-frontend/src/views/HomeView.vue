<template>
  <div class="home">
    <section class="home-hero" aria-labelledby="home-title">
      <div class="home-intro">
        <h1 id="home-title">下一站，去哪里？</h1>
        <p>从想去的地方开始，也可以把目的地交给旅行盲盒。</p>
        <router-link to="/destinations" class="home-action">探索目的地 →</router-link>
      </div>
    <!-- 轮播 Banner：无启用数据时隐藏 -->
    <el-carousel
      v-if="banners.length"
      class="banner-carousel"
      :autoplay="false"
      :arrow="banners.length > 1 ? 'always' : 'never'"
      :indicator-position="banners.length > 1 ? '' : 'none'"
    >
      <el-carousel-item v-for="banner in banners" :key="banner.id">
        <div
          class="banner-item"
          :class="{ clickable: banner.linkUrl }"
          @click="handleLinkClick(banner.linkUrl)"
        >
          <el-image :src="banner.imageUrl" fit="cover" class="banner-image">
            <template #error>
              <div class="banner-fallback"></div>
            </template>
          </el-image>
          <div class="banner-content">
            <h2>{{ banner.title }}</h2>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
    </section>

    <!-- 盲盒快捷入口 -->
    <div class="blind-box-entry-section">
      <div class="section-card blind-box-card">
        <div class="bb-card-inner">
          <el-icon :size="28"><Present /></el-icon>
          <h3>旅行盲盒</h3>
          <p>未知的目的地，惊喜的旅程</p>
          <router-link to="/blind-box" class="home-action">立即开启</router-link>
        </div>
      </div>
      <div class="section-card guide-card">
        <div class="bb-card-inner">
          <el-icon :size="28"><Reading /></el-icon>
          <h3>旅行攻略</h3>
          <p>精选旅行攻略，灵感源泉</p>
          <div class="card-actions">
            <router-link to="/guides" class="home-action">浏览攻略</router-link>
            <router-link to="/guides/create" class="home-action">发布攻略</router-link>
          </div>
        </div>
      </div>
      <div class="section-card itinerary-card">
        <div class="bb-card-inner">
          <el-icon :size="28"><MapLocation /></el-icon>
          <h3>行程规划</h3>
          <p>一键规划你的完美旅程</p>
          <router-link to="/itineraries" class="home-action">开始规划</router-link>
        </div>
      </div>
    </div>

    <!-- 热门目的地 -->
    <section class="section">
      <div class="section-header">
        <h2>热门目的地</h2>
        <router-link to="/destinations" class="section-more">查看全部 &gt;</router-link>
      </div>
      <p v-if="!hotDestinations.length" class="section-status" role="status">{{ sectionMessage('destinations', '热门目的地') }}</p>
      <el-row :gutter="20">
        <el-col v-for="item in hotDestinations" :key="item.id" :xs="12" :sm="12" :md="6">
          <router-link class="destination-card" :to="`/destinations/${item.id}`">
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
          </router-link>
        </el-col>
      </el-row>
    </section>

    <!-- 精选推荐：无启用数据时隐藏 -->
    <section v-if="recommendations.length" class="section">
      <div class="section-header">
        <h2>精选推荐</h2>
      </div>
      <el-row :gutter="20">
        <el-col
          v-for="item in recommendations"
          :key="item.id"
          :xs="24"
          :sm="12"
          :md="8"
        >
          <div class="recommendation-card" @click="handleLinkClick(item.linkUrl)">
            <div class="guide-img-wrapper">
              <el-image
                :src="item.coverUrl || '/placeholder-guide.jpg'"
                fit="cover"
                class="guide-img"
              >
                <template #error>
                  <div class="img-placeholder">
                    <el-icon :size="28"><Star /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="guide-info">
              <h3>{{ item.targetTitle || item.title }}</h3>
              <p>{{ item.title }}</p>
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
      <p v-if="!featuredGuides.length" class="section-status" role="status">{{ sectionMessage('guides', '精选攻略') }}</p>
      <el-row :gutter="20">
        <el-col v-for="item in featuredGuides" :key="item.id" :xs="24" :sm="12" :md="8">
          <router-link class="guide-card-h" :to="`/guides/${item.id}`">
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
          </router-link>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDestinations } from '@/api/destination'
import { getGuides } from '@/api/guide'
import { getPublicBanners } from '@/api/banner'
import { getPublicRecommendations } from '@/api/recommendation'

const router = useRouter()

const banners = ref([])
const recommendations = ref([])
const hotDestinations = ref([])
const featuredGuides = ref([])
const sectionStatus = reactive({ destinations: 'loading', guides: 'loading' })

function handleLinkClick(linkUrl) {
  if (!linkUrl) return
  if (/^https?:\/\//i.test(linkUrl)) {
    window.open(linkUrl, '_blank', 'noopener')
    return
  }
  router.push(linkUrl)
}

function sectionMessage(key, label) {
  if (sectionStatus[key] === 'loading') return `正在加载${label}…`
  if (sectionStatus[key] === 'error') return `${label}暂时无法加载，请稍后重试。`
  return `暂无${label}。`
}

async function loadSection(key, request, items) {
  try {
    items.value = (await request()).data?.records || []
    sectionStatus[key] = 'ready'
  } catch {
    sectionStatus[key] = 'error'
  }
}

onMounted(() => {
  getPublicBanners().then(({ data }) => { banners.value = data || [] }).catch(() => {})
  getPublicRecommendations('home_featured').then(({ data }) => { recommendations.value = data || [] }).catch(() => {})
  loadSection('destinations', () => getDestinations({ current: 1, size: 4 }), hotDestinations)
  loadSection('guides', () => getGuides({ current: 1, size: 3 }), featuredGuides)
})
</script>

<style scoped>
.home {
  padding: 0 0 40px;
}

.home-hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 310px), 1fr));
  align-items: center;
  margin-bottom: 24px;
  border-radius: 18px;
  overflow: hidden;
  color: #fff;
  background: #153d4b;
}

.home-intro { padding: clamp(24px, 5vw, 56px); }
.home-intro h1 { font-size: clamp(28px, 4vw, 52px); line-height: 1.2; }
.home-intro p { margin: 14px 0 22px; line-height: 1.65; color: #dfedf0; }
.home-action { display: inline-flex; align-items: center; min-height: 44px; padding: 0 14px; border-radius: 8px; color: #153d4b; background: #e8f2f2; font-weight: 600; }
.home-action:hover { color: #153d4b; background: #cce4e5; }
.home-intro .home-action, .blind-box-card .home-action { background: #ffdd9a; }
.home-intro .home-action:hover, .blind-box-card .home-action:hover { background: #ffcc70; }
.home :is(a, button):focus-visible { outline: 2px solid #206681; outline-offset: 2px; }
.home-intro a:focus-visible { outline-color: #ffdd9a; }
.section-status { padding: 16px; border: 1px solid #d5e2e6; border-radius: 10px; color: #425b67; background: #fff; }

/* Banner */
.banner-carousel {
  overflow: hidden;
  height: clamp(180px, 27vw, 340px);
  min-width: 0;
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
  position: relative;
  overflow: hidden;
}

.banner-item.clickable {
  cursor: pointer;
}

.banner-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.banner-fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #409eff 0%, #764ba2 100%);
}

.banner-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 8px 16px;
  max-width: 65%;
  border-radius: 8px;
  background: #153d4be6;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.45);
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
  font-size: clamp(20px, 3vw, 36px);
  font-weight: 700;
  overflow-wrap: anywhere;
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
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 212px), 1fr));
  gap: 20px;
  margin-bottom: 48px;
}

.section-card {
  border-radius: 20px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  min-height: 170px;
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
  background: #ffefd0;
}

.guide-card {
  background: #fff;
}

.itinerary-card {
  background: #fff;
}

.bb-card-inner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 22px;
  color: #153d4b;
  position: relative;
  z-index: 1;
}

.bb-card-inner h3 {
  font-size: 22px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: #153d4b;
}

.bb-card-inner p {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 24px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.destination-card, .guide-card-h { display: block; margin-bottom: 14px; }

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
  color: #206681;
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

/* 推荐位卡片 */
.recommendation-card {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  margin-bottom: 20px;
}

.recommendation-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
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
