import { describe, expect, it, vi } from 'vitest'
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
  it('renders section labels instead of difficulty and chapter ids', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/chapters/tree') {
        return Promise.resolve({
          data: {
            code: 0,
            data: [
              {
                id: 1,
                title: '必修一',
                children: [
                  {
                    id: 2,
                    title: '函数',
                    children: [
                      { id: 4, title: '函数的定义域', children: [] }
                    ]
                  }
                ]
              }
            ]
          }
        })
      }
      return Promise.resolve({
        data: {
          code: 0,
          data: [
            {
              id: 1,
              questionId: 100,
              chapterId: 4,
              difficulty: 'MEDIUM',
              questionTitle: '函数定义域判断',
              questionContent: '函数 f(x)=1/(x-2) 的定义域是？'
            }
          ]
        }
      })
    })

    const wrapper = mount(MistakesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.find('.filter-bar').exists()).toBe(true)
    expect(wrapper.find('.question-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('函数定义域判断')
    expect(wrapper.text()).toContain('函数的定义域')
    expect(wrapper.text()).not.toContain('MEDIUM')
    expect(wrapper.text()).not.toContain('章节 4')
  })
})
