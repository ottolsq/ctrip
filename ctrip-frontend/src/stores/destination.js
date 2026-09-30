import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDestinations, getDestination } from '@/api/destination'

export const useDestinationStore = defineStore('destination', () => {
  const destinations = ref([])
  const currentDestination = ref(null)
  const loading = ref(false)

  async function fetchDestinations(params = {}) {
    loading.value = true
    try {
      const res = await getDestinations(params)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchDestination(id) {
    loading.value = true
    try {
      const res = await getDestination(id)
      currentDestination.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  return {
    destinations,
    currentDestination,
    loading,
    fetchDestinations,
    fetchDestination
  }
})