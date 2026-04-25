<template>
  <section class="library-page mistake-workbench">
    <header class="library-head">
      <h2>我的错题本</h2>
      <p>左侧聚焦当前错题作答，右侧按册、章、节筛选定位，便于快速回看与整理。</p>
    </header>

    <div class="mistake-layout">
      <section class="mistake-main">
        <form class="filter-bar" @submit.prevent="applyFilters">
          <input v-model.trim="filters.keyword" type="text" placeholder="搜索错题题干、关键词" />
          <button class="primary-btn" type="submit">筛选</button>
        </form>

        <div v-if="libraryStore.loadingMistakes" class="panel-card">正在加载错题数据...</div>
        <div v-else-if="!libraryStore.mistakes.length" class="panel-card">当前还没有错题记录。</div>

        <article v-else-if="activeMistake" class="mistake-card-detail">
          <div class="mistake-toolbar">
            <div class="mistake-toolbar__meta">
              <span class="mistake-counter">错题 {{ activeIndex + 1 }} / {{ libraryStore.mistakes.length }}</span>
              <span class="tag source">{{ sourceText(activeMistake) }}</span>
            </div>
            <div class="mistake-toolbar__actions">
              <button class="outline-btn" type="button" :disabled="activeIndex === 0" @click="selectPrev">上一题</button>
              <button
                class="outline-btn"
                type="button"
                :disabled="activeIndex >= libraryStore.mistakes.length - 1"
                @click="selectNext"
              >
                下一题
              </button>
            </div>
          </div>

          <div class="mistake-question-head">
            <h3>{{ activeMistake.questionTitle }}</h3>
            <time>{{ formatDate(activeMistake.updatedAt || activeMistake.createdAt) }}</time>
          </div>

          <p class="mistake-question-content">{{ activeMistake.questionContent }}</p>

          <div class="mistake-section-row">
            <span class="tag chapter">{{ sectionLabel(activeMistake.chapterId) }}</span>
          </div>

          <div v-if="canCheckAnswer" class="mistake-options">
            <button
              v-for="option in activeMistake.options"
              :key="option"
              type="button"
              class="mistake-option"
              :class="optionClass(option)"
              @click="pickOption(option)"
            >
              <span class="mistake-option__code">{{ optionCode(option) }}.</span>
              <span class="mistake-option__text">{{ optionText(option) }}</span>
            </button>
          </div>

          <div v-else class="mistake-readonly-tip">
            当前错题缺少原题选项数据，暂不支持在线检查答案，可继续加入收藏本或删除。
          </div>

          <button
            class="check-answer-btn"
            type="button"
            :disabled="!canCheckAnswer || !libraryStore.selectedMistakeAnswer || libraryStore.isAnswerRevealed"
            @click="checkAnswer"
          >
            检查答案
          </button>

          <div v-if="libraryStore.isAnswerRevealed" class="mistake-result-stack">
            <div class="mistake-answer-box">
              <strong>正确答案：</strong>
              <span>{{ activeMistake.correctAnswer }}</span>
            </div>

            <div class="mistake-feedback" :class="isCorrectAnswer ? 'success' : 'danger'">
              <span class="mistake-feedback__icon">{{ isCorrectAnswer ? '✓' : '!' }}</span>
              <span>{{ isCorrectAnswer ? '回答正确！' : '回答错误！' }}</span>
            </div>

            <div class="mistake-explanation-box">
              <strong>解析：</strong>
              <p>{{ activeMistake.explanation || '暂无解析' }}</p>
            </div>
          </div>

          <footer class="mistake-detail-actions">
            <button class="outline-btn favorite-btn" type="button" @click="addFavorite(activeMistake)">加入收藏本</button>
            <button class="danger-btn" type="button" @click="remove(activeMistake.id)">删除</button>
          </footer>
        </article>
      </section>

      <aside class="mistake-side">
        <article class="side-card chapter-tree-card">
          <div class="chapter-tree-head">
            <div>
              <p class="eyebrow">章节筛选</p>
              <h3>章节目录</h3>
            </div>
            <button
              class="outline-btn"
              type="button"
              :disabled="!libraryStore.selectedChapterId"
              @click="clearSectionFilter"
            >
              清空筛选
            </button>
          </div>

          <div v-if="!chapterTree.length" class="muted">章节目录加载中...</div>

          <div v-else class="collapsible-tree">
            <article v-for="book in chapterTree" :key="book.id" class="book-node">
              <button class="tree-toggle book-toggle" type="button" @click="toggleBook(book.id)">
                <span class="tree-caret" :class="{ expanded: isBookExpanded(book.id) }">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M9 6l6 6-6 6" />
                  </svg>
                </span>
                <span>{{ book.title }}</span>
              </button>

              <div v-if="isBookExpanded(book.id)" class="chapter-branch">
                <article v-for="chapter in book.children || []" :key="chapter.id" class="chapter-node">
                  <button class="tree-toggle chapter-toggle" type="button" @click="toggleChapter(chapter.id)">
                    <span class="tree-caret" :class="{ expanded: isChapterExpanded(chapter.id) }">
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M9 6l6 6-6 6" />
                      </svg>
                    </span>
                    <span>{{ chapter.title }}</span>
                  </button>

                  <div v-if="isChapterExpanded(chapter.id)" class="section-branch">
                    <button
                      v-for="section in chapter.children || []"
                      :key="section.id"
                      type="button"
                      class="section-link"
                      :class="{ active: Number(libraryStore.selectedChapterId) === Number(section.id) }"
                      @click="selectSection(section.id)"
                    >
                      <span>{{ section.title }}</span>
                    </button>
                  </div>
                </article>
              </div>
            </article>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, unwrap } from '../api/client'
