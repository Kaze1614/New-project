<template>
  <section class="library-page favorites-page">
    <header class="library-head">
      <h2>收藏本</h2>
    </header>

    <form class="favorites-filter-bar" @submit.prevent="applyFilters">
      <input v-model.trim="filters.keyword" type="text" placeholder="搜索收藏题目、关键词" />

      <select v-model="filters.bookId" @change="onBookChange">
        <option value="">全部册</option>
        <option v-for="book in bookOptions" :key="book.id" :value="String(book.id)">
          {{ book.title }}
        </option>
      </select>

      <select v-model="filters.chapterId" :disabled="!chapterOptions.length" @change="onChapterChange">
        <option value="">全部章</option>
        <option v-for="chapter in chapterOptions" :key="chapter.id" :value="String(chapter.id)">
          {{ chapter.title }}
        </option>
      </select>

      <select v-model="filters.sectionId" :disabled="!sectionOptions.length">
        <option value="">全部节</option>
        <option v-for="section in sectionOptions" :key="section.id" :value="String(section.id)">
          {{ section.title }}
        </option>
      </select>

      <button class="primary-btn" type="submit">筛选</button>
    </form>

    <div v-if="libraryStore.loadingFavorites" class="panel-card">正在加载收藏数据...</div>
    <div v-else-if="!libraryStore.favorites.length" class="panel-card">当前还没有收藏题目。</div>

    <div v-else class="favorites-grid">
      <article v-for="item in libraryStore.favorites" :key="item.id" class="favorite-card">
        <div class="mistake-toolbar">
          <div class="mistake-toolbar__meta">
            <span class="tag source">{{ sourceText(item) }}</span>
          </div>
          <time class="favorite-date">{{ formatDate(item.createdAt) }}</time>
        </div>

        <p class="mistake-question-content favorite-content">{{ item.content }}</p>

        <div class="mistake-section-row">
          <span class="tag chapter">{{ sectionLabel(item.chapterId) }}</span>
        </div>

        <div v-if="isChoice(item)" class="mistake-options">
          <button
            v-for="option in item.options"
            :key="option"
            type="button"
            class="mistake-option"
            :class="optionClass(item, option)"
            @click="pickOption(item, option)"
          >
            <span class="mistake-option__code">{{ optionCode(option) }}.</span>
            <span class="mistake-option__text">{{ optionText(option) }}</span>
          </button>
        </div>

        <label v-else-if="isFill(item) && canCheckAnswer(item)" class="fill-answer">
          作答区域
          <div class="fill-row">
            <input
              :value="answerValue(item.id)"
              type="text"
              maxlength="200"
              @input="updateFillAnswer(item.id, $event.target.value)"
              @keyup.enter="saveFillAnswer(item.id)"
            />
            <button class="outline-btn" type="button" @click="saveFillAnswer(item.id)">保存答案</button>
          </div>
        </label>

        <div v-else class="mistake-readonly-tip">
          题目原始作答数据缺失，当前仅支持加入待复习或删除。
        </div>

        <button
          class="check-answer-btn"
          type="button"
          :disabled="!canCheckAnswer(item) || !answerValue(item.id) || isRevealed(item.id)"
          @click="checkAnswer(item.id)"
        >
          检查答案
        </button>

        <div v-if="isRevealed(item.id)" class="mistake-result-stack">
          <div class="mistake-answer-box">
            <strong>正确答案：</strong>
            <span>{{ item.correctAnswer }}</span>
          </div>

          <div class="mistake-feedback" :class="isCorrectAnswer(item) ? 'success' : 'danger'">
            <span>{{ isCorrectAnswer(item) ? '回答正确！' : '回答错误！' }}</span>
          </div>

          <div class="mistake-explanation-box">
            <strong>解析：</strong>
            <p>{{ item.explanation || '暂无解析' }}</p>
          </div>
        </div>

        <footer class="mistake-detail-actions favorite-actions">
          <button class="outline-btn" type="button" @click="addToReview(item.id)">加入待复习</button>
          <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, unwrap } from '../api/client'
import { useLibraryStore } from '../stores/library'
import { buildSectionLabelMap, resolveSectionLabel } from '../utils/chapterLabels'

const libraryStore = useLibraryStore()
const chapterTree = ref([])
const chapterLabelMap = ref(new Map())
const answerState = ref({})
const revealedState = ref(new Set())

const filters = reactive({
  keyword: '',
  bookId: '',
  chapterId: '',
  sectionId: ''
})

const bookOptions = computed(() => chapterTree.value)
const chapterOptions = computed(() => {
  const book = chapterTree.value.find((item) => String(item.id) === filters.bookId)
  return book?.children || []
})
const sectionOptions = computed(() => {
  const chapter = chapterOptions.value.find((item) => String(item.id) === filters.chapterId)
  return chapter?.children || []
})

onMounted(async () => {
  filters.keyword = libraryStore.favoriteFilters.keyword || ''
  filters.bookId = libraryStore.favoriteFilters.bookId ? String(libraryStore.favoriteFilters.bookId) : ''
  filters.chapterId = libraryStore.favoriteFilters.chapterNodeId ? String(libraryStore.favoriteFilters.chapterNodeId) : ''
  filters.sectionId = libraryStore.favoriteFilters.sectionId ? String(libraryStore.favoriteFilters.sectionId) : ''
  await Promise.all([loadChapterLabels(), applyFilters()])
})

