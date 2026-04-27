export function normalizeStudentSourceLabel(label) {
  const text = String(label || '').trim()
  if (!text) return ''

  const match = text.match(/^\d+\.(.+)$/)
  if (!match) return text

  return match[1].trim() || text
}

export function stripStudentQuestionNoPrefix(text) {
  const value = String(text || '').trim()
  if (!value) return ''

  return value
    .replace(/^\d+\.\(\d{4}\)\([^)]*\)\s*/, '')
    .replace(/^\d+\.\s*/, '')
    .trim()
}
