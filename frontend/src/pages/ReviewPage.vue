<template>
  <section class="panel-card">
    <h3>待复习</h3>
    <p v-if="loading">加载中...</p>
    <p v-else-if="!tasks.length">当前无待复习任务。</p>
    <ul v-else class="item-list">
      <li v-for="task in tasks" :key="task.id" class="item-card">
        <div class="item-head">
          <h4>{{ task.questionTitle }}</h4>
          <span class="status-chip" :class="task.completed ? 'ok' : 'todo'">{{ task.completed ? '已完成' : '未完成' }}</span>
        </div>
        <p>{{ task.questionContent }}</p>
        <p>截止时间：{{ formatDate(task.dueDate) }}</p>
        <button v-if="!task.completed" class="primary-btn" type="button" @click="complete(task.id)">标记完成</button>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, unwrap } from '../api/client'

const loading = ref(false)
const tasks = ref([])

function formatDate(text) {
  if (!text) return '--'
  return new Date(text).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    tasks.value = await unwrap(api.get('/review/tasks'))
  } finally {
    loading.value = false
  }
}

async function complete(id) {
  const updated = await unwrap(api.post(`/review/tasks/${id}/complete`))
  tasks.value = tasks.value.map((task) => (task.id === id ? updated : task))
}

onMounted(load)
</script>
