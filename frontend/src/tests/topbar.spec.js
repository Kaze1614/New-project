import { describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :data-to="to"><slot /></a>'
  },
  useRouter: () => ({ push })
}))

import TopBar from '../components/TopBar.vue'
import { useAuthStore } from '../stores/auth'

describe('TopBar', () => {
  it('shows login/register for guests and keeps search on the right action area', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(TopBar, {
      global: {
        plugins: [pinia]
      }
    })

    expect(wrapper.find('.topbar-actions .topbar-search').exists()).toBe(true)
    expect(wrapper.find('.user-avatar').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('退出登录')
    expect(wrapper.text()).toContain('登录 / 注册')
    expect(wrapper.find('[data-to="/login"]').exists()).toBe(true)
    expect(wrapper.find('kbd').exists()).toBe(false)
    expect(wrapper.find('[aria-label="通知"]').exists()).toBe(false)

    await wrapper.find('input[type="search"]').setValue('函数')
    await wrapper.find('form').trigger('submit')
    expect(push).toHaveBeenCalledWith({ path: '/chapters', query: { keyword: '函数' } })
  })

  it('shows avatar and logout only when logged in', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.token = 'token'
    authStore.user = { username: 'student01', displayName: '陈同学' }

    const wrapper = mount(TopBar, {
      global: {
        plugins: [pinia]
      }
    })

    expect(wrapper.find('.user-avatar').text()).toBe('陈')
    expect(wrapper.text()).toContain('退出登录')
    expect(wrapper.text()).not.toContain('登录 / 注册')
  })
})
