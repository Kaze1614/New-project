<template>
  <section class="auth-wrapper">
    <div class="auth-card-v2">
      <h2>数学园</h2>
      <p class="muted">默认管理员账号：admin / 123456</p>

      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" type="button" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" type="button" @click="mode = 'register'">注册</button>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <label>
          账号
          <input
            v-model.trim="form.username"
            type="text"
            minlength="3"
            maxlength="32"
            autocomplete="off"
            required
            placeholder="请输入账号"
          />
        </label>
        <label>
          密码
          <input
            v-model="form.password"
            type="password"
            minlength="6"
            maxlength="64"
            required
            placeholder="请输入密码"
          />
        </label>
        <button class="primary-btn" type="submit" :disabled="loading">
          {{ loading ? '提交中...' : mode === 'login' ? '进入系统' : '注册并进入' }}
        </button>
      </form>

      <p v-if="error" class="error-text">{{ error }}</p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: ''
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'login') {
      await authStore.login(form.username, form.password)
    } else {
      await authStore.register(form.username, form.password)
    }
    router.push('/dashboard')
  } catch (err) {
    error.value = err?.response?.data?.message || '请求失败，请检查输入'
  } finally {
    loading.value = false
  }
}
</script>
