<template>
  <div class="about">
    <div class="about-hero">
      <h2>关于捷程旅行网</h2>
      <p>致力于为您提供最优质的旅行服务体验</p>
    </div>

    <div class="info-section">
      <el-row :gutter="24">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Star /></el-icon>
                <span>我们的优势</span>
              </div>
            </template>
            <ul class="advantage-list">
              <li>全球覆盖的旅行目的地</li>
              <li>24/7 全天候客户服务</li>
              <li>最具竞争力的价格</li>
              <li>专业的旅行规划团队</li>
            </ul>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Phone /></el-icon>
                <span>联系我们</span>
              </div>
            </template>
            <div class="contact-info">
              <p><strong>邮箱:</strong> contact@ctrip.com</p>
              <p><strong>电话:</strong> 400-888-8888</p>
              <p><strong>地址:</strong> 北京市朝阳区xxx大厦</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.about {
  padding: 20px 0;
}

.about-hero {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #409eff 0%, #36d1dc 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 40px;
}

.about-hero h2 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 12px;
}

.about-hero p {
  font-size: 16px;
  opacity: 0.9;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.advantage-list {
  list-style: none;
  padding: 0;
}

.advantage-list li {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;
}

.advantage-list li:last-child {
  border-bottom: none;
}

.advantage-list li::before {
  content: '✓ ';
  color: #67c23a;
  font-weight: bold;
}

.contact-info p {
  padding: 8px 0;
  font-size: 14px;
  color: #666;
}
</style>
