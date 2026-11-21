<template>
  <div class="recommendation-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="位置">
          <el-input
            v-model="filters.slot"
            placeholder="如 home_featured"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreate">新建推荐位</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="recommendations" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="slot" label="位置" min-width="140" />
        <el-table-column prop="title" label="展示标题" min-width="160" />
        <el-table-column label="目标" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ targetTypeLabel(row.targetType) }}</el-tag>
            <span class="target-id">ID: {{ row.targetId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示时间" min-width="230">
          <template #default="{ row }">
            {{ formatDateTime(row.startAt) }} ~ {{ formatDateTime(row.endAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
          @current-change="loadRecommendations"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑推荐位' : '新建推荐位'"
      width="560px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="位置" prop="slot">
          <el-input v-model="form.slot" maxlength="50" placeholder="如 home_featured" />
        </el-form-item>
        <el-form-item label="展示标题" prop="title">
          <el-input v-model="form.title" maxlength="100" placeholder="首页展示的标题" />
        </el-form-item>
        <el-form-item label="目标类型" prop="targetType">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option label="攻略" value="GUIDE" />
            <el-option label="目的地" value="DESTINATION" />
            <el-option label="盲盒模板" value="BLIND_BOX_TEMPLATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 ID" prop="targetId">
          <el-input-number v-model="form.targetId" :min="1" :step="1" step-strictly />
          <span class="form-hint">攻略/目的地/模板的实际 ID</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :step="1" step-strictly />
          <span class="form-hint">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            placeholder="留空表示不限制"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            placeholder="留空表示不限制"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminRecommendations,
  createRecommendation,
  updateRecommendation,
  deleteRecommendation
} from '@/api/admin/recommendation'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const recommendations = ref([])
const formRef = ref()

const filters = reactive({ slot: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const form = reactive({
  id: null,
  slot: 'home_featured',
  title: '',
  targetType: 'DESTINATION',
  targetId: null,
  sortOrder: 0,
  status: 'ENABLED',
  startAt: null,
  endAt: null
})

const rules = {
  slot: [{ required: true, message: '请输入位置标识', trigger: 'blur' }],
  title: [{ required: true, message: '请输入展示标题', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请输入目标 ID', trigger: 'change' }]
}

function targetTypeLabel(type) {
  return {
    GUIDE: '攻略',
    DESTINATION: '目的地',
    BLIND_BOX_TEMPLATE: '盲盒模板'
  }[type] || type || '-'
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

function toIsoString(value) {
  if (!value) return null
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value
    const pad = n => String(n).padStart(2, '0')
    return `${y}-${pad(m)}-${pad(d)}T${pad(h)}:${pad(min)}:${pad(s)}`
  }
  return String(value)
}

function buildParams() {
  const params = { page: pagination.page, size: pagination.size }
  if (filters.slot) params.slot = filters.slot
  if (filters.status) params.status = filters.status
  return params
}

async function loadRecommendations() {
  loading.value = true
  try {
    const res = await getAdminRecommendations(buildParams())
    recommendations.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    recommendations.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadRecommendations()
}

function handleReset() {
  filters.slot = ''
  filters.status = ''
  pagination.page = 1
  loadRecommendations()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadRecommendations()
}

function resetForm() {
  form.id = null
  form.slot = 'home_featured'
  form.title = ''
  form.targetType = 'DESTINATION'
  form.targetId = null
  form.sortOrder = 0
  form.status = 'ENABLED'
  form.startAt = null
  form.endAt = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  form.id = row.id
  form.slot = row.slot
  form.title = row.title
  form.targetType = row.targetType
  form.targetId = row.targetId
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status
  form.startAt = toIsoString(row.startAt)
  form.endAt = toIsoString(row.endAt)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  resetForm()
}

function buildPayload() {
  return {
    slot: form.slot,
    title: form.title,
    targetType: form.targetType,
    targetId: form.targetId,
    sortOrder: form.sortOrder ?? 0,
    status: form.status,
    startAt: form.startAt || null,
    endAt: form.endAt || null
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateRecommendation(form.id, buildPayload())
      ElMessage.success('推荐位已更新')
    } else {
      await createRecommendation(buildPayload())
      ElMessage.success('推荐位已创建')
    }
    dialogVisible.value = false
    await loadRecommendations()
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除推荐位「${row.title}」吗？`,
      '删除推荐位',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteRecommendation(row.id)
    ElMessage.success('推荐位已删除')
    await loadRecommendations()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadRecommendations)
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

.target-id {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
