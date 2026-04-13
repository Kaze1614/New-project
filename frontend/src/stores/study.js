import { defineStore } from 'pinia'
import { api, unwrap } from '../api/client'

export const useStudyStore = defineStore('study', {
  state: () => ({
    session: null,
    loading: false,
    submitting: false
  }),
  getters: {
    activeItem(state) {
      if (!state.session?.items?.length) return null
      const current = state.session.items.find((item) => item.isCurrent)
      return current ?? state.session.items[0]
    }
  },
  actions: {
    async createSession(chapterId) {
      this.loading = true
      try {
        this.session = await unwrap(api.post('/study/sessions', { chapterId }))
      } finally {
        this.loading = false
      }
      return this.session
    },
    async loadSession(sessionId) {
      this.loading = true
      try {
        this.session = await unwrap(api.get(`/study/sessions/${sessionId}`))
      } finally {
        this.loading = false
      }
      return this.session
    },
    async saveAnswer(sessionId, itemId, answer) {
      this.session = await unwrap(
        api.post(`/study/sessions/${sessionId}/answers`, { itemId, answer })
      )
      return this.session
    },
    async submitSession(sessionId) {
      this.submitting = true
      try {
        this.session = await unwrap(api.post(`/study/sessions/${sessionId}/submit`))
      } finally {
        this.submitting = false
      }
      return this.session
    }
  }
})
