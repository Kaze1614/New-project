<template>
  <div class="admin-shell">
    <aside class="admin-rail">
      <RouterLink class="admin-brand" to="/dashboard" aria-label="返回学生端首页">
        <span class="admin-brand-mark">M</span>
        <span>
          <strong>数学园后台</strong>
          <small>题库录入工作台</small>
        </span>
      </RouterLink>

      <nav class="admin-nav" aria-label="后台导航">
        <RouterLink class="admin-nav-item" to="/admin/questions/new">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14"></path>
          </svg>
          新增题目
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
          <p class="admin-kicker">数学园后台 &gt; 题目管理 &gt; 新增/编辑题目</p>
          <h1>题目录入</h1>
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
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>
