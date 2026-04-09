import { describe, expect, it, vi } from 'vitest'
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
  it('shows analysis details after analyze action', async () => {
    api.get.mockResolvedValueOnce([
      {
        id: 1,
        chapterId: 4,
        questionTitle: '极限题',
        questionContent: '求极限',
        imageUrl: null,
        status: 'NEW',
        analysis: null
      }
    ])
    api.post.mockResolvedValueOnce({
      knowledgePoints: ['极限存在性'],
      errorType: '概念混淆',
      solvingSteps: ['步骤1', '步骤2'],
      variants: ['变式1'],
      followUpSuggestions: ['建议1']
    })

    const wrapper = mount(MistakesPage)
    await flushPromises()

    await wrapper.findAll('button').find((btn) => btn.text() === 'AI分析').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('概念混淆')
    expect(wrapper.text()).toContain('极限存在性')
  })
})
