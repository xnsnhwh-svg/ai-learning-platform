<template>
  <div class="knowledge-page page-enter">
    <AppNavbar />
    <div class="page-container">
      <div class="page-header">
        <h1>知识图谱</h1>
        <p>浏览课程知识点的层级结构和依赖关系</p>
        <div class="header-controls">
          <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadGraph" style="width: 300px">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="view-toggle">
            <el-radio-group v-model="viewMode" size="small" @change="loadGraph">
              <el-radio-button value="all">全部知识点</el-radio-button>
              <el-radio-button value="path">关联学习路径</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>

      <div class="graph-content" v-loading="loading">
        <div class="graph-layout">
          <div class="graph-main">
            <div class="graph-visual" ref="chartRef">
              <div v-if="!selectedCourseId" class="empty-state">
                <el-icon :size="64" color="#c0c4cc"><Connection /></el-icon>
                <p>请选择上方课程查看知识图谱</p>
              </div>
            </div>
          </div>
          <aside class="graph-sidebar">
            <div class="sidebar-section">
              <h3>知识点详情</h3>
              <div v-if="selectedKp" class="kp-detail">
                <h4>{{ selectedKp.name }}</h4>
                <el-tag :type="difficultyType(selectedKp.difficulty)" size="small">
                  {{ selectedKp.difficulty }}
                </el-tag>
                <el-tag v-if="selectedKp.parentName" size="small" type="info" style="margin-left:4px">
                  所属: {{ selectedKp.parentName }}
                </el-tag>
                <p class="kp-desc" v-html="renderMarkdown(selectedKp.description)"></p>
                <div v-if="selectedKp.keywords" class="kp-tags">
                  <el-tag v-for="kw in selectedKp.keywords.split(',')" :key="kw" size="small" hit>
                    {{ kw.trim() }}
                  </el-tag>
                </div>
                <div v-if="prereqs.length" class="kp-pre">
                  <strong>前置知识：</strong>
                  <el-tag v-for="pre in prereqs" :key="pre.id" size="small" type="warning">
                    {{ pre.name }}
                  </el-tag>
                </div>
              </div>
              <el-empty v-else description="点击图谱中的节点查看详情" :image-size="40" />
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import AppNavbar from '../components/AppNavbar.vue'
import { useAuthStore } from '../stores/auth'
import { getCourses, getCourseGraph } from '../api/knowledge'
import { getUserPaths, getPathDetail } from '../api/learningPath'

const authStore = useAuthStore()
const courses = ref([])
const selectedCourseId = ref(null)
const selectedKp = ref(null)
const prereqs = ref([])
const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null
const viewMode = ref('all')
const pathKpIds = ref(new Set())
const pathNodes = ref([])

const renderMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br/>')
    .replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
}

const difficultyType = (d) => {
  if (d === 'L1') return 'success'
  if (d === 'L2') return 'warning'
  if (d === 'L3') return 'danger'
  return 'info'
}

const chapterMap = {
  'java-basic': { name: 'Java入门', color: '#42b883' },
  'java-oop': { name: '面向对象', color: '#3b82f6' },
  'java-api': { name: '常用API', color: '#f59e0b' },
  'java-exception': { name: '异常处理', color: '#ef4444' },
  'java-collection': { name: '集合框架', color: '#8b5cf6' },
  'java-io': { name: 'IO流', color: '#06b6d4' },
  'java-thread': { name: '多线程', color: '#f97316' },
  'python-basic': { name: 'Python基础', color: '#2d8c7d' },
  'python-oop': { name: 'Python面向对象', color: '#4a90c4' },
  'python-adv': { name: 'Python高级', color: '#c4784a' },
  'python-lib': { name: 'Python标准库', color: '#8b9a7a' },
  'ds-intro': { name: '概述', color: '#7a8b99' },
  'ds-linear': { name: '线性结构', color: '#5b9b8a' },
  'ds-tree': { name: '树结构', color: '#6b8a5b' },
  'ds-graph': { name: '图结构', color: '#9b7a5b' },
  'ds-algo': { name: '算法', color: '#8a6b7b' }
}

