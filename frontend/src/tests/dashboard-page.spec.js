import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const hoisted = vi.hoisted(() => ({
  tokenGet: vi.fn(),
  apiGet: vi.fn()
}))

vi.mock('../api/client', () => {
  const api = {
    get: hoisted.apiGet
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: {
      get: hoisted.tokenGet,
      set: vi.fn(),
      clear: vi.fn()
    }
  }
})

import DashboardPage from '../pages/DashboardPage.vue'

describe('DashboardPage', () => {
  it('renders four required KPI cards with logged-in data', async () => {
    hoisted.tokenGet.mockReturnValue('token')
    hoisted.apiGet.mockResolvedValueOnce({
      questionBankTotal: 20,
      mastered: 6,
      pendingReview: 4,
      todayCompleted: 2,
      criticalReviewCount: 3,
      weakSpotHint: '函数及其表示 · 6题'
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

    expect(wrapper.text()).toContain('题库总量')
    expect(wrapper.text()).toContain('已经掌握')
    expect(wrapper.text()).toContain('待复习')
    expect(wrapper.text()).toContain('今日完成')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('6')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('3 道题到达遗忘临界点')
  })

  it('shows guest empty state and does not fetch private overview without token', async () => {
    hoisted.tokenGet.mockReturnValue('')
    hoisted.apiGet.mockReset()

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

    expect(hoisted.apiGet).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('欢迎来到数学园')
    expect(wrapper.text()).toContain('去登录')
    expect(wrapper.text()).toContain('--')
  })
})
