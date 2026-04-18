<template>
  <section class="question-editor-page" @paste="handlePaste">
    <form class="question-editor" @submit.prevent="submitQuestion">
      <section class="admin-section relation-strip" aria-labelledby="relation-title">
        <div>
          <p class="section-eyebrow">区域 A</p>
          <h2 id="relation-title">教材体系关联</h2>
        </div>

        <label>
          <span>教材名</span>
          <select v-model="selectedBookId" @change="resetChapter">
            <option value="">请选择教材</option>
            <option v-for="book in chapterTree" :key="book.id" :value="book.id">{{ book.title }}</option>
          </select>
        </label>

        <label>
          <span>章节名</span>
          <select v-model="selectedChapterId" :disabled="!chapterOptions.length" @change="resetSection">
            <option value="">请选择章节</option>
            <option v-for="chapter in chapterOptions" :key="chapter.id" :value="chapter.id">{{ chapter.title }}</option>
          </select>
        </label>

        <label>
          <span>小节名</span>
          <select v-model="selectedSectionId" :disabled="!sectionOptions.length">
            <option value="">请选择小节</option>
            <option v-for="section in sectionOptions" :key="section.id" :value="section.id">{{ section.title }}</option>
          </select>
        </label>

        <label class="question-no-field">
          <span>题号</span>
          <input v-model.number="form.questionNo" type="number" min="1" placeholder="如：15" />
        </label>
      </section>

      <div class="editor-workspace">
        <section class="editor-left" aria-label="题目内容编辑">
          <article class="admin-section upload-card">
            <div class="section-headline">
              <div>
                <p class="section-eyebrow">区域 B</p>
                <h2>图片附件</h2>
              </div>
              <button class="outline-btn" type="button" @click="openFilePicker">选择图片</button>
            </div>

            <div
              class="dropzone"
              :class="{ active: dragging, filled: Boolean(form.imageUrl) }"
              tabindex="0"
              @click="openFilePicker"
              @keydown.enter.prevent="openFilePicker"
              @keydown.space.prevent="openFilePicker"
              @dragover.prevent="dragging = true"
              @dragleave.prevent="dragging = false"
              @drop.prevent="handleDrop"
            >
              <input ref="fileInput" class="sr-only" type="file" accept="image/png,image/jpeg,image/webp" @change="handleFileSelect" />
              <div class="dropzone-copy">
                <strong>{{ form.imageUrl ? '图片已上传' : '拖拽、点击或粘贴题目截图' }}</strong>
                <span>支持 png、jpg、jpeg、webp，单图最大 10MB。</span>
              </div>
              <img v-if="form.imageUrl" class="upload-thumb" :src="form.imageUrl" alt="题目图片预览" />
            </div>

            <div v-if="form.imageUrl" class="url-row">
              <input :value="form.imageUrl" readonly aria-label="图片 URL" />
              <button class="outline-btn" type="button" @click="copyText(form.imageUrl)">复制 URL</button>
              <button class="outline-btn" type="button" @click="insertImageLatex">插入 LaTeX</button>
            </div>
            <p v-else class="field-tip warning-text">双轨制建议上传原题截图，便于公式复杂时保真展示。</p>
          </article>

          <article class="admin-section latex-card">
            <div class="section-headline">
              <div>
                <p class="section-eyebrow">区域 B</p>
                <h2>题干编辑</h2>
              </div>
              <div class="source-preview">{{ sourceLabel || '待生成出处标签' }}</div>
            </div>

            <div class="symbol-toolbar" aria-label="常用数学符号工具栏">
              <button v-for="symbol in symbols" :key="symbol.label" type="button" @click="insertSnippet(symbol.snippet, 'raw')">
                {{ symbol.label }}
              </button>
            </div>

            <label class="textarea-field">
              <span>题目文本（LaTeX）</span>
              <textarea
                ref="rawTextInput"
                v-model="form.rawTextLatex"
                rows="14"
                placeholder="输入题干与选项。提交时会自动按 题号.(年份)(卷名) 重组出处前缀。"
                @focus="activeEditor = 'raw'"
              ></textarea>
            </label>
          </article>

          <article class="admin-section answer-grid">
            <label class="textarea-field">
              <span>答案（LaTeX）</span>
              <textarea
                ref="answerInput"
                v-model="form.answerLatex"
                rows="7"
                placeholder="如：A 或 $\left\{-1,1\right\}$"
                @focus="activeEditor = 'answer'"
              ></textarea>
            </label>

            <label class="textarea-field">
              <span>教师解析</span>
              <textarea
                ref="explanationInput"
                v-model="form.teacherExplanation"
                rows="10"
                placeholder="教师补充解析：第一步，..."
                @focus="activeEditor = 'explanation'"
              ></textarea>
            </label>
          </article>

          <article class="admin-section source-card">
            <div>
              <p class="section-eyebrow">区域 D</p>
              <h2>溯源信息</h2>
            </div>
            <label>
              <span>年份</span>
              <input v-model.number="form.sourceYear" type="number" min="1900" placeholder="2025" />
            </label>
            <label>
              <span>试卷来源</span>
              <input v-model.trim="form.sourcePaper" type="text" placeholder="全国甲卷" />
            </label>
            <p class="field-tip">提交入库的题干将使用：{{ sourceLabel || '题号.(年份)(卷名)' }} 题干文本及选项。</p>
          </article>
        </section>

        <aside class="preview-pane" aria-label="实时渲染预览">
          <div class="preview-sticky">
            <div class="preview-head">
              <div>
                <p class="section-eyebrow">区域 C</p>
                <h2>实时预览</h2>
              </div>
              <span class="student-chip">学生端视图</span>
            </div>

            <article class="student-preview-card">
              <div class="preview-question-meta">
                <span>{{ selectedBook?.title || '未选教材' }}</span>
                <span>{{ selectedSection?.title || '未选小节' }}</span>
              </div>

              <div v-if="form.imageUrl" class="preview-image-wrap">
                <img :src="form.imageUrl" alt="题目图片" />
              </div>

              <section class="preview-block">
                <h3>题目</h3>
                <div v-if="renderedRaw.hasContent" class="rendered-latex" :class="{ invalid: renderedRaw.hasError }" v-html="renderedRaw.html"></div>
                <p v-else class="preview-empty">左侧输入题干后实时预览。</p>
              </section>

              <section class="preview-block answer-preview">
                <h3>答案</h3>
                <div v-if="renderedAnswer.hasContent" class="rendered-latex" :class="{ invalid: renderedAnswer.hasError }" v-html="renderedAnswer.html"></div>
                <p v-else class="preview-empty">暂未填写答案。</p>
              </section>

              <section class="preview-block explanation-preview">
                <h3>教师解析</h3>
                <div v-if="renderedExplanation.hasContent" class="rendered-latex" :class="{ invalid: renderedExplanation.hasError }" v-html="renderedExplanation.html"></div>
                <p v-else class="preview-empty">暂未填写教师解析。</p>
              </section>
            </article>

            <div v-if="previewHasError" class="render-warning">有公式渲染失败，已保留原始 LaTeX，请检查反斜杠、括号和分隔符。</div>
          </div>
        </aside>
      </div>

      <footer class="admin-submit-bar">
        <p :class="['submit-message', messageType]">{{ submitMessage }}</p>
        <div class="submit-actions">
          <button class="outline-btn" type="button" @click="resetForm">取消</button>
          <button class="outline-btn" type="button" @click="scrollPreview">预览学生端</button>
          <button class="primary-btn" type="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交入库' }}</button>
        </div>
      </footer>
    </form>
  </section>
