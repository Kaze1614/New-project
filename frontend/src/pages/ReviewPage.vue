<template>
  <section class="review-page">
    <header class="review-head">
      <h2>渐进式复习</h2>
      <p>盲回演算后再看官方解析，最后进行自我评级。</p>
    </header>

    <article v-if="reviewStore.loading" class="panel-card">加载复习任务中...</article>

    <article v-else-if="!reviewStore.currentTask" class="panel-card empty-review">
      <h3>今日复习已清空</h3>
      <p>当前没有到期卡片，可先去刷题页继续训练。</p>
    </article>

    <article v-else class="review-card">
      <p class="review-badge">任务截止：{{ formatTime(reviewStore.currentTask.dueDate) }}</p>
      <h3>{{ reviewStore.currentTask.questionTitle }}</h3>
      <p class="review-content">{{ reviewStore.currentTask.questionContent }}</p>

      <section v-if="reviewStore.phase === 'blind'" class="blind-phase">
        <p>请先在草稿纸独立演算，再点击下方按钮查看标准解析。</p>
        <button class="primary-btn" type="button" @click="reviewStore.reveal()">查看标准解析</button>
      </section>

      <section v-else class="reveal-phase">
        <div class="official-box">
          <p><strong>官方答案：</strong>{{ reviewStore.currentTask.officialAnswer || '无' }}</p>
          <p><strong>官方解析：</strong>{{ reviewStore.currentTask.officialExplanation || '暂无解析' }}</p>
          <button class="ghost-btn" type="button" @click="openAIExplain">💡 没看懂解析？</button>
        </div>

        <div class="rating-row">
          <button class="rating danger" type="button" :disabled="reviewStore.rating" @click="rate('again')">
            🔴 算错了
          </button>
          <button class="rating warn" type="button" :disabled="reviewStore.rating" @click="rate('hard')">
            🟡 有点模糊
          </button>
          <button class="rating ok" type="button" :disabled="reviewStore.rating" @click="rate('easy')">
            🟢 轻松秒杀
          </button>
        </div>
      </section>
    </article>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useReviewStore } from '../stores/review'
import { useUiStore } from '../stores/ui'

const reviewStore = useReviewStore()
const uiStore = useUiStore()

onMounted(() => {
  reviewStore.loadNext()
})

function formatTime(value) {
  if (!value) return '--'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function rate(grade) {
  await reviewStore.rate(grade)
}

function openAIExplain() {
  if (!reviewStore.currentTask) return
  const context = `题目：${reviewStore.currentTask.questionContent}\n官方解析：${reviewStore.currentTask.officialExplanation || '暂无'}`
  uiStore.openAIDrawer(context)
}
</script>
