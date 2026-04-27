<template>
  <section class="question-editor-page">
    <form class="question-editor" @submit.prevent="submitForm">
      <section class="admin-section relation-strip" aria-label="教材关联">
        <div>
          <p class="section-eyebrow">Section A</p>
          <h2>教材关联</h2>
        </div>

        <label>
          <span>教材名</span>
          <select v-model.number="form.bookId" @change="handleBookChange">
            <option v-for="book in chapterTree" :key="book.id" :value="book.id">{{ book.title }}</option>
          </select>
        </label>

        <label>
          <span>章节名</span>
          <select v-model.number="form.chapterId" @change="handleChapterChange">
            <option v-for="chapter in currentChapters" :key="chapter.id" :value="chapter.id">{{ chapter.title }}</option>
          </select>
        </label>

        <label>
          <span>小节名</span>
          <select v-model.number="form.sectionId">
            <option v-for="section in currentSections" :key="section.id" :value="section.id">{{ section.title }}</option>
          </select>
        </label>

        <label>
          <span>题型</span>
          <select v-model="form.questionType" @change="handleTypeChange">
            <option value="SINGLE">单选</option>
            <option value="MULTI">多选</option>
            <option value="FILL">填空</option>
            <option value="SOLUTION">解答</option>
          </select>
        </label>
      </section>

      <div class="editor-workspace">
        <div class="editor-left">
          <section class="admin-section latex-card">
            <div class="section-headline">
              <div>
                <p class="section-eyebrow">Section B</p>
                <h2>题干编辑</h2>
              </div>
            </div>

            <label class="textarea-field">
              <span>题干</span>
              <textarea
                v-model.trim="form.rawTextLatex"
                rows="8"
                maxlength="5000"
                placeholder="请输入题干内容"
              ></textarea>
            </label>
          </section>

          <section class="admin-section upload-card">
            <div class="section-headline">
              <div>
                <p class="section-eyebrow">Section C</p>
                <h2>图片附件</h2>
              </div>
            </div>

            <input
              ref="fileInput"
              class="sr-only"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              @change="handleFileChange"
            />

            <div
              class="dropzone"
              :class="{ active: uploading }"
              tabindex="0"
              role="button"
              @click="openFileDialog"
              @dragover.prevent
              @drop.prevent="handleDrop"
              @paste="handlePaste"
              @keydown.enter.prevent="openFileDialog"
              @keydown.space.prevent="openFileDialog"
            >
              <div class="dropzone-copy">
                <strong>{{ uploading ? '上传中...' : '选择图片或拖拽到此处' }}</strong>
                <span>支持 PNG、JPG、WEBP，单张不超过 10MB。</span>
              </div>
              <button class="outline-btn" type="button" @click.stop="openFileDialog">选择图片</button>
            </div>

            <p v-if="uploadError" class="warning-text">{{ uploadError }}</p>
            <img v-if="form.imageUrl" class="upload-thumb" :src="form.imageUrl" alt="题目附件预览" />
          </section>

          <section class="admin-section answer-grid">
            <div class="type-editor-head">
              <div>
                <p class="section-eyebrow">Section D</p>
                <h2>正确答案与解析</h2>
              </div>
              <span class="type-chip">{{ currentTypeLabel }}</span>
            </div>

            <div v-if="isChoiceType" class="choice-editor">
              <div class="editor-block-head">
                <h3>选项设置</h3>
                <button
                  class="outline-btn"
                  type="button"
                  :disabled="form.choiceOptions.length >= 6"
                  @click="addChoiceOption"
                >
                  新增选项
                </button>
              </div>

              <div v-for="(option, index) in form.choiceOptions" :key="option.id" class="choice-option-row">
                <span class="choice-option-key">{{ option.key }}</span>
                <input
                  v-model.trim="option.content"
                  type="text"
                  :placeholder="`请输入 ${option.key} 选项内容`"
                />
                <button class="outline-btn" type="button" @click="toggleChoiceAnswer(option.key)">
                  {{ isChoiceAnswerSelected(option.key) ? '取消答案' : '设为答案' }}
                </button>
                <button
                  class="danger-btn"
                  type="button"
                  :disabled="form.choiceOptions.length <= 4"
                  @click="removeChoiceOption(index)"
                >
                  删除
                </button>
              </div>
            </div>

            <div v-else-if="isFillType" class="fill-answer-editor">
              <div class="editor-block-head">
                <h3>正确答案</h3>
                <button class="outline-btn" type="button" @click="addFillAnswer">新增空位</button>
              </div>

              <div v-for="(answer, index) in form.fillAnswers" :key="answer.id" class="fill-answer-row">
                <span class="fill-answer-index">第 {{ index + 1 }} 空</span>
                <input v-model.trim="answer.content" type="text" placeholder="请输入该空正确答案" />
                <button
                  class="danger-btn"
                  type="button"
                  :disabled="form.fillAnswers.length <= 1"
                  @click="removeFillAnswer(index)"
                >
                  删除
                </button>
              </div>
            </div>

            <div v-else class="solution-editor">
              <div class="editor-block-head">
                <h3>小问设置</h3>
                <button class="outline-btn" type="button" @click="addSubQuestion">新增小问</button>
              </div>

              <article
                v-for="(subQuestion, index) in form.subQuestions"
                :key="subQuestion.id"
                class="sub-question-card"
              >
                <div class="sub-question-head">
                  <h4>第 {{ index + 1 }} 小问</h4>
                  <button
                    class="danger-btn"
                    type="button"
                    :disabled="form.subQuestions.length <= 1"
                    @click="removeSubQuestion(index)"
                  >
                    删除小问
                  </button>
                </div>

                <label class="textarea-field">
                  <span>小问题干</span>
                  <textarea
                    v-model.trim="subQuestion.prompt"
                    rows="4"
                    maxlength="2000"
                    placeholder="请输入该小问题干"
                  ></textarea>
                </label>

                <label class="textarea-field">
                  <span>参考答案</span>
                  <textarea
                    v-model.trim="subQuestion.referenceAnswer"
                    rows="4"
                    maxlength="2000"
                    placeholder="请输入该小问参考答案"
                  ></textarea>
                </label>
              </article>
            </div>

            <label class="textarea-field">
              <span>解析</span>
              <textarea
                v-model.trim="form.teacherExplanation"
                rows="6"
                maxlength="4000"
                placeholder="请输入解析内容"
              ></textarea>
            </label>
          </section>

          <section class="admin-section source-card">
            <label>
              <span>年份</span>
              <input v-model.trim="form.sourceYearText" type="text" inputmode="numeric" placeholder="2025" />
            </label>

            <label>
              <span>试卷来源</span>
              <input v-model.trim="form.sourcePaper" type="text" placeholder="全国甲卷" maxlength="80" />
            </label>
          </section>
        </div>

        <aside class="preview-pane">
          <div class="preview-sticky">
            <section class="admin-section student-preview-card">
              <div class="preview-head">
                <div>
                  <p class="section-eyebrow">Preview</p>
                  <h2>学生端预览</h2>
                </div>
                <span class="student-chip">{{ currentTypeLabel }}</span>
              </div>

              <div class="preview-question-meta">
                <span>{{ currentBookTitle || '未选择教材' }}</span>
                <span>{{ currentChapterTitle || '未选择章节' }}</span>
                <span>{{ currentSectionTitle || '未选择小节' }}</span>
              </div>

              <div class="preview-block">
                <h3>来源标记</h3>
                <p class="preview-source-label">{{ previewSourceLabel }}</p>
              </div>

              <div v-if="form.imageUrl" class="preview-image-wrap">
                <img :src="form.imageUrl" alt="题目图片预览" />
              </div>

              <div class="preview-block">
                <h3>题干</h3>
                <div class="rendered-latex">{{ form.rawTextLatex || '请输入题干后预览' }}</div>
              </div>

              <div v-if="isChoiceType" class="preview-block">
                <h3>选项</h3>
                <div class="preview-option-list">
                  <div v-for="option in previewChoiceOptions" :key="option.key" class="preview-option-item">
                    <strong>{{ option.key }}.</strong>
                    <span>{{ option.content || '待补充选项内容' }}</span>
                  </div>
                </div>
              </div>

              <div v-else-if="isFillType" class="preview-block">
                <h3>正确答案</h3>
                <div class="preview-option-list">
                  <div v-for="(answer, index) in previewFillAnswers" :key="index" class="preview-option-item">
                    <strong>第 {{ index + 1 }} 空</strong>
                    <span>{{ answer || '待补充正确答案' }}</span>
                  </div>
                </div>
              </div>

              <div v-else class="preview-block">
                <h3>小问与参考答案</h3>
                <div class="preview-sub-question-list">
                  <article
                    v-for="(subQuestion, index) in previewSubQuestions"
                    :key="index"
                    class="preview-sub-question"
                  >
                    <h4>（{{ index + 1 }}）{{ subQuestion.prompt || '待补充小问题干' }}</h4>
                    <p class="preview-sub-answer">参考答案：{{ subQuestion.referenceAnswer || '待补充' }}</p>
                  </article>
                </div>
              </div>

              <div class="preview-block">
                <h3>解析</h3>
                <div class="rendered-latex">{{ form.teacherExplanation || '无' }}</div>
              </div>
            </section>
          </div>
        </aside>
      </div>

      <footer class="admin-submit-bar">
        <p class="submit-message" :class="submitStateClass">{{ submitMessage }}</p>
        <div class="submit-actions">
          <button class="outline-btn" type="button" @click="router.push('/admin/questions')">返回列表</button>
          <button class="primary-btn" type="submit" :disabled="saving || loading || uploading">
            {{ saving ? '保存中...' : isEditMode ? '保存修改' : '提交题目' }}
          </button>
        </div>
      </footer>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, unwrap } from '../api/client'