</template>

<script setup>
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { api, unwrap } from '../api/client'

const symbols = [
  { label: '\\frac{}{}', snippet: '\\frac{}{}' },
  { label: '\\sqrt{}', snippet: '\\sqrt{}' },
  { label: '\\Delta', snippet: '\\Delta' },
  { label: '\\left(\\right)', snippet: '\\left(  \\right)' },
  { label: 'cases', snippet: '\\begin{cases}\n  \\n\\end{cases}' },
  { label: '\\mathbb{}', snippet: '\\mathbb{}' },
  { label: '\\vec{}', snippet: '\\vec{}' }
]

const form = reactive({
  imageUrl: '',
  rawTextLatex: '',
  answerLatex: '',
  teacherExplanation: '',
  sourceYear: new Date().getFullYear(),
  sourcePaper: '',
  questionNo: null
})

const chapterTree = ref([])
const selectedBookId = ref('')
const selectedChapterId = ref('')
const selectedSectionId = ref('')
const fileInput = ref(null)
const rawTextInput = ref(null)
const answerInput = ref(null)
const explanationInput = ref(null)
const activeEditor = ref('raw')
const dragging = ref(false)
const submitting = ref(false)
const submitMessage = ref('填写完整后可提交入库。')
const messageType = ref('muted')

