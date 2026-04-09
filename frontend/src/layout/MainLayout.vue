<template>
  <div class="app-shell">
    <SidebarNav />
    <div class="app-main">
      <TopBar />
      <main class="app-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import SidebarNav from '../components/SidebarNav.vue'
import TopBar from '../components/TopBar.vue'
</script>
