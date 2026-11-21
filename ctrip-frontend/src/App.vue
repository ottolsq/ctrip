<template>
  <div id="app">
    <AppHeader v-if="!isAdminLayout" />
    <main :class="['main-wrapper', { 'admin-main-wrapper': isAdminLayout }]">
      <router-view />
    </main>
    <AppFooter v-if="!isAdminLayout" />
  </div>
</template>

<script setup>
import '@/assets/css/global.css'
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'

const route = useRoute()
const userStore = useUserStore()
const isAdminLayout = computed(() => route.meta.adminLayout === true)

onMounted(() => {
  userStore.init()
})
</script>

<style>
#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-wrapper {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px;
}

.main-wrapper.admin-main-wrapper {
  max-width: none;
  margin: 0;
  padding: 0;
  display: flex;
}
</style>
