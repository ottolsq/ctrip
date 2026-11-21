<template>
  <div class="banner-manage">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreate">新建轮播图</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="banners" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="110">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl"
              fit="cover"
              class="banner-thumb"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
            >
              <template #error>
                <div class="thumb-placeholder"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="跳转地址" min-width="160">
          <template #default="{ row }">{{ row.linkUrl || '-' }}</template>
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
          @current-change="loadBanners"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑轮播图' : '新建轮播图'"
      width="560px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" placeholder="请输入轮播标题" />
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" maxlength="500" placeholder="https://..." />
          <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover" class="image-preview">
            <template #error>
              <div class="thumb-placeholder">图片无法加载</div>
            </template>
          </el-image>
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.linkUrl" maxlength="500" placeholder="站内路径，如 /destinations" />
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
  getAdminBanners,
  createBanner,
  updateBanner,
  deleteBanner
} from '@/api/admin/banner'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const banners = ref([])
const formRef = ref()

const filters = reactive({ status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const form = reactive({
  id: null,
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 'ENABLED',
  startAt: null,
  endAt: null
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片地址', trigger: 'blur' }]
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
  if (filters.status) params.status = filters.status
  return params
}

async function loadBanners() {
  loading.value = true
  try {
    const res = await getAdminBanners(buildParams())
    banners.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    banners.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadBanners()
}

function handleReset() {
  filters.status = ''
  pagination.page = 1
  loadBanners()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadBanners()
}

function resetForm() {
  form.id = null
  form.title = ''
  form.imageUrl = ''
  form.linkUrl = ''
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
  form.title = row.title
  form.imageUrl = row.imageUrl
  form.linkUrl = row.linkUrl || ''
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
    title: form.title,
    imageUrl: form.imageUrl,
    linkUrl: form.linkUrl || null,
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
      await updateBanner(form.id, buildPayload())
      ElMessage.success('轮播图已更新')
    } else {
      await createBanner(buildPayload())
      ElMessage.success('轮播图已创建')
    }
    dialogVisible.value = false
    await loadBanners()
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除轮播图「${row.title}」吗？`,
      '删除轮播图',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteBanner(row.id)
    ElMessage.success('轮播图已删除')
    await loadBanners()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadBanners)
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

.banner-thumb {
  width: 84px;
  height: 44px;
  border-radius: 6px;
}

.thumb-placeholder {
  width: 100%;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}

.image-preview {
  margin-top: 8px;
  width: 100%;
  max-height: 140px;
  border-radius: 6px;
}

.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
