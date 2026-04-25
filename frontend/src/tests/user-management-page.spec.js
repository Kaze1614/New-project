import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const userPayload = {
  items: [
    { id: 1, username: 'admin', createdAt: '2026-04-20T10:00:00', role: 'ADMIN' },
    { id: 2, username: 'student_a', createdAt: '2026-04-20T11:00:00', role: 'STUDENT' }
  ],
  page: 1,
  size: 20,
  total: 2
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(() => Promise.resolve(userPayload)),
    post: vi.fn(() => Promise.resolve(null)),
    delete: vi.fn(() => Promise.resolve(null))
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: { get: () => 'token', set: vi.fn(), clear: vi.fn() }
  }
})

import UserManagementPage from '../pages/UserManagementPage.vue'
import router from '../router'
import { api } from '../api/client'

describe('UserManagementPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    await router.push('/admin/users')
    await router.isReady()
  })

  it('renders user table and protects admin delete action', async () => {
    const wrapper = mount(UserManagementPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('student_a')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('学生')
    expect(wrapper.find('table.admin-table').exists()).toBe(true)

    const dangerButtons = wrapper.findAll('.danger-btn')
    expect(dangerButtons[0].attributes('disabled')).toBeDefined()
    expect(dangerButtons[1].attributes('disabled')).toBeUndefined()
  })

  it('resets password and deletes normal user after confirmation', async () => {
    const wrapper = mount(UserManagementPage, { global: { plugins: [router] } })
    await flushPromises()

    const actionButtons = wrapper.findAll('.table-actions .outline-btn')
    await actionButtons[0].trigger('click')
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/admin/users/1/reset-password')

    const dangerButtons = wrapper.findAll('.danger-btn')
    await dangerButtons[1].trigger('click')
    await flushPromises()
    expect(api.delete).toHaveBeenCalledWith('/admin/users/2')
  })
})
