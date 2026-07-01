import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getGuides, getGuide, deleteGuide, likeGuide, unlikeGuide } from '@/api/guide'
import { ElMessage } from 'element-plus'

export const useGuideStore = defineStore('guide', () => {
  const guides = ref([])
  const currentGuide = ref(null)
  const loading = ref(false)

  async function fetchGuides(params = {}) {
    loading.value = true
    try {
      const res = await getGuides(params)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchGuide(id) {
    loading.value = true
    try {
      const res = await getGuide(id)
      currentGuide.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function removeGuide(id) {
    try {
      await deleteGuide(id)
      ElMessage.success('攻略已删除')
      return true
    } catch (error) {
      return false
    }
  }

  async function toggleLike(id, isLiked) {
    try {
      if (isLiked) {
        await unlikeGuide(id)
      } else {
        await likeGuide(id)
      }
      return true
    } catch (error) {
      return false
    }
  }

  return {
    guides,
    currentGuide,
    loading,
    fetchGuides,
    fetchGuide,
    removeGuide,
    toggleLike
  }
})