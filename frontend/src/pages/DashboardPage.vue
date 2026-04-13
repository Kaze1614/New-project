<template>
  <section class="dashboard-page">
    <article v-if="isGuest" class="panel-card dashboard-guest">
      <h3>欢迎来到数学园</h3>
      <p>登录后查看个人学习进度、复习临界点与学情简报。</p>
      <RouterLink class="primary-btn" to="/login">去登录</RouterLink>
    </article>

    <div class="kpi-grid">
      <article v-for="card in kpiCards" :key="card.label" class="kpi-card">
        <p class="kpi-label">{{ card.label }}</p>
        <p class="kpi-value">{{ card.value }}</p>
      </article>
    </div>

    <div class="action-grid">
      <RouterLink class="action-card primary" :to="isGuest ? '/login' : '/study'">
        <h3>前去刷题</h3>
        <p>{{ isGuest ? '登录后开启 20 题沉浸式训练。' : '继续 20 题沉浸式训练，维持手感。' }}</p>
      </RouterLink>

      <RouterLink class="action-card warning" :to="isGuest ? '/login' : '/review'">
        <h3>前去复习</h3>
        <p>
          {{
            isGuest
              ? '登录后查看今日遗忘临界点题数。'
              : `今日有 ${overview.criticalReviewCount} 道题到达遗忘临界点。`
          }}
        </p>
        <div class="ring-wrap">
          <svg viewBox="0 0 120 120" class="ring">
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

      <article class="action-card muted">
        <h3>学情简报</h3>
        <p>{{ isGuest ? '登录后生成个人薄弱项提示。' : (overview.weakSpotHint || '暂无薄弱项数据') }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api, tokenStorage, unwrap } from '../api/client'

const overview = reactive({
  questionBankTotal: 0,
  totalMistakes: 0,
  mastered: 0,
  pendingReview: 0,
  todayCompleted: 0,
  criticalReviewCount: 0,
  weakSpotHint: ''
})

const isGuest = ref(!tokenStorage.get())

const kpiCards = computed(() => [
  { label: '题库总量', value: isGuest.value ? '--' : overview.questionBankTotal },
  { label: '已经掌握', value: isGuest.value ? '--' : overview.mastered },
  { label: '待复习', value: isGuest.value ? '--' : overview.pendingReview },
  { label: '今日完成', value: isGuest.value ? '--' : overview.todayCompleted }
])

const ringPercent = computed(() => {
  if (isGuest.value) {
    return 0
  }
  const base = Math.max(overview.pendingReview, 1)
  return Math.min(100, Math.round((overview.criticalReviewCount / base) * 100))
})

const ringValue = computed(() => Number((ringPercent.value / 100) * 302).toFixed(2))

async function loadOverview() {
  if (isGuest.value) {
    return
  }
  try {
    const data = await unwrap(api.get('/dashboard/overview'))
    Object.assign(overview, data)
  } catch (error) {
    if (error?.response?.status === 401) {
      isGuest.value = true
    } else {
      console.error('dashboard load failed', error)
    }
  }
}

onMounted(loadOverview)
</script>