const selectedBook = computed(() => chapterTree.value.find((item) => String(item.id) === String(selectedBookId.value)))
const chapterOptions = computed(() => selectedBook.value?.children || [])
const selectedChapter = computed(() => chapterOptions.value.find((item) => String(item.id) === String(selectedChapterId.value)))
const sectionOptions = computed(() => selectedChapter.value?.children || [])
const selectedSection = computed(() => sectionOptions.value.find((item) => String(item.id) === String(selectedSectionId.value)))

const sourceLabel = computed(() => {
  const questionNo = Number(form.questionNo)
  if (!Number.isFinite(questionNo) || questionNo <= 0) return ''
  if (!form.sourceYear || !form.sourcePaper) return `${questionNo}.`
  return `${questionNo}.(${form.sourceYear})(${form.sourcePaper})`
})

const normalizedRawText = computed(() => {
  const text = form.rawTextLatex.trim()
  if (!sourceLabel.value) return text
  const body = stripSourcePrefix(text)
  return `${sourceLabel.value} ${body}`.trim()
})

const renderedRaw = computed(() => renderLatexText(normalizedRawText.value))
const renderedAnswer = computed(() => renderLatexText(form.answerLatex))
const renderedExplanation = computed(() => renderLatexText(form.teacherExplanation))
const previewHasError = computed(() => renderedRaw.value.hasError || renderedAnswer.value.hasError || renderedExplanation.value.hasError)

onMounted(loadChapters)

async function loadChapters() {
  try {
    chapterTree.value = await unwrap(api.get('/chapters/tree'))
  } catch (error) {
    chapterTree.value = []
    submitMessage.value = '章节目录加载失败，请确认后端服务已启动。'
    messageType.value = 'error'
  }
}

function resetChapter() {
  selectedChapterId.value = ''
  selectedSectionId.value = ''
}

function resetSection() {
  selectedSectionId.value = ''
}

function openFilePicker() {
  fileInput.value?.click()
}

function handleFileSelect(event) {
  const file = event.target.files?.[0]
  if (file) uploadFile(file)
  event.target.value = ''
}

function handleDrop(event) {
  dragging.value = false
  const file = event.dataTransfer.files?.[0]
  if (file) uploadFile(file)
}

function handlePaste(event) {
  const file = Array.from(event.clipboardData?.files || []).find((item) => item.type.startsWith('image/'))
  if (file) {
    event.preventDefault()
    uploadFile(file)
  }
}

async function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  submitMessage.value = '正在上传图片...'
  messageType.value = 'muted'
  try {
    const data = await unwrap(api.post('/admin/uploads/question-image', formData))
    form.imageUrl = data.url
    submitMessage.value = '图片上传成功。'
    messageType.value = 'success'
  } catch (error) {
    submitMessage.value = error?.response?.data?.message || '图片上传失败。'
    messageType.value = 'error'
  }
}

