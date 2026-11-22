<template>
  <div class="blindbox-list-page">
    <div class="page-header">
      <h1>旅行盲盒</h1>
      <p>日常盲盒随时可选，限定盲盒请留意库存与活动时间。</p>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="typeFilter" aria-label="盲盒类型" @change="handleTypeChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="DAILY">日常盲盒</el-radio-button>
        <el-radio-button value="LIMITED">限定盲盒</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="24" v-loading="loading">
      <el-col
        v-for="item in list"
        :key="item.id"
        :xs="24"
        :sm="12"
        :md="8"
        class="blindbox-col"
      >
        <div class="blindbox-card">
          <div class="card-cover">
            <el-image :src="item.coverUrl" fit="cover" class="cover-img">
              <template #error>
                <div class="img-placeholder">
                  <el-icon :size="40"><Box /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-if="item.type === 'DAILY'" class="card-tag daily">日常盲盒</div>
            <div v-else-if="item.type === 'LIMITED'" class="card-tag limited">限定盲盒</div>
          </div>
          <div class="card-body">
            <h2 class="card-title">{{ item.name }}</h2>
            <p class="card-desc">{{ getRuleDescription(item) || '暂无描述' }}</p>
            <div class="card-footer">
              <div class="price-wrapper">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ item.price }}</span>
                <span class="price-original" v-if="item.originalPrice">¥{{ item.originalPrice }}</span>
              </div>
              <span v-if="item.type === 'LIMITED' && Number.isInteger(item.stock) && item.stock >= 0"
                class="stock-text" :class="{ 'is-sold-out': item.stock === 0 }">
                {{ item.stock === 0 ? '已售罄' : `剩余 ${item.stock} 份` }}
              </span>
              <router-link :to="`/blind-box/${item.id}`" class="detail-link" :aria-label="`查看${item.name}详情`">
                查看详情
              </router-link>
            </div>
          </div>
        </div>
      </el-col>
      <el-col v-if="!loading && list.length === 0" :span="24">
        <el-empty :description="loadFailed ? '盲盒列表暂时无法加载' : '暂无盲盒数据'">
          <el-button v-if="loadFailed" @click="loadData">重新加载</el-button>
        </el-empty>
      </el-col>
    </el-row>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[9, 12, 18, 30]"
        :pager-count="5"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getBlindBoxTemplates } from '@/api/blindbox'
import { Box } from '@element-plus/icons-vue'
import { ruleDescription } from '@/utils/businessRules'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const loadFailed = ref(false)
const typeFilter = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 9
})

function getRuleDescription(item) {
  return ruleDescription(item?.ruleConfig)
}

async function loadData() {
  loading.value = true
  loadFailed.value = false
  try {
    const params = {
      page: pagination.current,
      limit: pagination.pageSize
    }
    if (typeFilter.value) {
      params.type = typeFilter.value
    }
    const res = await getBlindBoxTemplates(params)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    loadFailed.value = true
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  pagination.current = 1
  loadData()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.current = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.blindbox-list-page {
  padding: 10px 0 40px;
}

.page-header {
  margin-bottom: 24px;
  padding: clamp(20px, 4vw, 40px);
  border-radius: 18px;
  background: #153d4b;
}

.page-header h1 {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}

.page-header p {
  font-size: 15px;
  color: #dfedf0;
}

.filter-bar {
  margin-bottom: 32px;
}

.blindbox-col {
  margin-bottom: 28px;
}

.blindbox-card {
  position: relative;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.15);
}

.blindbox-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
}

.card-cover {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.card-tag {
  position: absolute;
  top: 16px;
  left: 16px;
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.card-tag.daily {
  background: #153d4b;
}

.card-tag.limited {
  color: #603b14;
  background: #ffdd9a;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.5);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(255, 107, 107, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0);
  }
}

.card-body {
  padding: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 44px;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price-symbol {
  font-size: 15px;
  color: #873d20;
  font-weight: 600;
}

.price-value {
  font-size: 24px;
  color: #873d20;
  font-weight: 700;
}

.price-original {
  font-size: 14px;
  color: #667781;
  text-decoration: line-through;
  margin-left: 6px;
}

.stock-text {
  font-size: 12px;
  color: #425b67;
  white-space: nowrap;
}

.stock-text.is-sold-out { color: #8a371f; font-weight: 600; }
.detail-link { display: inline-flex; align-items: center; min-height: 44px; padding: 0 12px; border-radius: 8px; color: #153d4b; background: #e8f2f2; font-weight: 600; }
.detail-link::after { content: ''; position: absolute; inset: 0; }
.detail-link:hover { color: #153d4b; background: #cce4e5; }
.detail-link:focus-visible { outline: 2px solid #206681; outline-offset: 2px; }

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.pagination-wrapper :deep(.el-pagination) { flex-wrap: wrap; justify-content: center; row-gap: 8px; max-width: 100%; }

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f8ff 100%);
  color: var(--primary-color);
}
</style>
