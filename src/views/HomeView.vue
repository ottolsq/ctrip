<template>
  <div class="home">
    <div class="hero-section">
      <h2>探索世界的美好</h2>
      <p>发现最精彩的旅行体验</p>
      <el-button type="primary" size="large" @click="handleExplore">开始探索</el-button>
    </div>

    <div class="features">
      <div class="feature-card">
        <el-icon :size="32" color="#409eff"><Location /></el-icon>
        <h3>热门目的地</h3>
        <p>探索全球最受欢迎的旅行胜地</p>
      </div>
      <div class="feature-card">
        <el-icon :size="32" color="#67c23a"><Coin /></el-icon>
        <h3>优惠价格</h3>
        <p>享受最具竞争力的旅行套餐</p>
      </div>
      <div class="feature-card">
        <el-icon :size="32" color="#e6a23c"><Present /></el-icon>
        <h3>旅行盲盒</h3>
        <p>开启未知的惊喜旅行体验</p>
      </div>
      <div class="feature-card">
        <el-icon :size="32" color="#f56c6c"><Service /></el-icon>
        <h3>专业服务</h3>
        <p>为您提供贴心的旅行顾问服务</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const handleExplore = () => {
  router.push('/destinations')
}
</script>

<style scoped>
.home {
  padding: 20px 0;
}

.hero-section {
  text-align: center;
  padding: 80px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 40px;
}

.hero-section h2 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
}

.hero-section p {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 24px;
}

.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  padding: 32px 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.feature-card h3 {
  font-size: 16px;
  margin: 16px 0 8px;
  color: #333;
}

.feature-card p {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}
</style>
