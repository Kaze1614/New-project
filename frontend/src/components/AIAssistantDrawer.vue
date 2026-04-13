<template>
  <button class="ai-fab" type="button" @click="openDrawer">
    <span>AI</span>
  </button>

  <aside class="ai-drawer" :class="{ open: uiStore.aiDrawerOpen }" aria-label="AI 助理对话抽屉">
    <header class="ai-drawer-head">
      <h3>AI 辅导员</h3>
      <button class="icon-btn" type="button" aria-label="关闭" @click="uiStore.closeAIDrawer()">×</button>
    </header>

    <div v-if="!authStore.isLoggedIn" class="ai-empty">
      <p>登录后可使用 AI 答疑</p>
      <RouterLink class="outline-btn" to="/login">去登录</RouterLink>
    </div>

    <template v-else>
      <p v-if="pendingContext" class="context-tip">已携带当前题目上下文，发送消息时将自动注入。</p>

      <div class="ai-messages">
        <p v-if="loadingMessages" class="muted">加载会话中...</p>
        <p v-else-if="!messages.length" class="muted">输入问题后开始答疑。</p>
        <article
          v-for="message in messages"
          :key="message.id"
          class="ai-message"
          :class="message.role"
        >
          {{ message.content }}
        </article>
      </div>

      <form class="ai-form" @submit.prevent="sendMessage">
        <input
          v-model.trim="draft"
          type="text"
          maxlength="2000"
          placeholder="输入数学问题，按 Enter 发送"
        />
        <button class="primary-btn" type="submit" :disabled="sending || !draft">
          {{ sending ? '发送中...' : '发送' }}
        </button>
      </form>
    </template>
  </aside>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api, unwrap } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()

const loadingMessages = ref(false)
const sending = ref(false)
const messages = ref([])
const draft = ref('')
const pendingContext = ref('')

const isOpen = computed(() => uiStore.aiDrawerOpen)

watch(isOpen, async (open) => {
  if (!open || !authStore.isLoggedIn) return
  pendingContext.value = uiStore.consumePendingContext()
  await ensureSession()
  await loadMessages()
})

watch(
  () => uiStore.aiPendingContext,
  (value) => {
    if (uiStore.aiDrawerOpen && value) {
      pendingContext.value = value
    }
  }
)

async function openDrawer() {
  uiStore.openAIDrawer()
}

async function ensureSession() {
  if (uiStore.aiSessionId) return uiStore.aiSessionId
  const session = await unwrap(api.post('/qa/sessions', { title: '全局答疑' }))
  uiStore.setAiSession(session.id)
  return session.id
}

async function loadMessages() {
  if (!uiStore.aiSessionId) return
  loadingMessages.value = true
  try {
    messages.value = await unwrap(api.get(`/qa/sessions/${uiStore.aiSessionId}/messages`))
  } finally {
    loadingMessages.value = false
  }
}

async function sendMessage() {
  if (!draft.value) return
  const context = pendingContext.value
  sending.value = true
  try {
    const conversation = await unwrap(
      api.post(`/qa/sessions/${uiStore.aiSessionId}/messages`, {
        content: draft.value,
        context: context || null
      })
    )
    messages.value.push(conversation.userMessage, conversation.assistantMessage)
    draft.value = ''
    pendingContext.value = ''
  } finally {
    sending.value = false
  }
}
</script>
