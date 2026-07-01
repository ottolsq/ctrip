<template>
  <div class="order-create-page" v-loading="loading">
    <div class="page-header">
      <h2>确认订单</h2>
      <p>请确认以下信息并提交订单</p>
    </div>

    <el-row :gutter="24">
      <el-col :md="16" :sm="24">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>盲盒信息</h3>
            </div>
          </template>
          <div v-if="template" class="template-info">
            <div class="template-cover">
              <el-image :src="template.coverUrl" fit="cover" class="cover-img">
                <template #error>
                  <div class="img-placeholder">
                    <el-icon :size="40"><Box /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="template-detail">
              <h3 class="template-name">{{ template.name }}</h3>
              <p class="template-desc">{{ template.ruleConfig?.description || template.description || '暂无描述' }}</p>
              <div v-if="template.type === 'DAILY'" class="template-tag daily">日常盲盒</div>
              <div v-else-if="template.type === 'LIMITED'" class="template-tag limited">限定盲盒</div>
            </div>
          </div>
        </el-card>

        <el-card class="form-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>旅行偏好</h3>
            </div>
          </template>
          <el-form
            ref="formRef"
            :model="form"
            :rules="formRules"
            label-width="100px"
            size="large"
          >
            <el-form-item label="出发城市" prop="departureCity">
              <el-input
                v-model="form.departureCity"
                placeholder="请输入出发城市"
                clearable
              />
            </el-form-item>

            <el-form-item label="预算等级" prop="budgetLevel">
              <el-select
                v-model="form.budgetLevel"
                placeholder="请选择预算等级"
                clearable
              >
                <el-option label="经济型" value="ECONOMY" />
                <el-option label="标准型" value="STANDARD" />
                <el-option label="豪华型" value="LUXURY" />
              </el-select>
            </el-form-item>

            <el-form-item label="旅行主题" prop="theme">
              <el-input
                v-model="form.theme"
                placeholder="选填，如：美食、海滨、古镇等"
                clearable
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :md="8" :sm="24">
        <el-card class="price-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>价格明细</h3>
            </div>
          </template>
          <div v-if="template" class="price-detail">
            <div class="price-item">
              <span class="price-label">盲盒价格</span>
              <span class="price-value">¥{{ template.price }}</span>
            </div>
            <el-divider />
            <div class="price-total">
              <span class="total-label">应付金额</span>
              <span class="total-value">¥{{ template.price }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            提交订单
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlindBoxTemplate, createBlindBoxOrder } from '@/api/blindbox'
import { Box } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const submitLoading = ref(false)
const template = ref(null)

const form = reactive({
  departureCity: '',
  budgetLevel: '',
  theme: ''
})

const formRules = {
  departureCity: [
    { required: true, message: '请输入出发城市', trigger: 'blur' },
    { min: 2, max: 20, message: '城市名称长度为2-20个字符', trigger: 'blur' }
  ],
  budgetLevel: [
    { required: true, message: '请选择预算等级', trigger: 'change' }
  ]
}

const loadTemplate = async () => {
  const id = route.params.id
  if (!id) {
    ElMessage.error('模板ID不存在')
    return
  }

  loading.value = true
  try {
    const res = await getBlindBoxTemplate(id)
    template.value = res.data
  } catch (error) {
    console.error('[OrderCreate] loadTemplate error:', error)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const res = await createBlindBoxOrder({
      templateId: parseInt(route.params.id),
      departureCity: form.departureCity,
      budgetLevel: form.budgetLevel,
      theme: form.theme || undefined
    })

    const orderNo = res.data?.orderNo
    if (orderNo) {
      ElMessage.success('订单创建成功')
      router.push(`/blind-box/orders/${orderNo}/pay`)
    } else {
      ElMessage.success('订单创建成功')
      router.push('/blind-box/my')
    }
  } catch (error) {
    console.error('[OrderCreate] handleSubmit error:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadTemplate()
})
</script>

<style scoped>
.order-create-page {
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

.info-card,
.form-card {
  margin-bottom: 24px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.template-info {
  display: flex;
  gap: 20px;
}

.template-cover {
  width: 160px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.img-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}

.template-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.template-name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.template-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.template-tag {
  display: inline-block;
  color: #fff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  align-self: flex-start;
}

.template-tag.daily {
  background: linear-gradient(135deg, #409eff, #667eea);
}

.template-tag.limited {
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
}

.price-card {
  position: sticky;
  top: 20px;
}

.price-detail {
  padding: 8px 0;
}

.price-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
}

.price-item .price-label {
  color: #666;
}

.price-item .price-value {
  color: #333;
}

.price-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.total-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.total-value {
  font-size: 26px;
  font-weight: 700;
  color: #ff6b6b;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .template-info {
    flex-direction: column;
  }

  .template-cover {
    width: 100%;
    height: 180px;
  }

  .price-card {
    position: static;
  }
}
</style>