const route = useRoute()
const router = useRouter()

const fileInput = ref(null)
const chapterTree = ref([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const uploadError = ref('')
const submitMessage = ref('准备提交题目。')
const submitState = ref('')
const currentQuestionNo = ref(null)

const form = reactive({
  bookId: null,
  chapterId: null,
  sectionId: null,
  questionType: 'SINGLE',
  rawTextLatex: '',
  imageUrl: '',
  sourceYearText: '',
  sourcePaper: '',
  teacherExplanation: '',
  choiceOptions: createDefaultChoiceOptions(),
  choiceAnswers: [],
  fillAnswers: [createFillAnswer()],
  subQuestions: [createSubQuestion()]
})

const isEditMode = computed(() => Boolean(route.params.id))
const isChoiceType = computed(() => ['SINGLE', 'MULTI'].includes(form.questionType))
const isFillType = computed(() => form.questionType === 'FILL')
const isSolutionType = computed(() => form.questionType === 'SOLUTION')

const currentBook = computed(() => chapterTree.value.find((item) => item.id === form.bookId) ?? null)
const currentChapters = computed(() => currentBook.value?.children ?? [])
const currentChapter = computed(() => currentChapters.value.find((item) => item.id === form.chapterId) ?? null)
const currentSections = computed(() => currentChapter.value?.children ?? [])

const currentBookTitle = computed(() => currentBook.value?.title ?? '')
const currentChapterTitle = computed(() => currentChapter.value?.title ?? '')
const currentSectionTitle = computed(
  () => currentSections.value.find((item) => item.id === form.sectionId)?.title ?? ''
)

const currentTypeLabel = computed(() => {
  switch (form.questionType) {
    case 'SINGLE':
      return '单选'
    case 'MULTI':
      return '多选'
    case 'FILL':
      return '填空'
    case 'SOLUTION':
      return '解答'
    default:
      return '题型未定'
  }
})

const previewSourceLabel = computed(() => {
  const sourceYear = normalizedSourceYear()
  const sourcePaper = form.sourcePaper.trim()
  if (sourceYear && sourcePaper) return `(${sourceYear})(${sourcePaper})`
  if (sourceYear) return `(${sourceYear})`
  if (sourcePaper) return `(${sourcePaper})`
  return '未设置来源'
})

const previewChoiceOptions = computed(() => form.choiceOptions.filter((item) => item.content.trim()))
const previewFillAnswers = computed(() => form.fillAnswers.map((item) => item.content.trim()))
const previewSubQuestions = computed(() =>
  form.subQuestions.map((item) => ({
    prompt: item.prompt.trim(),
    referenceAnswer: item.referenceAnswer.trim()
  }))
)
const submitStateClass = computed(() => ({
  success: submitState.value === 'success',
  error: submitState.value === 'error'
}))

onMounted(async () => {
  await loadChapterTree()
  if (isEditMode.value) {
    await loadQuestionDetail()
  } else {
    applyDefaultChapterSelection()
  }
})

async function loadChapterTree() {
  loading.value = true
  try {
    const data = await unwrap(api.get('/chapters/tree'))
    chapterTree.value = Array.isArray(data) ? data : []
    if (!chapterTree.value.length) {
      submitMessage.value = '章节目录为空，请先初始化章节数据。'
      submitState.value = 'error'
    }
  } catch (error) {
    chapterTree.value = []
    submitMessage.value = error?.response?.data?.message || '章节目录加载失败'
    submitState.value = 'error'
  } finally {
    loading.value = false
  }
}

async function loadQuestionDetail() {
  loading.value = true
  try {
    const detail = await unwrap(api.get(`/admin/math-questions/${route.params.id}`))
    form.questionType = detail.questionType || 'SINGLE'
    form.rawTextLatex = detail.rawTextLatex || ''
    form.imageUrl = detail.imageUrl || ''
    form.sourceYearText = detail.sourceYear ? String(detail.sourceYear) : ''
    form.sourcePaper = detail.sourcePaper || ''
    form.teacherExplanation = detail.teacherExplanation || ''
    currentQuestionNo.value = detail.questionNo ?? null

    applyChapterSelectionByTitles(detail.bookName, detail.chapterName, detail.sectionName)

    if (['SINGLE', 'MULTI'].includes(form.questionType)) {
      form.choiceOptions = hydrateChoiceOptions(detail.options)
      form.choiceAnswers = Array.isArray(detail.answers)
        ? detail.answers.map((item) => String(item).trim().toUpperCase())
        : []
    } else if (form.questionType === 'FILL') {
      form.fillAnswers = hydrateFillAnswers(detail.answers)
    } else {
      form.subQuestions = hydrateSubQuestions(detail.subQuestions)
    }

    submitMessage.value = '题目已加载，可继续编辑。'
    submitState.value = 'success'
  } catch (error) {
    submitMessage.value = error?.response?.data?.message || '题目详情加载失败'
    submitState.value = 'error'
  } finally {
    loading.value = false
  }
}

function applyDefaultChapterSelection() {
  const firstBook = chapterTree.value[0]
  if (!firstBook) return
  form.bookId = firstBook.id
  const firstChapter = firstBook.children?.[0]
  form.chapterId = firstChapter?.id ?? null
  const firstSection = firstChapter?.children?.[0]
  form.sectionId = firstSection?.id ?? null
}

function applyChapterSelectionByTitles(bookTitle, chapterTitle, sectionTitle) {
  const book = chapterTree.value.find((item) => item.title === bookTitle) ?? chapterTree.value[0]
  form.bookId = book?.id ?? null
  const chapter = book?.children?.find((item) => item.title === chapterTitle) ?? book?.children?.[0]
  form.chapterId = chapter?.id ?? null
  const section = chapter?.children?.find((item) => item.title === sectionTitle) ?? chapter?.children?.[0]
  form.sectionId = section?.id ?? null
}

function handleBookChange() {
  const nextChapter = currentChapters.value[0] ?? null
  form.chapterId = nextChapter?.id ?? null
  form.sectionId = nextChapter?.children?.[0]?.id ?? null
}

function handleChapterChange() {
  form.sectionId = currentSections.value[0]?.id ?? null
}

function handleTypeChange() {
  if (isChoiceType.value && !form.choiceOptions.length) {
    form.choiceOptions = createDefaultChoiceOptions()
  }
  if (isFillType.value && !form.fillAnswers.length) {
    form.fillAnswers = [createFillAnswer()]
  }
  if (isSolutionType.value && !form.subQuestions.length) {
    form.subQuestions = [createSubQuestion()]
  }
}

function createDefaultChoiceOptions() {
  return ['A', 'B', 'C', 'D'].map((key, index) => ({
    id: `option-${index + 1}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    key,
    content: ''
  }))
}

function createFillAnswer(content = '') {
  return {
    id: `fill-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    content
  }
}

function createSubQuestion(prompt = '', referenceAnswer = '') {
  return {
    id: `sub-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    prompt,
    referenceAnswer
  }
}

function hydrateChoiceOptions(options) {
  const source = Array.isArray(options) && options.length ? options : []
  const mapped = source
    .map((item, index) => ({
      id: `option-loaded-${index + 1}`,
      key: String(item?.key || String.fromCharCode(65 + index))
        .trim()
        .toUpperCase(),
      content: String(item?.content || '').trim()
    }))
    .filter((item) => item.key && item.content)
  return mapped.length ? mapped : createDefaultChoiceOptions()
}

function hydrateFillAnswers(answers) {
  const source = Array.isArray(answers) ? answers : []
  const mapped = source
    .map((item) => createFillAnswer(String(item || '').trim()))
    .filter((item) => item.content)
  return mapped.length ? mapped : [createFillAnswer()]
}

function hydrateSubQuestions(subQuestions) {
  const source = Array.isArray(subQuestions) ? subQuestions : []
  const mapped = source
    .map((item) =>
      createSubQuestion(String(item?.prompt || '').trim(), String(item?.referenceAnswer || '').trim())
    )
    .filter((item) => item.prompt || item.referenceAnswer)
  return mapped.length ? mapped : [createSubQuestion()]
}

function addChoiceOption() {
  if (form.choiceOptions.length >= 6) return
  const key = String.fromCharCode(65 + form.choiceOptions.length)
  form.choiceOptions.push({
    id: `option-${key}-${Date.now()}`,
    key,
    content: ''
  })
}

function removeChoiceOption(index) {
  if (form.choiceOptions.length <= 4) return
  const [removed] = form.choiceOptions.splice(index, 1)
  form.choiceAnswers = form.choiceAnswers.filter((item) => item !== removed.key)
  form.choiceOptions.forEach((item, optionIndex) => {
    item.key = String.fromCharCode(65 + optionIndex)
  })
  form.choiceAnswers = form.choiceAnswers.filter((key) =>
    form.choiceOptions.some((item) => item.key === key)
  )
}

function isChoiceAnswerSelected(key) {
  return form.choiceAnswers.includes(key)
}

function toggleChoiceAnswer(key) {
  if (form.questionType === 'SINGLE') {
    form.choiceAnswers = [key]
    return
  }
  if (form.choiceAnswers.includes(key)) {
    form.choiceAnswers = form.choiceAnswers.filter((item) => item !== key)
    return
  }
  form.choiceAnswers = [...form.choiceAnswers, key].sort()
}

function addFillAnswer() {
  form.fillAnswers.push(createFillAnswer())
}

function removeFillAnswer(index) {
  if (form.fillAnswers.length <= 1) return
  form.fillAnswers.splice(index, 1)
}

function addSubQuestion() {
  form.subQuestions.push(createSubQuestion())
}

function removeSubQuestion(index) {
  if (form.subQuestions.length <= 1) return
  form.subQuestions.splice(index, 1)
}

function openFileDialog() {
  fileInput.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (file) {
    await uploadImage(file)
  }
  event.target.value = ''
}

async function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    await uploadImage(file)
  }
}

async function handlePaste(event) {
  const file = [...(event.clipboardData?.files ?? [])][0]
  if (file) {
    await uploadImage(file)
  }
}

async function uploadImage(file) {
  uploadError.value = ''
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await unwrap(
      api.post('/admin/uploads/question-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    )
    form.imageUrl = data.url || ''
  } catch (error) {
    uploadError.value = error?.response?.data?.message || '图片上传失败'
  } finally {
    uploading.value = false
  }
}

function normalizedSourceYear() {
  const value = form.sourceYearText.trim()
  if (!value) return null
  const number = Number(value)
  return Number.isInteger(number) ? number : null
}

function buildAnswerLatex() {
  if (form.questionType === 'SINGLE' || form.questionType === 'MULTI') {
    return form.choiceAnswers.join(', ')
  }
  if (form.questionType === 'FILL') {
    return form.fillAnswers
      .map((item) => item.content.trim())
      .filter(Boolean)
      .join('\n')
  }
  return form.subQuestions
    .map((item, index) => {
      const answer = item.referenceAnswer.trim()
      return answer ? `（${index + 1}）${answer}` : ''
    })
    .filter(Boolean)
    .join('\n')
}

function buildPayload() {
  return {
    imageUrl: form.imageUrl || '',
    questionType: form.questionType,
    rawTextLatex: form.rawTextLatex.trim(),
    options: isChoiceType.value
      ? form.choiceOptions.map((item) => ({ key: item.key, content: item.content.trim() }))
      : [],
    answers:
      form.questionType === 'SINGLE' || form.questionType === 'MULTI'
        ? [...form.choiceAnswers].sort()
        : form.questionType === 'FILL'
          ? form.fillAnswers.map((item) => item.content.trim()).filter(Boolean)
          : [],
    subQuestions: isSolutionType.value
      ? form.subQuestions.map((item, index) => ({
          index: index + 1,
          prompt: item.prompt.trim(),
          referenceAnswer: item.referenceAnswer.trim(),
          steps: []
        }))
      : [],
    answerLatex: buildAnswerLatex(),
    teacherExplanation: form.teacherExplanation.trim(),
    bookName: currentBookTitle.value,
    chapterName: currentChapterTitle.value,
    sectionName: currentSectionTitle.value,
    sourceYear: normalizedSourceYear(),
    sourcePaper: form.sourcePaper.trim()
  }
}

function validateForm() {
  if (!currentBookTitle.value || !currentChapterTitle.value || !currentSectionTitle.value) {
    return '请先完整选择教材、章节和小节'
  }
  if (!form.rawTextLatex.trim()) {
    return '题干不能为空'
  }
  if (form.questionType === 'SINGLE' || form.questionType === 'MULTI') {
    const validOptions = form.choiceOptions.filter((item) => item.content.trim())
    if (validOptions.length < 2) {
      return '选择题至少需要两个有效选项'
    }
    if (!form.choiceAnswers.length) {
      return '请选择正确答案'
    }
    if (form.questionType === 'SINGLE' && form.choiceAnswers.length !== 1) {
      return '单选题只能设置一个正确答案'
    }
  }
  if (form.questionType === 'FILL') {
    const validAnswers = form.fillAnswers.map((item) => item.content.trim()).filter(Boolean)
    if (!validAnswers.length) {
      return '请至少填写一个正确答案'
    }
  }
  if (form.questionType === 'SOLUTION') {
    const invalid = form.subQuestions.some((item) => !item.prompt.trim() || !item.referenceAnswer.trim())
    if (invalid) {
      return '每个小问都需要填写题干和参考答案'
    }
  }
  if (form.sourceYearText.trim() && normalizedSourceYear() === null) {
    return '年份必须是有效整数'
  }
  return ''
}

async function submitForm() {
  const validationError = validateForm()
  if (validationError) {
    submitMessage.value = validationError
    submitState.value = 'error'
    return
  }

  saving.value = true
  submitState.value = ''
  submitMessage.value = '正在保存题目...'
  try {
    const payload = buildPayload()
    if (isEditMode.value) {
      await unwrap(api.put(`/admin/math-questions/${route.params.id}`, payload))
      await router.push({ path: '/admin/questions', query: { saved: 'updated' } })
      return
    }
    const result = await unwrap(api.post('/admin/math-questions', payload))
    currentQuestionNo.value = result?.questionNo ?? currentQuestionNo.value
    await router.push({ path: '/admin/questions', query: { saved: 'created' } })
  } catch (error) {
    submitMessage.value = error?.response?.data?.message || '题目保存失败'
    submitState.value = 'error'
  } finally {
    saving.value = false
  }
}
</script>
