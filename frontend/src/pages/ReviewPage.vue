<template>
  <section class="review-page">
    <header class="study-head">
      <h2>待复习</h2>
    </header>

    <div v-if="reviewStore.loading" class="panel-card">正在加载待复习题库...</div>
    <div v-else-if="!reviewStore.hasTasks" class="panel-card">当前没有待复习题目，可以先回到刷题页继续训练。</div>
    <div v-else class="study-layout">
      <article class="study-main">
        <div class="question-meta">
          <p class="question-index">第 {{ currentIndex + 1 }} / {{ reviewStore.tasks.length }} 题</p>
          <span v-if="currentSourceLabel" class="source-pill">{{ currentSourceLabel }}</span>
        </div>

        <h3 class="question-title">{{ currentTask.questionTitle }}</h3>
        <p class="question-content">{{ currentQuestionContent }}</p>
        <p v-if="currentTask.sourceSnapshotPath" class="source-path">原卷资源：{{ currentTask.sourceSnapshotPath }}</p>

        <div class="answer-panel">
          <template v-if="isChoice(currentTask)">
            <button
              v-for="option in currentTask.options"
              :key="option"
              type="button"
              class="option-card"
              :class="{ selected: isSelected(option) }"
              :disabled="reviewStore.submitted"
              @click="selectOption(option)"
            >
              {{ option }}
            </button>
          </template>

          <template v-else>
            <label class="fill-answer">
              作答区域
              <div class="fill-row">
                <input
                  v-model.trim="fillDraft"
                  type="text"
                  maxlength="200"
                  :disabled="reviewStore.submitted"
                  @keyup.enter="saveFillAnswer"
                />
                <button class="outline-btn" type="button" :disabled="reviewStore.submitted" @click="saveFillAnswer">
                  保存答案
                </button>
              </div>
            </label>
          </template>
        </div>

        <div v-if="reviewStore.submitted && currentTask.result" class="result-panel">
          <p><strong>正确答案：</strong>{{ currentTask.result.correctAnswer || '暂无' }}</p>
          <p><strong>解析：</strong>{{ currentTask.result.explanation || '无' }}</p>
          <p><strong>本题结果：</strong>{{ resultLabel(currentTask.result) }}</p>
        </div>

        <footer class="study-actions">
          <button class="outline-btn" type="button" :disabled="currentIndex === 0" @click="goPrev">上一题</button>
          <button class="outline-btn" type="button" :disabled="currentIndex >= reviewStore.tasks.length - 1" @click="goNext">下一题</button>
          <button class="primary-btn" type="button" :disabled="reviewStore.submitted || reviewStore.submitting" @click="submitReview">
            {{ reviewStore.submitting ? '提交中...' : '提交本组' }}
          </button>
        </footer>
        <p v-if="actionError" class="error-text">{{ actionError }}</p>
      </article>

      <aside class="study-side">
        <article class="side-card timer-card">
          <h4>计时器</h4>
          <p class="timer-value">{{ displayTime }}</p>
        </article>

        <article class="side-card">
          <h4>导航矩阵</h4>
          <div class="matrix-grid">
            <button
              v-for="(task, index) in reviewStore.tasks"
              :key="task.id"
              type="button"
              class="matrix-cell"
              :class="matrixClass(index, task)"
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
import { useReviewStore } from '../stores/review'
import { normalizeStudentSourceLabel, stripStudentQuestionNoPrefix } from '../utils/sourceLabel'

const reviewStore = useReviewStore()

const currentIndex = ref(0)
const fillDraft = ref('')
const actionError = ref('')
const startedAt = ref(Date.now())
const tick = ref(Date.now())
let timer = null

const currentTask = computed(() => reviewStore.tasks[currentIndex.value] ?? null)
const currentSourceLabel = computed(() => normalizeStudentSourceLabel(currentTask.value?.sourceLabel))
const currentQuestionContent = computed(() => stripStudentQuestionNoPrefix(currentTask.value?.questionContent))

const elapsedSeconds = computed(() => {
  const end = reviewStore.submitted ? new Date(reviewStore.submittedAt).getTime() : tick.value
  return Math.max(0, Math.floor((end - startedAt.value) / 1000))
})

const displayTime = computed(() => {
  const total = elapsedSeconds.value
  const hours = Math.floor(total / 3600).toString().padStart(2, '0')
  const minutes = Math.floor((total % 3600) / 60).toString().padStart(2, '0')
  const seconds = (total % 60).toString().padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
})

watch(currentTask, (task) => {
  fillDraft.value = task?.userAnswer || ''
})

watch(
  () => reviewStore.tasks.length,
  () => {
    currentIndex.value = 0
    startedAt.value = Date.now()
    tick.value = Date.now()
    actionError.value = ''
  }
)

onMounted(async () => {
  try {
    await reviewStore.loadDueTasks()
    startedAt.value = Date.now()
    tick.value = Date.now()
  } catch (error) {
    actionError.value = error?.response?.data?.message || '加载待复习题库失败，请稍后重试'
  }

  timer = window.setInterval(() => {
    tick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

function isChoice(task) {
  return ['SINGLE', 'MULTI'].includes(task?.type) && Array.isArray(task?.options) && task.options.length > 0
}

function extractOptionCode(option) {
  const match = String(option).match(/^([A-Z])/)
  return match ? match[1] : String(option).trim().toUpperCase()
}

function selectedCodes() {
  if (!currentTask.value?.userAnswer) return []
  return currentTask.value.userAnswer
    .split(',')
    .map((value) => value.trim().toUpperCase())
    .filter(Boolean)
}

function isSelected(option) {
  return selectedCodes().includes(extractOptionCode(option))
}

function selectOption(option) {
  if (!currentTask.value || reviewStore.submitted) return
  actionError.value = ''
  const code = extractOptionCode(option)

  if (currentTask.value.type === 'MULTI') {
    const next = new Set(selectedCodes())
    next.has(code) ? next.delete(code) : next.add(code)
    reviewStore.saveAnswer(currentTask.value.id, [...next].sort().join(','))
    return
  }

  reviewStore.saveAnswer(currentTask.value.id, code)
}

function saveFillAnswer() {
  if (!currentTask.value || reviewStore.submitted || !fillDraft.value) return
  actionError.value = ''
  reviewStore.saveAnswer(currentTask.value.id, fillDraft.value)
}

function goPrev() {
  currentIndex.value = Math.max(0, currentIndex.value - 1)
}

function goNext() {
  currentIndex.value = Math.min(reviewStore.tasks.length - 1, currentIndex.value + 1)
}

function jumpTo(index) {
  currentIndex.value = index
}

function matrixClass(index, task) {
  if (index === currentIndex.value) return 'current'
  if (task.result) return task.result.correct ? 'answered-correct' : 'answered-wrong'
  if (task.answered) return 'answered'
  return 'empty'
}

async function submitReview() {
  actionError.value = ''
  try {
    await reviewStore.submitBatch()
  } catch (error) {
    actionError.value = error?.response?.data?.message || '提交复习结果失败，请稍后重试'
  }
}

function resultLabel(result) {
  if (result.removedFromMistakes) {
    return '本题已完成复习，已从错题本移出'
  }
  if (!result.answered) {
    return '未作答，本题保留在待复习题库'
  }
  if (result.correct) {
    return '回答正确，已进入下一轮复习'
  }
  return '回答错误，已重新进入当前复习队列'
}
</script>
