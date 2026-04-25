<template>
  <section class="review-page">
    <header class="review-head">
      <h2>渐进式复习</h2>
      <p>先盲回演算，再查看标准答案和解析，最后按真实掌握程度评级。</p>
    </header>

    <article v-if="reviewStore.loading" class="panel-card">加载复习任务中...</article>

    <article v-else-if="!reviewStore.currentTask" class="panel-card empty-review">
      <h3>今日复习已清空</h3>
      <p>当前没有到期卡片，可以先去刷题页继续训练。</p>
    </article>

    <article v-else class="review-card">
      <div class="question-meta">
        <p class="review-badge">任务截止：{{ formatTime(reviewStore.currentTask.dueDate) }}</p>
        <span v-if="reviewStore.currentTask.sourceLabel" class="source-pill">
          {{ reviewStore.currentTask.sourceLabel }}
        </span>
      </div>

      <h3>{{ reviewStore.currentTask.questionTitle }}</h3>
      <p class="review-content">{{ reviewStore.currentTask.questionContent }}</p>
      <p v-if="reviewStore.currentTask.sourceSnapshotPath" class="source-path">
        原卷资源：{{ reviewStore.currentTask.sourceSnapshotPath }}
      </p>

      <section v-if="reviewStore.phase === 'blind'" class="blind-phase">
        <p>请先在草稿纸独立演算，再点击下方按钮查看标准答案与解析。</p>
        <button class="primary-btn" type="button" @click="reviewStore.reveal()">查看标准解析</button>
      </section>

      <section v-else class="reveal-phase">
        <div class="official-box">
          <p><strong>标准答案：</strong>{{ reviewStore.currentTask.officialAnswer || '暂无' }}</p>
          <p>
            <strong>{{ explanationTitle(reviewStore.currentTask) }}：</strong>
            {{ reviewStore.currentTask.officialExplanation || '暂无解析' }}
          </p>
          <p v-if="reviewStore.currentTask.explanationReviewStatus === 'PENDING_REVIEW'" class="review-warning">
            该解析为教师补充解析或题面含公式图片，建议人工复核后用于正式练习。
          </p>
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

const reviewStore = useReviewStore()

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

function explanationTitle(task) {
  return task?.explanationSource === 'TEACHER_GENERATED' ? '教师补充解析' : '官方解析'
}

</script>
