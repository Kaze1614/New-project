<template>
  <section class="panel-card page-stack">
    <h3>智能搜索</h3>
    <div class="search-row">
      <input v-model.trim="keyword" placeholder="输入关键词，如：极限、导数、单调性" @keyup.enter="runSearch" />
      <button class="primary-btn" type="button" @click="runSearch">搜索</button>
    </div>

    <section class="result-box" v-if="results">
      <h4>章节命中</h4>
      <p v-if="!results.chapters.length">无</p>
      <ul v-else>
        <li v-for="item in results.chapters" :key="item.id">{{ item.title }}</li>
      </ul>

      <h4>题库命中</h4>
      <p v-if="!results.questions.length">无</p>
      <ul v-else>
        <li v-for="item in results.questions" :key="item.id">
          <strong>{{ item.title }}</strong>：{{ item.content }}
        </li>
      </ul>

      <h4>错题命中</h4>
      <p v-if="!results.mistakes.length">无</p>
      <ul v-else>
        <li v-for="item in results.mistakes" :key="item.id">
          <strong>{{ item.title }}</strong>（{{ item.status }}）
        </li>
      </ul>
    </section>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { api, unwrap } from '../api/client'

const keyword = ref('')
const results = ref(null)

async function runSearch() {
  if (!keyword.value) {
    results.value = { chapters: [], questions: [], mistakes: [] }
    return
  }
  results.value = await unwrap(api.get('/search', { params: { keyword: keyword.value } }))
}
</script>
