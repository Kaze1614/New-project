import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { chapterId: '4' } })
}))

let sessionState = null

function makeSession({ submitted = false, answered = false } = {}) {
  return {
    id: 1,
    chapterId: 4,
    durationSeconds: 1800,
    startedAt: new Date().toISOString(),
    submittedAt: submitted ? new Date().toISOString() : null,
    submitted,
    totalCount: 20,
    answeredCount: answered ? 1 : 0,
    correctCount: 0,
    items: Array.from({ length: 20 }).map((_, index) => ({
      itemId: index + 1,
      questionId: index + 1,
      sortOrder: index + 1,
      type: index % 2 === 0 ? 'SINGLE' : 'FILL',
      difficulty: 'MEDIUM',
      title: `题目${index + 1}`,
      content: `内容${index + 1}`,
      options: index % 2 === 0 ? ['A. 1', 'B. 2', 'C. 3', 'D. 4'] : null,
      userAnswer: answered && index === 0 ? 'A' : '',
      answered: answered && index === 0,
      correct: submitted && index === 0 ? false : null,
      officialAnswer: submitted ? 'B' : null,
      officialExplanation: submitted ? '官方解析' : null
    }))
  }
}

vi.mock('../api/client', () => {
  const api = {
    post: vi.fn((url) => {
      if (url === '/study/sessions') {
        sessionState = makeSession()
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/answers') {
        sessionState = makeSession({ answered: true })
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/submit') {
        sessionState = makeSession({ answered: true, submitted: true })
        return Promise.resolve(sessionState)
      }
      return Promise.resolve({})
    })
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import StudyPage from '../pages/StudyPage.vue'
import { api } from '../api/client'

describe('StudyPage', () => {
  it('renders 7:3 layout with 20-cell matrix and submit flow', async () => {
    const wrapper = mount(StudyPage, {
      global: {
        plugins: [createPinia()]
      }
    })
    await flushPromises()

    expect(wrapper.find('.study-layout').exists()).toBe(true)
    expect(wrapper.findAll('.matrix-cell')).toHaveLength(20)

    await wrapper.find('.option-card').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/study/sessions/1/answers', {
      itemId: 1,
      answer: 'A'
    })

    await wrapper.findAll('.primary-btn')[0].trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('官方解析')
  })
})
