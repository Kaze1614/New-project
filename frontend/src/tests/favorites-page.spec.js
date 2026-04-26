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

import FavoritesPage from '../pages/FavoritesPage.vue'
import { api } from '../api/client'

const chapterTree = [
  {
    id: 1,
    title: '高等数学上册',
    children: [
      {
        id: 3,
        title: '极限',
        children: [
          { id: 5, title: '极限的定义', children: [] },
          { id: 6, title: '无穷小比较', children: [] }
        ]
      }
    ]
  }
]

describe('FavoritesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders three-column favorite cards with chapter filters and review action', async () => {
    const requestParams = []
    api.get.mockImplementation((url, config) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({
          data: { code: 0, data: chapterTree }
        })
      }
      requestParams.push(config?.params ?? {})
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 9,
              questionId: 200,
              chapterId: 5,
              difficulty: 'HARD',
              title: '极限基础判断',
              content: '已知 lim x→0 sinx/x = ?',
              questionType: 'SINGLE',
              options: ['A. 0', 'B. 1', 'C. -1', 'D. 不存在'],
              correctAnswer: 'B',
              explanation: '经典极限，结果为 1。',
              sourceLabel: '教材例题',
              createdAt: '2026-04-26T10:00:00'
            }
          ]
        }
      })
    })
    api.post.mockResolvedValue({ data: { code: 0, data: null } })

    const wrapper = mount(FavoritesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.find('.favorites-filter-bar').exists()).toBe(true)
    expect(wrapper.find('.favorites-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('极限基础判断')
    expect(wrapper.text()).toContain('极限的定义')
    expect(wrapper.text()).toContain('加入待复习')

    const selects = wrapper.findAll('select')
    await selects[0].setValue('1')
    await flushPromises()
    await selects[1].setValue('3')
    await flushPromises()
    await selects[2].setValue('5')
    await flushPromises()
    await wrapper.find('.favorites-filter-bar').trigger('submit')
    await flushPromises()

    expect(requestParams).toContainEqual({})
    expect(requestParams).toContainEqual({ chapterId: 5 })

    await wrapper.find('.mistake-option:nth-child(1)').trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('回答错误！')
    expect(wrapper.text()).toContain('正确答案：')

    await wrapper.find('.favorite-actions .outline-btn').trigger('click')
    expect(api.post).toHaveBeenCalledWith('/favorites/9/review')
  })

  it('supports multi-choice and fill-answer cards', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({ data: { code: 0, data: chapterTree } })
      }
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 10,
              questionId: 201,
              chapterId: 5,
              difficulty: 'MEDIUM',
              title: '集合运算',
              content: '属于集合 A 的元素有？',
              questionType: 'MULTI',
              options: ['A. 1', 'B. 2', 'C. 3', 'D. 4'],
              correctAnswer: 'A,C',
              explanation: '正确选项为 A 和 C。',
              sourceLabel: '教材例题',
              createdAt: '2026-04-26T10:00:00'
            },
            {
              id: 11,
              questionId: 202,
              chapterId: 6,
              difficulty: 'EASY',
              title: '函数值计算',
              content: '已知 f(x)=2x+1，则 f(3)=？',
              questionType: 'FILL',
              options: [],
              correctAnswer: '7',
              explanation: '代入即可得到 7。',
              sourceLabel: '教材例题',
              createdAt: '2026-04-26T10:05:00'
            }
          ]
        }
      })
    })

    const wrapper = mount(FavoritesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    const cards = wrapper.findAll('.favorite-card')
    const multiCard = cards[0]
    const fillCard = cards[1]

    const multiOptions = multiCard.findAll('.mistake-option')
    await multiOptions[0].trigger('click')
    await multiOptions[2].trigger('click')
    await multiCard.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(multiCard.text()).toContain('回答正确！')
    expect(multiCard.text()).toContain('A,C')

    const fillInput = fillCard.find('.fill-answer input')
    await fillInput.setValue('7')
    await fillCard.find('.fill-row .outline-btn').trigger('click')
    await fillCard.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(fillCard.text()).toContain('回答正确！')
    expect(fillCard.text()).toContain('代入即可得到 7。')
  })

  it('keeps legacy favorite read-only when question data is missing', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({ data: { code: 0, data: chapterTree } })
      }
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 12,
              questionId: null,
              chapterId: 5,
              difficulty: null,
              title: '旧收藏题',
              content: '旧收藏内容',
              questionType: null,
              options: [],
              correctAnswer: null,
              explanation: null,
              sourceLabel: null,
              createdAt: '2026-04-26T10:00:00'
            }
          ]
        }
      })
    })

    const wrapper = mount(FavoritesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('题目原始作答数据缺失，当前仅支持加入待复习或删除。')
    expect(wrapper.find('.check-answer-btn').attributes('disabled')).toBeDefined()
  })
})
