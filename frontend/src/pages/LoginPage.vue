<template>
  <section class="auth-wrapper">
    <div class="auth-glass-card">
      <div class="auth-header">
        <h2>Login</h2>
      </div>

      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">快速注册</button>
      </div>

      <form @submit.prevent="submit" class="auth-form">
        <div class="input-group">
          <label>账号</label>
          <input 
            v-model.trim="form.username" 
            required 
            maxlength="32" 
            placeholder="请输入管理员或学生账号" 
            autocomplete="off"
          />
        </div>

        <div class="input-group">
          <label>密码</label>
          <input 
            v-model="form.password" 
            type="password" 
            required 
            minlength="6" 
            maxlength="64" 
            placeholder="请输入密码" 
          />
        </div>

        <button class="btn-primary" type="submit" :disabled="loading" :class="{ 'is-loading': loading }">
          {{ loading ? '验证中...' : (mode === 'login' ? '进入系统' : '注册并进入') }}
        </button>
      </form>

      <div v-if="error" class="auth-alert error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
        <span>{{ error }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, tokenStorage, unwrap } from '../api/client'

const router = useRouter()
const mode = ref('login')
const loading = ref(false)
const error = ref('')

// 数据模型保持原样，与后端模型紧密咬合
const form = reactive({
  username: '',
  password: ''
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const payload = { username: form.username, password: form.password }
    const endpoint = mode.value === 'login' ? '/auth/login' : '/auth/register'
    const result = await unwrap(api.post(endpoint, payload))
    
    tokenStorage.set(result.token)
    router.push('/dashboard') // 默认页导向首页
  } catch (err) {
    if (!err?.response) {
      error.value = '网络请求中断，请确认本地 18000 端口服务运转正常。'
    } else {
      error.value = err?.response?.data?.message || '请求失败，请检查账号密码输入'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  display: grid;
  place-items: center;
  /* 科技感深邃背景结合浅色卡片，突出视觉焦点 */
  background: radial-gradient(circle at 50% 0%, #e2e8f0 0%, #cbd5e1 100%);
  padding: 20px;
}

.auth-glass-card {
  width: 100%;
  max-width: 440px;
  background: var(--bg-panel);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h2 {
  font-family: 'Fira Code', monospace;
  font-size: 28px;
  color: var(--text-main);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.auth-header p {
  color: var(--text-muted);
  font-size: 14px;
}

.auth-tabs {
  display: flex;
  background: var(--bg-app);
  padding: 4px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
}

.auth-tabs button {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  border-radius: var(--radius-sm);
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.auth-tabs button.active {
  background: var(--bg-panel);
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}

.auth-form {
  display: grid;
  gap: 20px;
}

.input-group {
  display: grid;
  gap: 8px;
}

.input-group label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.input-group input {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #f8fafc;
  transition: all var(--transition-fast);
  font-size: 15px;
}

.input-group input:focus {
  background: var(--bg-panel);
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.btn-primary {
  margin-top: 8px;
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 14px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  justify-content: center;
  align-items: center;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-alert {
  margin-top: 20px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 500;
  animation: slideIn 0.3s ease;
}

.auth-alert.error {
  background: #fef2f2;
  color: var(--danger);
  border: 1px solid #fecaca;
}

.auth-alert .icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>