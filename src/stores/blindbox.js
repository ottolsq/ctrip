import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBlindBoxTemplates, getBlindBoxTemplate, getMyBlindBoxes, getBlindBoxOrders, getBlindBoxOrder, getBlindBoxResult } from '@/api/blindbox'

export const useBlindBoxStore = defineStore('blindbox', () => {
  const templates = ref([])
  const currentTemplate = ref(null)
  const myBoxes = ref([])
  const orders = ref([])
  const currentOrder = ref(null)
  const currentResult = ref(null)
  const loading = ref(false)

  async function fetchTemplates(params = {}) {
    loading.value = true
    try {
      const res = await getBlindBoxTemplates(params)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchTemplate(id) {
    loading.value = true
    try {
      const res = await getBlindBoxTemplate(id)
      currentTemplate.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchMyBoxes() {
    loading.value = true
    try {
      const res = await getMyBlindBoxes()
      myBoxes.value = res.data || []
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchOrders(params = {}) {
    loading.value = true
    try {
      const res = await getBlindBoxOrders(params)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchOrder(orderNo) {
    loading.value = true
    try {
      const res = await getBlindBoxOrder(orderNo)
      currentOrder.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchResult(orderNo) {
    loading.value = true
    try {
      const res = await getBlindBoxResult(orderNo)
      currentResult.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  return {
    templates,
    currentTemplate,
    myBoxes,
    orders,
    currentOrder,
    currentResult,
    loading,
    fetchTemplates,
    fetchTemplate,
    fetchMyBoxes,
    fetchOrders,
    fetchOrder,
    fetchResult
  }
})