<template>
  <div class="guide-editor" v-loading="loading">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="editor-container">
      <!-- 页面标题区域 -->
      <div class="page-hero">
        <div class="hero-icon">
          <el-icon :size="40"><Edit /></el-icon>
        </div>
        <h1>{{ isEdit ? '编辑攻略' : '发布攻略' }}</h1>
        <p>分享你的旅行经验，帮助更多旅行者</p>
      </div>

      <el-card class="editor-card" shadow="hover">
        <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" size="large">
          <!-- 标题 -->
          <div class="form-section">
            <div class="section-label">
              <el-icon><Document /></el-icon>
              <span>攻略标题</span>
              <em class="required">*</em>
            </div>
            <el-form-item prop="title" class="no-margin">
              <el-input 
                v-model="form.title" 
                placeholder="给攻略起个响亮的名字，让更多人看到..." 
                maxlength="100" 
                show-word-limit
                class="custom-input"
              />
            </el-form-item>
          </div>

          <!-- 目的地 -->
          <div class="form-section">
            <div class="section-label">
              <el-icon><Location /></el-icon>
              <span>关联目的地</span>
            </div>
            <el-form-item prop="destinationId" class="no-margin">
              <el-select
                v-model="form.destinationId"
                placeholder="选择目的地（可选），帮助用户快速找到你的攻略"
                filterable
                remote
                :remote-method="searchDestinations"
                :loading="destLoading"
                clearable
                class="custom-select"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
                <el-option
                  v-for="d in destOptions"
                  :key="d.id"
                  :label="d.name"
                  :value="d.id"
                >
                  <span style="float: left">{{ d.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ d.country }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </div>

          <!-- 封面图 -->
          <div class="form-section">
            <div class="section-label">
              <el-icon><Picture /></el-icon>
              <span>封面图片</span>
              <span class="label-tip">建议尺寸 800x450 或比例 16:9</span>
            </div>
            <el-form-item class="no-margin">
              <el-upload
                action=""
                :show-file-list="false"
                :before-upload="handleCoverUpload"
                accept="image/*"
                class="cover-upload"
              >
                <div v-if="form.coverUrl" class="cover-preview">
                  <el-image :src="form.coverUrl" fit="cover" class="cover-img" />
                  <div class="cover-overlay">
                    <el-icon :size="28"><RefreshRight /></el-icon>
                    <span>点击更换图片</span>
                  </div>
                </div>
                <div v-else class="cover-uploader">
                  <div class="upload-icon-wrap">
                    <el-icon :size="36"><Plus /></el-icon>
                  </div>
                  <span class="upload-text">点击上传封面图</span>
                  <span class="upload-hint">支持 JPG、PNG 格式</span>
                </div>
              </el-upload>
            </el-form-item>
          </div>

          <!-- 攻略内容 -->
          <div class="form-section">
            <div class="section-label">
              <el-icon><Tickets /></el-icon>
              <span>攻略内容</span>
              <em class="required">*</em>
              <span class="label-tip">分享你的旅行经历、实用tips</span>
            </div>
            <el-form-item prop="content" class="no-margin">
              <div class="editor-wrapper">
                <div ref="toolbarRef" class="editor-toolbar"></div>
                <div ref="editorRef" class="editor-content"></div>
              </div>
            </el-form-item>
          </div>

          <!-- 内容预览 -->
          <div class="form-section preview-section" v-if="form.content">
            <div class="section-label">
              <el-icon><View /></el-icon>
              <span>内容预览</span>
            </div>
            <div class="content-preview">
              <div class="preview-content" v-html="formatPreviewContent(form.content)"></div>
            </div>
          </div>

          <!-- 提交按钮 -->
          <div class="form-actions">
            <el-button @click="handleCancel" size="large" class="cancel-btn">
              取消
            </el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitLoading" size="large" class="submit-btn">
              <el-icon v-if="!submitLoading"><Check /></el-icon>
              {{ isEdit ? '保存修改' : '立即发布' }}
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGuide, createGuide, updateGuide } from '@/api/guide'
import { getDestinations } from '@/api/destination'
import { uploadImage } from '@/api/upload'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Document, Location, Picture, RefreshRight, Tickets, View, Search, Check } from '@element-plus/icons-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const editorRef = ref(null)
const toolbarRef = ref(null)
const loading = ref(false)
const submitLoading = ref(false)
const destLoading = ref(false)
const destOptions = ref([])
let editor = null
let toolbar = null

// 格式化预览内容，将换行转换为 <br>
function formatPreviewContent(content) {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

const form = reactive({
  title: '',
  destinationId: null,
  coverUrl: '',
  content: ''
})

const formRules = {
  title: [
    { required: true, message: '请输入攻略标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为2-100个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入攻略内容', trigger: 'blur' }
  ]
}

// 搜索目的地
async function searchDestinations(query) {
  if (!query) return
  destLoading.value = true
  try {
    const res = await getDestinations({ keyword: query, current: 1, size: 10 })
    destOptions.value = res.data.records || []
  } catch {
    destOptions.value = []
  } finally {
    destLoading.value = false
  }
}

// 封面上传
async function handleCoverUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片'); return false }
  if (!isLt5M) { ElMessage.error('图片不能超过5MB'); return false }

  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadImage(formData)
    form.coverUrl = res.data.url
  } catch {
    // 错误已处理
  }
  return false
}

