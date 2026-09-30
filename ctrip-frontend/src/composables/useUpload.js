import { ref } from 'vue'
import { uploadImage, uploadImages } from '@/api/upload'
import { ElMessage } from 'element-plus'

/**
 * 上传相关组合式函数
 */
export function useUpload() {
  const uploading = ref(false)
  const uploadedUrls = ref([])

  /**
   * 上传单张图片
   */
  async function handleUploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)

    uploading.value = true
    try {
      const res = await uploadImage(formData)
      const url = res.data.url
      uploadedUrls.value.push(url)
      return url
    } catch (error) {
      // 错误已处理
      throw error
    } finally {
      uploading.value = false
    }
  }

  /**
   * 批量上传图片
   */
  async function handleUploadImages(files) {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))

    uploading.value = true
    try {
      const res = await uploadImages(formData)
      const urls = res.data || []
      uploadedUrls.value.push(...urls)
      return urls
    } catch (error) {
      // 错误已处理
      throw error
    } finally {
      uploading.value = false
    }
  }

  /**
   * 上传前校验
   */
  function beforeUploadCheck(file) {
    const isImage = file.type.startsWith('image/')
    if (!isImage) {
      ElMessage.error('只能上传图片文件')
      return false
    }
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isLt5M) {
      ElMessage.error('图片大小不能超过 5MB')
      return false
    }
    return true
  }

  return {
    uploading,
    uploadedUrls,
    handleUploadImage,
    handleUploadImages,
    beforeUploadCheck
  }
}