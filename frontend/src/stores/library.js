import { defineStore } from 'pinia'
import { api, unwrap } from '../api/client'

function cleanParams(filters) {
  const result = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    result[key] = value
  })
  return result
}

export const useLibraryStore = defineStore('library', {
  state: () => ({
    mistakes: [],
    favorites: [],
    loadingMistakes: false,
    loadingFavorites: false,
    mistakeFilters: { keyword: '', chapterId: null },
    favoriteFilters: { keyword: '' },
    selectedMistakeId: null,
    selectedMistakeAnswer: '',
    revealedMistakeId: null,
    selectedChapterId: null
  }),
  getters: {
    activeMistake(state) {
      if (!state.mistakes.length) return null
      return state.mistakes.find((item) => item.id === state.selectedMistakeId) ?? state.mistakes[0]
    },
    canCheckActiveMistake() {
      return this.activeMistake?.questionType === 'SINGLE' && Array.isArray(this.activeMistake?.options) && this.activeMistake.options.length > 0
    },
    isAnswerRevealed() {
      return this.activeMistake && this.revealedMistakeId === this.activeMistake.id
    }
  },
  actions: {
    async loadMistakes() {
      this.loadingMistakes = true
      try {
        this.mistakes = await unwrap(
          api.get('/mistakes', { params: cleanParams(this.mistakeFilters) })
        )
        if (!this.mistakes.length) {
          this.resetMistakeUi()
          return this.mistakes
        }
        const stillExists = this.mistakes.some((item) => item.id === this.selectedMistakeId)
        if (!stillExists) {
          this.selectMistake(this.mistakes[0].id)
        }
      } finally {
        this.loadingMistakes = false
      }
      return this.mistakes
    },
    async loadFavorites() {
      this.loadingFavorites = true
      try {
        this.favorites = await unwrap(
          api.get('/favorites', { params: cleanParams(this.favoriteFilters) })
        )
      } finally {
        this.loadingFavorites = false
      }
      return this.favorites
    },
    selectMistake(id) {
      this.selectedMistakeId = id
      this.selectedMistakeAnswer = ''
      this.revealedMistakeId = null
    },
    setMistakeAnswer(answer) {
      this.selectedMistakeAnswer = answer
    },
    revealActiveMistake() {
      if (!this.activeMistake) return
      this.revealedMistakeId = this.activeMistake.id
    },
    setSelectedChapter(id) {
      this.selectedChapterId = id
      this.mistakeFilters = { ...this.mistakeFilters, chapterId: id }
    },
    resetMistakeUi() {
      this.selectedMistakeId = null
      this.selectedMistakeAnswer = ''
      this.revealedMistakeId = null
    }
  }
})
