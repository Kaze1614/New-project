<template>
  <section class="panel-card page-stack">
    <div class="item-head">
      <h3>智能答疑</h3>
      <button class="outline-btn" type="button" @click="newSession">新建会话</button>
    </div>

    <div class="chat-grid">
      <aside class="session-list">
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-btn"
          :class="{ active: session.id === currentSessionId }"
          @click="selectSession(session.id)"
          type="button"
        >
          {{ session.title }}
        </button>
      </aside>

      <section class="chat-panel">
        <div class="message-list">
          <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
            <p>{{ msg.content }}</p>
          </div>
        </div>

        <form class="chat-form" @submit.prevent="sendMessage">
          <input v-model.trim="input" placeholder="输入你的数学问题，支持追问" maxlength="2000" />
          <button class="primary-btn" :disabled="sending" type="submit">发送</button>
        </form>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, unwrap } from '../api/client'

const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const input = ref('')
const sending = ref(false)

async function loadSessions() {
  sessions.value = await unwrap(api.get('/qa/sessions'))
  if (!sessions.value.length) {
    await newSession()
  } else if (!currentSessionId.value) {
    await selectSession(sessions.value[0].id)
  }
}

async function newSession() {
  const created = await unwrap(api.post('/qa/sessions', { title: `答疑会话 ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}` }))
  sessions.value.unshift(created)
  await selectSession(created.id)
}

async function selectSession(id) {
  currentSessionId.value = id
  messages.value = await unwrap(api.get(`/qa/sessions/${id}/messages`))
}

async function sendMessage() {
  if (!input.value || !currentSessionId.value) return
  sending.value = true
  try {
    const turn = await unwrap(api.post(`/qa/sessions/${currentSessionId.value}/messages`, { content: input.value }))
    messages.value.push(turn.userMessage, turn.assistantMessage)
    input.value = ''
  } finally {
    sending.value = false
  }
}

onMounted(loadSessions)
</script>
