<template>
  <div class="my-guides-page">
    <el-row :gutter="24">
      <el-col :span="6">
        <UserSidebar active-menu="/user/guides" />
      </el-col>

      <el-col :span="18">
        <el-card class="guides-card">
          <template #header>
            <div class="card-header">
              <h3>我的攻略</h3>
              <el-button type="primary" size="small" @click="router.push('/guides/create')">
                <el-icon><Plus /></el-icon>
                发布攻略
              </el-button>
            </div>
          </template>

          <div v-loading="loading">
            <div v-if="guides.length > 0" class="guide-list">
              <div v-for="guide in guides" :key="guide.id" class="guide-item">
                <div class="guide-cover">
                  <el-image
                    v-if="guide.coverUrl"
                    :src="guide.coverUrl"
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
                </div>
                <div class="guide-info" @click="goToDetail(guide.id)">
                  <h4 class="guide-title">{{ guide.title }}</h4>
                  <p class="guide-summary">{{ guide.summary || stripHtml(guide.content) }}</p>
                  <div class="guide-meta">
                    <span class="meta-item">
                      <el-icon><Location /></el-icon>
                      {{ guide.destinationName || '未知目的地' }}
                    </span>
                    <span class="meta-item">
                      <el-icon><Clock /></el-icon>
                      {{ formatDate(guide.createdAt) }}
                    </span>
                    <span class="meta-item">
                      <el-icon><View /></el-icon>
                      {{ guide.viewCount || 0 }}
                    </span>
                    <span class="meta-item">
                      <el-icon><Star /></el-icon>
                      {{ guide.likeCount || 0 }}
                    </span>
                  </div>
                </div>
                <div class="guide-actions">
                  <el-button size="small" plain @click="goToEdit(guide.id)">
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" plain @click="handleDelete(guide.id)">
                    删除
                  </el-button>
                </div>
              </div>
            </div>

            <el-empty v-else-if="!loading" description="还没有发布过攻略">
              <el-button type="primary" @click="router.push('/guides/create')">发布第一篇攻略</el-button>
            </el-empty>

            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadGuides"
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
import { Plus, Picture, Location, Clock, View, Star } from '@element-plus/icons-vue'
import UserSidebar from '@/components/common/UserSidebar.vue'
import { getGuides, deleteGuide } from '@/api/guide'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const guides = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

async function loadGuides() {
  loading.value = true
  try {
    const res = await getGuides({
      page: page.value,
      size: pageSize.value,
      authorId: userStore.userInfo?.id
    })
    guides.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载攻略失败:', e)
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  if (Array.isArray(dateStr)) {
    const [y, m, d] = dateStr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return String(dateStr).substring(0, 10)
}

function stripHtml(html) {
  if (!html) return '暂无摘要'
  return html.replace(/<[^>]+>/g, '').substring(0, 80) + '...'
}

function goToDetail(id) {
  router.push(`/guides/${id}`)
}

function goToEdit(id) {
  router.push(`/guides/${id}/edit`)
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除这篇攻略吗？删除后不可恢复。', '提示', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteGuide(id)
    ElMessage.success('攻略已删除')
    loadGuides()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadGuides()
})
</script>

<style scoped>
.my-guides-page {
  padding: 10px 0;
}

.guides-card {
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

.guide-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  transition: all 0.2s;
}

.guide-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.guide-cover {
  width: 120px;
  height: 90px;
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

.guide-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.guide-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-summary {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.guide-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #aaa;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.guide-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  justify-content: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
