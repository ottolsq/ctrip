<template>
  <div class="collections-page">
    <el-row :gutter="24">
      <el-col :span="6">
        <UserSidebar active-menu="/user/collections" />
      </el-col>

      <el-col :span="18">
        <el-card class="collections-card">
          <template #header>
            <div class="card-header">
              <h3>我的收藏</h3>
              <el-radio-group v-model="contentType" size="small" @change="handleTypeChange">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="GUIDE">攻略</el-radio-button>
                <el-radio-button value="ITINERARY">行程</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div v-loading="loading">
            <div v-if="collections.length > 0" class="collection-list">
              <div
                v-for="item in collections"
                :key="item.id"
                class="collection-item"
                @click="goToDetail(item)"
              >
                <div class="item-cover">
                  <el-image
                    v-if="item.coverUrl"
                    :src="item.coverUrl"
                    fit="cover"
                    class="cover-img"
                  >
                    <template #error>
                      <div class="img-placeholder">
                        <el-icon :size="28"><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div v-else class="img-placeholder">
                    <el-icon :size="28"><Picture /></el-icon>
                  </div>
                  <el-tag class="type-tag" size="small" :type="item.contentType === 'GUIDE' ? 'primary' : 'success'">
                    {{ item.contentType === 'GUIDE' ? '攻略' : '行程' }}
                  </el-tag>
                </div>
                <div class="item-info">
                  <h4 class="item-title">{{ item.title }}</h4>
                  <p class="item-desc">{{ item.summary || item.description || '暂无描述' }}</p>
                  <div class="item-meta">
                    <span class="meta-item">
                      <el-icon><Clock /></el-icon>
                      {{ formatDate(item.createdAt) }}
                    </span>
                    <span v-if="item.authorName" class="meta-item">
                      <el-icon><User /></el-icon>
                      {{ item.authorName }}
                    </span>
                  </div>
                </div>
                <div class="item-actions">
                  <el-button
                    type="danger"
                    size="small"
                    plain
                    circle
                    @click.stop="handleUncollect(item)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>

            <el-empty v-else-if="!loading" description="暂无收藏内容" />

            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadCollections"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Clock, User, Delete } from '@element-plus/icons-vue'
import UserSidebar from '@/components/common/UserSidebar.vue'
import { getCollections, uncollectGuide } from '@/api/collection'

const router = useRouter()

const loading = ref(false)
const collections = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const contentType = ref('')

async function loadCollections() {
  loading.value = true
  try {
    const res = await getCollections({
      page: page.value,
      size: pageSize.value,
      type: contentType.value || undefined
    })
    collections.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载收藏失败:', e)
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  page.value = 1
  loadCollections()
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  if (Array.isArray(dateStr)) {
    const [y, m, d] = dateStr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return dateStr.substring(0, 10)
}

function goToDetail(item) {
  if (item.contentType === 'GUIDE') {
    router.push(`/guides/${item.contentId || item.guideId || item.id}`)
  } else if (item.contentType === 'ITINERARY') {
    router.push(`/itineraries/${item.contentId || item.itineraryId || item.id}`)
  }
}

async function handleUncollect(item) {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    const contentId = item.contentId || item.guideId || item.id
    await uncollectGuide(contentId)
    ElMessage.success('已取消收藏')
    loadCollections()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadCollections()
})
</script>

<style scoped>
.collections-page {
  padding: 10px 0;
}

.collections-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.collection-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.collection-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.item-cover {
  position: relative;
  width: 120px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f7fa;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
}

.type-tag {
  position: absolute;
  top: 4px;
  left: 4px;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #aaa;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
