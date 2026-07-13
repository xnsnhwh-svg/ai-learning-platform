<template>
  <div class="resource-doc">
    <div class="doc-content" v-html="rendered"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ content: String, title: String })

const rendered = computed(() => {
  if (!props.content) return ''
  let html = props.content
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*\*([^*]+)\*\*\*/g, '<em><strong>$1</strong></em>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br>')
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
  return html
})
</script>

<style scoped>
.resource-doc { padding: var(--space-md); line-height: 1.8; color: var(--text-primary); font-size: var(--fs-base); }
.doc-content h2, .doc-content h3, .doc-content h4 { margin: var(--space-md) 0 var(--space-xs); color: var(--text-primary); }
.doc-content pre { background: #1e1e1e; color: #d4d4d4; padding: var(--space-sm) var(--space-md); border-radius: var(--radius-md); overflow-x: auto; }
.doc-content code { background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 4px; font-size: .9em; }
.doc-content ul { padding-left: var(--space-lg); }
.doc-content li { margin: var(--space-xs) 0; }
</style>
