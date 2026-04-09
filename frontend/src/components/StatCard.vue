<template>
  <article class="stat-card" :class="{ highlight }">
    <div class="stat-icon"></div>
    <h3>{{ title }}</h3>
    <p class="stat-value">{{ value }}</p>
    <p v-if="hint" class="stat-hint">{{ hint }}</p>
  </article>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  hint: { type: String, default: '' },
  highlight: { type: Boolean, default: false }
})
</script>
