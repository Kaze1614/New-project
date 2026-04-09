import axios from 'axios'

const TOKEN_KEY = 'math_ai_token'

export const tokenStorage = {
  get() {
    return localStorage.getItem(TOKEN_KEY)
  },
  set(token) {
    localStorage.setItem(TOKEN_KEY, token)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function unwrap(promise) {
  const { data } = await promise
  return data.data
}
