<template>
  <div class="blind-box-detail" v-loading="loading">
    <div v-if="template" class="detail-content">
      <el-button @click="router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <el-card class="main-card" shadow="never">
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="cover-wrapper">
              <el-image :src="template.coverUrl" fit="cover" class="cover-image">
                <template #error>
                  <div class="img-placeholder-lg">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-if="template.type === 'DAILY'" class="tag-badge daily">日常盲盒</div>
            <div v-else-if="template.type === 'LIMITED'" class="tag-badge limited">限定盲盒</div>
            </div>
          </el-col>

          <el-col :span="12">
            <div class="info-section">
              <h1 class="template-name">{{ template.name }}</h1>
              <div class="price-row">
                <span class="price-label">价格</span>
                <span class="price-value">¥{{ template.price }}</span>
                <span v-if="template.originalPrice" class="original-price">¥{{ template.originalPrice }}</span>
              </div>

              <div class="desc-section">
                <h4>简介</h4>
                <p class="description">{{ template.description || '暂无描述' }}</p>
              </div>

              <div class="rules-section">
                <h4>规则说明</h4>
                <div class="rules-content" v-html="template.rules || '暂无规则说明'"></div>
              </div>

              <div class="action-section">
                <el-button type="primary" size="large" class="buy-btn" @click="handleBuy">
                  <el-icon><ShoppingCart /></el-icon>
                  立即购买
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="detail-card" shadow="never">
        <template #header>
          <h3>详细介绍</h3>
        </template>
        <div class="detail-content-inner" v-html="template.detail || template.description || '暂无详细介绍'"></div>
      </el-card>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty description="盲盒模板不存在" />
      <el-button type="primary" @click="router.push('/blind-box')">返回列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlindBoxTemplate } from '@/api/blindbox'
import { ArrowLeft, Picture, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const template = ref(null)

function handleBuy() {
  router.push(`/blind-box/${route.params.id}/order`)
}

onMounted(async () => {
  const id = route.params.id
  try {
    const res = await getBlindBoxTemplate(id)
    template.value = res.data
  } catch (err) {
    template.value = null
    ElMessage.error('获取盲盒详情失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.blind-box-detail {
  padding: 10px 0 40px;
}

.back-btn {
  margin-bottom: 16px;
}

.main-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.cover-wrapper {
  position: relative;
  width: 100%;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.tag-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag-badge.daily {
  background: linear-gradient(135deg, #409eff, #667eea);
}

.tag-badge.limited {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.4);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(255, 107, 107, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0);
  }
}

.info-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.template-name {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 16px 0;
  line-height: 1.3;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fff7e6;
  border-radius: 8px;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.price-value {
  font-size: 32px;
  font-weight: 700;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.desc-section,
.rules-section {
  margin-bottom: 20px;
}

.desc-section h4,
.rules-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.rules-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.action-section {
  margin-top: auto;
  padding-top: 20px;
}

.buy-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
}

.detail-card {
  border-radius: 12px;
}

.detail-card h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.detail-content-inner {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
}

.detail-content-inner :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.img-placeholder-lg {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 64px;
}
</style>
