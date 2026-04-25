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

describe('MistakesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders 7:3 layout, supports chapter filtering and shows wrong-answer feedback', async () => {
    const requestParams = []

    api.get.mockImplementation((url, config) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({
          data: {
            code: 0,
            data: [
              {
                id: 1,
                title: '高等数学上册',
                children: [
                  {
                    id: 2,
                    title: '函数',
                    children: [{ id: 4, title: '函数的定义域', children: [] }]
                  }
                ]
              }
            ]
          }
        })
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
              questionTitle: '2016 年 408 真题 第 9 题',
              questionContent: '函数 f(x)=1/(x-2) 的定义域是？',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: 'SINGLE',
              options: ['A. x>2', 'B. x<2', 'C. x≠2', 'D. x=2'],
              correctAnswer: 'C',
              explanation: '分母不能为 0，所以 x≠2。',
              sourceLabel: '2016 年 408 真题 第 9 题',
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            }
          ]
        }
      })
    })
    api.post.mockResolvedValue({ data: { code: 0, data: {} } })

    const wrapper = mount(MistakesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    await wrapper.find('.section-link').trigger('click')
    await flushPromises()

    expect(requestParams).toContainEqual({})
    expect(requestParams).toContainEqual({ chapterId: 4 })
    expect(wrapper.find('.mistake-layout').exists()).toBe(true)
    expect(wrapper.find('.mistake-card-detail').exists()).toBe(true)
    expect(wrapper.find('.chapter-tree-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('2016 年 408 真题 第 9 题')
    expect(wrapper.text()).toContain('函数的定义域')
    expect(wrapper.text()).toContain('加入收藏本')

    const options = wrapper.findAll('.mistake-option')
    await options[1].trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(options[1].classes()).toContain('wrong')
    expect(options[2].classes()).toContain('correct')
    expect(wrapper.text()).toContain('回答错误！')
    expect(wrapper.text()).toContain('正确答案：')
    expect(wrapper.text()).toContain('解析：')

    await wrapper.find('.favorite-btn').trigger('click')
    expect(api.post).toHaveBeenCalledWith('/favorites', {
      questionId: 100,
      chapterId: 4,
      title: '2016 年 408 真题 第 9 题',
      content: '函数 f(x)=1/(x-2) 的定义域是？'
    })
  })

  it('shows success feedback when the selected answer is correct', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({
          data: {
            code: 0,
            data: [{ id: 1, title: '高等数学上册', children: [] }]
          }
        })
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
              questionTitle: '函数单调性',
              questionContent: '已知 f(x)=x^2，正确选项是？',
              imageUrl: null,
              status: 'REVIEWING',
              questionType: 'SINGLE',
              options: ['A. 单调递减', 'B. 在全域单调递增', 'C. 关于 y 轴对称', 'D. 恒为负数'],
              correctAnswer: 'C',
              explanation: 'x^2 是偶函数，图像关于 y 轴对称。',
              sourceLabel: '教材例题',
              createdAt: '2026-04-25T10:00:00',
              updatedAt: '2026-04-25T10:00:00',
              analysis: null
            }
          ]
        }
      })
    })

    const wrapper = mount(MistakesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    const options = wrapper.findAll('.mistake-option')
    await options[2].trigger('click')
    await wrapper.find('.check-answer-btn').trigger('click')
    await flushPromises()

    expect(options[2].classes()).toContain('correct')
    expect(wrapper.text()).toContain('回答正确！')
    expect(wrapper.text()).toContain('解析：')
  })
})
