<template>
  <div class="guide-list-page">
    <div class="page-header">
      <h2>旅行攻略</h2>
      <p>精选旅行攻略，发现旅途灵感</p>
    </div>

    <!-- 筛选与排序 -->
    <div class="filter-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索攻略标题..."
        :prefix-icon="Search"
        clearable
        class="search-input"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="sortField" placeholder="排序方式" @change="loadData" class="sort-select">
        <el-option label="最新发布" value="create_time" />
        <el-option label="最多浏览" value="view_count" />
        <el-option label="最多点赞" value="like_count" />
      </el-select>
      <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
    </div>

    <!-- 列表 -->
    <div class="guide-list" v-loading="loading">
      <div v-for="item in list" :key="item.id" class="guide-card" @click="router.push(`/guides/${item.id}`)">
        <div class="guide-left">
          <el-image :src="item.coverUrl" fit="cover" class="guide-cover">
            <template #error>
              <div class="img-placeholder">
                <el-icon :size="32"><Document /></el-icon>
              </div>
            </template>
          </el-image>
        </div>
        <div class="guide-right">
          <h3 class="guide-title">{{ item.title }}</h3>
          <p class="guide-summary">{{ extractText(item.content) }}</p>
          <div class="guide-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon> {{ item.authorName || '匿名' }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon> {{ item.viewCount || 0 }}
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon> {{ item.likeCount || 0 }}
            </span>
            <span class="meta-item">
              <el-icon><ChatDotSquare /></el-icon> {{ item.commentCount || 0 }}
            </span>
            <span class="meta-date">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂无攻略数据" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[10, 20, 30, 50]"
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
import { getGuides } from '@/api/guide'
import { Search, Star, ChatDotSquare } from '@element-plus/icons-vue'

const router = useRouter()

// 格式化日期，处理数组和字符串两种格式
function formatDate(date) {
  if (!date) return ''
  // 如果是数组格式 [year, month, day, hour, minute, second]
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  // 如果是 ISO 字符串格式
  if (typeof date === 'string') {
    return date.slice(0, 10)
  }
  return ''
}

// 从 HTML 内容中提取纯文本，并截取前 N 个字符
function extractText(content, maxLength = 50) {
  if (!content) return '暂无摘要'
  // 使用 DOMParser 正确解析 HTML
  const doc = new DOMParser().parseFromString(content, 'text/html')
  const text = doc.body.textContent || ''
  const cleaned = text.replace(/\s+/g, ' ').trim()
  if (!cleaned) return '暂无摘要'
  if (cleaned.length <= maxLength) return cleaned
  return cleaned.slice(0, maxLength) + '...'
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const searchQuery = ref('')
const sortField = ref('create_time')

const pagination = reactive({
  current: 1,
  pageSize: 10
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      limit: pagination.pageSize,
      keyword: searchQuery.value || undefined,
      sortBy: sortField.value
    }
    const res = await getGuides(params)
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
.guide-list-page {
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
  width: 320px;
}

.sort-select {
  width: 140px;
}

.guide-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.guide-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.guide-left {
  width: 220px;
  min-height: 150px;
  flex-shrink: 0;
}

.guide-cover {
  width: 100%;
  height: 100%;
  display: block;
}

.guide-right {
  flex: 1;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
}

.guide-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-summary {
  font-size: 14px;
  color: #888;
  line-height: 1.6;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.guide-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #aaa;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-date {
  margin-left: auto;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.img-placeholder {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}
</style>