<template>
  <div class="guide-detail" v-loading="loading">
    <div v-if="guide" class="detail-content">
      <!-- 标题区 -->
      <div class="guide-header">
        <h1 class="guide-title">{{ guide.title }}</h1>
        <div class="guide-meta-bar">
          <span class="meta-item">
            <el-icon><User /></el-icon> {{ guide.authorName || '匿名' }}
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon> {{ formatDate(guide.createdAt) }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon> {{ guide.viewCount || 0 }}
          </span>
        </div>
      </div>

      <!-- 封面图 -->
      <div class="cover-wrapper" v-if="guide.coverUrl">
        <el-image :src="guide.coverUrl" fit="contain" class="detail-cover" />
      </div>

      <!-- 攻略内容 -->
      <el-card class="content-card" shadow="never">
        <div class="guide-content" v-html="guide.content || '<p>暂无内容</p>'"></div>
      </el-card>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button
          :type="guide.isLiked ? 'primary' : 'default'"
          :icon="Star"
          @click="handleToggleLike"
          round
        >
          {{ guide.isLiked ? '已赞' : '点赞' }} {{ guide.likeCount || '' }}
        </el-button>
        <el-button
          :type="guide.isCollected ? 'warning' : 'default'"
          :icon="Star"
          @click="handleToggleCollect"
          round
        >
          {{ guide.isCollected ? '已收藏' : '收藏' }}
        </el-button>
        <el-button v-if="isAuthor" type="danger" plain :icon="Delete" @click="handleDelete" round>
          删除
        </el-button>
      </div>

      <!-- 评论区域 -->
      <el-card class="comment-section" shadow="never">
        <template #header>
          <h3>评论 ({{ commentCount }})</h3>
        </template>

        <!-- 发表评论 -->
        <div class="comment-input-area" v-if="userStore.isLoggedIn">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <div class="comment-submit">
            <el-button type="primary" @click="handleSubmitComment" :loading="commentLoading">
              发表评论
            </el-button>
          </div>
        </div>
        <el-alert v-else type="info" :closable="false" show-icon>
          <template #default>
            <router-link to="/login">登录</router-link> 后即可发表评论
          </template>
        </el-alert>

        <!-- 评论列表（树形） -->
        <div class="comments-list" v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <el-avatar :size="28" :src="comment.userAvatar">
                <el-icon :size="16"><User /></el-icon>
              </el-avatar>
              <span class="comment-author">{{ comment.userName || '匿名' }}</span>
              <span class="comment-time">{{ formatDate(comment.createdAt, true) }}</span>
            </div>
            <div class="comment-body">{{ comment.content }}</div>
            <div class="comment-actions">
              <span class="reply-btn" @click="toggleReply(comment.id)">回复</span>
              <span
                v-if="canDeleteComment(comment)"
                class="delete-btn"
                @click="handleDeleteComment(comment.id)"
              >删除</span>
            </div>

            <!-- 回复输入框 -->
            <div v-if="replyTarget === comment.id" class="reply-input-area">
              <el-input
                v-model="replyContent"
                placeholder="写下回复..."
                size="small"
                @keyup.enter="handleSubmitReply(comment.id)"
              />
              <el-button size="small" type="primary" @click="handleSubmitReply(comment.id)">回复</el-button>
            </div>

            <!-- 子评论 -->
            <div v-if="comment.children?.length" class="sub-comments">
              <div v-for="child in comment.children" :key="child.id" class="sub-comment-item">
                <div class="comment-header">
                  <el-avatar :size="24" :src="child.userAvatar">
                    <el-icon :size="14"><User /></el-icon>
                  </el-avatar>
                  <span class="comment-author">{{ child.userName || '匿名' }}</span>
                  <span class="comment-time">{{ formatDate(child.createdAt, true) }}</span>
                </div>
                <div class="comment-body">{{ child.content }}</div>
                <div class="comment-actions">
                  <span
                    v-if="canDeleteComment(child)"
                    class="delete-btn"
                    @click="handleDeleteComment(child.id)"
                  >删除</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无评论" />
      </el-card>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty description="攻略不存在" />
      <el-button type="primary" @click="router.push('/guides')">返回列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getGuide, deleteGuide, likeGuide, unlikeGuide } from '@/api/guide'
