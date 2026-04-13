<template>
  <section class="study-page">
    <header class="study-head">
      <h2>沉浸式刷题</h2>
      <p>题目与解析均来自题库，提交后才展示官方答案。</p>
    </header>

    <div v-if="studyStore.loading" class="panel-card">组卷中...</div>
    <div v-else-if="!session" class="panel-card">当前暂无可用学习会话。</div>
    <div v-else class="study-layout">
      <article class="study-main">
        <p class="question-index">第 {{ currentIndex + 1 }} / {{ session.items.length }} 题</p>
        <h3 class="question-title">{{ currentItem.title }}</h3>
        <p class="question-content">{{ currentItem.content }}</p>

        <div class="answer-panel">
          <template v-if="isSingleChoice(currentItem)">
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
              填空答案
              <div class="fill-row">
                <input v-model.trim="fillDraft" type="text" maxlength="100" @keyup.enter="saveFillAnswer" />
                <button class="outline-btn" type="button" @click="saveFillAnswer">保存答案</button>
              </div>
            </label>
          </template>
        </div>

        <div v-if="session.submitted" class="result-panel">
          <p>
            <strong>官方答案：</strong>
            {{ currentItem.officialAnswer || '无' }}
          </p>
          <p>
            <strong>官方解析：</strong>
            {{ currentItem.officialExplanation || '暂无解析' }}
          </p>
          <button class="ghost-btn" type="button" @click="askAIForCurrent">
            💡 没看懂解析？
          </button>
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
import { useUiStore } from '../stores/ui'

const route = useRoute()
const studyStore = useStudyStore()
const uiStore = useUiStore()

const currentIndex = ref(0)
const fillDraft = ref('')
const tick = ref(Date.now())
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
  await studyStore.createSession(chapterId)
  timer = window.setInterval(() => {
    tick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

function isSingleChoice(item) {
  return item?.type === 'SINGLE' && Array.isArray(item?.options) && item.options.length > 0
}

function extractOptionCode(option) {
  const match = String(option).match(/^([A-Z])/)
  return match ? match[1] : String(option).trim().toUpperCase()
}

function isSelected(option) {
  if (!currentItem.value?.userAnswer) return false
  return currentItem.value.userAnswer.toUpperCase() === extractOptionCode(option)
}

async function selectOption(option) {
  if (!session.value || !currentItem.value || session.value.submitted) return
  await studyStore.saveAnswer(session.value.id, currentItem.value.itemId, extractOptionCode(option))
}

async function saveFillAnswer() {
  if (!session.value || !currentItem.value || session.value.submitted || !fillDraft.value) return
  await studyStore.saveAnswer(session.value.id, currentItem.value.itemId, fillDraft.value)
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
  await studyStore.submitSession(session.value.id)
}

function askAIForCurrent() {
  if (!currentItem.value) return
  const context = `题目：${currentItem.value.content}\n官方答案：${currentItem.value.officialAnswer || '无'}\n官方解析：${currentItem.value.officialExplanation || '暂无'}`
  uiStore.openAIDrawer(context)
}
</script>
