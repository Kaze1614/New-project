import { describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('../api/client', () => {
  const api = { get: vi.fn() }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import SidebarNav from '../components/SidebarNav.vue'
import { api } from '../api/client'

describe('SidebarNav', () => {
  it('renders compact five-item navigation and chapter flyout trigger', async () => {
    api.get.mockResolvedValueOnce([{ id: 1, title: '必修一', children: [] }])

    const wrapper = mount(SidebarNav, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('收藏本')
    expect(wrapper.text()).toContain('待复习')
    expect(wrapper.text()).toContain('章节目录')
  })
})
