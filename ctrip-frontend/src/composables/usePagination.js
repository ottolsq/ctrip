import { ref, reactive, computed } from 'vue'

/**
 * 分页相关组合式函数
 */
export function usePagination(fetchFn, defaultPageSize = 10) {
  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const pages = ref(0)

  const pagination = reactive({
    current: 1,
    pageSize: defaultPageSize
  })

  // 分页组件所需的参数
  const paginationProps = computed(() => ({
    current: pagination.current,
    pageSize: pagination.pageSize,
    total: total.value,
    pageSizes: [5, 10, 20, 50]
  }))

  /**
   * 加载数据
   */
  async function loadData(params = {}) {
    loading.value = true
    try {
      const res = await fetchFn({
        current: pagination.current,
        size: pagination.pageSize,
        ...params
      })
      list.value = res.data.records || res.data || []
      total.value = res.data.total || 0
      pages.value = res.data.pages || 0
    } catch (error) {
      // 错误已由 request.js 统一处理
    } finally {
      loading.value = false
    }
  }

  /**
   * 切换页码
   */
  function handleCurrentChange(page) {
    pagination.current = page
    loadData()
  }

  /**
   * 切换每页条数
   */
  function handleSizeChange(size) {
    pagination.pageSize = size
    pagination.current = 1
    loadData()
  }

  return {
    loading,
    list,
    total,
    pages,
    pagination,
    paginationProps,
    loadData,
    handleCurrentChange,
    handleSizeChange
  }
}