import { useLibraryStore } from '../stores/library'
import { buildSectionLabelMap, resolveSectionLabel } from '../utils/chapterLabels'

const libraryStore = useLibraryStore()
const filters = reactive({
  keyword: ''
})
const chapterTree = ref([])
const sectionLabelMap = ref(new Map())
const expandedBooks = ref(new Set())
const expandedChapters = ref(new Set())

const activeMistake = computed(() => libraryStore.activeMistake)
const activeIndex = computed(() => {
  if (!activeMistake.value) return -1
  return libraryStore.mistakes.findIndex((item) => item.id === activeMistake.value.id)
})
const canCheckAnswer = computed(() => libraryStore.canCheckActiveMistake)
const isCorrectAnswer = computed(() => {
  if (!activeMistake.value || !libraryStore.isAnswerRevealed) return false
  return normalizeAnswer(activeMistake.value.correctAnswer) === normalizeAnswer(libraryStore.selectedMistakeAnswer)
})

onMounted(async () => {
  filters.keyword = libraryStore.mistakeFilters.keyword || ''
  await Promise.all([loadChapterTree(), applyFilters()])
})

async function loadChapterTree() {
  try {
    chapterTree.value = await unwrap(api.get('/chapters/tree'))
    sectionLabelMap.value = buildSectionLabelMap(chapterTree.value)
    expandedBooks.value = new Set(chapterTree.value.map((item) => item.id))
    expandedChapters.value = new Set(
      chapterTree.value.flatMap((book) => (book.children || []).map((chapter) => chapter.id))
    )
  } catch {
    chapterTree.value = []
    sectionLabelMap.value = new Map()
    expandedBooks.value = new Set()
    expandedChapters.value = new Set()
  }
}

async function applyFilters() {
  libraryStore.mistakeFilters = {
    keyword: filters.keyword,
    chapterId: libraryStore.selectedChapterId
  }
  await libraryStore.loadMistakes()
}

function sourceText(item) {
  return item?.sourceLabel || '题目来源未标注'
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

function sectionLabel(chapterId) {
  return resolveSectionLabel(sectionLabelMap.value, chapterId)
}

function optionCode(option) {
  return String(option).split('.')[0]?.trim() || ''
}

function optionText(option) {
  const parts = String(option).split('.')
  return parts.length > 1 ? parts.slice(1).join('.').trim() : String(option)
}

function normalizeAnswer(value) {
  return String(value || '').trim().toUpperCase()
}

function pickOption(option) {
  if (!canCheckAnswer.value || libraryStore.isAnswerRevealed) return
  libraryStore.setMistakeAnswer(optionCode(option))
}

function optionClass(option) {
  const code = optionCode(option)
  if (!libraryStore.isAnswerRevealed) {
    return {
      selected: normalizeAnswer(libraryStore.selectedMistakeAnswer) === code
    }
  }
  return {
    selected: normalizeAnswer(libraryStore.selectedMistakeAnswer) === code,
    correct: normalizeAnswer(activeMistake.value?.correctAnswer) === code,
    wrong:
      normalizeAnswer(libraryStore.selectedMistakeAnswer) === code &&
      normalizeAnswer(activeMistake.value?.correctAnswer) !== code
  }
}

function checkAnswer() {
  if (!canCheckAnswer.value || !libraryStore.selectedMistakeAnswer) return
  libraryStore.revealActiveMistake()
}

function selectPrev() {
  if (activeIndex.value <= 0) return
  libraryStore.selectMistake(libraryStore.mistakes[activeIndex.value - 1].id)
}

function selectNext() {
  if (activeIndex.value >= libraryStore.mistakes.length - 1) return
  libraryStore.selectMistake(libraryStore.mistakes[activeIndex.value + 1].id)
}

async function addFavorite(item) {
  await unwrap(
    api.post('/favorites', {
      questionId: item.questionId,
      chapterId: item.chapterId,
      title: item.questionTitle,
      content: item.questionContent
    })
  )
}

async function remove(id) {
  await unwrap(api.delete(`/mistakes/${id}`))
  await libraryStore.loadMistakes()
}

function toggleBook(id) {
  const next = new Set(expandedBooks.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedBooks.value = next
}

function toggleChapter(id) {
  const next = new Set(expandedChapters.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedChapters.value = next
}

function isBookExpanded(id) {
  return expandedBooks.value.has(id)
}

function isChapterExpanded(id) {
  return expandedChapters.value.has(id)
}

async function selectSection(id) {
  libraryStore.setSelectedChapter(id)
  await applyFilters()
}

async function clearSectionFilter() {
  libraryStore.setSelectedChapter(null)
  await applyFilters()
}
</script>
