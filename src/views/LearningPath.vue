<template>
  <div class="path-page page-enter">
    <AppNavbar />

    <!-- 页面头部 -->
    <div class="page-header">
      <h1><el-icon><Guide /></el-icon> 我的学习路径</h1>
      <p>个性化规划，循序渐进掌握每一个知识点</p>
    </div>

    <div class="content-container" v-loading="loading">
      <!-- 路径选择 -->
      <div class="path-selector" v-if="pathList.length > 0">
        <div
          v-for="path in pathList"
          :key="path.id"
          class="path-tab"
          :class="{ active: currentPathId === path.id }"
          @click="loadPathDetail(path.id)"
        >
          <el-icon><Position /></el-icon>
          <div class="path-tab-info">
            <div class="path-tab-name">{{ path.courseName || '学习路径' }}</div>
            <div class="path-tab-meta">
              <el-tag size="small" :type="statusTagType(path.status)">{{ statusLabel(path.status) }}</el-tag>
              <span class="progress-text">{{ path.completedNodeCount || 0 }}/{{ path.nodeCount || 0 }} 节</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 诊断问卷 -->
      <div v-if="showQuiz" class="quiz-wrapper">
        <DiagnosticQuiz
          @complete="handleQuizComplete"
          @load-courses="handleLoadCourses"
          :courses="courseList"
        />
      </div>

      <!-- 空状态（无路径且未开始问卷） -->
      <div v-if="!loading && pathList.length === 0 && !showQuiz && !diagnosticLoading" class="empty-state">
        <el-empty description="还没有学习路径">
          <template #description>
            <p>还没有学习路径，先做个诊断吧</p>
            <p class="empty-tip">回答几个问题，AI 会为你生成专属学习路径</p>
          </template>
          <el-button type="primary" @click="showQuiz = true">开始诊断</el-button>
        </el-empty>
      </div>

      <!-- 路径详情 -->
      <div v-if="currentPath" class="path-detail">
        <!-- 概览卡片 -->
        <div class="overview-card">
          <div class="overview-info">
            <h2>{{ currentPath.courseName || '学习路径' }}</h2>
            <div class="overview-stats">
              <div class="stat">
                <span class="stat-value">{{ currentPath.nodeCount || 0 }}</span>
                <span class="stat-label">学习节点</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ currentPath.completedNodeCount || 0 }}</span>
                <span class="stat-label">已完成</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ totalEstimatedMinutes }}</span>
                <span class="stat-label">预计分钟</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ progressPercent }}%</span>
                <span class="stat-label">完成度</span>
              </div>
            </div>
          </div>
          <div class="overview-actions">
            <el-progress
              type="circle"
              :percentage="progressPercent"
              :width="100"
              color="var(--primary)"
            />
            <el-button size="small" text @click="reassess">重新评估</el-button>
          </div>
        </div>

        <!-- 节点时间线 -->
        <div class="timeline-section">
          <h3 class="section-title"><el-icon><List /></el-icon> 学习进度</h3>
          <div class="timeline">
            <div
              v-for="(node, index) in currentPath.nodes"
              :key="node.id"
              class="timeline-item"
              :class="node.status"
            >
              <div class="timeline-marker">
                <div class="marker-circle">
                  <el-icon v-if="node.status === 'completed'"><Check /></el-icon>
                  <el-icon v-else-if="node.status === 'in_progress'"><Loading /></el-icon>
                  <span v-else>{{ index + 1 }}</span>
                </div>
                <div class="marker-line" v-if="index < currentPath.nodes.length - 1"></div>
              </div>

              <div class="timeline-content">
                <div class="node-card">
                  <div class="node-header">
                    <div class="node-title-row">
                      <span class="node-order">第 {{ node.nodeOrder || index + 1 }} 节</span>
                      <h4 class="node-title">{{ node.title }}</h4>
                    </div>
                    <div class="node-tags">
                      <el-tag size="small" :type="nodeTypeTagType(node.nodeType)">{{ nodeTypeLabel(node.nodeType) }}</el-tag>
                      <el-tag size="small" type="warning" v-if="node.estimatedMinutes">
                        <el-icon><Clock /></el-icon> {{ node.estimatedMinutes }}分钟
                      </el-tag>
                      <el-tag size="small" :type="nodeStatusTagType(node.status)">{{ nodeStatusLabel(node.status) }}</el-tag>
                    </div>
                  </div>

                  <p class="node-reason" v-if="node.reason">{{ node.reason }}</p>

                  <!-- 学习目标 -->
                  <div class="node-teach" v-if="node.learningObjectives">
                    <div class="teach-section">
                      <div class="teach-label"><el-icon><Flag /></el-icon> 学习目标</div>
                      <div class="teach-body" v-html="renderObjectives(node.learningObjectives)"></div>
                    </div>
                  </div>

                  <!-- 教学内容大纲 -->
                  <div class="node-teach" v-if="node.contentOutline">
                    <div class="teach-section">
                      <div class="teach-label"><el-icon><Notebook /></el-icon> 教学内容</div>
                      <div class="teach-content" v-html="renderOutline(node.contentOutline)"></div>
                    </div>
                  </div>

                  <!-- 代码示例 -->
                  <div class="node-teach" v-if="node.codeExample">
                    <div class="teach-section">
                      <div class="teach-label"><el-icon><Code /></el-icon> 代码示例</div>
                      <pre class="code-block"><code>{{ node.codeExample }}</code></pre>
                    </div>
                  </div>

                  <!-- 练习任务 -->
                  <div class="node-teach" v-if="node.practiceTask">
                    <div class="teach-section">
                      <div class="teach-label"><el-icon><Edit /></el-icon> 练习任务</div>
                      <div class="teach-content practice">{{ node.practiceTask }}</div>
                    </div>
                  </div>

                  <!-- 关联知识点 -->
                  <div class="node-points" v-if="node.knowledgePoints && node.knowledgePoints.length">
                    <span class="points-label">关联知识点：</span>
                    <el-tag
                      v-for="kp in node.knowledgePoints"
                      :key="kp.id"
                      size="small"
                      effect="plain"
                      :color="difficultyColor(kp.difficulty)"
                    >
                      {{ kp.name }}
                    </el-tag>
                  </div>

                  <!-- 资源 -->
                  <div class="node-resources" v-if="node.resources && node.resources.length">
                    <div class="resources-label">学习资源：</div>
                    <div class="resource-list">
                      <div
                        v-for="res in node.resources"
                        :key="res.id"
                        class="resource-item"
                        @click="openResource(res)"
                      >
                        <el-icon class="resource-icon"><component :is="resourceIcon(res.resourceType)" /></el-icon>
                        <div class="resource-info">
                          <div class="resource-title">{{ res.title }}</div>
                          <el-tag size="small" effect="plain">{{ resourceTypeLabel(res.resourceType) }}</el-tag>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 状态切换按钮 -->
                  <div class="node-actions">
                    <el-button
                      v-if="node.status === 'pending'"
                      size="small"
                      type="primary"
                      @click="changeNodeStatus(node, 'in_progress')"
                    >开始学习</el-button>
                    <el-button
                      v-if="node.status === 'in_progress'"
                      size="small"
                      type="success"
                      @click="changeNodeStatus(node, 'completed')"
                    >标记完成</el-button>
                    <el-button
                      v-if="node.status !== 'pending'"
                      size="small"
                      text
                      @click="changeNodeStatus(node, 'pending')"
                    >重置</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 资源查看弹窗 -->
    <el-dialog v-model="resourceDialogVisible" :title="currentResource?.title" width="70%" top="5vh">
      <div v-if="currentResource" class="resource-dialog-content">
        <div class="resource-meta">
          <el-tag size="small">{{ resourceTypeLabel(currentResource.resourceType) }}</el-tag>
          <el-tag size="small" type="warning" v-if="currentResource.difficulty">{{ currentResource.difficulty }}</el-tag>
        </div>
        <el-divider />
        <div class="resource-body" v-html="renderResource(currentResource)"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppNavbar from '../components/AppNavbar.vue'
