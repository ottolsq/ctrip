<template>
  <div class="itinerary-editor">
    <div class="page-header">
      <h1>{{ isEdit ? '编辑行程' : '创建行程' }}</h1>
      <div class="header-actions">
        <el-button @click="router.push('/itineraries')">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存行程</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="行程标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入行程标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destinationId">
              <el-select v-model="form.destinationId" placeholder="选择目的地" filterable>
                <el-option
                  v-for="dest in destinations"
                  :key="dest.id"
                  :label="dest.name"
                  :value="dest.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="选择开始日期"
                value-format="YYYY-MM-DD"
                @change="handleDateChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="选择结束日期"
                :disabled-date="disabledEndDate"
                value-format="YYYY-MM-DD"
                @change="handleDateChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <div class="days-section">
      <div class="section-header">
        <h2>行程日程</h2>
        <el-button type="primary" size="small" @click="addDayItem">
          <el-icon><Plus /></el-icon>
          添加一天
        </el-button>
      </div>

      <div
        v-for="(day, dayIndex) in days"
        :key="day.id || dayIndex"
        class="day-section"
      >
        <el-card class="day-card">
          <div class="day-header" :class="{ dragging: dragDayIndex === dayIndex }">
            <div class="day-info">
              <span class="day-badge">第 {{ dayIndex + 1 }} 天</span>
              <el-input
                v-model="day.title"
                placeholder="输入当天主题（可选）"
                class="day-title-input"
              />
            </div>
            <div class="day-actions">
              <el-button
                v-if="days.length > 1"
                size="small"
                type="danger"
                @click="removeDay(dayIndex)"
              >
                删除
              </el-button>
            </div>
          </div>

          <div class="items-section">
            <div class="items-header">
              <span>行程安排</span>
              <el-button size="small" type="success" @click="addItemToDay(dayIndex)">
                <el-icon><Plus /></el-icon>
                添加行程项
              </el-button>
            </div>

            <div v-if="day.items?.length === 0" class="empty-items">
              <el-empty description="暂无安排，请添加行程项" :image-size="60" />
            </div>

            <div v-else class="items-list">
              <div
                v-for="(item, itemIndex) in day.items"
                :key="item.id || itemIndex"
                class="item-row"
                draggable="true"
                @dragstart="handleDragStart(dayIndex, itemIndex)"
                @dragover.prevent
                @drop="handleDrop(dayIndex, itemIndex)"
              >
                <div class="drag-handle">
                  <el-icon><Sort /></el-icon>
                </div>
                <div class="item-fields">
                  <el-row :gutter="12">
                    <el-col :span="5">
                      <el-select v-model="item.type" placeholder="类型" class="type-select">
                        <el-option label="景点" value="ATTRACTION" />
                        <el-option label="美食" value="FOOD" />
                        <el-option label="住宿" value="HOTEL" />
                        <el-option label="交通" value="TRANSPORT" />
                        <el-option label="活动" value="ACTIVITY" />
                      </el-select>
                    </el-col>
                    <el-col :span="6">
                      <el-input v-model="item.name" placeholder="名称" />
                    </el-col>
                    <el-col :span="4">
                      <el-input v-model="item.timeSlot" placeholder="时段" />
                    </el-col>
                    <el-col :span="6">
                      <el-input v-model="item.location" placeholder="地点" />
                    </el-col>
                    <el-col :span="2">
                      <el-button size="small" type="danger" @click="removeItem(dayIndex, itemIndex)">
                        删除
                      </el-button>
                    </el-col>
                  </el-row>
                  <el-row v-if="item.type === 'ATTRACTION'" :gutter="12" style="margin-top: 8px">
                    <el-col :span="22">
                      <el-input v-model="item.description" placeholder="备注说明（可选）" />
                    </el-col>
                  </el-row>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Sort } from '@element-plus/icons-vue'
import { getDestinations } from '@/api/destination'
import { createItinerary, updateItinerary, getItinerary, addDay, updateDay, deleteDay, addItem, updateItem } from '@/api/itinerary'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)

const destinations = ref([])

const form = ref({
  title: '',
  destinationId: null,
  startDate: '',
  endDate: ''
})

const isDataLoaded = ref(false) // 标记数据是否已加载完成

const days = ref([
  {
    id: null,
    title: '',
    items: []
  }
])

const dragDayIndex = ref(-1)
const dragItemIndex = ref(-1)

function disabledEndDate(time) {
  if (!form.value.startDate) return false
  return time.getTime() < new Date(form.value.startDate).getTime()
}

async function loadDestinations() {
  try {
    const res = await getDestinations({ current: 1, size: 100 })
    destinations.value = res.data.records || []
  } catch {
    destinations.value = []
  }
}

