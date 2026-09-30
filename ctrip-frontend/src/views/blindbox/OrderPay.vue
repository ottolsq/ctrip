<template>
  <div class="order-pay-page" v-loading="loading">
    <div class="pay-container">
      <div class="pay-header">
        <el-icon :size="48" class="pay-icon"><Wallet /></el-icon>
        <h2>订单支付</h2>
        <p class="pay-desc">请在倒计时内完成支付，超时订单将自动取消</p>
      </div>

      <div class="order-info-card">
        <div class="info-row">
          <span class="label">盲盒名称</span>
          <span class="value">{{ orderInfo.templateName || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="label">订单号</span>
          <span class="value order-no">{{ orderInfo.orderNo || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="label">支付金额</span>
          <span class="value price">¥{{ orderInfo.payAmount || '0.00' }}</span>
        </div>
        <div class="countdown-row" v-if="countdown > 0">
          <el-icon><Clock /></el-icon>
          <span>支付剩余时间：</span>
          <span class="countdown-time">{{ formatCountdown }}</span>
        </div>
      </div>

      <div class="pay-methods">
        <h3 class="section-title">选择支付方式</h3>
        <div class="method-list">
          <div
            v-for="method in payMethods"
            :key="method.value"
            class="method-item"
            :class="{ active: payMethod === method.value }"
            @click="payMethod = method.value"
          >
            <div class="method-left">
              <div class="method-icon" :class="method.iconClass">
                <el-icon :size="24"><component :is="method.icon" /></el-icon>
              </div>
              <span class="method-name">{{ method.label }}</span>
            </div>
            <el-radio :model-value="payMethod" :value="method.value" />
          </div>
        </div>
      </div>

      <div class="pay-actions">
        <el-button
          type="primary"
          size="large"
          class="pay-btn"
          :loading="paying"
          :disabled="countdown === 0"
          @click="handlePay"
        >
          立即支付 ¥{{ orderInfo.payAmount || '0.00' }}
        </el-button>
        <el-button
          size="large"
          class="cancel-btn"
          :loading="canceling"
          @click="handleCancel"
        >
          取消订单
        </el-button>
      </div>

      <div class="pay-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>支付成功后将自动跳转至开盒页面</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Clock, InfoFilled } from '@element-plus/icons-vue'
import { getBlindBoxOrder, cancelBlindBoxOrder, blindBoxPayCallback } from '@/api/blindbox'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const canceling = ref(false)
const payMethod = ref('WECHAT_PAY')
const countdown = ref(900)

const orderInfo = reactive({
  orderNo: '',
  templateName: '',
  payAmount: ''
})

const payMethods = [
  {
    value: 'WECHAT_PAY',
    label: '微信支付',
    icon: Wallet,
    iconClass: 'wechat'
  },
  {
    value: 'ALIPAY',
    label: '支付宝',
    icon: Wallet,
    iconClass: 'alipay'
  }
]

const formatCountdown = computed(() => {
  const minutes = Math.floor(countdown.value / 60)
  const seconds = countdown.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

let countdownTimer = null

function startCountdown() {
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(countdownTimer)
      ElMessage.warning('支付超时，订单已自动取消')
      router.replace('/blind-box/my')
    }
  }, 1000)
}

async function loadOrderInfo() {
  const orderNo = route.params.orderNo
  if (!orderNo) {
    ElMessage.error('订单号不存在')
    router.replace('/blind-box')
    return
  }

  loading.value = true
  try {
    const res = await getBlindBoxOrder(orderNo)
    const data = res.data
    orderInfo.orderNo = data.orderNo
    orderInfo.templateName = data.templateName || data.name
    orderInfo.payAmount = data.payAmount || data.price

    if (data.expireAt) {
      let expireTime
      if (Array.isArray(data.expireAt)) {
        const [year, month, day, hour, minute, second] = data.expireAt
        expireTime = new Date(year, month - 1, day, hour, minute, second).getTime()
      } else {
        expireTime = new Date(data.expireAt).getTime()
      }
      const now = Date.now()
      const remaining = Math.floor((expireTime - now) / 1000)
      countdown.value = Math.max(0, remaining)
    }
  } catch (err) {
    ElMessage.error(err.message || '获取订单信息失败')
    router.replace('/blind-box/my')
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  if (countdown.value === 0) {
    ElMessage.warning('支付已超时，请重新下单')
    return
  }

  paying.value = true
  try {
    await blindBoxPayCallback({
      orderNo: orderInfo.orderNo,
      payMethod: payMethod.value
    })
    ElMessage.success('支付成功')
    router.replace(`/blind-box/orders/${orderInfo.orderNo}/open`)
  } catch (error) {
    console.error('[OrderPay] handlePay error:', error)
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？',
      '取消订单',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  canceling.value = true
  try {
    await cancelBlindBoxOrder(orderInfo.orderNo)
    ElMessage.success('订单已取消')
    router.replace('/blind-box/my')
  } catch (err) {
    ElMessage.error(err.message || '取消订单失败')
  } finally {
    canceling.value = false
  }
}

onMounted(() => {
  loadOrderInfo()
  startCountdown()
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.order-pay-page {
  min-height: calc(100vh - 200px);
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  background: #f5f7fa;
}

.pay-container {
  width: 100%;
  max-width: 500px;
}

.pay-header {
  text-align: center;
  margin-bottom: 32px;
}

.pay-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.pay-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px;
}

.pay-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.order-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
}

.info-row:last-of-type {
  border-bottom: none;
}

.label {
  font-size: 14px;
  color: #666;
}

.value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.order-no {
  font-family: monospace;
}

.price {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
}

.countdown-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px 0 8px;
  font-size: 14px;
  color: #666;
}

.countdown-time {
  color: #ff6b6b;
  font-weight: 600;
  font-size: 16px;
}

.pay-methods {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px;
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.method-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.method-item:hover {
  border-color: #409eff;
}

.method-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.method-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.method-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.method-icon.wechat {
  background: #07c160;
}

.method-icon.alipay {
  background: #1677ff;
}

.method-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.pay-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.pay-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
}

.cancel-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

.pay-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
}

@media (max-width: 768px) {
  .order-pay-page {
    padding: 20px 12px;
  }

  .pay-header h2 {
    font-size: 24px;
  }
}
</style>
