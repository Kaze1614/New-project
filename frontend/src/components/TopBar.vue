<template>
  <header class="topbar">
    <div class="topbar-spacer" aria-hidden="true"></div>

    <div class="topbar-actions">
      <form class="topbar-search" @submit.prevent="submitSearch">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="7"></circle>
          <path d="M20 20l-3.5-3.5"></path>
        </svg>
        <input
          ref="searchInput"
          v-model.trim="keyword"
          type="search"
          placeholder="搜索章节、题目、错题"
          aria-label="全局搜索"
        />
      </form>

      <template v-if="authStore.isLoggedIn">
        <div class="user-avatar" :title="avatarName" aria-label="当前用户头像">
          {{ avatarInitial }}
        </div>
        <button class="outline-btn" type="button" @click="logout">退出登录</button>
      </template>

      <RouterLink v-else class="primary-btn topbar-login" to="/login">登录 / 注册</RouterLink>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const searchInput = ref(null)

const avatarName = computed(() => {
  return authStore.user?.displayName || authStore.user?.username || 'User'
})

const avatarInitial = computed(() => {
  return avatarName.value.trim().charAt(0).toUpperCase() || 'U'
})

function submitSearch() {
  const query = keyword.value
  router.push({ path: '/chapters', query: query ? { keyword: query } : {} })
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>
