<template>
  <div class="admin-shell">
    <aside class="admin-rail">
      <RouterLink class="admin-brand" to="/admin/questions" aria-label="返回题目管理">
        <span class="admin-brand-mark">M</span>
        <span>
          <strong>数学园后台</strong>
          <small>题库管理工作台</small>
        </span>
      </RouterLink>

      <nav class="admin-nav" aria-label="后台导航">
        <RouterLink class="admin-nav-item" to="/admin/questions">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h16M4 12h16M4 18h10"></path>
          </svg>
          题目管理
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/users">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="10" cy="7" r="4"></circle>
            <path d="M20 8v6M17 11h6"></path>
          </svg>
          用户管理
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/dashboard">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 11l9-8 9 8"></path>
            <path d="M5 10v10h14V10"></path>
          </svg>
          学生端首页
        </RouterLink>
      </nav>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div>
          <p class="admin-kicker">数学园后台 &gt; {{ adminCrumb }}</p>
          <h1>{{ adminTitle }}</h1>
        </div>
        <div class="admin-topbar-actions">
          <RouterLink class="outline-btn" to="/dashboard">返回学生端</RouterLink>
          <button class="outline-btn" type="button" @click="logout">退出</button>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const adminTitle = computed(() => route.meta.adminTitle || '题目管理')
const adminCrumb = computed(() => route.meta.adminCrumb || '题目管理')

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>
