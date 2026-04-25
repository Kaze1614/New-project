import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn(), post: vi.fn(), delete: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import MistakesPage from '../pages/MistakesPage.vue'
import { api } from '../api/client'

describe('MistakesPage', () => {
  it('renders lightweight grid cards with filter bar', async () => {
    api.get.mockResolvedValueOnce([
      {
        id: 1,
        chapterId: 4,
        difficulty: 'MEDIUM',
        questionTitle: '函数定义域判定',
        questionContent: '函数 f(x)=1/(x-2) 的定义域是？'
      }
    ])

    const wrapper = mount(MistakesPage, {
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.find('.filter-bar').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('章节ID')
    expect(wrapper.text()).not.toContain('全部难度')
    expect(wrapper.find('.question-grid').exists()).toBe(true)
    expect(wrapper.text()).toContain('函数定义域判定')
    expect(wrapper.text()).not.toContain('AI分析')
  })
})