async function loadItinerary() {
  const id = route.params.id
  try {
    const res = await getItinerary(id)
    const data = res.data
    form.value = {
      title: data.title,
      destinationId: data.destinationId,
      startDate: formatDateForInput(data.startDate),
      endDate: formatDateForInput(data.endDate)
    }
    days.value = data.days?.map(d => ({
      id: d.id,
      title: d.title,
      items: d.items?.map(i => ({
        id: i.id,
        type: i.type,
        name: i.name,
        location: i.location,
        timeSlot: i.timeSlot,
        description: i.description,
        sortOrder: i.sortOrder
      })) || []
    })) || [{ id: null, title: '', items: [] }]
    isDataLoaded.value = true // 数据加载完成
  } catch (error) {
    ElMessage.error('加载行程失败')
    router.push('/itineraries')
  }
}

function formatDateForInput(date) {
  if (!date) return ''

  // 如果是数组格式 [year, month, day]
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }

  // 如果是 ISO 字符串格式 "2026-01-15T00:00:00"
  if (typeof date === 'string' && date.includes('T')) {
    const d = new Date(date)
    if (isNaN(d.getTime())) return date
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }

  // 如果是普通字符串格式 "2026-01-15"
  if (typeof date === 'string') {
    // 验证格式是否正确
    const parts = date.split('-')
    if (parts.length === 3) {
      return date
    }
    // 如果格式不对，尝试解析
    const d = new Date(date)
    if (!isNaN(d.getTime())) {
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
  }

  return ''
}

function calculateDayCount(startDate, endDate) {
  if (!startDate || !endDate) return 0
  const start = new Date(startDate)
  const end = new Date(endDate)
  const diffTime = Math.abs(end - start)
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
}

// 监听日期变化，自动调整日程数量
function handleDateChange() {
  // 只在数据加载完成后才响应日期变化
  if (!isDataLoaded.value) return
  if (!form.value.startDate || !form.value.endDate) return

  const newDayCount = calculateDayCount(form.value.startDate, form.value.endDate)
  const currentDayCount = days.value.length

  // 如果新的天数小于当前天数，删除多余的日程
  if (newDayCount < currentDayCount) {
    const removedCount = currentDayCount - newDayCount
    // 使用 splice 直接修改数组，确保响应式更新
    days.value.splice(newDayCount, removedCount)
    ElMessage.warning(`日期范围变短，已删除最后 ${removedCount} 天的日程`)
  }
  // 如果新的天数大于当前天数，添加日程
  else if (newDayCount > currentDayCount) {
    const addedCount = newDayCount - currentDayCount
    for (let i = 0; i < addedCount; i++) {
      days.value.push({
        id: null,
        title: '',
        items: []
      })
    }
  }
}

function addDayItem() {
  // 检查是否超出行程日期范围
  if (form.value.startDate && form.value.endDate) {
    const maxDays = calculateDayCount(form.value.startDate, form.value.endDate)

    if (days.value.length >= maxDays) {
      ElMessage.warning(`行程日期范围为 ${maxDays} 天，不能再添加更多天数`)
      return
    }
  } else {
    ElMessage.warning('请先选择起始日期和结束日期')
    return
  }

  days.value.push({
    id: null,
    title: '',
    items: []
  })
}

function removeDay(index) {
  if (days.value.length > 1) {
    days.value.splice(index, 1)
  }
}

function addItemToDay(dayIndex) {
    days.value[dayIndex].items.push({
    id: null,
    type: 'ATTRACTION',
    name: '',
    location: '',
    timeSlot: '',
    description: ''
  })
}

function removeItem(dayIndex, itemIndex) {
  days.value[dayIndex].items.splice(itemIndex, 1)
}

function handleDragStart(dayIndex, itemIndex) {
  dragDayIndex.value = dayIndex
  dragItemIndex.value = itemIndex
}

function handleDrop(targetDayIndex, targetItemIndex) {
  if (dragDayIndex.value === -1 || dragItemIndex.value === -1) return
  
  const sourceItem = days.value[dragDayIndex.value].items[dragItemIndex.value]
  
  if (dragDayIndex.value === targetDayIndex) {
    days.value[dragDayIndex.value].items.splice(dragItemIndex.value, 1)
    days.value[targetDayIndex].items.splice(targetItemIndex, 0, sourceItem)
  } else {
    days.value[dragDayIndex.value].items.splice(dragItemIndex.value, 1)
    days.value[targetDayIndex].items.splice(targetItemIndex, 0, sourceItem)
  }
  
  dragDayIndex.value = -1
  dragItemIndex.value = -1
}

