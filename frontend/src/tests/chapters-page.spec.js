import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push })
}))

const catalog = [
  {
    id: 1,
    title: '必修第一册',
    children: [
      {
        id: 2,
        title: '第一章 集合与常用逻辑用语',
        children: [
          { id: 3, title: '集合的概念', children: [] },
          { id: 4, title: '集合间的基本关系', children: [] }
        ]
      },
      {
        id: 8,
        title: '第二章 一元二次函数、方程和不等式',
        children: [{ id: 9, title: '等式性质与不等式性质', children: [] }]
      }
    ]
  },
  {
    id: 100,
    title: '必修第二册',
    children: [
      {
        id: 101,
        title: '第六章 平面向量及其应用',
        children: [{ id: 102, title: '平面向量的概念', children: [] }]
      }
    ]
  }
]

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn(() => Promise.resolve(catalog))
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise)
  }
})

import ChaptersPage from '../pages/ChaptersPage.vue'

function findButton(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

describe('ChaptersPage', () => {
  it('defaults to all books and chapters collapsed', async () => {
    const wrapper = mount(ChaptersPage)
    await flushPromises()

    expect(wrapper.text()).toContain('必修第一册')
    expect(wrapper.text()).toContain('必修第二册')
    expect(wrapper.text()).not.toContain('第一章 集合与常用逻辑用语')
    expect(wrapper.text()).not.toContain('集合的概念')
    expect(wrapper.text()).not.toContain('平面向量的概念')
  })

  it('toggles book and chapter branches', async () => {
    const wrapper = mount(ChaptersPage)
    await flushPromises()

    await findButton(wrapper, '必修第一册').trigger('click')
    expect(wrapper.text()).toContain('第一章 集合与常用逻辑用语')
    expect(wrapper.text()).not.toContain('集合的概念')

    await findButton(wrapper, '第一章 集合与常用逻辑用语').trigger('click')
    expect(wrapper.text()).toContain('集合的概念')

    await findButton(wrapper, '第一章 集合与常用逻辑用语').trigger('click')
    expect(wrapper.text()).not.toContain('集合的概念')
  })

  it('navigates to study page when clicking a section', async () => {
    const wrapper = mount(ChaptersPage)
    await flushPromises()

    await findButton(wrapper, '必修第一册').trigger('click')
    await findButton(wrapper, '第一章 集合与常用逻辑用语').trigger('click')
    await findButton(wrapper, '集合的概念').trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/study', query: { chapterId: 3 } })
  })
})
