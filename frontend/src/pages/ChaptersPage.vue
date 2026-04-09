<template>
  <section class="panel-card">
    <h3>章节目录</h3>
    <p v-if="loading">加载中...</p>
    <ul v-else class="tree-list">
      <ChapterTreeNode v-for="node in chapters" :key="node.id" :node="node" />
    </ul>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, unwrap } from '../api/client'
import ChapterTreeNode from '../components/ChapterTreeNode.vue'

const loading = ref(false)
const chapters = ref([])

async function load() {
  loading.value = true
  try {
    chapters.value = await unwrap(api.get('/chapters/tree'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
