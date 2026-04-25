import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn(), delete: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import FavoritesPage from '../pages/FavoritesPage.vue'
import { api } from '../api/client'

describe('FavoritesPage', () => {
  it('renders favorite question cards with filters', async () => {
    api.get.mockResolvedValueOnce([
      {
        id: 9,
        chapterId: 5,
        difficulty: 'HARD',
        title: '函数解析式',
        content: '已知 f(x+1)=2x+3，则 f(x)=？'
      }
    ])

    const wrapper = mount(FavoritesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.find('.filter-bar').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('章节ID')
    expect(wrapper.text()).not.toContain('全部难度')
    expect(wrapper.find('.question-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('函数解析式')
    expect(wrapper.text()).toContain('章节 5')
  })
})
