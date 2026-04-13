<template>
  <header class="topbar">
    <form class="topbar-search" @submit.prevent="submitSearch">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="7"></circle>
        <path d="M20 20l-3.5-3.5"></path>
      </svg>
      <input
        ref="searchInput"
        v-model.trim="keyword"
        type="search"
        placeholder="全局智能搜索（章节/题目/错题）"
        aria-label="全局智能搜索"
      />
      <kbd>⌘K</kbd>
    </form>

    <div class="topbar-actions">
      <button class="icon-btn" type="button" aria-label="通知">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 18h5l-1.5-1.5c-.3-.3-.5-.8-.5-1.3V10a6 6 0 1 0-12 0v5.2c0 .5-.2 1-.5 1.3L4 18h5"></path>
          <path d="M10 18a2 2 0 0 0 4 0"></path>
        </svg>
      </button>
      <button class="outline-btn" type="button" @click="logout">退出</button>
    </div>
  </header>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const searchInput = ref(null)

function submitSearch() {
  const query = keyword.value
  router.push({ path: '/chapters', query: query ? { keyword: query } : {} })
}

function onKeydown(event) {
  const isQuickSearch = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k'
  if (!isQuickSearch) return
  event.preventDefault()
  searchInput.value?.focus()
}

function logout() {
  authStore.logout()
  router.push('/login')
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>
