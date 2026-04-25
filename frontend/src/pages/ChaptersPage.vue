<template>
  <section class="chapters-page">
    <header class="library-head">
      <h2>章节目录</h2>
      <p>按“册 - 章 - 节”浏览高中数学目录，点击具体小节直接进入对应题组。</p>
    </header>

    <article class="panel-card chapter-tree-card">
      <div class="chapter-tree-head">
        <div>
          <p class="section-eyebrow">CATALOG</p>
          <h3>高中数学章节树</h3>
        </div>
      </div>

      <p v-if="loading" class="muted">章节加载中...</p>
      <p v-else-if="loadError" class="error-text">{{ loadError }}</p>

      <ul v-else class="chapter-tree collapsible-tree">
        <li v-for="book in chapters" :key="book.id" class="book-node">
          <button
            class="tree-toggle book-toggle"
            type="button"
            :aria-expanded="isBookExpanded(book.id)"
            @click="toggleBook(book.id)"
          >
            <span class="tree-caret" :class="{ expanded: isBookExpanded(book.id) }" aria-hidden="true">
              <svg viewBox="0 0 16 16"><path d="M5.5 3.5 10 8l-4.5 4.5" /></svg>
            </span>
            <span>{{ book.title }}</span>
          </button>

          <ul v-if="isBookExpanded(book.id)" class="chapter-branch">
            <li v-for="chapter in book.children || []" :key="chapter.id" class="chapter-node">
              <button
                class="tree-toggle chapter-toggle"
                type="button"
                :aria-expanded="isChapterExpanded(chapter.id)"
                @click="toggleChapter(chapter.id)"
              >
                <span class="tree-caret" :class="{ expanded: isChapterExpanded(chapter.id) }" aria-hidden="true">
                  <svg viewBox="0 0 16 16"><path d="M5.5 3.5 10 8l-4.5 4.5" /></svg>
                </span>
                <span>{{ chapter.title }}</span>
              </button>

              <ul v-if="isChapterExpanded(chapter.id)" class="section-branch">
                <li v-for="section in chapter.children || []" :key="section.id">
                  <button class="section-link" type="button" @click="goStudy(section.id)">
                    <span>{{ section.title }}</span>
                  </button>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </article>

    <article v-if="searchKeyword" class="panel-card">
      <h3>搜索结果：{{ searchKeyword }}</h3>
      <p v-if="searchLoading" class="muted">搜索中...</p>
      <template v-else>
        <p class="muted">章节命中：{{ searchResults.chapters.length }}，题目命中：{{ searchResults.questions.length }}</p>
        <ul class="search-list">
          <li v-for="question in searchResults.questions" :key="question.id">{{ question.title }}</li>
        </ul>
      </template>
    </article>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, unwrap } from '../api/client'

const route = useRoute()
const router = useRouter()

const chapters = ref([])
const loading = ref(false)
const loadError = ref('')
const expandedBooks = ref(new Set())
const expandedChapters = ref(new Set())
const searchLoading = ref(false)
const searchKeyword = ref('')
const searchResults = reactive({
  chapters: [],
  questions: [],
  mistakes: []
})

onMounted(loadChapters)

watch(
  () => route.query.keyword,
  async (value) => {
    searchKeyword.value = typeof value === 'string' ? value.trim() : ''
    if (!searchKeyword.value) {
      searchResults.chapters = []
      searchResults.questions = []
      searchResults.mistakes = []
      return
    }
    searchLoading.value = true
    try {
      const data = await unwrap(api.get('/search', { params: { keyword: searchKeyword.value } }))
      searchResults.chapters = data.chapters || []
      searchResults.questions = data.questions || []
      searchResults.mistakes = data.mistakes || []
    } finally {
      searchLoading.value = false
    }
  },
  { immediate: true }
)

async function loadChapters() {
  loading.value = true
  loadError.value = ''
  try {
    chapters.value = await unwrap(api.get('/chapters/tree'))
    expandInitialBranch(chapters.value)
  } catch (error) {
    loadError.value = '章节目录加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function expandInitialBranch(tree) {
  expandedBooks.value = new Set()
  expandedChapters.value = new Set()
}

function isBookExpanded(bookId) {
  return expandedBooks.value.has(bookId)
}

function isChapterExpanded(chapterId) {
  return expandedChapters.value.has(chapterId)
}

function toggleBook(bookId) {
  const next = new Set(expandedBooks.value)
  if (next.has(bookId)) {
    next.delete(bookId)
  } else {
    next.add(bookId)
  }
  expandedBooks.value = next
}

function toggleChapter(chapterId) {
  const next = new Set(expandedChapters.value)
  if (next.has(chapterId)) {
    next.delete(chapterId)
  } else {
    next.add(chapterId)
  }
  expandedChapters.value = next
}

function goStudy(chapterId) {
  router.push({ path: '/study', query: { chapterId } })
}
</script>
