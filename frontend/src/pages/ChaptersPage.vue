<template>
  <section class="chapters-page">
    <header class="library-head">
      <h2>章节目录</h2>
      <p>点击章节可直接进入 20 题学习会话。</p>
    </header>

    <article class="panel-card chapter-tree-card">
      <h3>章节树</h3>
      <ul class="chapter-tree">
        <li v-for="root in chapters" :key="root.id">
          <p class="chapter-title">{{ root.title }}</p>
          <ul>
            <li v-for="node in root.children || []" :key="node.id">
              <button class="chapter-button" type="button" @click="goStudy(node.id)">
                {{ node.title }}
              </button>
              <ul>
                <li v-for="leaf in node.children || []" :key="leaf.id">
                  <button class="chapter-leaf" type="button" @click="goStudy(leaf.id)">
                    {{ leaf.title }}
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
const searchLoading = ref(false)
const searchKeyword = ref('')
const searchResults = reactive({
  chapters: [],
  questions: [],
  mistakes: []
})

onMounted(async () => {
  chapters.value = await unwrap(api.get('/chapters/tree'))
})

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

function goStudy(chapterId) {
  router.push({ path: '/study', query: { chapterId } })
}
</script>
