<template>
  <div class="blind-box-detail" v-loading="loading">
    <div v-if="template" class="detail-content">
      <el-button @click="router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <el-card class="main-card" shadow="never">
        <el-row :gutter="24">
          <el-col :xs="24" :sm="12">
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

          <el-col :xs="24" :sm="12">
            <div class="info-section">
              <h1 class="template-name">{{ template.name }}</h1>
              <div class="price-row">
                <span class="price-label">价格</span>
                <span class="price-value">¥{{ template.price }}</span>
                <span v-if="template.originalPrice" class="original-price">¥{{ template.originalPrice }}</span>
              </div>

              <div v-if="isLimited" class="limited-info">
                <div class="info-row">
                  <span class="info-label">剩余库存</span>
                  <span>{{ soldOut ? '已售罄' : `${template.stock} 份` }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">活动时间</span>
                  <span>{{ formatDateTime(rule.activityStartTime) }} ~ {{ formatDateTime(rule.activityEndTime) }}</span>
                </div>
              </div>

              <div class="action-section">
                <el-button type="primary" size="large" class="buy-btn" :disabled="soldOut" @click="handleBuy">
                  <el-icon><ShoppingCart /></el-icon>
                  {{ soldOut ? '已售罄' : '立即购买' }}
                </el-button>
              </div>

              <div class="rules-section">
                <h2>规则说明</h2>
                <div class="rules-content">
                  <p v-if="rule.description">{{ rule.description }}</p>
                  <p v-if="isLimited">活动期间限量供应，售罄即止。</p>
                  <p v-else-if="!rule.description">暂无规则说明</p>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card v-if="probabilities" class="detail-card probability-card" shadow="never">
        <template #header>
          <h3>概率公示</h3>
        </template>
        <el-table :data="probabilities.tiers" style="width: 100%">
          <el-table-column label="档位" min-width="68">
            <template #default="{ row }">
              <el-tag :type="tierTagType(row.tier)" size="small">{{ tierLabel(row.tier) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="概率" min-width="76">
            <template #default="{ row }">{{ Number(row.probability).toFixed(2) }}%</template>
          </el-table-column>
          <el-table-column label="可能目的地" min-width="100">
            <template #default="{ row }">
              {{ row.destinations?.length ? row.destinations.join('、') : '奖品池调整中' }}
            </template>
          </el-table-column>
        </el-table>
        <p class="guarantee-tip">
          连续 {{ probabilities.guaranteeCount }} 次未中稀有及以上，下一次必中稀有及以上。
        </p>
        <p class="version-tip">概率版本：{{ probabilities.probabilityVersion }}，实际抽取与公示一致。</p>
      </el-card>

      <el-card class="detail-card" shadow="never">
        <template #header>
          <h3>详细介绍</h3>
        </template>
        <div class="detail-content-inner">{{ rule.description || '暂无详细介绍' }}</div>
      </el-card>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty :description="loadFailed ? '盲盒详情暂时无法加载' : '盲盒模板不存在或已下架'">
        <el-button v-if="loadFailed" type="primary" @click="loadDetail">重新加载</el-button>
        <el-button @click="router.push('/blind-box')">返回列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlindBoxTemplate, getBlindBoxProbabilities } from '@/api/blindbox'
import { ArrowLeft, Picture, ShoppingCart } from '@element-plus/icons-vue'
import { parseRuleConfig } from '@/utils/businessRules'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const loadFailed = ref(false)
const template = ref(null)
const probabilities = ref(null)

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

const rule = computed(() => parseRuleConfig(template.value?.ruleConfig))
const isLimited = computed(() => template.value?.type === 'LIMITED')
const soldOut = computed(() => isLimited.value && Number(template.value?.stock ?? -1) <= 0)

function formatDateTime(value) {
  if (!value) return '未配置'
  const date = new Date(value)
  if (isNaN(date.getTime())) return '未配置'
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function handleBuy() {
  if (soldOut.value) return
  const suffix = isLimited.value ? '?flash=1' : ''
  router.push(`/blind-box/${route.params.id}/order${suffix}`)
}

async function loadDetail() {
  loading.value = true
  loadFailed.value = false
  const id = route.params.id
  try {
    const res = await getBlindBoxTemplate(id)
    template.value = res.data
  } catch (err) {
    template.value = null
    loadFailed.value = err.response?.status !== 404
  } finally {
    loading.value = false
  }

  if (!template.value) return
  try {
    const probabilityRes = await getBlindBoxProbabilities(id)
    probabilities.value = probabilityRes.data
  } catch {
    probabilities.value = null
  }
}

onMounted(loadDetail)
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

.probability-card {
  margin-bottom: 24px;
}

.guarantee-tip {
  margin: 16px 0 6px;
  color: #e6a23c;
  font-weight: 600;
}

.version-tip {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.cover-wrapper {
  position: relative;
  width: 100%;
  height: clamp(220px, 32vw, 400px);
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
  background: #153d4b;
}

.tag-badge.limited {
  color: #603b14;
  background: #ffdd9a;
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
  flex-wrap: wrap;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: #ffefd0;
  border-radius: 8px;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.price-value {
  font-size: 32px;
  font-weight: 700;
  color: #873d20;
}

.original-price {
  font-size: 14px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.rules-section {
  margin-top: 20px;
}

.rules-section h2 {
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

.rules-content p {
  margin: 0 0 6px 0;
}

.rules-content p:last-child {
  margin-bottom: 0;
}

.limited-info {
  margin-bottom: 20px;
  padding: 12px;
  background: #e8f2f2;
  border-radius: 6px;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.info-row + .info-row {
  margin-top: 8px;
}

.info-label {
  color: #909399;
}

.action-section {
  margin-top: 4px;
}

.buy-btn {
  --el-button-bg-color: #206681;
  --el-button-hover-bg-color: #174a5a;
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
}

@media (max-width: 767px) {
  .cover-wrapper { height: auto; aspect-ratio: 16 / 9; margin-bottom: 18px; }
  .template-name { font-size: 24px; }
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
