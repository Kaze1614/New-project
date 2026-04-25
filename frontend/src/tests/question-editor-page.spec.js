import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const chapterTree = [
  {
    id: 1,
    title: '必修第一册',
    children: [
      {
        id: 2,
        title: '第一章 集合与常用逻辑用语',
        children: [{ id: 3, title: '集合的概念', children: [] }]
      }
    ]
  }
]

const questionDetail = {
  id: 7,
  imageUrl: '/uploads/questions/2026/04/demo.png',
  rawTextLatex: '5.(2025)(全国甲卷) 已知集合 $A=\\{1,2\\}$，求 $A$。',
  answerLatex: 'A',
  teacherExplanation: '教师补充解析：直接观察集合元素。',
  bookName: '必修第一册',
  chapterName: '第一章 集合与常用逻辑用语',
  sectionName: '集合的概念',
  sourceYear: 2025,
  sourcePaper: '全国甲卷',
  questionNo: 5
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn((url) => {
      if (url === '/chapters/tree') return Promise.resolve(chapterTree)
      if (url === '/admin/math-questions/7') return Promise.resolve(questionDetail)
      return Promise.resolve({})
    }),
    post: vi.fn((url) => {
      if (url === '/admin/uploads/question-image') {
        return Promise.resolve({ url: '/uploads/questions/2026/04/demo.png', filename: 'demo.png', size: 9 })
      }
      if (url === '/admin/math-questions') {
        return Promise.resolve({ id: 8, sourceLabel: '5.(2025)(全国甲卷)' })
      }
      return Promise.resolve({})
    }),
    put: vi.fn(() => Promise.resolve({ id: 7 }))
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
  beforeEach(async () => {
    vi.clearAllMocks()
    await router.push('/admin/questions/new')
    await router.isReady()
  })

  it('registers admin question routes', () => {
    expect(router.hasRoute('admin-question-list')).toBe(true)
    expect(router.hasRoute('admin-question-new')).toBe(true)
    expect(router.hasRoute('admin-question-edit')).toBe(true)
  })

  it('supports clean editor labels, upload preview and create submit', async () => {
    const wrapper = mount(QuestionEditorPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('.symbol-toolbar').exists()).toBe(false)
    expect(wrapper.text()).toContain('题干')
    expect(wrapper.text()).toContain('标准答案')
    expect(wrapper.text()).toContain('教师解析')
    expect(wrapper.text()).not.toContain('待生成出处')
    expect(wrapper.text()).not.toContain('题干（LaTeX）')

    const selects = wrapper.findAll('select')
    await selects[0].setValue('1')
    await selects[1].setValue('2')
    await selects[2].setValue('3')

    const inputs = wrapper.findAll('input')
    await inputs.find((item) => item.attributes('placeholder') === '如：15').setValue('5')
    await inputs.find((item) => item.attributes('placeholder') === '2025').setValue('2025')
    await inputs.find((item) => item.attributes('placeholder') === '全国甲卷').setValue('全国甲卷')

    const rawTextarea = wrapper.findAll('textarea')[0]
    await rawTextarea.setValue('已知集合 $A=\\{1,2\\}$，求 $A$。')

    const file = new File(['content'], 'demo.png', { type: 'image/png' })
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [file] } })
    await flushPromises()
    expect(api.post).toHaveBeenCalledWith('/admin/uploads/question-image', expect.any(FormData))
    expect(wrapper.find('.upload-thumb').exists()).toBe(true)

    await wrapper.findAll('textarea')[1].setValue('A')
    await wrapper.findAll('textarea')[2].setValue('教师补充解析：直接观察集合元素。')

    expect(wrapper.find('.katex').exists()).toBe(true)

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/admin/math-questions', expect.objectContaining({
      imageUrl: '/uploads/questions/2026/04/demo.png',
      rawTextLatex: expect.stringContaining('5.(2025)(全国甲卷)'),
      bookName: '必修第一册',
      chapterName: '第一章 集合与常用逻辑用语',
      sectionName: '集合的概念',
      questionNo: 5
    }))
    expect(router.currentRoute.value.path).toBe('/admin/questions')
    expect(router.currentRoute.value.query.saved).toBe('created')
  })

  it('loads existing question and updates it in edit mode', async () => {
    await router.push('/admin/questions/7/edit')
    const wrapper = mount(QuestionEditorPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/admin/math-questions/7')
    expect(wrapper.findAll('textarea')[0].element.value).toContain('已知集合')
    expect(wrapper.findAll('textarea')[0].element.value).not.toContain('5.(2025)(全国甲卷)')
    expect(wrapper.text()).toContain('题目已加载')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/admin/math-questions/7', expect.objectContaining({
      rawTextLatex: expect.stringContaining('5.(2025)(全国甲卷)'),
      answerLatex: 'A'
    }))
    expect(router.currentRoute.value.path).toBe('/admin/questions')
    expect(router.currentRoute.value.query.saved).toBe('updated')
  })
})
