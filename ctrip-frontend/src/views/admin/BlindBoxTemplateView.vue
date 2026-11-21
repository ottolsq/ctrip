<template>
  <div class="blind-box-template">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="盲盒名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="日常盲盒" value="DAILY" />
            <el-option label="限定盲盒" value="LIMITED" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架" value="ACTIVE" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreate">新建模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="templates" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type === 'LIMITED' ? 'warning' : 'success'" size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            {{ row.stock === -1 ? '不限量' : row.stock }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.limit"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadTemplates"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑模板' : '新建模板'"
      width="560px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入盲盒名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="DAILY">日常盲盒</el-radio>
            <el-radio value="LIMITED">限定盲盒</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item v-if="form.type === 'LIMITED'" label="库存" prop="stock">
          <el-input-number
            v-model="form.stock"
            :min="form.id ? 0 : 1"
            :step="1"
            step-strictly
          />
          <span class="form-hint">售罄后可改名或下架，不可将库存改为负数</span>
        </el-form-item>
        <el-form-item v-else label="库存">
          <el-input model-value="不限量（-1）" disabled />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">上架</el-radio>
            <el-radio value="INACTIVE">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.type === 'LIMITED'">
          <el-form-item label="活动时间" prop="activityRange">
            <el-date-picker
              v-model="form.activityRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              format="YYYY-MM-DD HH:mm"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="活动描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="选填，展示在活动规则中"
            />
          </el-form-item>
          <el-form-item label="折扣">
            <el-input-number v-model="form.discount" :min="0" :max="10" :precision="1" :step="0.5" />
            <span class="form-hint">选填，例如 8.5 表示 8.5 折</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate
} from '@/api/admin/blindbox'
import { parseRuleConfig, mergeRuleConfig } from '@/utils/businessRules'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const templates = ref([])
const formRef = ref()

const filters = reactive({
  keyword: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  limit: 10,
  total: 0
})

const form = reactive({
  id: null,
  name: '',
  type: 'DAILY',
  price: 99,
  stock: 10,
  originalStock: null,
  status: 'ACTIVE',
  activityRange: [],
  description: '',
  discount: null,
  originalRule: {}
})

const rules = {
  name: [{ required: true, message: '请输入盲盒名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  stock: [{
    validator: (rule, value, callback) => {
      if (form.type !== 'LIMITED') {
        callback()
        return
      }
      const min = form.id ? 0 : 1
      if (value === null || value === undefined || value < min) {
        callback(new Error(form.id ? '库存不能为负数' : '限定盲盒库存必须大于 0'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  activityRange: [{
    validator: (rule, value, callback) => {
      if (form.type === 'LIMITED' && (!value || value.length !== 2)) {
        callback(new Error('请选择活动开始和结束时间'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

function typeLabel(type) {
  return type === 'LIMITED' ? '限定盲盒' : '日常盲盒'
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
    limit: pagination.limit
  }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.type) params.type = filters.type
  if (filters.status) params.status = filters.status
  return params
}

async function loadTemplates() {
  loading.value = true
  try {
    const res = await getAdminTemplates(buildParams())
    templates.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    templates.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadTemplates()
}

function handleReset() {
  filters.keyword = ''
  filters.type = ''
  filters.status = ''
  pagination.page = 1
  loadTemplates()
}

function handleSizeChange(limit) {
  pagination.limit = limit
  pagination.page = 1
  loadTemplates()
}

function resetForm() {
  form.id = null
  form.name = ''
  form.type = 'DAILY'
  form.price = 99
  form.stock = 10
  form.originalStock = null
  form.status = 'ACTIVE'
  form.activityRange = []
  form.description = ''
  form.discount = null
  form.originalRule = {}
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  const rule = parseRuleConfig(row.ruleConfig)
  form.id = row.id
  form.name = row.name
  form.type = row.type
  form.price = Number(row.price)
  form.stock = row.type === 'LIMITED' ? row.stock : 10
  form.originalStock = row.stock
  form.status = row.status
  form.activityRange = rule.activityStartTime && rule.activityEndTime
    ? [rule.activityStartTime, rule.activityEndTime]
    : []
  form.description = rule.description || ''
  form.discount = rule.discount ?? null
  form.originalRule = rule
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  resetForm()
}

function buildPayload() {
  const payload = {
    name: form.name,
    type: form.type,
    price: form.price,
    status: form.status
  }
  if (form.type === 'LIMITED') {
    // 编辑时库存未变化则不提交，避免用页面打开时的陈旧库存覆盖并发扣减
    if (!form.id || form.stock !== form.originalStock) {
      payload.stock = form.stock
    }
    const ruleConfig = mergeRuleConfig(
      form.originalRule,
      form.activityRange[0],
      form.activityRange[1],
      form.description,
      form.discount
    )
    payload.ruleConfig = JSON.stringify(ruleConfig)
  }
  return payload
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await updateTemplate(form.id, payload)
      ElMessage.success('模板已更新')
    } else {
      await createTemplate(payload)
      ElMessage.success('模板已创建')
    }
    dialogVisible.value = false
    await loadTemplates()
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除模板「${row.name}」吗？存在关联订单时将无法删除，建议改为下架。`,
      '删除模板',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteTemplate(row.id)
    ElMessage.success('模板已删除')
    await loadTemplates()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(loadTemplates)
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

.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
