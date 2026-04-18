import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    aiDrawerOpen: false,
    aiSessionId: null,
    aiPendingContext: ''
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
    }
  }
})
