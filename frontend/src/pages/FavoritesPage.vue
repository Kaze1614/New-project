<template>
  <section class="library-page">
    <header class="library-head">
      <h2>收藏本</h2>
      <p>用关键词回看已收藏题目。</p>
    </header>

    <form class="filter-bar" @submit.prevent="applyFilters">
      <input v-model.trim="filters.keyword" type="text" placeholder="关键词搜索题干" />
      <button class="primary-btn" type="submit">筛选</button>
    </form>

    <p v-if="libraryStore.loadingFavorites" class="muted">加载中...</p>
    <p v-else-if="!libraryStore.favorites.length" class="muted">暂无收藏</p>

    <div v-else class="question-grid">
      <article v-for="item in libraryStore.favorites" :key="item.id" class="question-card">
        <div class="card-top">
          <span class="tag">{{ item.difficulty || 'UNKNOWN' }}</span>
          <span class="tag chapter">章节 {{ item.chapterId || '未标注' }}</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.content }}</p>
        <footer class="card-actions">
          <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { api, unwrap } from '../api/client'
import { useLibraryStore } from '../stores/library'

const libraryStore = useLibraryStore()
const filters = reactive({
  keyword: ''
})

onMounted(() => {
  applyFilters()
})

async function applyFilters() {
  libraryStore.favoriteFilters = { ...filters }
  await libraryStore.loadFavorites()
}

async function remove(id) {
  await unwrap(api.delete(`/favorites/${id}`))
  await libraryStore.loadFavorites()
}
</script>
