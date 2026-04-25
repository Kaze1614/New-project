import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'

const listPayload = {
  items: [],
  page: 1,
  size: 20,
  total: 0
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn((url) => {
      if (url === '/admin/math-questions') return Promise.resolve(listPayload)
      return Promise.resolve({ items: [], page: 1, size: 20, total: 0 })
    })
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: { get: () => 'token', set: vi.fn(), clear: vi.fn() }
  }
})

import AdminLayout from '../layout/AdminLayout.vue'
import router from '../router'

describe('AdminLayout', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    await router.push('/admin/questions')
    await router.isReady()
  })

  it('shows question and user management nav without duplicate create entry', async () => {
    const wrapper = mount(AdminLayout, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('题目管理')
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.findAll('a[href="/admin/questions/new"]')).toHaveLength(1)
    expect(wrapper.find('nav').text()).not.toContain('新增题目')
  })
})
