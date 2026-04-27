import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn(), post: vi.fn(), delete: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => {
      const response = await promise
      return response.data.data
    })
  }
})

import MistakesPage from '../pages/MistakesPage.vue'
import { api } from '../api/client'

const chapterTree = [
  {
    id: 1,
    title: 'Calculus I',
    children: [
      {
        id: 2,
        title: 'Functions',
        children: [{ id: 4, title: 'Domain', children: [] }]
      }
    ]
  }
]

describe('MistakesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('hides subtitle, defaults chapter tree to collapsed, and supports single-choice wrong feedback', async () => {
    const requestParams = []
    api.get.mockImplementation((url, config) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({ data: { code: 0, data: chapterTree } })
      }
      requestParams.push(config?.params ?? {})
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 1,
              questionId: 100,
              chapterId: 4,
              difficulty: 'MEDIUM',
              questionTitle: 'Domain card',
              questionContent: 'Domain of 1/(x-2) is?',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: 'SINGLE',
              options: ['A. x>2', 'B. x<2', 'C. x!=2', 'D. x=2'],
              correctAnswer: 'C',
              explanation: null,
              sourceLabel: '2016 Mock Q9',
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            }
          ]
        }
      })
    })
    api.post.mockResolvedValue({ data: { code: 0, data: {} } })

    const wrapper = mount(MistakesPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    expect(wrapper.find('.library-head p').exists()).toBe(false)
    expect(wrapper.find('.section-link').exists()).toBe(false)

    await wrapper.find('.book-toggle').trigger('click')
    await wrapper.find('.chapter-toggle').trigger('click')
    await flushPromises()
    await wrapper.find('.section-link').trigger('click')
    await flushPromises()

    expect(requestParams).toContainEqual({})
    expect(requestParams).toContainEqual({ chapterId: 4 })
    expect(wrapper.text()).toContain('2016 Mock Q9')

    const options = wrapper.findAll('.mistake-option')
    await options[1].trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(options[1].classes()).toContain('wrong')
    expect(options[2].classes()).toContain('correct')
    expect(wrapper.text()).toContain('回答错误。')
    expect(wrapper.text()).toContain('正确答案：')
    expect(wrapper.text()).toContain('解析：')
    expect(wrapper.text()).toContain('无')

    await wrapper.find('.favorite-btn').trigger('click')
    expect(api.post).toHaveBeenCalledWith('/favorites', {
      questionId: 100,
      chapterId: 4,
      title: 'Domain card',
      content: 'Domain of 1/(x-2) is?'
    })
  })

  it('supports multi-choice answer format compatible with study page', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({ data: { code: 0, data: chapterTree } })
      }
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 2,
              questionId: 101,
              chapterId: 4,
              difficulty: 'MEDIUM',
              questionTitle: 'Set card',
              questionContent: 'Which options belong to set A?',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: 'MULTI',
              options: ['A. 1', 'B. 2', 'C. 3', 'D. 4'],
              correctAnswer: 'A,C',
              explanation: 'Set A contains 1 and 3.',
              sourceLabel: 'Textbook Example',
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            }
          ]
        }
      })
    })

    const wrapper = mount(MistakesPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    const options = wrapper.findAll('.mistake-option')
    await options[0].trigger('click')
    await options[2].trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(options[0].classes()).toContain('correct')
    expect(options[2].classes()).toContain('correct')
    expect(wrapper.text()).toContain('回答正确。')
    expect(wrapper.text()).toContain('正确答案：')
    expect(wrapper.text()).toContain('A,C')
  })

  it('supports fill-answer checking and readonly fallback for legacy items', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({ data: { code: 0, data: chapterTree } })
      }
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 3,
              questionId: 102,
              chapterId: 4,
              difficulty: 'MEDIUM',
              questionTitle: 'Fill card',
              questionContent: 'If f(x)=2x+1, then f(3)=?',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: 'FILL',
              options: [],
              correctAnswer: '7',
              explanation: 'Substitute x=3 and get 7.',
              sourceLabel: 'Textbook Example',
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            },
            {
              id: 4,
              questionId: null,
              chapterId: 4,
              difficulty: null,
              questionTitle: 'Legacy card',
              questionContent: 'Legacy content',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: null,
              options: [],
              correctAnswer: null,
              explanation: null,
              sourceLabel: null,
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            }
          ]
        }
      })
    })

    const wrapper = mount(MistakesPage, { global: { plugins: [createPinia()] } })
    await flushPromises()

    const input = wrapper.find('.fill-answer input')
    await input.setValue('7')
    await wrapper.find('.fill-row .outline-btn').trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('回答正确。')
    expect(wrapper.text()).toContain('Substitute x=3 and get 7.')

    await wrapper.find('.mistake-toolbar__actions .outline-btn:last-child').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('题目原始作答数据缺失，当前仅支持加入收藏本或删除。')
    expect(wrapper.find('.check-answer-btn').attributes('disabled')).toBeDefined()
  })
})
