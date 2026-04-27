import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(),
    post: vi.fn()
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => {
      const response = await promise
      return response.data.data
    })
  }
})

import ReviewPage from '../pages/ReviewPage.vue'
import { api } from '../api/client'

describe('ReviewPage', () => {
  it('renders due review flow, strips question number prefix and falls back to 无', async () => {
    api.get.mockResolvedValue({
      data: {
        code: 0,
        data: [
          {
            id: 11,
            repetition: 1,
            dueDate: '2026-04-25T10:00:00',
            sourceLabel: '1.(2025)(National I)',
            questionTitle: 'Derivative review',
            questionContent: '1.(2025)(National I) Let f(x)=x^2, find the derivative.',
            type: 'SINGLE',
            options: ['A. x', 'B. 2x', 'C. x^2', 'D. 2']
          }
        ]
      }
    })

    api.post.mockResolvedValue({
      data: {
        code: 0,
        data: {
          submittedAt: '2026-04-25T10:00:00',
          totalCount: 1,
          answeredCount: 1,
          correctCount: 1,
          items: [
            {
              taskId: 11,
              answered: true,
              correct: true,
              correctAnswer: 'B',
              explanation: null,
              nextBox: 2,
              removedFromMistakes: false
            }
          ]
        }
      }
    })

    const wrapper = mount(ReviewPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/review/tasks', { params: { scope: 'due' } })
    expect(wrapper.text()).toContain('(2025)(National I)')
    expect(wrapper.text()).not.toContain('1.(2025)(National I)')
    expect(wrapper.text()).toContain('Let f(x)=x^2, find the derivative.')
    expect(wrapper.text()).not.toContain('1.(2025)(National I) Let f(x)=x^2, find the derivative.')

    await wrapper.findAll('.option-card')[1].trigger('click')
    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/review/submit', {
      answers: [{ taskId: 11, answer: 'B' }]
    })
    expect(wrapper.text()).toContain('正确答案')
    expect(wrapper.text()).toContain('无')
  })

  it('shows plain empty state when no due tasks exist', async () => {
    api.get.mockResolvedValue({ data: { code: 0, data: [] } })

    const wrapper = mount(ReviewPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    expect(wrapper.findAll('.matrix-cell')).toHaveLength(0)
  })
})
