import { defineStore } from 'pinia'
import { api, unwrap } from '../api/client'

function cloneTask(task) {
  return {
    ...task,
    options: Array.isArray(task.options) ? task.options : [],
    userAnswer: task.userAnswer || '',
    answered: Boolean(task.userAnswer),
    result: task.result || null
  }
}

export const useReviewStore = defineStore('review', {
  state: () => ({
    tasks: [],
    loading: false,
    submitting: false,
    submittedAt: null,
    summary: null
  }),
  getters: {
    hasTasks(state) {
      return state.tasks.length > 0
    },
    submitted(state) {
      return Boolean(state.submittedAt)
    }
  },
  actions: {
    async loadDueTasks() {
      this.loading = true
      try {
        const tasks = await unwrap(api.get('/review/tasks', { params: { scope: 'due' } }))
        this.tasks = Array.isArray(tasks) ? tasks.map(cloneTask) : []
        this.submittedAt = null
        this.summary = null
      } finally {
        this.loading = false
      }
      return this.tasks
    },
    saveAnswer(taskId, answer) {
      const target = this.tasks.find((task) => task.id === taskId)
      if (!target) return null
      target.userAnswer = answer
      target.answered = Boolean(answer && answer.trim())
      return target
    },
    async submitBatch() {
      this.submitting = true
      try {
        const payload = {
          answers: this.tasks.map((task) => ({
            taskId: task.id,
            answer: task.userAnswer || ''
          }))
        }
        const response = await unwrap(api.post('/review/submit', payload))
        const resultMap = new Map((response.items || []).map((item) => [item.taskId, item]))
        this.tasks = this.tasks.map((task) => ({
          ...task,
          result: resultMap.get(task.id) || null
        }))
        this.submittedAt = response.submittedAt || new Date().toISOString()
        this.summary = {
          totalCount: response.totalCount || this.tasks.length,
          answeredCount: response.answeredCount || 0,
          correctCount: response.correctCount || 0
        }
      } finally {
        this.submitting = false
      }
      return this.tasks
    }
  }
})
