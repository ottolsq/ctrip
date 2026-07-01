import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getItineraries, getItinerary } from '@/api/itinerary'

export const useItineraryStore = defineStore('itinerary', () => {
  const itineraries = ref([])
  const currentItinerary = ref(null)
  const loading = ref(false)

  async function fetchItineraries(params = {}) {
    loading.value = true
    try {
      const res = await getItineraries(params)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchItinerary(id) {
    loading.value = true
    try {
      const res = await getItinerary(id)
      currentItinerary.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  function clearCurrent() {
    currentItinerary.value = null
  }

  return {
    itineraries,
    currentItinerary,
    loading,
    fetchItineraries,
    fetchItinerary,
    clearCurrent
  }
})