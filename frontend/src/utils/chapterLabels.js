export function buildSectionLabelMap(tree) {
  const map = new Map()

  function visit(nodes) {
    if (!Array.isArray(nodes)) return
    nodes.forEach((node) => {
      if (!node || typeof node !== 'object') return
      const children = Array.isArray(node.children) ? node.children : []
      if (children.length === 0 && node.id != null) {
        map.set(Number(node.id), node.title || '未标注章节')
      }
      visit(children)
    })
  }

  visit(tree)
  return map
}

export function resolveSectionLabel(map, chapterId) {
  if (chapterId == null || chapterId === '') {
    return '未标注章节'
  }
  return map.get(Number(chapterId)) || '未标注章节'
}
