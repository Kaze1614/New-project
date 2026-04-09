<template>
  <section>
    <div class="stats-grid">
      <StatCard title="题目总量" :value="overview.totalMistakes" />
      <StatCard title="已经掌握" :value="overview.mastered" />
      <StatCard title="待复习" :value="overview.pendingReview" hint="点击左侧待复习查看" highlight />
      <StatCard title="今日完成" :value="overview.todayCompleted" />
    </div>

    <div class="panel-grid">
      <section class="panel-card">
        <h3></h3>
        <div class="panel-content">
          <p class="big-title"></p>
          <p>。</p>
          <a class="text-link"  target="_blank" rel="noreferrer"></a>
        </div>
      </section>

      <section class="panel-card">
        <h3></h3>
        <div class="panel-content">
          <div class="qr-box"></div>
          <p class="big-title"></p>
          <p></p>
        </div>
      </section>

      <section class="panel-card">
        <h3>公告看板</h3>
        <div class="panel-content center">
          <p>登录后可查看最新公告。</p>
          <router-link class="text-link" to="/mistakes"></router-link>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { api, unwrap } from '../api/client'
import StatCard from '../components/StatCard.vue'

const overview = reactive({
  totalMistakes: 0,
  mastered: 0,
  pendingReview: 0,
  todayCompleted: 0
})

async function loadOverview() {
  try {
    const data = await unwrap(api.get('/dashboard/overview'))
    overview.totalMistakes = data.totalMistakes
    overview.mastered = data.mastered
    overview.pendingReview = data.pendingReview
    overview.todayCompleted = data.todayCompleted
  } catch (err) {
    // Allow guests to land on dashboard as the default page.
    if (err?.response?.status !== 401) {
      console.error('Failed to load dashboard overview', err)
    }
  }
}

onMounted(loadOverview)
</script>
