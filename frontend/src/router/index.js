import { createRouter, createWebHistory } from 'vue-router'
import { tokenStorage } from '../api/client'

import MainLayout from '../layout/MainLayout.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import LoginPage from '../pages/LoginPage.vue'
import DashboardPage from '../pages/DashboardPage.vue'
import StudyPage from '../pages/StudyPage.vue'
import MistakesPage from '../pages/MistakesPage.vue'
import FavoritesPage from '../pages/FavoritesPage.vue'
import ReviewPage from '../pages/ReviewPage.vue'
import ChaptersPage from '../pages/ChaptersPage.vue'
import QuestionListPage from '../pages/QuestionListPage.vue'
import QuestionEditorPage from '../pages/QuestionEditorPage.vue'
import UserManagementPage from '../pages/UserManagementPage.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginPage },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: DashboardPage },
      { path: 'study', name: 'study', component: StudyPage },
      { path: 'mistakes', name: 'mistakes', component: MistakesPage },
      { path: 'favorites', name: 'favorites', component: FavoritesPage },
      { path: 'review', name: 'review', component: ReviewPage },
      { path: 'chapters', name: 'chapters', component: ChaptersPage }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/admin/questions' },
      { path: 'questions', name: 'admin-question-list', component: QuestionListPage, meta: { adminTitle: '题目管理', adminCrumb: '题目管理' } },
      { path: 'questions/new', name: 'admin-question-new', component: QuestionEditorPage, meta: { adminTitle: '新增题目', adminCrumb: '题目管理 > 新增题目' } },
      { path: 'questions/:id/edit', name: 'admin-question-edit', component: QuestionEditorPage, meta: { adminTitle: '编辑题目', adminCrumb: '题目管理 > 编辑题目' } },
      { path: 'users', name: 'admin-user-list', component: UserManagementPage, meta: { adminTitle: '用户管理', adminCrumb: '用户管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = tokenStorage.get()
  const publicPaths = new Set(['/login', '/dashboard'])
  if (!token && !publicPaths.has(to.path)) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  return true
})

export default router
