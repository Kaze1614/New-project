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
    mistakeFilters: { chapterId: '', difficulty: '', keyword: '' },
    favoriteFilters: { chapterId: '', difficulty: '', keyword: '' }
  }),
  actions: {
    async loadMistakes() {
      this.loadingMistakes = true
      try {
        this.mistakes = await unwrap(
          api.get('/mistakes', { params: cleanParams(this.mistakeFilters) })
        )
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
    }
  }
})
