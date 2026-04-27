import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const chapterTree = [
  {
    id: 1,
    title: 'Book One',
    children: [
      {
        id: 2,
        title: 'Functions',
        children: [
          { id: 3, title: 'Function Basics', children: [] }
        ]
      }
    ]
  }
]

const listPayload = {
  items: [
    {
      id: 7,
      questionNo: 5,
      bookName: 'Book One',
      chapterName: 'Functions',
      sectionName: 'Function Basics',
      contentPreview: 'Set A={1,2}, find its size.',
      sourceYear: 2025,
      sourcePaper: 'National A',
      sourceLabel: '5.(2025)(National A)',
      updatedAt: '2026-04-26T06:28:06Z'
    }
  ],
  page: 1,
  size: 20,
  total: 1
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn((url) => {
      if (url === '/chapters/tree') return Promise.resolve({ data: { data: chapterTree } })
      return Promise.resolve({ data: { data: listPayload } })
    }),
    delete: vi.fn(() => Promise.resolve({ data: { data: null } }))
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => {
      const response = await promise
      return response.data.data
    }),
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

  it('renders table, time and one create entry', async () => {
    const wrapper = mount(QuestionListPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('table.admin-table').exists()).toBe(true)
    expect(wrapper.find('.question-search__actions').exists()).toBe(true)
    expect(wrapper.find('.question-search__query').exists()).toBe(true)
    expect(wrapper.find('.question-search__buttons').exists()).toBe(true)
    expect(wrapper.findAll('.question-search__buttons button, .question-search__buttons a')).toHaveLength(3)
    expect(wrapper.text()).toContain('Set A={1,2}')
    expect(wrapper.text()).toContain('2026/4/26 06:28:06')
    expect(wrapper.html()).not.toContain('UTC+8')
    expect(wrapper.findAll('a[href="/admin/questions/new"]')).toHaveLength(1)
    expect(wrapper.find('a[href="/admin/questions/7/edit"]').exists()).toBe(true)
  })

  it('loads chapter tree filters and sends the most specific chapter params', async () => {
    const wrapper = mount(QuestionListPage, { global: { plugins: [router] } })
    await flushPromises()

    const selects = wrapper.findAll('select')
    expect(selects).toHaveLength(3)
    expect(wrapper.find('input[type="search"]').exists()).toBe(true)

    await selects[0].setValue('1')
    await selects[1].setValue('2')
    await selects[2].setValue('3')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.get).toHaveBeenLastCalledWith('/admin/math-questions', expect.objectContaining({
      params: expect.objectContaining({
        bookId: 1,
        chapterId: 2,
        sectionId: 3,
        page: 1,
        size: 20
      })
    }))
  })

  it('searches and deletes question after confirmation', async () => {
    const wrapper = mount(QuestionListPage, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.find('input[type="search"]').setValue('Set')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(api.get).toHaveBeenLastCalledWith('/admin/math-questions', expect.objectContaining({
      params: expect.objectContaining({ keyword: 'Set', page: 1 })
    }))

    await wrapper.find('.danger-btn').trigger('click')
    await flushPromises()
    expect(window.confirm).toHaveBeenCalled()
    expect(api.delete).toHaveBeenCalledWith('/admin/math-questions/7')
  })
})
