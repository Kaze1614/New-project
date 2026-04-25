<template>
  <section class="question-list-page">
    <div v-if="successMessage" class="admin-alert success">{{ successMessage }}</div>

    <section class="admin-section question-list-toolbar" aria-label="题目管理筛选">
      <div>
        <p class="section-eyebrow">Question Bank</p>
        <h2>题库内容</h2>
        <p class="muted">集中查看、检索、修改和删除后台录入的数学题。</p>
      </div>
      <form class="question-search" @submit.prevent="loadQuestions(1)">
        <label>
          <span class="sr-only">搜索题目</span>
          <input v-model.trim="keyword" type="search" placeholder="搜索题干、章节、来源" />
        </label>
        <button class="outline-btn" type="submit">搜索</button>
        <button class="outline-btn" type="button" @click="refresh">刷新</button>
        <RouterLink class="primary-btn" to="/admin/questions/new">新增题目</RouterLink>
      </form>
    </section>

    <section class="admin-section question-table-card" aria-label="题目列表">
      <div class="table-meta">
        <span>共 {{ pagination.total }} 道题</span>
        <span>第 {{ pagination.page }} 页</span>
      </div>

      <div v-if="loading" class="table-empty">正在加载题目...</div>
      <div v-else-if="error" class="table-empty error-text">{{ error }}</div>
      <div v-else-if="!questions.length" class="table-empty">暂无题目，可通过右上角按钮新增题目。</div>

      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>题号</th>
              <th>章节路径</th>
              <th>题干摘要</th>
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
                  <RouterLink class="outline-btn" :to="`/admin/questions/${question.id}/edit`">修改</RouterLink>
                  <button class="danger-btn" type="button" @click="deleteQuestion(question)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-row">
        <button class="outline-btn" type="button" :disabled="pagination.page <= 1 || loading" @click="loadQuestions(pagination.page - 1)">上一页</button>
        <button class="outline-btn" type="button" :disabled="!hasNextPage || loading" @click="loadQuestions(pagination.page + 1)">下一页</button>
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
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const successMessage = computed(() => {
  if (route.query.saved === 'created') return '题目已保存，已返回题目管理列表。'
  if (route.query.saved === 'updated') return '题目已更新，已返回题目管理列表。'
  if (route.query.deleted === '1') return '题目已删除。'
  return ''
})

const hasNextPage = computed(() => pagination.page * pagination.size < pagination.total)

onMounted(() => loadQuestions(1))

async function loadQuestions(page = pagination.page) {
  loading.value = true
  error.value = ''
  try {
    const data = await unwrap(api.get('/admin/math-questions', {
      params: {
        keyword: keyword.value || undefined,
        page,
        size: pagination.size
      }
    }))
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

function refresh() {
  if (route.query.saved || route.query.deleted) {
    router.replace({ path: '/admin/questions' })
  }
  loadQuestions(pagination.page)
}

async function deleteQuestion(question) {
  const confirmed = window.confirm(`确认删除第 ${question.questionNo} 题？该操作不可恢复。`)
  if (!confirmed) return
  loading.value = true
  error.value = ''
  try {
    await unwrap(api.delete(`/admin/math-questions/${question.id}`))
    await router.replace({ path: '/admin/questions', query: { deleted: '1' } })
    await loadQuestions(1)
  } catch (err) {
    error.value = err?.response?.data?.message || '删除失败'
  } finally {
    loading.value = false
  }
}

function formatSource(question) {
  if (!question.sourceYear || !question.sourcePaper) return `${question.questionNo}.`
  return `${question.questionNo}.(${question.sourceYear})(${question.sourcePaper})`
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>
