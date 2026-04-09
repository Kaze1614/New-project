import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn(), post: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import QaPage from '../pages/QaPage.vue'
import { api } from '../api/client'

describe('QaPage', () => {
  it('appends assistant reply into message list', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/qa/sessions') {
        return Promise.resolve([{ id: 10, title: '会话A' }])
      }
      return Promise.resolve([])
    })

    api.post.mockResolvedValueOnce({
      userMessage: { id: 1, sessionId: 10, role: 'user', content: '什么是导数？' },
      assistantMessage: { id: 2, sessionId: 10, role: 'assistant', content: '导数描述函数变化率。' }
    })

    const wrapper = mount(QaPage)
    await flushPromises()

    await wrapper.find('input').setValue('什么是导数？')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('导数描述函数变化率。')
  })
})