async function handleSubmit() {
  if (!form.value.title) {
    ElMessage.warning('请输入行程标题')
    return
  }
  if (!form.value.destinationId) {
    ElMessage.warning('请选择目的地')
    return
  }
  if (!form.value.startDate || !form.value.endDate) {
    ElMessage.warning('请选择日期')
    return
  }

  // 验证日程数量是否与日期范围匹配
  const dayCount = calculateDayCount(form.value.startDate, form.value.endDate)
  if (days.value.length > dayCount) {
    ElMessage.warning(`日期范围为 ${dayCount} 天，当前有 ${days.value.length} 天日程，请调整`)
    return
  }

  try {
    if (isEdit.value) {
      // 更新行程基本信息
      await updateItinerary(route.params.id, form.value)
      // 保存日程和行程项
      await saveDaysAndItems(route.params.id)
      ElMessage.success('更新成功')
    } else {
      // 1. 创建行程
      const createRes = await createItinerary(form.value)
      const itineraryId = createRes.data.id

      // 2. 获取行程详情，获取后端自动生成的天数ID
      const detailRes = await getItinerary(itineraryId)
      const backendDays = detailRes.data.days || []

      // 3. 将后端天数ID映射到前端
      for (let i = 0; i < days.value.length && i < backendDays.length; i++) {
        days.value[i].id = backendDays[i].id
      }

      // 4. 保存行程项
      await saveItemsOnly(itineraryId)
      ElMessage.success('创建成功')
    }
    router.push('/itineraries')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(isEdit.value ? '更新失败：' + (error.message || '未知错误') : '创建失败：' + (error.message || '未知错误'))
  }
}

async function saveDaysAndItems(itineraryId) {
  // 1. 先获取后端当前的天数列表
  const detailRes = await getItinerary(itineraryId)
  const backendDays = detailRes.data.days || []
  const backendDayIds = backendDays.map(d => d.id)

  // 2. 如果后端天数多于前端，需要删除多余的天数
  const frontendDayIds = days.value.filter(d => d.id).map(d => d.id)
  const idsToDelete = backendDayIds.filter(id => !frontendDayIds.includes(id))

  // 3. 删除多余的天数
  for (const dayId of idsToDelete) {
    try {
      await deleteDay(dayId)
    } catch (e) {
      console.error('删除日程失败:', dayId, e)
    }
  }

  // 4. 保存/更新前端的天数
  for (let dayIndex = 0; dayIndex < days.value.length; dayIndex++) {
    const day = days.value[dayIndex]

    if (!day.id) {
      // 新增的日程，需要先创建
      const res = await addDay(itineraryId, {
        dayNumber: dayIndex + 1,
        title: day.title || `Day ${dayIndex + 1}`
      })
      day.id = res.data.id
    } else {
      // 已存在的日程，更新即可
      await updateDay(day.id, {
        title: day.title || `Day ${dayIndex + 1}`
      })
    }

    // 保存该天所有行程项
    for (let itemIndex = 0; itemIndex < day.items.length; itemIndex++) {
      await saveItem(itineraryId, day.id, day.items[itemIndex])
    }
  }
}

async function saveItemsOnly(itineraryId) {
  for (let dayIndex = 0; dayIndex < days.value.length; dayIndex++) {
    const day = days.value[dayIndex]
    if (!day.id) continue
    
    for (let itemIndex = 0; itemIndex < day.items.length; itemIndex++) {
      await saveItem(itineraryId, day.id, day.items[itemIndex])
    }
  }
}

async function saveItem(itineraryId, dayId, item) {
  if (!item.name) return
  
  if (!item.id) {
    await addItem(itineraryId, dayId, {
      type: item.type,
      name: item.name,
      location: item.location,
      timeSlot: item.timeSlot,
      description: item.description
    })
  } else {
    await updateItem(item.id, {
      type: item.type,
      name: item.name,
      location: item.location,
      timeSlot: item.timeSlot,
      description: item.description
    })
  }
}

onMounted(async () => {
  await loadDestinations()
  if (isEdit.value) {
    await loadItinerary()
  } else {
    // 创建行程时，直接启用日期监听
    isDataLoaded.value = true
  }
})
</script>

<style scoped>
.itinerary-editor {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }

  .days-section {
    margin-top: 24px;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .day-section {
    margin-bottom: 20px;
  }

  .day-card {
    border-left: 4px solid #409eff;
  }

  .day-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.2s;
  }

  .day-header.dragging {
    opacity: 0.5;
  }

  .day-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .day-badge {
    background: #409eff;
    color: #fff;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 600;
  }

  .day-title-input {
    width: 300px;
  }

  .day-actions {
    display: flex;
    gap: 8px;
  }

  .items-section {
    margin-top: 16px;
  }

  .items-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 500;
    color: #606266;
  }

  .empty-items {
    padding: 20px 0;
  }

  .items-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .item-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: #fafafa;
    border-radius: 8px;
    cursor: move;
    transition: all 0.2s;
  }

  .item-row:hover {
    background: #f0f5ff;
  }

  .item-row.dragging {
    opacity: 0.5;
  }

  .drag-handle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: #c0c4cc;
    cursor: move;
    flex-shrink: 0;
  }

  .drag-handle:hover {
    color: #409eff;
  }

  .item-fields {
    flex: 1;
  }

  .type-select {
    width: 100%;
  }
}
</style>