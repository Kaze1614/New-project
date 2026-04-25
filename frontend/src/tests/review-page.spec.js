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
  it('shows due tasks in study-style review flow and submits answers', async () => {
    api.get.mockResolvedValue({
      data: {
        code: 0,
        data: [
          {
            id: 11,
            repetition: 1,
            questionTitle: '函数导数复习',
            questionContent: '已知 f(x)=x^2，求导数。',
            type: 'SINGLE',
            options: ['A. x', 'B. 2x', 'C. x^2', 'D. 2'],
            dueDate: '2026-04-25T10:00:00'
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
              explanation: '导数为 2x。',
              nextBox: 2,
              removedFromMistakes: false
            }
          ]
        }
      }
    })

    const wrapper = mount(ReviewPage, {
      global: {
        plugins: [createPinia()]
      }
    })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/review/tasks', { params: { scope: 'due' } })
    expect(wrapper.text()).toContain('函数导数复习')
    expect(wrapper.findAll('.matrix-cell')).toHaveLength(1)

    await wrapper.findAll('.option-card')[1].trigger('click')
    await wrapper.find('.primary-btn').trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/review/submit', {
      answers: [{ taskId: 11, answer: 'B' }]
    })
    expect(wrapper.text()).toContain('正确答案：')
    expect(wrapper.text()).toContain('回答正确，进入 Box 2')
  })

  it('shows empty state when there are no due tasks', async () => {
    api.get.mockResolvedValue({
      data: {
        code: 0,
        data: []
      }
    })

    const wrapper = mount(ReviewPage, {
      global: {
        plugins: [createPinia()]
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('今日没有到期错题')
    expect(wrapper.findAll('.matrix-cell')).toHaveLength(0)
  })
})
