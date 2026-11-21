<template>
  <div class="comment-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="攻略 ID">
          <el-input v-model="filters.guideId" clearable placeholder="攻略 ID" style="width: 120px" />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input v-model="filters.userId" clearable placeholder="用户 ID" style="width: 120px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="评论内容"
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="comments" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="guideTitle" label="攻略" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userName" label="评论者" width="120" />
        <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
          @current-change="loadComments"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminComments, deleteAdminComment } from '@/api/admin/comment'

const loading = ref(false)
const comments = ref([])
const filters = reactive({ guideId: '', userId: '', keyword: '', dateRange: [] })
const pagination = reactive({ page: 1, size: 10, total: 0 })

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
  if (filters.guideId) params.guideId = filters.guideId
  if (filters.userId) params.userId = filters.userId
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

async function loadComments() {
  loading.value = true
  try {
    const res = await getAdminComments(buildParams())
    comments.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    comments.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadComments()
}

function handleReset() {
  filters.guideId = ''
  filters.userId = ''
  filters.keyword = ''
  filters.dateRange = []
  pagination.page = 1
  loadComments()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadComments()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除该评论吗？子回复会一并删除。\n「${row.content}」`,
      '删除评论',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteAdminComment(row.id)
    ElMessage.success('评论已删除')
    loadComments()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadComments)
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
</style>
