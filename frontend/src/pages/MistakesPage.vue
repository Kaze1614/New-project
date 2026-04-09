<template>
  <section class="page-stack">
    <section class="panel-card">
      <h3>提交错题</h3>
      <form class="form-grid two-cols" @submit.prevent="createMistake">
        <label>
          所属章节ID（可选）
          <input v-model.number="form.chapterId" type="number" min="1" placeholder="例如：4" />
        </label>
        <label>
          题目标题
          <input v-model.trim="form.questionTitle" required maxlength="120" placeholder="例如：函数极限基础题" />
        </label>
        <label class="full">
          题目内容
          <textarea v-model.trim="form.questionContent" required maxlength="4000" rows="4" placeholder="输入题目文本或你的解题过程"></textarea>
        </label>
        <label class="full">
          图片地址（可选）
          <input v-model.trim="form.imageUrl" maxlength="1024" placeholder="先支持URL存储，后续可替换真实上传" />
        </label>
        <button class="primary-btn" type="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交错题' }}</button>
      </form>
    </section>

    <section class="panel-card">
      <h3>错题列表</h3>
      <p v-if="loading">加载中...</p>
      <p v-else-if="!mistakes.length">暂无错题，先提交一题试试。</p>
      <ul v-else class="item-list">
        <li v-for="item in mistakes" :key="item.id" class="item-card">
          <div class="item-head">
            <h4>{{ item.questionTitle }}</h4>
            <span class="status-chip" :class="item.status === 'MASTERED' ? 'ok' : 'todo'">{{ item.status }}</span>
          </div>
          <p>{{ item.questionContent }}</p>

          <div class="action-row">
            <button class="outline-btn" type="button" @click="analyze(item.id)">AI分析</button>
            <button class="outline-btn" type="button" @click="addFavorite(item)">加入收藏</button>
            <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
          </div>

          <div v-if="item.analysis" class="analysis-box">
            <p><strong>错误类型：</strong>{{ item.analysis.errorType }}</p>
            <p><strong>知识点：</strong>{{ item.analysis.knowledgePoints.join('、') }}</p>
            <p><strong>分步讲解：</strong></p>
            <ol>
              <li v-for="step in item.analysis.solvingSteps" :key="step">{{ step }}</li>
            </ol>
            <p><strong>同类变式：</strong>{{ item.analysis.variants.join('；') }}</p>
            <p><strong>追问建议：</strong>{{ item.analysis.followUpSuggestions.join('；') }}</p>
          </div>
        </li>
      </ul>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, unwrap } from '../api/client'

const form = reactive({
  chapterId: null,
  questionTitle: '',
  questionContent: '',
  imageUrl: ''
})

const loading = ref(false)
const submitting = ref(false)
const mistakes = ref([])

async function loadMistakes() {
  loading.value = true
  try {
    mistakes.value = await unwrap(api.get('/mistakes'))
  } finally {
    loading.value = false
  }
}

async function createMistake() {
  submitting.value = true
  try {
    const payload = {
      chapterId: form.chapterId || null,
      questionTitle: form.questionTitle,
      questionContent: form.questionContent,
      imageUrl: form.imageUrl || null
    }
    await unwrap(api.post('/mistakes', payload))
    form.chapterId = null
    form.questionTitle = ''
    form.questionContent = ''
    form.imageUrl = ''
    await loadMistakes()
  } finally {
    submitting.value = false
  }
}

async function analyze(id) {
  const analysis = await unwrap(api.post(`/mistakes/${id}/analyze`))
  const target = mistakes.value.find((m) => m.id === id)
  if (target) {
    target.analysis = analysis
  }
}

async function remove(id) {
  await unwrap(api.delete(`/mistakes/${id}`))
  mistakes.value = mistakes.value.filter((m) => m.id !== id)
}

async function addFavorite(item) {
  await unwrap(api.post('/favorites', {
    title: item.questionTitle,
    content: item.questionContent
  }))
}

onMounted(loadMistakes)
</script>