function insertSnippet(snippet, target = activeEditor.value) {
  const map = {
    raw: { ref: rawTextInput, key: 'rawTextLatex' },
    answer: { ref: answerInput, key: 'answerLatex' },
    explanation: { ref: explanationInput, key: 'teacherExplanation' }
  }
  const entry = map[target] || map.raw
  const el = entry.ref.value
  const value = form[entry.key] || ''
  const start = el?.selectionStart ?? value.length
  const end = el?.selectionEnd ?? value.length
  form[entry.key] = value.slice(0, start) + snippet + value.slice(end)
  nextTick(() => {
    el?.focus()
    const cursor = start + snippet.length
    el?.setSelectionRange(cursor, cursor)
  })
}

function insertImageLatex() {
  if (!form.imageUrl) return
  insertSnippet(`\\includegraphics[width=\\linewidth]{${form.imageUrl}}`, 'raw')
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    submitMessage.value = '已复制到剪贴板。'
    messageType.value = 'success'
  } catch (error) {
    submitMessage.value = '复制失败，请手动复制。'
    messageType.value = 'error'
  }
}

async function submitQuestion() {
  const validationMessage = validateForm()
  if (validationMessage) {
    submitMessage.value = validationMessage
    messageType.value = 'error'
    return
  }

  submitting.value = true
  submitMessage.value = '正在提交入库...'
  messageType.value = 'muted'
  try {
    const payload = {
      imageUrl: form.imageUrl || null,
      rawTextLatex: normalizedRawText.value,
      answerLatex: form.answerLatex.trim() || null,
      teacherExplanation: form.teacherExplanation.trim() || null,
      bookName: selectedBook.value.title,
      chapterName: selectedChapter.value.title,
      sectionName: selectedSection.value.title,
      sourceYear: form.sourceYear || null,
      sourcePaper: form.sourcePaper || null,
      questionNo: Number(form.questionNo)
    }
    const saved = await unwrap(api.post('/admin/math-questions', payload))
    submitMessage.value = `已提交入库：${saved.sourceLabel || `ID ${saved.id}`}`
    messageType.value = 'success'
  } catch (error) {
    submitMessage.value = error?.response?.data?.message || '提交入库失败。'
    messageType.value = 'error'
  } finally {
    submitting.value = false
  }
}

function validateForm() {
  if (!selectedBook.value) return '请选择教材名。'
  if (!selectedChapter.value) return '请选择章节名。'
  if (!selectedSection.value) return '请选择小节名。'
  if (!Number.isFinite(Number(form.questionNo)) || Number(form.questionNo) <= 0) return '请输入有效题号。'
  if (!form.rawTextLatex.trim()) return '题干不能为空。'
  return ''
}

function resetForm() {
  form.imageUrl = ''
  form.rawTextLatex = ''
  form.answerLatex = ''
  form.teacherExplanation = ''
  form.sourceYear = new Date().getFullYear()
  form.sourcePaper = ''
  form.questionNo = null
  selectedBookId.value = ''
  selectedChapterId.value = ''
  selectedSectionId.value = ''
  submitMessage.value = '已清空当前表单。'
  messageType.value = 'muted'
}

function scrollPreview() {
  document.querySelector('.preview-pane')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function stripSourcePrefix(text) {
  return text
    .replace(/^\s*\d+\.\(\d{4}\)\([^)]*\)\s*/, '')
    .replace(/^\s*\d+\.\s*/, '')
}

function renderLatexText(value) {
  const source = value?.trim() || ''
  if (!source) return { html: '', hasError: false, hasContent: false }

  const pattern = /(\$\$[\s\S]+?\$\$|\$[^$\n]+\$)/g
  let cursor = 0
  let hasError = false
  let html = ''
  for (const match of source.matchAll(pattern)) {
    html += escapeHtml(source.slice(cursor, match.index)).replace(/\n/g, '<br>')
    const token = match[0]
    const displayMode = token.startsWith('$$')
    const math = displayMode ? token.slice(2, -2) : token.slice(1, -1)
    try {
      html += katex.renderToString(math, { displayMode, throwOnError: true })
    } catch (error) {
      hasError = true
      html += `<code class="latex-error">${escapeHtml(token)}</code>`
    }
    cursor = match.index + token.length
  }
  html += escapeHtml(source.slice(cursor)).replace(/\n/g, '<br>')
  return { html, hasError, hasContent: true }
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}
</script>
