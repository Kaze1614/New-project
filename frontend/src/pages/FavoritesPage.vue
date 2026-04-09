<template>
  <section class="panel-card">
    <h3>收藏本</h3>
    <p v-if="loading">加载中...</p>
    <p v-else-if="!items.length">暂无收藏。</p>
    <ul v-else class="item-list">
      <li v-for="item in items" :key="item.id" class="item-card">
        <div class="item-head">
          <h4>{{ item.title }}</h4>
          <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
        </div>
        <p>{{ item.content }}</p>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, unwrap } from '../api/client'

const loading = ref(false)
const items = ref([])

async function load() {
  loading.value = true
  try {
    items.value = await unwrap(api.get('/favorites'))
  } finally {
    loading.value = false
  }
}

async function remove(id) {
  await unwrap(api.delete(`/favorites/${id}`))
  items.value = items.value.filter((it) => it.id !== id)
}

onMounted(load)
</script>