import DiagnosticQuiz from '../components/DiagnosticQuiz.vue'
import { useAuthStore } from '../stores/auth'
import { getUserPaths, getPathDetail, updateNodeStatus } from '../api/learningPath'
import { profileFromDiagnostic } from '../api/agent'
import { getCourses } from '../api/knowledge'

const authStore = useAuthStore()

const loading = ref(false)
const pathList = ref([])
const currentPathId = ref(null)
const currentPath = ref(null)
const resourceDialogVisible = ref(false)
const currentResource = ref(null)
const showQuiz = ref(false)
const diagnosticLoading = ref(false)
const courseList = ref([])

const totalEstimatedMinutes = computed(() => {
  if (!currentPath.value?.nodes) return 0
  return currentPath.value.nodes.reduce((sum, n) => sum + (n.estimatedMinutes || 0), 0)
})

const progressPercent = computed(() => {
  if (!currentPath.value?.nodes?.length) return 0
  const total = currentPath.value.nodes.length
  const done = currentPath.value.nodes.filter(n => n.status === 'completed').length
  return Math.round((done / total) * 100)
})

const statusLabel = (s) => ({ active: '进行中', completed: '已完成', abandoned: '已放弃' }[s] || s)
const statusTagType = (s) => ({ active: 'primary', completed: 'success', abandoned: 'info' }[s] || 'info')
const nodeTypeLabel = (t) => ({ review: '复习巩固', new_learn: '新学内容', reinforce: '强化训练' }[t] || t)
const nodeTypeTagType = (t) => ({ review: 'info', new_learn: 'success', reinforce: 'warning' }[t] || 'info')
const nodeStatusLabel = (s) => ({ pending: '未开始', in_progress: '进行中', completed: '已完成' }[s] || s)
const nodeStatusTagType = (s) => ({ pending: 'info', in_progress: 'warning', completed: 'success' }[s] || 'info')

