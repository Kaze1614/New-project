import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('../api/client', () => {
  const api = { get: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import DashboardPage from '../pages/DashboardPage.vue'
import { api } from '../api/client'

describe('DashboardPage', () => {
  it('renders kpi values from api', async () => {
    api.get.mockResolvedValueOnce({
      totalMistakes: 8,
      mastered: 3,
      pendingReview: 4,
      todayCompleted: 1
    })

    const wrapper = mount(DashboardPage, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('8')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('1')
  })
})
