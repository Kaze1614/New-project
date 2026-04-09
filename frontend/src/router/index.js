import { createRouter, createWebHistory } from 'vue-router'
import { tokenStorage } from '../api/client'

import MainLayout from '../layout/MainLayout.vue'
import LoginPage from '../pages/LoginPage.vue'
import DashboardPage from '../pages/DashboardPage.vue'
import MistakesPage from '../pages/MistakesPage.vue'
import FavoritesPage from '../pages/FavoritesPage.vue'
import ReviewPage from '../pages/ReviewPage.vue'
import SearchPage from '../pages/SearchPage.vue'
import ChaptersPage from '../pages/ChaptersPage.vue'
import QaPage from '../pages/QaPage.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginPage },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: DashboardPage },
      { path: 'mistakes', name: 'mistakes', component: MistakesPage },
      { path: 'favorites', name: 'favorites', component: FavoritesPage },
      { path: 'review', name: 'review', component: ReviewPage },
      { path: 'search', name: 'search', component: SearchPage },
      { path: 'chapters', name: 'chapters', component: ChaptersPage },
      { path: 'qa', name: 'qa', component: QaPage }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = tokenStorage.get()
  if (!token && to.path !== '/login' && to.path !== '/dashboard') {
    return '/dashboard'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  return true
})

export default router
