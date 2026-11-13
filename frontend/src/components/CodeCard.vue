<script setup>
defineProps({ title: String, desc: String, code: String })
</script>

<template>
  <div class="card">
    <h3>{{ title }}</h3>
    <p v-if="desc" class="desc">{{ desc }}</p>
    <details v-if="code">
      <summary>핵심 코드 보기</summary>
      <pre class="code"><code>{{ code }}</code></pre>
    </details>
    <slot />
  </div>
</template>