const getChapterIdx = (code) => {
  const prefix = code.split('-').slice(0, 2).join('-')
  return Object.keys(chapterMap).indexOf(prefix)
}

const loadCourses = async () => {
  try {
    const res = await getCourses()
    courses.value = res.data || []
    if (courses.value.length > 0) {
      selectedCourseId.value = courses.value[0].id
      await loadGraph()
    }
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const loadGraph = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  pathKpIds.value = new Set()
  pathNodes.value = []
  try {
    // 如果为路径模式，先加载该用户的学习路径知识点
    if (viewMode.value === 'path' && authStore.userId) {
      const pathsRes = await getUserPaths(authStore.userId)
      const userPaths = (pathsRes.data || []).filter(p => p.courseId === selectedCourseId.value && p.status === 'active')
      if (userPaths.length > 0) {
        const detail = await getPathDetail(userPaths[0].id)
        const pathData = detail.data
        if (pathData && pathData.nodes) {
          pathNodes.value = pathData.nodes
          const ids = new Set()
          pathData.nodes.forEach(n => {
            if (n.knowledgePointIds) n.knowledgePointIds.forEach(id => ids.add(Number(id)))
          })
          pathKpIds.value = ids
        }
      }
    }
    const res = await getCourseGraph(selectedCourseId.value)
    const data = res.data
    const points = data.points || []
    const edges = data.edges || []
    await nextTick()
    renderECharts(points, edges)
  } catch (e) {
    console.error('加载图谱失败', e)
  } finally {
    loading.value = false
  }
}

const renderECharts = (points, edges) => {
  selectedKp.value = null
  prereqs.value = []

  if (!chartRef.value) return

  if (!points.length) {
    chartRef.value.innerHTML = '<div class="empty-state"><p>该课程暂无知识点</p></div>'
    return
  }

  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(chartRef.value)

  const degree = {}
  points.forEach(p => { degree[p.id] = 0 })
  edges.forEach(e => {
    degree[e.fromKpId] = (degree[e.fromKpId] || 0) + 1
    degree[e.toKpId] = (degree[e.toKpId] || 0) + 1
  })
  const degValues = Object.values(degree)
  const minDeg = Math.min(...degValues)
  const maxDeg = Math.max(...degValues)
  const nodeSize = (deg) => {
    if (maxDeg === minDeg) return 14
    return 7 + (deg - minDeg) / (maxDeg - minDeg) * 15
  }

  const categories = Object.entries(chapterMap).map(([, v]) => ({
    name: v.name,
    itemStyle: { color: v.color }
  }))

  const isPathMode = viewMode.value === 'path' && (pathKpIds.value.size > 0 || pathNodes.value.length > 0)
  const filteredPoints = isPathMode ? points.filter(p => pathKpIds.value.has(p.id)) : points

  const graphNodes = filteredPoints.map(p => {
    const inPath = pathKpIds.value.has(p.id)
    return {
      id: String(p.id),
      name: p.name,
      category: getChapterIdx(p.code),
      symbolSize: nodeSize(degree[p.id] || 0),
      kp: p,
      itemStyle: inPath ? {
        borderColor: '#C4704A',
        borderWidth: 3,
        shadowBlur: 8,
        shadowColor: 'rgba(196,112,74,0.4)'
      } : undefined
    }
  })

  const filteredEdgeIds = new Set()
  filteredPoints.forEach(p => { filteredEdgeIds.add(p.id) })
  const graphEdges = edges.filter(e => filteredEdgeIds.has(e.fromKpId) && filteredEdgeIds.has(e.toKpId)).map(e => ({
    source: String(e.fromKpId),
    target: String(e.toKpId),
    lineStyle: isPathMode ? { width: 2.5, opacity: 0.7, color: 'source' } : undefined
  }))

  // 路径模式：将路径节点作为主图节点显示（关联的 KP 节点作为辅助）
  if (isPathMode && pathNodes.value.length > 0) {
    const existingIds = new Set(filteredPoints.map(p => p.id))
    const addedIds = new Set()

    categories.push({ name: '路径节点', itemStyle: { color: '#C4704A' } })
    const pathCatIdx = categories.length - 1

    // 1) 先移除已有的 filteredPoints 节点（路径模式下以路径节点为主要）
    graphNodes.length = 0

    // 2) 所有路径节点作为主节点
    const sorted = [...pathNodes.value].sort((a, b) => a.nodeOrder - b.nodeOrder)
    sorted.forEach((n, i) => {
      const nodeId = 'pn-' + n.id
      addedIds.add(nodeId)
      graphNodes.push({
        id: nodeId,
        name: n.title,
        category: pathCatIdx,
        symbolSize: 18 - i * 0.5,
        kp: { name: n.title, difficulty: n.nodeType === 'new_learn' ? 'L1' : 'L2', description: n.reason || '', isPathNode: true },
        itemStyle: { color: '#C4704A', borderColor: '#E8A87C', borderWidth: 2, shadowBlur: 8, shadowColor: 'rgba(196,112,74,0.4)' }
      })
    })

    // 3) 路径节点之间的顺序边
    for (let i = 0; i < sorted.length - 1; i++) {
      const fromId = 'pn-' + sorted[i].id
      const toId = 'pn-' + sorted[i + 1].id
      graphEdges.push({
        source: fromId,
        target: toId,
        lineStyle: { width: 2.5, opacity: 0.7, color: '#C4704A', curveness: 0.05, type: 'solid' }
      })
    }

    // 4) 如果有关联的 KP 节点，作为辅助节点（小尺寸、低不透明度）
    sorted.forEach(n => {
      if (n.knowledgePointIds) {
        n.knowledgePointIds.forEach(kpId => {
          const kpStr = String(kpId)
          if (existingIds.has(kpId) && !addedIds.has(kpStr)) {
            addedIds.add(kpStr)
            const kp = points.find(p => p.id === kpId)
            if (kp) {
              graphNodes.push({
                id: kpStr,
                name: kp.name,
                category: getChapterIdx(kp.code),
                symbolSize: 10,
                kp: kp,
                itemStyle: { opacity: 0.55, borderColor: '#C4704A', borderWidth: 1 }
              })
              // 从路径节点指向关联 KP
              graphEdges.push({
                source: 'pn-' + n.id,
                target: kpStr,
                lineStyle: { width: 1, opacity: 0.3, color: '#C4704A', curveness: 0.2, type: 'dotted' }
              })
            }
          }
        })
      }
    })
  }

  const option = {
    title: { show: false },
    tooltip: {
      formatter: params => {
        if (params.dataType === 'node') {
          const c = categories[params.data.category] || {}
          return `<strong>${params.data.name}</strong><br/>` +
            `<span style="color:${c.itemStyle?.color || '#666'}">● </span>${c.name || ''}` +
            `<br/>难度: ${params.data.kp.difficulty}`
        }
        return ''
      }
    },
    legend: {
      data: categories.map(c => ({ name: c.name, icon: 'circle' })),
      bottom: 0,
      left: 'center',
      orient: 'horizontal',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 11, color: '#94a3b8' }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      force: {
        repulsion: 350,
        edgeLength: [40, 200],
        gravity: 0.05,
        friction: 0.1,
        layoutAnimation: false
      },
      roam: true,
      draggable: true,
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8],
      data: graphNodes,
      edges: graphEdges,
      categories,
      label: {
        show: true,
        position: 'bottom',
        fontSize: 10,
        color: '#64748b',
        opacity: 0.65,
        formatter: p => p.data.name.length > 6 ? p.data.name.slice(0, 6) + '..' : p.data.name
      },
      lineStyle: {
        color: 'source',
        curveness: 0.1,
        width: 1.5,
        opacity: 0.35
      },
      emphasis: {
        focus: 'adjacency',
        itemStyle: { opacity: 1 },
        label: {
          color: '#1e293b',
          fontSize: 12,
          fontWeight: 'bold',
          opacity: 1
        },
        lineStyle: { width: 2.5, opacity: 0.85 }
      },
      blur: {
        opacity: 0.07,
        label: { opacity: 0 },
        lineStyle: { opacity: 0.02 }
      },
      animation: true,
      animationDuration: 500
    }]
  }

  chartInstance.setOption(option)
  chartInstance.on('click', params => {
    if (params.dataType === 'node' && params.data.kp) {
      selectedKp.value = params.data.kp
      prereqs.value = edges
        .filter(e => e.toKpId === params.data.kp.id)
        .map(e => points.find(p => p.id === e.fromKpId))
        .filter(Boolean)
    }
  })
}

