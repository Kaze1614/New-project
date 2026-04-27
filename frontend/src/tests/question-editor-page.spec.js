import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const chapterTree = [
  {
    id: 1,
    title: 'Book Two',
    children: [
      {
        id: 2,
        title: 'Derivatives',
        children: [{ id: 3, title: 'Derivative Basics', children: [] }]
      }
    ]
  }
]

const editQuestion = {
  id: 7,
  imageUrl: '/uploads/questions/2026/04/demo.png',
  questionType: 'SOLUTION',
  rawTextLatex: '5.(2025)(National A) Let f(x)=x^2.',
  options: [],
  answers: ['(1) 1'],
  subQuestions: [
    { index: 1, prompt: 'Find f(1)', referenceAnswer: '1', steps: [] }
  ],
  answerLatex: '(1) 1',
  teacherExplanation: 'Substitute first.',
  bookName: chapterTree[0].title,
  chapterName: chapterTree[0].children[0].title,
  sectionName: chapterTree[0].children[0].children[0].title,
  sourceYear: 2025,
  sourcePaper: 'National A',
  questionNo: 5
}

vi.mock('../api/client', () => {
  const api = {
    get: vi.fn((url) => {
      if (url === '/chapters/tree') return Promise.resolve({ data: { data: chapterTree } })
      if (url === '/admin/math-questions/7') return Promise.resolve({ data: { data: editQuestion } })
      return Promise.resolve({ data: { data: {} } })
    }),
    post: vi.fn((url) => {
      if (url === '/admin/uploads/question-image') {
        return Promise.resolve({ data: { data: { url: '/uploads/questions/2026/04/demo.png' } } })
      }
      if (url === '/admin/math-questions') {
        return Promise.resolve({ data: { data: { id: 8, questionNo: 6, sourceLabel: '6.(2025)(National A)' } } })
      }
      return Promise.resolve({ data: { data: {} } })
    }),
    put: vi.fn(() => Promise.resolve({ data: { data: { id: 7, questionNo: 5 } } }))
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

  it('submits single-choice payload without manual questionNo', async () => {
    const wrapper = mount(QuestionEditorPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('.preview-image-wrap').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('questionNo')
    expect(wrapper.text()).toContain('无')

    const sectionTitles = wrapper.findAll('.editor-left .admin-section h2').map((item) => item.text())
    expect(sectionTitles.slice(0, 2)).toEqual(['题干编辑', '图片附件'])

    const selects = wrapper.findAll('select')
    await selects[0].setValue('1')
    await selects[1].setValue('2')
    await selects[2].setValue('3')
    await selects[3].setValue('SINGLE')

    const allInputs = wrapper.findAll('input')
    const sourceYearInput = allInputs.find((item) => item.attributes('placeholder') === '2025')
    const sourcePaperInput = allInputs.find((item) => item.attributes('placeholder') === '全国甲卷')
    await sourceYearInput.setValue('2026')
    await sourcePaperInput.setValue('Mock A')

    const textareas = wrapper.findAll('textarea')
    await textareas[0].setValue('Which axis is symmetric for y=|x|?')

    const optionInputs = wrapper.findAll('.choice-option-row input')
    await optionInputs[0].setValue('x-axis')
    await optionInputs[1].setValue('y-axis')
    await optionInputs[2].setValue('line y=x')
    await optionInputs[3].setValue('origin')

    const setAnswerButtons = wrapper.findAll('.choice-option-row .outline-btn')
    await setAnswerButtons[1].trigger('click')

    const explanationTextarea = wrapper.findAll('textarea')[1]
    await explanationTextarea.setValue('Absolute value is symmetric about the y-axis.')

    const file = new File(['content'], 'demo.png', { type: 'image/png' })
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [file] } })
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/admin/uploads/question-image', expect.any(FormData), expect.any(Object))
    expect(wrapper.find('.preview-image-wrap').exists()).toBe(true)
    expect(wrapper.find('.preview-source-label').text()).toBe('(2026)(Mock A)')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const payload = api.post.mock.calls.find(([url]) => url === '/admin/math-questions')[1]
    expect(payload).toMatchObject({
      imageUrl: '/uploads/questions/2026/04/demo.png',
      questionType: 'SINGLE',
      rawTextLatex: 'Which axis is symmetric for y=|x|?',
      answers: ['B'],
      teacherExplanation: 'Absolute value is symmetric about the y-axis.',
      bookName: chapterTree[0].title,
      chapterName: chapterTree[0].children[0].title,
      sectionName: chapterTree[0].children[0].children[0].title,
      sourceYear: 2026,
      sourcePaper: 'Mock A'
    })
    expect(payload.options).toEqual([
      { key: 'A', content: 'x-axis' },
      { key: 'B', content: 'y-axis' },
      { key: 'C', content: 'line y=x' },
      { key: 'D', content: 'origin' }
    ])
    expect(payload.questionNo).toBeUndefined()
    expect(router.currentRoute.value.path).toBe('/admin/questions')
    expect(router.currentRoute.value.query.saved).toBe('created')
  })

  it('loads existing solution detail and keeps source preview without question number', async () => {
    await router.push('/admin/questions/7/edit')
    const wrapper = mount(QuestionEditorPage, { global: { plugins: [router] } })
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/admin/math-questions/7')
    expect(wrapper.find('.solution-editor').exists()).toBe(true)
    expect(wrapper.findAll('.sub-question-card')).toHaveLength(1)
    expect(wrapper.find('.preview-source-label').text()).toBe('(2025)(National A)')

    const promptTextarea = wrapper.findAll('.solution-editor textarea')[0]
    await promptTextarea.setValue('Find f(2)')
    const refAnswerTextarea = wrapper.findAll('.solution-editor textarea')[1]
    await refAnswerTextarea.setValue('4')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const payload = api.put.mock.calls[0][1]
    expect(payload.questionType).toBe('SOLUTION')
    expect(payload.answers).toEqual([])
    expect(payload.subQuestions).toEqual([
      { index: 1, prompt: 'Find f(2)', referenceAnswer: '4', steps: [] }
    ])
    expect(payload.questionNo).toBeUndefined()
    expect(router.currentRoute.value.path).toBe('/admin/questions')
    expect(router.currentRoute.value.query.saved).toBe('updated')
  })
})
