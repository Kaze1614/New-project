<template>
  <section class="dashboard-page">
    <article v-if="isGuest" class="panel-card dashboard-guest">
      <div>
        <h3>欢迎来到数学园</h3>
        <p>登录后可查看个人学习进度、错题复习与学情简报。</p>
      </div>
      <button class="primary-btn" type="button" @click="openLoginModal">登录解锁</button>
    </article>

    <div class="kpi-grid">
      <article
        v-for="card in kpiCards"
        :key="card.label"
        class="kpi-card"
        :class="{ locked: isGuest }"
        :tabindex="isGuest ? 0 : undefined"
        :role="isGuest ? 'button' : undefined"
        @click="handleLockedContent"
        @keydown.enter.prevent="handleLockedContent"
      >
        <p class="kpi-label">{{ card.label }}</p>
        <p class="kpi-value">{{ card.value }}</p>
      </article>
    </div>

    <div class="action-grid">
      <RouterLink class="action-card primary" :to="isGuest ? '/dashboard' : '/study'" @click="guardGuestAction">
        <h3>前去刷题</h3>
        <p>{{ isGuest ? '登录后开启题组训练。' : '继续题组训练，维持手感。' }}</p>
      </RouterLink>

      <RouterLink class="action-card warning" :to="isGuest ? '/dashboard' : '/review'" @click="guardGuestAction">
        <h3>前去复习</h3>
        <p>{{ reviewHint }}</p>
        <div class="ring-wrap">
          <svg viewBox="0 0 120 120" class="ring" aria-hidden="true">
            <circle cx="60" cy="60" r="48" class="ring-track" />
            <circle
              cx="60"
              cy="60"
              r="48"
              class="ring-progress"
              :style="{ strokeDasharray: `${ringValue} 400` }"
            />
          </svg>
          <span>{{ ringPercent }}%</span>
        </div>
      </RouterLink>

      <article
        class="action-card muted"
        :class="{ locked: isGuest }"
        :tabindex="isGuest ? 0 : undefined"
        :role="isGuest ? 'button' : undefined"
        @click="handleLockedContent"
        @keydown.enter.prevent="handleLockedContent"
      >
        <h3>学情简报</h3>
        <p>{{ isGuest ? '登录后生成个人薄弱项提示。' : (overview.weakSpotHint || '暂无薄弱项数据') }}</p>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showLoginModal" class="login-modal-backdrop" role="presentation" @click.self="closeLoginModal">
        <section class="login-modal" role="dialog" aria-modal="true" aria-labelledby="login-modal-title">
          <p class="eyebrow">登录后继续</p>
          <h3 id="login-modal-title">请先登录以解锁完整学情分析与复习功能</h3>
          <p>数学园会保存刷题进度、错题记录和复习节奏，方便持续跟踪学习状态。</p>
          <div class="login-modal-actions">
            <button class="primary-btn" type="button" @click="goLogin">前往登录</button>
            <button class="outline-btn" type="button" @click="closeLoginModal">稍后再说</button>
          </div>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { api, tokenStorage, unwrap } from '../api/client'

const EMPTY_TEXT = '--'

const router = useRouter()
const isGuest = ref(!tokenStorage.get())
const showLoginModal = ref(false)

const dashboardStats = reactive({
  totalQuestions: null,
  mastered: null,
  toReview: null,
  completedToday: null
})

const overview = reactive({
  criticalReviewCount: 0,
  weakSpotHint: ''
})

const kpiCards = computed(() => [
  { label: '题库总量', value: formatStat(dashboardStats.totalQuestions) },
  { label: '已经掌握', value: formatStat(dashboardStats.mastered) },
  { label: '待复习', value: formatStat(dashboardStats.toReview) },
  { label: '今日完成', value: formatStat(dashboardStats.completedToday) }
])

const reviewHint = computed(() => {
  if (isGuest.value) {
    return '登录后查看今日复习任务与遗忘临界点。'
  }
  return `今日有 ${overview.criticalReviewCount} 道题到达遗忘临界点。`
})

const ringPercent = computed(() => {
  if (isGuest.value) return 0
  const pendingReview = dashboardStats.toReview ?? 0
  if (pendingReview <= 0) return 0
  return Math.min(100, Math.round((overview.criticalReviewCount / pendingReview) * 100))
})

const ringValue = computed(() => Number((ringPercent.value / 100) * 302).toFixed(2))

function formatStat(value) {
  if (isGuest.value || value === null || value === undefined) {
    return EMPTY_TEXT
  }
  return value
}

function openLoginModal() {
  showLoginModal.value = true
}

function closeLoginModal() {
  showLoginModal.value = false
}

function handleLockedContent() {
  if (isGuest.value) {
    openLoginModal()
  }
}

function guardGuestAction(event) {
  if (!isGuest.value) return
  event.preventDefault()
  openLoginModal()
}

function goLogin() {
  closeLoginModal()
  router.push('/login')
}

async function loadOverview() {
  if (isGuest.value) return

  try {
    const data = await unwrap(api.get('/dashboard/overview'))
    dashboardStats.totalQuestions = data.questionBankTotal ?? null
    dashboardStats.mastered = data.mastered ?? null
    dashboardStats.toReview = data.pendingReview ?? null
    dashboardStats.completedToday = data.todayCompleted ?? null
    overview.criticalReviewCount = data.criticalReviewCount ?? 0
    overview.weakSpotHint = data.weakSpotHint ?? ''
  } catch (error) {
    if (error?.response?.status === 401) {
      isGuest.value = true
      dashboardStats.totalQuestions = null
      dashboardStats.mastered = null
      dashboardStats.toReview = null
      dashboardStats.completedToday = null
      return
    }
    console.error('dashboard load failed', error)
  }
}

onMounted(loadOverview)
</script>