onMounted(loadCourses)

onBeforeUnmount(() => {
  if (chartInstance) chartInstance.dispose()
})
</script>

<style scoped>
.knowledge-page { min-height: 100vh; background: var(--bg-page); }
.page-container { max-width: var(--content-max-xl); margin: 0 auto; padding: var(--space-2xl) var(--space-lg); }
.page-header { text-align: center; margin-bottom: var(--space-lg); }
.page-header h1 { font-size: var(--fs-2xl); color: var(--text-primary); margin-bottom: var(--space-xs); }
.page-header p { color: var(--text-secondary); margin-bottom: var(--space-md); }
.header-controls { display: flex; flex-direction: column; align-items: center; gap: var(--space-md); }
.course-selector { display: flex; justify-content: center; }
.view-toggle .el-radio-button__inner { border-color: var(--border); color: var(--text-secondary); }
.view-toggle .el-radio-button.is-active .el-radio-button__inner { background: var(--primary); border-color: var(--primary); color: #fff; }
.graph-content { background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--border); overflow: hidden; }
.graph-layout { display: flex; min-height: 600px; }
.graph-main { flex: 1; padding: var(--space-lg); overflow: hidden; }
.graph-visual { min-height: 600px; width: 100%; }
.graph-sidebar { width: 320px; border-left: 1px solid var(--border); padding: var(--space-lg); overflow-y: auto; max-height: 640px; }
.sidebar-section h3 { font-size: var(--fs-base); margin-bottom: var(--space-md); color: var(--text-primary); }
.kp-detail h4 { font-size: var(--fs-lg); color: var(--text-primary); margin-bottom: var(--space-xs); }
.kp-desc { margin: var(--space-sm) 0; color: var(--text-secondary); font-size: var(--fs-base); line-height: 1.7; max-height: 200px; overflow-y: auto; }
.kp-desc :deep(code) { background: rgba(0,0,0,0.06); padding: 1px 5px; border-radius: 3px; font-size: .9em; }
.kp-desc :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: var(--space-sm); border-radius: var(--radius-md); overflow-x: auto; margin: var(--space-xs) 0; }
.kp-desc :deep(h2), .kp-desc :deep(h3), .kp-desc :deep(h4) { margin: var(--space-sm) 0 var(--space-xs); color: var(--text-primary); }
.kp-desc :deep(ul) { margin: var(--space-xs) 0; padding-left: var(--space-lg); }
.kp-tags { display: flex; flex-wrap: wrap; gap: var(--space-xs); margin: var(--space-xs) 0; }
.kp-pre { margin-top: var(--space-sm); }
.kp-pre strong { display: block; margin-bottom: var(--space-xs); color: var(--text-secondary); font-size: var(--fs-sm); }
.empty-state { text-align: center; padding: 60px 20px; color: var(--text-tertiary); }
</style>