import { getComments, createComment, deleteComment } from '@/api/comment'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, Delete, Clock } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 格式化日期，处理数组和字符串两种格式
function formatDate(date, includeTime = false) {
  if (!date) return ''
  // 如果是数组格式 [year, month, day, hour, minute, second]
  if (Array.isArray(date)) {
    const [year, month, day, hour = 0, minute = 0] = date
    let result = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    if (includeTime) {
      result += ` ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
    }
    return result
  }
  // 如果是 ISO 字符串格式
  if (typeof date === 'string') {
    return includeTime ? date.slice(0, 16) : date.slice(0, 10)
  }
  return ''
}

// 格式化纯文本内容为 HTML（处理换行）
function formatContent(content) {
  if (!content) return '<p>暂无内容</p>'
  // 将换行符转换为 <p> 标签
  const paragraphs = content.split('\n').filter(p => p.trim())
  if (paragraphs.length === 0) return '<p>暂无内容</p>'
  return paragraphs.map(p => `<p>${p}</p>`).join('')
}

const loading = ref(true)
const guide = ref(null)
const comments = ref([])
const commentCount = ref(0)
const newComment = ref('')
const replyContent = ref('')
const replyTarget = ref(null)
const commentLoading = ref(false)

const isAuthor = computed(() => {
  return guide.value && userStore.userId && guide.value.authorId === userStore.userId
})

function canDeleteComment(comment) {
  return userStore.isLoggedIn && (comment.authorId === userStore.userId || userStore.isAdmin)
}

async function loadGuide() {
  const id = route.params.id
  try {
    const res = await getGuide(id)
    guide.value = res.data
  } catch {
    guide.value = null
  }
}

async function loadComments() {
  const id = route.params.id
  try {
    const res = await getComments(id)
    comments.value = res.data || []
    commentCount.value = comments.value.length
  } catch {
    comments.value = []
  }
}

async function handleToggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (guide.value.isLiked) {
      await unlikeGuide(guide.value.id)
      guide.value.isLiked = false
      guide.value.likeCount = Math.max(0, (guide.value.likeCount || 0) - 1)
    } else {
      await likeGuide(guide.value.id)
      guide.value.isLiked = true
      guide.value.likeCount = (guide.value.likeCount || 0) + 1
    }
  } catch {
    // 错误已处理
  }
}

async function handleToggleCollect() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  guide.value.isCollected = !guide.value.isCollected
  ElMessage.success(guide.value.isCollected ? '已收藏' : '已取消收藏')
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这篇攻略吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteGuide(guide.value.id)
    ElMessage.success('攻略已删除')
    router.push('/guides')
  } catch {
    // 取消或错误
  }
}

async function handleSubmitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentLoading.value = true
  try {
    const res = await createComment(route.params.id, { content: newComment.value })
    comments.value.unshift(res.data)
    commentCount.value++
    newComment.value = ''
    ElMessage.success('评论成功')
  } catch {
    // 错误已处理
  } finally {
    commentLoading.value = false
  }
}

function toggleReply(commentId) {
  replyTarget.value = replyTarget.value === commentId ? null : commentId
  replyContent.value = ''
}

async function handleSubmitReply(parentId) {
  if (!replyContent.value.trim()) return
  try {
    const res = await createComment(route.params.id, {
      content: replyContent.value,
      parentId
    })
    const parent = comments.value.find(c => c.id === parentId)
    if (parent) {
      if (!parent.children) parent.children = []
      parent.children.push(res.data)
    }
    replyContent.value = ''
    replyTarget.value = null
    ElMessage.success('回复成功')
  } catch {
    // 错误已处理
  }
}

async function handleDeleteComment(commentId) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
    await deleteComment(route.params.id, commentId)

    // 从列表中移除
    const idx = comments.value.findIndex(c => c.id === commentId)
    if (idx > -1) {
      comments.value.splice(idx, 1)
    } else {
      // 可能在子评论中
      for (const c of comments.value) {
        if (c.children) {
          const childIdx = c.children.findIndex(ch => ch.id === commentId)
          if (childIdx > -1) {
            c.children.splice(childIdx, 1)
            break
          }
        }
      }
    }
    commentCount.value--
    ElMessage.success('评论已删除')
  } catch {
    // 取消或错误
  }
}

onMounted(async () => {
  await Promise.all([loadGuide(), loadComments()])
  loading.value = false
})
</script>

<style scoped>
.guide-detail {
  padding: 10px 0 40px;
  max-width: 860px;
  margin: 0 auto;
}

.guide-header {
  margin-bottom: 24px;
}

.guide-title {
  font-size: 28px;
  font-weight: 700;
  color: #222;
  line-height: 1.4;
  margin-bottom: 16px;
}

.guide-meta-bar {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #999;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cover-wrapper {
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 24px;
  background: #f5f7fa;
}

.detail-cover {
  width: 100%;
  display: block;
  max-height: none;
}

.content-card {
  border: none;
  margin-bottom: 24px;
}

.guide-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.guide-content :deep(p) {
  margin-bottom: 16px;
}

.guide-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 12px 0;
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-section {
  border: none;
}

.comment-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.comment-input-area {
  margin-bottom: 24px;
}

.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  border-bottom: 1px solid #f5f5f5;
  padding-bottom: 16px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #bbb;
  margin-left: auto;
}

.comment-body {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 8px;
  padding-left: 38px;
}

.comment-actions {
  padding-left: 38px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.reply-btn {
  color: #409eff;
  cursor: pointer;
}

.delete-btn {
  color: #f56c6c;
  cursor: pointer;
}

.reply-input-area {
  display: flex;
  gap: 8px;
  margin: 12px 0 12px 38px;
}

.sub-comments {
  margin: 12px 0 0 38px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sub-comment-item {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.sub-comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}
</style>