<template>
  <section class="user-management-page">
    <div v-if="successMessage" class="admin-alert success">{{ successMessage }}</div>

    <section class="admin-section question-list-toolbar" aria-label="用户管理筛选">
      <div>
        <p class="section-eyebrow">Users</p>
        <h2>用户管理</h2>
        <p class="muted">查看注册用户、角色状态，并执行密码重置与删除操作。</p>
      </div>
      <form class="question-search user-search" @submit.prevent="loadUsers(1)">
        <label>
          <span class="sr-only">搜索用户</span>
          <input v-model.trim="keyword" type="search" placeholder="搜索用户名或角色" />
        </label>
        <button class="outline-btn" type="submit">搜索</button>
        <button class="outline-btn" type="button" @click="refresh">刷新</button>
      </form>
    </section>

    <section class="admin-section question-table-card" aria-label="用户列表">
      <div class="table-meta">
        <span>共 {{ pagination.total }} 个用户</span>
        <span>第 {{ pagination.page }} 页</span>
      </div>

      <div v-if="loading" class="table-empty">正在加载用户...</div>
      <div v-else-if="error" class="table-empty error-text">{{ error }}</div>
      <div v-else-if="!users.length" class="table-empty">暂无用户数据。</div>

      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>注册日期</th>
              <th>角色</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td class="mono">{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ formatTime(user.createdAt) }}</td>
              <td>
                <span class="role-badge" :class="user.role?.toLowerCase()">{{ formatRole(user.role) }}</span>
              </td>
              <td>
                <div class="table-actions">
                  <button class="outline-btn" type="button" @click="resetPassword(user)">修改</button>
                  <button
                    class="danger-btn"
                    type="button"
                    :disabled="isProtectedUser(user)"
                    :title="isProtectedUser(user) ? 'admin 账号不可删除' : ''"
                    @click="deleteUser(user)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-row">
        <button class="outline-btn" type="button" :disabled="pagination.page <= 1 || loading" @click="loadUsers(pagination.page - 1)">上一页</button>
        <button class="outline-btn" type="button" :disabled="!hasNextPage || loading" @click="loadUsers(pagination.page + 1)">下一页</button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, unwrap } from '../api/client'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const users = ref([])
const loading = ref(false)
const error = ref('')
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const successMessage = computed(() => {
  if (route.query.reset === '1') return '密码已重置为 123456。'
  if (route.query.deleted === '1') return '用户已删除。'
  return ''
})

const hasNextPage = computed(() => pagination.page * pagination.size < pagination.total)

onMounted(() => loadUsers(1))

async function loadUsers(page = pagination.page) {
  loading.value = true
  error.value = ''
  try {
    const data = await unwrap(api.get('/admin/users', {
      params: {
        keyword: keyword.value || undefined,
        page,
        size: pagination.size
      }
    }))
    users.value = data.items || []
    pagination.page = data.page || page
    pagination.size = data.size || pagination.size
    pagination.total = data.total || 0
  } catch (err) {
    error.value = err?.response?.data?.message || '用户列表加载失败'
    users.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function refresh() {
  if (route.query.reset || route.query.deleted) {
    router.replace({ path: '/admin/users' })
  }
  loadUsers(pagination.page)
}

async function resetPassword(user) {
  const confirmed = window.confirm(`确认将用户 ${user.username} 的密码重置为 123456 吗？`)
  if (!confirmed) return
  loading.value = true
  error.value = ''
  try {
    await unwrap(api.post(`/admin/users/${user.id}/reset-password`))
    await router.replace({ path: '/admin/users', query: { reset: '1' } })
    await loadUsers(pagination.page)
  } catch (err) {
    error.value = err?.response?.data?.message || '密码重置失败'
  } finally {
    loading.value = false
  }
}

async function deleteUser(user) {
  if (isProtectedUser(user)) return
  const confirmed = window.confirm(`确认删除用户 ${user.username} 吗？该操作不可恢复。`)
  if (!confirmed) return
  loading.value = true
  error.value = ''
  try {
    await unwrap(api.delete(`/admin/users/${user.id}`))
    await router.replace({ path: '/admin/users', query: { deleted: '1' } })
    await loadUsers(1)
  } catch (err) {
    error.value = err?.response?.data?.message || '删除失败'
  } finally {
    loading.value = false
  }
}

function formatRole(role) {
  if (String(role).toUpperCase() === 'ADMIN') return '管理员'
  return '学生'
}

function isProtectedUser(user) {
  return String(user.username).toLowerCase() === 'admin'
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return date.toLocaleString('zh-CN', { hour12: false })
}
</script>
