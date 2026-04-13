<template>
  <div class="app-shell" @click="handleShellClick">
    <SidebarNav />
    <div class="app-main">
      <TopBar />
      <main class="app-content">
        <router-view />
      </main>
    </div>
    <AIAssistantDrawer />
  </div>
</template>

<script setup>
import SidebarNav from '../components/SidebarNav.vue'
import TopBar from '../components/TopBar.vue'
import AIAssistantDrawer from '../components/AIAssistantDrawer.vue'
import { useUiStore } from '../stores/ui'

const uiStore = useUiStore()

function handleShellClick() {
  if (uiStore.chapterFlyoutOpen) {
    uiStore.setChapterFlyoutOpen(false)
  }
}
</script>
