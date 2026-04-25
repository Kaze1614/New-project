<template>
  <section class="library-page">
    <header class="library-head">
      <h2>错题本</h2>
      <p>按关键词快速定位错题，章节标签统一显示为本地章节目录中的节名称。</p>
    </header>

    <form class="filter-bar" @submit.prevent="applyFilters">
      <input v-model.trim="filters.keyword" type="text" placeholder="搜索题干、题目关键词" />
      <button class="primary-btn" type="submit">筛选</button>
    </form>

    <p v-if="libraryStore.loadingMistakes" class="muted">正在加载错题数据...</p>
    <p v-else-if="!libraryStore.mistakes.length" class="muted">当前还没有错题记录</p>

    <div v-else class="question-grid">
      <article v-for="item in libraryStore.mistakes" :key="item.id" class="question-card">
        <div class="card-top">
          <span class="tag chapter">{{ sectionLabel(item.chapterId) }}</span>
        </div>
        <h3>{{ item.questionTitle }}</h3>
        <p>{{ item.questionContent }}</p>
        <footer class="card-actions">
          <button class="outline-btn" type="button" @click="addFavorite(item)">加入收藏</button>
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
  libraryStore.mistakeFilters = { ...filters }
  await libraryStore.loadMistakes()
}

function sectionLabel(chapterId) {
  return resolveSectionLabel(chapterLabelMap.value, chapterId)
}

async function addFavorite(item) {
  await unwrap(api.post('/favorites', {
    questionId: item.questionId,
    chapterId: item.chapterId,
    title: item.questionTitle,
    content: item.questionContent
  }))
}

async function remove(id) {
  await unwrap(api.delete(`/mistakes/${id}`))
  await libraryStore.loadMistakes()
}
</script>
