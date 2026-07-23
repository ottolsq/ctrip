<template>
  <div class="blindbox-list-page">
    <div class="page-header">
      <h2>盲盒专区</h2>
      <p>开启未知惊喜，遇见专属旅行</p>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="typeFilter" @change="handleTypeChange">
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
          <div class="card-cover" @click="goToDetail(item.id)">
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
            <h3 class="card-title" @click="goToDetail(item.id)">{{ item.name }}</h3>
            <p class="card-desc">{{ item.description || '暂无描述' }}</p>
            <div class="card-footer">
              <div class="price-wrapper">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ item.price }}</span>
                <span class="price-original" v-if="item.originalPrice">¥{{ item.originalPrice }}</span>
              </div>
              <el-button type="primary" size="small" @click="goToDetail(item.id)">
                立即购买
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
      <el-col v-if="!loading && list.length === 0" :span="24">
        <el-empty description="暂无盲盒数据" />
      </el-col>
    </el-row>

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[9, 12, 18, 30]"
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
import { useRouter } from 'vue-router'
import { getBlindBoxTemplates } from '@/api/blindbox'
import { Box } from '@element-plus/icons-vue'

const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const typeFilter = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 9
})

async function loadData() {
  loading.value = true
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

function goToDetail(id) {
  router.push(`/blind-box/${id}`)
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
  margin-bottom: 32px;
  text-align: center;
}

.page-header h2 {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.page-header p {
  font-size: 15px;
  color: var(--text-muted);
}

.filter-bar {
  margin-bottom: 32px;
}

.blindbox-col {
  margin-bottom: 28px;
}

.blindbox-card {
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
  cursor: pointer;
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
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
}

.card-tag.limited {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  animation: pulse 2s infinite;
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
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title:hover {
  color: var(--primary-color);
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
  color: var(--danger-color);
  font-weight: 600;
}

.price-value {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: 700;
}

.price-original {
  font-size: 14px;
  color: var(--text-light);
  text-decoration: line-through;
  margin-left: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f8ff 100%);
  color: var(--primary-color);
}
</style>