const difficultyColor = (d) => ({ L1: '#67c23a22', L2: '#e6a23c22', L3: '#f56c6c22' }[d] || '#90939922')

const resourceIcon = (t) => ({
  document: 'Document', quiz: 'EditPen', mindmap: 'Connection',
  reading: 'Reading', video_script: 'VideoPlay'
}[t] || 'Files')
const resourceTypeLabel = (t) => ({
  document: '讲解文档', quiz: '练习题', mindmap: '思维导图',
  reading: '拓展阅读', video_script: '视频讲稿'
}[t] || t)

const renderObjectives = (text) => {
  if (!text) return ''
  return text.split(/\d+[.、]/).filter(Boolean).map(s => `<li>${s.trim()}</li>`).join('')
}

const renderOutline = (text) => {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

const renderResource = (res) => {
  if (!res) return ''
  if (res.contentUrl) {
    return `<p>资源地址：<a href="${res.contentUrl}" target="_blank">${res.contentUrl}</a></p>`
  }
  if (res.contentJson) {
    try {
      const cj = typeof res.contentJson === 'string' ? JSON.parse(res.contentJson) : res.contentJson
      if (cj.markdown) {
        return cj.markdown.replace(/\n/g, '<br>')
      }
      if (cj.questions) {
        return cj.questions.map((q, i) => `<div class="quiz-item"><strong>题${i+1}：</strong>${q.content}</div>`).join('')
      }
      return `<pre>${JSON.stringify(cj, null, 2)}</pre>`
    } catch {
      return `<pre>${String(res.contentJson)}</pre>`
    }
  }
  return '<p>暂无内容</p>'
}

const openResource = (res) => {
  currentResource.value = res
  resourceDialogVisible.value = true
}

const loadPaths = async () => {
  loading.value = true
  try {
    const res = await getUserPaths(authStore.userId)
    pathList.value = res.data || []
    if (pathList.value.length > 0) {
      await loadPathDetail(pathList.value[0].id)
    } else {
      // 无路径时自动弹出诊断问卷
      await handleLoadCourses()
      showQuiz.value = true
    }
  } catch (e) {
    console.error('加载路径失败', e)
  } finally {
    loading.value = false
  }
}

const loadPathDetail = async (pathId) => {
  currentPathId.value = pathId
  try {
    const res = await getPathDetail(pathId)
    currentPath.value = res.data
  } catch (e) {
    console.error('加载路径详情失败', e)
    ElMessage.error('加载路径详情失败')
  }
}

const changeNodeStatus = async (node, status) => {
  try {
    await updateNodeStatus(node.id, status)
    node.status = status
    ElMessage.success('状态已更新')
    // 更新概览统计
    if (currentPath.value) {
      currentPath.value.completedNodeCount = currentPath.value.nodes.filter(n => n.status === 'completed').length
    }
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const handleLoadCourses = async () => {
  try {
    const res = await getCourses()
    courseList.value = res.data || []
  } catch (e) {
    console.error('加载课程失败', e)
  }
}

const handleQuizComplete = async (payload) => {
  showQuiz.value = false
  diagnosticLoading.value = true
  try {
    const res = await profileFromDiagnostic({
      userId: authStore.userId,
      diagnosticText: payload.diagnosticText
    })
    // 重新加载路径列表
    await loadPaths()
    ElMessage.success('学习路径已生成！')
  } catch (e) {
    ElMessage.error('生成路径失败，请重试')
    console.error(e)
  } finally {
    diagnosticLoading.value = false
  }
}

const reassess = () => {
  showQuiz.value = true
  currentPath.value = null
  currentPathId.value = null
}

onMounted(() => {
  loadPaths()
})
</script>

<style scoped>
.path-page { min-height: 100vh; background: var(--bg-page); }

.page-header {
  background: linear-gradient(135deg, #5B7B8A 0%, #4A6572 100%);
  color: white; text-align: center; padding: var(--space-2xl) var(--space-lg);
}
.page-header h1 {
  font-size: var(--fs-3xl); font-weight: var(--fw-bold);
  margin: 0 0 var(--space-xs); display: flex; align-items: center; justify-content: center; gap: var(--space-sm);
}
.page-header p { font-size: var(--fs-base); opacity: 0.9; }

.content-container {
  max-width: 1000px; margin: -20px auto var(--space-2xl);
  padding: 0 var(--space-lg); position: relative; z-index: 1;
}

/* 路径选择 */
.path-selector { display: flex; gap: var(--space-sm); margin-bottom: var(--space-lg); overflow-x: auto; padding-bottom: var(--space-xs); }
.path-tab {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg); background: var(--bg-card);
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
  cursor: pointer; border: 2px solid transparent; transition: all var(--t-fast); min-width: 200px;
}
.path-tab:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.path-tab.active { border-color: var(--primary); }
.path-tab .el-icon { font-size: 24px; color: var(--primary); }
.path-tab-info { flex: 1; }
.path-tab-name { font-size: var(--fs-base); font-weight: var(--fw-semibold); color: var(--text-primary); }
.path-tab-meta { display: flex; align-items: center; gap: var(--space-xs); margin-top: var(--space-xs); }
.progress-text { font-size: var(--fs-xs); color: var(--text-tertiary); }

/* 空状态 */
.empty-state { background: var(--bg-card); border-radius: var(--radius-lg); padding: var(--space-3xl) var(--space-lg); text-align: center; box-shadow: var(--shadow-sm); border: 1px solid var(--border); }
.empty-tip { color: var(--text-secondary); font-size: var(--fs-sm); margin-top: var(--space-xs); }

/* 诊断问卷 */
.quiz-wrapper { background: var(--bg-card); border-radius: var(--radius-lg); padding: var(--space-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--border); }

/* 概览卡片 */
.overview-card { background: var(--bg-card); border-radius: var(--radius-lg); padding: var(--space-lg); margin-bottom: var(--space-lg); display: flex; justify-content: space-between; align-items: center; box-shadow: var(--shadow-sm); border: 1px solid var(--border); }
.overview-actions { display: flex; flex-direction: column; align-items: center; gap: var(--space-sm); }
.overview-info h2 { font-size: var(--fs-2xl); color: var(--text-primary); margin: 0 0 var(--space-md); }
.overview-stats { display: flex; gap: var(--space-xl); }
.stat { display: flex; flex-direction: column; }
.stat-value { font-size: var(--fs-2xl); font-weight: var(--fw-bold); color: var(--primary); }
.stat-label { font-size: var(--fs-xs); color: var(--text-tertiary); margin-top: 2px; }

/* 时间线 */
.section-title { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--fs-xl); color: var(--text-primary); margin: 0 0 var(--space-lg); }
.timeline { position: relative; }
.timeline-item { display: flex; gap: var(--space-lg); margin-bottom: var(--space-xs); }
.timeline-marker { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; }
.marker-circle { width: 36px; height: 36px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-size: var(--fs-base); font-weight: var(--fw-semibold); background: var(--border); color: var(--text-tertiary); flex-shrink: 0; z-index: 1; }
.timeline-item.completed .marker-circle { background: var(--success); color: #fff; }
.timeline-item.in_progress .marker-circle { background: var(--accent); color: #fff; }
.marker-line { width: 2px; flex: 1; background: var(--border); min-height: 40px; }
.timeline-item:last-child .marker-line { display: none; }
.timeline-item.completed .marker-line { background: var(--success); }

.timeline-content { flex: 1; padding-bottom: var(--space-lg); }
.node-card { background: var(--bg-card); border-radius: var(--radius-md); padding: var(--space-lg); box-shadow: var(--shadow-sm); border-left: 4px solid var(--border); border: 1px solid var(--border); transition: all var(--t-fast); }
.node-card:hover { box-shadow: var(--shadow-md); }
.timeline-item.completed .node-card { border-left-color: var(--success); }
.timeline-item.in_progress .node-card { border-left-color: var(--accent); }
.timeline-item.pending .node-card { border-left-color: var(--border); }

.node-header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-sm); flex-wrap: wrap; }
.node-title-row { display: flex; align-items: center; gap: var(--space-sm); flex-wrap: wrap; }
.node-order { font-size: var(--fs-xs); color: var(--text-tertiary); background: var(--bg-subtle); padding: 2px var(--space-xs); border-radius: var(--radius-xs); }
.node-title { font-size: var(--fs-lg); font-weight: var(--fw-semibold); color: var(--text-primary); margin: 0; }
.node-tags { display: flex; gap: var(--space-xs); flex-wrap: wrap; }
.node-reason { font-size: var(--fs-sm); color: var(--text-secondary); margin: var(--space-sm) 0 0; line-height: 1.5; }
.node-points { margin-top: var(--space-sm); display: flex; align-items: center; gap: var(--space-xs); flex-wrap: wrap; }
.points-label { font-size: var(--fs-sm); color: var(--text-tertiary); }
.node-resources { margin-top: var(--space-sm); }
.resources-label { font-size: var(--fs-sm); color: var(--text-tertiary); margin-bottom: var(--space-xs); }
.resource-list { display: flex; flex-direction: column; gap: var(--space-xs); }
.resource-item { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-sm) var(--space-md); background: var(--bg-subtle); border-radius: var(--radius-md); cursor: pointer; transition: all var(--t-fast); }
.resource-item:hover { background: var(--bg-active); }
.resource-icon { font-size: 20px; color: var(--primary); }
.resource-info { flex: 1; display: flex; justify-content: space-between; align-items: center; gap: var(--space-xs); }
.resource-title { font-size: var(--fs-base); color: var(--text-primary); }
.node-actions { margin-top: var(--space-sm); display: flex; gap: var(--space-xs); }

/* 资源弹窗 */
.resource-dialog-content { max-height: 60vh; overflow-y: auto; }
.resource-meta { display: flex; gap: var(--space-xs); }
.resource-body { font-size: var(--fs-base); line-height: 1.7; color: var(--text-primary); }
.resource-body :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: var(--space-sm) var(--space-md); border-radius: var(--radius-md); overflow-x: auto; }
.resource-body :deep(.quiz-item) { padding: var(--space-sm); background: var(--bg-subtle); border-radius: var(--radius-sm); margin-bottom: var(--space-xs); }

