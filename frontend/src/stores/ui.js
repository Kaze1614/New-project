import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    aiDrawerOpen: false,
    aiSessionId: null,
    aiPendingContext: '',
    chapterTree: [],
    chapterFlyoutOpen: false
  }),
  actions: {
    openAIDrawer(context = '') {
      this.aiDrawerOpen = true
      if (context) {
        this.aiPendingContext = context
      }
    },
    closeAIDrawer() {
      this.aiDrawerOpen = false
    },
    setAiSession(id) {
      this.aiSessionId = id
    },
    consumePendingContext() {
      const context = this.aiPendingContext
      this.aiPendingContext = ''
      return context
    },
    setChapterTree(nodes) {
      this.chapterTree = Array.isArray(nodes) ? nodes : []
    },
    setChapterFlyoutOpen(open) {
      this.chapterFlyoutOpen = open
    }
  }
})