async function loadChapterLabels() {
  try {
    const tree = await unwrap(api.get('/chapters/tree'))
    chapterTree.value = tree
    chapterLabelMap.value = buildSectionLabelMap(tree)
    hydrateAncestorFilters()
  } catch {
    chapterTree.value = []
    chapterLabelMap.value = new Map()
  }
}

function hydrateAncestorFilters() {
  if (filters.bookId || filters.chapterId || !filters.sectionId) return
  for (const book of chapterTree.value) {
    for (const chapter of book.children || []) {
      const found = (chapter.children || []).find((section) => String(section.id) === filters.sectionId)
      if (found) {
        filters.bookId = String(book.id)
        filters.chapterId = String(chapter.id)
        return
      }
    }
  }
}

async function applyFilters() {
  const chapterId = filters.sectionId || filters.chapterId || filters.bookId || null
  libraryStore.favoriteFilters = {
    keyword: filters.keyword,
    chapterId: chapterId ? Number(chapterId) : null,
    bookId: filters.bookId ? Number(filters.bookId) : null,
    chapterNodeId: filters.chapterId ? Number(filters.chapterId) : null,
    sectionId: filters.sectionId ? Number(filters.sectionId) : null
  }
  await libraryStore.loadFavorites()
  answerState.value = {}
  revealedState.value = new Set()
}

function onBookChange() {
  filters.chapterId = ''
  filters.sectionId = ''
}

function onChapterChange() {
  filters.sectionId = ''
}

function sourceText(item) {
  return item?.sourceLabel || '题目来源未标注'
}

function sectionLabel(chapterId) {
  return resolveSectionLabel(chapterLabelMap.value, chapterId)
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

function isChoice(item) {
  return ['SINGLE', 'MULTI'].includes(item?.questionType) && Array.isArray(item?.options) && item.options.length > 0
}

function isFill(item) {
  return item?.questionType === 'FILL'
}

function canCheckAnswer(item) {
  if (!item?.questionType || !item?.correctAnswer) return false
  if (item.questionType === 'FILL') return true
  return ['SINGLE', 'MULTI'].includes(item.questionType) && Array.isArray(item.options) && item.options.length > 0
}

function extractOptionCode(option) {
  const match = String(option).match(/^([A-Z])/)
  return match ? match[1] : String(option).trim().toUpperCase()
}

function optionCode(option) {
  return extractOptionCode(option)
}

function optionText(option) {
  const parts = String(option).split('.')
  return parts.length > 1 ? parts.slice(1).join('.').trim() : String(option)
}

function normalizeAnswer(value) {
  return String(value || '').trim().toUpperCase()
}

function selectedCodes(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
}

function sortedCodes(value) {
  return selectedCodes(value).sort().join(',')
}

function answersEqual(item, expected, actual) {
  if (!item) return false
  if (item.questionType === 'MULTI') {
    return sortedCodes(expected) === sortedCodes(actual)
  }
  return normalizeAnswer(expected) === normalizeAnswer(actual)
}

function answerValue(id) {
  return answerState.value[id] || ''
}

function setAnswerValue(id, value) {
  answerState.value = { ...answerState.value, [id]: value }
}

function isRevealed(id) {
  return revealedState.value.has(id)
}

function reveal(id) {
  const next = new Set(revealedState.value)
  next.add(id)
  revealedState.value = next
}

function pickOption(item, option) {
  if (!canCheckAnswer(item) || isRevealed(item.id)) return
  const code = extractOptionCode(option)
  if (item.questionType === 'MULTI') {
    const next = new Set(selectedCodes(answerValue(item.id)))
    next.has(code) ? next.delete(code) : next.add(code)
    setAnswerValue(item.id, [...next].sort().join(','))
    return
  }
  setAnswerValue(item.id, code)
}

function isSelected(item, option) {
  return selectedCodes(answerValue(item.id)).includes(extractOptionCode(option))
}

function optionClass(item, option) {
  const code = extractOptionCode(option)
  if (!isRevealed(item.id)) {
    return {
      selected: isSelected(item, option)
    }
  }
  return {
    selected: isSelected(item, option),
    correct: selectedCodes(item.correctAnswer).includes(code),
    wrong: isSelected(item, option) && !selectedCodes(item.correctAnswer).includes(code)
  }
}

function updateFillAnswer(id, value) {
  if (isRevealed(id)) return
  setAnswerValue(id, value)
}

function saveFillAnswer(id) {
  setAnswerValue(id, String(answerValue(id)).trim())
}

function checkAnswer(id) {
  const item = libraryStore.favorites.find((favorite) => favorite.id === id)
  if (!item || !canCheckAnswer(item) || !answerValue(id)) return
  if (item.questionType === 'FILL') {
    saveFillAnswer(id)
    if (!answerValue(id)) return
  }
  reveal(id)
}

function isCorrectAnswer(item) {
  return answersEqual(item, item.correctAnswer, answerValue(item.id))
}

async function addToReview(id) {
  await unwrap(api.post(`/favorites/${id}/review`))
}

async function remove(id) {
  await unwrap(api.delete(`/favorites/${id}`))
  await libraryStore.loadFavorites()
}
</script>
