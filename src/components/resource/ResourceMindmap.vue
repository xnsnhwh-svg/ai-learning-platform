<template>
  <div class="resource-mindmap">
    <h3>{{ title }}</h3>
    <div class="mindmap-container" ref="containerRef">
      <svg :width="svgWidth" :height="svgHeight" v-if="nodes.length">
        <line v-for="(edge, i) in edges" :key="'e'+i" :x1="edge.x1" :y1="edge.y1" :x2="edge.x2" :y2="edge.y2" stroke="#c0c4cc" stroke-width="2" />
        <g v-for="(node, i) in nodes" :key="'n'+i" @click="selected = i">
          <rect :x="node.x - 60" :y="node.y - 16" width="120" height="32" rx="6" :fill="i === 0 ? '#5B7B8A' : selected === i ? '#C4704A' : '#F0ECE4'" :stroke="selected === i ? '#C4704A' : '#E3DED7'" stroke-width="2" style="cursor:pointer" />
          <text :x="node.x" :y="node.y + 5" text-anchor="middle" :fill="i === 0 ? '#FCFAF7' : '#1C1B1A'" font-size="13">{{ node.label }}</text>
        </g>
      </svg>
      <el-empty v-else description="暂无思维导图数据" :image-size="40" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({ content: String, title: String })

const nodes = ref([])
const edges = ref([])
const selected = ref(0)

const parseMindmap = () => {
  try {
    const parsed = JSON.parse(props.content || '{}')
    if (parsed.nodes) { nodes.value = parsed.nodes; edges.value = parsed.edges || []; return }
  } catch {}
  const lines = (props.content || '').split('\n').filter(l => l.trim())
  const result = []
  const conns = []
  lines.forEach((line, i) => {
    const depth = (line.match(/^\s*/)[0] || '').length / 2
    const label = line.trim().replace(/^[-*]\s*/, '')
    result.push({ label, depth, y: 40 + i * 50 })
    if (i > 0) {
      for (let j = i - 1; j >= 0; j--) {
        if ((result[j].depth || 0) < depth) {
          conns.push({ from: j, to: i })
          break
        }
      }
    }
  })
  const xPositions = { 0: 400, 1: 200, 2: 600, 3: 100, 4: 300, 5: 500, 6: 700 }
  nodes.value = result.map((n, i) => ({ ...n, x: xPositions[n.depth] || 400, y: n.y }))
  edges.value = conns.map(c => ({
    x1: nodes.value[c.from].x, y1: nodes.value[c.from].y + 16,
    x2: nodes.value[c.to].x, y2: nodes.value[c.to].y - 16
  }))
}

const svgWidth = computed(() => 800)
const svgHeight = computed(() => Math.max(400, nodes.value.length * 50 + 60))

onMounted(parseMindmap)
</script>

<style scoped>
.resource-mindmap { padding: var(--space-md); }
.resource-mindmap h3 { margin-bottom: var(--space-md); color: var(--text-primary); font-size: var(--fs-lg); }
.mindmap-container { overflow-x: auto; text-align: center; }
</style>