/* 教学内容区块 */
.node-teach { margin-top: var(--space-sm); }
.teach-section { background: var(--bg-subtle); border-radius: var(--radius-md); padding: var(--space-sm) var(--space-md); margin-bottom: var(--space-xs); }
.teach-label { font-size: var(--fs-sm); font-weight: var(--fw-semibold); color: var(--primary); display: flex; align-items: center; gap: 4px; margin-bottom: var(--space-xs); }
.teach-body { font-size: var(--fs-sm); color: var(--text-primary); padding-left: var(--space-md); }
.teach-body li { margin-bottom: 2px; line-height: 1.6; }
.teach-content { font-size: var(--fs-sm); color: var(--text-primary); line-height: 1.7; }
.teach-content.practice { color: var(--accent); }
.code-block { background: #1e1e1e; color: #d4d4d4; padding: var(--space-sm) var(--space-md); border-radius: var(--radius-md); overflow-x: auto; font-size: var(--fs-sm); line-height: 1.5; margin: var(--space-xs) 0 0; }
.code-block code { font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace; white-space: pre; }

@media (max-width: 768px) {
  .overview-card { flex-direction: column; gap: var(--space-lg); }
  .overview-stats { flex-wrap: wrap; gap: var(--space-lg); justify-content: center; }
}
</style>
