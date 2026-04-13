import { defineStore } from 'pinia'
import { api, tokenStorage, unwrap } from '../api/client'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: tokenStorage.get(),
    user: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setSession(payload) {
      this.token = payload.token
      this.user = payload.user ?? null
      tokenStorage.set(payload.token)
    },
    async login(username, password) {
      const data = await unwrap(api.post('/auth/login', { username, password }))
      this.setSession(data)
      return data
    },
    async register(username, password) {
      const data = await unwrap(api.post('/auth/register', { username, password }))
      this.setSession(data)
      return data
    },
    logout() {
      this.token = null
      this.user = null
      tokenStorage.clear()
    }
  }
})
