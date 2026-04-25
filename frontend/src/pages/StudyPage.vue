<template>
  <section class="study-page">
    <header class="study-head">
      <h2>沉浸式刷题</h2>
      <p>题目与答案均来自题库，提交后展示标准答案与解析来源。</p>
    </header>

    <div v-if="studyStore.loading" class="panel-card">组卷中...</div>
    <div v-else-if="!session" class="panel-card">当前暂无可用学习会话。</div>
    <div v-else class="study-layout">
      <article class="study-main">
        <div class="question-meta">
          <p class="question-index">第 {{ currentIndex + 1 }} / {{ session.items.length }} 题</p>
          <span v-if="currentItem.sourceLabel" class="source-pill">{{ currentItem.sourceLabel }}</span>
        </div>

        <h3 class="question-title">{{ currentItem.title }}</h3>
        <p class="question-content">{{ currentItem.content }}</p>
        <p v-if="currentItem.sourceSnapshotPath" class="source-path">
          原卷资源：{{ currentItem.sourceSnapshotPath }}
        </p>

        <div class="answer-panel">
          <template v-if="isChoice(currentItem)">
            <button
              v-for="option in currentItem.options || []"
              :key="option"
              type="button"
              class="option-card"
              :class="{ selected: isSelected(option) }"
              @click="selectOption(option)"
            >
              {{ option }}
            </button>
          </template>

          <template v-else>
            <label class="fill-answer">
              作答区域
              <div class="fill-row">
                <input v-model.trim="fillDraft" type="text" maxlength="200" @keyup.enter="saveFillAnswer" />
                <button class="outline-btn" type="button" @click="saveFillAnswer">保存答案</button>
              </div>
            </label>
          </template>
        </div>

        <div v-if="session.submitted" class="result-panel">
          <p>
            <strong>标准答案：</strong>
            {{ currentItem.officialAnswer || '暂无' }}
          </p>
          <p>
            <strong>{{ explanationTitle(currentItem) }}：</strong>
            {{ currentItem.officialExplanation || '暂无解析' }}
          </p>
          <p v-if="currentItem.explanationReviewStatus === 'PENDING_REVIEW'" class="review-warning">
            当前解析仍处于待复核状态，建议人工确认后再作为正式练习依据。
          </p>
        </div>

        <footer class="study-actions">
          <button class="outline-btn" type="button" :disabled="currentIndex === 0" @click="goPrev">上一题</button>
          <button
            class="outline-btn"
            type="button"
            :disabled="currentIndex >= session.items.length - 1"
            @click="goNext"
          >
            下一题
          </button>
          <button
            class="primary-btn"
            type="button"
            :disabled="session.submitted || studyStore.submitting"
            @click="submitSession"
          >
            {{ studyStore.submitting ? '提交中...' : '提交本组' }}
          </button>
        </footer>
        <p v-if="actionError" class="error-text">{{ actionError }}</p>
      </article>

      <aside class="study-side">
        <article class="side-card timer-card">
          <h4>全局计时</h4>
          <p class="timer-value">{{ displayTime }}</p>
        </article>

        <article class="side-card">
          <h4>20 题导航矩阵</h4>
          <div class="matrix-grid">
            <button
              v-for="(item, index) in session.items"
              :key="item.itemId"
              type="button"
              class="matrix-cell"
              :class="matrixClass(index, item)"
              @click="jumpTo(index)"
            >
              {{ index + 1 }}
            </button>
          </div>
        </article>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useStudyStore } from '../stores/study'

const route = useRoute()
const studyStore = useStudyStore()

const currentIndex = ref(0)
const fillDraft = ref('')
const tick = ref(Date.now())
const actionError = ref('')
let timer = null

const session = computed(() => studyStore.session)
const currentItem = computed(() => session.value?.items?.[currentIndex.value] ?? null)

const remainingSeconds = computed(() => {
  if (!session.value) return 0
  if (session.value.submitted) return 0
  const started = new Date(session.value.startedAt).getTime()
  const deadline = started + session.value.durationSeconds * 1000
  return Math.max(0, Math.floor((deadline - tick.value) / 1000))
})

const displayTime = computed(() => {
  const total = remainingSeconds.value
  const minutes = Math.floor(total / 60).toString().padStart(2, '0')
  const seconds = (total % 60).toString().padStart(2, '0')
  return `${minutes}:${seconds}`
})

watch(currentItem, (item) => {
  if (!item) return
  fillDraft.value = item.userAnswer || ''
})

onMounted(async () => {
  const chapterId = Number(route.query.chapterId) || null
  try {
    await studyStore.createSession(chapterId)
  } catch (error) {
    actionError.value = error?.response?.data?.message || '创建刷题会话失败，请稍后重试'
  }
  timer = window.setInterval(() => {
    tick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

function isChoice(item) {
  return ['SINGLE', 'MULTI'].includes(item?.type) && Array.isArray(item?.options) && item.options.length > 0
}

function extractOptionCode(option) {
  const match = String(option).match(/^([A-Z])/)
  return match ? match[1] : String(option).trim().toUpperCase()
}

function selectedCodes() {
  if (!currentItem.value?.userAnswer) return []
  return currentItem.value.userAnswer
    .split(',')
    .map((value) => value.trim().toUpperCase())
    .filter(Boolean)
}

function isSelected(option) {
  return selectedCodes().includes(extractOptionCode(option))
}

async function selectOption(option) {
  if (!session.value || !currentItem.value || session.value.submitted) return
  const code = extractOptionCode(option)
  actionError.value = ''
  try {
    if (currentItem.value.type === 'MULTI') {
      const next = new Set(selectedCodes())
      next.has(code) ? next.delete(code) : next.add(code)
      await studyStore.saveAnswer(session.value.id, currentItem.value.itemId, [...next].sort().join(''))
      return
    }
    await studyStore.saveAnswer(session.value.id, currentItem.value.itemId, code)
  } catch (error) {
    actionError.value = error?.response?.data?.message || '保存答案失败，请稍后重试'
  }
}

async function saveFillAnswer() {
  if (!session.value || !currentItem.value || session.value.submitted || !fillDraft.value) return
  actionError.value = ''
  try {
    await studyStore.saveAnswer(session.value.id, currentItem.value.itemId, fillDraft.value)
  } catch (error) {
    actionError.value = error?.response?.data?.message || '保存答案失败，请稍后重试'
  }
}

function goPrev() {
  currentIndex.value = Math.max(0, currentIndex.value - 1)
}

function goNext() {
  if (!session.value) return
  currentIndex.value = Math.min(session.value.items.length - 1, currentIndex.value + 1)
}

function jumpTo(index) {
  currentIndex.value = index
}

function matrixClass(index, item) {
  if (index === currentIndex.value) return 'current'
  if (item.answered) return 'answered'
  return 'empty'
}

async function submitSession() {
  if (!session.value || session.value.submitted) return
  actionError.value = ''
  try {
    await studyStore.submitSession(session.value.id)
  } catch (error) {
    actionError.value = error?.response?.data?.message || '提交本组失败，请稍后重试'
  }
}

function explanationTitle(item) {
  return item?.explanationSource === 'TEACHER_GENERATED' ? '教师补充解析' : '官方解析'
}
</script>