// 初始化富文本编辑器
async function initEditor() {
  if (!editorRef.value || !toolbarRef.value) return
  try {
    const WangEditor = await import('@wangeditor/editor')
    const { createEditor, createToolbar } = WangEditor

    const editorConfig = {
      placeholder: '请输入攻略内容...',
      onChange: () => {
        form.content = editor.getHtml()
      },
      MENU_CONF: {
        uploadImage: {
          async customUpload(file, insertFn) {
            const formData = new FormData()
            formData.append('file', file)
            try {
              const res = await uploadImage(formData)
              insertFn(res.data.url)
            } catch {
              ElMessage.error('图片上传失败')
            }
          }
        }
      }
    }

    editor = createEditor({
      selector: editorRef.value,
      config: {
        ...editorConfig,
        html: form.content || '<p><br></p>'
      }
    })

    toolbar = createToolbar({
      editor,
      selector: toolbarRef.value,
      config: {}
    })
  } catch {
    ElMessage.warning('富文本编辑器未安装，使用纯文本模式')
  }
}

// 加载攻略（编辑模式）
async function loadGuide() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getGuide(route.params.id)
    const data = res.data
    form.title = data.title || ''
    form.destinationId = data.destinationId || null
    form.coverUrl = data.coverUrl || ''
    form.content = data.content || ''
  } catch {
    ElMessage.error('加载攻略失败')
    router.push('/guides')
  } finally {
    loading.value = false
  }
}

// 提交
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const postData = {
      title: form.title,
      destinationId: form.destinationId || undefined,
      coverUrl: form.coverUrl || undefined,
      content: form.content,
      status: 'PUBLISHED'
    }

    if (isEdit.value) {
      await updateGuide(route.params.id, postData)
      ElMessage.success('攻略已更新')
    } else {
      await createGuide(postData)
      ElMessage.success('攻略发布成功')
    }
    router.push(isEdit.value ? `/guides/${route.params.id}` : '/guides')
  } catch {
    // 错误已处理
  } finally {
    submitLoading.value = false
  }
}

function handleCancel() {
  if (isEdit.value) {
    router.push(`/guides/${route.params.id}`)
  } else {
    router.push('/guides')
  }
}

onMounted(async () => {
  await loadGuide()
  // 使用 nextTick 确保 DOM 渲染完成
  setTimeout(() => initEditor(), 100)
})

onBeforeUnmount(() => {
  if (editor && editor.destroy) {
    editor.destroy()
  }
  if (toolbar && toolbar.destroy) {
    toolbar.destroy()
  }
})
</script>

<style scoped>
.guide-editor {
  min-height: calc(100vh - 200px);
  padding: 30px 20px 60px;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.bg-circle-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  top: -200px;
  right: -100px;
}

.bg-circle-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  bottom: -100px;
  left: -100px;
}

.bg-circle-3 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.editor-container {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 页面标题 */
.page-hero {
  text-align: center;
  margin-bottom: 40px;
  animation: fadeInUp 0.6s ease-out;
}

.hero-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
}

.page-hero h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.page-hero p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 卡片样式 */
.editor-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.editor-card :deep(.el-card__body) {
  padding: 40px;
}

/* 表单区块 */
.form-section {
  margin-bottom: 32px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.section-label .el-icon {
  color: #667eea;
  font-size: 18px;
}

.section-label .required {
  color: #f56c6c;
  font-style: normal;
}

.label-tip {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  margin-left: 8px;
}

.no-margin {
  margin-bottom: 0 !important;
}

/* 标题输入框 */
.custom-input :deep(.el-input__wrapper) {
  padding: 14px 16px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e8e8e8;
  transition: all 0.3s;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
}

.custom-input :deep(.el-input__inner) {
  font-size: 15px;
}

/* 目的地选择 */
.custom-select {
  width: 100%;
}

.custom-select :deep(.el-select__wrapper) {
  padding: 12px 16px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e8e8e8;
  min-height: 44px;
  transition: all 0.3s;
}

.custom-select :deep(.el-select__wrapper:hover),
.custom-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
}

/* 封面图上传 */
.cover-upload {
  display: block;
}

.cover-uploader {
  width: 100%;
  height: 240px;
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;
  cursor: pointer;
  transition: all 0.3s;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f2ff 100%);
}

.cover-uploader:hover {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f2ff 0%, #e8ebff 100%);
}

.upload-icon-wrap {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.upload-text {
  font-size: 15px;
  color: #666;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #999;
}

.cover-preview {
  width: 100%;
  height: 240px;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.cover-img {
  width: 100%;
  height: 100%;
  transition: transform 0.3s;
}

.cover-preview:hover .cover-img {
  transform: scale(1.02);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

/* 编辑器 */
.editor-wrapper {
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.editor-wrapper:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

.editor-toolbar {
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.editor-toolbar :deep(.w-e-toolbar) {
  background: #fafafa;
  border: none;
}

.editor-toolbar :deep(.w-e-bar-item button) {
  border-radius: 4px;
}

.editor-toolbar :deep(.w-e-bar-item button:hover) {
  background: #e8e8e8;
}

.editor-content {
  min-height: 350px;
  font-size: 15px;
}

.editor-content :deep(.w-e-text-container) {
  min-height: 350px;
}

.editor-content :deep(p) {
  margin: 8px 0;
}

/* 内容预览 */
.preview-section {
  padding-top: 20px;
  border-top: 1px dashed #e8e8e8;
}

.content-preview {
  background: linear-gradient(135deg, #f8f9ff 0%, #fff 100%);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e8e8e8;
}

.preview-content {
  font-size: 15px;
  line-height: 2;
  color: #333;
  white-space: pre-wrap;
}

/* 提交按钮 */
.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  width: 140px;
  height: 48px;
  border-radius: 24px;
  font-size: 15px;
  transition: all 0.3s;
}

.submit-btn {
  width: 160px;
  height: 48px;
  border-radius: 24px;
  font-size: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .editor-card :deep(.el-card__body) {
    padding: 24px 20px;
  }
  
  .page-hero h1 {
    font-size: 24px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .cancel-btn,
  .submit-btn {
    width: 100%;
  }
}
</style>