<template>
  <section class="question-list-page">
    <div v-if="successMessage" class="admin-alert success">{{ successMessage }}</div>

    <section class="admin-section question-list-toolbar" aria-label="题库筛选">
      <div class="question-list-toolbar__copy">
        <p class="section-eyebrow">QUESTION BANK</p>
        <h2>题库内容</h2>
        <p class="muted">集中查看、检索、修改和删除后台录入的数学题。</p>
      </div>

      <form class="question-search" @submit.prevent="loadQuestions(1)">
        <div class="question-search__fields">
          <select v-model="filters.bookId" @change="onBookChange">
            <option value="">教材</option>
            <option v-for="book in bookOptions" :key="book.id" :value="String(book.id)">
              {{ book.title }}
            </option>
          </select>

          <select
            v-model="filters.chapterId"
            :disabled="!chapterOptions.length"
            @change="onChapterChange"
          >
            <option value="">章节</option>
            <option v-for="chapter in chapterOptions" :key="chapter.id" :value="String(chapter.id)">
              {{ chapter.title }}
            </option>
          </select>

          <select v-model="filters.sectionId" :disabled="!sectionOptions.length">
            <option value="">小节</option>
            <option v-for="section in sectionOptions" :key="section.id" :value="String(section.id)">
              {{ section.title }}
            </option>
          </select>
        </div>

        <div class="question-search__actions">
          <div class="question-search__query">
            <label class="question-search__keyword">
              <span class="sr-only">搜索题目</span>
              <input
                v-model.trim="keyword"
                type="search"
                placeholder="搜索题干、章节、来源"
              />
            </label>
          </div>
          <div class="question-search__buttons">
            <button class="outline-btn" type="submit">搜索</button>
            <button class="outline-btn" type="button" @click="refresh">刷新</button>
            <RouterLink class="primary-btn" to="/admin/questions/new">新增题目</RouterLink>
          </div>
        </div>
      </form>
    </section>

    <section class="admin-section question-table-card" aria-label="题目列表">
      <div class="table-meta">
        <span>共 {{ pagination.total }} 题</span>
        <span>第 {{ pagination.page }} 页</span>
      </div>

      <div v-if="loading" class="table-empty">正在加载题目列表...</div>
      <div v-else-if="error" class="table-empty error-text">{{ error }}</div>
      <div v-else-if="!questions.length" class="table-empty">
        当前筛选条件下没有题目，请调整关键词或教材范围。
      </div>

      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>题号</th>
              <th>教材关联</th>
              <th>题目摘要</th>
              <th>来源</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="question in questions" :key="question.id">
              <td class="mono">{{ question.questionNo }}</td>
              <td>
                <strong>{{ question.bookName }}</strong>
                <span>{{ question.chapterName }} / {{ question.sectionName }}</span>
              </td>
              <td>{{ question.contentPreview }}</td>
              <td>{{ question.sourceLabel || formatSource(question) }}</td>
              <td>{{ formatTime(question.updatedAt) }}</td>
              <td>
                <div class="table-actions">
                  <RouterLink class="outline-btn" :to="`/admin/questions/${question.id}/edit`">
                    修改
                  </RouterLink>
                  <button class="danger-btn" type="button" @click="deleteQuestion(question)">
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-row">
        <button
          class="outline-btn"
          type="button"
          :disabled="pagination.page <= 1 || loading"
          @click="loadQuestions(pagination.page - 1)"
        >
          上一页
        </button>
        <button
          class="outline-btn"
          type="button"
          :disabled="!hasNextPage || loading"
          @click="loadQuestions(pagination.page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { api, unwrap } from '../api/client'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const questions = ref([])
const loading = ref(false)
const error = ref('')
const chapterTree = ref([])
const filters = reactive({
  bookId: '',
  chapterId: '',
  sectionId: ''
})
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const successMessage = computed(() => {
  if (route.query.saved === 'created') return '题目已创建，列表已同步更新。'
  if (route.query.saved === 'updated') return '题目已更新，列表已同步更新。'
  if (route.query.deleted === '1') return '题目已删除。'
  return ''
})

const hasNextPage = computed(() => pagination.page * pagination.size < pagination.total)
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
  await Promise.all([loadChapterTree(), loadQuestions(1)])
})

async function loadChapterTree() {
  try {
    const data = await unwrap(api.get('/chapters/tree'))
    chapterTree.value = Array.isArray(data) ? data : []
  } catch {
    chapterTree.value = []
  }
}

async function loadQuestions(page = pagination.page) {
  loading.value = true
  error.value = ''
  try {
    const data = await unwrap(
      api.get('/admin/math-questions', {
        params: {
          keyword: keyword.value || undefined,
          bookId: filters.bookId ? Number(filters.bookId) : undefined,
          chapterId: filters.chapterId ? Number(filters.chapterId) : undefined,
          sectionId: filters.sectionId ? Number(filters.sectionId) : undefined,
          page,
          size: pagination.size
        }
      })
    )
    questions.value = data.items || []
    pagination.page = data.page || page
    pagination.size = data.size || pagination.size
    pagination.total = data.total || 0
  } catch (err) {
    error.value = err?.response?.data?.message || '题目列表加载失败'
    questions.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function onBookChange() {
  filters.chapterId = ''
  filters.sectionId = ''
}

function onChapterChange() {
  filters.sectionId = ''
}

function refresh() {
  keyword.value = keyword.value.trim()
  if (route.query.saved || route.query.deleted) {
    router.replace({ path: '/admin/questions' })
  }
  loadQuestions(1)
}

async function deleteQuestion(question) {
  const confirmed = window.confirm(`确认删除题号 ${question.questionNo} 的题目吗？该操作不可恢复。`)
  if (!confirmed) return
  loading.value = true
  error.value = ''
  try {
    await unwrap(api.delete(`/admin/math-questions/${question.id}`))
    await router.replace({ path: '/admin/questions', query: { deleted: '1' } })
    await loadQuestions(1)
  } catch (err) {
    error.value = err?.response?.data?.message || '删除题目失败'
  } finally {
    loading.value = false
  }
}

function formatSource(question) {
  const year = question.sourceYear ? `(${question.sourceYear})` : ''
  const paper = question.sourcePaper ? `(${question.sourcePaper})` : ''
  return `${year}${paper}` || '-'
}

function formatTime(value) {
  if (!value) return '-'
  const text = String(value).trim()
  const matched = text.match(/^(\d{4})-(\d{1,2})-(\d{1,2})[T\s](\d{1,2}):(\d{1,2}):(\d{1,2})/)
  if (matched) {
    const [, year, month, day, hour, minute, second] = matched
    return `${Number(year)}/${Number(month)}/${Number(day)} ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}:${second.padStart(2, '0')}`
  }

  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text.replace('T', ' ')

  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}
</script>
