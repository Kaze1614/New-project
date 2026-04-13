<template>
  <aside class="sidebar" @mouseleave="uiStore.setChapterFlyoutOpen(false)">
    <h1 class="brand">数学园</h1>

    <nav class="nav-list">
      <RouterLink v-for="item in normalItems" :key="item.path" :to="item.path" class="nav-item">
        <span class="nav-dot" aria-hidden="true"></span>
        <span>{{ item.label }}</span>
      </RouterLink>

      <button
        type="button"
        class="nav-item nav-chapter"
        @mouseenter="openChapterFlyout"
        @focus="openChapterFlyout"
      >
        <span class="nav-dot" aria-hidden="true"></span>
        <span>章节目录</span>
      </button>
    </nav>

    <div
      v-if="uiStore.chapterFlyoutOpen"
      class="chapter-flyout"
      @mouseenter="uiStore.setChapterFlyoutOpen(true)"
      @click.stop
    >
      <div v-if="loading" class="flyout-empty">加载中...</div>
      <div v-else-if="!uiStore.chapterTree.length" class="flyout-empty">暂无章节数据</div>
      <ul v-else class="flyout-level">
        <li v-for="group in uiStore.chapterTree" :key="group.id">
          <button type="button" class="flyout-parent">{{ group.title }}</button>
          <ul class="flyout-level child">
            <li v-for="node in group.children || []" :key="node.id">
              <button type="button" class="flyout-child" @click="jumpToStudy(node.id)">
                {{ node.title }}
              </button>
              <ul v-if="node.children?.length" class="flyout-level leaf">
                <li v-for="leaf in node.children" :key="leaf.id">
                  <button type="button" class="flyout-leaf" @click="jumpToStudy(leaf.id)">
                    {{ leaf.title }}
                  </button>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </aside>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, unwrap } from '../api/client'
import { useUiStore } from '../stores/ui'

const router = useRouter()
const uiStore = useUiStore()
const loading = ref(false)

const normalItems = [
  { path: '/dashboard', label: '首页' },
  { path: '/mistakes', label: '错题本' },
  { path: '/favorites', label: '收藏本' },
  { path: '/review', label: '待复习' }
]

async function loadChapterTree() {
  loading.value = true
  try {
    const tree = await unwrap(api.get('/chapters/tree'))
    uiStore.setChapterTree(tree)
  } catch (error) {
    uiStore.setChapterTree([])
  } finally {
    loading.value = false
  }
}

function openChapterFlyout() {
  uiStore.setChapterFlyoutOpen(true)
}

function jumpToStudy(chapterId) {
  uiStore.setChapterFlyoutOpen(false)
  router.push({ path: '/study', query: { chapterId } })
}

onMounted(loadChapterTree)
</script>
