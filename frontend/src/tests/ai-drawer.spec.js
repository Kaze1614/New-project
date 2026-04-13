import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

const hoisted = vi.hoisted(() => ({
  postMock: vi.fn(),
  getMock: vi.fn()
}))

vi.mock('../api/client', () => {
  const api = {
    get: hoisted.getMock,
    post: hoisted.postMock
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: {
      get: () => null,
      set: () => {},
      clear: () => {}
    }
  }
})

import AIAssistantDrawer from '../components/AIAssistantDrawer.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

describe('AIAssistantDrawer', () => {
  it('opens with context and injects it on first message', async () => {
    hoisted.getMock.mockResolvedValue([])
    hoisted.postMock.mockImplementation((url, payload) => {
      if (url === '/qa/sessions') return Promise.resolve({ id: 11, title: '全局答疑' })
      if (url === '/qa/sessions/11/messages') {
        return Promise.resolve({
          userMessage: { id: 1, sessionId: 11, role: 'user', content: payload.content },
          assistantMessage: { id: 2, sessionId: 11, role: 'assistant', content: '收到，开始讲解。' }
        })
      }
      return Promise.resolve({})
    })

    const pinia = createPinia()
    const wrapper = mount(AIAssistantDrawer, {
      global: {
        plugins: [pinia],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })

    const authStore = useAuthStore()
    authStore.token = 'token'
    const uiStore = useUiStore()
    uiStore.openAIDrawer('题目上下文')
    await flushPromises()

    await wrapper.find('input').setValue('这道题怎么做')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(hoisted.postMock).toHaveBeenCalledWith('/qa/sessions/11/messages', {
      content: '这道题怎么做',
      context: '题目上下文'
    })
    expect(wrapper.text()).toContain('收到，开始讲解。')
  })
})
