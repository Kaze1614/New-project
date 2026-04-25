<template>
  <section class="library-page">
    <header class="library-head">
      <h2>收藏本</h2>
      <p>按关键词回看已收藏题目，卡片只展示章节节名称与题目内容。</p>
    </header>

    <form class="filter-bar" @submit.prevent="applyFilters">
      <input v-model.trim="filters.keyword" type="text" placeholder="搜索收藏题目" />
      <button class="primary-btn" type="submit">筛选</button>
    </form>

    <p v-if="libraryStore.loadingFavorites" class="muted">正在加载收藏数据...</p>
    <p v-else-if="!libraryStore.favorites.length" class="muted">当前还没有收藏题目</p>

    <div v-else class="question-grid">
      <article v-for="item in libraryStore.favorites" :key="item.id" class="question-card">
        <div class="card-top">
          <span class="tag chapter">{{ sectionLabel(item.chapterId) }}</span>
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
import { onMounted, reactive, ref } from 'vue'
import { api, unwrap } from '../api/client'
import { useLibraryStore } from '../stores/library'
import { buildSectionLabelMap, resolveSectionLabel } from '../utils/chapterLabels'

const libraryStore = useLibraryStore()
const filters = reactive({
  keyword: ''
})
const chapterLabelMap = ref(new Map())

onMounted(async () => {
  await Promise.all([loadChapterLabels(), applyFilters()])
})

async function loadChapterLabels() {
  try {
    const tree = await unwrap(api.get('/chapters/tree'))
    chapterLabelMap.value = buildSectionLabelMap(tree)
  } catch {
    chapterLabelMap.value = new Map()
  }
}

async function applyFilters() {
  libraryStore.favoriteFilters = { ...filters }
  await libraryStore.loadFavorites()
}

function sectionLabel(chapterId) {
  return resolveSectionLabel(chapterLabelMap.value, chapterId)
}

async function remove(id) {
  await unwrap(api.delete(`/favorites/${id}`))
  await libraryStore.loadFavorites()
}
</script>
