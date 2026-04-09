<template>
  <li class="tree-node">
    <span>{{ node.title }}</span>
    <ul v-if="node.children?.length" class="tree-list">
      <ChapterTreeNode v-for="child in node.children" :key="child.id" :node="child" />
    </ul>
  </li>
</template>

<script setup>
defineProps({
  node: { type: Object, required: true }
})
</script>
