import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn(), delete: vi.fn() }
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

describe('FavoritesPage', () => {
  it('renders favorite cards with section labels only', async () => {
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
                    id: 3,
                    title: '极限',
                    children: [
                      { id: 5, title: '极限的定义', children: [] }
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
              id: 9,
              chapterId: 5,
              difficulty: 'HARD',
              title: '极限解析式',
              content: '已知 f(x+1)=2x+3，则 f(x)=？'
            }
          ]
        }
      })
    })

    const wrapper = mount(FavoritesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.find('.filter-bar').exists()).toBe(true)
    expect(wrapper.find('.question-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('极限解析式')
    expect(wrapper.text()).toContain('极限的定义')
    expect(wrapper.text()).not.toContain('HARD')
    expect(wrapper.text()).not.toContain('章节 5')
  })
})
