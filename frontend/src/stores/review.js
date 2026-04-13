import { defineStore } from 'pinia'
import { api, unwrap } from '../api/client'

export const useReviewStore = defineStore('review', {
  state: () => ({
    currentTask: null,
    phase: 'blind',
    loading: false,
    rating: false
  }),
  actions: {
    async loadNext() {
      this.loading = true
      try {
        this.currentTask = await unwrap(api.get('/review/next'))
        this.phase = 'blind'
      } finally {
        this.loading = false
      }
      return this.currentTask
    },
    reveal() {
      this.phase = 'revealed'
    },
    async rate(grade) {
      if (!this.currentTask) return null
      this.rating = true
      try {
        await unwrap(api.post(`/review/tasks/${this.currentTask.id}/rate`, { grade }))
      } finally {
        this.rating = false
      }
      return this.loadNext()
    }
  }
})
