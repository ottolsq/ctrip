<template>
  <div class="user-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="用户名 / 邮箱 / 手机号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="未验证" value="UNVERIFIED" />
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已封禁" value="SUSPENDED" />
            <el-option label="已删除" value="DELETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="全部" clearable style="width: 160px">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="内容运营" value="CONTENT_OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canBan(row)"
              link
              type="danger"
              @click="handleBan(row)"
            >
              封禁
            </el-button>
            <el-button
              v-if="row.status === 'SUSPENDED'"
              link
              type="success"
              @click="handleUnban(row)"
            >
              解封
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
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="用户详情" width="520px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleLabel(detail.role) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="封禁前状态">
          {{ detail.statusBeforeSuspend ? statusLabel(detail.statusBeforeSuspend) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="攻略数">{{ detail.guideCount }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ detail.orderCount }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, getUser, banUser, unbanUser } from '@/api/admin/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const users = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const filters = reactive({
  keyword: '',
  status: '',
  role: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const roleLabels = {
  USER: '普通用户',
  ADMIN: '管理员',
  CONTENT_OPERATOR: '内容运营'
}

const statusLabels = {
  UNVERIFIED: '未验证',
  ACTIVE: '正常',
  SUSPENDED: '已封禁',
  DELETED: '已删除'
}

function roleLabel(role) {
  return roleLabels[role] || role || '-'
}

function roleTagType(role) {
  return role === 'ADMIN' ? 'danger' : role === 'CONTENT_OPERATOR' ? 'warning' : 'info'
}

function statusLabel(status) {
  return statusLabels[status] || status || '-'
}

function statusTagType(status) {
  return status === 'ACTIVE' ? 'success' : status === 'SUSPENDED' ? 'danger' : 'info'
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
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.status) params.status = filters.status
  if (filters.role) params.role = filters.role
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  return params
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers(buildParams())
    users.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    users.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadUsers()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  filters.role = ''
  filters.dateRange = []
  pagination.page = 1
  loadUsers()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadUsers()
}

function canBan(row) {
  return row.role !== 'ADMIN'
    && row.status !== 'DELETED'
    && row.id !== userStore.userId
}

async function handleBan(row) {
  try {
    await ElMessageBox.confirm(
      `确定封禁用户「${row.username}」吗？封禁后其登录状态将立即失效。`,
      '封禁用户',
      { confirmButtonText: '确定封禁', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await banUser(row.id)
    ElMessage.success('用户已封禁')
    await loadUsers()
  } catch {
    // 错误已由请求拦截器提示
  }
}

async function handleUnban(row) {
  try {
    await ElMessageBox.confirm(
      `确定解封用户「${row.username}」吗？`,
      '解封用户',
      { confirmButtonText: '确定解封', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await unbanUser(row.id)
    ElMessage.success('用户已解封')
    await loadUsers()
  } catch {
    // 错误已由请求拦截器提示
  }
}

async function openDetail(row) {
  try {
    const res = await getUser(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadUsers)
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
