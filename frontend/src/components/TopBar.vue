<template>
  <header class="topbar">
    <div class="topbar-actions">
      <button class="icon-btn" type="button" aria-label="通知">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V10a6 6 0 10-12 0v4.2c0 .5-.2 1-.6 1.4L4 17h5" />
          <path d="M9 17a3 3 0 006 0" />
        </svg>
      </button>
      <button class="outline-btn" type="button" @click="logout">退出登录</button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { tokenStorage } from '../api/client'

const router = useRouter()

function logout() {
  tokenStorage.clear()
  router.push('/login')
}
</script>
