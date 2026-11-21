<template>
  <div class="guide-audit">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="攻略标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="作者 ID">
          <el-input v-model="filters.authorId" placeholder="作者用户 ID" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="guides" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回理由" min-width="180" show-overflow-tooltip />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="success"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="danger"
              @click="openReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadGuides"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="攻略详情" width="760px">
      <template v-if="detail">
        <div class="detail-header">
          <h3>{{ detail.title }}</h3>
          <el-tag :type="statusTagType(detail.status)" size="small">
            {{ statusLabel(detail.status) }}
          </el-tag>
        </div>
        <p class="detail-meta">
          作者：{{ detail.authorName || '-' }} · 目的地：{{ detail.destinationName || '-' }}
        </p>
        <el-alert
          v-if="detail.rejectReason"
          :title="'驳回理由：' + detail.rejectReason"
          type="error"
          :closable="false"
          show-icon
          class="detail-alert"
        />
        <div class="detail-content" v-html="sanitizeHtml(detail.content)"></div>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回攻略" width="480px">
      <p class="reject-target">攻略：{{ rejectTarget?.title }}</p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请填写驳回理由（必填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminGuides, getAdminGuide, approveGuide, rejectGuide } from '@/api/admin/guide'
import { sanitizeHtml } from '@/utils/sanitize'

const loading = ref(false)
const guides = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejecting = ref(false)

const filters = reactive({ status: 'PENDING', keyword: '', authorId: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusLabels = {
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回'
}

function statusLabel(status) {
  return statusLabels[status] || status || '-'
}

function statusTagType(status) {
  return {
    DRAFT: 'info',
    PENDING: 'warning',
    PUBLISHED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0] = value
    const pad = n => String(n).padStart(2, '0')
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}`
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

function buildParams() {
  const params = { page: pagination.page, size: pagination.size }
  if (filters.status) params.status = filters.status
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.authorId) params.authorId = filters.authorId
  return params
}

async function loadGuides() {
  loading.value = true
  try {
    const res = await getAdminGuides(buildParams())
    guides.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    guides.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadGuides()
}

function handleReset() {
  filters.status = 'PENDING'
  filters.keyword = ''
  filters.authorId = ''
  pagination.page = 1
  loadGuides()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadGuides()
}

async function openDetail(row) {
  try {
    const res = await getAdminGuide(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    // 错误已由请求拦截器提示
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确定通过攻略「${row.title}」吗？`, '通过审核', {
      confirmButtonText: '确定通过',
      cancelButtonText: '取消',
      type: 'success'
    })
  } catch {
    return
  }
  try {
    await approveGuide(row.id)
    ElMessage.success('已通过审核')
    loadGuides()
  } catch {
    // 错误已由请求拦截器提示
  }
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  rejecting.value = true
  try {
    await rejectGuide(rejectTarget.value.id, rejectReason.value.trim())
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadGuides()
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    rejecting.value = false
  }
}

onMounted(loadGuides)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.table-card {
  border-radius: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.detail-header h3 {
  margin: 0;
}

.detail-meta {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-content {
  line-height: 1.8;
  max-height: 55vh;
  overflow-y: auto;
}

.reject-target {
  color: #606266;
  margin: 0 0 12px;
}
</style>
