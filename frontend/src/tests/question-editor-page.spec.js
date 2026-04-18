import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const chapterTree = [
  {
    id: 1,
    title: '必修第一册',
    children: [
      {
        id: 2,
        title: '第一章 集合与常用逻辑用语',
        children: [{ id: 3, title: '1.3 集合的基本运算', children: [] }]
      }
    ]
  }
]

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn((url) => {
      if (url === '/chapters/tree') return Promise.resolve(chapterTree)
      return Promise.resolve({})
    }),
    post: vi.fn((url) => {
      if (url === '/admin/uploads/question-image') {
        return Promise.resolve({ url: '/uploads/questions/2026/04/demo.png', filename: 'demo.png', size: 9 })
      }
      if (url === '/admin/math-questions') {
        return Promise.resolve({ id: 7, sourceLabel: '5.(2025)(全国甲卷)' })
      }
      return Promise.resolve({})
    })
  }
  return {
    api,
    unwrap: vi.fn(async (promise) => promise),
    tokenStorage: { get: () => 'token', set: vi.fn(), clear: vi.fn() }
  }
})

import QuestionEditorPage from '../pages/QuestionEditorPage.vue'
import router from '../router'
import { api } from '../api/client'

describe('QuestionEditorPage', () => {
  it('registers admin route', () => {
    expect(router.hasRoute('admin-question-new')).toBe(true)
  })

  it('supports chapter cascade, symbol insertion, upload preview and submit', async () => {
    const wrapper = mount(QuestionEditorPage)
    await flushPromises()

    const selects = wrapper.findAll('select')
    await selects[0].setValue('1')
    await selects[1].setValue('2')
    await selects[2].setValue('3')

    const inputs = wrapper.findAll('input')
    const questionNoInput = inputs.find((item) => item.attributes('placeholder') === '如：15')
    await questionNoInput.setValue('5')
    const yearInput = inputs.find((item) => item.attributes('placeholder') === '2025')
    await yearInput.setValue('2025')
    const paperInput = inputs.find((item) => item.attributes('placeholder') === '全国甲卷')
    await paperInput.setValue('全国甲卷')

    const rawTextarea = wrapper.findAll('textarea')[0]
    await rawTextarea.setValue('已知集合 $A=\\{1,2\\}$，求 $A$。')
    await wrapper.find('.symbol-toolbar button').trigger('click')
    expect(rawTextarea.element.value).toContain('\\frac{}{}')

    const file = new File(['content'], 'demo.png', { type: 'image/png' })
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [file] } })
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/admin/uploads/question-image', expect.any(FormData))
    expect(wrapper.find('.upload-thumb').exists()).toBe(true)

    await wrapper.findAll('textarea')[1].setValue('A')
    await wrapper.findAll('textarea')[2].setValue('教师补充解析：直接观察集合元素。')

    expect(wrapper.text()).toContain('5.(2025)(全国甲卷)')
    expect(wrapper.find('.katex').exists()).toBe(true)

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/admin/math-questions', expect.objectContaining({
      imageUrl: '/uploads/questions/2026/04/demo.png',
      rawTextLatex: expect.stringContaining('5.(2025)(全国甲卷)'),
      bookName: '必修第一册',
      chapterName: '第一章 集合与常用逻辑用语',
      sectionName: '1.3 集合的基本运算',
      questionNo: 5
    }))
    expect(wrapper.text()).toContain('已提交入库')
  })
})
