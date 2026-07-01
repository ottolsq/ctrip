<template>
  <div id="app">
    <AppHeader />
    <main class="main-wrapper">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import '@/assets/css/global.css'
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'

const userStore = useUserStore()

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
</style>
