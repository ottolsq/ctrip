<template>
  <div class="open-box-page">
    <div class="page-header">
      <h2>开启盲盒</h2>
      <p>点击按钮，开启你的专属旅行惊喜</p>
    </div>

    <div class="box-container" v-loading="loading" element-loading-text="加载中...">
      <div class="box-scene">
        <div class="blind-box" :class="{ shaking: isShaking, opening: isOpening, opened: isOpened }">
          <div class="box-lid">
            <div class="lid-face lid-top"></div>
            <div class="lid-face lid-front"></div>
            <div class="lid-face lid-back"></div>
            <div class="lid-face lid-left"></div>
            <div class="lid-face lid-right"></div>
          </div>
          <div class="box-body">
            <div class="body-face body-front">
              <div class="box-decoration">
                <div class="ribbon ribbon-h"></div>
                <div class="ribbon ribbon-v"></div>
                <div class="bow"></div>
              </div>
              <div class="box-content">
                <div class="box-logo">?</div>
                <div class="box-text">旅行盲盒</div>
              </div>
            </div>
            <div class="body-face body-back"></div>
            <div class="body-face body-left"></div>
            <div class="body-face body-right"></div>
            <div class="body-face body-bottom"></div>
          </div>
          <div class="glow-effect" v-if="isOpening || isOpened"></div>
          <div class="particles" v-if="isOpening || isOpened">
            <span v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></span>
          </div>
        </div>
      </div>

      <div class="result-panel" v-if="isOpened && result">
        <div class="result-card">
          <div class="result-header">
            <div class="result-icon">
              <el-icon :size="36" color="#fff"><Box /></el-icon>
            </div>
            <h3>恭喜你！</h3>
            <p>你的专属旅行盲盒已开启</p>
          </div>
          <div class="result-content">
            <div class="result-item">
              <span class="label">
                <el-icon><Location /></el-icon>
                目的地
              </span>
              <span class="value">{{ result.destination || '未知惊喜' }}</span>
            </div>
            <div class="result-item" v-if="result.theme">
              <span class="label">
                <el-icon><CollectionTag /></el-icon>
                主题
              </span>
              <span class="value">{{ result.theme }}</span>
            </div>
            <div class="result-item" v-if="result.days">
              <span class="label">
                <el-icon><Clock /></el-icon>
                行程天数
              </span>
              <span class="value">{{ result.days }} 天</span>
            </div>
            <div class="result-item" v-if="result.price">
              <span class="label">
                <el-icon><Wallet /></el-icon>
                价值
              </span>
              <span class="value price">¥{{ result.price }}</span>
            </div>
          </div>
          <div class="result-actions">
            <el-button type="primary" size="large" @click="goToResult" class="result-btn">
              查看完整行程
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="action-area" v-if="!isOpened">
        <el-button
          type="primary"
          size="large"
          :disabled="isShaking || isOpening"
          :loading="isOpening"
          @click="handleOpenBox"
          class="open-btn"
        >
          <el-icon class="el-icon--left"><Box /></el-icon>
          {{ isShaking ? '摇晃中...' : isOpening ? '开启中...' : '开启盲盒' }}
        </el-button>
        <p class="tip" v-if="orderInfo">
          {{ orderInfo.templateName || '旅行盲盒' }} · 订单号: {{ orderNo }}
        </p>
        <p class="tip" v-else>
          订单号: {{ orderNo }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBlindBoxOrder, openBlindBox } from '@/api/blindbox'
import { Box, ArrowRight, Location, Clock, Wallet, CollectionTag } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const orderNo = route.params.orderNo
const loading = ref(false)
const isShaking = ref(false)
const isOpening = ref(false)
const isOpened = ref(false)
const result = ref(null)
const orderInfo = ref(null)

function getParticleStyle(i) {
  const angle = (i * 18) * Math.PI / 180
  const distance = 80 + Math.random() * 60
  const x = Math.cos(angle) * distance
  const y = Math.sin(angle) * distance
  const delay = (i - 1) * 0.05
  const size = 4 + Math.random() * 6
  const colors = ['#ffd700', '#ff6b6b', '#ff8e53', '#ffeb3b', '#ff9800']
  const color = colors[Math.floor(Math.random() * colors.length)]
  return {
    '--tx': `${x}px`,
    '--ty': `${y}px`,
    '--delay': `${delay}s`,
    '--size': `${size}px`,
    '--color': color
  }
}

async function loadOrderInfo() {
  loading.value = true
  try {
    const res = await getBlindBoxOrder(orderNo)
    orderInfo.value = res.data
  } catch (e) {
    // 静默失败，不影响开盒
  } finally {
    loading.value = false
  }
}

