<template>
  <aside class="sidebar">
    <h1 class="brand">数学园</h1>
    <nav class="nav-list">
      <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
        <span class="nav-icon" aria-hidden="true"></span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </aside>
</template>

<script setup>
const navItems = [
  { path: '/dashboard', label: '首页' },
  { path: '/mistakes', label: '错题本' },
  { path: '/favorites', label: '收藏本' },
  { path: '/review', label: '待复习' },
  { path: '/search', label: '智能搜索' },
  { path: '/chapters', label: '章节目录' },
  { path: '/qa', label: '智能答疑' }
]
</script>
