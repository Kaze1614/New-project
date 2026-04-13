import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

let nextCallCount = 0

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(() => {
      nextCallCount += 1
      if (nextCallCount === 1) {
        return Promise.resolve({
          id: 7,
          questionTitle: '函数复习题',
          questionContent: '已知 f(x)=x^2，求导数',
          dueDate: new Date().toISOString(),
          officialAnswer: '2x',
          officialExplanation: '幂函数求导法则'
        })
      }
      return Promise.resolve(null)
    }),
    post: vi.fn(() => Promise.resolve({}))
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import ReviewPage from '../pages/ReviewPage.vue'

describe('ReviewPage', () => {
  it('supports blind -> reveal -> rate state flow', async () => {
    const wrapper = mount(ReviewPage, {
      global: {
        plugins: [createPinia()]
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('查看标准解析')
    await wrapper.find('.primary-btn').trigger('click')
    expect(wrapper.text()).toContain('官方解析')

    const rateButtons = wrapper.findAll('.rating')
    expect(rateButtons).toHaveLength(3)
    await rateButtons[0].trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('今日复习已清空')
  })
})
