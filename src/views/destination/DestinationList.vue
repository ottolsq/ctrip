<template>
  <div class="destination-list-page">
    <div class="page-header">
      <h2>目的地</h2>
      <p>探索全球热门旅行目的地</p>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索目的地..."
        :prefix-icon="Search"
        clearable
        class="search-input"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
    </div>

    <!-- 列表 -->
    <el-row :gutter="20" v-loading="loading">
      <el-col v-for="item in list" :key="item.id" :span="6" class="dest-col">
        <div class="destination-card" @click="router.push(`/destinations/${item.id}`)">
          <div class="dest-img-wrapper">
            <el-image :src="item.coverUrl" fit="cover" class="dest-img">
              <template #error>
                <div class="img-placeholder">
                  <el-icon :size="36"><Location /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div class="dest-info">
            <h3>{{ item.name }}</h3>
            <p>{{ item.description?.slice(0, 40) || '暂无描述' }}</p>
          </div>
        </div>
      </el-col>
      <el-col v-if="!loading && list.length === 0" :span="24">
        <el-empty description="暂无目的地数据" />
      </el-col>
    </el-row>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[8, 12, 20, 40]"
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
import { getDestinations } from '@/api/destination'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const searchQuery = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 8
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDestinations({
      page: pagination.current,
      limit: pagination.pageSize,
      keyword: searchQuery.value || undefined
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
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
.destination-list-page {
  padding: 10px 0 40px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: #999;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
}

.search-input {
  width: 360px;
}

.dest-col {
  margin-bottom: 20px;
}

.destination-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.destination-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.dest-img-wrapper {
  height: 180px;
  overflow: hidden;
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
  color: #333;
  margin-bottom: 8px;
}

.dest-info p {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}
</style>