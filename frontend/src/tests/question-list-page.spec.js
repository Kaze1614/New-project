import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const listPayload = {
  items: [
    {
      id: 7,
      questionNo: 5,
      bookName: '必修第一册',
      chapterName: '第一章 集合与常用逻辑用语',
      sectionName: '集合的概念',
      contentPreview: '已知集合 A={1,2}，求 A 的元素个数。',
      sourceYear: 2025,
      sourcePaper: '全国甲卷',
      sourceLabel: '5.(2025)(全国甲卷)',
      updatedAt: '2026-04-20T10:00:00'
    }
  ],
  page: 1,
  size: 20,
  total: 1
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(() => Promise.resolve(listPayload)),
    delete: vi.fn(() => Promise.resolve(null))
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: { get: () => 'token', set: vi.fn(), clear: vi.fn() }
  }
})

import QuestionListPage from '../pages/QuestionListPage.vue'
import router from '../router'
import { api } from '../api/client'

describe('QuestionListPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    await router.push('/admin/questions?saved=created')
    await router.isReady()
  })

  it('renders table, success message and only one create entry', async () => {
    const wrapper = mount(QuestionListPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('题目已保存')
    expect(wrapper.text()).toContain('题库内容')
    expect(wrapper.text()).toContain('已知集合 A={1,2}')
    expect(wrapper.find('table.admin-table').exists()).toBe(true)
    expect(wrapper.findAll('a[href="/admin/questions/new"]')).toHaveLength(1)
    expect(wrapper.find('a[href="/admin/questions/7/edit"]').exists()).toBe(true)
  })

  it('searches and deletes question after confirmation', async () => {
    const wrapper = mount(QuestionListPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('input[type="search"]').setValue('集合')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(api.get).toHaveBeenLastCalledWith('/admin/math-questions', expect.objectContaining({
      params: expect.objectContaining({ keyword: '集合', page: 1 })
    }))

    await wrapper.find('.danger-btn').trigger('click')
    await flushPromises()
    expect(window.confirm).toHaveBeenCalled()
    expect(api.delete).toHaveBeenCalledWith('/admin/math-questions/7')
  })
})
