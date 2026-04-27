import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { chapterId: '4' } })
}))

let sessionState = null

function makeSession({
  submitted = false,
  answered = false,
  type = 'SINGLE',
  officialExplanation = submitted ? 'Explain' : null
} = {}) {
  const firstItem = {
    itemId: 1,
    questionId: 1,
    sortOrder: 1,
    type,
    difficulty: 'MEDIUM',
    title: 'Question 1',
    content: '1.(2025)(National I) Content 1',
    options: type === 'SOLUTION' ? null : ['A. 1', 'B. 2', 'C. 3', 'D. 4'],
    userAnswer: answered ? (type === 'SOLUTION' ? 'step 1: let x=2' : 'A') : '',
    studentAnswerText: answered && type === 'SOLUTION' ? 'step 1: let x=2' : null,
    answered,
    correct: submitted && type !== 'SOLUTION' ? false : null,
    officialAnswer: submitted ? (type === 'SOLUTION' ? '(1) 4' : 'B') : null,
    officialExplanation,
    explanationSource: submitted ? 'TEACHER_GENERATED' : null,
    explanationReviewStatus: submitted ? 'PENDING_REVIEW' : null,
    sourceLabel: '1.(2025)(National I)',
    sourceSnapshotPath: null,
    subQuestions: type === 'SOLUTION' ? [{ index: 1, prompt: 'Find f(2)', referenceAnswer: '4', steps: [] }] : []
  }

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
    items: [
      firstItem,
      ...Array.from({ length: 19 }).map((_, index) => ({
        itemId: index + 2,
        questionId: index + 2,
        sortOrder: index + 2,
        type: 'FILL',
        difficulty: 'MEDIUM',
        title: `Question ${index + 2}`,
        content: `${index + 2}.(2025)(National I) Content ${index + 2}`,
        options: null,
        userAnswer: '',
        studentAnswerText: null,
        answered: false,
        correct: null,
        officialAnswer: submitted ? 'Answer' : null,
        officialExplanation,
        explanationSource: submitted ? 'TEACHER_GENERATED' : null,
        explanationReviewStatus: submitted ? 'PENDING_REVIEW' : null,
        sourceLabel: `${index + 2}.(2025)(National I)`,
        sourceSnapshotPath: null,
        subQuestions: []
      }))
    ]
  }
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(),
    post: vi.fn((url, payload) => {
      if (url === '/study/sessions') {
        sessionState = makeSession()
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/answers') {
        sessionState = makeSession({
          answered: true,
          type: payload?.answer?.includes('step 1') ? 'SOLUTION' : 'SINGLE'
        })
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/submit') {
        const type = sessionState?.items?.[0]?.type || 'SINGLE'
        sessionState = makeSession({ answered: true, submitted: true, type })
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
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-04-25T10:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders layout, starts timer, hides source question number and submits single-choice answers', async () => {
    const wrapper = mount(StudyPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    expect(wrapper.find('.study-layout').exists()).toBe(true)
    expect(wrapper.findAll('.matrix-cell')).toHaveLength(20)
    expect(wrapper.text()).toContain('(2025)(National I)')
    expect(wrapper.text()).not.toContain('1.(2025)(National I)')
    expect(wrapper.text()).toContain('Content 1')
    expect(wrapper.text()).not.toContain('1.(2025)(National I) Content 1')
    expect(wrapper.find('.timer-value').text()).toBe('00:00:00')

    await vi.advanceTimersByTimeAsync(5000)
    expect(wrapper.find('.timer-value').text()).toBe('00:00:05')

    await wrapper.find('.option-card').trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/study/sessions/1/answers', {
      itemId: 1,
      answer: 'A'
    })

    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('正确答案')
    expect(wrapper.text()).toContain('解析')
  })

  it('renders solution answer editor and falls back to 无 when explanation is empty', async () => {
    api.post.mockImplementation((url) => {
      if (url === '/study/sessions') {
        sessionState = makeSession({ type: 'SOLUTION' })
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/submit') {
        sessionState = makeSession({
          type: 'SOLUTION',
          answered: true,
          submitted: true,
          officialExplanation: null
        })
        return Promise.resolve(sessionState)
      }
      return Promise.resolve(sessionState)
    })

    const wrapper = mount(StudyPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    expect(wrapper.find('textarea').exists()).toBe(true)
    await wrapper.find('textarea').setValue('step 1: let x=2')
    await wrapper.find('.solution-answer .outline-btn').trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/study/sessions/1/answers', {
      itemId: 1,
      answer: 'step 1: let x=2'
    })

    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('参考答案：4')
    expect(wrapper.text()).not.toContain('分点解析')
    expect(wrapper.text()).toContain('无')
  })

  it('shows submit error text when submit request fails', async () => {
    api.post.mockImplementation((url) => {
      if (url === '/study/sessions') {
        sessionState = makeSession({ answered: true })
        return Promise.resolve(sessionState)
      }
      if (url === '/study/sessions/1/submit') {
        return Promise.reject({ response: { data: { message: 'submit failed' } } })
      }
      return Promise.resolve(sessionState)
    })

    const wrapper = mount(StudyPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('submit failed')
  })
})