async function handleOpenBox() {
  if (isShaking.value || isOpening.value) return

  isShaking.value = true

  await new Promise(resolve => setTimeout(resolve, 1500))

  isShaking.value = false
  isOpening.value = true

  try {
    const res = await openBlindBox(orderNo)
    result.value = res.data

    await new Promise(resolve => setTimeout(resolve, 1800))

    isOpened.value = true
  } catch (e) {
    isOpening.value = false
    ElMessage.error(e.message || '开盒失败，请重试')
  }
}

function goToResult() {
  router.push(`/blind-box/orders/${orderNo}/result`)
}

onMounted(() => {
  loadOrderInfo()
})
</script>

<style scoped>
.open-box-page {
  min-height: calc(100vh - 200px);
  padding: 20px 0 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(180deg, #fff5f5 0%, #fff 30%);
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: #999;
}

.box-container {
  width: 100%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.box-scene {
  width: 300px;
  height: 350px;
  perspective: 1200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.blind-box {
  position: relative;
  width: 180px;
  height: 160px;
  transform-style: preserve-3d;
  transform: rotateX(-20deg) rotateY(-25deg);
}

.blind-box.shaking {
  animation: shake 0.12s ease-in-out infinite;
}

.blind-box.opening {
  animation: float 2.5s ease-in-out infinite;
}

.blind-box.opened {
  animation: float 2.5s ease-in-out infinite;
}

@keyframes shake {
  0%, 100% { transform: rotateX(-20deg) rotateY(-25deg) translateX(0) rotateZ(0); }
  20% { transform: rotateX(-20deg) rotateY(-25deg) translateX(-6px) rotateZ(-4deg); }
  40% { transform: rotateX(-20deg) rotateY(-25deg) translateX(6px) rotateZ(4deg); }
  60% { transform: rotateX(-20deg) rotateY(-25deg) translateX(-4px) rotateZ(-2deg); }
  80% { transform: rotateX(-20deg) rotateY(-25deg) translateX(4px) rotateZ(2deg); }
}

@keyframes float {
  0%, 100% { transform: rotateX(-20deg) rotateY(-25deg) translateY(0); }
  50% { transform: rotateX(-20deg) rotateY(-25deg) translateY(-15px); }
}

.box-lid {
  position: absolute;
  top: -25px;
  left: 0;
  width: 180px;
  height: 35px;
  transform-style: preserve-3d;
  transform-origin: bottom center;
  transition: transform 0.9s cubic-bezier(0.68, -0.6, 0.32, 1.6);
  z-index: 10;
}

.blind-box.opening .box-lid,
.blind-box.opened .box-lid {
  transform: rotateX(-130deg) translateY(-25px) translateZ(35px);
}

.lid-face {
  position: absolute;
}

.lid-top {
  width: 180px;
  height: 35px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  transform: rotateX(90deg) translateZ(17.5px);
  border-radius: 4px;
}

.lid-front {
  width: 180px;
  height: 25px;
  background: linear-gradient(180deg, #ff6b6b, #ee5a5a);
  transform: translateZ(90px) translateY(10px);
  border-radius: 4px 4px 0 0;
}

.lid-back {
  width: 180px;
  height: 25px;
  background: linear-gradient(180deg, #cc5555, #bb4444);
  transform: rotateY(180deg) translateZ(90px) translateY(10px);
  border-radius: 4px 4px 0 0;
}

.lid-left {
  width: 35px;
  height: 25px;
  background: linear-gradient(90deg, #dd5555, #ff6b6b);
  transform: rotateY(-90deg) translateZ(17.5px) translateX(-90px) translateY(10px);
  border-radius: 4px 0 0 0;
}

.lid-right {
  width: 35px;
  height: 25px;
  background: linear-gradient(90deg, #ff6b6b, #dd5555);
  transform: rotateY(90deg) translateZ(162.5px) translateX(-90px) translateY(10px);
  border-radius: 0 4px 0 0;
}

.box-body {
  position: relative;
  width: 180px;
  height: 160px;
  transform-style: preserve-3d;
}

.body-face {
  position: absolute;
}

.body-front {
  width: 180px;
  height: 160px;
  background: linear-gradient(180deg, #ff8e53, #ff6b6b);
  transform: translateZ(17.5px);
  border-radius: 0 0 10px 10px;
  overflow: hidden;
}

.body-back {
  width: 180px;
  height: 160px;
  background: linear-gradient(180deg, #cc6644, #aa4433);
  transform: rotateY(180deg) translateZ(17.5px);
  border-radius: 0 0 10px 10px;
}

.body-left {
  width: 35px;
  height: 160px;
  background: linear-gradient(90deg, #dd6644, #ff8e53);
  transform: rotateY(-90deg) translateZ(0) translateX(-90px);
  border-radius: 0 0 0 10px;
}

.body-right {
  width: 35px;
  height: 160px;
  background: linear-gradient(90deg, #ff6b6b, #cc5544);
  transform: rotateY(90deg) translateZ(180px) translateX(-90px);
  border-radius: 0 0 10px 0;
}

.body-bottom {
  width: 180px;
  height: 35px;
  background: #993322;
  transform: rotateX(-90deg) translateZ(142.5px);
  border-radius: 10px;
}

.box-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.ribbon-h {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 20px;
  background: linear-gradient(90deg, #ffd700, #ffeb3b, #ffd700);
  transform: translateY(-50%);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.ribbon-v {
  position: absolute;
  top: 0;
  left: 50%;
  width: 20px;
  height: 100%;
  background: linear-gradient(180deg, #ffd700, #ffeb3b, #ffd700);
  transform: translateX(-50%);
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
}

.bow {
  position: absolute;
  top: -5px;
  left: 50%;
  width: 50px;
  height: 35px;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, #ffd700 0%, #ff9800 100%);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.bow::before,
.bow::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 25px;
  height: 30px;
  background: radial-gradient(ellipse at center, #ffd700 0%, #ff9800 100%);
  border-radius: 50%;
  transform: translateY(-50%);
}

.bow::before {
  left: -18px;
  transform: translateY(-50%) rotate(-20deg);
}

.bow::after {
  right: -18px;
  transform: translateY(-50%) rotate(20deg);
}

.box-content {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.box-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 30% 20%, rgba(255,255,255,0.25), transparent 60%);
  pointer-events: none;
  z-index: 1;
}

.box-logo {
  font-size: 52px;
  font-weight: 900;
  color: #fff;
  text-shadow: 2px 2px 10px rgba(0, 0, 0, 0.2);
  margin-bottom: 6px;
  animation: pulse 2s ease-in-out infinite;
  position: relative;
  z-index: 2;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

.box-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 3px;
  text-shadow: 1px 1px 4px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 2;
}

.glow-effect {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 350px;
  height: 350px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 215, 0, 0.5) 0%, rgba(255, 152, 0, 0.3) 35%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: glowBurst 2s ease-out forwards;
  z-index: -1;
}

@keyframes glowBurst {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.2);
  }
  40% {
    opacity: 1;
  }
  100% {
    opacity: 0.5;
    transform: translate(-50%, -50%) scale(1.8);
  }
}

.particles {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  pointer-events: none;
  z-index: 5;
}

.particle {
  position: absolute;
  width: var(--size);
  height: var(--size);
  background: var(--color);
  border-radius: 50%;
  animation: particleFly 1.8s ease-out forwards;
  animation-delay: var(--delay);
  box-shadow: 0 0 6px var(--color);
}

@keyframes particleFly {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0);
  }
  30% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(var(--tx), var(--ty)) scale(1);
  }
}

.result-panel {
  width: 100%;
  margin-bottom: 20px;
  animation: slideUp 0.7s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-card {
  background: #fff;
  border-radius: 20px;
  padding: 28px 24px;
  box-shadow: 0 10px 40px rgba(255, 107, 107, 0.15);
  border: 1px solid rgba(255, 107, 107, 0.1);
}

.result-header {
  text-align: center;
  margin-bottom: 24px;
}

.result-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(255, 107, 107, 0.4);
  animation: iconBounce 0.8s ease-out;
}

@keyframes iconBounce {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.result-header h3 {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 6px;
}

.result-header p {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.result-content {
  background: #fafafa;
  border-radius: 12px;
  padding: 8px 16px;
  margin-bottom: 24px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.result-item:last-child {
  border-bottom: none;
}

.result-item .label {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-item .label .el-icon {
  color: #ff6b6b;
}

.result-item .value {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.result-item .value.price {
  color: #ff6b6b;
  font-size: 18px;
}

.result-actions {
  text-align: center;
}

.result-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 24px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  border: none;
  box-shadow: 0 4px 16px rgba(255, 107, 107, 0.4);
}

.result-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.5);
}

.action-area {
  text-align: center;
  width: 100%;
}

.open-btn {
  width: 220px;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 26px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  border: none;
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.4);
  transition: all 0.3s ease;
}

.open-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(255, 107, 107, 0.5);
}

.open-btn:active {
  transform: translateY(-1px);
}

.open-btn.is-disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none !important;
}

.tip {
  margin-top: 18px;
  font-size: 13px;
  color: #aaa;
}
</style>
