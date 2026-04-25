import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :data-to="to"><slot /></a>'
  },
  useRouter: () => ({ push })
}))

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
  it('renders four KPI cards from dashboard overview response', async () => {
    hoisted.tokenGet.mockReturnValue('token')
    hoisted.apiGet.mockResolvedValueOnce({
      questionBankTotal: 7,
      mastered: 6,
      pendingReview: 4,
      todayCompleted: 2,
      criticalReviewCount: 3,
      weakSpotHint: '函数及其表示 · 6题'
    })

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(wrapper.text()).toContain('题库总量')
    expect(wrapper.text()).toContain('已经掌握')
    expect(wrapper.text()).toContain('待复习')
    expect(wrapper.text()).toContain('今日完成')
    expect(wrapper.text()).toContain('7')
    expect(wrapper.text()).toContain('6')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('今日有 3 道题到达遗忘临界点')
  })

  it('shows locked guest state without mock numbers and opens login modal', async () => {
    hoisted.tokenGet.mockReturnValue('')
    hoisted.apiGet.mockReset()
    push.mockReset()

    const wrapper = mount(DashboardPage, {
      attachTo: document.body
    })
    await flushPromises()

    expect(hoisted.apiGet).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('欢迎来到数学园')
    expect(wrapper.text()).toContain('登录解锁')
    expect(wrapper.text()).toContain('--')
    expect(wrapper.text()).toContain('登录后查看今日复习任务与遗忘临界点')
    expect(wrapper.text()).not.toContain('15200')
    expect(wrapper.text()).not.toContain('8900')
    expect(wrapper.text()).not.toContain('450')
    expect(wrapper.text()).not.toContain('125')

    await wrapper.find('.action-card.primary').trigger('click')
    expect(document.body.textContent).toContain('请先登录以解锁完整学情分析与复习功能')

    document.body.querySelector('.login-modal .primary-btn').click()
    await flushPromises()
    expect(push).toHaveBeenCalledWith('/login')

    wrapper.unmount()
  })

  it('keeps empty state when overview request fails', async () => {
    hoisted.tokenGet.mockReturnValue('token')
    hoisted.apiGet.mockRejectedValueOnce(new Error('network'))

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(wrapper.text()).toContain('题库总量')
    expect(wrapper.text()).toContain('--')
    expect(wrapper.text()).not.toContain('15200')
    expect(wrapper.text()).not.toContain('8900')
    expect(wrapper.text()).not.toContain('450')
    expect(wrapper.text()).not.toContain('125')
  })
})
